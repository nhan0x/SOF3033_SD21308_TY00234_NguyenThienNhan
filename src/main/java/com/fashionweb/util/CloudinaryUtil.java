package com.fashionweb.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tải ảnh lên Cloudinary qua REST API (dùng HttpClient có sẵn của Java 17, không cần thêm thư viện).
 * Đọc khóa từ src/main/resources/cloudinary.properties (sao chép từ cloudinary.properties.example).
 *
 * <p>Không có file hoặc thiếu khóa thì {@link #isConfigured()} trả false và ImageUploadUtil lưu ảnh vào máy như cũ.
 * <p>Ảnh trả về là URL https://res.cloudinary.com/...; ảnh nhỏ (thumb) chỉ là URL khác của cùng ảnh, xem {@link #transform}.
 */
public final class CloudinaryUtil {

    private static final Properties PROPS = new Properties();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Pattern SECURE_URL = Pattern.compile("\"secure_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
    private static final String UPLOAD_MARK = "/image/upload/";

    static {
        try (InputStream in = CloudinaryUtil.class.getClassLoader().getResourceAsStream("cloudinary.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private CloudinaryUtil() {
    }

    /** Đã điền đủ cloud_name, api_key, api_secret chưa. */
    public static boolean isConfigured() {
        return !blank(prop("cloud_name")) && !blank(prop("api_key")) && !blank(prop("api_secret"));
    }

    /** URL Cloudinary của project này (để phân biệt với ảnh lưu trong máy). */
    public static boolean isCloudinaryUrl(String url) {
        return url != null && url.startsWith("https://res.cloudinary.com/") && url.contains(UPLOAD_MARK);
    }

    /**
     * Tải ảnh lên thư mục &lt;cloudinary.folder&gt;/&lt;subFolder&gt;, trả URL https của ảnh gốc.
     *
     * @throws IOException khi mạng lỗi hoặc Cloudinary từ chối (sai khóa, file không phải ảnh...)
     */
    public static String upload(InputStream data, String fileName, String contentType, String subFolder) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình cloudinary.properties");
        }
        String folder = (blank(prop("folder")) ? "saltair" : prop("folder").trim()) + "/" + subFolder;
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        Map<String, String> signed = new TreeMap<>();
        signed.put("folder", folder);
        signed.put("timestamp", timestamp);

        String boundary = "----saltair" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (Map.Entry<String, String> e : signed.entrySet()) {
            writeField(body, boundary, e.getKey(), e.getValue());
        }
        writeField(body, boundary, "api_key", prop("api_key").trim());
        writeField(body, boundary, "signature", sign(signed));
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                + "\"\r\nContent-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        data.transferTo(body);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + prop("cloud_name").trim() + "/image/upload"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        HttpResponse<String> resp;
        try {
            resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Tải ảnh lên Cloudinary bị ngắt", e);
        }
        String json = resp.body();
        if (resp.statusCode() != 200) {
            Matcher m = ERROR_MESSAGE.matcher(json);
            throw new IOException("Cloudinary từ chối ảnh (HTTP " + resp.statusCode() + "): " + (m.find() ? m.group(1) : json));
        }
        Matcher m = SECURE_URL.matcher(json);
        if (!m.find()) {
            throw new IOException("Cloudinary không trả về đường dẫn ảnh");
        }
        return m.group(1).replace("\\/", "/");
    }

    /**
     * Thêm phép biến đổi vào URL Cloudinary, ví dụ "c_limit,w_600,q_auto,f_auto" (thu nhỏ tối đa 600px, tự nén,
     * tự chọn định dạng nhẹ). URL không phải Cloudinary thì trả nguyên.
     */
    public static String transform(String url, String transformation) {
        if (!isCloudinaryUrl(url)) {
            return url;
        }
        int i = url.indexOf(UPLOAD_MARK) + UPLOAD_MARK.length();
        return url.substring(0, i) + transformation + "/" + url.substring(i);
    }

    /** Chữ ký SHA-1 theo quy tắc của Cloudinary: các tham số sắp theo tên, nối "a=b&c=d", cộng api_secret. */
    static String sign(Map<String, String> sortedParams) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, String> e : sortedParams.entrySet()) {
            if (s.length() > 0) {
                s.append('&');
            }
            s.append(e.getKey()).append('=').append(e.getValue());
        }
        s.append(prop("api_secret").trim());
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(s.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : d) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String prop(String key) {
        return PROPS.getProperty("cloudinary." + key);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
