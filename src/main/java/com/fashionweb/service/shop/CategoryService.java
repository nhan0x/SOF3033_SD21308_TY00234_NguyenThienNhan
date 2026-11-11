package com.fashionweb.service.shop;

import com.fashionweb.repository.shop.CategoryRepository;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Category;
import com.fashionweb.util.SlugUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh mục sản phẩm.
 *
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 * <p>Quy ước của nhóm: KHÔNG cho xóa mềm danh mục còn sản phẩm đang bán (báo rõ số sản phẩm),
 * để trang cửa hàng không có sản phẩm "mồ côi" danh mục.
 */
public class CategoryService {

    private final CategoryRepository categoryRepository = new CategoryRepository();

    /**
     * Danh mục chưa xóa mềm kèm productCount.
     */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    /**
     * Danh mục chưa xóa mềm có tên chứa {@code q}, kèm productCount (cho khung tìm kiếm nhanh).
     */
    public List<Category> searchByName(String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        return categoryRepository.searchByName(q.trim());
    }

    /**
     * Một danh mục chưa xóa mềm theo slug. null nếu không có.
     */
    public Category findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return categoryRepository.findBySlug(slug.trim());
    }

    /**
     * Một danh mục theo id, kể cả đã xóa mềm. null nếu không có.
     */
    public Category findById(int id) {
        return categoryRepository.findById(id);
    }

    /**
     * id = 0 thêm, id > 0 sửa. Slug để trống thì tự sinh từ tên; slug phải đúng định dạng và duy nhất.
     * Sai thì ném ValidationException với lỗi theo ô (name, slug).
     */
    public void save(Category c) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = c.getName() == null ? "" : c.getName().trim();
        String slug = c.getSlug() == null ? "" : c.getSlug().trim().toLowerCase();
        if (slug.isEmpty()) {
            slug = SlugUtil.toSlug(name);
        }

        if (name.length() < 2 || name.length() > 100) {
            errors.put("name", "Tên danh mục từ 2 đến 100 ký tự.");
        }
        if (slug.isEmpty() || slug.length() > 120 || !SlugUtil.isValid(slug)) {
            errors.put("slug", "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang (tối đa 120 ký tự).");
        } else if (categoryRepository.existsSlug(slug, c.getId())) {
            errors.put("slug", "Slug \"" + slug + "\" đã được dùng (có thể ở danh mục trong Thùng rác).");
        }

        Category existing = null;
        if (c.getId() > 0) {
            existing = categoryRepository.findById(c.getId());
            if (existing == null || existing.isDeleted()) {
                errors.put("name", "Danh mục không tồn tại hoặc đã bị xóa.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        c.setName(name);
        c.setSlug(slug);
        if (existing == null) {
            c.setId(categoryRepository.insert(c));
        } else {
            if (c.getImage() == null) {
                c.setImage(existing.getImage()); // không chọn ảnh mới thì giữ ảnh cũ
            }
            categoryRepository.update(c);
        }
    }

    /**
     * Xóa mềm. Chặn khi danh mục còn sản phẩm đang bán.
     */
    public void softDelete(int id) {
        Category c = categoryRepository.findById(id);
        if (c == null || c.isDeleted()) {
            throw new ValidationException(null, "Danh mục không tồn tại hoặc đã nằm trong Thùng rác.");
        }
        if (c.getProductCount() > 0) {
            throw new ValidationException(null, "Không thể xóa \"" + c.getName() + "\" vì còn " + c.getProductCount()
                    + " sản phẩm đang bán. Hãy chuyển hoặc xóa các sản phẩm này trước.");
        }
        categoryRepository.setDeleted(id, true);
    }

    /**
     * Danh mục đã xóa mềm (Thùng rác).
     */
    public List<Category> findDeleted() {
        return categoryRepository.findDeleted();
    }

    /**
     * Khôi phục danh mục đã xóa mềm.
     */
    public void restore(int id) {
        Category c = categoryRepository.findById(id);
        if (c == null || !c.isDeleted()) {
            throw new ValidationException(null, "Danh mục không có trong Thùng rác.");
        }
        categoryRepository.setDeleted(id, false);
    }
}
