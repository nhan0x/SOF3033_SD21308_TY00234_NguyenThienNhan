# -*- coding: utf-8 -*-
from dg import *

ICON = "strokeColor=#FFFFFF;fillColor=%s;verticalLabelPosition=bottom;verticalAlign=top;fontSize=14;fontColor=#222222;" % BLUE


def icon(d, x, y, w, h, shape, label):
    return d.box(x, y, w, h, label, "shape=%s;%s" % (shape, ICON))


# ============================================================= LAB 1
def l1_deploy():
    d = Dia("Bai 2 - Mo hinh trien khai", 1240, 620)
    c1 = icon(d, 70, 70, 120, 90, "mxgraph.office.devices.laptop", "Nhân viên / Quản lý")
    c2 = icon(d, 70, 330, 120, 90, "mxgraph.office.devices.laptop", "Khách hàng")
    boundary(d, 520, 70, 640, 390, "Hệ thống PolyCoffee", 18)
    sv = icon(d, 600, 170, 90, 120, "mxgraph.cisco.servers.standard_host",
              "Web Server<br><font style='font-size:11px'>Apache Tomcat – Servlet/JSP</font>")
    db = d.box(950, 190, 110, 100, "SQL Server<br><font style='font-size:11px'>CSDL PolyCoffee</font>",
               "shape=cylinder3;boundedLbl=1;backgroundOutline=1;size=14;%s" % ICON)
    d.edge(c1, sv, "HTTP / HTTPS", "endArrow=block;endFill=1;strokeWidth=1.6;edgeStyle=orthogonalEdgeStyle;fontSize=12;labelBackgroundColor=#FFFFFF;",
           pts=[(330, 115), (330, 240)])
    d.edge(c2, sv, "", "endArrow=block;endFill=1;strokeWidth=1.6;edgeStyle=orthogonalEdgeStyle;fontSize=12;labelBackgroundColor=#FFFFFF;",
           pts=[(330, 375), (330, 240)])
    d.edge(sv, db, "JDBC<br>(TCP 1433)", "endArrow=block;endFill=1;strokeWidth=1.6;fontSize=12;labelBackgroundColor=#FFFFFF;")
    d.text(520, 480, 640, 30, "Client (trình duyệt)  →  Web Server (Servlet / JSP / JDBC)  →  Database (SQL Server 2008+)", size=12, color="#555555")
    return d


def l1_uc_nv():
    d = Dia("Bai 3 - Use case Nhan vien", 1240, 940)
    boundary(d, 190, 20, 1020, 890, "Hệ thống quản lý bán nước uống PolyCoffee", 19)
    AX, AY, R, OFF = 90, 470, 480, 430
    a = actor(d, AX, AY, "Nhân viên")
    X = lambda y: arc_x(AX, AY, R, y)
    dn = uc(d, X(120), 120, "Đăng nhập")
    qmk = uc(d, X(120) + OFF, 120, "Quên mật khẩu")
    dmk = uc(d, X(250), 250, "Đổi mật khẩu")
    stt = uc(d, X(370), 370, "Sửa thông tin cá nhân")
    tao = uc(d, X(520), 520, "Tạo phiếu bán hàng")
    them = uc(d, X(520) + OFF, 460, "Thêm đồ uống và số lượng", 250)
    dc = uc(d, X(520) + OFF, 580, "Điều chỉnh đồ uống\nvà số lượng", 250)
    sua = uc(d, X(700), 700, "Sửa phiếu bán hàng\n(chưa thanh toán)", 230, 72)
    huy = uc(d, X(700) + OFF, 660, "Hủy phiếu", 190)
    tt = uc(d, X(700) + OFF, 750, "Thanh toán (đóng phiếu)", 250)
    ls = uc(d, X(840), 840, "Xem lịch sử bán hàng", 230)
    ct = uc(d, X(840) + OFF, 840, "Xem chi tiết phiếu", 210)
    for u in (dn, dmk, stt, tao, sua, ls):
        assoc(d, a, u)
    extend(d, qmk, dn)
    include(d, tao, them)
    extend(d, dc, tao)
    extend(d, huy, sua)
    extend(d, tt, sua)
    extend(d, ct, ls)
    note(d, 760, 200, 420, 110,
         "<b>Ghi chú</b><br>Nhân viên phải <b>đăng nhập</b> mới sử dụng được các chức năng còn lại "
         "(đổi mật khẩu, sửa thông tin, tạo/sửa phiếu, xem lịch sử).", 12)
    return d


def l1_uc_ql():
    d = Dia("Bai 4 - Use case Quan ly", 1240, 900)
    boundary(d, 230, 20, 980, 850, "Hệ thống quản lý bán nước uống PolyCoffee", 19)
    AX, AY, R, OFF = 90, 520, 480, 440
    ql = actor(d, AX, AY, "Quản lý")
    nv = actor(d, AX, 130, "Nhân viên")
    X = lambda y: arc_x(AX, AY, R, y)
    chung = uc(d, 440, 90, "Các chức năng của\nnhân viên", 230, 72, "#F2F7FB")
    rows = [("Quản lý loại đồ uống", 230, [("Thêm / sửa loại", 190, 56), ("Xóa loại (chưa có đồ uống)", 250, 56)]),
            ("Quản lý đồ uống", 370, [("Thêm / sửa đồ uống", 210, 56), ("Xóa đồ uống (chưa bán)", 250, 56)]),
            ("Quản lý hóa đơn", 510, [("Xem danh sách / chi tiết", 230, 56), ("Cập nhật trạng thái hóa đơn", 260, 56)]),
            ("Quản lý nhân viên", 650, [("Thêm / sửa nhân viên", 220, 56), ("Xóa nhân viên (chưa có dữ liệu)", 280, 56)]),
            ("Thống kê doanh thu\ntheo thời gian", 750, []), ("Thống kê top sản phẩm\nbán chạy", 835, [])]
    for title, y, subs in rows:
        b = uc(d, X(y), y, title, 240, 64 if not subs else 60)
        assoc(d, ql, b)
        for k, (t, w, h) in enumerate(subs):
            extend(d, uc(d, X(y) + OFF, y - 33 + k * 66, t, w, h), b)
    inherit(d, ql, nv)
    assoc(d, nv, chung)
    return d


def erd1():
    d = Dia("ERD Level 1", 1240, 640)
    es = "rounded=0;strokeColor=#222222;strokeWidth=1.6;fillColor=#FFFFFF;fontSize=15;"
    cat = d.box(40, 250, 210, 90, "<b>categories</b><br><font style='font-size:12px'>Loại đồ uống</font>", es)
    dr = d.box(420, 250, 210, 90, "<b>drinks</b><br><font style='font-size:12px'>Đồ uống</font>", es)
    bd = d.box(800, 250, 210, 90, "<b>bill_details</b><br><font style='font-size:12px'>Chi tiết hóa đơn</font>", es)
    bl = d.box(800, 460, 210, 90, "<b>bills</b><br><font style='font-size:12px'>Hóa đơn / Phiếu bán hàng</font>", es)
    us = d.box(420, 460, 210, 90, "<b>users</b><br><font style='font-size:12px'>Nhân viên / Quản lý</font>", es)
    rd = "rhombus;strokeColor=#222222;strokeWidth=1.4;fillColor=#F2F7FB;fontSize=12;"
    r1 = d.box(270, 255, 130, 80, "gồm", rd)
    r2 = d.box(640, 255, 140, 80, "được bán<br>trong", rd)
    r3 = d.box(830, 360, 150, 80, "gồm", rd)
    r4 = d.box(640, 465, 140, 80, "lập", rd)
    ln = "endArrow=none;strokeWidth=1.5;"
    for a, b in ((cat, r1), (r1, dr), (dr, r2), (r2, bd), (bd, r3), (r3, bl), (bl, r4), (r4, us)):
        d.edge(a, b, "", ln)
    lab = lambda x, y, t: d.text(x, y, 30, 22, "<b>%s</b>" % t, size=13)
    lab(246, 258, "1"); lab(392, 258, "N")
    lab(616, 258, "1"); lab(770, 258, "N")
    lab(818, 335, "N"); lab(818, 440, "1")
    lab(776, 468, "N"); lab(618, 468, "1")
    d.text(40, 40, 1160, 34, "<b>ERD LEVEL 1</b> – Thực thể và mối quan hệ (hệ thống PolyCoffee)", size=18)
    note(d, 40, 580, 1170, 40,
         "1 loại có nhiều đồ uống · 1 đồ uống nằm trong nhiều dòng chi tiết · 1 hóa đơn gồm nhiều chi tiết · 1 nhân viên lập nhiều hóa đơn"
         " (hóa đơn ↔ đồ uống là N–N, tách qua bill_details)", 12)
    return d


def erd2():
    d = Dia("ERD Level 2", 1500, 860)
    d.text(40, 14, 1420, 34, "<b>ERD LEVEL 2</b> – Thuộc tính, khóa chính (PK), khóa ngoại (FK)", size=18)
    cat, _ = table(d, 40, 220, "categories", [("PK", "id : INT", True), ("", "name : NVARCHAR(100) – UNIQUE", True)], 270)
    dr, _ = table(d, 400, 110, "drinks", [
        ("PK", "id : INT", True), ("FK", "category_id : INT", True), ("", "name : NVARCHAR(100)", True),
        ("", "image : VARCHAR(255)", False), ("", "unit_price : DECIMAL(18,0)", True),
        ("", "discount : FLOAT (0..1)", True), ("", "available : BIT", True)], 280)
    bd, _ = table(d, 780, 110, "bill_details", [
        ("PK", "id : BIGINT", True), ("FK", "bill_id : BIGINT", True), ("FK", "drink_id : INT", True),
        ("", "unit_price : DECIMAL(18,0)", True), ("", "discount : FLOAT", False),
        ("", "quantity : INT (> 0)", True)], 280)
    bl, _ = table(d, 1130, 330, "bills", [
        ("PK", "id : BIGINT", True), ("FK", "user_id : INT", True), ("", "created_at : DATETIME", True),
        ("", "checkout_at : DATETIME", False), ("", "status : INT (0/1/2)", True)], 280)
    us, _ = table(d, 760, 470, "users", [
        ("PK", "id : INT", True), ("", "fullname : NVARCHAR(100)", True), ("", "email : VARCHAR(100) – UNIQUE", True),
        ("", "password : VARCHAR(100)", True), ("", "manager : BIT", True), ("", "enabled : BIT", True)], 300)
    st = "endArrow=ERmandOne;startArrow=ERmany;startSize=14;endSize=14;strokeWidth=1.5;edgeStyle=orthogonalEdgeStyle;fontSize=12;labelBackgroundColor=#FFFFFF;fontStyle=1;"
    d.edge(dr, cat, "N – 1", st, src_pt=None)
    d.edge(bd, dr, "N – 1", st)
    d.edge(bd, bl, "N – 1", st)
    d.edge(bl, us, "N – 1", st)
    note(d, 40, 520, 600, 110,
         "<b>Chú thích</b><br>PK: khóa chính (gạch chân) · FK: khóa ngoại · <b>chữ đậm</b>: NOT NULL<br>"
         "Ràng buộc: UNIQUE(bill_id, drink_id); CHECK (quantity &gt; 0)<br>"
         "Xóa hóa đơn → xóa chi tiết (ON DELETE CASCADE)<br>"
         "bills.status: 0 đang xử lý · 1 đã thanh toán · 2 đã hủy", 12)
    return d


def deploy2():
    d = Dia("Mo hinh trien khai 3 lop", 1280, 700)
    d.text(40, 14, 1200, 34, "<b>MÔ HÌNH TRIỂN KHAI</b> – Web Browser · Web Server · Database", size=18)
    cs = "rounded=1;arcSize=4;strokeColor=%s;strokeWidth=1.6;fillColor=#F5FAFD;" % BLUE
    chip = "rounded=1;arcSize=30;strokeColor=%s;fillColor=#FFFFFF;fontSize=12;" % BLUE
    # client
    d.box(40, 90, 280, 430, "", cs)
    d.text(40, 96, 280, 28, "<b>WEB BROWSER</b> (Client)", size=14, color=BLUE)
    cl = icon(d, 130, 140, 100, 80, "mxgraph.office.devices.laptop", "")
    for i, t in enumerate(("HTML – giao diện, form", "CSS – định dạng", "JavaScript – kiểm tra form")):
        d.box(70, 250 + i * 50, 220, 38, t, chip)
    d.text(60, 410, 240, 90, "Nhân viên · Quản lý · Khách hàng<br>(Chrome, Edge, Safari…)", size=12, color="#555555")
    # server
    d.box(470, 90, 340, 430, "", cs)
    d.text(470, 96, 340, 28, "<b>WEB SERVER</b>  Apache Tomcat 9 + JDK 1.8+", size=14, color=BLUE)
    sv = icon(d, 585, 135, 70, 90, "mxgraph.cisco.servers.standard_host", "")
    ts = ("Filter: Utf8Filter, AuthFilter", "Java Servlet (Controller)", "JSP + JSTL (View)", "JDBC – DAO, Entity")
    for i, t in enumerate(ts):
        d.box(500, 245 + i * 50, 280, 38, t, chip)
    # db
    d.box(960, 90, 280, 430, "", cs)
    d.text(960, 96, 280, 28, "<b>DATABASE</b> SQL Server 2008+", size=14, color=BLUE)
    db = d.box(1050, 150, 100, 90, "", "shape=cylinder3;boundedLbl=1;backgroundOutline=1;size=14;%s" % ICON)
    for i, t in enumerate(("categories, drinks", "users, bills", "bill_details")):
        d.box(990, 270 + i * 50, 220, 38, t, chip)
    arr = "endArrow=block;startArrow=none;endFill=1;strokeWidth=2;strokeColor=%s;fontSize=12;fontStyle=1;labelBackgroundColor=#FFFFFF;" % BLUE
    d.edge(None, None, "① HTTP Request<br>(GET / POST)", arr, src_pt=(320, 215), tgt_pt=(470, 215))
    d.edge(None, None, "④ HTTP Response<br>(HTML / redirect)", arr, src_pt=(470, 400), tgt_pt=(320, 400))
    d.edge(None, None, "② JDBC: câu lệnh SQL<br>(TCP 1433)", arr, src_pt=(810, 215), tgt_pt=(960, 215))
    d.edge(None, None, "③ ResultSet<br>(kết quả truy vấn)", arr, src_pt=(960, 400), tgt_pt=(810, 400))
    note(d, 40, 560, 1200, 100,
         "<b>Luồng giao tiếp:</b> ① Trình duyệt gửi HTTP Request → Tomcat (Filter kiểm tra UTF-8, đăng nhập, quyền) → Servlet đọc & kiểm tra tham số → "
         "② DAO gửi SQL qua JDBC tới SQL Server → ③ SQL Server trả ResultSet → Servlet đưa dữ liệu vào request, forward JSP → "
         "④ Tomcat gửi HTTP Response (HTML) về trình duyệt.", 12)
    return d


def flow():
    d = Dia("Luu do Request-Response", 1100, 1200)
    d.text(40, 10, 1020, 34, "<b>LƯU ĐỒ XỬ LÝ REQUEST / RESPONSE</b>", size=18)
    term = "rounded=1;arcSize=50;strokeColor=%s;strokeWidth=1.6;fillColor=%s;fontColor=#FFFFFF;fontSize=13;fontStyle=1;" % (BLUE, BLUE)
    proc = "rounded=1;arcSize=12;strokeColor=%s;strokeWidth=1.5;fillColor=#F5FAFD;fontSize=12;" % BLUE
    dec = "rhombus;strokeColor=#B26B00;strokeWidth=1.5;fillColor=#FFF4DE;fontSize=12;"
    err = "rounded=1;arcSize=12;strokeColor=#B00020;strokeWidth=1.5;fillColor=#FDECEE;fontSize=12;"
    ar = "endArrow=block;endFill=1;strokeWidth=1.5;edgeStyle=orthogonalEdgeStyle;fontSize=12;fontStyle=1;labelBackgroundColor=#FFFFFF;"
    CX = 330
    s = d.box(CX - 110, 60, 220, 44, "Browser gửi HTTP Request", term)
    f1 = d.box(CX - 130, 135, 260, 50, "Utf8Filter: đặt mã hóa UTF-8", proc)
    q1 = d.box(CX - 150, 215, 300, 86, "Đường dẫn công khai?<br>(trang chủ, /auth, css, images)", dec)
    q2 = d.box(CX - 120, 335, 240, 70, "Đã đăng nhập?", dec)
    q3 = d.box(CX - 150, 440, 300, 86, "URL /manager/* và<br>không phải quản lý?", dec)
    sv = d.box(CX - 140, 560, 280, 56, "Servlet: đọc và kiểm tra (validate)<br>tham số", proc)
    q4 = d.box(CX - 110, 650, 220, 70, "Dữ liệu hợp lệ?", dec)
    dao = d.box(CX - 150, 760, 300, 60, "DAO gửi SQL qua JDBC tới SQL Server,<br>nhận kết quả (ResultSet)", proc)
    fw = d.box(CX - 150, 855, 300, 60, "Servlet đưa dữ liệu vào request,<br>forward JSP (hoặc redirect)", proc)
    e = d.box(CX - 120, 950, 240, 44, "Browser nhận HTTP Response", term)
    r1 = d.box(720, 340, 260, 60, "Redirect về /auth/login", err)
    r2 = d.box(720, 450, 260, 60, "Trả lỗi 403 (không đủ quyền)", err)
    r3 = d.box(720, 655, 260, 60, "Forward JSP form<br>kèm thông báo lỗi", err)
    for a, b, l in ((s, f1, ""), (f1, q1, ""), (q2, q3, "Có"), (q3, sv, "Không"), (sv, q4, ""), (q4, dao, "Có"), (dao, fw, ""), (fw, e, "")):
        d.edge(a, b, l, ar)
    d.edge(q1, q2, "Không", ar)
    d.edge(q1, sv, "Có", ar, pts=[(120, 258), (120, 588)])
    d.edge(q2, r1, "Không", ar)
    d.edge(q3, r2, "Có", ar)
    d.edge(q4, r3, "Không", ar)
    d.edge(r3, sv, "", ar, pts=[(850, 588)])
    return d
