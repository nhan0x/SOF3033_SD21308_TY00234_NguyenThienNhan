package com.fashionweb.service.cart;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.DiscountType;
import com.fashionweb.repository.cart.VoucherRepository;
import com.fashionweb.repository.shop.CategoryRepository;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Category;
import com.fashionweb.exception.VoucherException;
import com.fashionweb.model.Cart;
import com.fashionweb.model.CartItem;
import com.fashionweb.model.Voucher;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Mã giảm giá: kiểm tra, tính giảm giá, quản lý của Admin.
 *
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL. Quy tắc theo mục "Quy tắc voucher và phí ship" trong project.md.
 */
public class VoucherService {

    private static final Pattern CODE = Pattern.compile("[A-Z0-9]{4,20}");

    private final VoucherRepository voucherRepository = new VoucherRepository();
    private final CategoryRepository categoryRepository = new CategoryRepository();

    /**
     * Kiểm tra theo đúng thứ tự 7 bước trong project.md. Hợp lệ trả Voucher; sai ném VoucherException
     * với thông báo tiếng Việt riêng cho từng bước.
     */
    public Voucher validate(String code, int userId, Cart cart) throws VoucherException {
        String c = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        // 1. Không tồn tại
        Voucher v = c.isEmpty() ? null : voucherRepository.findByCode(c);
        if (v == null) {
            throw new VoucherException("Mã giảm giá không tồn tại.");
        }
        // 2. Bị vô hiệu hóa
        if (!v.isActive()) {
            throw new VoucherException("Mã giảm giá " + v.getCode() + " đang bị vô hiệu hóa.");
        }
        // 3. Chưa đến hạn hoặc đã quá hạn
        Date now = new Date();
        if (now.before(v.getStartDate())) {
            throw new VoucherException("Mã giảm giá " + v.getCode() + " chưa đến thời gian áp dụng.");
        }
        if (now.after(v.getEndDate())) {
            throw new VoucherException("Mã giảm giá " + v.getCode() + " đã hết hạn.");
        }
        // 4. Hết tổng lượt dùng
        if (v.getUsedCount() >= v.getUsageLimit()) {
            throw new VoucherException("Mã giảm giá " + v.getCode() + " đã hết lượt sử dụng.");
        }
        // 5. Khách đã dùng đủ số lần cho phép (số đơn chưa hủy có mã này)
        if (voucherRepository.countUsedByUser(v.getId(), userId) >= v.getPerUserLimit()) {
            throw new VoucherException("Bạn đã dùng mã " + v.getCode() + " đủ số lần cho phép.");
        }
        // 6. Chưa đạt đơn tối thiểu (tính trên tổng tiền hàng cả đơn)
        long subtotal = cart == null ? 0 : cart.getSubtotal();
        if (subtotal < v.getMinOrderAmount()) {
            throw new VoucherException("Đơn hàng cần tối thiểu " + money(v.getMinOrderAmount())
                    + " để dùng mã " + v.getCode() + ".");
        }
        // 7. Đơn không có sản phẩm thuộc danh mục áp dụng
        if (v.getCategoryId() != null) {
            boolean hasCategory = false;
            if (cart != null && cart.getItems() != null) {
                for (CartItem it : cart.getItems()) {
                    if (it.getCategoryId() == v.getCategoryId()) {
                        hasCategory = true;
                        break;
                    }
                }
            }
            if (!hasCategory) {
                throw new VoucherException("Mã " + v.getCode() + " chỉ áp dụng cho đơn có sản phẩm thuộc danh mục \""
                        + v.getCategoryName() + "\".");
            }
        }
        return v;
    }

    /**
     * PERCENT: subtotal x % (không vượt maxDiscount). AMOUNT: số tiền cố định. Không vượt subtotal.
     */
    public long computeDiscount(Voucher v, long subtotal) {
        if (v == null || subtotal <= 0) {
            return 0;
        }
        long discount;
        if (v.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal * v.getDiscountValue() / 100;
            if (v.getMaxDiscount() != null) {
                discount = Math.min(discount, v.getMaxDiscount());
            }
        } else {
            discount = v.getDiscountValue();
        }
        return Math.max(0, Math.min(discount, subtotal));
    }

    /**
     * Phí ship: 0 khi đơn dùng voucher hợp lệ, ngược lại phí cố định AppConstant.SHIPPING_FEE.
     */
    public long shippingFee(Voucher validVoucher) {
        return validVoucher == null ? AppConstant.SHIPPING_FEE : 0;
    }

    /**
     * Voucher đang dùng được (bật, trong thời hạn, còn lượt), để gợi ý ở trang thanh toán.
     */
    public List<Voucher> findActive() {
        Date now = new Date();
        List<Voucher> list = new ArrayList<>();
        for (Voucher v : voucherRepository.findAll()) {
            if (v.isActive() && !now.before(v.getStartDate()) && !now.after(v.getEndDate()) && v.getUsedCount() < v.getUsageLimit()) {
                list.add(v);
            }
        }
        return list;
    }

    /**
     * Mọi voucher kèm usedCount, categoryName ("Tất cả danh mục" khi không giới hạn), statusLabel (Admin), mới tạo trước.
     * Trạng thái: Tạm dừng | Chưa bắt đầu | Hết hạn | Hết lượt | Đang hoạt động (ưu tiên theo thứ tự này).
     */
    public List<Voucher> findAll() {
        Date now = new Date();
        List<Voucher> list = voucherRepository.findAll();
        for (Voucher v : list) {
            if (v.getCategoryName() == null) {
                v.setCategoryName("Tất cả danh mục");
            }
            String label;
            if (!v.isActive()) {
                label = "Tạm dừng";
            } else if (now.before(v.getStartDate())) {
                label = "Chưa bắt đầu";
            } else if (now.after(v.getEndDate())) {
                label = "Hết hạn";
            } else if (v.getUsedCount() >= v.getUsageLimit()) {
                label = "Hết lượt";
            } else {
                label = "Đang hoạt động";
            }
            v.setStatusLabel(label);
        }
        return list;
    }

    /**
     * id = 0 thêm, id &gt; 0 sửa. code viết hoa không dấu [A-Z0-9]{4,20}, duy nhất, không sửa code khi đã tạo;
     * endDate không trước startDate; PERCENT 1 đến 100, AMOUNT &gt; 0; maxDiscount chỉ dùng cho PERCENT;
     * usageLimit &gt; 0 và không nhỏ hơn số lượt đã dùng; perUserLimit từ 1 đến usageLimit.
     */
    public void save(Voucher v) {
        Map<String, String> errors = new LinkedHashMap<>();
        Voucher existing = v.getId() > 0 ? voucherRepository.findById(v.getId()) : null;
        if (v.getId() > 0 && existing == null) {
            throw new ValidationException(null, "Voucher không tồn tại.");
        }
        if (existing != null) {
            v.setCode(existing.getCode());
        } else {
            String code = v.getCode() == null ? "" : v.getCode().trim().toUpperCase(Locale.ROOT);
            v.setCode(code);
            if (!CODE.matcher(code).matches()) {
                errors.put("code", "Mã gồm 4 đến 20 chữ in hoa không dấu hoặc số.");
            } else if (voucherRepository.findByCode(code) != null) {
                errors.put("code", "Mã đã tồn tại.");
            }
        }
        if (v.getDescription() != null) {
            String d = v.getDescription().trim();
            v.setDescription(d.isEmpty() ? null : d);
            if (d.length() > 255) {
                errors.put("description", "Mô tả tối đa 255 ký tự.");
            }
        }
        if (v.getDiscountType() == null) {
            errors.put("discountType", "Chọn loại giảm giá.");
        } else if (v.getDiscountType() == DiscountType.PERCENT) {
            if (v.getDiscountValue() < 1 || v.getDiscountValue() > 100) {
                errors.put("discountValue", "Phần trăm giảm từ 1 đến 100.");
            }
            if (v.getMaxDiscount() != null && v.getMaxDiscount() <= 0) {
                errors.put("maxDiscount", "Giảm tối đa phải lớn hơn 0 (để trống nếu không giới hạn).");
            }
        } else {
            if (v.getDiscountValue() <= 0) {
                errors.put("discountValue", "Số tiền giảm phải lớn hơn 0.");
            }
            v.setMaxDiscount(null);
        }
        if (v.getMinOrderAmount() < 0) {
            errors.put("minOrderAmount", "Đơn tối thiểu không được âm.");
        }
        if (v.getCategoryId() != null) {
            Category c = categoryRepository.findById(v.getCategoryId());
            if (c == null || c.isDeleted()) {
                errors.put("categoryId", "Danh mục không hợp lệ.");
            }
        }
        if (v.getStartDate() == null) {
            errors.put("startDate", "Chọn ngày bắt đầu.");
        }
        if (v.getEndDate() == null) {
            errors.put("endDate", "Chọn ngày kết thúc.");
        } else if (v.getStartDate() != null && v.getEndDate().before(v.getStartDate())) {
            errors.put("endDate", "Ngày kết thúc không được trước ngày bắt đầu.");
        }
        int used = existing == null ? 0 : existing.getUsedCount();
        if (v.getUsageLimit() <= 0) {
            errors.put("usageLimit", "Tổng lượt dùng phải lớn hơn 0.");
        } else if (v.getUsageLimit() < used) {
            errors.put("usageLimit", "Tổng lượt không được nhỏ hơn số lượt đã dùng (" + used + ").");
        }
        if (v.getPerUserLimit() < 1 || (v.getUsageLimit() > 0 && v.getPerUserLimit() > v.getUsageLimit())) {
            errors.put("perUserLimit", "Lượt mỗi khách từ 1 đến tổng lượt dùng.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (existing == null) {
            v.setId(voucherRepository.insert(v));
        } else {
            voucherRepository.update(v);
        }
    }

    /**
     * Bật hoặc vô hiệu hóa. Trả trạng thái mới (true = đang bật).
     */
    public boolean toggleActive(int id) {
        Voucher v = voucherRepository.findById(id);
        if (v == null) {
            throw new ValidationException(null, "Voucher không tồn tại.");
        }
        voucherRepository.setActive(id, !v.isActive());
        return !v.isActive();
    }

    /**
     * Chỉ xóa khi chưa có lượt dùng và chưa nằm trong đơn nào (kể cả đơn đã hủy); ngược lại chỉ được tạm dừng.
     */
    public void delete(int id) {
        Voucher v = voucherRepository.findById(id);
        if (v == null) {
            throw new ValidationException(null, "Voucher không tồn tại.");
        }
        if (v.getUsedCount() > 0 || voucherRepository.countOrders(id) > 0) {
            throw new ValidationException(null, "Voucher " + v.getCode() + " đã có người dùng, chỉ có thể tạm dừng.");
        }
        voucherRepository.delete(id);
    }

    private static String money(long n) {
        return String.format(Locale.ROOT, "%,d", n).replace(',', '.') + "đ";
    }
}
