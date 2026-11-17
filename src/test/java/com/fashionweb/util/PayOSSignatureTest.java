package com.fashionweb.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test chữ ký payOS: HMAC-SHA256, thứ tự khóa, các trường ký khi tạo link. */
class PayOSSignatureTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode json(String text) throws Exception {
        return JSON.readTree(text);
    }

    @Test
    @DisplayName("HMAC-SHA256 đúng theo vector chuẩn RFC 4231 (ca 2)")
    void hmacMatchesRfc4231() {
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                PayOSSignature.hmacSha256Hex("what do ya want for nothing?", "Jefe"));
    }

    @Test
    @DisplayName("Chữ ký webhook ký trên các khóa xếp theo chữ cái, không phụ thuộc thứ tự trong JSON")
    void signDataSortsKeys() throws Exception {
        String expected = PayOSSignature.hmacSha256Hex("amount=500000&orderCode=123&reference=ABC", "khoa");
        assertEquals(expected, PayOSSignature.signData(json("{\"reference\":\"ABC\",\"orderCode\":123,\"amount\":500000}"), "khoa"));
        assertEquals(expected, PayOSSignature.signData(json("{\"amount\":500000,\"reference\":\"ABC\",\"orderCode\":123}"), "khoa"));
    }

    @Test
    @DisplayName("Giá trị null được ký như chuỗi rỗng")
    void signDataTreatsNullAsEmpty() throws Exception {
        String expected = PayOSSignature.hmacSha256Hex("a=1&b=", "khoa");
        assertEquals(expected, PayOSSignature.signData(json("{\"b\":null,\"a\":1}"), "khoa"));
    }

    @Test
    @DisplayName("Đổi một trường hoặc khóa thì chữ ký đổi")
    void signatureChangesWithDataOrKey() throws Exception {
        String base = PayOSSignature.signData(json("{\"amount\":500000}"), "khoa");
        assertNotEquals(base, PayOSSignature.signData(json("{\"amount\":500001}"), "khoa"));
        assertNotEquals(base, PayOSSignature.signData(json("{\"amount\":500000}"), "khoa-khac"));
    }

    @Test
    @DisplayName("Chữ ký tạo link ký đúng 5 trường theo thứ tự amount, cancelUrl, description, orderCode, returnUrl")
    void signPaymentRequestFormat() {
        String expected = PayOSSignature.hmacSha256Hex(
                "amount=549000&cancelUrl=http://x/c&description=SA20261117001&orderCode=120261117001&returnUrl=http://x/r", "khoa");
        assertEquals(expected, PayOSSignature.signPaymentRequest(549000, "http://x/c", "SA20261117001", 120261117001L, "http://x/r", "khoa"));
    }

    @Test
    @DisplayName("So sánh chữ ký: đúng, sai, null, khác chữ hoa thường")
    void matches() {
        assertTrue(PayOSSignature.matches("abc123", "abc123"));
        assertTrue(PayOSSignature.matches("abc123", "ABC123"));
        assertTrue(PayOSSignature.matches("abc123", " abc123 "));
        assertFalse(PayOSSignature.matches("abc123", "abc124"));
        assertFalse(PayOSSignature.matches("abc123", null));
        assertFalse(PayOSSignature.matches(null, "abc123"));
        assertFalse(PayOSSignature.matches("abc123", ""));
    }
}
