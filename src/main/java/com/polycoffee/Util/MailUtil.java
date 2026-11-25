package com.polycoffee.Util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Gửi email qua SMTP (dùng cho cấp lại mật khẩu, BR-19).
 *
 * <p>Thông tin đọc từ biến môi trường, không ghi trong mã nguồn (BR-20):
 * <ul>
 *   <li>{@code MAIL_USER}, {@code MAIL_PASSWORD}: tài khoản SMTP (Gmail cần App Password)</li>
 *   <li>{@code MAIL_HOST} (mặc định smtp.gmail.com), {@code MAIL_PORT} (mặc định 587)</li>
 * </ul>
 * Khi đặt {@code MAIL_HOST} mà không đặt {@code MAIL_USER} thì gửi không xác thực
 * (dùng cho máy chủ giả lập như Mailpit: MAIL_HOST=localhost MAIL_PORT=1025).
 * Không bao giờ ghi giá trị mật khẩu ra log.
 */
public final class MailUtil {

    private MailUtil() {
    }

    /** @throws IllegalStateException chưa cấu hình mail; @throws MessagingException gửi thất bại */
    public static void send(String to, String subject, String body) throws MessagingException {
        String user = env("MAIL_USER");
        String password = env("MAIL_PASSWORD");
        String host = env("MAIL_HOST");
        boolean auth = user != null;

        if (host == null) {
            if (user == null || password == null) {
                throw new IllegalStateException("Chưa cấu hình MAIL_USER và MAIL_PASSWORD");
            }
            host = "smtp.gmail.com";
        } else if (auth && password == null) {
            throw new IllegalStateException("Đã đặt MAIL_USER nhưng thiếu MAIL_PASSWORD");
        }
        String port = env("MAIL_PORT") != null ? env("MAIL_PORT") : "587";

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(auth));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        final String u = user;
        final String p = password;
        Session session = auth
                ? Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(u, p);
                    }
                })
                : Session.getInstance(props);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(auth ? user : "no-reply@polycoffee.local"));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject, "UTF-8");
        msg.setText(body, "UTF-8");
        Transport.send(msg);
    }

    private static String env(String name) {
        String v = System.getenv(name);
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
