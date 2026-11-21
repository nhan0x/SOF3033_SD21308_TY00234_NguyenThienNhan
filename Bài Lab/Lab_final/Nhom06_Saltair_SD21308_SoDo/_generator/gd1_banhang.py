# -*- coding: utf-8 -*-
"""Dung GIAIDOAN1 bang cach nhan ban file mau 'bao cao SITE BAN HANG': giu nguyen bia, header/footer, style,
chi clone tung doan/bang mau roi thay noi dung Saltair."""
import sys, copy, json, os
from docx import Document
from docx.shared import Cm, Pt
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.text.paragraph import Paragraph

SRC = "/Users/nguyenluke/Downloads/mau ban hang - Project Document du an khac - bao cao SITE BAN HANG.docx"
BASE = "/Users/nguyenluke/Documents/FPOLY/SOF/SOF3033_SD21308_TY00234_NguyenThienNhan/Bài Lab/Lab_final"
G = BASE + "/Nhom06_Saltair_SD21308_SoDo/giaidoan1/"
OUT = sys.argv[1] if len(sys.argv) > 1 else BASE + "/Nhom06_Saltair_SD21308/Nhom06_Saltair_SD21308_GIAIDOAN1.docx"
PAGES = json.load(open(sys.argv[2])) if len(sys.argv) > 2 and os.path.exists(sys.argv[2]) else []
sys.path.insert(0, BASE + "/Nhom06_Saltair_SD21308_SoDo/_generator")
import saltair as S

d = Document(SRC)
body = d.element.body
B = list(body)
SECT = B[-1]
W = lambda tag: qn("w:" + tag)


def cp(i):
    return copy.deepcopy(B[i])


def first_rpr(p):
    for r in p.iter(W("r")):
        rp = r.find(W("rPr"))
        if r.find(W("t")) is not None:
            return copy.deepcopy(rp) if rp is not None else None
    ppr = p.find(W("pPr"))
    if ppr is not None and ppr.find(W("rPr")) is not None:
        rp = copy.deepcopy(ppr.find(W("rPr")))
        return rp
    return None


def set_text(p, text):
    rpr = first_rpr(p)
    for ch in list(p):
        if ch.tag != W("pPr"):
            p.remove(ch)
    if text:
        r = OxmlElement("w:r")
        if rpr is not None:
            r.append(rpr)
        t = OxmlElement("w:t"); t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve"); t.text = text
        r.append(t); p.append(r)
    return p


NEW = []


def add(el):
    NEW.append(el); return el


def P(proto, text):
    p = set_text(cp(proto), text)
    if proto in (133, 135, 312, 230):
        ppr = p.find(W("pPr"))
        if ppr is None:
            ppr = OxmlElement("w:pPr"); p.insert(0, ppr)
        kn = OxmlElement("w:keepNext"); ppr.insert(0, kn)
    return add(p)


PB_PROTO = next(p for p in B if p.tag == W("p") and any(br.get(W("type")) == "page" for br in p.iter(W("br"))))


def page_break():
    p = OxmlElement("w:p"); r = OxmlElement("w:r"); br = OxmlElement("w:br"); br.set(W("type"), "page"); r.append(br); p.append(r); add(p)


HEADS = []


def H1(t):
    if HEADS:
        page_break()
    HEADS.append((1, t)); return P(218, t)


def H1u(t):  # chuong khong danh so (nhu 'Thiet ke du lieu')
    page_break(); HEADS.append((0, t)); return P(316, t)


def H2(t, proto=219):
    HEADS.append((2, t)); return P(proto, t)


def H3(t):
    HEADS.append((3, t)); return P(143, t)


LABEL, LABEL2, BODY, DASH, DOT, PLUS, CAP, FIG, UILABEL = 76, 89, 77, 90, 99, 98, 137, 133, 230


def bullets(items, proto=DASH):
    for t in items:
        P(proto, t)


def image(fname, width_cm=16):
    p = cp(134)
    for ch in list(p):
        if ch.tag != W("pPr"):
            p.remove(ch)
    ppr = p.find(W("pPr")); jc = OxmlElement("w:jc"); jc.set(W("val"), "center"); ppr.insert(0, jc)
    add(p)
    return (p, G + fname, width_cm)


IMAGES = []


def IMG(fname, caption=None, width_cm=16):
    IMAGES.append(image(fname, width_cm))
    if caption:
        P(CAP, caption)


# ------------------------------------------------------------------ bang
def fill_cell(tc, lines):
    if isinstance(lines, str):
        lines = [lines]
    ps = tc.findall(W("p")); proto = ps[0]
    for p in ps[1:]:
        tc.remove(p)
    set_text(proto, lines[0] if lines else "")
    last = proto
    for ln in lines[1:]:
        np_ = set_text(copy.deepcopy(proto), ln); last.addnext(np_); last = np_


def table_like(i, rows, keep_head=1, proto_row=None):
    t = cp(i); trs = t.findall(W("tr"))
    proto = copy.deepcopy(trs[proto_row if proto_row is not None else keep_head])
    for tr in trs[keep_head:]:
        t.remove(tr)
    for row in rows:
        tr = copy.deepcopy(proto)
        for tc, val in zip(tr.findall(W("tc")), row):
            fill_cell(tc, val)
        t.append(tr)
    return add(t)


def set_header(t, heads):
    tr = t.findall(W("tr"))[0]
    for tc, h in zip(tr.findall(W("tc")), heads):
        fill_cell(tc, h)


def set_widths(t, ws):
    tp = t.find(W("tblPr")); tw = tp.find(W("tblW"))
    if tw is None:
        tw = OxmlElement("w:tblW"); tp.append(tw)
    tw.set(W("w"), str(sum(ws))); tw.set(W("type"), "dxa")
    g = t.find(W("tblGrid"))
    for gc in list(g):
        g.remove(gc)
    for w in ws:
        gc = OxmlElement("w:gridCol"); gc.set(W("w"), str(w)); g.append(gc)
    for tr in t.findall(W("tr")):
        k = 0
        for tc in tr.findall(W("tc")):
            pr = tc.find(W("tcPr")); gs = pr.find(W("gridSpan")) if pr is not None else None
            n = int(gs.get(W("val"))) if gs is not None else 1
            cw = pr.find(W("tcW")) if pr is not None else None
            if cw is None and pr is not None:
                cw = OxmlElement("w:tcW"); pr.insert(0, cw)
            if cw is not None:
                cw.set(W("w"), str(sum(ws[k:k + n]))); cw.set(W("type"), "dxa")
            k += n


def drop_last_col(t):
    g = t.find(W("tblGrid")); g.remove(g.findall(W("gridCol"))[-1])
    for tr in t.findall(W("tr")):
        tr.remove(tr.findall(W("tc"))[-1])
    set_widths(t, [2400, 3300, 3300])


def spec(desc, inp, proc, out, role):
    t = cp(144); trs = t.findall(W("tr"))
    for tr, val in zip(trs, (desc, inp, proc, out, role)):
        fill_cell(tr.findall(W("tc"))[1], val)
    add(t); P(BODY, "")


# ================================================================== BIA
REP = {"BÁO CÁO DỰ ÁN TỐT NGHIỆP": "BÁO CÁO DỰ ÁN JAVA 1 – GIAI ĐOẠN 1",
       "WEBSITE BÁN HÀNG LƯU NIỆM ONLINE": "WEBSITE BÁN QUẦN ÁO BASIC SALTAIR",
       "Thành viên nhóm :": "Thành viên nhóm 06 – Lớp SD21308:", "12": "11", " 2020": " 2026"}
cover = B[0]
for t in cover.iter(W("t")):
    if t.text in REP:
        t.text = REP[t.text]
MEM = [("TY00234", "Nguyễn Thiện Nhân", "Trưởng nhóm"), ("TY00254", "Lê Hoàng Minh", "Thành viên"), ("TY00253", "Phúc", "Thành viên")]
for tbl in cover.iter(W("tbl")):
    trs = tbl.findall(W("tr"))
    while len(trs) < len(MEM) + 1:
        nr = copy.deepcopy(trs[-1]); trs[-1].addnext(nr); trs = tbl.findall(W("tr"))
    for tr in trs[len(MEM) + 1:]:
        tbl.remove(tr)
    trs = tbl.findall(W("tr"))
    for k, tr in enumerate(trs[1:len(MEM) + 1]):
        tcs = tr.findall(W("tc"))
        vals = [str(k + 1)] + list(MEM[k])
        for tc, v in zip(tcs, vals):
            ps = tc.findall(W("p"))
            rp = None
            for c2 in trs[1].findall(W("tc")):
                rp = first_rpr(c2.find(W("p"))) or rp
            p0 = ps[0]
            for ch in list(p0):
                if ch.tag != W("pPr"):
                    p0.remove(ch)
            if v:
                r = OxmlElement("w:r")
                if rp is not None:
                    r.append(copy.deepcopy(rp))
                tt = OxmlElement("w:t"); tt.text = v; r.append(tt); p0.append(r)

# ================================================================== LOI MO DAU
set_text(B[60], "Những năm gần đây, mua sắm thời trang trực tuyến tại Việt Nam phát triển rất mạnh. Khách hàng ngày càng quen với việc xem sản phẩm, chọn màu, chọn size và đặt hàng ngay trên điện thoại hoặc máy tính thay vì đến tận cửa hàng.")
set_text(B[61], "Một website bán hàng giúp cửa hàng giới thiệu sản phẩm rõ ràng, quản lý tồn kho theo từng biến thể, xử lý đơn hàng và thống kê doanh thu chính xác; còn khách hàng có thể mua sắm mọi lúc, theo dõi đơn hàng và nhận ưu đãi qua mã giảm giá.")
set_text(B[62], "Vì lí do đó, nhóm 06 chúng em quyết định thực hiện đề tài “Website bán quần áo basic Saltair”. Mục đích là xây dựng một website thương mại điện tử hoàn chỉnh bằng Java Servlet/JSP và MySQL, giúp khách hàng mua sắm dễ dàng và giúp cửa hàng quản lý sản phẩm, đơn hàng, khuyến mãi hiệu quả.")

# ================================================================== NOI DUNG
H1("Giới thiệu")
P(LABEL, "Lý do chọn đề tài:")
for t in ["Hiện nay, thương mại điện tử đã trở thành kênh bán hàng chính của nhiều cửa hàng thời trang vừa và nhỏ. Nhu cầu mua quần áo qua mạng ngày càng tăng, nhất là các sản phẩm basic dễ phối đồ.",
          "Cửa hàng Saltair kinh doanh quần áo basic phong cách biển, unisex, gồm khoảng 24 sản phẩm thuộc 4 danh mục: Áo thun và Polo, Áo nỉ và Hoodie, Áo khoác, Quần. Mỗi sản phẩm có 2 đến 3 màu và 4 size (S, M, L, XL).",
          "Trước đây cửa hàng nhận đơn qua tin nhắn, ghi chép tồn kho thủ công nên dễ nhầm size, nhầm màu, khó thống kê doanh thu và khó áp dụng chương trình khuyến mãi.",
          "Đề tài giúp nhóm vận dụng kiến thức Java Servlet/JSP, JDBC, cơ sở dữ liệu và quy trình phát triển phần mềm đã học vào một sản phẩm thực tế.",
          "Qua đó đề tài nhóm em xin chọn xây dựng “Website bán quần áo basic Saltair”."]:
    P(BODY, t)
P(LABEL, "Mục tiêu của đề tài:")
bullets(["Khách hàng xem, tìm kiếm, lọc sản phẩm và đặt mua theo màu, size; thanh toán COD hoặc chuyển khoản.",
         "Cửa hàng quản lý sản phẩm, tồn kho theo biến thể, đơn hàng, mã giảm giá, nội dung (banner, blog) và thống kê doanh thu.",
         "Hệ thống có đăng nhập, phân quyền, kiểm tra dữ liệu ở client và server."])

H1("Khảo sát")
P(LABEL2, "Yêu cầu khách hàng:")
bullets(["Cửa hàng yêu cầu xây dựng một website bán quần áo có giao diện sáng, đơn giản, hiển thị đẹp trên cả máy tính và điện thoại.",
         "Sản phẩm được phân theo danh mục, có ảnh, mô tả, giá, màu, size, số lượng tồn và đánh giá; khách lọc theo danh mục, khoảng giá và tìm kiếm theo tên.",
         "Quản lý và xử lý đơn hàng trơn tru: khách được hủy đơn khi đơn còn “Chờ xác nhận”; quản trị viên xác nhận, giao, hoàn thành hoặc hủy đơn kèm lý do.",
         "Có mã giảm giá theo phần trăm hoặc số tiền, phí ship cố định 30.000đ và miễn phí ship khi dùng mã hợp lệ.",
         "Quản trị viên xem được thống kê doanh thu và sản phẩm bán chạy."])
P(BODY, "")
P(LABEL2, "Yêu cầu nghiệp vụ:")
bullets(["Bảo mật: các trang cá nhân (hồ sơ, sổ địa chỉ, yêu thích, đơn hàng) phải đăng nhập; khu vực quản trị /admin chỉ dành cho tài khoản ADMIN; mật khẩu băm bằng PBKDF2-SHA256.",
         "Quản lý:"])
P(PLUS, "+  Khách (Guest): ")
bullets(["Xem trang chủ, danh sách sản phẩm có phân trang; lọc theo danh mục, khoảng giá, màu, size; sắp xếp; tìm kiếm và xem chi tiết sản phẩm.",
         "Thêm sản phẩm vào giỏ hàng tạm (lưu trong phiên) và đặt hàng bằng email; hệ thống tìm hoặc tạo tài khoản theo email đó.",
         "Xem blog, đăng ký tài khoản và đăng nhập."], DOT)
P(PLUS, "+  Khách hàng (Customer): ")
bullets(["Khách hàng có quyền chỉnh sửa họ tên, số điện thoại, đổi mật khẩu và quản lý sổ địa chỉ giao hàng của bản thân.",
         "Khách hàng quản lý giỏ hàng, danh sách yêu thích; đặt hàng, áp mã giảm giá, thanh toán COD hoặc chuyển khoản.",
         "Khách hàng xem lịch sử đơn, hủy đơn khi còn chờ xác nhận, xác nhận đã nhận hàng.",
         "Khách hàng đánh giá sản phẩm khi đơn đã hoàn thành, được sửa hoặc xóa đánh giá của mình."], DOT)
P(PLUS, "+  Quản trị viên (Admin): ")
bullets(["Quản trị viên quản lý sản phẩm và biến thể (màu, size, tồn kho), danh mục, màu và size; xóa mềm và khôi phục dữ liệu.",
         "Quản trị viên quản lý đơn hàng: tìm kiếm, lọc, cập nhật trạng thái, hủy đơn kèm lý do.",
         "Quản trị viên quản lý mã giảm giá, người dùng (khóa, mở khóa, đổi vai trò), banner, blog và bình luận.",
         "Quản trị viên xem thống kê tổng đơn, doanh thu theo khoảng ngày và top sản phẩm bán chạy."], DOT)
P(BODY, "")
P(LABEL2, "Tính khả thi:")
bullets(["Nhóm hoàn toàn có đủ năng lực để đáp ứng dự án.", "Nằm trong khả năng lập trình của nhóm (Java Servlet/JSP, JDBC, MySQL).", "Chủ đề thiết thực, gần với nhu cầu mua sắm hằng ngày."])
P(BODY, "")
P(LABEL2, "Kế hoạch thực hiện (năm 2026):")
PLAN = [("1", "Khởi tạo dự án, cấu hình gửi mail, tóm tắt dự án và phân công nhóm", "06/11", "07/11", "Nhân", "PR #28–#30"),
        ("2", "Chia code theo tầng, thư mục con theo chức năng", "09/11", "09/11", "Nhân, Minh", "PR #31"),
        ("3", "Phase 1: đăng nhập, phân quyền, sản phẩm, giỏ hàng, voucher, danh mục, màu và size", "09/11", "09/11", "Nhân, Phúc", "PR #32–#39"),
        ("4", "Phase 2: tài khoản, trang chủ, blog, đặt hàng, đơn hàng, đánh giá, sản phẩm", "09/11", "09/11", "Nhân, Minh, Phúc", "Release PR #49"),
        ("5", "Phase 3: quản trị người dùng, banner, voucher, đơn hàng, bình luận, thùng rác, dashboard", "09/11", "09/11", "Nhân, Minh, Phúc", "Release PR #59"),
        ("6", "Lưu ảnh sản phẩm trên Cloudinary, làm lại giao diện trang chủ", "10/11", "10/11", "Nhân, Phúc", "PR #60–#71"),
        ("7", "Tìm kiếm nhanh sản phẩm", "11/11", "11/11", "Phúc", "PR #76, #77"),
        ("7,1", "Unit test JUnit 5 cho nghiệp vụ", "11/11", "11/11", "Minh", "PR #80"),
        ("7,2", "Tài liệu quy trình phát triển", "11/11", "11/11", "Nhân", "PR #92"),
        ("8", "Đăng nhập email và mật khẩu, Flyway, bỏ chế độ demo", "16/11", "17/11", "Nhân, Minh", "PR #93–#100"),
        ("9", "Thanh toán chuyển khoản payOS", "17/11", "17/11", "Nhân, Minh", "PR #101"),
        ("10", "Viết tài liệu dự án giai đoạn 1", "19/11", "21/11", "Nhân, Minh, Phúc", "")]
set_widths(table_like(130, [[a, b, c, e, m, "Hoàn thành", g] for a, b, c, e, m, g in PLAN]), [600, 2400, 500, 500, 500, 500, 450, 450, 450, 950, 1000])

H1("Phân tích")
P(FIG, "Mô hình sơ đồ triển khai")
IMG("trien_khai.png", None)
P(135, "Sơ đồ Use Case")
IMG("uc_tong_quat.png", "Use Case tổng quát", 15)
IMG("uc_customer.png", "Use Case khách hàng", 15.5)
IMG("uc_admin.png", "Use Case quản trị viên", 13.5)
H2("Đặc tả yêu cầu khách hàng", 142)
H3("Đăng ký và đăng nhập")
spec(["Mô tả cách đăng ký tài khoản và đăng nhập vào hệ thống."],
     ["Đăng ký: email, họ tên, mật khẩu, nhập lại mật khẩu.", "Đăng nhập: email và mật khẩu."],
     ["Đăng ký: kiểm tra email hợp lệ và chưa có tài khoản, họ tên tối đa 100 ký tự, mật khẩu đủ độ dài và khớp ô nhập lại.", "Đăng nhập: kiểm tra email, mật khẩu băm và trạng thái tài khoản."],
     ["Thông báo lỗi từng ô nếu dữ liệu sai; báo tài khoản đã bị khóa.", "Đăng ký xong được đăng nhập luôn và trở về trang trước đó hoặc trang chủ.", "Tài khoản ADMIN vào /admin (phiên 30 phút, gửi mail báo có lượt đăng nhập quản trị)."],
     ["Khách (Guest), khách hàng, quản trị viên."])
H3("Xem và tìm kiếm sản phẩm")
spec(["Khách xem danh sách sản phẩm, lọc, sắp xếp, tìm kiếm và xem chi tiết sản phẩm."],
     ["Danh mục, khoảng giá, màu, size, từ khóa, trang.", "Sắp xếp: mới nhất, giá tăng, giá giảm, bán chạy, đánh giá cao."],
     ["Hệ thống truy xuất sản phẩm chưa xóa mềm theo điều kiện lọc, phân trang 12 sản phẩm mỗi trang.", "Ô tìm kiếm nhanh gợi ý sản phẩm và danh mục khi gõ từ 2 ký tự."],
     ["Danh sách sản phẩm phù hợp.", "Trang chi tiết: ảnh, mô tả, giá, màu, size, tồn kho, đánh giá."],
     ["Mọi người dùng."])
H3("Quản lý giỏ hàng")
spec(["Thêm sản phẩm theo màu và size vào giỏ, tăng giảm số lượng, xóa sản phẩm khỏi giỏ.", "Khách chưa đăng nhập dùng giỏ tạm trong phiên; khi đăng nhập giỏ tạm được gộp vào giỏ trong CSDL."],
     ["Biến thể (màu + size) và số lượng."],
     ["Kiểm tra đã chọn size, màu.", "Số lượng không vượt tồn kho của biến thể; số lượng 0 thì xóa dòng.", "Cập nhật giỏ hàng (CSDL hoặc phiên)."],
     ["Giỏ hàng cập nhật số lượng, tạm tính và số sản phẩm trên biểu tượng giỏ.", "Thông báo lỗi nếu chưa chọn size."],
     ["Khách (Guest), khách hàng."])
H3("Mua hàng")
spec(["Đặt hàng từ giỏ: chọn địa chỉ trong sổ địa chỉ hoặc nhập địa chỉ mới (có thể lưu lại), ghi chú, phương thức thanh toán COD hoặc chuyển khoản.", "Khách chưa đăng nhập nhập thêm email; đơn được gắn vào tài khoản của email đó (chưa có thì tạo mới)."],
     ["Địa chỉ giao hàng, ghi chú, mã giảm giá (nếu có), phương thức thanh toán."],
     ["Kiểm tra tồn kho và mã giảm giá.", "Trong một transaction: tạo đơn và chi tiết đơn, trừ tồn kho, tăng lượt dùng voucher, xóa giỏ hàng.", "Chuyển khoản (chỉ khi đã cấu hình tài khoản nhận tiền): trang Saltair hiện mã QR payOS kèm đồng hồ đếm ngược; khi tiền về payOS gọi webhook, hệ thống kiểm tra chữ ký rồi đặt trạng thái thanh toán là đã thanh toán."],
     ["Trang đặt hàng thành công kèm mã đơn (dạng SAyyyyMMddNNN).", "Đơn mới ở trạng thái “Chờ xác nhận”; khách hủy thanh toán chuyển khoản thì đơn vẫn giữ để thanh toán lại."],
     ["Khách (Guest), khách hàng."])
H3("Áp mã giảm giá")
spec(["Khách hàng nhập mã giảm giá khi đặt hàng; mỗi đơn dùng tối đa một mã."],
     ["Mã giảm giá."],
     ["Kiểm tra lần lượt: mã tồn tại, còn kích hoạt, trong thời hạn, còn lượt dùng, khách chưa dùng quá số lần, đạt đơn tối thiểu, có sản phẩm thuộc danh mục áp dụng.", "Tính giảm theo PERCENT (không vượt giảm tối đa) hoặc AMOUNT."],
     ["Số tiền được giảm, phí ship = 0.", "Tổng thanh toán = tiền hàng − giảm giá + phí ship.", "Thông báo lỗi đúng theo thứ tự kiểm tra."],
     ["Khách (Guest), khách hàng."])
H3("Theo dõi và hủy đơn hàng")
spec(["Khách hàng xem lịch sử đơn, xem chi tiết, hủy đơn khi còn chờ xác nhận và xác nhận đã nhận hàng."],
     ["Trạng thái cần lọc, mã đơn."],
     ["Hệ thống truy xuất đơn của khách.", "Hủy đơn: hoàn tồn kho và trả lượt dùng voucher."],
     ["Danh sách và chi tiết đơn hàng.", "Trạng thái đơn được cập nhật."],
     ["Khách hàng."])
H3("Quản lý tài khoản")
spec(["Khách hàng tự quản lý tài khoản, sổ địa chỉ và danh sách yêu thích của chính mình."],
     ["Họ tên, số điện thoại (email và vai trò không sửa được); mật khẩu hiện tại và mật khẩu mới; địa chỉ giao hàng; sản phẩm yêu thích."],
     ["Kiểm tra dữ liệu hợp lệ; đổi mật khẩu phải nhập đúng mật khẩu hiện tại.", "Chỉ thao tác trên dữ liệu của chính tài khoản đang đăng nhập."],
     ["Thông tin tài khoản, sổ địa chỉ (có địa chỉ mặc định), danh sách yêu thích được cập nhật."],
     ["Khách hàng."])
H3("Quản lý đánh giá")
spec(["Khách hàng đánh giá sao (1 đến 5) và bình luận sản phẩm khi đơn đã hoàn thành; sửa hoặc xóa đánh giá của mình."],
     ["Số sao (1 đến 5), nội dung bình luận (10 đến 1000 ký tự)."],
     ["Kiểm tra khách có đơn Hoàn thành chứa sản phẩm.", "Mỗi khách một đánh giá cho mỗi sản phẩm; đánh giá lại thì cập nhật đánh giá cũ."],
     ["Đánh giá hiển thị ở trang chi tiết sản phẩm."],
     ["Khách hàng; quản trị viên ẩn hoặc hiện bình luận."])
H2("Đặc tả yêu cầu quản trị viên", 142)
H3("Quản lý sản phẩm")
spec(["Cho phép quản trị viên toàn quyền xử lý sản phẩm và biến thể."],
     ["Tên, slug, danh mục, giá, mô tả, ảnh (JPG/PNG/WebP tối đa 2MB), sản phẩm nổi bật; các màu áp dụng và số lượng tồn cho từng cặp màu × size."],
     ["Kiểm tra: tên 2 đến 150 ký tự, giá 1.000đ đến 99.999.999đ, chọn ít nhất một màu, tồn kho 0 đến 99.999, mỗi cặp màu × size chỉ nhập một lần.", "Tải ảnh lên Cloudinary (nếu đã cấu hình) và tạo ảnh thu nhỏ 600px; lưu sản phẩm và biến thể trong một transaction.", "Tìm theo tên, lọc theo danh mục; xóa mềm đưa vào Thùng rác."],
     ["Danh sách sản phẩm được cập nhật."],
     ["Quản trị viên."])
H3("Quản lý danh mục, màu và size")
spec(["Cho phép quản trị viên xử lý danh mục sản phẩm, màu và size."],
     ["Tên danh mục, ảnh; tên màu, mã màu, mã hex; tên size, thứ tự."],
     ["Kiểm tra trùng tên, mã, slug.", "Xóa mềm: danh mục còn sản phẩm đang bán, hoặc màu/size đang có trong biến thể thì bị chặn.", "Dữ liệu đã xóa mềm được khôi phục ở Thùng rác (sản phẩm, danh mục, màu, size)."],
     ["Danh sách danh mục, màu, size được cập nhật."],
     ["Quản trị viên."])
H3("Quản lý đơn hàng")
spec(["Cho phép quản trị viên tra cứu và xử lý đơn hàng."],
     ["Tên khách hoặc mã đơn, trạng thái đơn, trạng thái thanh toán, lý do hủy (5 đến 255 ký tự)."],
     ["Hệ thống truy xuất CSDL và hiển thị danh sách đơn.", "Cập nhật trạng thái theo thứ tự Chờ xác nhận → Đang giao → Hoàn thành.", "Hủy đơn (khi chờ xác nhận hoặc đang giao): hoàn tồn kho và lượt voucher."],
     ["Danh sách và chi tiết đơn hàng; trạng thái đơn được cập nhật."],
     ["Quản trị viên."])
H3("Quản lý mã giảm giá")
spec(["Cho phép quản trị viên tạo và quản lý mã giảm giá."],
     ["Mã, loại giảm, giá trị, giảm tối đa, đơn tối thiểu, danh mục áp dụng, thời hạn, tổng lượt, lượt mỗi khách."],
     ["Kiểm tra: mã viết hoa không dấu 4 đến 20 ký tự và không sửa sau khi tạo; ngày kết thúc không trước ngày bắt đầu; PERCENT từ 1 đến 100; tổng lượt > 0.", "Bật hoặc tạm dừng mã; chỉ xóa được mã chưa từng được dùng, mã đã dùng thì chỉ tạm dừng."],
     ["Danh sách mã giảm giá kèm số lần đã dùng."],
     ["Quản trị viên."])
H3("Quản lý người dùng và nội dung")
spec(["Cho phép quản trị viên quản lý tài khoản, banner, blog và bình luận."],
     ["Email hoặc họ tên cần tìm, vai trò cần lọc; thông tin banner, bài viết; trạng thái hiển thị."],
     ["Khóa, mở khóa tài khoản, đổi vai trò (không được tự khóa hoặc tự đổi vai trò của chính mình).", "Thêm, sửa, xóa, bật tắt banner và sắp xếp thứ tự; thêm, sửa, xóa, đăng hoặc ẩn bài viết; ẩn hoặc hiện bình luận."],
     ["Dữ liệu được cập nhật và hiển thị trên website."],
     ["Quản trị viên."])
H3("Thống kê doanh thu")
spec(["Cho phép quản trị viên thống kê doanh thu theo khoảng thời gian."],
     ["Ngày bắt đầu, ngày kết thúc."],
     ["Mặc định 7 ngày gần nhất, tối đa 92 ngày.", "Doanh thu chỉ tính đơn Hoàn thành; top bán chạy tính trên đơn không bị hủy."],
     ["Tổng số đơn, doanh thu, biểu đồ doanh thu theo ngày, số đơn theo trạng thái, top sản phẩm bán chạy, đơn mới nhất và sản phẩm sắp hết hàng."],
     ["Quản trị viên."])
H2("Yêu cầu hệ thống", 211)
bullets(["Công nghệ sử dụng: Java 17, Jakarta Servlet 6.1 / JSP 4, JSTL 3.", "Kiến trúc: MVC + Service + DAO, JDBC thuần với PreparedStatement; Flyway tạo bảng tự động khi khởi động.",
         "Web server: Apache Tomcat 11; công cụ build: Maven.", "Cơ sở dữ liệu: MySQL 8.", "IDE: IntelliJ IDEA; quản lý mã nguồn: Git, GitHub."], 213)

H1("Thiết kế chức năng")
H2("Mô hình công nghệ")
IMG("kien_truc.png", None)
H2("Sơ đồ trạng thái ứng dụng")
IMG("trang_thai_don.png", "Trạng thái đơn hàng")
IMG("hoat_dong_dat_hang.png", "Biểu đồ hoạt động đặt hàng", 14.5)
H2("Thiết kế chi tiết giao diện chức năng")
UI_KH = [("Giao diện trang chủ", "kh_trang_chu"), ("Trang chủ: sản phẩm mới, danh mục", "kh_trang_chu_2"), ("Footer", "kh_footer"),
         ("Giao diện trang sản phẩm", "kh_san_pham"), ("Chi tiết sản phẩm", "kh_chi_tiet"), ("Giao diện giỏ hàng", "kh_gio_hang"),
         ("Giao diện thanh toán", "kh_thanh_toan"), ("Giao diện đăng nhập", "kh_dang_nhap"), ("Giao diện đăng ký", "kh_dang_ky"),
         ("Giao diện thông tin tài khoản", "kh_tai_khoan"), ("Sổ địa chỉ", "kh_so_dia_chi"), ("Danh sách yêu thích", "kh_yeu_thich"),
         ("Lịch sử đơn hàng", "kh_don_hang"), ("Chi tiết đơn hàng", "kh_chi_tiet_don"), ("Giao diện trang blog", "kh_blog")]
UI_AD = [("Trang chủ admin (dashboard thống kê)", "ad_dashboard"), ("Quản lý sản phẩm", "ad_san_pham"), ("Giao diện chỉnh sửa sản phẩm", "ad_form_san_pham"),
         ("Quản lý danh mục", "ad_danh_muc"), ("Quản lý màu và size", "ad_mau_size"), ("Quản lý đơn hàng", "ad_don_hang"),
         ("Quản lý mã giảm giá", "ad_voucher"), ("Quản lý người dùng", "ad_nguoi_dung"), ("Quản lý bình luận", "ad_binh_luan"),
         ("Quản lý banner", "ad_banner"), ("Quản lý bài viết", "ad_blog"), ("Thùng rác", "ad_thung_rac")]
P(229, "Giao diện người dùng ")
for label, f in UI_KH:
    P(UILABEL, label)
    IMG("giao_dien/%s.png" % f, None, 15.5)
P(229, "Giao diện trang admin")
for label, f in UI_AD:
    P(UILABEL, label)
    IMG("giao_dien/%s.png" % f, None, 15.5)
P(312, "ERD")
IMG("erd.png", None)
P(312, "CLASS DIAGRAM")
IMG("class.png", None)

H1u("Thiết kế dữ liệu")
H2("Thiết kế mô hình quan hệ dữ liệu", 317)
IMG("erd.png", None)
H2("Thiết kế chi tiết", 321)
P(322, "CHI TIẾT THỰC THỂ")
VN = {"users": "Người dùng", "addresses": "Sổ địa chỉ", "categories": "Danh mục", "colors": "Màu", "sizes": "Size", "products": "Sản phẩm",
      "product_images": "Ảnh sản phẩm", "product_variants": "Biến thể sản phẩm", "carts": "Giỏ hàng", "cart_items": "Chi tiết giỏ hàng",
      "wishlists": "Yêu thích", "vouchers": "Mã giảm giá", "orders": "Đơn hàng", "order_details": "Chi tiết đơn hàng", "reviews": "Đánh giá",
      "banners": "Banner", "blogs": "Bài viết", "payment_transactions": "Giao dịch thanh toán"}
DESC = {"id": "ID tự tăng của bản ghi", "user_id": "ID người dùng", "email": "Địa chỉ email (dùng để đăng nhập)", "password_hash": "Mật khẩu đã băm",
        "full_name": "Họ và tên", "phone": "Số điện thoại", "role": "Vai trò: CUSTOMER hoặc ADMIN", "is_active": "Trạng thái hoạt động",
        "receiver_name": "Tên người nhận", "address_line": "Số nhà, tên đường", "ward, district": "Phường/xã, quận/huyện", "city": "Tỉnh/thành phố",
        "is_default": "Địa chỉ mặc định", "name": "Tên", "slug": "Đường dẫn thân thiện (duy nhất)", "image": "Ảnh", "is_deleted": "Cờ xóa mềm",
        "code": "Mã (duy nhất)", "hex_code": "Mã màu hex", "sort_order": "Thứ tự hiển thị", "category_id": "ID danh mục", "description": "Mô tả",
        "price": "Giá bán", "image, image_thumb": "Ảnh chính và ảnh thu nhỏ", "is_featured": "Sản phẩm nổi bật", "created_at": "Ngày tạo",
        "product_id": "ID sản phẩm", "color_id": "ID màu", "size_id": "ID size", "stock": "Số lượng tồn", "updated_at": "Ngày cập nhật",
        "cart_id": "ID giỏ hàng", "variant_id": "ID biến thể", "quantity": "Số lượng", "discount_type": "Loại giảm: PERCENT hoặc AMOUNT",
        "discount_value": "Giá trị giảm", "max_discount": "Giảm tối đa (với PERCENT)", "min_order_amount": "Giá trị đơn tối thiểu",
        "start_date, end_date": "Ngày bắt đầu, ngày kết thúc", "usage_limit, used_count": "Tổng lượt dùng, số lượt đã dùng", "per_user_limit": "Số lần mỗi khách được dùng",
        "order_code": "Mã đơn hàng (duy nhất)", "voucher_id": "ID mã giảm giá", "receiver_name, receiver_phone": "Tên, số điện thoại người nhận",
        "shipping_address": "Địa chỉ giao hàng", "payment_method": "Phương thức thanh toán", "payment_status": "Trạng thái thanh toán",
        "status": "Trạng thái đơn hàng", "            COMPLETED, CANCELLED)": None, "subtotal, discount_amount": "Tiền hàng, số tiền giảm",
        "shipping_fee, total_amount": "Phí ship, tổng thanh toán", "voucher_code": "Mã voucher lưu tại thời điểm đặt", "cancel_reason, cancelled_by": "Lý do hủy, người hủy",
        "created_at, completed_at": "Ngày tạo, ngày hoàn thành", "order_id": "ID đơn hàng", "product_name, color_name, size_name": "Tên, màu, size lưu tại thời điểm mua",
        "unit_price": "Đơn giá", "line_total": "Thành tiền", "rating": "Số sao (1 đến 5)", "comment": "Nội dung bình luận", "is_hidden": "Bị ẩn",
        "title": "Tiêu đề", "link_url": "Đường dẫn khi bấm", "author_id": "ID tác giả", "summary": "Tóm tắt", "content": "Nội dung",
        "cover_image": "Ảnh bìa", "is_published": "Đã xuất bản", "provider, provider_tx_id": "Nhà cung cấp, mã giao dịch", "amount": "Số tiền",
        "result": "Kết quả xử lý giao dịch"}
for n, rows in S.T.items():
    P(323, VN[n])
    data, desc = [], []
    for k, txt, b in rows:
        nm, _, ty = txt.partition(" : ")
        if not ty:
            if desc and txt.strip().startswith("COMPLETED"):
                data[-1][2] += " " + txt.strip(); continue
            nm, ty = txt, ""
        nm = nm.strip()
        data.append([k, nm, ty]); desc.append([nm, DESC.get(nm, "")])
    t = table_like(324, data, keep_head=1, proto_row=2)
    set_header(t, [n.upper()]); set_widths(t, [900, 3900, 4200])
    P(BODY, "")
    t2 = table_like(333, desc); set_widths(t2, [3600, 5400])
    P(BODY, "")

H1("Thực hiện")
P(426, "Thiết lập Maven và các “Dependency” phù hợp cho dự án vào file “pom.xml”:")
POM = ["<properties>", "    <maven.compiler.release>17</maven.compiler.release>", "</properties>", "<dependencies>",
       "    jakarta.servlet-api 6.1.0 (provided) · jakarta.servlet.jsp-api 4.0.0 (provided)",
       "    jakarta.servlet.jsp.jstl-api 3.0.2 · org.glassfish.web jakarta.servlet.jsp.jstl 3.0.1",
       "    mysql-connector-j 9.1.0 · flyway-core / flyway-mysql 10.20.1",
       "    angus-mail 2.0.3 · jackson-databind 2.15.2 · zxing core 3.5.3",
       "    junit-jupiter 5.10.2 (test)", "</dependencies>", "<build><finalName>saltair</finalName> … packaging war</build>"]
for ln in POM:
    p = OxmlElement("w:p"); ppr = OxmlElement("w:pPr"); sp = OxmlElement("w:spacing"); sp.set(W("before"), "0"); sp.set(W("after"), "0"); ppr.append(sp)
    shd = OxmlElement("w:shd"); shd.set(W("val"), "clear"); shd.set(W("color"), "auto"); shd.set(W("fill"), "2B2B2B"); ppr.append(shd); p.append(ppr)
    r = OxmlElement("w:r"); rp = OxmlElement("w:rPr"); f = OxmlElement("w:rFonts"); f.set(W("ascii"), "Consolas"); f.set(W("hAnsi"), "Consolas"); rp.append(f)
    c = OxmlElement("w:color"); c.set(W("val"), "A9B7C6"); rp.append(c); s = OxmlElement("w:sz"); s.set(W("val"), "18"); rp.append(s); r.append(rp)
    t = OxmlElement("w:t"); t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve"); t.text = ln; r.append(t); p.append(r); add(p)
P(BODY, "")
P(428, "Xây dựng phía quản trị (Admin):")
for name, text in [("Controller (Servlet admin)", "Các Servlet trong controller/admin chia theo catalog (sản phẩm, danh mục, màu và size, thùng rác), sales (đơn hàng, voucher, bình luận) và system (dashboard, người dùng, banner, blog)."),
                   ("Service", "Chứa nghiệp vụ và transaction: ProductService, OrderService, VoucherService, StatisticService, UserService..."),
                   ("DAO", "Toàn bộ câu lệnh SQL dùng PreparedStatement: ProductDAO, OrderDAO, VoucherDAO, StatisticDAO..."),
                   ("Model", "16 lớp ánh xạ bảng (User, Product, ProductVariant, Order, OrderDetail, Voucher...) với getter, setter viết tay."),
                   ("Filter", "AdminFilter chặn /admin và /admin/* nếu không phải tài khoản ADMIN; EncodingFilter đặt UTF-8."),
                   ("Utils", "DBContext (kết nối), DBMigration (Flyway), PasswordUtil (PBKDF2-SHA256), MailUtil, ImageUploadUtil, CloudinaryUtil, PayOSSignature, QrSvg (vẽ mã QR), Page (phân trang)."),
                   ("Constant và Exception", "Role, OrderStatus, PaymentMethod, DiscountType; ValidationException, VoucherException."),
                   ("Giao diện cho admin", "JSP trong WEB-INF/views/admin dùng tag admin-layout.tag và admin.css.")]:
    P(429, name)
    P(BODY, text)
P(455, "Xây dựng phía khách hàng:")
for name, text in [("Controller cho khách hàng", "Servlet theo chức năng auth, account, shop, cart, order; AuthFilter bảo vệ các trang cá nhân."),
                   ("Giao diện cho khách hàng", "JSP trong WEB-INF/views (shop, cart, order, account, auth) dùng tag layout.tag, product-card.tag, pagination.tag."),
                   ("Thư viện hỗ trợ", "HTML5, CSS3, JavaScript thuần (không dùng framework), font Manrope, bộ icon SVG nội bộ; Jackson đọc JSON payOS, ZXing tạo mã QR.")]:
    P(456, name)
    P(BODY, text)

H1("Test case")
TC = {"Form Đăng nhập": [("1", "Đăng nhập đúng email và mật khẩu", "Vào trang chủ (Admin vào /admin)"), ("2", "Nhập sai mật khẩu", "Hiện lại form, báo lỗi"),
                        ("3", "Để trống email hoặc mật khẩu", "Báo lỗi bắt buộc nhập"), ("4", "Tài khoản bị khóa đăng nhập", "Không cho đăng nhập, báo bị khóa"),
                        ("5", "Customer mở /admin", "Lỗi 403"), ("6", "Chưa đăng nhập mở /orders", "Chuyển về trang đăng nhập")],
      "Form Giỏ hàng, Thanh toán": [("7", "Tăng số lượng trong giỏ vượt tồn kho", "Số lượng không vượt quá tồn kho"), ("8", "Đặt hàng COD", "Tạo đơn Chờ xác nhận, trừ tồn kho, xóa giỏ"),
                                   ("9", "Nhập mã WELCOME10 với đơn từ 200.000đ", "Giảm 10% (tối đa 50.000đ), phí ship 0"), ("10", "Nhập mã đã hết hạn hoặc hết lượt", "Báo lỗi đúng thứ tự kiểm tra"),
                                   ("11", "Thanh toán chuyển khoản, payOS gọi webhook", "Đơn chuyển sang đã thanh toán"), ("12", "Webhook sai chữ ký", "Từ chối (401), không đổi trạng thái"), ("12b", "Khách chưa đăng nhập đặt hàng bằng email", "Đơn gắn vào tài khoản theo email")],
      "Test Case Đơn hàng": [("13", "Khách hủy đơn Chờ xác nhận", "Đã hủy, hoàn tồn kho và lượt voucher"), ("14", "Khách hủy đơn Đang giao", "Không cho hủy"),
                             ("15", "Đánh giá sản phẩm khi đơn chưa hoàn thành", "Không cho đánh giá"), ("16", "Admin xác nhận → giao → hoàn thành", "Trạng thái đổi đúng thứ tự")],
      "Test Case Quản lý sản phẩm": [("17", "Thêm sản phẩm giá dưới 1.000đ hoặc không chọn màu", "Báo lỗi từng ô"), ("18", "Xóa mềm rồi khôi phục sản phẩm", "Ẩn khỏi cửa hàng; khôi phục thì hiện lại"),
                                     ("19", "Xóa danh mục còn sản phẩm đang bán", "Bị chặn, có thông báo")],
      "Test Case Thống kê": [("20", "Lọc doanh thu theo khoảng ngày", "Doanh thu chỉ tính đơn Hoàn thành trong khoảng"), ("21", "Xem top sản phẩm bán chạy", "Danh sách sắp xếp theo số lượng bán")]}
for k, rows in TC.items():
    P(466, k)
    t = table_like(502, [[a, b, c, ""] for a, b, c in rows])
    set_header(t, ["TC ID", "MÔ TẢ", "KẾT QUẢ MONG MUỐN", "TRẠNG THÁI"])
    P(BODY, "")

H1("Quy trình làm việc nhóm")
H2("Công cụ và vai trò", 219)
bullets(["Mã nguồn đặt trên một repository GitHub riêng tư; công việc được giao bằng GitHub Issues, mỗi issue gắn nhãn loại (logic, giao-dien) và được giao cho một trong 3 thành viên.",
         "Trưởng nhóm: nhận yêu cầu, phân tích, chia và tạo issue, review, merge Pull Request, phát hành từ develop sang main.",
         "Thành viên: nhận issue theo nhãn của mình, tạo nhánh riêng, code, tự kiểm thử, mở Pull Request.",
         "Tài liệu dùng chung trong repo: tóm tắt dự án, bảng phân công, cẩm nang quy trình phát triển, mẫu issue và mẫu Pull Request."])
STEP = [0]
H2("Quy trình nhận và giao task", 219)
IMG("quy_trinh_giao_task.png", "Quy trình nhận và giao task trên GitHub", 16)
for name, text in [("Nhận yêu cầu", "Ghi lại đúng yêu cầu của giảng viên hoặc người dùng, chưa nghĩ cách làm."),
                   ("Phân tích", "Viết quy tắc nghiệp vụ có con số cụ thể, luồng xử lý, phần ngoài phạm vi; chia thành các issue Logic và Giao diện có thứ tự phụ thuộc."),
                   ("Tạo issue", "Tạo issue bằng mẫu [Logic] hoặc [Giao diện], gắn nhãn, giao người, ghi mục Nghiệm thu đo được (bấm gì, thấy gì)."),
                   ("Code", "Người nhận tạo nhánh feature/<số-issue>-<tên-ngắn> từ develop mới nhất, code đúng luồng Servlet → Service → DAO, commit nhỏ kèm số issue."),
                   ("Kiểm thử", "Tự chạy các bước trong mục Nghiệm thu và unit test; cập nhật develop vào nhánh trước khi mở PR."),
                   ("Pull Request và review", "Mở PR vào develop theo mẫu, ghi Closes #<số-issue>; reviewer kiểm tra phân tích, code, chạy thử; cần sửa thì người làm thêm commit trên cùng nhánh."),
                   ("Merge và nghiệm thu", "Merge bằng merge commit, issue tự đóng, xóa nhánh; hết một giai đoạn thì mở PR release từ develop sang main.")]:
    STEP[0] += 1
    P(LABEL2, "Bước %d. %s:" % (STEP[0], name))
    P(BODY, text)
H2("Mô hình nhánh Git", 219)
IMG("nhanh_git.png", "Mô hình nhánh Git của nhóm", 16)
t = table_like(502, [["main", "Bản ổn định, chỉ nhận PR release từ develop", "develop → main (PR #49, #59)", ""],
                     ["develop", "Nhánh phát triển chính, nhánh mặc định của repo", "Mọi PR tính năng đổ về đây", ""],
                     ["feature/<số>-<tên>", "Chức năng mới theo issue", "feature/12-cart, feature/24-admin-product", ""],
                     ["fix/…", "Sửa lỗi", "fix/bo-form-email-popup", ""],
                     ["chore/…, refactor/…", "Dọn dẹp, cấu hình, đổi cấu trúc code", "chore/bo-che-do-demo, refactor/layered-structure", ""],
                     ["docs/…, content/…", "Tài liệu, dữ liệu nội dung", "docs/quy-trinh-feature, content/lo-02-06", ""]])
set_header(t, ["NHÁNH", "DÙNG CHO", "VÍ DỤ THẬT", ""]); drop_last_col(t)
P(BODY, "")
H2("Quy ước commit và Pull Request", 219)
P(BODY, "Commit theo dạng <loại>(<phạm vi>): <việc đã làm> (#<số-issue>), mỗi commit một ý:")
t = table_like(502, [["feat", "Thêm chức năng", "feat(payment): thanh toán chuyển khoản qua payOS", ""],
                     ["fix", "Sửa lỗi", "fix(auth): chỉnh kiểu trang nhập mã 6 ô và chống cache CSS cũ", ""],
                     ["refactor", "Đổi cấu trúc, không đổi hành vi", "refactor: đổi tầng repository thành dao", ""],
                     ["test", "Thêm hoặc sửa kiểm thử", "test: thêm JUnit 5 và unit test cho SlugUtil, VoucherService", ""],
                     ["docs / chore / content", "Tài liệu, dọn dẹp, dữ liệu", "chore: bỏ chế độ demo (demoMode)", ""]])
set_header(t, ["LOẠI", "Ý NGHĨA", "VÍ DỤ TRONG DỰ ÁN", ""]); drop_last_col(t)
P(BODY, "")
P(LABEL2, "Mẫu issue và Pull Request:")
bullets(["Mẫu [Logic]: phụ thuộc, mô tả, cần làm (dữ liệu, nghiệp vụ, endpoint, cấu hình), nghiệm thu.",
         "Mẫu [Giao diện]: phụ thuộc, tham chiếu, mô tả, bố cục, hiệu ứng, hành vi, nghiệm thu (đúng trên máy tính và điện thoại).",
         "Mẫu Pull Request: tóm tắt, Closes #, đã làm, cách kiểm tra, ảnh chụp màn hình, thay đổi dữ liệu/cấu hình, checklist chung và checklist riêng dự án (đúng luồng Servlet → Service → DAO, SQL dùng PreparedStatement, không commit file bí mật)."])
P(LABEL2, "Quy tắc nhóm:")
bullets(["Mỗi issue một nhánh, tách từ develop.", "Không push thẳng vào develop hoặc main, không tự merge PR của mình.",
         "Kẹt quá 30 phút thì hỏi ngay trong nhóm.", "Sửa file dùng chung (web.xml, style.css, migration) thì ghi rõ trong PR và cập nhật develop trước khi mở PR."])

H1("Bảng phân công")
H2("Phân công theo thành viên", 498)
t = table_like(502, [["Nhân (trưởng nhóm)", "Tài khoản và bảo mật, trang chủ, blog, banner; review, merge, tích hợp, kiểm thử toàn luồng", "#1 đến #6, #9 đến #11, #27", ""],
                     ["Phúc", "Sản phẩm, giỏ hàng, voucher, dữ liệu sản phẩm (danh mục, màu, size, thùng rác)", "#7 #8 #12 #13 #15, #22 đến #25", ""],
                     ["Minh", "Đặt hàng, đơn hàng, đánh giá, bình luận, dashboard thống kê", "#14, #16 đến #21, #26", ""]])
set_header(t, ["THÀNH VIÊN", "MẢNG PHỤ TRÁCH", "ISSUE", ""]); drop_last_col(t)
P(BODY, "")
PH = [("Bảng phân công phase 1 (nền tảng)", [("#1", "Đăng nhập, đăng xuất, đăng ký", "Nhân", "PR #36"), ("#2", "AuthFilter và AdminFilter", "Nhân", "PR #37"),
        ("#7", "Danh sách sản phẩm: phân trang, lọc, tìm kiếm", "Phúc", "PR #34"), ("#8", "Chi tiết sản phẩm", "Phúc", "PR #35"),
        ("#12", "Giỏ hàng", "Phúc", "PR #38"), ("#13", "Kiểm tra mã giảm giá", "Phúc", "PR #39"), ("#22", "Danh mục", "Phúc", "PR #32"), ("#23", "Màu và size", "Phúc", "PR #33")]),
      ("Bảng phân công phase 2", [("#4, #5", "Hồ sơ, đổi mật khẩu, sổ địa chỉ, yêu thích", "Nhân", "PR #40"), ("#9", "Trang chủ và giới thiệu", "Nhân", "PR #46"),
        ("#10", "Blog", "Nhân", "PR #47"), ("#3", "Quên mật khẩu qua email", "Nhân", "PR #48"), ("#14", "Thanh toán COD và đặt hàng", "Minh", "PR #41"),
        ("#16", "Lịch sử và chi tiết đơn", "Minh", "PR #42"), ("#17", "Hủy đơn, xác nhận đã nhận", "Minh", "PR #43"), ("#18", "Đánh giá sản phẩm", "Minh", "PR #44"),
        ("#24", "Sản phẩm, biến thể, tồn kho", "Phúc", "PR #45")]),
      ("Bảng phân công phase 3", [("#6", "Quản lý người dùng", "Nhân", "PR #54"), ("#11", "Quản lý banner", "Nhân", "PR #55"), ("#15", "Quản lý voucher", "Phúc", "PR #53"),
        ("#25", "Thùng rác chung", "Phúc", "PR #56"), ("#19", "Danh sách đơn hàng (Admin)", "Minh", "PR #50"), ("#20", "Cập nhật trạng thái, hủy đơn", "Minh", "PR #51"),
        ("#21", "Quản lý bình luận", "Minh", "PR #52"), ("#26", "Dashboard thống kê", "Minh", "PR #57"), ("#27", "Xóa dữ liệu giả, kiểm thử toàn luồng", "Nhân", "PR #58")])]
for title, rows in PH:
    H2(title, 498)
    t = table_like(502, [[a, b, c, "Đã merge (%s)" % e] for a, b, c, e in rows]); set_widths(t, [1000, 4000, 1800, 2000])
    set_header(t, ["ISSUE", "CHỨC NĂNG", "THÀNH VIÊN", "TÌNH TRẠNG"])
    if title != PH[-1][0]:
        P(BODY, "")

H1("Kết luận")
P(517, "Tóm tắt:")
P(518, "Trong quá trình thực hiện, nhóm đã hoàn thành phân tích, thiết kế và xây dựng website Saltair với đầy đủ chức năng cho khách hàng và quản trị viên: xem và lọc sản phẩm, giỏ hàng, đặt hàng, mã giảm giá, thanh toán COD và chuyển khoản, quản lý đơn hàng, sản phẩm, nội dung và thống kê doanh thu.")
P(517, "Hạn chế:")
P(518, "Chưa triển khai lên máy chủ thật; chưa có đổi trả hàng, đăng nhập bằng mạng xã hội và xuất báo cáo Excel.")
P(517, "Hướng khắc phục:")
bullets(["Triển khai website lên máy chủ và tên miền thật, bổ sung HTTPS.", "Tìm hiểu thêm nghiệp vụ bán hàng, chăm sóc khách hàng để bổ sung đổi trả, tích điểm.",
         "Tiếp tục viết thêm kiểm thử tự động cho các chức năng chính."], 522)
P(518, "Nhóm em xin chân thành cảm ơn thầy/cô đã tận tình hướng dẫn nhóm trong suốt quá trình thực hiện dự án.")

# ================================================================== GHEP LAI
for i in (68, 71):
    ppr = B[i].find(W("pPr")); ppr.insert(1, OxmlElement("w:pageBreakBefore"))
set_text(B[543], "TP.HCM, ngày …/…/2026")
keep_head = B[0:1]
toc_title = B[1:3]          # "Muc luc" + "Contents"
toc_proto1, toc_proto2 = B[7], B[8]
front = B[56:74]            # Loi mo dau + 2 trang Nhan xet
tail = B[525:547]           # Nhan xet cua GVHD
for e in list(body):
    body.remove(e)
ppr = toc_title[0].find(W("pPr")); npr = OxmlElement("w:numPr"); il = OxmlElement("w:ilvl"); il.set(W("val"), "0"); ni = OxmlElement("w:numId"); ni.set(W("val"), "0")
npr.append(il); npr.append(ni); ppr.append(npr) if ppr.find(W("numPr")) is None else None
for e in keep_head + toc_title:
    body.append(e)


# muc luc: clone dong TOC1/TOC2 cua mau
def toc_line(proto, text, page, first=False, last=False):
    p = copy.deepcopy(proto); rp = first_rpr(proto)
    for ch in list(p):
        if ch.tag != W("pPr"):
            p.remove(ch)

    def run(child):
        r = OxmlElement("w:r")
        if rp is not None:
            r.append(copy.deepcopy(rp))
        r.append(child); p.append(r)
    if first:
        f = OxmlElement("w:fldChar"); f.set(W("fldCharType"), "begin"); run(f)
        it = OxmlElement("w:instrText"); it.set("{http://www.w3.org/XML/1998/namespace}space", "preserve"); it.text = ' TOC \\o "1-3" \\h \\z \\u '; run(it)
        f = OxmlElement("w:fldChar"); f.set(W("fldCharType"), "separate"); run(f)
    t = OxmlElement("w:t"); t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve"); t.text = text; run(t)
    run(OxmlElement("w:tab"))
    t = OxmlElement("w:t"); t.text = str(page); run(t)
    if last:
        f = OxmlElement("w:fldChar"); f.set(W("fldCharType"), "end"); run(f)
    return p


ents = [("Mục lục", 1), ("Lời mở đầu", 1), ("Nhận xét", 1), ("Nhận xét", 1)]
c = [0, 0, 0]
for lv, t in HEADS:
    if lv == 0:
        ents.append((t.upper(), 1)); continue
    if lv == 1:
        c[0] += 1; c[1] = c[2] = 0; ents.append(("%d. %s" % (c[0], t.upper()), 1))
    elif lv == 2:
        c[1] += 1; c[2] = 0; ents.append(("%d.%d %s" % (c[0], c[1], t), 2))
    else:
        c[2] += 1; ents.append(("%d.%d.%d %s" % (c[0], c[1], c[2], t), 2))
for k, (t, lv) in enumerate(ents):
    body.append(toc_line(toc_proto1 if lv == 1 else toc_proto2, t, (PAGES[k] if k < len(PAGES) else ""), k == 0, k == len(ents) - 1))
pbp = OxmlElement("w:p"); pr_ = OxmlElement("w:r"); bb = OxmlElement("w:br"); bb.set(W("type"), "page"); pr_.append(bb); pbp.append(pr_); body.append(pbp)
for e in front + NEW + tail:
    body.append(e)
body.append(SECT)
for p, path, w in IMAGES:
    Paragraph(p, d._body).add_run().add_picture(path, width=Cm(w))
st = d.settings.element
u = OxmlElement("w:updateFields"); u.set(W("val"), "true"); st.append(u)
d.save(OUT)
json.dump([t for t, _ in ents], open(OUT + ".toc.json", "w"), ensure_ascii=False)
print(OUT, len(ents))
