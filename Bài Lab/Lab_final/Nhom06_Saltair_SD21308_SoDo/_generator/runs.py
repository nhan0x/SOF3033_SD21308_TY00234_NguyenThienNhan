# -*- coding: utf-8 -*-
import sys
sys.path.insert(0, ".")
from dg import *
import saltair as S

OUT = "/private/tmp/claude-501/-Users-nguyenluke-Documents-FPOLY/9866e0ff-6f23-4f53-a620-ee2f8d5b88e2/scratchpad/out/gd1"
pages = [S.uc_tong_quat(), S.uc_customer(), S.uc_admin(), S.deploy(), S.arch(), S.erd(), S.cls(), S.state(), S.activity()]
names = ["uc_tong_quat", "uc_customer", "uc_admin", "trien_khai", "kien_truc", "erd", "class", "trang_thai_don", "hoat_dong_dat_hang"]
save_pages(pages, OUT + "/Saltair_SoDo.drawio")
export_png(OUT + "/Saltair_SoDo.drawio", OUT, names)
print("ok")
