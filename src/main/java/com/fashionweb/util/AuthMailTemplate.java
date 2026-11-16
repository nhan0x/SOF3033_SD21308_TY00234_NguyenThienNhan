package com.fashionweb.util;

/** Mẫu xác thực dùng chung; nội dung lấy từ mục đích yêu cầu, không nhận HTML từ người dùng. */
public final class AuthMailTemplate {
    private AuthMailTemplate() {}

    public enum Purpose {
        LOGIN("Đăng nhập vào Saltair", "Bạn vừa yêu cầu đăng nhập vào tài khoản Saltair. Nhập mã bên dưới trên trang xác thực để tiếp tục.", "đăng nhập"),
        REGISTRATION("Chào mừng bạn đến với Saltair", "Xác thực email để tạo tài khoản Saltair và bắt đầu hành trình mua sắm của bạn. Nhập mã bên dưới trên trang xác thực để tiếp tục.", "xác thực email"),
        ADMIN_LOGIN("Xác thực đăng nhập quản trị", "Bạn vừa yêu cầu truy cập trang quản trị Saltair. Nhập mã bên dưới trên trang xác thực để tiếp tục.", "đăng nhập quản trị");

        private final String title;
        private final String description;
        private final String action;
        Purpose(String title, String description, String action) {
            this.title = title;
            this.description = description;
            this.action = action;
        }
    }

    public record Content(String subject, String text, String html) {}

    public static Content verification(String code, int minutes, Purpose purpose) {
        if (code == null || !code.matches("[0-9]{6}") || minutes < 1 || purpose == null) {
            throw new IllegalArgumentException("Thông tin email xác thực không hợp lệ");
        }
        String subject = code + " là mã " + purpose.action + " Saltair của bạn";
        String text = "SALTAIR\n\n" + purpose.title + "\n\n" + purpose.description
                + "\n\nMã xác thực: " + code + "\nCó hiệu lực trong " + minutes
                + " phút và chỉ dùng một lần.\nKhông chia sẻ mã này với bất kỳ ai, kể cả người tự xưng là nhân viên Saltair."
                + "\nNếu bạn không gửi yêu cầu này, hãy bỏ qua email.\n\nSaltair · Everyday essentials.\nEmail tự động, vui lòng không trả lời.";
        String html = """
                <!doctype html>
                <html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                <body style="margin:0;padding:0;background-color:#f3f5f4;color:#193b36;font-family:Arial,Helvetica,sans-serif;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;">Mã %s của bạn có hiệu lực trong %d phút.</div>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f3f5f4;"><tr><td align="center" style="padding:40px 16px;">
                <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="width:100%%;max-width:560px;">
                <tr><td style="padding:30px 32px;background-color:#193b36;border-radius:16px 16px 0 0;">
                <div style="font-size:27px;letter-spacing:6px;font-weight:bold;color:#ffffff;">SALTAIR</div>
                <div style="margin-top:8px;font-size:11px;letter-spacing:2px;color:#cbdcd4;">EVERYDAY ESSENTIALS</div></td></tr>
                <tr><td style="padding:36px 32px;background-color:#ffffff;border-radius:0 0 16px 16px;">
                <div style="font-size:11px;letter-spacing:2px;color:#658078;font-weight:bold;">XÁC THỰC TÀI KHOẢN</div>
                <h1 style="margin:14px 0 16px;font-size:27px;line-height:1.3;color:#193b36;">%s</h1>
                <p style="margin:0 0 26px;font-size:15px;line-height:1.8;color:#596b65;">%s</p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"><tr><td align="center" style="padding:24px 12px;background-color:#edf3ef;border:1px solid #dce7df;border-radius:12px;">
                <div style="font-size:12px;color:#596b65;">Mã xác thực của bạn</div>
                <div dir="ltr" style="margin:12px 0;font-family:Consolas,monospace;font-size:38px;line-height:1.3;font-weight:bold;letter-spacing:8px;color:#193b36;">%s</div>
                <div style="font-size:12px;color:#596b65;">Có hiệu lực trong <strong>%d phút</strong> · Chỉ dùng một lần</div>
                </td></tr></table>
                <p style="margin:24px 0 0;font-size:13px;line-height:1.8;color:#596b65;"><strong>Bảo vệ tài khoản của bạn</strong><br>Không chia sẻ mã này với bất kỳ ai, kể cả người tự xưng là nhân viên Saltair.</p>
                <p style="margin:20px 0 0;padding-top:20px;border-top:1px solid #e9eeeb;font-size:12px;line-height:1.8;color:#7a8781;">Nếu bạn không gửi yêu cầu này, hãy bỏ qua email.</p>
                </td></tr>
                <tr><td align="center" style="padding:24px 12px;font-size:11px;line-height:1.9;color:#7a8781;">Saltair · Phong cách mỗi ngày, thoải mái là bạn.<br>Email được gửi tự động. Vui lòng không trả lời email này.</td></tr>
                </table></td></tr></table></body></html>
                """.formatted(purpose.action, minutes, purpose.title, purpose.description, code, minutes);
        return new Content(subject, text, html);
    }
}
