package com.fashionweb.util;

import com.fasterxml.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Chữ ký của payOS: HMAC-SHA256 (khóa = Checksum Key) trên chuỗi "khóa=giá trị&khóa=giá trị" với các khóa xếp theo thứ tự
 * chữ cái. Quy tắc lấy từ mã nguồn SDK chính thức (github.com/payOSHQ/payos-lib-java, CryptoProviderImpl).
 * <ul>
 *   <li>Webhook: ký trên đối tượng "data" (mọi trường, xếp theo khóa).</li>
 *   <li>Tạo link thanh toán: chỉ ký 5 trường amount, cancelUrl, description, orderCode, returnUrl theo đúng thứ tự đó.</li>
 * </ul>
 */
public final class PayOSSignature {

    private PayOSSignature() {
    }

    /** Chữ ký của đối tượng "data" trong webhook. Giá trị null hoặc thiếu thì coi là chuỗi rỗng. */
    public static String signData(JsonNode data, String checksumKey) {
        Map<String, String> sorted = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = data.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            sorted.put(e.getKey(), v == null || v.isNull() ? "" : (v.isContainerNode() ? v.toString() : v.asText()));
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return hmacSha256Hex(sb.toString(), checksumKey);
    }

    /** Chữ ký khi tạo link thanh toán. */
    public static String signPaymentRequest(long amount, String cancelUrl, String description, long orderCode, String returnUrl,
                                            String checksumKey) {
        String text = "amount=" + amount + "&cancelUrl=" + cancelUrl + "&description=" + description
                + "&orderCode=" + orderCode + "&returnUrl=" + returnUrl;
        return hmacSha256Hex(text, checksumKey);
    }

    /** So sánh chữ ký ở thời gian cố định, không phân biệt chữ hoa thường của chuỗi hex. */
    public static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actual.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    public static String hmacSha256Hex(String text, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(out.length * 2);
            for (byte b : out) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
