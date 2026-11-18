package com.poly.sof309.controllers;

import com.fashionweb.controller.order.PaymentWebhookServlet;
import com.fashionweb.exception.DataAccessException;
import com.fashionweb.service.order.PaymentService;
import com.fashionweb.service.order.PaymentService.Outcome;
import com.fashionweb.util.PayOSSignature;
import com.fashionweb.util.PaymentConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poly.sof309.controllers.support.Harness;
import com.poly.sof309.controllers.support.ServletFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Webhook payOS (cụm thanh toán, rủi ro cao): TC_PAY_04, 05, 06, 07, 10.
 * Cấu hình payOS được thay bằng khóa giả (mock static) nên kết quả không phụ thuộc file payment.properties
 * và không bao giờ gọi payOS thật. PaymentService giả: chỉ kiểm tra Controller xác thực và chuyển dữ liệu đúng.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class PaymentWebhookControllerTest {

    private static final String KEY = "khoa-checksum-gia";
    private static final ObjectMapper JSON = new ObjectMapper();

    private MockedStatic<PaymentConfig> config;
    private PaymentConfig off;
    private PaymentConfig on;
    private Harness<PaymentWebhookServlet> harness;
    private PaymentService payments;

    @BeforeEach
    void setUp() {
        // Phải tạo PaymentConfig trước khi mock static
        off = new PaymentConfig(new Properties());
        Properties keys = new Properties();
        keys.setProperty("payos.clientId", "id");
        keys.setProperty("payos.apiKey", "key");
        keys.setProperty("payos.checksumKey", KEY);
        on = new PaymentConfig(keys);
        config = Mockito.mockStatic(PaymentConfig.class);
        config.when(PaymentConfig::get).thenReturn(on);
        harness = Harness.mocked(PaymentWebhookServlet::new, PaymentService.class);
        payments = harness.service(PaymentService.class);
    }

    @AfterEach
    void tearDown() {
        harness.close();
        config.close();
    }

    // ---------- dựng body ----------

    private static ObjectNode data(long orderCode, long amount, String linkId) {
        ObjectNode d = JSON.createObjectNode();
        d.put("orderCode", orderCode);
        d.put("amount", amount);
        d.put("description", "SA20261118001");
        d.put("code", "00");
        if (linkId != null) {
            d.put("paymentLinkId", linkId);
        }
        return d;
    }

    private static byte[] signed(boolean success, ObjectNode data, String key) {
        ObjectNode root = JSON.createObjectNode();
        root.put("code", "00");
        root.put("success", success);
        root.set("data", data);
        root.put("signature", PayOSSignature.signData(data, key));
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ServletFixture post(byte[] body) {
        return ServletFixture.post("/api/payment/payos").body(body);
    }

    private ServletFixture post(String body) {
        return post(body.getBytes(StandardCharsets.UTF_8));
    }

    private void assertReply(ServletFixture f, int status, boolean success) {
        assertEquals(status, f.status);
        assertEquals("{\"success\":" + success + "}", f.responseBody());
        verify(f.resp).setContentType("application/json;charset=UTF-8");
    }

    // ---------- TC_PAY_04: chưa cấu hình ----------

    @Test
    @DisplayName("TC_PAY_04: chưa cấu hình đủ khóa payOS thì từ chối mọi yêu cầu bằng 503, không đọc body")
    void notConfigured() throws Exception {
        config.when(PaymentConfig::get).thenReturn(off);
        ServletFixture f = post(signed(true, data(120261118001L, 578_000, "PL001"), KEY));

        f.run(harness.servlet);

        assertReply(f, 503, false);
        verifyNoInteractions(payments);
    }

    // ---------- TC_PAY_06: body không hợp lệ ----------

    @ParameterizedTest(name = "TC_PAY_06 body [{0}] -> 400")
    @DisplayName("TC_PAY_06: body hỏng hoặc thiếu data/signature thì 400")
    @ValueSource(strings = {
            "",
            "{khong phai json",
            "[]",
            "\"chuoi\"",
            "null",
            "{\"signature\":\"abc\"}",
            "{\"data\":{}}",
            "{\"data\":\"chuoi\",\"signature\":\"abc\"}",
            "{\"data\":[],\"signature\":\"abc\"}",
            "{\"data\":{},\"signature\":123}",
            "{\"data\":{},\"signature\":null}"
    })
    void malformedBody(String body) throws Exception {
        ServletFixture f = post(body);

        f.run(harness.servlet);

        assertReply(f, 400, false);
        verifyNoInteractions(payments);
    }

    @ParameterizedTest(name = "TC_PAY_06 body {0} byte -> {1}")
    @DisplayName("TC_PAY_06: biên 64 KB: đúng 65.536 byte còn được đọc (rồi bị 401 vì chữ ký), 65.537 byte bị 413")
    @CsvSource({"65536, 401", "65537, 413"})
    void bodySizeBoundary(int size, int expectedStatus) throws Exception {
        String prefix = "{\"data\":{},\"signature\":\"x\",\"pad\":\"";
        String suffix = "\"}";
        String body = prefix + "a".repeat(size - prefix.length() - suffix.length()) + suffix;
        assertEquals(size, body.getBytes(StandardCharsets.UTF_8).length);
        ServletFixture f = post(body);

        f.run(harness.servlet);

        assertReply(f, expectedStatus, false);
        verifyNoInteractions(payments);
    }

    // ---------- TC_PAY_05: chữ ký ----------

    @Test
    @DisplayName("TC_PAY_05: chữ ký sai thì 401 và không đổi đơn")
    void wrongSignature() throws Exception {
        ObjectNode data = data(120261118001L, 578_000, "PL001");
        ObjectNode root = JSON.createObjectNode();
        root.put("success", true);
        root.set("data", data);
        root.put("signature", "0".repeat(64));
        ServletFixture f = post(root.toString());

        f.run(harness.servlet);

        assertReply(f, 401, false);
        verifyNoInteractions(payments);
    }

    @Test
    @DisplayName("TC_PAY_05: ký bằng khóa khác (kẻ giả mạo không biết Checksum Key) thì 401")
    void signedWithAnotherKey() throws Exception {
        ServletFixture f = post(signed(true, data(120261118001L, 578_000, "PL001"), "khoa-cua-ke-gia-mao"));

        f.run(harness.servlet);

        assertReply(f, 401, false);
        verifyNoInteractions(payments);
    }

    @Test
    @DisplayName("TC_PAY_05: sửa số tiền sau khi ký (giả mạo dữ liệu) thì chữ ký không còn khớp, 401")
    void tamperedAmount() throws Exception {
        ObjectNode data = data(120261118001L, 1_000, "PL001");
        String sig = PayOSSignature.signData(data, KEY);
        data.put("amount", 578_000); // sửa sau khi ký
        ObjectNode root = JSON.createObjectNode();
        root.put("success", true);
        root.set("data", data);
        root.put("signature", sig);
        ServletFixture f = post(root.toString());

        f.run(harness.servlet);

        assertReply(f, 401, false);
        verifyNoInteractions(payments);
    }

    // ---------- TC_PAY_07: thanh toán thành công ----------

    @Test
    @DisplayName("TC_PAY_07: webhook ký đúng thì ghi nhận giao dịch với mã đơn Saltair đổi ngược từ mã payOS")
    void validWebhookRecordsTransfer() throws Exception {
        when(payments.recordTransfer(anyString(), anyString(), anyString(), anyLong(), anyString())).thenReturn(Outcome.PAID);
        ServletFixture f = post(signed(true, data(120261118001L, 578_000, "PL001"), KEY));

        f.run(harness.servlet);

        assertReply(f, 200, true);
        // 120261118001 = 1 * 10^11 + 20261118001 -> lần tạo link thứ 1 của đơn SA20261118001
        verify(payments).recordTransfer(PaymentService.PROVIDER_PAYOS, "PL001", "SA20261118001", 578_000, "SA20261118001");
    }

    @Test
    @DisplayName("TC_PAY_07: chữ ký viết HOA vẫn được chấp nhận (so sánh không phân biệt hoa thường)")
    void uppercaseSignatureAccepted() throws Exception {
        ObjectNode data = data(120261118001L, 578_000, "PL002");
        ObjectNode root = JSON.createObjectNode();
        root.put("success", true);
        root.set("data", data);
        root.put("signature", PayOSSignature.signData(data, KEY).toUpperCase());
        ServletFixture f = post(root.toString());

        f.run(harness.servlet);

        assertReply(f, 200, true);
        verify(payments).recordTransfer(PaymentService.PROVIDER_PAYOS, "PL002", "SA20261118001", 578_000, "SA20261118001");
    }

    @Test
    @DisplayName("TC_PAY_07: lỗi database thì trả 500 để payOS gửi lại sau")
    void databaseErrorReturns500() throws Exception {
        when(payments.recordTransfer(anyString(), anyString(), anyString(), anyLong(), anyString()))
                .thenThrow(new DataAccessException("Lỗi ghi giao dịch thanh toán", new java.sql.SQLException("down")));
        ServletFixture f = post(signed(true, data(120261118001L, 578_000, "PL001"), KEY));

        f.run(harness.servlet);

        assertReply(f, 500, false);
    }

    // ---------- TC_PAY_10: thông báo không thành công vẫn trả 200 ----------

    @Test
    @DisplayName("TC_PAY_10: success=false thì bỏ qua nhưng vẫn trả 200 (để payOS không gửi lại), không ghi giao dịch")
    void unsuccessfulNotificationIsAcknowledged() throws Exception {
        ServletFixture f = post(signed(false, data(120261118001L, 578_000, "PL001"), KEY));

        f.run(harness.servlet);

        assertReply(f, 200, true);
        verifyNoInteractions(payments);
    }

    @Test
    @DisplayName("TC_PAY_10: data.code khác \"00\" thì bỏ qua")
    void failureCodeIsIgnored() throws Exception {
        ObjectNode data = data(120261118001L, 578_000, "PL001");
        data.put("code", "01");
        ServletFixture f = post(signed(true, data, KEY));

        f.run(harness.servlet);

        assertReply(f, 200, true);
        verifyNoInteractions(payments);
    }

    @ParameterizedTest(name = "TC_PAY_10 thiếu trường {0} thì bỏ qua")
    @DisplayName("TC_PAY_10: thiếu orderCode, amount hoặc paymentLinkId thì bỏ qua (200), không đoán mò")
    @ValueSource(strings = {"orderCode", "amount", "paymentLinkId"})
    void missingFieldIsIgnored(String missing) throws Exception {
        ObjectNode data = data(120261118001L, 578_000, "PL001");
        data.remove(missing);
        ServletFixture f = post(signed(true, data, KEY));

        f.run(harness.servlet);

        assertReply(f, 200, true);
        verifyNoInteractions(payments);
    }

    @Test
    @DisplayName("TC_PAY_10: giá trị null trong data bị coi là thiếu")
    void nullFieldIsIgnored() throws Exception {
        ObjectNode data = data(120261118001L, 578_000, "PL001");
        data.putNull("paymentLinkId");
        ServletFixture f = post(signed(true, data, KEY));

        f.run(harness.servlet);

        assertReply(f, 200, true);
        verifyNoInteractions(payments);
    }
}
