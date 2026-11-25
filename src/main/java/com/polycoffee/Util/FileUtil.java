package com.polycoffee.Util;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Lưu và xóa ảnh đồ uống trong thư mục /images của webapp (BR-14).
 * Lưu ý: ảnh nằm trong thư mục triển khai nên sẽ mất khi deploy lại file WAR (R-06).
 */
public final class FileUtil {

    private static final String FOLDER = "/images";

    private FileUtil() {
    }

    /**
     * Lưu file upload với tên ngẫu nhiên. Trả tên file mới, hoặc {@code null} nếu không có file.
     *
     * @throws IOException nếu không phải file ảnh jpg, jpeg, png, gif, webp
     */
    public static String save(ServletContext ctx, Part part) throws IOException {
        if (part == null || part.getSize() == 0) {
            return null;
        }
        String submitted = part.getSubmittedFileName();
        String ext = submitted != null && submitted.contains(".")
                ? submitted.substring(submitted.lastIndexOf('.')).toLowerCase() : "";
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            throw new IOException("Chỉ chấp nhận file ảnh (jpg, png, gif, webp)");
        }
        String dir = ctx.getRealPath(FOLDER);
        if (dir == null) {
            throw new IOException("Không xác định được thư mục lưu ảnh");
        }
        new File(dir).mkdirs();
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        part.write(dir + File.separator + name);
        return name;
    }

    /** Xóa ảnh đã lưu. Bỏ qua tên rỗng hoặc có dấu hiệu thoát thư mục. */
    public static void delete(ServletContext ctx, String fileName) {
        if (fileName == null || fileName.isEmpty()
                || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return;
        }
        String dir = ctx.getRealPath(FOLDER);
        if (dir == null) {
            return;
        }
        File f = new File(dir, fileName);
        if (f.isFile()) {
            f.delete();
        }
    }
}
