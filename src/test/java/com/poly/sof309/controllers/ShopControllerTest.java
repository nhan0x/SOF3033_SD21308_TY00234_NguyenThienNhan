package com.poly.sof309.controllers;

import com.fashionweb.controller.shop.ProductDetailServlet;
import com.fashionweb.controller.shop.ProductListServlet;
import com.fashionweb.controller.shop.QuickSearchServlet;
import com.fashionweb.controller.account.WishlistServlet;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Category;
import com.fashionweb.model.Color;
import com.fashionweb.model.Review;
import com.fashionweb.model.Size;
import com.fashionweb.service.account.AccountService;
import com.fashionweb.service.order.ReviewService;
import com.fashionweb.service.shop.AttributeService;
import com.fashionweb.service.shop.CategoryService;
import com.fashionweb.service.shop.ProductService;
import com.fashionweb.util.Page;
import com.poly.sof309.controllers.support.Harness;
import com.poly.sof309.controllers.support.ServletFixture;
import com.poly.sof309.controllers.support.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.poly.sof309.controllers.support.ServletFixture.CTX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Controller cửa hàng: TC_SHOP_02..06, TC_REVIEW_01..03 ở tầng Controller. Service giả, không cần database. */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class ShopControllerTest {

    private Harness<?> harness;

    @AfterEach
    void closeHarness() {
        if (harness != null) {
            harness.close();
        }
    }

    // =====================================================================
    // /products: tham số sai bị bỏ qua, không báo lỗi (TC_SHOP_02)
    // =====================================================================

    private Harness<ProductListServlet> listHarness() {
        Harness<ProductListServlet> h = Harness.mocked(ProductListServlet::new,
                ProductService.class, CategoryService.class, AttributeService.class);
        harness = h;
        Color black = new Color();
        black.setCode("den");
        black.setName("Đen");
        Size m = new Size();
        m.setName("M");
        when(h.service(AttributeService.class).findColors()).thenReturn(List.of(black));
        when(h.service(AttributeService.class).findSizes()).thenReturn(List.of(m));
        when(h.service(ProductService.class).search(any(), any(), any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(new Page<>(List.of(), 1, 12, 100));
        return h;
    }

    @ParameterizedTest(name = "TC_SHOP_02 minPrice=[{0}] maxPrice=[{1}] -> {2}..{3}")
    @DisplayName("TC_SHOP_02: giá âm hoặc không phải số bị bỏ; minPrice > maxPrice thì tự đổi chỗ")
    @CsvSource(value = {
            "-5, abc, , ",
            "500000, 300000, 300000, 500000",
            "0, 100, 0, 100",
            "100, '', 100, ",
            "' 250000 ', 1000000, 250000, 1000000",
            "9999999999999999999, '', , "
    })
    void priceParametersAreSanitised(String min, String max, Long expectedMin, Long expectedMax) throws Exception {
        Harness<ProductListServlet> h = listHarness();
        ServletFixture f = ServletFixture.get("/products").param("minPrice", min).param("maxPrice", max);

        f.run(h.servlet);

        verify(h.service(ProductService.class)).search(isNull(), isNull(), eq(expectedMin), eq(expectedMax), isNull(), isNull(),
                eq("newest"), eq(1), eq(12));
        assertEquals("/WEB-INF/views/shop/product-list.jsp", f.lastForward());
        assertNull(f.errorStatus, "Tham số sai không được gây lỗi");
    }

    @ParameterizedTest(name = "TC_SHOP_02 sort=[{0}] -> {1}")
    @DisplayName("TC_SHOP_02: sort ngoài danh sách (kể cả sai hoa thường) dùng mặc định newest")
    @CsvSource(value = {
            "price_asc, price_asc", "price_desc, price_desc", "bestseller, bestseller", "rating, rating", "newest, newest",
            "xxx, newest", "PRICE_ASC, newest", "'', newest", "'; DROP TABLE products', newest"
    })
    void sortIsWhitelisted(String sort, String expected) throws Exception {
        Harness<ProductListServlet> h = listHarness();
        ServletFixture f = ServletFixture.get("/products").param("sort", sort);

        f.run(h.servlet);

        verify(h.service(ProductService.class)).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(expected), eq(1), eq(12));
    }

    @ParameterizedTest(name = "TC_SHOP_02 page=[{0}] -> trang {1}")
    @DisplayName("TC_SHOP_02: page không phải số, 0 hoặc âm thì về trang 1")
    @CsvSource(value = {"abc, 1", "0, 1", "-4, 1", "'', 1", "3, 3", "7, 7"})
    void pageParameter(String page, int expected) throws Exception {
        Harness<ProductListServlet> h = listHarness();
        ServletFixture f = ServletFixture.get("/products").param("page", page);

        f.run(h.servlet);

        verify(h.service(ProductService.class)).search(any(), any(), any(), any(), any(), any(), anyString(), eq(expected), eq(12));
    }

    @Test
    @DisplayName("TC_SHOP_02: page vượt trang cuối thì truy vấn lại ở trang cuối")
    void pageBeyondLastIsClamped() throws Exception {
        Harness<ProductListServlet> h = listHarness();
        ProductService products = h.service(ProductService.class);
        when(products.search(any(), any(), any(), any(), any(), any(), anyString(), eq(999), anyInt()))
                .thenReturn(new Page<>(List.of(), 999, 12, 24)); // 24 sản phẩm = 2 trang
        when(products.search(any(), any(), any(), any(), any(), any(), anyString(), eq(2), anyInt()))
                .thenReturn(new Page<>(List.of(), 2, 12, 24));
        ServletFixture f = ServletFixture.get("/products").param("page", "999");

        f.run(h.servlet);

        verify(products).search(any(), any(), any(), any(), any(), any(), anyString(), eq(999), eq(12));
        verify(products).search(any(), any(), any(), any(), any(), any(), anyString(), eq(2), eq(12));
        assertEquals(2, ((Page<?>) f.requestAttr("productPage")).getPage());
    }

    @Test
    @DisplayName("TC_SHOP_02: danh mục, màu, size không tồn tại bị bỏ; giá trị có thật được giữ (size đổi sang chữ HOA)")
    void unknownFiltersAreDropped() throws Exception {
        Harness<ProductListServlet> h = listHarness();
        ProductService products = h.service(ProductService.class);
        ServletFixture unknown = ServletFixture.get("/products").param("category", "khongco").param("color", "xxx").param("size", "zz");
        ServletFixture known = ServletFixture.get("/products").param("category", "quan").param("color", "den").param("size", "m");
        Category pants = new Category();
        pants.setName("Quần");
        pants.setSlug("quan");
        when(h.service(CategoryService.class).findBySlug("quan")).thenReturn(pants);

        unknown.run(h.servlet);
        known.run(h.servlet);

        verify(products).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("newest"), eq(1), eq(12));
        verify(products).search(isNull(), eq("quan"), isNull(), isNull(), eq("den"), eq("M"), eq("newest"), eq(1), eq(12));
    }

    @Test
    @DisplayName("TC_SHOP_03: từ khóa dài hơn 100 ký tự bị cắt, khoảng trắng hai đầu bị bỏ")
    void searchQueryIsTrimmedAndCut() throws Exception {
        Harness<ProductListServlet> h = listHarness();
        ServletFixture f = ServletFixture.get("/products").param("q", "  " + "x".repeat(150) + "  ");

        f.run(h.servlet);

        verify(h.service(ProductService.class)).search(eq("x".repeat(100)), any(), any(), any(), any(), any(), anyString(), anyInt(), anyInt());
        assertEquals("x".repeat(100), f.requestAttr("q"));
    }

    // =====================================================================
    // /search/quick (TC_SHOP_03, TC_SHOP_06)
    // =====================================================================

    private Harness<QuickSearchServlet> quickHarness() {
        Harness<QuickSearchServlet> h = Harness.mocked(QuickSearchServlet::new, ProductService.class, CategoryService.class);
        harness = h;
        when(h.service(ProductService.class).search(any(), any(), any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(new Page<>(List.of(), 1, 8, 0));
        return h;
    }

    @ParameterizedTest(name = "TC_SHOP_03 q=[{0}] (dưới 2 ký tự) không truy vấn CSDL")
    @DisplayName("TC_SHOP_03: từ khóa dưới 2 ký tự trả fragment rỗng (200), không gọi Service")
    @CsvSource(value = {"ABSENT", "''", "'   '", "a", "' a '"})
    void shortQueryDoesNotHitDatabase(String q) throws Exception {
        Harness<QuickSearchServlet> h = quickHarness();
        ServletFixture f = ServletFixture.get("/search/quick");
        if (!"ABSENT".equals(q)) {
            f.param("q", q);
        }

        f.run(h.servlet);

        assertEquals("/WEB-INF/views/shop/_quick-search.jsp", f.lastForward());
        assertEquals("", f.requestAttr("q"));
        verifyNoInteractions(h.service(ProductService.class), h.service(CategoryService.class));
        verify(f.resp).setHeader("Cache-Control", "no-store");
    }

    @Test
    @DisplayName("TC_SHOP_03: từ khóa 2 ký tự có gợi ý; phần khớp được tách để JSP bọc <mark>, tên không bị chèn HTML")
    void twoCharacterQuerySearches() throws Exception {
        Harness<QuickSearchServlet> h = quickHarness();
        when(h.service(ProductService.class).suggestNames("ao", 4)).thenReturn(List.of("Áo polo", "<script>alert(1)</script>"));
        ServletFixture f = ServletFixture.get("/search/quick").param("q", "ao");

        f.run(h.servlet);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> suggestions = (List<Map<String, String>>) f.requestAttr("suggestions");
        assertEquals("", suggestions.get(0).get("prefix"));
        assertEquals("Áo", suggestions.get(0).get("match"));
        assertEquals(" polo", suggestions.get(0).get("suffix"));
        assertEquals("<script>alert(1)</script>", suggestions.get(1).get("name"), "Tên giữ nguyên, JSP xuất bằng c:out nên không chạy script");
        verify(h.service(ProductService.class)).search(eq("ao"), isNull(), isNull(), isNull(), isNull(), isNull(), eq("newest"), eq(1), eq(8));
    }

    @Test
    @DisplayName("TC_SHOP_06: từ khóa dài 101 ký tự bị cắt còn 100")
    void quickSearchCutsAt100() throws Exception {
        Harness<QuickSearchServlet> h = quickHarness();
        ServletFixture f = ServletFixture.get("/search/quick").param("q", "x".repeat(101));

        f.run(h.servlet);

        verify(h.service(ProductService.class)).suggestNames("x".repeat(100), 4);
    }

    // =====================================================================
    // /product: chi tiết và đánh giá (TC_SHOP_04, TC_REVIEW_01..03)
    // =====================================================================

    private Harness<ProductDetailServlet> detailHarness() {
        Harness<ProductDetailServlet> h = Harness.mocked(ProductDetailServlet::new,
                ProductService.class, ReviewService.class, AccountService.class);
        harness = h;
        return h;
    }

    @ParameterizedTest(name = "TC_SHOP_04 slug=[{0}] -> 404")
    @DisplayName("TC_SHOP_04: thiếu slug, slug không tồn tại hoặc sản phẩm đã xóa mềm thì trả 404")
    @CsvSource(value = {"ABSENT", "khongco", "ao-polo"})
    void unknownProductIs404(String slug) throws Exception {
        Harness<ProductDetailServlet> h = detailHarness(); // findBySlug trả null cho mọi slug
        ServletFixture f = ServletFixture.get("/product");
        if (!"ABSENT".equals(slug)) {
            f.param("slug", slug);
        }

        f.run(h.servlet);

        assertEquals(404, f.errorStatus);
        assertNull(f.lastForward());
    }

    @Test
    @DisplayName("TC_REVIEW_01: chưa đăng nhập gửi đánh giá thì chuyển sang /login và nhớ trang sản phẩm")
    void reviewRequiresLogin() throws Exception {
        Harness<ProductDetailServlet> h = detailHarness();
        ServletFixture f = ServletFixture.post("/product").param("action", "review").param("slug", "ao-polo").noSession();

        f.run(h.servlet);

        assertEquals(CTX + "/login?redirect=%2Fproduct%3Fslug%3Dao-polo", f.redirectedTo);
        verifyNoInteractions(h.service(ReviewService.class));
    }

    @Test
    @DisplayName("TC_REVIEW_01: chưa mua hàng thì Service từ chối và Controller hiện đúng thông báo")
    void reviewRejectedWhenNotPurchased() throws Exception {
        Harness<ProductDetailServlet> h = detailHarness();
        doThrow(new ValidationException(null, "Bạn chỉ đánh giá được sản phẩm nằm trong đơn đã hoàn thành của mình."))
                .when(h.service(ReviewService.class)).save(any(Review.class));
        ServletFixture f = ServletFixture.post("/product").param("action", "review").param("slug", "ao-polo")
                .param("productId", "4").param("rating", "5").param("comment", "Áo rất đẹp và vừa vặn")
                .loggedIn(Users.customer(2));

        f.run(h.servlet);

        assertEquals("Bạn chỉ đánh giá được sản phẩm nằm trong đơn đã hoàn thành của mình.", f.flashMessage());
        assertEquals("error", f.flashType());
        assertEquals(CTX + "/product?slug=ao-polo#reviews", f.redirectedTo);
    }

    @ParameterizedTest(name = "TC_REVIEW_03 save trả {0} -> [{1}]")
    @DisplayName("TC_REVIEW_03: gửi lần đầu là thêm, gửi lại là sửa: thông báo khác nhau")
    @CsvSource({"false, 'Cảm ơn bạn đã đánh giá sản phẩm!'", "true, 'Đã cập nhật đánh giá của bạn.'"})
    void reviewCreatedOrUpdated(boolean updated, String message) throws Exception {
        Harness<ProductDetailServlet> h = detailHarness();
        when(h.service(ReviewService.class).save(any(Review.class))).thenReturn(updated);
        ServletFixture f = ServletFixture.post("/product").param("action", "review").param("slug", "ao-polo")
                .param("productId", "4").param("rating", "4").param("comment", "Polo gọn gàng, màu đẹp")
                .loggedIn(Users.customer(2));

        f.run(h.servlet);

        assertEquals(message, f.flashMessage());
        assertEquals("success", f.flashType());
    }

    @Test
    @DisplayName("TC_REVIEW_02: đánh giá luôn gắn với người dùng trong session; rating không phải số thành 0 để Service từ chối")
    void reviewUsesSessionUserAndSafeRating() throws Exception {
        Harness<ProductDetailServlet> h = detailHarness();
        ServletFixture f = ServletFixture.post("/product").param("action", "review").param("slug", "ao-polo")
                .param("productId", "4").param("rating", "abc").param("comment", "Nhận xét đủ dài rồi")
                .param("userId", "99").loggedIn(Users.customer(2));

        f.run(h.servlet);

        ArgumentCaptor<Review> review = ArgumentCaptor.forClass(Review.class);
        verify(h.service(ReviewService.class)).save(review.capture());
        assertEquals(2, review.getValue().getUserId(), "Không nhận userId do client gửi");
        assertEquals(4, review.getValue().getProductId());
        assertEquals(0, review.getValue().getRating());
    }

    @Test
    @DisplayName("TC_REVIEW_03: xóa đánh giá của chính mình")
    void deleteOwnReview() throws Exception {
        Harness<ProductDetailServlet> h = detailHarness();
        ServletFixture f = ServletFixture.post("/product").param("action", "deleteReview").param("slug", "ao-polo")
                .param("productId", "4").loggedIn(Users.customer(2));

        f.run(h.servlet);

        verify(h.service(ReviewService.class)).delete(2, 4);
        assertEquals("Đã xóa đánh giá của bạn.", f.flashMessage());
        assertEquals(CTX + "/product?slug=ao-polo#reviews", f.redirectedTo);
    }

    @ParameterizedTest(name = "action [{0}] trả 400")
    @DisplayName("POST /product với action thiếu hoặc lạ trả 400")
    @CsvSource(value = {"hack", "ABSENT"})
    void productPostUnknownAction(String action) throws Exception {
        Harness<ProductDetailServlet> h = detailHarness();
        ServletFixture f = ServletFixture.post("/product").param("slug", "ao-polo").loggedIn(Users.customer(2));
        if (!"ABSENT".equals(action)) {
            f.param("action", action);
        }

        f.run(h.servlet);

        assertEquals(400, f.errorStatus);
        verify(h.service(ReviewService.class), never()).save(any());
    }

    // =====================================================================
    // /wishlist (TC_SHOP_05)
    // =====================================================================

    private Harness<WishlistServlet> wishlistHarness() {
        Harness<WishlistServlet> h = Harness.mocked(WishlistServlet::new, AccountService.class);
        harness = h;
        return h;
    }

    @ParameterizedTest(name = "TC_SHOP_05 productId=[{0}] action=[{1}] trả 400")
    @DisplayName("TC_SHOP_05: productId không hợp lệ hoặc action thiếu/lạ trả 400, không đổi dữ liệu")
    @CsvSource(value = {"0, add", "-1, add", "abc, add", "'', add", "ABSENT, add", "1, ABSENT", "1, delete", "1, ADD"})
    void invalidWishlistRequest(String productId, String action) throws Exception {
        Harness<WishlistServlet> h = wishlistHarness();
        ServletFixture f = ServletFixture.post("/wishlist").loggedIn(Users.customer(2));
        if (!"ABSENT".equals(productId)) {
            f.param("productId", productId);
        }
        if (!"ABSENT".equals(action)) {
            f.param("action", action);
        }

        f.run(h.servlet);

        assertEquals(400, f.errorStatus);
        verifyNoInteractions(h.service(AccountService.class));
    }

    @ParameterizedTest(name = "TC_SHOP_05 toggle khi đang yêu thích = {0} -> JSON inWishlist={1}")
    @DisplayName("TC_SHOP_05: nút tim (AJAX) đảo trạng thái và trả JSON")
    @CsvSource({"false, true", "true, false"})
    void toggleViaAjax(boolean currentlyIn, boolean expected) throws Exception {
        Harness<WishlistServlet> h = wishlistHarness();
        AccountService account = h.service(AccountService.class);
        when(account.isInWishlist(2, 1)).thenReturn(currentlyIn);
        ServletFixture f = ServletFixture.post("/wishlist").param("action", "toggle").param("productId", "1")
                .header("X-Requested-With", "XMLHttpRequest").loggedIn(Users.customer(2));

        f.run(h.servlet);

        assertEquals("{\"inWishlist\":" + expected + "}", f.responseBody());
        if (expected) {
            verify(account).addWishlist(2, 1);
            verify(account, never()).removeWishlist(2, 1);
        } else {
            verify(account).removeWishlist(2, 1);
            verify(account, never()).addWishlist(2, 1);
        }
        assertNull(f.redirectedTo, "AJAX không redirect");
    }

    @Test
    @DisplayName("TC_SHOP_05: thêm/bỏ bằng form thường thì flash rồi về /wishlist")
    void addViaForm() throws Exception {
        Harness<WishlistServlet> h = wishlistHarness();
        ServletFixture add = ServletFixture.post("/wishlist").param("action", "add").param("productId", "1").loggedIn(Users.customer(2));
        ServletFixture remove = ServletFixture.post("/wishlist").param("action", "remove").param("productId", "1").loggedIn(Users.customer(2));

        add.run(h.servlet);
        remove.run(h.servlet);

        assertEquals("Đã thêm vào yêu thích.", add.flashMessage());
        assertEquals("Đã bỏ khỏi yêu thích.", remove.flashMessage());
        assertEquals(CTX + "/wishlist", add.redirectedTo);
        verify(h.service(AccountService.class), times(1)).addWishlist(2, 1);
        verify(h.service(AccountService.class), times(1)).removeWishlist(2, 1);
        assertTrue(add.responseBody().isEmpty());
    }
}
