package com.fashionweb.repository.shop;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.model.Blog;
import com.fashionweb.util.DBContext;
import com.fashionweb.util.Page;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn bảng blogs.
 *
 * <p>Chỉ chứa SQL (PreparedStatement) và chuyển ResultSet thành entity. Kết nối lấy từ DBContext.getConnection().
 */
public class BlogRepository {

    private static final String SELECT = "SELECT b.*, u.full_name AS author_name FROM blogs b JOIN users u ON u.id = b.author_id ";

    /**
     * SELECT published = 1 ORDER BY created_at DESC, JOIN users lấy authorName. limit &lt;= 0 = tất cả.
     */
    public List<Blog> findPublished(int limit) {
        return limit > 0
                ? query(SELECT + "WHERE b.is_published = 1 ORDER BY b.created_at DESC, b.id DESC LIMIT ?", limit)
                : query(SELECT + "WHERE b.is_published = 1 ORDER BY b.created_at DESC, b.id DESC");
    }

    /**
     * n bài đã xuất bản khác bài excludeId, mới nhất trước.
     */
    public List<Blog> findPublishedExcept(int excludeId, int n) {
        return query(SELECT + "WHERE b.is_published = 1 AND b.id <> ? ORDER BY b.created_at DESC, b.id DESC LIMIT ?", excludeId, n);
    }

    /**
     * SELECT theo slug (kể cả chưa xuất bản; Service quyết định có hiện hay không).
     */
    public Blog findBySlug(String slug) {
        List<Blog> list = query(SELECT + "WHERE b.slug = ?", slug);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * SELECT theo id.
     */
    public Blog findById(int id) {
        List<Blog> list = query(SELECT + "WHERE b.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Slug đã có ở bài khác chưa.
     */
    public boolean existsSlug(String slug, int excludeId) {
        return !query(SELECT + "WHERE b.slug = ? AND b.id <> ?", slug, excludeId).isEmpty();
    }

    /**
     * SELECT phân trang, tìm theo tiêu đề, mới nhất trước. Trang vượt quá trang cuối thì lấy trang cuối.
     */
    public Page<Blog> findPage(String q, int page, int pageSize) {
        String where = "";
        List<Object> params = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            where = "WHERE b.title LIKE ? ESCAPE '!' ";
            params.add("%" + q.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        long total;
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM blogs b " + where)) {
            bind(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đếm bài viết", e);
        }
        int totalPages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int safePage = Math.min(Math.max(1, page), totalPages);
        params.add(pageSize);
        params.add((safePage - 1) * pageSize);
        List<Blog> items = query(SELECT + where + "ORDER BY b.created_at DESC, b.id DESC LIMIT ? OFFSET ?", params.toArray());
        return new Page<>(items, safePage, pageSize, total);
    }

    /**
     * INSERT, trả id mới.
     */
    public int insert(Blog b) {
        String sql = "INSERT INTO blogs (author_id, title, slug, summary, content, cover_image, is_published) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, b.getAuthorId(), b.getTitle(), b.getSlug(), b.getSummary(), b.getContent(), b.getCoverImage(), b.isPublished());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi thêm bài viết", e);
        }
    }

    /**
     * UPDATE title, slug, summary, content, cover_image, is_published (giữ tác giả gốc).
     */
    public void update(Blog b) {
        execute("UPDATE blogs SET title = ?, slug = ?, summary = ?, content = ?, cover_image = ?, is_published = ? WHERE id = ?",
                b.getTitle(), b.getSlug(), b.getSummary(), b.getContent(), b.getCoverImage(), b.isPublished(), b.getId());
    }

    /**
     * UPDATE published.
     */
    public void setPublished(int id, boolean published) {
        execute("UPDATE blogs SET is_published = ? WHERE id = ?", published, id);
    }

    /**
     * DELETE.
     */
    public void delete(int id) {
        execute("DELETE FROM blogs WHERE id = ?", id);
    }

    // ----- Hàm phụ -----

    private List<Blog> query(String sql, Object... params) {
        List<Blog> list = new ArrayList<>();
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi đọc bài viết", e);
        }
        return list;
    }

    private void execute(String sql, Object... params) {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi cập nhật bài viết", e);
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static Blog map(ResultSet rs) throws SQLException {
        Blog b = new Blog();
        b.setId(rs.getInt("id"));
        b.setAuthorId(rs.getInt("author_id"));
        b.setTitle(rs.getString("title"));
        b.setSlug(rs.getString("slug"));
        b.setSummary(rs.getString("summary"));
        b.setContent(rs.getString("content"));
        b.setCoverImage(rs.getString("cover_image"));
        b.setPublished(rs.getBoolean("is_published"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setUpdatedAt(rs.getTimestamp("updated_at"));
        b.setAuthorName(rs.getString("author_name"));
        return b;
    }
}
