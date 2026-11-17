/**
 * CHỨC NĂNG: Đăng nhập (email + mật khẩu), đăng ký, đăng xuất. Khách và Admin dùng chung form /login.
 *
 * Servlet: LoginServlet (/login, kèm /admin/login cũ chuyển về /login), RegisterServlet (/register), LogoutServlet.
 * Service: AuthService (đăng nhập, đăng ký, đổi mật khẩu). Mật khẩu băm bằng util/PasswordUtil.
 * Filter: AuthFilter (bắt buộc đăng nhập), AdminFilter (chỉ ADMIN vào /admin/*).
 * JSP: WEB-INF/views/auth/login.jsp, register.jsp.
 * Luồng: Servlet -> Service -> DAO -> MySQL.
 */
package com.fashionweb.controller.auth;
