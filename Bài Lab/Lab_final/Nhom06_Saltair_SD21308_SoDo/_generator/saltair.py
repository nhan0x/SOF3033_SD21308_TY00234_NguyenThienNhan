# -*- coding: utf-8 -*-
from dg import *
from lab12 import icon, ICON

AR = "endArrow=block;endFill=1;strokeWidth=1.6;fontSize=12;fontStyle=1;labelBackgroundColor=#FFFFFF;"


# ------------------------------------------------------------ USE CASE
def uc_tong_quat():
    d = Dia("Use case tong quat", 1500, 1100)
    boundary(d, 220, 20, 1080, 1060, "Website thương mại điện tử Saltair", 20)
    g = actor(d, 90, 260, "Khách (Guest)", top=True)
    c = actor(d, 90, 780, "Khách hàng\n(Customer)")
    a = actor(d, 1410, 540, "Quản trị viên\n(Admin)")
    guest = [("Xem trang chủ", 110), ("Xem / lọc / tìm kiếm sản phẩm", 195), ("Xem chi tiết sản phẩm", 280),
             ("Giỏ hàng và đặt hàng bằng email", 365), ("Xem blog", 450), ("Đăng ký / Đăng nhập", 530)]
    cust = [("Quản lý giỏ hàng", 640), ("Đặt hàng và thanh toán", 720), ("Theo dõi / hủy đơn hàng", 800),
            ("Đánh giá sản phẩm", 880), ("Danh sách yêu thích", 960), ("Hồ sơ và sổ địa chỉ", 1035)]
    adm = [("Quản lý sản phẩm, biến thể", 110), ("Quản lý danh mục, màu, size", 195), ("Quản lý đơn hàng", 280),
           ("Quản lý mã giảm giá (voucher)", 365), ("Quản lý người dùng", 450), ("Quản lý banner", 535),
           ("Quản lý blog", 620), ("Quản lý bình luận", 705), ("Thống kê doanh thu", 790), ("Thùng rác (khôi phục)", 875)]
    for t, y in guest:
        assoc(d, g, uc(d, arc_x(90, 260, 440, y), y, t, 270, 60))
    for t, y in cust:
        assoc(d, c, uc(d, arc_x(90, 780, 420, y), y, t, 270, 60, "#F2F7FB"))
    inherit(d, c, g)
    for t, y in adm:
        assoc(d, a, uc(d, arc_x(1410, 540, 530, y, -1), y, t, 290, 60, "#FFF7EC"))
    note(d, 690, 960, 520, 100, "<b>Phân quyền</b>: Guest xem sản phẩm, có giỏ hàng tạm và đặt hàng bằng email; Customer kế thừa mọi quyền "
         "của Guest và có khu vực tài khoản; Admin quản trị toàn hệ thống tại /admin (AdminFilter).", 12)
    return d


def uc_customer():
    d = Dia("Use case Customer", 1560, 1000)
    boundary(d, 200, 20, 1330, 960, "Khách hàng (Customer) – website Saltair", 20)
    AX, AY, R, O2, O3 = 90, 500, 520, 380, 760
    c = actor(d, AX, AY, "Khách hàng\n(Customer)")
    X = lambda y: arc_x(AX, AY, R, y)
    dx = uc(d, X(90), 90, "Đăng xuất", 190, 56)
    dn = uc(d, X(90) + O2, 90, "Đăng nhập", 190, 56)
    gh = uc(d, X(190), 190, "Quản lý giỏ hàng", 220, 60)
    th = uc(d, X(190) + O2, 190, "Thêm vào giỏ (màu + size)", 240, 60)
    ck = uc(d, X(190) + O3, 190, "Kiểm tra tồn kho", 190, 56)
    dh = uc(d, X(380), 380, "Đặt hàng", 220, 60)
    ad = uc(d, X(380) + O2, 300, "Chọn địa chỉ giao hàng", 240, 56)
    vc = uc(d, X(380) + O2, 385, "Áp mã giảm giá", 200, 56)
    tt = uc(d, X(380) + O2, 470, "Thanh toán COD / chuyển khoản", 280, 60)
    qr = uc(d, X(380) + O3, 470, "Quét QR payOS", 190, 56)
    dn2 = uc(d, X(620), 620, "Xem lịch sử đơn hàng", 230, 60)
    ct = uc(d, X(620) + O2, 620, "Xem chi tiết đơn", 200, 56)
    hy = uc(d, X(620) + O3, 580, "Hủy đơn (Chờ xác nhận)", 230, 56)
    nh = uc(d, X(620) + O3, 665, "Xác nhận đã nhận hàng", 230, 56)
    dg = uc(d, X(770), 770, "Đánh giá sản phẩm", 220, 60)
    sd = uc(d, X(770) + O2, 770, "Sửa / xóa đánh giá", 210, 56)
    yt = uc(d, X(860), 860, "Danh sách yêu thích", 220, 60)
    hs = uc(d, X(940), 940, "Hồ sơ và sổ địa chỉ", 220, 60)
    for u in (gh, dh, dn2, dg, yt, hs, dx):
        assoc(d, c, u)
    include(d, dx, dn)
    include(d, gh, th)
    include(d, th, ck)
    include(d, dh, ad)
    extend(d, vc, dh)
    include(d, dh, tt)
    extend(d, qr, tt)
    include(d, dn2, ct)
    extend(d, hy, ct)
    extend(d, nh, ct)
    extend(d, sd, dg)
    note(d, 820, 850, 640, 100, "Chỉ đơn <b>Hoàn thành</b> mới được đánh giá. Hủy đơn chỉ khi <b>Chờ xác nhận</b>; "
         "hủy đơn hoàn lại tồn kho và lượt dùng voucher.", 12)
    return d


def uc_admin():
    d = Dia("Use case Admin", 1560, 1320)
    boundary(d, 200, 20, 1330, 1270, "Quản trị viên (Admin) – website Saltair", 20)
    L = [("Quản lý sản phẩm", ["Thêm / sửa sản phẩm + biến thể", "Xóa mềm / khôi phục"]),
         ("Quản lý danh mục", ["Thêm / sửa danh mục", "Xóa mềm / khôi phục"]),
         ("Quản lý màu và size", ["Thêm / sửa / xóa màu, size"]),
         ("Quản lý đơn hàng", ["Tìm kiếm, lọc theo trạng thái", "Cập nhật: Xác nhận → Giao → Hoàn thành", "Hủy đơn (kèm lý do)"]),
         ("Quản lý voucher", ["Thêm / sửa voucher", "Tạm dừng, xem lượt dùng"]),
         ("Quản lý người dùng", ["Khóa / mở khóa tài khoản", "Đổi role"]),
         ("Quản lý banner, blog", ["Thêm / sửa / xóa, ẩn / hiện"]),
         ("Quản lý bình luận", ["Ẩn / hiện bình luận"]),
         ("Thống kê doanh thu", ["Lọc theo ngày, top bán chạy"])]
    AX, AY, R, OFF = 90, 660, 680, 600
    a = actor(d, AX, AY, "Quản trị viên\n(Admin)")
    cur = 100
    for title, subs in L:
        n = len(subs)
        y = cur + n * 64 / 2
        x = arc_x(AX, AY, R, y)
        bx = uc(d, x, y, title, 240, 60, "#FFF7EC")
        assoc(d, a, bx)
        for k, sname in enumerate(subs):
            extend(d, uc(d, x + OFF, cur + 32 + k * 64, sname, 330, 54), bx)
        cur += n * 64 + 26
    return d


# ------------------------------------------------------------ TRIEN KHAI
def deploy():
    d = Dia("Mo hinh trien khai Saltair", 1400, 800)
    d.text(40, 12, 1320, 34, "<b>MÔ HÌNH TRIỂN KHAI</b> – Saltair", size=18)
    cl = icon(d, 70, 130, 120, 90, "mxgraph.office.devices.laptop", "Khách hàng")
    ad = icon(d, 70, 330, 120, 90, "mxgraph.office.devices.laptop", "Quản trị viên")
    boundary(d, 380, 70, 520, 470, "Máy chủ ứng dụng (Apache Tomcat 11 + Java 17)", 15)
    sv = icon(d, 420, 130, 80, 100, "mxgraph.cisco.servers.standard_host", "")
    chip = "rounded=1;arcSize=30;strokeColor=%s;fillColor=#FFFFFF;fontSize=12;" % BLUE
    for i, t in enumerate(("saltair.war  (Servlet 6.1 / JSP 4 / JSTL 3)", "Filter: Encoding · Auth · Admin",
                           "Controller → Service → DAO (JDBC)", "DBMigration (Flyway kiểu V1…V4.sql)")):
        d.box(530, 150 + i * 58, 340, 42, t, chip)
    db = d.box(1040, 130, 110, 100, "MySQL 8<br><font style='font-size:11px'>CSDL saltair (18 bảng)</font>",
               "shape=cylinder3;boundedLbl=1;backgroundOutline=1;size=14;%s" % ICON)
    d.edge(cl, sv, "HTTP / HTTPS", AR + "edgeStyle=orthogonalEdgeStyle;", pts=[(300, 175), (300, 180)])
    d.edge(ad, sv, "", AR + "edgeStyle=orthogonalEdgeStyle;", pts=[(300, 375), (300, 190)])
    d.edge(None, None, "JDBC (TCP 3306)", AR, src_pt=(900, 180), tgt_pt=(1040, 180))
    ext = [("SMTP Gmail", "mxgraph.office.concepts.email", "gửi mail báo đăng nhập", 440),
           ("Cloudinary", "mxgraph.office.concepts.cloud", "lưu ảnh upload", 590),
           ("payOS", "mxgraph.office.concepts.cloud", "thanh toán chuyển khoản + webhook", 740)]
    d.text(980, 330, 380, 28, "<b>Dịch vụ bên ngoài</b>", size=14, color=BLUE)
    for i, (n, sh, sub, _) in enumerate(ext):
        d.box(980, 370 + i * 80, 380, 62, "<b>%s</b><br><font style='font-size:11px'>%s</font>" % (n, sub),
              "rounded=1;arcSize=12;strokeColor=%s;fillColor=#F5FAFD;fontSize=13;" % BLUE)
        d.edge(None, None, "", AR + "dashed=1;", src_pt=(900, 300 + i * 40), tgt_pt=(980, 401 + i * 80))
    note(d, 40, 660, 1320, 100,
         "<b>Luồng:</b> Trình duyệt gửi HTTP Request → Tomcat → Filter (mã hóa UTF-8, kiểm tra đăng nhập, kiểm tra quyền Admin) → Servlet (Controller) → "
         "Service (nghiệp vụ + transaction) → DAO (PreparedStatement) → MySQL; kết quả trả lại Servlet, forward sang JSP (View) và HTTP Response về trình duyệt. "
         "Service gọi thêm SMTP Gmail, Cloudinary, payOS khi cần; payOS gọi ngược lại PaymentWebhookServlet khi tiền về.", 12)
    return d


def arch():
    d = Dia("Kien truc phan tang MVC", 1400, 800)
    d.text(40, 12, 1320, 34, "<b>KIẾN TRÚC PHÂN TẦNG</b> – MVC + Service + DAO (Servlet → Service → DAO → MySQL)", size=18)
    cols = [("Trình duyệt", "Khách / Admin<br>HTML · CSS · JS", "#EDEDED", "#555555"),
            ("Filter", "EncodingFilter<br>AuthFilter · AdminFilter", "#FFF4DE", "#B26B00"),
            ("Controller", "Servlet theo chức năng:<br>auth · account · shop<br>cart · order · admin", "#DCEBFA", BLUE),
            ("Service", "Nghiệp vụ + transaction:<br>AuthService, CartService<br>OrderService, VoucherService<br>PaymentService, ...", "#DCEBFA", BLUE),
            ("DAO", "Toàn bộ SQL<br>(PreparedStatement):<br>UserDAO, ProductDAO<br>OrderDAO, VoucherDAO...", "#DCEBFA", BLUE),
            ("MySQL 8", "18 bảng<br>InnoDB, utf8mb4", "#E3F4E6", "#2E7D32")]
    ids = []
    for i, (t, s, f, k) in enumerate(cols):
        x = 40 + i * 225
        ids.append(d.box(x, 120, 190, 210, "<b>%s</b><br><br><font style='font-size:12px'>%s</font>" % (t, s),
                         "rounded=1;arcSize=6;strokeColor=%s;strokeWidth=2;fillColor=%s;fontSize=15;" % (k, f)))
    for a, b in zip(ids, ids[1:]):
        d.edge(a, b, "", AR)
    view = d.box(515, 420, 190, 90, "<b>View (JSP + JSTL + tag)</b><br><font style='font-size:12px'>/WEB-INF/views/…</font>",
                 "rounded=1;arcSize=8;strokeColor=%s;strokeWidth=2;fillColor=#F3E5F5;fontSize=14;" % "#7B1FA2")
    model = d.box(965, 420, 250, 90, "<b>Model / DTO</b><br><font style='font-size:12px'>16 lớp ánh xạ bảng, constant, exception</font>",
                  "rounded=1;arcSize=8;strokeColor=%s;strokeWidth=2;fillColor=#F3E5F5;fontSize=14;" % "#7B1FA2")
    d.edge(ids[2], view, "forward", AR + "dashed=1;")
    d.edge(ids[3], model, "", AR + "dashed=1;startArrow=block;startFill=1;")
    d.edge(ids[4], model, "", AR + "dashed=1;startArrow=block;startFill=1;")
    util = d.box(40, 560, 1175, 60, "<b>Util dùng chung:</b> DBContext · PasswordUtil · MailUtil · ImageUploadUtil · CloudinaryUtil · PayOSSignature · QrSvg · SlugUtil · Page · DBMigration",
                 "rounded=1;strokeColor=#888;fillColor=#FAFAFA;fontSize=12;")
    note(d, 40, 650, 1175, 100,
         "<b>Quy tắc luồng gọi:</b> Servlet không gọi thẳng DAO và không viết SQL · DAO chỉ chứa SQL, không chứa nghiệp vụ · "
         "JSP chỉ hiển thị, không gọi DAO/Service · Mỗi chức năng nằm trong thư mục cùng tên ở cả ba tầng.", 12)
    return d


# ------------------------------------------------------------ ERD
T = {
 "users": [("PK","id : INT",1),("","email : VARCHAR(150) – UQ",1),("","password_hash : VARCHAR(255)",0),("","full_name : VARCHAR(100)",1),("","phone : VARCHAR(15)",0),("","role : ENUM(CUSTOMER, ADMIN)",1),("","is_active : TINYINT(1)",1)],
 "addresses": [("PK","id : INT",1),("FK","user_id : INT",1),("","receiver_name : VARCHAR(100)",1),("","phone : VARCHAR(15)",1),("","address_line : VARCHAR(255)",1),("","ward, district : VARCHAR(100)",0),("","city : VARCHAR(100)",1),("","is_default : TINYINT(1)",1)],
 "categories": [("PK","id : INT",1),("","name : VARCHAR(100)",1),("","slug : VARCHAR(120) – UQ",1),("","image : VARCHAR(255)",0),("","is_deleted : TINYINT(1)",1)],
 "colors": [("PK","id : INT",1),("","name : VARCHAR(50)",1),("","code : VARCHAR(30) – UQ",1),("","hex_code : CHAR(7)",1),("","is_deleted : TINYINT(1)",1)],
 "sizes": [("PK","id : INT",1),("","name : VARCHAR(20) – UQ",1),("","sort_order : INT",1),("","is_deleted : TINYINT(1)",1)],
 "products": [("PK","id : INT",1),("FK","category_id : INT",1),("","name : VARCHAR(150)",1),("","slug : VARCHAR(170) – UQ",1),("","description : TEXT",0),("","price : DECIMAL(12,0) (> 0)",1),("","image, image_thumb : VARCHAR(255)",0),("","is_featured : TINYINT(1)",1),("","is_deleted : TINYINT(1)",1),("","created_at : DATETIME",1)],
 "product_images": [("PK","id : INT",1),("FK","product_id : INT",1),("","image : VARCHAR(255)",1),("","sort_order : INT",1)],
 "product_variants": [("PK","id : INT",1),("FK","product_id : INT",1),("FK","color_id : INT",1),("FK","size_id : INT",1),("","stock : INT (>= 0)",1)],
 "carts": [("PK","id : INT",1),("FK","user_id : INT – UQ",1),("","updated_at : DATETIME",1)],
 "cart_items": [("PK","id : INT",1),("FK","cart_id : INT",1),("FK","variant_id : INT",1),("","quantity : INT (> 0)",1)],
 "wishlists": [("PK","user_id : INT (FK)",1),("PK","product_id : INT (FK)",1),("","created_at : DATETIME",1)],
 "vouchers": [("PK","id : INT",1),("","code : VARCHAR(30) – UQ",1),("","discount_type : ENUM(PERCENT, AMOUNT)",1),("","discount_value : DECIMAL(12,0)",1),("","max_discount : DECIMAL(12,0)",0),("","min_order_amount : DECIMAL(12,0)",1),("FK","category_id : INT (null = mọi DM)",0),("","start_date, end_date : DATETIME",1),("","usage_limit, used_count : INT",1),("","per_user_limit : INT",1),("","is_active : TINYINT(1)",1)],
 "orders": [("PK","id : INT",1),("","order_code : VARCHAR(20) – UQ",1),("FK","user_id : INT",1),("FK","voucher_id : INT",0),("","receiver_name, receiver_phone",1),("","shipping_address : VARCHAR(300)",1),("","payment_method : ENUM(COD, BANK_TRANSFER)",1),("","payment_status : ENUM(UNPAID, PAID)",1),("","status : ENUM(PENDING, SHIPPING,",1),("","            COMPLETED, CANCELLED)",1),("","subtotal, discount_amount : DECIMAL",1),("","shipping_fee, total_amount : DECIMAL",1),("","voucher_code : VARCHAR(30)",0),("","cancel_reason, cancelled_by",0),("","created_at, completed_at : DATETIME",0)],
 "order_details": [("PK","id : INT",1),("FK","order_id : INT",1),("FK","variant_id : INT",1),("FK","product_id : INT",1),("","product_name, color_name, size_name",1),("","unit_price : DECIMAL(12,0)",1),("","quantity : INT (> 0)",1),("","line_total : DECIMAL(12,0)",1)],
 "reviews": [("PK","id : INT",1),("FK","user_id : INT",1),("FK","product_id : INT",1),("","rating : TINYINT (1..5)",1),("","comment : VARCHAR(1000)",0),("","is_hidden : TINYINT(1)",1)],
 "banners": [("PK","id : INT",1),("","title : VARCHAR(150)",1),("","image : VARCHAR(255)",1),("","link_url : VARCHAR(255)",0),("","sort_order : INT",1),("","is_active : TINYINT(1)",1)],
 "blogs": [("PK","id : INT",1),("FK","author_id : INT",1),("","title : VARCHAR(200)",1),("","slug : VARCHAR(220) – UQ",1),("","summary : VARCHAR(500)",0),("","content : MEDIUMTEXT",1),("","cover_image : VARCHAR(255)",0),("","is_published : TINYINT(1)",1)],
 "payment_transactions": [("PK","id : INT",1),("","provider, provider_tx_id – UQ",1),("FK","order_id : INT",0),("","order_code : VARCHAR(20)",0),("","amount : DECIMAL(14,0)",1),("","result : ENUM(PAID, NO_ORDER, …)",1),("","created_at : DATETIME",1)],
}
POS = {"addresses": (40, 40), "users": (40, 340), "blogs": (40, 610), "payment_transactions": (40, 880),
       "carts": (400, 40), "cart_items": (760, 40), "wishlists": (400, 230), "reviews": (400, 400),
       "products": (760, 250), "orders": (400, 680), "order_details": (1120, 700),
       "product_variants": (1120, 40), "colors": (1480, 40), "sizes": (1480, 210),
       "categories": (1480, 380), "product_images": (760, 600), "vouchers": (1480, 570), "banners": (1840, 570)}
FK = [("addresses","users"),("carts","users"),("wishlists","users"),("wishlists","products"),("reviews","users"),
      ("reviews","products"),("orders","users"),("orders","vouchers"),("blogs","users"),("payment_transactions","orders"),
      ("order_details","orders"),("order_details","product_variants"),("order_details","products"),("cart_items","carts"),
      ("cart_items","product_variants"),("product_variants","products"),("product_variants","colors"),
      ("product_variants","sizes"),("product_images","products"),("products","categories"),("vouchers","categories")]


ERD_POS = {"addresses": (40, 40), "users": (40, 300), "blogs": (40, 540), "payment_transactions": (40, 800),
           "carts": (450, 50), "wishlists": (450, 200), "reviews": (450, 340), "orders": (450, 600), "vouchers": (450, 1010),
           "cart_items": (860, 40), "products": (860, 230), "categories": (860, 540), "order_details": (860, 730),
           "product_variants": (1270, 300), "product_images": (1270, 580), "colors": (1680, 300), "sizes": (1680, 470), "banners": (1680, 700)}
# (con, cha, canh_con, frac, canh_cha, frac, lan_x)  -> duong gap khuc di trong khe giua cac cot, khong xuyen bang
ROUTES = [("addresses","users","bottom",.5,"top",.5,None),("blogs","users","top",.5,"bottom",.5,None),
 ("carts","users","left",.5,"right",.15,360),("wishlists","users","left",.5,"right",.4,385),
 ("reviews","users","left",.5,"right",.65,410),("orders","users","left",.1,"right",.9,432),
 ("payment_transactions","orders","right",.3,"left",.8,380),("vouchers","orders","top",.5,"bottom",.5,None),
 ("wishlists","products","right",.5,"left",.2,770),("reviews","products","right",.5,"left",.7,800),
 ("cart_items","carts","left",.5,"right",.5,790),("cart_items","product_variants","right",.5,"left",.15,1160),
 ("products","product_variants","right",.3,"left",.4,1185),("order_details","product_variants","right",.2,"left",.85,1210),
 ("product_variants","colors","right",.3,"left",.5,1600),("product_variants","sizes","right",.7,"left",.5,1620),
 ("product_images","products","left",.5,"right",.9,1235),("order_details","orders","left",.3,"right",.5,790),
 ("order_details","products","right",.6,"right",.6,1255),("products","categories","bottom",.5,"top",.5,None),
 ("vouchers","categories","right",.1,"left",.5,840)]


def port(node, side, f):
    x, y, w, h = node
    return {"left": (x, y + f * h), "right": (x + w, y + f * h), "top": (x + f * w, y), "bottom": (x + f * w, y + h)}[side]


def route_edges(d, ids, nodes, routes, rename=None):
    st = "endArrow=ERmandOne;startArrow=ERmany;startSize=11;endSize=11;strokeWidth=1.3;strokeColor=#333333;"
    for c, p, s1, f1, s2, f2, lane in routes:
        if rename:
            if c not in rename or p not in rename:
                continue
            c, p = rename[c], rename[p]
        sx = {"left": 0, "right": 1}.get(s1, f1); sy = {"top": 0, "bottom": 1}.get(s1, f1)
        tx = {"left": 0, "right": 1}.get(s2, f2); ty = {"top": 0, "bottom": 1}.get(s2, f2)
        pts = None
        if lane is not None:
            p1 = port(nodes[c], s1, f1); p2 = port(nodes[p], s2, f2)
            pts = [(lane, p1[1]), (lane, p2[1])]
        d.edge(ids[c], ids[p], "", st + "exitX=%s;exitY=%s;entryX=%s;entryY=%s;exitPerimeter=0;entryPerimeter=0;" % (sx, sy, tx, ty), pts=pts)


def erd():
    d = Dia("ERD Saltair", 2000, 1320)
    d.text(40, 4, 1900, 30, "<b>SƠ ĐỒ QUAN HỆ THỰC THỂ (ERD)</b> – CSDL Saltair (MySQL 8, 18 bảng)", size=18)
    ids, nodes = {}, {}
    for n, rows in T.items():
        x, y = ERD_POS[n]
        ids[n], h = table(d, x, y, n, rows, 270, 21)
        nodes[n] = (x, y, 270, h)
    route_edges(d, ids, nodes, ROUTES)
    note(d, 1680, 900, 300, 150, "<b>Chú thích</b><br>PK: khóa chính (gạch chân)<br>FK: khóa ngoại<br><b>Chữ đậm</b>: NOT NULL · UQ: UNIQUE<br>Đầu chân chim = phía «N»", 12)
    return d


# ------------------------------------------------------------ CLASS
CLS = {
 "User": ["id: int", "email: String", "fullName: String", "phone: String", "role: Role", "active: boolean"],
 "Address": ["id: int", "userId: int", "receiverName: String", "phone: String", "addressLine: String", "city: String", "isDefault: boolean"],
 "Category": ["id: int", "name: String", "slug: String", "image: String", "deleted: boolean"],
 "Product": ["id: int", "categoryId: int", "name: String", "slug: String", "price: long", "image: String", "featured: boolean", "deleted: boolean"],
 "ProductVariant": ["id: int", "productId: int", "colorId: int", "sizeId: int", "stock: int"],
 "Color": ["id: int", "name: String", "code: String", "hexCode: String"],
 "Size": ["id: int", "name: String", "sortOrder: int"],
 "Cart": ["id: int", "userId: int", "updatedAt: Date"],
 "CartItem": ["id: int", "cartId: int", "variantId: int", "quantity: int"],
 "Wishlist": ["userId: int", "productId: int", "createdAt: Date"],
 "Order": ["id: int", "orderCode: String", "userId: int", "status: OrderStatus", "paymentMethod: PaymentMethod", "paymentStatus: PaymentStatus", "subtotal: long", "discountAmount: long", "shippingFee: long", "totalAmount: long"],
 "OrderDetail": ["id: int", "orderId: int", "variantId: int", "productName: String", "unitPrice: long", "quantity: int", "lineTotal: long"],
 "Voucher": ["id: int", "code: String", "discountType: DiscountType", "discountValue: long", "minOrderAmount: long", "usageLimit: int", "usedCount: int", "active: boolean"],
 "Review": ["id: int", "userId: int", "productId: int", "rating: int", "comment: String", "hidden: boolean"],
 "Banner": ["id: int", "title: String", "image: String", "sortOrder: int", "active: boolean"],
 "Blog": ["id: int", "authorId: int", "title: String", "slug: String", "published: boolean"],
}
CPOS = {"Address": (40, 40), "User": (40, 330), "Blog": (40, 640), "Cart": (380, 40), "CartItem": (760, 40),
        "Wishlist": (380, 240), "Review": (380, 400), "Order": (380, 640), "OrderDetail": (1140, 700),
        "Product": (760, 250), "ProductVariant": (1140, 40), "Color": (1480, 40), "Size": (1480, 210),
        "Category": (1480, 380), "Voucher": (1480, 570), "Banner": (1840, 570)}
CREL = [("Address","User","*","1"),("Cart","User","1","1"),("Wishlist","User","*","1"),("Wishlist","Product","*","1"),
        ("Review","User","*","1"),("Review","Product","*","1"),("Order","User","*","1"),("Order","Voucher","*","0..1"),
        ("Blog","User","*","1"),("OrderDetail","Order","*","1"),("OrderDetail","ProductVariant","*","1"),
        ("CartItem","Cart","*","1"),("CartItem","ProductVariant","*","1"),("ProductVariant","Product","*","1"),
        ("ProductVariant","Color","*","1"),("ProductVariant","Size","*","1"),("Product","Category","*","1"),("Voucher","Category","*","0..1")]


CLS_POS = {"Address": (40, 40), "User": (40, 300), "Blog": (40, 540),
           "Cart": (450, 50), "Wishlist": (450, 200), "Review": (450, 340), "Order": (450, 600), "Voucher": (450, 900),
           "CartItem": (860, 40), "Product": (860, 230), "Category": (860, 500), "OrderDetail": (860, 700),
           "ProductVariant": (1270, 300), "Color": (1680, 300), "Size": (1680, 470), "Banner": (1680, 700)}
CLS_NAME = {"users": "User", "addresses": "Address", "blogs": "Blog", "carts": "Cart", "wishlists": "Wishlist", "reviews": "Review",
            "orders": "Order", "vouchers": "Voucher", "cart_items": "CartItem", "products": "Product", "categories": "Category",
            "order_details": "OrderDetail", "product_variants": "ProductVariant", "colors": "Color", "sizes": "Size"}


def cls():
    d = Dia("Class diagram", 2000, 1180)
    d.text(40, 4, 1900, 30, "<b>SƠ ĐỒ LỚP (CLASS DIAGRAM)</b> – package com.fashionweb.model", size=18)
    ids, nodes = {}, {}
    for n, attrs in CLS.items():
        x, y = CLS_POS[n]
        h = 30 + 20 * len(attrs) + 8
        ids[n] = d.box(x, y, 270, h, "", "strokeColor=#333;strokeWidth=1.2;fillColor=#FFFFFF;")
        d.box(x, y, 270, 30, "<b>%s</b>" % n, "strokeColor=#333;strokeWidth=1.2;fillColor=#DCEBFA;fontSize=13;")
        d.text(x + 6, y + 32, 258, 20 * len(attrs), "<br>".join("- " + a for a in attrs), size=11, align="left", valign="top")
        nodes[n] = (x, y, 270, h)
    route_edges(d, ids, nodes, ROUTES, CLS_NAME)
    note(d, 1680, 900, 300, 130, "<b>Chú thích</b><br>Đường nối: quan hệ nhiều–một (*–1) qua khóa ngoại<br>Mỗi lớp ánh xạ một bảng (getter/setter viết tay)", 12)
    return d


# ------------------------------------------------------------ STATE
def state():
    d = Dia("Trang thai don hang", 1400, 540)
    d.text(40, 8, 1320, 30, "<b>SƠ ĐỒ TRẠNG THÁI ĐƠN HÀNG</b>", size=18)
    st = "rounded=1;arcSize=40;strokeColor=%s;strokeWidth=2;fillColor=#F5FAFD;fontSize=14;fontStyle=1;" % BLUE
    s0 = d.box(40, 190, 28, 28, "", "ellipse;fillColor=#222222;strokeColor=#222222;")
    p = d.box(260, 170, 190, 70, "Chờ xác nhận<br><font style='font-size:11px;font-weight:normal'>PENDING</font>", st)
    sh = d.box(660, 170, 190, 70, "Đang giao<br><font style='font-size:11px;font-weight:normal'>SHIPPING</font>", st)
    co = d.box(1060, 170, 190, 70, "Hoàn thành<br><font style='font-size:11px;font-weight:normal'>COMPLETED</font>",
               "rounded=1;arcSize=40;strokeColor=#2E7D32;strokeWidth=2;fillColor=#E3F4E6;fontSize=14;fontStyle=1;")
    ca = d.box(260, 350, 190, 70, "Đã hủy<br><font style='font-size:11px;font-weight:normal'>CANCELLED</font>",
               "rounded=1;arcSize=40;strokeColor=#B00020;strokeWidth=2;fillColor=#FDECEE;fontSize=14;fontStyle=1;")
    e = d.box(1320, 190, 34, 34, "", "ellipse;shape=doubleEllipse;fillColor=#222222;strokeColor=#222222;")
    d.edge(s0, p, "Đặt hàng (trừ tồn kho)", AR)
    d.edge(p, sh, "Admin xác nhận", AR)
    d.edge(sh, co, "Khách xác nhận đã nhận /<br>Admin hoàn thành", AR)
    d.edge(co, e, "", AR)
    d.edge(p, ca, "Khách hoặc Admin hủy (hoàn tồn kho, trả lượt voucher)", AR + "labelPosition=right;align=left;spacingLeft=6;")
    d.edge(sh, ca, "Admin hủy kèm lý do", AR + "edgeStyle=orthogonalEdgeStyle;", pts=[(755, 385)])
    note(d, 40, 460, 1320, 50, "Khách chỉ hủy được khi <b>Chờ xác nhận</b>. Chỉ đơn <b>Hoàn thành</b> mới được đánh giá sản phẩm. Đơn Hoàn thành → paymentStatus = PAID.", 12)
    return d


# ------------------------------------------------------------ ACTIVITY (swimlane)
def activity():
    d = Dia("Hoat dong dat hang", 1500, 1300)
    d.text(40, 6, 1420, 30, "<b>BIỂU ĐỒ HOẠT ĐỘNG</b> – Đặt hàng (Checkout)", size=18)
    lanes = [("Khách hàng", 40, 420), ("Hệ thống Saltair", 460, 560), ("payOS", 1020, 440)]
    for n, x, w in lanes:
        d.box(x, 50, w, 1210, "", "strokeColor=#888;fillColor=none;")
        d.box(x, 50, w, 34, "<b>%s</b>" % n, "strokeColor=#888;fillColor=#DCEBFA;fontSize=14;")
    pr = "rounded=1;arcSize=14;strokeColor=%s;strokeWidth=1.4;fillColor=#F5FAFD;fontSize=12;" % BLUE
    dc = "rhombus;strokeColor=#B26B00;strokeWidth=1.4;fillColor=#FFF4DE;fontSize=12;"
    er = "rounded=1;arcSize=14;strokeColor=#B00020;strokeWidth=1.4;fillColor=#FDECEE;fontSize=12;"
    s = d.box(240, 110, 28, 28, "", "ellipse;fillColor=#222;strokeColor=#222;")
    k1 = d.box(150, 170, 210, 50, "Xem giỏ hàng, bấm «Thanh toán»", pr)
    k2 = d.box(150, 260, 210, 50, "Chọn địa chỉ giao, ghi chú", pr)
    k3 = d.box(150, 350, 210, 50, "Nhập mã giảm giá (tùy chọn)", pr)
    h1 = d.box(520, 330, 220, 90, "Mã hợp lệ?<br><font style='font-size:10px'>(7 bước kiểm tra voucher)</font>", dc)
    h0 = d.box(790, 345, 190, 60, "Báo lỗi mã, giữ giỏ hàng", er)
    k4 = d.box(150, 470, 210, 50, "Chọn phương thức: COD / chuyển khoản", pr)
    k5 = d.box(150, 560, 210, 50, "Bấm «Đặt hàng»", pr)
    h2 = d.box(520, 540, 220, 90, "Đủ tồn kho?", dc)
    h3 = d.box(790, 555, 190, 60, "Báo hết hàng, quay về giỏ", er)
    h4 = d.box(500, 680, 260, 70, "Transaction: tạo đơn + chi tiết<br>trừ tồn kho, tăng lượt voucher,<br>xóa giỏ hàng", pr)
    h5 = d.box(520, 800, 220, 90, "Thanh toán<br>chuyển khoản?", dc)
    h6 = d.box(470, 960, 200, 56, "Đơn Chờ xác nhận<br>(thanh toán COD)", pr)
    q1 = d.box(1090, 815, 270, 60, "payOS tạo link / mã QR thanh toán", pr)
    q2 = d.box(1090, 940, 270, 76, "Khách quét QR, chuyển khoản;<br>payOS gọi webhook khi tiền về", pr)
    h7 = d.box(700, 950, 300, 66, "PaymentWebhookServlet: kiểm tra chữ ký,<br>đặt payment_status = PAID", pr)
    h8 = d.box(520, 1090, 260, 50, "Trang đặt hàng thành công", pr)
    k7 = d.box(150, 1090, 210, 50, "Theo dõi đơn trong «Đơn hàng»", pr)
    e = d.box(240, 1190, 34, 34, "", "ellipse;shape=doubleEllipse;fillColor=#222;strokeColor=#222;")
    ar = AR + "edgeStyle=orthogonalEdgeStyle;"
    for a_, b_, l in ((s, k1, ""), (k1, k2, ""), (k2, k3, ""), (k3, h1, ""), (h1, h0, "Không"), (k4, k5, ""), (k5, h2, ""),
                      (h2, h3, "Không"), (h2, h4, "Có"), (h4, h5, ""), (h5, h6, "Không"), (h5, q1, "Có"), (q1, q2, ""),
                      (q2, h7, ""), (h6, h8, ""), (h7, h8, ""), (h8, k7, ""), (k7, e, "")):
        d.edge(a_, b_, l, ar)
    d.edge(h1, k4, "Có", ar, pts=[(630, 495)])
    return d
