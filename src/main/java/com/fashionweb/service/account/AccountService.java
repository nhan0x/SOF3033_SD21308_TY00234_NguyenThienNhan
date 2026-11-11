package com.fashionweb.service.account;

import com.fashionweb.repository.account.AddressRepository;
import com.fashionweb.repository.account.WishlistRepository;
import com.fashionweb.repository.auth.UserRepository;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Address;
import com.fashionweb.model.Product;
import com.fashionweb.model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Hồ sơ, đổi mật khẩu, sổ địa chỉ, yêu thích của khách hàng.
 *
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL. Mọi thao tác đều kiểm tra dữ liệu thuộc đúng user đang đăng nhập.
 */
public class AccountService {

    private static final Pattern PHONE = Pattern.compile("0[0-9]{9}");

    private final UserRepository userRepository = new UserRepository();
    private final AddressRepository addressRepository = new AddressRepository();
    private final WishlistRepository wishlistRepository = new WishlistRepository();

    // ----- Hồ sơ (#4) -----

    /**
     * Cập nhật họ tên, số điện thoại (email và role không đổi ở đây). Sai thì ném ValidationException (fullName, phone).
     */
    public void updateProfile(int userId, String fullName, String phone) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = fullName == null ? "" : fullName.trim();
        String ph = phone == null ? "" : phone.trim();
        if (name.length() < 2 || name.length() > 100) {
            errors.put("fullName", "Họ và tên từ 2 đến 100 ký tự.");
        }
        if (!PHONE.matcher(ph).matches()) {
            errors.put("phone", "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        userRepository.updateProfile(userId, name, ph);
    }

    /**
     * Thông tin user mới nhất từ DB.
     */
    public User findUser(int userId) {
        return userRepository.findById(userId);
    }

    /**
     * Số đơn hàng của user.
     */
    public int countOrders(int userId) {
        return userRepository.countOrders(userId);
    }

    // ----- Sổ địa chỉ (#5) -----

    /**
     * Sổ địa chỉ, địa chỉ mặc định đứng đầu.
     */
    public List<Address> findAddresses(int userId) {
        return addressRepository.findByUser(userId);
    }

    /**
     * Một địa chỉ của user. null nếu không có hoặc thuộc người khác.
     */
    public Address findAddress(int userId, int addressId) {
        Address a = addressRepository.findById(addressId);
        return a != null && a.getUserId() == userId ? a : null;
    }

    /**
     * id = 0 thêm, id > 0 sửa (kiểm tra thuộc user). Địa chỉ đầu tiên tự thành mặc định; đặt mặc định thì bỏ mặc định cũ.
     */
    public void saveAddress(Address a) {
        Map<String, String> errors = new LinkedHashMap<>();
        a.setReceiverName(trim(a.getReceiverName()));
        a.setPhone(trim(a.getPhone()));
        a.setAddressLine(trim(a.getAddressLine()));
        a.setWard(emptyToNull(a.getWard()));
        a.setDistrict(emptyToNull(a.getDistrict()));
        a.setCity(trim(a.getCity()));
        if (a.getReceiverName().length() < 2 || a.getReceiverName().length() > 100) {
            errors.put("receiverName", "Tên người nhận từ 2 đến 100 ký tự.");
        }
        if (!PHONE.matcher(a.getPhone()).matches()) {
            errors.put("phone", "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
        }
        if (a.getAddressLine().length() < 5 || a.getAddressLine().length() > 255) {
            errors.put("addressLine", "Địa chỉ (số nhà, đường) từ 5 đến 255 ký tự.");
        }
        if (a.getWard() != null && a.getWard().length() > 100) {
            errors.put("ward", "Phường/xã tối đa 100 ký tự.");
        }
        if (a.getDistrict() != null && a.getDistrict().length() > 100) {
            errors.put("district", "Quận/huyện tối đa 100 ký tự.");
        }
        if (a.getCity().isEmpty() || a.getCity().length() > 100) {
            errors.put("city", "Vui lòng nhập tỉnh/thành phố (tối đa 100 ký tự).");
        }
        if (a.getId() > 0 && findAddress(a.getUserId(), a.getId()) == null) {
            throw new ValidationException(null, "Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        List<Address> existing = addressRepository.findByUser(a.getUserId());
        boolean onlyThis = existing.isEmpty() || (existing.size() == 1 && existing.get(0).getId() == a.getId());
        if (onlyThis) {
            a.setDefaultAddress(true); // địa chỉ duy nhất luôn là mặc định
        }
        if (a.isDefaultAddress()) {
            addressRepository.clearDefault(a.getUserId());
        } else if (a.getId() > 0 && findAddress(a.getUserId(), a.getId()).isDefaultAddress()) {
            a.setDefaultAddress(true); // không cho bỏ mặc định bằng cách bỏ tick; hãy đặt địa chỉ khác làm mặc định
        }
        if (a.getId() > 0) {
            addressRepository.update(a);
        } else {
            a.setId(addressRepository.insert(a));
        }
    }

    /**
     * Xóa địa chỉ của chính user. Xóa địa chỉ mặc định thì địa chỉ còn lại đầu tiên thành mặc định.
     */
    public void deleteAddress(int userId, int addressId) {
        Address a = findAddress(userId, addressId);
        if (a == null) {
            throw new ValidationException(null, "Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn.");
        }
        addressRepository.delete(addressId);
        if (a.isDefaultAddress()) {
            List<Address> rest = addressRepository.findByUser(userId);
            if (!rest.isEmpty()) {
                addressRepository.setDefault(rest.get(0).getId());
            }
        }
    }

    /**
     * Đặt mặc định, bỏ mặc định các địa chỉ khác.
     */
    public void setDefaultAddress(int userId, int addressId) {
        if (findAddress(userId, addressId) == null) {
            throw new ValidationException(null, "Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn.");
        }
        addressRepository.clearDefault(userId);
        addressRepository.setDefault(addressId);
    }

    // ----- Yêu thích (#5) -----

    /**
     * Sản phẩm yêu thích (chưa xóa mềm), mới thêm trước.
     */
    public List<Product> findWishlist(int userId) {
        return wishlistRepository.findProductsByUser(userId);
    }

    /**
     * Đã yêu thích chưa.
     */
    public boolean isInWishlist(int userId, int productId) {
        return wishlistRepository.exists(userId, productId);
    }

    /**
     * Thêm vào yêu thích (bỏ qua nếu đã có).
     */
    public void addWishlist(int userId, int productId) {
        wishlistRepository.insert(userId, productId);
    }

    /**
     * Bỏ khỏi yêu thích.
     */
    public void removeWishlist(int userId, int productId) {
        wishlistRepository.delete(userId, productId);
    }

    /**
     * Số sản phẩm yêu thích.
     */
    public int countWishlist(int userId) {
        return wishlistRepository.count(userId);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
