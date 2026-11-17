package com.fashionweb.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test cấu hình payOS: khi nào bật, địa chỉ API mặc định. */
class PaymentConfigTest {

    private static PaymentConfig config(String clientId, String apiKey, String checksumKey, String baseUrl) {
        Properties p = new Properties();
        p.setProperty("payos.clientId", clientId);
        p.setProperty("payos.apiKey", apiKey);
        p.setProperty("payos.checksumKey", checksumKey);
        p.setProperty("payos.baseUrl", baseUrl);
        return new PaymentConfig(p);
    }

    @Test
    @DisplayName("Đủ 3 khóa thì bật")
    void enabledWithAllKeys() {
        assertTrue(config("id", "key", "checksum", "").isEnabled());
    }

    @Test
    @DisplayName("Thiếu bất kỳ khóa nào thì tắt")
    void disabledWhenAnyKeyMissing() {
        assertFalse(config("", "key", "checksum", "").isEnabled());
        assertFalse(config("id", "", "checksum", "").isEnabled());
        assertFalse(config("id", "key", "", "").isEnabled());
        assertFalse(config("   ", "key", "checksum", "").isEnabled());
    }

    @Test
    @DisplayName("Không có file cấu hình (Properties rỗng): tắt")
    void emptyConfigDisabled() {
        assertFalse(new PaymentConfig(new Properties()).isEnabled());
    }

    @Test
    @DisplayName("Địa chỉ API mặc định là api-merchant.payos.vn")
    void defaultBaseUrl() {
        assertEquals("https://api-merchant.payos.vn", config("id", "key", "checksum", "").getBaseUrl());
    }

    @Test
    @DisplayName("Địa chỉ API tự đặt: bỏ dấu / ở cuối")
    void customBaseUrlTrimsTrailingSlash() {
        assertEquals("http://localhost:9000", config("id", "key", "checksum", "http://localhost:9000//").getBaseUrl());
    }

    @Test
    @DisplayName("Khóa có khoảng trắng thừa được cắt bỏ")
    void keysAreTrimmed() {
        assertEquals("abc", config(" abc ", "k", "c", "").getClientId());
    }
}
