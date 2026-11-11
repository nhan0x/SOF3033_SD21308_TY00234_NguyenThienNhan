package com.fashionweb.service.cart;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.constant.DiscountType;
import com.fashionweb.exception.VoucherException;
import com.fashionweb.model.Voucher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test cho VoucherService ở các hàm không cần database:
 * tính tiền giảm (computeDiscount), phí ship (shippingFee) và bước 1 của validate (mã rỗng).
 */
class VoucherServiceTest {

    private final VoucherService service = new VoucherService();

    private static Voucher percent(long value, Long maxDiscount) {
        Voucher v = new Voucher();
        v.setDiscountType(DiscountType.PERCENT);
        v.setDiscountValue(value);
        v.setMaxDiscount(maxDiscount);
        return v;
    }

    private static Voucher amount(long value) {
        Voucher v = new Voucher();
        v.setDiscountType(DiscountType.AMOUNT);
        v.setDiscountValue(value);
        return v;
    }

    @ParameterizedTest(name = "{0}% của {1}đ, trần {2} -> {3}đ")
    @DisplayName("computeDiscount PERCENT: tính % và áp trần maxDiscount")
    @CsvSource({
            // percent, subtotal, maxDiscount (rỗng = không trần), expected
            "10, 500000,       , 50000",
            "10, 500000, 30000 , 30000",   // vượt trần -> lấy trần
            "10, 300000, 30000 , 30000",   // đúng bằng trần
            "10, 299999, 30000 , 29999",   // ngay dưới trần (làm tròn xuống)
            "100, 200000,      , 200000",  // giảm 100% không vượt tổng tiền
            "15, 1,            , 0"        // tiền rất nhỏ -> 0
    })
    void computeDiscount_percent(long pct, long subtotal, Long max, long expected) {
        assertEquals(expected, service.computeDiscount(percent(pct, max), subtotal));
    }

    @ParameterizedTest(name = "giảm {0}đ cho đơn {1}đ -> {2}đ")
    @DisplayName("computeDiscount AMOUNT: không vượt tổng tiền hàng")
    @CsvSource({
            "50000, 500000, 50000",
            "50000, 50000,  50000",   // biên: bằng tổng tiền
            "50000, 49999,  49999",   // biên: tổng tiền nhỏ hơn mức giảm
            "50000, 1,      1"
    })
    void computeDiscount_amount(long value, long subtotal, long expected) {
        assertEquals(expected, service.computeDiscount(amount(value), subtotal));
    }

    @ParameterizedTest
    @DisplayName("computeDiscount: tổng tiền <= 0 thì không giảm")
    @ValueSource(longs = {0, -1, -100000})
    void computeDiscount_nonPositiveSubtotal_returnsZero(long subtotal) {
        assertEquals(0, service.computeDiscount(amount(50000), subtotal));
    }

    @Test
    @DisplayName("computeDiscount: không có voucher thì không giảm")
    void computeDiscount_nullVoucher_returnsZero() {
        assertEquals(0, service.computeDiscount(null, 500000));
    }

    @Test
    @DisplayName("shippingFee: có voucher hợp lệ thì miễn phí ship")
    void shippingFee_withVoucher_isFree() {
        assertEquals(0, service.shippingFee(amount(50000)));
    }

    @Test
    @DisplayName("shippingFee: không có voucher thì tính phí cố định")
    void shippingFee_withoutVoucher_isFixedFee() {
        assertEquals(AppConstant.SHIPPING_FEE, service.shippingFee(null));
    }

    @ParameterizedTest
    @DisplayName("validate: mã rỗng ném VoucherException 'không tồn tại'")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void validate_blankCode_throws(String code) {
        VoucherException ex = assertThrows(VoucherException.class, () -> service.validate(code, 1, null));
        assertEquals("Mã giảm giá không tồn tại.", ex.getMessage());
    }
}
