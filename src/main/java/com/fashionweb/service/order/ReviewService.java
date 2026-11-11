package com.fashionweb.service.order;

import com.fashionweb.repository.order.OrderDetailRepository;
import com.fashionweb.repository.order.ReviewRepository;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Review;
import com.fashionweb.util.Page;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đánh giá sản phẩm và quản lý bình luận.
 *
 * <p>Đã làm: phần của khách (#8, #18). latestVisible, overallStats (#9); search, toggleHidden (#21).
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 */
public class ReviewService {

    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final OrderDetailRepository orderDetailRepository = new OrderDetailRepository();

    /**
     * Được viết (hoặc sửa) đánh giá khi có đơn COMPLETED của chính mình chứa sản phẩm.
     * Đã đánh giá rồi thì form dùng để sửa (xem findMine).
     */
    public boolean canReview(int userId, int productId) {
        return orderDetailRepository.hasPurchased(userId, productId);
    }

    /**
     * Đánh giá của chính user cho sản phẩm (kể cả đang bị ẩn), null nếu chưa có.
     */
    public Review findMine(int userId, int productId) {
        return reviewRepository.findByUserAndProduct(userId, productId);
    }

    /**
     * Thêm hoặc sửa đánh giá của chính user (UNIQUE user_id, product_id): đã có thì sửa, chưa có thì thêm.
     * rating 1 đến 5, nhận xét 10 đến 1000 ký tự. Trả true nếu là sửa.
     */
    public boolean save(Review r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (r.getRating() < 1 || r.getRating() > 5) {
            errors.put("rating", "Hãy chọn từ 1 đến 5 sao.");
        }
        String comment = r.getComment() == null ? "" : r.getComment().trim();
        if (comment.length() < 10 || comment.length() > 1000) {
            errors.put("comment", "Nhận xét cần từ 10 đến 1000 ký tự.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (!canReview(r.getUserId(), r.getProductId())) {
            throw new ValidationException(null, "Bạn chỉ đánh giá được sản phẩm nằm trong đơn đã hoàn thành của mình.");
        }
        r.setComment(comment);
        if (reviewRepository.findByUserAndProduct(r.getUserId(), r.getProductId()) != null) {
            reviewRepository.update(r);
            return true;
        }
        try {
            reviewRepository.insert(r);
            return false;
        } catch (DataAccessException e) {
            // Hai lần gửi cùng lúc: lần sau vướng UNIQUE (user_id, product_id) thì chuyển sang sửa
            if (e.getCause() instanceof SQLIntegrityConstraintViolationException) {
                reviewRepository.update(r);
                return true;
            }
            throw e;
        }
    }

    /**
     * Khách xóa đánh giá của mình (chỉ xóa được dòng có user_id của chính mình).
     */
    public void delete(int userId, int productId) {
        reviewRepository.delete(userId, productId);
    }

    /**
     * Đánh giá chưa bị ẩn, kèm userName, mới nhất trước.
     */
    public List<Review> listVisibleByProduct(int productId) {
        return reviewRepository.findVisibleByProduct(productId);
    }

    /**
     * Mảng 6 phần tử, chỉ số 1 đến 5 là số đánh giá mỗi mức sao (chỉ đánh giá chưa bị ẩn).
     */
    public int[] ratingBreakdown(int productId) {
        int[] breakdown = new int[6];
        for (Review r : reviewRepository.findVisibleByProduct(productId)) {
            breakdown[Math.min(5, Math.max(1, r.getRating()))]++;
        }
        return breakdown;
    }

    /**
     * n đánh giá mới nhất cho trang chủ.
     */
    public List<Review> latestVisible(int n) {
        return reviewRepository.findLatestVisible(n);
    }

    /**
     * {số đánh giá chưa ẩn, điểm trung bình làm tròn 1 chữ số} trên toàn cửa hàng.
     */
    public double[] overallStats() {
        double[] s = reviewRepository.visibleStats();
        return new double[]{s[0], Math.round(s[1] * 10) / 10.0};
    }

    /**
     * Admin: lọc theo sản phẩm và ẩn hoặc hiện (status = visible | hidden).
     */
    public Page<Review> search(Integer productId, String status, int page, int pageSize) {
        Boolean hidden = "hidden".equals(status) ? Boolean.TRUE : "visible".equals(status) ? Boolean.FALSE : null;
        return reviewRepository.findPage(productId, hidden, page, pageSize);
    }

    /**
     * Admin: đảo hidden. Trả trạng thái mới (true = đang ẩn). Bình luận ẩn không hiện ở trang sản phẩm và không tính điểm.
     */
    public boolean toggleHidden(int id) {
        Boolean hidden = reviewRepository.findHidden(id);
        if (hidden == null) {
            throw new ValidationException(null, "Bình luận không tồn tại.");
        }
        reviewRepository.setHidden(id, !hidden);
        return !hidden;
    }
}
