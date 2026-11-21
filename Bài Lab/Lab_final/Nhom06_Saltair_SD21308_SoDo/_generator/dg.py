# -*- coding: utf-8 -*-
"""Thu vien ve so do (draw.io XML) + xuat PNG bang drawio CLI."""
import html, os, subprocess

FONT = "Helvetica"
BLUE = "#036897"
LINE = "#4D4D4D"


def esc(s):
    return html.escape(s, quote=True).replace("\n", "&lt;br&gt;")


def rich(s):
    """Giu nguyen the HTML don gian (<b>, <br>) trong value."""
    return html.escape(s, quote=True)


class Dia:
    def __init__(self, name, w=1200, h=800):
        self.name, self.w, self.h = name, w, h
        self.cells = []
        self.n = 1

    def _id(self):
        self.n += 1
        return "c%d" % self.n

    def box(self, x, y, w, h, label="", style="", html_label=True):
        i = self._id()
        base = "html=1;whiteSpace=wrap;fontFamily=%s;" % FONT
        self.cells.append(
            '<mxCell id="%s" value="%s" style="%s" vertex="1" parent="1">'
            '<mxGeometry x="%s" y="%s" width="%s" height="%s" as="geometry"/></mxCell>'
            % (i, rich(label), base + style, x, y, w, h)
        )
        return i

    def text(self, x, y, w, h, label, size=12, bold=False, align="center", color="#222222", valign="middle", extra=""):
        st = "text;strokeColor=none;fillColor=none;align=%s;verticalAlign=%s;fontSize=%d;fontColor=%s;%s%s" % (
            align, valign, size, color, "fontStyle=1;" if bold else "", extra)
        return self.box(x, y, w, h, label, st)

    def edge(self, a, b, label="", style="", pts=None, src_pt=None, tgt_pt=None):
        i = self._id()
        base = "html=1;fontFamily=%s;strokeColor=%s;" % (FONT, LINE)
        geo = '<mxGeometry relative="1" as="geometry">'
        if src_pt:
            geo += '<mxPoint x="%s" y="%s" as="sourcePoint"/>' % src_pt
        if tgt_pt:
            geo += '<mxPoint x="%s" y="%s" as="targetPoint"/>' % tgt_pt
        if pts:
            geo += "<Array as=\"points\">" + "".join('<mxPoint x="%s" y="%s"/>' % p for p in pts) + "</Array>"
        geo += "</mxGeometry>"
        sa = ' source="%s"' % a if a else ""
        tb = ' target="%s"' % b if b else ""
        self.cells.append(
            '<mxCell id="%s" value="%s" style="%s" edge="1" parent="1"%s%s>%s</mxCell>'
            % (i, rich(label), base + style, sa, tb, geo)
        )
        return i

    def xml(self):
        return (
            '<diagram name="%s" id="%s"><mxGraphModel dx="0" dy="0" grid="0" gridSize="10" guides="1" tooltips="1" '
            'connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="%d" pageHeight="%d" '
            'background="#FFFFFF" math="0" shadow="0"><root><mxCell id="0"/><mxCell id="1" parent="0"/>%s'
            "</root></mxGraphModel></diagram>" % (esc(self.name), "p" + str(abs(hash(self.name)) % 99999), self.w, self.h, "".join(self.cells))
        )


def save_pages(pages, path_drawio):
    xml = "<mxfile>" + "".join(p.xml() for p in pages) + "</mxfile>"
    os.makedirs(os.path.dirname(path_drawio), exist_ok=True)
    open(path_drawio, "w", encoding="utf8").write(xml)


def export_png(path_drawio, outdir, names, scale=2):
    """names: list ten file (khong .png) theo thu tu trang."""
    os.makedirs(outdir, exist_ok=True)
    for idx, nm in enumerate(names):
        out = os.path.join(outdir, nm + ".png")
        subprocess.run(["drawio", "-x", "-f", "png", "-s", str(scale), "-b", "24", "-p", str(idx + 1), "-o", out, path_drawio],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)


# ---------------------------------------------------------------- USE CASE
UC_STYLE = "ellipse;strokeColor=%s;strokeWidth=1.4;fillColor=#FFFFFF;fontSize=13;fontColor=#111111;" % LINE
DASH = ("dashed=1;dashPattern=5 4;endArrow=open;endFill=0;endSize=9;strokeWidth=1.3;fontSize=11;fontStyle=1;"
        "labelBackgroundColor=#FFFFFF;fontColor=#222222;")


def boundary(d, x, y, w, h, title, size=18):
    d.box(x, y, w, h, "", "rounded=1;arcSize=1.5;strokeColor=%s;strokeWidth=1.6;fillColor=none;" % LINE)
    d.text(x, y + 6, w, 34, title, size=size, bold=False)


def actor(d, cx, cy, label, color="#222222", top=False):
    pos = "verticalLabelPosition=top;verticalAlign=bottom;" if top else "verticalLabelPosition=bottom;verticalAlign=top;"
    return d.box(cx - 20, cy - 40, 40, 80, label, "shape=umlActor;" + pos +
                 "strokeColor=%s;strokeWidth=1.5;fillColor=#FFFFFF;fontSize=14;fontStyle=1;fontColor=%s;" % (LINE, color))


def uc(d, cx, cy, label, w=210, h=64, fill="#FFFFFF"):
    return d.box(cx - w / 2, cy - h / 2, w, h, label, UC_STYLE.replace("#FFFFFF", fill))


def assoc(d, a, b):
    return d.edge(a, b, "", "endArrow=none;strokeWidth=1.3;")


def include(d, base, inc, label="include"):
    return d.edge(base, inc, label, DASH)


def extend(d, ext, base, label="extend"):
    return d.edge(ext, base, label, DASH)


def inherit(d, child, parent):
    return d.edge(child, parent, "", "endArrow=block;endFill=0;endSize=14;strokeWidth=1.5;")


def note(d, x, y, w, h, text, size=12):
    return d.box(x, y, w, h, text, "shape=note;size=14;strokeColor=%s;fillColor=#FFFFFF;fontSize=%d;align=left;"
                 "spacingLeft=8;spacingRight=6;verticalAlign=middle;" % (LINE, size))


def note_link(d, a, b):
    return d.edge(a, b, "", "endArrow=none;dashed=1;strokeWidth=1.2;")


# ---------------------------------------------------------------- ERD
HDR = "#D9D9D9"


def table(d, x, y, name, rows, w=250, rh=22, hh=26, name_size=13):
    """rows: list (kind, text, bold)  kind in 'PK','FK','' . Tra ve id cua khung bao (de noi canh)."""
    h = hh + rh * len(rows) + 6
    outer = d.box(x, y, w, h, "", "strokeColor=#222222;strokeWidth=1.2;fillColor=#FFFFFF;")
    d.box(x, y, w, hh, "<b>%s</b>" % name, "strokeColor=#222222;strokeWidth=1.2;fillColor=%s;fontSize=%d;" % (HDR, name_size))
    cy = y + hh + 3
    for kind, text, bold in rows:
        d.text(x + 2, cy, 34, rh, ("<b>%s</b>" % kind) if kind else "", size=11, align="center")
        t = ("<b>%s</b>" % text) if bold else text
        if kind == "PK":
            t = "<u>%s</u>" % t
        d.text(x + 36, cy, w - 40, rh, t, size=11, align="left")
        cy += rh
    return outer, h


def fk_edge(d, child, parent, style_extra="", label="", pts=None):
    st = ("endArrow=ERmandOne;startArrow=ERmany;startSize=12;endSize=12;strokeWidth=1.4;"
          "edgeStyle=orthogonalEdgeStyle;rounded=0;fontSize=11;labelBackgroundColor=#FFFFFF;") + style_extra
    return d.edge(child, parent, label, st, pts=pts)


def arc_x(ax, ay, R, y, side=1):
    """Toa do x tren cung tron tam (ax, ay) ban kinh R: cac elip noi voi tac nhan khong che duong noi cua nhau."""
    import math
    return ax + side * math.sqrt(max(R * R - (y - ay) ** 2, 0))
