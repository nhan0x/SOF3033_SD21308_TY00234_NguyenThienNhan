/**
 * CHỨC NĂNG: Đăng nhập, đăng ký, quên và đặt lại mật khẩu, đăng xuất, phân quyền, quản lý người dùng (Admin).
 *
 * Servlet: LoginServlet, RegisterServlet, ForgotPasswordServlet, ResetPasswordServlet, LogoutServlet.
 * Khung đã có: AuthService, UserService, UserRepository, AuthFilter (bắt buộc đăng nhập), AdminFilter (chỉ ADMIN vào /admin/*).
 * JSP: WEB-INF/views/auth/.
 * Luồng: Servlet -> Service -> Repository -> MySQL.
 */
package com.fashionweb.controller.auth;
