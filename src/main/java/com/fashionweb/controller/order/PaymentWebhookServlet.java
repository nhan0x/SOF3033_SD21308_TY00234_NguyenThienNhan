package com.fashionweb.controller.order;

import com.fashionweb.exception.DataAccessException;
import com.fashionweb.service.order.PaymentService;
import com.fashionweb.service.order.PaymentService.Outcome;
import com.fashionweb.util.PayOSSignature;
import com.fashionweb.util.PaymentConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * POST /api/payment/payos : webhook payOS báo có khách đã thanh toán.
 *
 * <p>Bảo mật: payOS ký trường "data" bằng Checksum Key (xem {@link PayOSSignature}); chữ ký sai thì từ chối (401). Chưa
 * cấu hình khóa thì từ chối hết (503), nên không ai gọi giả được để tự đánh dấu đơn đã thanh toán.
 * <p>Phản hồi: 200 {"success":true} cho mọi yêu cầu hợp lệ (kể cả giao dịch không khớp đơn nào, như yêu cầu thử khi đăng ký
 * webhook) để payOS không gửi lại. Chỉ lỗi database mới trả 500 để payOS gửi lại sau.
 * <p>Trường dùng trong JSON: success, data.code ("00" là thành công), data.orderCode (số), data.amount, data.description,
 * data.paymentLinkId (dùng làm mã chống xử lý trùng), signature. Không dùng AuthFilter: đường dẫn này do payOS gọi.
 */
@WebServlet("/api/payment/payos")
public class PaymentWebhookServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(PaymentWebhookServlet.class.getName());
    private static final int MAX_BODY = 64 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PaymentConfig cfg = PaymentConfig.get();
        if (!cfg.isEnabled()) {
            reply(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, false);
            return;
        }

        JsonNode body;
        try (InputStream in = req.getInputStream()) {
            byte[] bytes = in.readNBytes(MAX_BODY + 1);
            if (bytes.length > MAX_BODY) {
                reply(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, false);
                return;
            }
            body = JSON.readTree(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            reply(resp, HttpServletResponse.SC_BAD_REQUEST, false);
            return;
        }
        if (body == null || !body.isObject() || !body.path("data").isObject() || !body.path("signature").isTextual()) {
            reply(resp, HttpServletResponse.SC_BAD_REQUEST, false);
            return;
        }

        JsonNode data = body.get("data");
        String expected = PayOSSignature.signData(data, cfg.getChecksumKey());
        if (!PayOSSignature.matches(expected, body.get("signature").asText())) {
            LOG.warning("Webhook payOS bị từ chối: sai chữ ký, IP " + req.getRemoteAddr());
            reply(resp, HttpServletResponse.SC_UNAUTHORIZED, false);
            return;
        }

        // Chỉ xử lý giao dịch thành công; các thông báo khác (lỗi, hủy) bỏ qua nhưng vẫn báo đã nhận
        boolean success = body.path("success").asBoolean(false) && "00".equals(data.path("code").asText(""));
        if (!success || !data.hasNonNull("orderCode") || !data.hasNonNull("amount") || !data.hasNonNull("paymentLinkId")) {
            reply(resp, HttpServletResponse.SC_OK, true);
            return;
        }

        String txId = data.get("paymentLinkId").asText();
        String orderCode = PaymentService.fromPayOSCode(data.get("orderCode").asLong());
        long amount = data.get("amount").asLong();
        try {
            Outcome outcome = paymentService.recordTransfer(PaymentService.PROVIDER_PAYOS, txId, orderCode, amount,
                    data.path("description").asText(""));
            LOG.info("Giao dịch payOS " + txId + " đơn " + orderCode + ", " + amount + "đ, kết quả " + outcome);
        } catch (DataAccessException e) {
            LOG.severe("Lỗi database khi xử lý giao dịch payOS " + txId + ": " + e.getMessage());
            reply(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false);
            return;
        }
        reply(resp, HttpServletResponse.SC_OK, true);
    }

    private static void reply(HttpServletResponse resp, int status, boolean success) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"success\":" + success + "}");
    }
}
