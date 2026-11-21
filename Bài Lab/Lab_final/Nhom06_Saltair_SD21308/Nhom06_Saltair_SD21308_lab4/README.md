# Lab 4 - Tiện ích, Filter, Đăng nhập, CRUD loại đồ uống

Lab 4 là **project web Maven** (`pom.xml`). Cách ráp project:

1. Copy thư mục `Lab_3/src/poly/coffee/{entity,util/JdbcUtil.java,dao}` vào `src/main/java/poly/coffee/`
   (`UserDAO.java` của Lab 4 ghi đè bản Lab 3 vì có thêm `findByEmail`).
2. Chạy `Lab_3/sql/Lab_3.sql`, sửa user/password trong `JdbcUtil`.
3. `mvn package` → deploy `target/PolyCoffee.war` lên Tomcat 9, mở `http://localhost:8080/PolyCoffee/`.

| Bài | File |
|---|---|
| 1 | `util/AuthUtil.java`, `util/ParamUtil.java` (+ `FileUtil`, `PasswordUtil` dùng cho các lab sau) |
| 2 | `filter/Utf8Filter.java`, `filter/AuthFilter.java` |
| 3 | `dao/UserDAO.findByEmail`, `servlet/AuthServlet.java`, `views/auth/login.jsp` |
| 4 | `servlet/CategoryServlet.java`, `views/manager/category/*.jsp` |

Tài khoản thử: `manager@polycoffee.com` (quản lý), `staff1@polycoffee.com` (nhân viên) - mật khẩu `123456`.
Nhân viên truy cập `/manager/*` sẽ nhận lỗi 403.
