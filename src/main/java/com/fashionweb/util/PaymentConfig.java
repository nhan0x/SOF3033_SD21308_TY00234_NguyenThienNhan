package com.fashionweb.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Cấu hình thanh toán chuyển khoản qua payOS, đọc từ src/main/resources/payment.properties
 * (sao chép từ payment.properties.example). Lấy Client ID, API Key, Checksum Key ở my.payos.vn khi tạo kênh thanh toán.
 *
 * <p>Thiếu bất kỳ khóa nào thì: khách không thấy lựa chọn chuyển khoản, và webhook từ chối mọi yêu cầu
 * (không có Checksum Key thì không kiểm tra được chữ ký, nên không nhận bất cứ thông báo thanh toán nào).
 */
public final class PaymentConfig {

    public static final String DEFAULT_BASE_URL = "https://api-merchant.payos.vn";

    private static final PaymentConfig INSTANCE = load();

    private final String clientId;
    private final String apiKey;
    private final String checksumKey;
    private final String baseUrl;

    /** Dùng cho test: tạo cấu hình từ Properties dựng sẵn. */
    public PaymentConfig(Properties props) {
        this.clientId = value(props, "payos.clientId");
        this.apiKey = value(props, "payos.apiKey");
        this.checksumKey = value(props, "payos.checksumKey");
        String url = value(props, "payos.baseUrl");
        this.baseUrl = url.isEmpty() ? DEFAULT_BASE_URL : url.replaceAll("/+$", "");
    }

    public static PaymentConfig get() {
        return INSTANCE;
    }

    private static PaymentConfig load() {
        Properties props = new Properties();
        try (InputStream in = PaymentConfig.class.getClassLoader().getResourceAsStream("payment.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        return new PaymentConfig(props);
    }

    private static String value(Properties props, String key) {
        String v = props.getProperty(key);
        return v == null ? "" : v.trim();
    }

    /** true khi đủ 3 khóa payOS: khách được chọn chuyển khoản và webhook hoạt động. */
    public boolean isEnabled() {
        return !clientId.isEmpty() && !apiKey.isEmpty() && !checksumKey.isEmpty();
    }

    public String getClientId() {
        return clientId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getChecksumKey() {
        return checksumKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
