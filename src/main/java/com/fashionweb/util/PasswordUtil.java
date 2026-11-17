package com.fashionweb.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Băm và kiểm tra mật khẩu bằng PBKDF2-HMAC-SHA256 (có sẵn trong Java, không cần thư viện).
 *
 * <p>Chuỗi lưu trong users.password_hash: {@code pbkdf2_sha256$<số vòng>$<salt base64>$<băm base64>}.
 * Mỗi mật khẩu có salt ngẫu nhiên riêng nên hai người cùng mật khẩu vẫn ra hai chuỗi khác nhau.
 * Không bao giờ lưu hay ghi log mật khẩu gốc.
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(key);
    }

    /** true nếu password khớp chuỗi băm đã lưu. Chuỗi rỗng hoặc sai định dạng thì false. */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] salt = b64.decode(parts[2]);
        byte[] expected = b64.decode(parts[3]);
        byte[] actual = derive(password, salt, Integer.parseInt(parts[1]));
        // So sánh thời gian cố định, không lộ thông tin qua thời gian phản hồi
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
