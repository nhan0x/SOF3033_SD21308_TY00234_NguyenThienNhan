package com.fashionweb.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrSvgTest {

    private static final String VIETQR = "00020101021238570010A000000727012700069704220113VQRQANRGZ34660208QRIBFTTA5303704540652900058020 VN6304ABCD";

    @Test
    @DisplayName("Dựng được SVG hợp lệ: có viewBox vuông, nền trắng và path các ô đen")
    void rendersSvg() {
        String svg = QrSvg.render(VIETQR);
        assertTrue(svg.startsWith("<svg "));
        assertTrue(svg.endsWith("</svg>"));
        assertTrue(svg.contains("<path fill=\"#000e1f\" d=\"M"));
        // viewBox vuông: hai cạnh bằng nhau
        String[] box = svg.replaceAll(".*viewBox=\"0 0 (\\d+) (\\d+)\".*", "$1 $2").split(" ");
        assertEquals(box[0], box[1]);
    }

    @Test
    @DisplayName("Cùng nội dung cho cùng ảnh, nội dung khác cho ảnh khác")
    void deterministic() {
        assertEquals(QrSvg.render(VIETQR), QrSvg.render(VIETQR));
        assertTrue(!QrSvg.render(VIETQR).equals(QrSvg.render(VIETQR + "1")));
    }

    @Test
    @DisplayName("Nội dung rỗng hoặc null: báo lỗi")
    void rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> QrSvg.render(""));
        assertThrows(IllegalArgumentException.class, () -> QrSvg.render(null));
    }
}
