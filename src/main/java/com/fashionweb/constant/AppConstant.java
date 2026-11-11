package com.fashionweb.constant;

public final class AppConstant {

    private AppConstant() {
    }

    /** Phí ship cố định (VNĐ). Bằng 0 khi đơn dùng voucher hợp lệ. */
    public static final long SHIPPING_FEE = 30000;

    /** Số sản phẩm mỗi trang ở trang cửa hàng. */
    public static final int PAGE_SIZE = 12;

    /** Số dòng mỗi trang ở các bảng của Admin. */
    public static final int ADMIN_PAGE_SIZE = 10;

    /** Số đơn mỗi trang ở trang "Đơn hàng của tôi". */
    public static final int ORDER_PAGE_SIZE = 5;

    /** Tên thuộc tính Session lưu người dùng đăng nhập (đối tượng User). */
    public static final String SESSION_USER = "currentUser";

    /** Tên thuộc tính Session lưu số loại sản phẩm trong giỏ để hiện trên menu. */
    public static final String SESSION_CART_COUNT = "cartCount";

    /** Giỏ của khách chưa đăng nhập: Map&lt;variantId, số lượng&gt; giữ thứ tự thêm. Đăng nhập thì gộp vào giỏ trong DB. */
    public static final String SESSION_GUEST_CART = "guestCart";

    /** Ghi chú đơn hàng khách nhập ở khung giỏ hàng, điền sẵn vào ô ghi chú ở trang thanh toán. */
    public static final String SESSION_CART_NOTE = "cartNote";

    /** Mã đơn khách chưa đăng nhập vừa đặt trong phiên này, để mở được trang đặt hàng thành công. */
    public static final String SESSION_GUEST_ORDER = "guestOrderCode";

    /** Email đang chờ nhập mã ở luồng khách (#74). Lưu trong session, không đặt lên URL. */
    public static final String SESSION_LOGIN_EMAIL = "loginEmail";

    /** Trang cần quay lại sau khi khách nhập đúng mã. */
    public static final String SESSION_LOGIN_REDIRECT = "loginRedirect";

    /** Email đang chờ nhập mã ở luồng admin. */
    public static final String SESSION_ADMIN_LOGIN_EMAIL = "adminLoginEmail";

    /** Trang admin cần quay lại sau khi nhập đúng mã. */
    public static final String SESSION_ADMIN_LOGIN_REDIRECT = "adminLoginRedirect";

    /** Mã vừa tạo, chỉ lưu khi auth.devCode bật để hiện trên trang nhập mã. */
    public static final String SESSION_DEV_CODE = "devLoginCode";

    /** Phiên của admin hết hạn sau 30 phút không thao tác. */
    public static final int ADMIN_SESSION_SECONDS = 1800;
}
