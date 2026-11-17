package com.fashionweb.exception;

/** Voucher không dùng được. getMessage() là câu tiếng Việt hiện cho khách (ví dụ "Mã giảm giá đã hết hạn"). */
public class VoucherException extends Exception {

    public VoucherException(String message) {
        super(message);
    }
}
