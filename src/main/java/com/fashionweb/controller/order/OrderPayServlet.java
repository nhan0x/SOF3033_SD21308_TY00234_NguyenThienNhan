package com.fashionweb.controller.order;

import com.fashionweb.constant.OrderStatus;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.constant.PaymentStatus;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Order;
import com.fashionweb.service.order.OrderService;
import com.fashionweb.service.order.PayOSClient.PayOSException;
import com.fashionweb.service.order.PayOSClient.PaymentLink;
import com.fashionweb.service.order.PaymentService;
import com.fashionweb.util.PaymentConfig;
import com.fashionweb.util.QrSvg;
import com.fashionweb.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Trang thanh toán chuyển khoản trong giao diện Saltair (mã QR + thông tin tài khoản nhận tiền), dùng link payOS tạo ngầm.
 *
 * <p>GET /order-pay?code=... : tạo link thanh toán (hoặc dùng lại link còn hạn trong phiên, để tải lại trang không sinh mã
 * mới) rồi hiện trang. JSP: /WEB-INF/views/order/order-pay.jsp. Thuộc tính request: order, qrSvg (SVG đã dựng, in thẳng),
 * link (PaymentLink), bankName, remaining (số giây còn hiệu lực, cho đồng hồ đếm ngược; tính ở server để không lệ thuộc đồng hồ máy khách), statusUrl (địa chỉ hỏi đã thanh toán chưa).
 * Đơn đã thanh toán thì chuyển về trang đặt hàng thành công; lỗi payOS thì về trang đó kèm thông báo.
 * <p>POST /order-pay (code=...) : khách bấm "Hủy thanh toán": hủy link ở payOS rồi chuyển sang trang thông báo đã hủy
 * (/order-success?code=...&amp;cancel=true). Đơn vẫn giữ nguyên để khách thanh toán lại sau.
 * Đơn phải là đơn khách được xem (xem {@link OrderSuccessServlet#findVisibleOrder}).
 */
@WebServlet("/order-pay")
public class OrderPayServlet extends HttpServlet {

    /** Tên ngân hàng theo mã BIN (payOS chỉ trả BIN). Mã lạ thì hiện BIN. */
    private static final Map<String, String> BANKS = Map.ofEntries(
            Map.entry("970422", "MB Bank"), Map.entry("970448", "OCB"), Map.entry("970416", "ACB"),
            Map.entry("970436", "Vietcombank"), Map.entry("970415", "VietinBank"), Map.entry("970418", "BIDV"),
            Map.entry("970407", "Techcombank"), Map.entry("970432", "VPBank"), Map.entry("970423", "TPBank"),
            Map.entry("970405", "Agribank"), Map.entry("970403", "Sacombank"));

    /** Còn dưới ngần này giây thì không dùng lại link cũ (khách không kịp chuyển), tạo link mới. */
    private static final long MIN_REMAINING_SECONDS = 60;

    private final OrderService orderService = new OrderService();
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Order order = OrderSuccessServlet.findVisibleOrder(req, orderService);
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String back = absolute(req) + "/order-success?code=" + URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8);
        if (!PaymentConfig.get().isEnabled()) {
            WebUtil.flash(req, "Hiện chưa hỗ trợ chuyển khoản.", "error");
            resp.sendRedirect(back);
            return;
        }
        // Tiền có thể đã về mà webhook chưa tới: hỏi payOS trước khi bắt khách chuyển lần nữa
        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && order.getPaymentStatus() == PaymentStatus.UNPAID
                && paymentService.syncPayment(order)) {
            order = orderService.findByCode(order.getOrderCode());
        }
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER || order.getPaymentStatus() == PaymentStatus.PAID
                || order.getStatus() == OrderStatus.CANCELLED) {
            resp.sendRedirect(back);
            return;
        }
        PaymentLink link;
        try {
            link = currentLink(req, order, back);
        } catch (ValidationException | PayOSException e) {
            WebUtil.flash(req, e.getMessage(), "error");
            resp.sendRedirect(back);
            return;
        }
        String ctx = req.getContextPath();
        req.setAttribute("order", order);
        req.setAttribute("link", link);
        req.setAttribute("qrSvg", QrSvg.render(link.qrCode()));
        req.setAttribute("bankName", BANKS.getOrDefault(link.bin(), "Ngân hàng (BIN " + link.bin() + ")"));
        req.setAttribute("remaining", Math.max(0, link.expiredAt() - System.currentTimeMillis() / 1000));
        req.setAttribute("statusUrl", ctx + "/order-success?code=" + URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8) + "&status=1");
        resp.setHeader("Cache-Control", "no-store");
        req.getRequestDispatcher("/WEB-INF/views/order/order-pay.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Order order = OrderSuccessServlet.findVisibleOrder(req, orderService);
        if (order == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String code = URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8);
        // Khách bấm hủy đúng lúc tiền vừa về: đơn đã thanh toán thì không hủy, về trang thành công
        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER && order.getPaymentStatus() == PaymentStatus.UNPAID
                && PaymentConfig.get().isEnabled()) {
            if (!paymentService.syncPayment(order)) {
                paymentService.cancelPayment(order);
            }
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute(sessionKey(order));
        }
        resp.sendRedirect(req.getContextPath() + "/order-success?code=" + code + "&cancel=true");
    }

    /** Link đang dùng của đơn: link còn hạn lưu trong phiên (tải lại trang không đổi mã QR), không có thì tạo mới. */
    private PaymentLink currentLink(HttpServletRequest req, Order order, String back) {
        HttpSession session = req.getSession();
        Object saved = session.getAttribute(sessionKey(order));
        long now = System.currentTimeMillis() / 1000;
        if (saved instanceof PaymentLink link && link.amount() == order.getTotalAmount()
                && link.expiredAt() - now > MIN_REMAINING_SECONDS) {
            return link;
        }
        PaymentLink fresh = paymentService.startPayment(order, back, back);
        session.setAttribute(sessionKey(order), fresh);
        return fresh;
    }

    private static String sessionKey(Order order) {
        return "payLink:" + order.getOrderCode();
    }

    /** Địa chỉ gốc của web (có context path) dạng tuyệt đối, vì payOS cần URL đầy đủ để chuyển khách về. */
    private static String absolute(HttpServletRequest req) {
        int port = req.getServerPort();
        boolean defaultPort = ("http".equals(req.getScheme()) && port == 80) || ("https".equals(req.getScheme()) && port == 443);
        return req.getScheme() + "://" + req.getServerName() + (defaultPort ? "" : ":" + port) + req.getContextPath();
    }
}
