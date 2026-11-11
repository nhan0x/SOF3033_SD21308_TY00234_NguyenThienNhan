package com.fashionweb.service.auth;

import com.fashionweb.constant.Role;
import com.fashionweb.repository.auth.UserRepository;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.User;
import com.fashionweb.util.Page;

/**
 * Quản lý người dùng cho Admin.
 *
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 */
public class UserService {

    private final UserRepository userRepository = new UserRepository();

    /**
     * Tìm theo email hoặc tên, lọc role, mới tạo trước. Mỗi User có orderCount (view-only), không kèm mật khẩu băm.
     */
    public Page<User> search(String q, Role role, int page, int pageSize) {
        return userRepository.findPage(q, role, page, pageSize);
    }

    /**
     * Người dùng theo id, null nếu không có.
     */
    public User findById(int id) {
        return userRepository.findById(id);
    }

    /**
     * Khóa hoặc mở khóa. Không cho Admin tự khóa chính mình. Trả trạng thái mới (true = đang hoạt động).
     */
    public boolean toggleActive(int adminId, int userId) {
        if (adminId == userId) {
            throw new ValidationException(null, "Bạn không thể tự khóa tài khoản của chính mình.");
        }
        User u = require(userId);
        userRepository.setActive(userId, !u.isActive());
        return !u.isActive();
    }

    /**
     * Đổi role. Không cho Admin tự đổi quyền của chính mình.
     */
    public void changeRole(int adminId, int userId, Role role) {
        if (role == null) {
            throw new ValidationException("role", "Chọn vai trò hợp lệ.");
        }
        if (adminId == userId) {
            throw new ValidationException("role", "Bạn không thể tự đổi quyền của chính mình.");
        }
        require(userId);
        userRepository.setRole(userId, role);
    }

    private User require(int userId) {
        User u = userRepository.findById(userId);
        if (u == null) {
            throw new ValidationException(null, "Người dùng không tồn tại.");
        }
        return u;
    }
}
