package com.fashionweb.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Gửi email qua SMTP (dùng cho mã đăng nhập 6 số, #74).
 * Đọc thông tin từ src/main/resources/mail.properties (sao chép từ mail.properties.example).
 *
 * Không có mail.properties thì ứng dụng vẫn chạy bình thường; chỉ khi gọi {@link #send} mới báo lỗi.
 * Bật auth.devCode=true để không gửi mail mà ghi mã vào log và hiện trên trang nhập mã (dùng cho tài khoản mẫu).
 */
public final class MailUtil {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = MailUtil.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private MailUtil() {
    }

    /** true khi mail.properties có auth.devCode=true (mặc định false). */
    public static boolean isDevCode() {
        return Boolean.parseBoolean(PROPS.getProperty("auth.devCode", "false").trim());
    }

    /**
     * Gửi một email văn bản thuần (UTF-8, có dấu tiếng Việt).
     *
     * @throws MessagingException nếu SMTP từ chối hoặc không kết nối được
     * @throws IllegalStateException nếu chưa có mail.properties hoặc thiếu username/password
     */
    public static void send(String to, String subject, String body) throws MessagingException {
        String host = PROPS.getProperty("mail.host");
        String username = PROPS.getProperty("mail.username");
        String password = PROPS.getProperty("mail.password");
        if (host == null || username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu mail.properties: hãy sao chép mail.properties.example thành mail.properties và điền thông tin");
        }

        Properties smtp = new Properties();
        smtp.put("mail.smtp.host", host);
        smtp.put("mail.smtp.port", PROPS.getProperty("mail.port", "587"));
        smtp.put("mail.smtp.auth", "true");
        smtp.put("mail.smtp.starttls.enable", "true");
        smtp.put("mail.smtp.connectiontimeout", "10000");
        smtp.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(smtp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(username, PROPS.getProperty("mail.from.name", "Saltair"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Tên người gửi không hợp lệ", e);
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");
        Transport.send(message);
    }
}
