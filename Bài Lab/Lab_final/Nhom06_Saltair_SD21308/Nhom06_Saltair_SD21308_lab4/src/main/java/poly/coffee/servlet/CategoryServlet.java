package poly.coffee.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import poly.coffee.dao.CategoryDAO;
import poly.coffee.entity.Category;
import poly.coffee.util.ParamUtil;

/**
 * CRUD loai do uong (chi quan ly - AuthFilter chan /manager/*)
 *   GET  /manager/categories           danh sach
 *   GET  /manager/categories/new       form them
 *   GET  /manager/categories/edit?id=  form sua
 *   POST /manager/categories/save      them / cap nhat
 *   POST /manager/categories/delete    xoa
 */
@WebServlet("/manager/categories/*")
public class CategoryServlet extends HttpServlet {
    private final CategoryDAO dao = new CategoryDAO();
    private static final String LIST = "/WEB-INF/views/manager/category/list.jsp";
    private static final String FORM = "/WEB-INF/views/manager/category/form.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "/new":
                req.setAttribute("category", new Category());
                req.getRequestDispatcher(FORM).forward(req, resp);
                break;
            case "/edit":
                Category c = dao.findById(ParamUtil.getInt(req, "id", 0));
                if (c == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Khong tim thay loai do uong");
                    return;
                }
                req.setAttribute("category", c);
                req.getRequestDispatcher(FORM).forward(req, resp);
                break;
            default:
                req.setAttribute("categories", dao.findAll());
                req.getRequestDispatcher(LIST).forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        String base = req.getContextPath() + "/manager/categories";
        if (path.equals("/delete")) {
            int id = ParamUtil.getInt(req, "id", 0);
            if (dao.countDrinks(id) > 0) {
                req.getSession().setAttribute("flashError", "Không thể xóa: loại này đang có đồ uống");
            } else {
                dao.deleteById(id);
                req.getSession().setAttribute("flashOk", "Đã xóa loại đồ uống");
            }
            resp.sendRedirect(base);
            return;
        }
        if (path.equals("/save")) {
            Category c = new Category(ParamUtil.getInt(req, "id", 0), ParamUtil.getString(req, "name", ""));
            String error = null;
            if (c.getName().isEmpty()) error = "Tên loại không được để trống";
            else if (c.getName().length() > 100) error = "Tên loại tối đa 100 ký tự";
            if (error != null) {
                req.setAttribute("error", error);
                req.setAttribute("category", c);
                req.getRequestDispatcher(FORM).forward(req, resp);
                return;
            }
            try {
                if (c.getId() == 0) dao.create(c); else dao.update(c);
            } catch (RuntimeException e) {          // vi pham UNIQUE(name)
                req.setAttribute("error", "Tên loại đã tồn tại");
                req.setAttribute("category", c);
                req.getRequestDispatcher(FORM).forward(req, resp);
                return;
            }
            req.getSession().setAttribute("flashOk", "Đã lưu loại đồ uống");
            resp.sendRedirect(base);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
