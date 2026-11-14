package com.fashionweb.exception;

/** Thời gian chờ do server quyết định; UI dùng để hiển thị đếm ngược. */
public class RetryAfterException extends ValidationException {
    private final int retryAfterSeconds;

    public RetryAfterException(int seconds) {
        super("email", "Mã vừa được yêu cầu. Vui lòng kiểm tra hộp thư hoặc chờ " + seconds + " giây để yêu cầu mã mới.");
        this.retryAfterSeconds = seconds;
    }

    public int getRetryAfterSeconds() { return retryAfterSeconds; }
}
