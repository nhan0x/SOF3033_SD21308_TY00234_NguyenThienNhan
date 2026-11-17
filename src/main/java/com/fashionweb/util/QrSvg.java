package com.fashionweb.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Map;

/**
 * Vẽ mã QR thành ảnh SVG (một thẻ svg, một path) để nhúng thẳng vào trang, không cần file ảnh hay dịch vụ ngoài.
 * Dùng cho chuỗi VietQR mà payOS trả về: app ngân hàng quét ra đúng tài khoản, số tiền và nội dung.
 */
public final class QrSvg {

    /** Viền trắng quanh mã, tính theo số ô. Khung chứa trên trang còn có thêm lề trắng nên 2 ô là đủ cho máy quét. */
    private static final int QUIET_ZONE = 2;

    private QrSvg() {
    }

    /**
     * @param content nội dung cần mã hóa (không rỗng)
     * @return chuỗi SVG, co giãn theo khung chứa (viewBox), nền trắng, ô đen màu navy của Saltair
     */
    public static String render(String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Nội dung mã QR trống");
        }
        BitMatrix matrix;
        try {
            // Mức sửa lỗi M: đủ để quét tốt khi màn hình bẩn hoặc chói, mà mã vẫn thưa, dễ đọc
            matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, 0,
                            EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalArgumentException("Không tạo được mã QR", e);
        }
        int size = matrix.getWidth() + QUIET_ZONE * 2;
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < matrix.getHeight(); y++) {
            int x = 0;
            while (x < matrix.getWidth()) {
                if (!matrix.get(x, y)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < matrix.getWidth() && matrix.get(x, y)) {
                    x++;
                }
                // Gộp các ô đen liền nhau trên một hàng thành một hình chữ nhật: m(x,y) h(dài) v1 h(-dài) z
                path.append('M').append(start + QUIET_ZONE).append(' ').append(y + QUIET_ZONE)
                        .append('h').append(x - start).append("v1h").append(start - x).append('z');
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + size + ' ' + size
                + "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"Mã QR chuyển khoản\">"
                + "<rect width=\"" + size + "\" height=\"" + size + "\" fill=\"#fff\"/>"
                + "<path fill=\"#000e1f\" d=\"" + path + "\"/></svg>";
    }
}
