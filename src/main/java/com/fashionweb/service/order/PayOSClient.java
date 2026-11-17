package com.fashionweb.service.order;

import com.fashionweb.util.PayOSSignature;
import com.fashionweb.util.PaymentConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Gọi API payOS (https://api-merchant.payos.vn): tạo link thanh toán và hỏi trạng thái. Chi tiết endpoint, tên header và
 * quy tắc ký lấy từ SDK chính thức github.com/payOSHQ/payos-lib-java.
 *
 * <p>Header bắt buộc: x-client-id, x-api-key. Phản hồi dạng {"code":"00","desc":"success","data":{...},"signature":"..."};
 * code "00" là thành công.
 */
public class PayOSClient {

    /**
     * Kết quả tạo link. Trang thanh toán của Saltair tự vẽ mã QR từ qrCode và hiện thông tin tài khoản nhận tiền;
     * checkoutUrl (trang của payOS) chỉ giữ lại để đối chiếu. expiredAt là giây kể từ 1/1/1970, 0 nếu payOS không trả.
     * Lưu được vào session nên là Serializable.
     */
    public record PaymentLink(String checkoutUrl, String paymentLinkId, String qrCode, String bin, String accountNumber,
                              String accountName, long amount, String description, long expiredAt) implements java.io.Serializable {
    }

    /** Trạng thái một link: PENDING, PAID, CANCELLED, EXPIRED, PROCESSING, UNDERPAID. */
    public record PaymentStatusInfo(String status, long amountPaid, String paymentLinkId) {
    }

    /** Lỗi khi gọi payOS: message là câu tiếng Việt có thể hiện cho khách. */
    public static class PayOSException extends RuntimeException {
        public PayOSException(String message) {
            super(message);
        }

        public PayOSException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final PaymentConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public PayOSClient() {
        this(PaymentConfig.get());
    }

    public PayOSClient(PaymentConfig config) {
        this.config = config;
    }

    /** Link thanh toán tự hết hạn sau từng này giây kể từ lúc tạo (khách thấy đồng hồ đếm ngược trên trang). */
    public static final long LINK_LIFETIME_SECONDS = 15 * 60;

    /**
     * POST /v2/payment-requests. description tối đa 25 ký tự (payOS giới hạn). returnUrl, cancelUrl là nơi payOS chuyển
     * khách về nếu họ dùng trang của payOS; Saltair tự hiện mã QR nên hai địa chỉ này chủ yếu để đủ tham số bắt buộc.
     */
    public PaymentLink createPaymentLink(long orderCode, long amount, String description, String returnUrl, String cancelUrl) {
        long expiredAt = System.currentTimeMillis() / 1000 + LINK_LIFETIME_SECONDS;
        ObjectNode body = JSON.createObjectNode();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("expiredAt", expiredAt);
        body.put("signature", PayOSSignature.signPaymentRequest(amount, cancelUrl, description, orderCode, returnUrl,
                config.getChecksumKey()));
        JsonNode data = call(HttpRequest.newBuilder(URI.create(config.getBaseUrl() + "/v2/payment-requests"))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)));
        String url = data.path("checkoutUrl").asText("");
        if (url.isEmpty()) {
            throw new PayOSException("Không tạo được link thanh toán, vui lòng thử lại sau.");
        }
        String qr = data.path("qrCode").asText("");
        if (qr.isEmpty()) {
            throw new PayOSException("payOS không trả mã QR, vui lòng thử lại sau.");
        }
        return new PaymentLink(url, data.path("paymentLinkId").asText(""), qr, data.path("bin").asText(""),
                data.path("accountNumber").asText(""), data.path("accountName").asText(""), data.path("amount").asLong(amount),
                data.path("description").asText(description), data.path("expiredAt").asLong(expiredAt));
    }

    /**
     * POST /v2/payment-requests/{orderCode}/cancel: hủy link chưa thanh toán để mã QR cũ không còn dùng được.
     * payOS từ chối (link đã trả tiền hoặc đã hủy sẵn) thì ném PayOSException.
     */
    public void cancelPaymentLink(long orderCode, String reason) {
        ObjectNode body = JSON.createObjectNode();
        body.put("cancellationReason", reason);
        call(HttpRequest.newBuilder(URI.create(config.getBaseUrl() + "/v2/payment-requests/" + orderCode + "/cancel"))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)));
    }

    /** GET /v2/payment-requests/{orderCode}: trạng thái thanh toán hiện tại của link. */
    public PaymentStatusInfo getPaymentStatus(long orderCode) {
        JsonNode data = call(HttpRequest.newBuilder(URI.create(config.getBaseUrl() + "/v2/payment-requests/" + orderCode)).GET());
        return new PaymentStatusInfo(data.path("status").asText(""), data.path("amountPaid").asLong(0), data.path("id").asText(""));
    }

    private JsonNode call(HttpRequest.Builder request) {
        HttpRequest req = request
                .header("Content-Type", "application/json")
                .header("x-client-id", config.getClientId())
                .header("x-api-key", config.getApiKey())
                .timeout(Duration.ofSeconds(10))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = JSON.readTree(res.body());
            if (!"00".equals(root.path("code").asText())) {
                throw new PayOSException("payOS từ chối yêu cầu: " + root.path("desc").asText("không rõ lý do"));
            }
            return root.path("data");
        } catch (IOException e) {
            throw new PayOSException("Không kết nối được tới payOS, vui lòng thử lại sau.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PayOSException("Kết nối tới payOS bị gián đoạn.", e);
        }
    }
}
