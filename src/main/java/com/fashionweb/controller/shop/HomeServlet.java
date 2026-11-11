package com.fashionweb.controller.shop;

import com.fashionweb.service.order.ReviewService;
import com.fashionweb.service.shop.BannerService;
import com.fashionweb.service.shop.BlogService;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.service.shop.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fashionweb.model.Category;
import com.fashionweb.model.Product;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /home : trang chủ (Guest + Customer).
 * JSP: /WEB-INF/views/shop/home.jsp
 * Thuộc tính: banners (đang bật, theo sort_order), newProducts (created_at giảm dần), featuredProducts (is_featured = 1),
 * bestSellers (tổng số lượng trong đơn không bị hủy), categories, tileGroups (khối danh mục 2 nhóm x 3 ô như a-dam),
 * latestReviews, reviewAvg, reviewTotal, latestBlogs,
 * splitImage1, splitImage2. Mục nào rỗng thì JSP ẩn mục đó.
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private static final int CAROUSEL_SIZE = 10;
    private static final int REVIEW_COUNT = 3;
    private static final int BLOG_COUNT = 3;

    private final ProductService productService = new ProductService();
    private final BannerService bannerService = new BannerService();
    private final BlogService blogService = new BlogService();
    private final ReviewService reviewService = new ReviewService();
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        double[] stats = reviewService.overallStats();

        req.setAttribute("banners", bannerService.findActive());
        req.setAttribute("newProducts", productService.newest(CAROUSEL_SIZE));
        req.setAttribute("featuredProducts", productService.featured(CAROUSEL_SIZE));
        req.setAttribute("bestSellers", productService.bestSellers(CAROUSEL_SIZE));
        req.setAttribute("categories", categoryService.findAll());
        req.setAttribute("tileGroups", tileGroups());
        req.setAttribute("latestReviews", reviewService.latestVisible(REVIEW_COUNT));
        req.setAttribute("reviewAvg", stats[1]);
        req.setAttribute("reviewTotal", (int) stats[0]);
        req.setAttribute("latestBlogs", blogService.latestPublished(BLOG_COUNT));
        req.setAttribute("splitImage1", "assets/images/banners/story-01.jpg");
        req.setAttribute("splitImage2", "assets/images/banners/hero-02.jpg");
        req.getRequestDispatcher("/WEB-INF/views/shop/home.jsp").forward(req, resp);
    }

    /** Danh mục "Quần" được tách thành các ô con theo từ khóa trong tên sản phẩm (nhãn hiển thị, từ khóa tìm). */
    private static final String PANTS_SLUG = "quan";
    private static final String[][] PANTS_TILES = {{"Short", "short"}, {"Jogger", "jogger"}, {"Linen", "linen"}};

    /**
     * Khối danh mục kiểu "Underwear / Socks" của a-dam: mỗi nhóm có tiêu đề và các ô vuông (name, image, href).
     * Nhóm "Áo": mọi danh mục trừ Quần. Nhóm "Quần": ô con Short, Jogger, Linen, mỗi ô lọc danh mục Quần theo từ khóa và
     * lấy ảnh sản phẩm mới nhất khớp; từ khóa nào không có sản phẩm thì bỏ ô đó; không ô nào khớp thì dùng chính danh mục Quần.
     */
    private List<Map<String, Object>> tileGroups() {
        List<Map<String, Object>> groups = new ArrayList<>();
        List<Map<String, String>> tops = new ArrayList<>();
        Category pants = null;
        for (Category c : categoryService.findAll()) {
            if (PANTS_SLUG.equals(c.getSlug())) {
                pants = c;
            } else {
                tops.add(tile(c.getName(), c.getImage(), "/products?category=" + enc(c.getSlug())));
            }
        }
        addGroup(groups, "Áo", tops);
        if (pants != null) {
            List<Map<String, String>> bottoms = new ArrayList<>();
            for (String[] t : PANTS_TILES) {
                List<Product> found = productService.search(t[1], PANTS_SLUG, null, null, null, null, "newest", 1, 1).getItems();
                if (!found.isEmpty()) {
                    bottoms.add(tile(t[0], found.get(0).getImage(), "/products?category=" + PANTS_SLUG + "&q=" + enc(t[1])));
                }
            }
            if (bottoms.isEmpty()) {
                bottoms.add(tile(pants.getName(), pants.getImage(), "/products?category=" + PANTS_SLUG));
            }
            addGroup(groups, pants.getName(), bottoms);
        }
        return groups;
    }

    private static void addGroup(List<Map<String, Object>> groups, String title, List<Map<String, String>> tiles) {
        if (!tiles.isEmpty()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("title", title);
            g.put("tiles", tiles);
            groups.add(g);
        }
    }

    private static Map<String, String> tile(String name, String image, String href) {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("name", name);
        t.put("image", image);
        t.put("href", href);
        return t;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
