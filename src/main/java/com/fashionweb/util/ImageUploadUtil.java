package com.fashionweb.util;

import com.fashionweb.exception.ValidationException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Lưu ảnh tải lên (JPG, PNG, WebP, tối đa 2MB) cho danh mục, sản phẩm, banner, ảnh bìa blog.
 *
 * <p>Có cloudinary.properties (xem CloudinaryUtil): ảnh lên Cloudinary, DB lưu URL https đã kèm phép thu nhỏ và nén
 * (tối đa 1600px); không mất khi deploy lại và máy nào cũng thấy.
 * <p>Không có: ảnh lưu vào thư mục assets/images/... của bản web đang chạy (deploy lại sạch thì mất ảnh đã tải lên).
 */
public final class ImageUploadUtil {

    public static final long MAX_BYTES = 2L * 1024 * 1024;

    /** Ảnh chính: thu nhỏ tối đa 1600px, tự nén, tự chọn định dạng nhẹ cho trình duyệt. */
    private static final String MAIN = "c_limit,w_1600,q_auto,f_auto";
    /** Ảnh nhỏ cho lưới sản phẩm, giỏ hàng: tối đa 600px. */
    private static final String THUMB = "c_limit,w_600,q_auto,f_auto";

    private ImageUploadUtil() {
    }

    /** Có chọn file không. */
    public static boolean hasFile(Part part) {
        return part != null && part.getSize() > 0;
    }

    /**
     * Kiểm tra rồi lưu ảnh, trả chuỗi để lưu DB: URL Cloudinary (khi đã cấu hình) hoặc đường dẫn tương đối
     * (ví dụ assets/images/blogs/ten-123.jpg). Cloudinary lỗi (sai khóa, mất mạng) thì báo lỗi ở ô field và ghi log.
     *
     * @param field tên trường form, dùng khi báo lỗi
     * @param dir   thư mục tương đối so với gốc web, ví dụ "assets/images/blogs"
     * @param base  phần đầu tên file (thường là slug)
     */
    public static String store(ServletContext ctx, Part part, String field, String dir, String base) throws IOException {
        String ext = switch (part.getContentType() == null ? "" : part.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> null;
        };
        if (ext == null) {
            throw new ValidationException(field, "Chỉ nhận ảnh JPG, PNG hoặc WebP.");
        }
        if (part.getSize() > MAX_BYTES) {
            throw new ValidationException(field, "Ảnh tối đa 2MB.");
        }
        String safeBase = SlugUtil.toSlug(base == null ? "" : base);
        String fileName = (safeBase.isEmpty() ? "image" : safeBase) + "-" + System.currentTimeMillis() + ext;
        if (CloudinaryUtil.isConfigured()) {
            String sub = dir.substring(dir.lastIndexOf('/') + 1);
            try (InputStream in = part.getInputStream()) {
                return CloudinaryUtil.transform(CloudinaryUtil.upload(in, fileName, part.getContentType(), sub), MAIN);
            } catch (IOException e) {
                ctx.log("Tải ảnh lên Cloudinary thất bại: " + e.getMessage());
                throw new ValidationException(field, "Không tải được ảnh lên kho ảnh, hãy thử lại sau.");
            }
        }
        String realDir = ctx.getRealPath("/" + dir);
        if (realDir == null) {
            throw new IOException("Không xác định được thư mục lưu ảnh");
        }
        File folder = new File(realDir);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Không tạo được thư mục lưu ảnh: " + realDir);
        }
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, Path.of(realDir, fileName), StandardCopyOption.REPLACE_EXISTING);
        }
        return dir + "/" + fileName;
    }

    /** Ảnh nhỏ (thumb) của ảnh đã lưu: URL Cloudinary thì đổi sang bản 600px, ảnh trong máy thì dùng chính nó. */
    public static String thumbOf(String stored) {
        return CloudinaryUtil.isCloudinaryUrl(stored) ? stored.replace(MAIN, THUMB) : stored;
    }

    /**
     * Xóa file ảnh vừa ghi (khi lưu DB thất bại). Bỏ qua nếu không xóa được.
     * Ảnh trên Cloudinary không xóa ở đây (gói miễn phí đủ chỗ; có thể dọn trong trang Media Library).
     */
    public static void deleteQuietly(ServletContext ctx, String relativePath) {
        if (relativePath == null || CloudinaryUtil.isCloudinaryUrl(relativePath)) {
            return;
        }
        String real = ctx.getRealPath("/" + relativePath);
        if (real != null) {
            try {
                Files.deleteIfExists(Path.of(real));
            } catch (IOException ignored) {
                // không ảnh hưởng dữ liệu
            }
        }
    }
}
