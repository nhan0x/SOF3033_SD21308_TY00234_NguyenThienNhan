# -*- coding: utf-8 -*-
"""So do quy trinh giao task qua GitHub va mo hinh nhanh Git cua nhom Saltair."""
import sys
sys.path.insert(0, ".")
from dg import *

AR = "endArrow=block;endFill=1;strokeWidth=1.6;fontSize=12;fontStyle=1;labelBackgroundColor=#FFFFFF;edgeStyle=orthogonalEdgeStyle;rounded=1;"


def quy_trinh():
    d = Dia("Quy trinh giao task", 1500, 980)
    d.text(40, 6, 1420, 30, "<b>QUY TRÌNH NHẬN VÀ GIAO TASK TRÊN GITHUB</b> – từ yêu cầu tới merge", size=18)
    lanes = [("Trưởng nhóm", 40, 330, "#FCE4D6"), ("Thành viên nhận issue", 370, 560, "#DCEBFA"), ("GitHub", 930, 260, "#EDEDED"),
             ("Reviewer / Trưởng nhóm", 1190, 270, "#E3F4E6")]
    for n, x, w, c in lanes:
        d.box(x, 50, w, 900, "", "strokeColor=#888;fillColor=none;")
        d.box(x, 50, w, 36, "<b>%s</b>" % n, "strokeColor=#888;fillColor=%s;fontSize=14;" % c)
    pr = "rounded=1;arcSize=14;strokeColor=%s;strokeWidth=1.4;fillColor=#F5FAFD;fontSize=12;" % BLUE
    gh = "rounded=1;arcSize=14;strokeColor=#555;strokeWidth=1.4;fillColor=#FFFFFF;fontSize=12;"
    dc = "rhombus;strokeColor=#B26B00;strokeWidth=1.4;fillColor=#FFF4DE;fontSize=12;"
    s = d.box(191, 110, 28, 28, "", "ellipse;fillColor=#222;strokeColor=#222;")
    a1 = d.box(75, 160, 260, 56, "<b>1. Nhận yêu cầu</b><br>đề bài, góp ý của giảng viên", pr)
    a2 = d.box(75, 245, 260, 70, "<b>2. Phân tích</b><br>quy tắc nghiệp vụ, chia thành<br>issue Logic / Giao diện", pr)
    a3 = d.box(75, 345, 260, 84, "<b>3. Tạo issue</b> theo mẫu<br>gắn nhãn (logic, giao-dien)<br>giao người, ghi Nghiệm thu", pr)
    g1 = d.box(950, 245, 220, 64, "Issue <b>#12</b> Open<br>[Logic] Giỏ hàng", gh)
    b1 = d.box(400, 355, 300, 64, "<b>4. Nhận issue</b>, đọc mục Nghiệm thu<br>git checkout develop · git pull", pr)
    b2 = d.box(400, 450, 300, 56, "<b>5. Tạo nhánh</b> từ develop<br>feature/12-cart", pr)
    b3 = d.box(400, 535, 300, 70, "<b>6. Code + commit nhỏ</b><br>feat(cart): giỏ hàng thật trong DB<br>Servlet → Service → DAO", pr)
    b4 = d.box(400, 635, 300, 56, "<b>7. Tự test</b> theo Nghiệm thu,<br>pull develop, git push", pr)
    b5 = d.box(720, 635, 190, 56, "<b>8. Mở Pull Request</b><br>vào develop, Closes #12", pr)
    g2 = d.box(950, 635, 220, 56, "PR <b>#38</b> Open<br>feature/12-cart → develop", gh)
    r1 = d.box(1215, 620, 220, 86, "<b>9. Review</b><br>đúng luồng tầng?<br>PreparedStatement? test?", dc)
    r2 = d.box(1215, 760, 220, 64, "<b>10. Merge commit</b><br>vào develop, xóa nhánh", pr)
    g3 = d.box(950, 760, 220, 64, "Issue #12 <b>tự đóng</b><br>PR #38 Merged", gh)
    r3 = d.box(1215, 860, 220, 64, "<b>11. Release</b> theo giai đoạn<br>PR develop → main", pr)
    e = d.box(1032, 878, 28, 28, "", "ellipse;shape=doubleEllipse;fillColor=#222;strokeColor=#222;")
    for x, y, l in ((s, a1, ""), (a1, a2, ""), (a2, a3, ""), (b1, b2, ""), (b2, b3, ""), (b3, b4, ""),
                    (b4, b5, ""), (b5, g2, ""), (g2, r1, ""), (r1, r2, "Approve"), (r2, g3, ""), (r2, r3, ""), (r3, e, "")):
        d.edge(x, y, l, AR)
    d.edge(r1, b3, "Cần sửa", AR + "dashed=1;strokeColor=#B00020;fontColor=#B00020;", pts=[(1325, 590), (760, 590), (760, 570)])
    d.edge(a3, g1, "tạo", AR, pts=[(355, 387), (355, 277)])
    d.edge(g1, b1, "giao", AR, pts=[(1060, 387)])
    note(d, 75, 470, 260, 200, "<b>Quy tắc nhóm</b><br>• Mỗi issue một nhánh<br>• Không push thẳng develop, main<br>• Không tự merge PR của mình<br>"
         "• Kẹt quá 30 phút thì hỏi nhóm<br>• File dùng chung (web.xml, style.css…) ghi rõ trong PR", 12)
    return d


def nhanh_git():
    d = Dia("Mo hinh nhanh Git", 1500, 560)
    d.text(40, 6, 1420, 30, "<b>MÔ HÌNH NHÁNH GIT</b> – main · develop · feature/&lt;số-issue&gt;-&lt;tên&gt;", size=18)
    rows = {"main": (110, "#2E7D32"), "develop": (210, BLUE), "feature/12-cart": (310, "#B26B00"), "feature/14-checkout": (400, "#7B1FA2")}
    for n, (y, c) in rows.items():
        d.text(30, y - 14, 170, 28, "<b>%s</b>" % n, size=13, align="right", color=c)
        d.edge(None, None, "", "endArrow=none;strokeWidth=3;strokeColor=%s;" % c, src_pt=(215, y), tgt_pt=(1440, y))
    def dot(x, y, c, lbl="", pos="top"):
        i = d.box(x - 11, y - 11, 22, 22, "", "ellipse;fillColor=%s;strokeColor=#FFFFFF;strokeWidth=2;" % c)
        if lbl:
            d.text(x - 110, y - 46 if pos == "top" else y + 14, 220, 32, lbl, size=11, valign="bottom" if pos == "top" else "top")
        return i
    M, Dv, F1, F2 = (rows[k] for k in rows)
    m0 = dot(250, M[0], M[1], "khởi tạo")
    d0 = dot(320, Dv[0], Dv[1], "", "bottom")
    d.edge(m0, d0, "", "endArrow=block;strokeWidth=2;strokeColor=%s;" % Dv[1])
    f1a = dot(520, F1[0], F1[1], "feat(cart): giỏ hàng thật trong DB", "bottom"); d.edge(d0, f1a, "checkout -b", "endArrow=block;strokeWidth=2;fontSize=11;labelBackgroundColor=#FFFFFF;strokeColor=%s;" % F1[1])
    d1 = dot(720, Dv[0], Dv[1], "PR #38 merge", "top")
    d.edge(f1a, d1, "", "endArrow=block;strokeWidth=2;strokeColor=%s;" % F1[1])
    f2a = dot(560, F2[0], F2[1], "feat(order): thanh toán COD và đặt hàng", "bottom"); d.edge(d0, f2a, "", "endArrow=block;strokeWidth=2;strokeColor=%s;" % F2[1], pts=[(345, 400)])
    f2b = dot(820, F2[0], F2[1], "git pull origin develop", "bottom"); d.edge(d1, f2b, "", "endArrow=block;dashed=1;strokeWidth=1.5;strokeColor=%s;" % Dv[1])
    d2 = dot(960, Dv[0], Dv[1], "PR #41 merge", "top"); d.edge(f2b, d2, "", "endArrow=block;strokeWidth=2;strokeColor=%s;" % F2[1])
    m1 = dot(1080, M[0], M[1], "PR #49 release giai đoạn 1, 2"); d.edge(d2, m1, "", "endArrow=block;strokeWidth=2;strokeColor=%s;" % M[1])
    d3 = dot(1220, Dv[0], Dv[1], "…phase 3", "top")
    m2 = dot(1340, M[0], M[1], "PR #59 release giai đoạn 3"); d.edge(d3, m2, "", "endArrow=block;strokeWidth=2;strokeColor=%s;" % M[1])
    note(d, 40, 490, 1400, 50, "<b>main</b>: bản ổn định, chỉ nhận PR release từ develop · <b>develop</b>: nhánh phát triển chính, mọi PR tính năng đổ về đây · "
         "<b>feature/…</b>: mỗi issue một nhánh, merge xong thì xóa. Ngoài ra: fix/…, chore/…, docs/…, refactor/…, content/…", 12)
    return d


if __name__ == "__main__":
    OUT = "/private/tmp/claude-501/-Users-nguyenluke-Documents-FPOLY/9866e0ff-6f23-4f53-a620-ee2f8d5b88e2/scratchpad/out/gd1"
    save_pages([quy_trinh(), nhanh_git()], OUT + "/Saltair_QuyTrinh.drawio")
    export_png(OUT + "/Saltair_QuyTrinh.drawio", OUT, ["quy_trinh_giao_task", "nhanh_git"])
    print("ok")
