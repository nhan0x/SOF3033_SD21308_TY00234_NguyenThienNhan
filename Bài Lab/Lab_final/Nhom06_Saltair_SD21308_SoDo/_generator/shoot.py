# -*- coding: utf-8 -*-
"""Chup man hinh Saltair bang chromedriver (WebDriver REST, khong can selenium)."""
import json, base64, subprocess, time, urllib.request, os, sys

DRV = os.path.expanduser("~/.cache/selenium/chromedriver/mac-arm64/157.0.8103.78/chromedriver")
BASE = "http://localhost:8080/saltair"
OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
proc = subprocess.Popen([DRV, "--port=9515"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
time.sleep(1.5)


def req(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request("http://localhost:9515" + path, data=data, method=method, headers={"Content-Type": "application/json"})
    return json.loads(urllib.request.urlopen(r, timeout=120).read())["value"]


def session():
    caps = {"capabilities": {"alwaysMatch": {"browserName": "chrome", "goog:chromeOptions": {
        "args": ["--headless=new", "--window-size=1440,900", "--hide-scrollbars", "--force-device-scale-factor=1", "--lang=vi-VN"]}}}}
    return req("POST", "/session", caps)["sessionId"]


def go(s, path, wait=2.5):
    req("POST", "/session/%s/url" % s, {"url": BASE + path}); time.sleep(wait)


def js(s, script, *args):
    return req("POST", "/session/%s/execute/sync" % s, {"script": script, "args": list(args)})


def shot(s, name, scroll=0):
    js(s, "window.scrollTo(0, arguments[0]);", scroll); time.sleep(0.8)
    png = base64.b64decode(req("GET", "/session/%s/screenshot" % s))
    open(os.path.join(OUT, name + ".png"), "wb").write(png); print("chup", name)


def login(s, email, pw):
    go(s, "/login")
    js(s, "var f=document.querySelector('form.auth-form');f.email.value=arguments[0];f.password.value=arguments[1];f.submit();", email, pw)
    time.sleep(3)


try:
    g = session()
    go(g, "/home", 5); shot(g, "kh_trang_chu"); shot(g, "kh_trang_chu_2", 1800)
    js(g, "window.scrollTo(0, document.body.scrollHeight);"); time.sleep(1); shot(g, "kh_footer", 100000)
    go(g, "/products"); shot(g, "kh_san_pham")
    go(g, "/product?slug=ao-thun-co-tron"); shot(g, "kh_chi_tiet")
    go(g, "/blog"); shot(g, "kh_blog")
    go(g, "/login"); shot(g, "kh_dang_nhap")
    go(g, "/register"); shot(g, "kh_dang_ky")
    req("DELETE", "/session/" + g)

    c = session()
    login(c, "khach@saltair.vn", "Khach@123")
    go(c, "/cart"); shot(c, "kh_gio_hang")
    go(c, "/checkout"); shot(c, "kh_thanh_toan")
    go(c, "/profile"); shot(c, "kh_tai_khoan")
    go(c, "/addresses"); shot(c, "kh_so_dia_chi")
    go(c, "/wishlist"); shot(c, "kh_yeu_thich")
    go(c, "/orders"); shot(c, "kh_don_hang")
    href = js(c, "var a=document.querySelector('a[href*=\"order?code=\"]');return a?a.getAttribute('href'):null;")
    if href:
        go(c, href.replace("/saltair", "", 1)); shot(c, "kh_chi_tiet_don")
    req("DELETE", "/session/" + c)

    a = session()
    login(a, "admin@saltair.vn", "Admin@123")
    for path, name in [("/admin", "ad_dashboard"), ("/admin/products", "ad_san_pham"), ("/admin/products/form?id=1", "ad_form_san_pham"),
                       ("/admin/categories", "ad_danh_muc"), ("/admin/attributes", "ad_mau_size"), ("/admin/orders", "ad_don_hang"),
                       ("/admin/vouchers", "ad_voucher"), ("/admin/users", "ad_nguoi_dung"), ("/admin/reviews", "ad_binh_luan"),
                       ("/admin/banners", "ad_banner"), ("/admin/blogs", "ad_blog"), ("/admin/trash", "ad_thung_rac")]:
        go(a, path); shot(a, name)
    href = js(a, "var x=document.querySelector('a[href*=\"admin/order?code=\"]');return x?x.getAttribute('href'):null;")
    if href:
        go(a, href.replace("/saltair", "", 1)); shot(a, "ad_chi_tiet_don")
    req("DELETE", "/session/" + a)
finally:
    proc.terminate()
