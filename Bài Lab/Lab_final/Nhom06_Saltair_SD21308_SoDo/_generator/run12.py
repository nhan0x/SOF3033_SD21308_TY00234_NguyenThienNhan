# -*- coding: utf-8 -*-
import sys
sys.path.insert(0, ".")
from dg import *
import lab12 as L

OUT = "/private/tmp/claude-501/-Users-nguyenluke-Documents-FPOLY/9866e0ff-6f23-4f53-a620-ee2f8d5b88e2/scratchpad/out"
p1 = [L.l1_deploy(), L.l1_uc_nv(), L.l1_uc_ql()]
n1 = ["lab1_b2_trien_khai", "lab1_b3_usecase_nhanvien", "lab1_b4_usecase_quanly"]
save_pages(p1, OUT + "/lab1/Lab_1.drawio"); export_png(OUT + "/lab1/Lab_1.drawio", OUT + "/lab1", n1)
p2 = [L.erd1(), L.erd2(), L.deploy2(), L.flow()]
n2 = ["lab2_erd_level1", "lab2_erd_level2", "lab2_b4_trien_khai", "lab2_b4_luu_do"]
save_pages(p2, OUT + "/lab2/Lab_2.drawio"); export_png(OUT + "/lab2/Lab_2.drawio", OUT + "/lab2", n2)
print("ok")
