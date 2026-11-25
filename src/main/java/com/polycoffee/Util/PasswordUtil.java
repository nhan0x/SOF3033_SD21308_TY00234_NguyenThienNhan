package com.polycoffee.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** Băm mật khẩu SHA-256 (BR-07) và sinh mật khẩu ngẫu nhiên (BR-19). */
public final class PasswordUtil {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** SHA-256 dạng hex thường, 64 ký tự. */
    public static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** So sánh mật khẩu nhập vào với bản băm đã lưu, thời gian không phụ thuộc vị trí khác biệt. */
    public static boolean matches(String raw, String storedHash) {
        if (raw == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(raw).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    /** Mật khẩu ngẫu nhiên, bỏ các ký tự dễ nhầm (0/O, 1/l/I). */
    public static String random(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
