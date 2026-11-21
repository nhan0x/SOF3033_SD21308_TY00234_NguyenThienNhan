package com.fashionweb.service.order;

import com.fashionweb.service.order.PayOSClient.PayOSException;
import com.fashionweb.util.PayOSSignature;
import com.fashionweb.util.PaymentConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test PayOSClient với một máy chủ giả chạy trên máy (không gọi payOS thật, không cần khóa thật):
 * kiểm tra đúng địa chỉ, header, chữ ký và cách đọc phản hồi.
 */
class PayOSClientTest {

    private HttpServer server;
    private PayOSClient client;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> clientIdHeader = new AtomicReference<>();
    private final AtomicReference<String> apiKeyHeader = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile String reply = "";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            method.set(ex.getRequestMethod());
            path.set(ex.getRequestURI().getPath());
            clientIdHeader.set(ex.getRequestHeaders().getFirst("x-client-id"));
            apiKeyHeader.set(ex.getRequestHeaders().getFirst("x-api-key"));
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
        Properties p = new Properties();
        p.setProperty("payos.clientId", "client-test");
        p.setProperty("payos.apiKey", "api-test");
        p.setProperty("payos.checksumKey", "checksum-test");
        p.setProperty("payos.baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        client = new PayOSClient(new PaymentConfig(p));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    @DisplayName("Tạo link: gọi POST /v2/payment-requests, gửi header khóa, ký đúng, đọc được checkoutUrl")
    void createPaymentLink() throws Exception {
        reply = "{\"code\":\"00\",\"desc\":\"success\",\"data\":{\"checkoutUrl\":\"https://pay.payos.vn/web/abc\",\"paymentLinkId\":\"abc\","
                + "\"qrCode\":\"00020101021238570010A000000727\",\"bin\":\"970422\",\"accountNumber\":\"VQRQ123\","
                + "\"accountName\":\"NGUYEN THIEN NHAN\",\"amount\":549000,\"description\":\"SA20261117001\",\"expiredAt\":1900000000}}";
        PayOSClient.PaymentLink link = client.createPaymentLink(120261117001L, 549000, "SA20261117001", "http://x/r", "http://x/c");

        assertEquals("POST", method.get());
        assertEquals("/v2/payment-requests", path.get());
        assertEquals("client-test", clientIdHeader.get());
        assertEquals("api-test", apiKeyHeader.get());
        assertEquals("https://pay.payos.vn/web/abc", link.checkoutUrl());
        assertEquals("abc", link.paymentLinkId());
        assertEquals("00020101021238570010A000000727", link.qrCode());
        assertEquals("970422", link.bin());
        assertEquals("VQRQ123", link.accountNumber());
        assertEquals("NGUYEN THIEN NHAN", link.accountName());
        assertEquals(549000, link.amount());
        assertEquals("SA20261117001", link.description());
        assertEquals(1900000000L, link.expiredAt());

        JsonNode sent = new ObjectMapper().readTree(body.get());
        assertEquals(120261117001L, sent.get("orderCode").asLong());
        assertEquals(549000, sent.get("amount").asLong());
        assertEquals("SA20261117001", sent.get("description").asText());
        assertEquals("http://x/r", sent.get("returnUrl").asText());
        assertEquals("http://x/c", sent.get("cancelUrl").asText());
        long expiresIn = sent.get("expiredAt").asLong() - System.currentTimeMillis() / 1000;
        assertTrue(expiresIn > PayOSClient.LINK_LIFETIME_SECONDS - 30 && expiresIn <= PayOSClient.LINK_LIFETIME_SECONDS,
                "link phải hết hạn sau khoảng 15 phút, thực tế " + expiresIn + " giây");
        assertEquals(PayOSSignature.signPaymentRequest(549000, "http://x/c", "SA20261117001", 120261117001L, "http://x/r", "checksum-test"),
                sent.get("signature").asText());
    }

    @Test
    @DisplayName("payOS trả mã lỗi: ném PayOSException kèm lý do")
    void createPaymentLinkRejected() {
        reply = "{\"code\":\"231\",\"desc\":\"Đơn thanh toán đã tồn tại\",\"data\":null}";
        PayOSException e = assertThrows(PayOSException.class,
                () -> client.createPaymentLink(1, 1000, "x", "http://x/r", "http://x/c"));
        assertTrue(e.getMessage().contains("Đơn thanh toán đã tồn tại"));
    }

    @Test
    @DisplayName("Phản hồi thiếu checkoutUrl: ném PayOSException")
    void createPaymentLinkWithoutUrl() {
        reply = "{\"code\":\"00\",\"desc\":\"success\",\"data\":{}}";
        assertThrows(PayOSException.class, () -> client.createPaymentLink(1, 1000, "x", "http://x/r", "http://x/c"));
    }

    @Test
    @DisplayName("Phản hồi thiếu mã QR: ném PayOSException (trang thanh toán không có gì để vẽ)")
    void createPaymentLinkWithoutQr() {
        reply = "{\"code\":\"00\",\"desc\":\"success\",\"data\":{\"checkoutUrl\":\"https://pay.payos.vn/web/abc\",\"paymentLinkId\":\"abc\"}}";
        assertThrows(PayOSException.class, () -> client.createPaymentLink(1, 1000, "x", "http://x/r", "http://x/c"));
    }

    @Test
    @DisplayName("Hủy link: POST /v2/payment-requests/{orderCode}/cancel kèm lý do")
    void cancelPaymentLink() throws Exception {
        reply = "{\"code\":\"00\",\"desc\":\"success\",\"data\":{\"status\":\"CANCELLED\"}}";
        client.cancelPaymentLink(120261117001L, "Khách hủy thanh toán");

        assertEquals("POST", method.get());
        assertEquals("/v2/payment-requests/120261117001/cancel", path.get());
        assertEquals("Khách hủy thanh toán", new ObjectMapper().readTree(body.get()).get("cancellationReason").asText());
    }

    @Test
    @DisplayName("Hủy link bị payOS từ chối (đã trả tiền hoặc đã hủy): ném PayOSException")
    void cancelPaymentLinkRejected() {
        reply = "{\"code\":\"101\",\"desc\":\"Link đã thanh toán\",\"data\":null}";
        assertThrows(PayOSException.class, () -> client.cancelPaymentLink(1, "x"));
    }

    @Test
    @DisplayName("Hỏi trạng thái: GET /v2/payment-requests/{orderCode}, đọc status, số tiền đã trả và id link")
    void getPaymentStatus() {
        reply = "{\"code\":\"00\",\"desc\":\"success\",\"data\":{\"id\":\"link-1\",\"orderCode\":120261117001,\"amount\":549000,\"amountPaid\":549000,\"status\":\"PAID\"}}";
        PayOSClient.PaymentStatusInfo info = client.getPaymentStatus(120261117001L);

        assertEquals("GET", method.get());
        assertEquals("/v2/payment-requests/120261117001", path.get());
        assertEquals("PAID", info.status());
        assertEquals(549000, info.amountPaid());
        assertEquals("link-1", info.paymentLinkId());
    }

    @Test
    @DisplayName("Không kết nối được payOS: ném PayOSException với câu tiếng Việt")
    void connectionFailure() {
        server.stop(0);
        PayOSException e = assertThrows(PayOSException.class, () -> client.getPaymentStatus(1));
        assertTrue(e.getMessage().contains("payOS"));
    }
}
