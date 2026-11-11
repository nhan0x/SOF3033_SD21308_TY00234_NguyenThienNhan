package com.fashionweb.service.auth;

import com.fashionweb.constant.Role;
import com.fashionweb.repository.auth.LoginCodeRepository;
import com.fashionweb.repository.auth.LoginCodeRepository.RecentStats;
import com.fashionweb.repository.auth.LoginCodeRepository.VerifyResult;
import com.fashionweb.repository.auth.UserRepository;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.util.MailUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Đăng nhập bằng email + mã 6 số (#74). Không còn mật khẩu.
 *
 * <p>Khách: email chưa có tài khoản thì tự tạo CUSTOMER khi nhập đúng mã. Admin: luồng riêng /admin/login,
 * hạn mã ngắn hơn, không bao giờ tự tạo tài khoản và không cho biết email có phải admin hay không.
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 */
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern CODE = Pattern.compile("\\d{6}");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    /** Số giây tối thiểu giữa hai lần xin mã. */
    public static final int RESEND_SECONDS = 60;

    /** Field lỗi riêng khi nhập email admin ở luồng khách: Servlet hiện kèm link tới /admin/login. */
    public static final String ERROR_ADMIN = "admin";

    public static final String MSG_LOCKED = "Tài khoản đã bị khóa. Vui lòng liên hệ Saltair để được hỗ trợ.";

    /** Giới hạn theo luồng: hạn mã (phút), số lần nhập sai, số mã mỗi giờ. */
    public enum Flow {
        CUSTOMER(15, 5, 5),
        ADMIN(5, 3, 3);

        public final int minutes;
        public final int maxAttempts;
        public final int maxPerHour;

        Flow(int minutes, int maxAttempts, int maxPerHour) {
            this.minutes = minutes;
            this.maxAttempts = maxAttempts;
            this.maxPerHour = maxPerHour;
        }
    }

    private final UserRepository userRepository = new UserRepository();
    private final LoginCodeRepository codeRepository = new LoginCodeRepository();

    /** Chuẩn hóa email (trim, chữ thường). Sai định dạng thì ném ValidationException field "email". */
    public String normalizeEmail(String email) {
        String e = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (e.isEmpty()) {
            throw new ValidationException("email", "Vui lòng nhập email.");
        }
        if (e.length() > 150 || !EMAIL.matcher(e).matches()) {
            throw new ValidationException("email", "Email không đúng định dạng.");
        }
        return e;
    }

    /**
     * Tạo mã 6 số cho email (đã chuẩn hóa), hủy mã cũ, gửi mail (hoặc ghi log khi auth.devCode).
     *
     * <p>Luồng ADMIN: email không phải admin đang hoạt động thì không làm gì và trả null (Servlet vẫn báo chung chung).
     * Luồng CUSTOMER: email admin bị chặn (field {@link #ERROR_ADMIN}), tài khoản bị khóa bị chặn.
     *
     * @return mã gốc chỉ khi auth.devCode bật (để hiện trên trang), ngược lại null
     */
    public String requestCode(String email, Flow flow) {
        User u = userRepository.findByEmail(email);
        if (flow == Flow.ADMIN) {
            if (u == null || u.getRole() != Role.ADMIN || !u.isActive()) {
                return null;
            }
        } else if (u != null) {
            if (u.getRole() == Role.ADMIN) {
                throw new ValidationException(ERROR_ADMIN, "Tài khoản quản trị vui lòng đăng nhập tại trang quản trị.");
            }
            if (!u.isActive()) {
                throw new ValidationException("email", MSG_LOCKED);
            }
        }

        RecentStats stats = codeRepository.recentStats(email);
        if (stats.countLastHour() >= flow.maxPerHour
                || (stats.secondsSinceLast() >= 0 && stats.secondsSinceLast() < RESEND_SECONDS)) {
            throw new ValidationException("email", "Bạn thử lại sau ít phút.");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codeRepository.replace(email, hash(email, code), flow.minutes);

        if (MailUtil.isDevCode()) {
            LOG.info("[DEV] Mã đăng nhập cho " + email + ": " + code);
            return code;
        }
        try {
            MailUtil.send(email, code + " là mã đăng nhập Saltair của bạn", mailBody(code, flow));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không gửi được mã đăng nhập tới " + email, e);
            throw new ValidationException("email", "Không gửi được mã, vui lòng thử lại.");
        }
        return null;
    }

    /** Số giây còn phải chờ trước khi được gửi lại mã (0 nếu gửi được ngay). */
    public int resendWaitSeconds(String email) {
        int since = codeRepository.recentStats(email).secondsSinceLast();
        return since < 0 ? 0 : Math.max(0, RESEND_SECONDS - since);
    }

    /**
     * Kiểm tra mã. Đúng thì trả User (khách mới thì tạo tài khoản CUSTOMER). Sai thì ném ValidationException field "code".
     */
    public User verifyCode(String email, String code, Flow flow) {
        String c = code == null ? "" : code.replaceAll("\\s", "");
        if (!CODE.matcher(c).matches()) {
            throw new ValidationException("code", "Mã gồm 6 chữ số.");
        }
        VerifyResult r = codeRepository.verify(email, hash(email, c), flow.maxAttempts);
        switch (r.status()) {
            case WRONG -> throw new ValidationException("code", "Mã không đúng, bạn còn " + r.remaining() + " lần thử.");
            case LOCKED -> throw new ValidationException("code", "Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã mới.");
            case NONE -> throw new ValidationException("code", "Mã đã hết hạn hoặc không còn hiệu lực. Vui lòng gửi lại mã mới.");
            default -> {
                // OK
            }
        }

        User u = userRepository.findByEmail(email);
        if (flow == Flow.ADMIN) {
            if (u == null || u.getRole() != Role.ADMIN || !u.isActive()) {
                throw new ValidationException("code", "Mã đã hết hạn hoặc không còn hiệu lực. Vui lòng gửi lại mã mới.");
            }
            return u;
        }
        if (u == null) {
            u = new User();
            u.setEmail(email);
            String name = email.substring(0, email.indexOf('@'));
            u.setFullName(name.length() > 100 ? name.substring(0, 100) : name);
            u.setRole(Role.CUSTOMER);
            u.setActive(true);
            u.setId(userRepository.insert(u));
            return userRepository.findById(u.getId());
        }
        if (u.getRole() == Role.ADMIN) {
            throw new ValidationException(ERROR_ADMIN, "Tài khoản quản trị vui lòng đăng nhập tại trang quản trị.");
        }
        if (!u.isActive()) {
            throw new ValidationException("code", MSG_LOCKED);
        }
        return u;
    }

    /**
     * Gửi mail báo có lượt đăng nhập trang quản trị (chạy nền, lỗi chỉ ghi log). auth.devCode bật thì chỉ ghi log.
     */
    public void notifyAdminLogin(User admin, String ip, String userAgent) {
        String time = LocalDateTime.now().format(TIME);
        String body = "Tài khoản quản trị " + admin.getEmail() + " vừa đăng nhập Saltair.\n\n"
                + "Thời gian: " + time + "\n"
                + "Địa chỉ IP: " + ip + "\n"
                + "Trình duyệt: " + (userAgent == null ? "không rõ" : userAgent) + "\n\n"
                + "Nếu không phải bạn, hãy báo ngay cho trưởng nhóm.\n";
        if (MailUtil.isDevCode()) {
            LOG.info("[DEV] Admin " + admin.getEmail() + " đăng nhập lúc " + time + " từ " + ip);
            return;
        }
        Thread t = new Thread(() -> {
            try {
                MailUtil.send(admin.getEmail(), "Đăng nhập trang quản trị Saltair lúc " + time, body);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Không gửi được mail báo đăng nhập admin", e);
            }
        }, "admin-login-mail");
        t.setDaemon(true);
        t.start();
    }

    private static String mailBody(String code, Flow flow) {
        return "SALTAIR\n\n"
                + "Mã xác thực của bạn\n\n"
                + "    " + code + "\n\n"
                + "Mã chỉ dùng được một lần và hết hạn sau " + flow.minutes + " phút.\n"
                + "Nếu bạn không yêu cầu mã này, hãy bỏ qua email.\n";
    }

    /** SHA-256 hex của email + ":" + mã: không lưu mã gốc, và cùng một mã ở hai email cho hai băm khác nhau. */
    private static String hash(String email, String code) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest((email + ":" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : d) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
