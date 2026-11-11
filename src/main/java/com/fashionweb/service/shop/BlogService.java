package com.fashionweb.service.shop;

import com.fashionweb.repository.shop.BlogRepository;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Blog;
import com.fashionweb.util.Page;
import com.fashionweb.util.SlugUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bài viết blog.
 *
 * <p>Khách chỉ thấy bài đang hiển thị (is_published = 1); Admin thấy tất cả.
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 */
public class BlogService {

    private final BlogRepository blogRepository = new BlogRepository();

    /**
     * Bài đã xuất bản, mới nhất trước.
     */
    public List<Blog> listPublished() {
        return blogRepository.findPublished(0);
    }

    /**
     * n bài đã xuất bản mới nhất (trang chủ).
     */
    public List<Blog> latestPublished(int n) {
        return blogRepository.findPublished(n);
    }

    /**
     * Bài đã xuất bản theo slug (kèm authorName). null nếu không có hoặc đang ẩn.
     */
    public Blog findPublishedBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        Blog b = blogRepository.findBySlug(slug.trim());
        return b != null && b.isPublished() ? b : null;
    }

    /**
     * n bài đã xuất bản khác bài excludeId, mới nhất trước (khối gợi ý).
     */
    public List<Blog> listOthers(int excludeId, int n) {
        return blogRepository.findPublishedExcept(excludeId, n);
    }

    /**
     * Danh sách cho Admin (cả bài đang ẩn), tìm theo tiêu đề, mới nhất trước.
     */
    public Page<Blog> search(String q, int page, int pageSize) {
        return blogRepository.findPage(q, page, pageSize);
    }

    /**
     * Bài theo id (kể cả chưa xuất bản). null nếu không có.
     */
    public Blog findById(int id) {
        return blogRepository.findById(id);
    }

    /**
     * id = 0 thêm, id &gt; 0 sửa (giữ tác giả gốc). Slug để trống thì tự sinh từ tiêu đề (không dấu), phải duy nhất.
     * Tiêu đề 5 đến 200 ký tự, tóm tắt tối đa 500, nội dung tối thiểu 20 ký tự.
     */
    public void save(Blog b) {
        Map<String, String> errors = new LinkedHashMap<>();
        String title = b.getTitle() == null ? "" : b.getTitle().trim();
        b.setTitle(title);
        if (title.length() < 5 || title.length() > 200) {
            errors.put("title", "Tiêu đề cần từ 5 đến 200 ký tự.");
        }
        String slug = b.getSlug() == null || b.getSlug().isBlank() ? SlugUtil.toSlug(title) : b.getSlug().trim();
        b.setSlug(slug);
        if (!SlugUtil.isValid(slug) || slug.length() > 220) {
            errors.put("slug", "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang.");
        } else if (blogRepository.existsSlug(slug, b.getId())) {
            errors.put("slug", "Slug đã được dùng cho bài viết khác.");
        }
        String summary = b.getSummary() == null ? "" : b.getSummary().trim();
        b.setSummary(summary.isEmpty() ? null : summary);
        if (summary.length() > 500) {
            errors.put("summary", "Tóm tắt tối đa 500 ký tự.");
        }
        String content = b.getContent() == null ? "" : b.getContent().trim();
        b.setContent(content);
        if (content.length() < 20) {
            errors.put("content", "Nội dung cần ít nhất 20 ký tự.");
        }
        if (b.getId() > 0 && blogRepository.findById(b.getId()) == null) {
            errors.put("title", "Bài viết không tồn tại.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (b.getId() == 0) {
            b.setId(blogRepository.insert(b));
        } else {
            blogRepository.update(b);
        }
    }

    /**
     * Đảo trạng thái đăng/ẩn. Trả trạng thái mới.
     */
    public boolean togglePublish(int id) {
        Blog b = blogRepository.findById(id);
        if (b == null) {
            throw new ValidationException(null, "Bài viết không tồn tại.");
        }
        blogRepository.setPublished(id, !b.isPublished());
        return !b.isPublished();
    }

    /**
     * Xóa bài.
     */
    public void delete(int id) {
        if (blogRepository.findById(id) == null) {
            throw new ValidationException(null, "Bài viết không tồn tại.");
        }
        blogRepository.delete(id);
    }
}
