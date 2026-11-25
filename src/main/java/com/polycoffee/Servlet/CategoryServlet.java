package com.polycoffee.Servlet;

import com.polycoffee.DAO.CategoryDAO;
import com.polycoffee.Entity.Category;
import com.polycoffee.Util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Quản lý loại đồ uống (BR-09, BR-10). AuthFilter đã giới hạn /manager/* cho quản lý.
 * <pre>
 *   GET  /manager/categories           danh sách
 *   GET  /manager/categories/new       form thêm
 *   GET  /manager/categories/edit?id=  form sửa
 *   POST /manager/categories/save      thêm hoặc cập nhật
 *   POST /manager/categories/delete    xóa
 * </pre>
 */
@WebServlet("/manager/categories/*")
public class CategoryServlet extends HttpServlet {

    static final String LIST = "/WEB-INF/views/manager/category/list.jsp";
    static final String FORM = "/WEB-INF/views/manager/category/form.jsp";
    static final int NAME_MAX = 100;

    private final CategoryDAO dao;

    public CategoryServlet() {
        this(new CategoryDAO());
    }

    CategoryServlet(CategoryDAO dao) {
        this.dao = dao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "":
            case "/":
                req.setAttribute("categories", dao.findAll());
                req.getRequestDispatcher(LIST).forward(req, resp);
                break;
            case "/new":
                req.setAttribute("category", new Category());
                req.getRequestDispatcher(FORM).forward(req, resp);
                break;
            case "/edit":
                Category c = dao.findById(ParamUtil.getInt(req, "id", 0));
                if (c == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy loại đồ uống");
                    return;
                }
                req.setAttribute("category", c);
                req.getRequestDispatcher(FORM).forward(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        String base = req.getContextPath() + "/manager/categories";
        switch (path) {
            case "/delete":
                delete(req, resp, base);
                break;
            case "/save":
                save(req, resp, base);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp, String base) throws IOException {
        int id = ParamUtil.getInt(req, "id", 0);
        if (dao.countDrinks(id) > 0) {
            req.getSession().setAttribute("flashError", "Không thể xóa: loại này đang có đồ uống");
        } else {
            dao.deleteById(id);
            req.getSession().setAttribute("flashOk", "Đã xóa loại đồ uống");
        }
        resp.sendRedirect(base);
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, String base)
            throws ServletException, IOException {
        Category c = new Category(ParamUtil.getInt(req, "id", 0), ParamUtil.getString(req, "name", ""));
        String error = null;
        if (c.getName().isEmpty()) {
            error = "Tên loại không được để trống";
        } else if (c.getName().length() > NAME_MAX) {
            error = "Tên loại tối đa " + NAME_MAX + " ký tự";
        } else if (dao.existsByName(c.getName(), c.getId())) {
            error = "Tên loại đã tồn tại";
        }
        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute("category", c);
            req.getRequestDispatcher(FORM).forward(req, resp);
            return;
        }
        if (c.getId() == 0) {
            dao.create(c);
        } else {
            dao.update(c);
        }
        req.getSession().setAttribute("flashOk", "Đã lưu loại đồ uống");
        resp.sendRedirect(base);
    }
}
