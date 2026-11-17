package com.fashionweb.service.auth;

import com.fashionweb.constant.Role;
import com.fashionweb.dao.auth.UserDAO;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.util.MailUtil;
import com.fashionweb.util.PasswordUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Đăng ký, đăng nhập bằng email + mật khẩu, đổi mật khẩu.
 *
 * <p>Khách và Admin dùng chung một form /login; vai trò quyết định trang đến sau khi đăng nhập. Sai email hay sai
 * mật khẩu đều báo cùng một câu để không lộ email nào đã có tài khoản.
 * <p>Luồng gọi: Servlet -> Service -> DAO -> MySQL.
 */
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;

    public static final String MSG_LOCKED = "Tài khoản đã bị khóa. Vui lòng liên hệ Saltair để được hỗ trợ.";
    private static final String MSG_WRONG = "Email hoặc mật khẩu không đúng.";
    private static final String DUMMY_HASH = PasswordUtil.hash("saltair-dummy-password");

    private final UserDAO userDAO = new UserDAO();

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
     * Đăng nhập (khách hoặc Admin). Sai thì ném ValidationException: field "email" nếu sai định dạng,
     * field "password" nếu thiếu, sai mật khẩu hoặc tài khoản bị khóa.
     */
    public User login(String email, String password) {
        String e = normalizeEmail(email);
        if (password == null || password.isEmpty()) {
            throw new ValidationException("password", "Vui lòng nhập mật khẩu.");
        }
        User u = userDAO.findByEmail(e);
        // Email không tồn tại vẫn băm thử một lần, để thời gian phản hồi giống nhau và không đoán được email nào có tài khoản
        String stored = u == null ? null : userDAO.findPasswordHash(u.getId());
        boolean ok = PasswordUtil.verify(password, stored == null ? DUMMY_HASH : stored) && stored != null;
        if (!ok) {
            throw new ValidationException("password", MSG_WRONG);
        }
        if (!u.isActive()) {
            throw new ValidationException("password", MSG_LOCKED);
        }
        return u;
    }

    /**
     * Tạo tài khoản CUSTOMER mới. Lỗi theo từng ô: email, fullName, password, confirm.
     *
     * <p>Email đã có tài khoản nhưng CHƯA có mật khẩu (tạo lúc khách đặt hàng không đăng nhập) thì đặt mật khẩu cho
     * tài khoản đó, để khách xem lại được các đơn cũ. Lưu ý: chưa xác thực email nên ai biết email cũng làm được việc này.
     */
    public User register(String email, String fullName, String password, String confirm) {
        Map<String, String> errors = new LinkedHashMap<>();
        String e = null;
        User existing = null;
        try {
            e = normalizeEmail(email);
            existing = userDAO.findByEmail(e);
            if (existing != null && (existing.getRole() != Role.CUSTOMER || userDAO.findPasswordHash(existing.getId()) != null)) {
                errors.put("email", "Email này đã có tài khoản.");
            }
        } catch (ValidationException ex) {
            errors.putAll(ex.getErrors());
        }
        String name = fullName == null ? "" : fullName.trim();
        if (name.isEmpty()) {
            errors.put("fullName", "Vui lòng nhập họ tên.");
        } else if (name.length() > 100) {
            errors.put("fullName", "Họ tên tối đa 100 ký tự.");
        }
        checkNewPassword(password, confirm, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (existing != null && !existing.isActive()) {
            throw new ValidationException("email", MSG_LOCKED);
        }
        if (existing != null) {
            userDAO.updateProfile(existing.getId(), name, existing.getPhone());
            userDAO.updatePasswordHash(existing.getId(), PasswordUtil.hash(password));
            return userDAO.findById(existing.getId());
        }

        User u = new User();
        u.setEmail(e);
        u.setFullName(name);
        u.setRole(Role.CUSTOMER);
        u.setActive(true);
        int id = userDAO.insert(u, PasswordUtil.hash(password));
        return userDAO.findById(id);
    }

    /** Đổi mật khẩu khi đã đăng nhập. Lỗi theo ô: currentPassword, newPassword, confirm. */
    public void changePassword(int userId, String current, String password, String confirm) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!PasswordUtil.verify(current, userDAO.findPasswordHash(userId))) {
            errors.put("currentPassword", "Mật khẩu hiện tại không đúng.");
        }
        Map<String, String> pwErrors = new LinkedHashMap<>();
        checkNewPassword(password, confirm, pwErrors);
        if (pwErrors.containsKey("password")) {
            errors.put("newPassword", pwErrors.get("password"));
        }
        if (pwErrors.containsKey("confirm")) {
            errors.put("confirm", pwErrors.get("confirm"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        userDAO.updatePasswordHash(userId, PasswordUtil.hash(password));
    }

    private static void checkNewPassword(String password, String confirm, Map<String, String> errors) {
        if (password == null || password.length() < PASSWORD_MIN) {
            errors.put("password", "Mật khẩu tối thiểu " + PASSWORD_MIN + " ký tự.");
        } else if (password.length() > PASSWORD_MAX) {
            errors.put("password", "Mật khẩu tối đa " + PASSWORD_MAX + " ký tự.");
        } else if (!password.equals(confirm)) {
            errors.put("confirm", "Mật khẩu nhập lại không khớp.");
        }
    }

    /**
     * Tài khoản khách hàng để gắn đơn của khách chưa đăng nhập: email đã có thì dùng tài khoản đó, chưa có thì tạo mới
     * (chưa có mật khẩu, không đăng nhập được). KHÔNG đăng nhập khách vào tài khoản này. Email sai, thuộc Admin hoặc
     * tài khoản bị khóa thì ném ValidationException field "email".
     */
    public User findOrCreateCustomer(String email, String fullName, String phone) {
        String e = normalizeEmail(email);
        User u = userDAO.findByEmail(e);
        if (u == null) {
            u = new User();
            u.setEmail(e);
            u.setFullName(fullName);
            u.setPhone(phone);
            u.setRole(Role.CUSTOMER);
            u.setActive(true);
            u.setId(userDAO.insert(u, null));
            return u;
        }
        if (u.getRole() == Role.ADMIN || !u.isActive()) {
            throw new ValidationException("email", "Email này không dùng để đặt hàng được. Vui lòng dùng email khác.");
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
                + "Nếu không phải bạn, hãy đổi mật khẩu ngay.\n";
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
}
