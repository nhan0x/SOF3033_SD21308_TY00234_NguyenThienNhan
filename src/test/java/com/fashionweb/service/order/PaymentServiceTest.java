package com.fashionweb.service.order;

import com.fashionweb.constant.OrderStatus;
import com.fashionweb.constant.PaymentMethod;
import com.fashionweb.constant.PaymentStatus;
import com.fashionweb.model.Order;
import com.fashionweb.service.order.PaymentService.Outcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test phần quyết định của PaymentService (không cần database): đổi mã đơn sang mã payOS và ngược lại,
 * và quyết định kết quả cho một khoản tiền vào.
 */
class PaymentServiceTest {

    private static Order order(PaymentMethod method, OrderStatus status, PaymentStatus payment, long total) {
        Order o = new Order();
        o.setPaymentMethod(method);
        o.setStatus(status);
        o.setPaymentStatus(payment);
        o.setTotalAmount(total);
        return o;
    }

    private static Order unpaidTransfer(long total) {
        return order(PaymentMethod.BANK_TRANSFER, OrderStatus.PENDING, PaymentStatus.UNPAID, total);
    }

    // ---------- đổi mã đơn <-> mã payOS ----------

    @Test
    @DisplayName("Lần tạo link đầu tiên: mã payOS = phần số của mã đơn + 1 bước")
    void toPayOSCodeFirstAttempt() {
        assertEquals(120261117001L, PaymentService.toPayOSCode("SA20261117001", 1));
    }

    @Test
    @DisplayName("Mỗi lần tạo link lại có mã payOS khác nhau")
    void toPayOSCodeDiffersPerAttempt() {
        long first = PaymentService.toPayOSCode("SA20261117001", 1);
        long second = PaymentService.toPayOSCode("SA20261117001", 2);
        assertEquals(100_000_000_000L, second - first);
    }

    @Test
    @DisplayName("Đổi ngược mã payOS về đúng mã đơn, bất kể là lần thử thứ mấy")
    void fromPayOSCodeIgnoresAttempt() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertEquals("SA20261117001", PaymentService.fromPayOSCode(PaymentService.toPayOSCode("SA20261117001", attempt)));
        }
    }

    @Test
    @DisplayName("Mã đơn có số đầu là 0 vẫn đổi qua lại đúng (giữ đủ 11 chữ số)")
    void roundTripWithLeadingZeros() {
        assertEquals("SA00000000007", PaymentService.fromPayOSCode(PaymentService.toPayOSCode("SA00000000007", 3)));
    }

    @Test
    @DisplayName("Mã đơn sai định dạng bị từ chối")
    void toPayOSCodeRejectsBadCode() {
        assertThrows(IllegalArgumentException.class, () -> PaymentService.toPayOSCode("DH123", 1));
        assertThrows(IllegalArgumentException.class, () -> PaymentService.toPayOSCode(null, 1));
        assertThrows(IllegalArgumentException.class, () -> PaymentService.toPayOSCode("SA2026111700", 1));
    }

    // ---------- decide ----------

    @Test
    @DisplayName("Đủ tiền, đơn chuyển khoản chưa trả: PAID")
    void exactAmountIsPaid() {
        assertEquals(Outcome.PAID, PaymentService.decide(unpaidTransfer(500_000), 500_000));
    }

    @Test
    @DisplayName("Chuyển dư tiền vẫn PAID")
    void overpaymentIsPaid() {
        assertEquals(Outcome.PAID, PaymentService.decide(unpaidTransfer(500_000), 600_000));
    }

    @Test
    @DisplayName("Thiếu 1 đồng: AMOUNT_TOO_LOW, không đánh dấu đã thanh toán")
    void oneDongShortIsTooLow() {
        assertEquals(Outcome.AMOUNT_TOO_LOW, PaymentService.decide(unpaidTransfer(500_000), 499_999));
    }

    @Test
    @DisplayName("Không tìm thấy đơn: NO_ORDER")
    void noOrder() {
        assertEquals(Outcome.NO_ORDER, PaymentService.decide(null, 500_000));
    }

    @Test
    @DisplayName("Đơn COD nhận được tiền chuyển khoản: WRONG_METHOD")
    void codOrderIsWrongMethod() {
        Order cod = order(PaymentMethod.COD, OrderStatus.PENDING, PaymentStatus.UNPAID, 500_000);
        assertEquals(Outcome.WRONG_METHOD, PaymentService.decide(cod, 500_000));
    }

    @Test
    @DisplayName("Đơn đã hủy nhận được tiền: CANCELLED (cần hoàn tiền thủ công)")
    void cancelledOrder() {
        Order o = order(PaymentMethod.BANK_TRANSFER, OrderStatus.CANCELLED, PaymentStatus.UNPAID, 500_000);
        assertEquals(Outcome.CANCELLED, PaymentService.decide(o, 500_000));
    }

    @Test
    @DisplayName("Đơn đã thanh toán, tiền về lần nữa: ALREADY_PAID")
    void alreadyPaidOrder() {
        Order o = order(PaymentMethod.BANK_TRANSFER, OrderStatus.PENDING, PaymentStatus.PAID, 500_000);
        assertEquals(Outcome.ALREADY_PAID, PaymentService.decide(o, 500_000));
    }

    @Test
    @DisplayName("Đơn đang giao (chưa trả) nhận đủ tiền vẫn PAID")
    void shippingOrderCanStillBePaid() {
        Order o = order(PaymentMethod.BANK_TRANSFER, OrderStatus.SHIPPING, PaymentStatus.UNPAID, 500_000);
        assertEquals(Outcome.PAID, PaymentService.decide(o, 500_000));
    }
}
