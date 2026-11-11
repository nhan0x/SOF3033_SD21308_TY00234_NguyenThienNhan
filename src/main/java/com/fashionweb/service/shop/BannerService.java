package com.fashionweb.service.shop;

import com.fashionweb.repository.shop.BannerRepository;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Banner;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Banner trang chủ.
 *
 * <p>Đã làm: đọc (#9), quản lý của Admin (#11).
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 */
public class BannerService {

    /** Trang trong web (/...) hoặc liên kết ngoài http(s)://, không khoảng trắng, không "//" đầu (tránh chuyển ra ngoài ngầm). */
    private static final Pattern LINK = Pattern.compile("(/(?!/)|https?://)\\S*");

    private final BannerRepository bannerRepository = new BannerRepository();

    /**
     * Banner đang bật, theo sortOrder (trang chủ).
     */
    public List<Banner> findActive() {
        return bannerRepository.findAll(true);
    }

    /**
     * Mọi banner (Admin).
     */
    public List<Banner> findAll() {
        return bannerRepository.findAll(false);
    }

    /**
     * id = 0 thêm, id > 0 sửa.
     */
    public void save(Banner b) {
        Map<String, String> errors = new LinkedHashMap<>();
        Banner existing = b.getId() > 0 ? bannerRepository.findById(b.getId()) : null;
        if (b.getId() > 0 && existing == null) {
            throw new ValidationException(null, "Banner không tồn tại.");
        }
        String title = b.getTitle() == null ? "" : b.getTitle().trim();
        b.setTitle(title);
        if (title.length() < 2 || title.length() > 120) {
            errors.put("title", "Tiêu đề cần từ 2 đến 120 ký tự.");
        }
        String link = b.getLinkUrl() == null ? "" : b.getLinkUrl().trim();
        b.setLinkUrl(link.isEmpty() ? null : link);
        if (!link.isEmpty() && (link.length() > 255 || !LINK.matcher(link).matches())) {
            errors.put("linkUrl", "Liên kết bắt đầu bằng / (trang trong web) hoặc http(s)://, không có khoảng trắng.");
        }
        if (b.getSortOrder() < 1 || b.getSortOrder() > 99) {
            errors.put("sortOrder", "Thứ tự từ 1 đến 99.");
        }
        if (b.getImage() == null || b.getImage().isBlank()) {
            errors.put("imageFile", "Chọn ảnh banner.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (existing == null) {
            b.setId(bannerRepository.insert(b));
        } else {
            bannerRepository.update(b);
        }
    }

    /**
     * Đảo active. Trả trạng thái mới.
     */
    public boolean toggle(int id) {
        Banner b = require(id);
        bannerRepository.setActive(id, !b.isActive());
        return !b.isActive();
    }

    /**
     * Bật hoặc tắt theo công tắc trong danh sách.
     */
    public void setActive(int id, boolean active) {
        require(id);
        bannerRepository.setActive(id, active);
    }

    /**
     * Banner theo id, null nếu không có.
     */
    public Banner findById(int id) {
        return bannerRepository.findById(id);
    }

    /**
     * Xóa hẳn.
     */
    public void delete(int id) {
        require(id);
        bannerRepository.delete(id);
    }

    private Banner require(int id) {
        Banner b = bannerRepository.findById(id);
        if (b == null) {
            throw new ValidationException(null, "Banner không tồn tại.");
        }
        return b;
    }
}
