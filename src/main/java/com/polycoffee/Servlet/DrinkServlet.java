package com.polycoffee.Servlet;

import com.polycoffee.DAO.CategoryDAO;
import com.polycoffee.DAO.DrinkDAO;
import com.polycoffee.Entity.Category;
import com.polycoffee.Entity.Drink;
import com.polycoffee.Util.FileUtil;
import com.polycoffee.Util.Page;
import com.polycoffee.Util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý đồ uống (BR-11 đến BR-14).
 * <pre>
 *   GET  /manager/drinks?keyword=&amp;categoryId=&amp;status=&amp;page=   danh sách, tìm kiếm, phân trang
 *   GET  /manager/drinks/new | /edit?id=                        form
 *   POST /manager/drinks/save                                   thêm hoặc cập nhật (multipart, có ảnh)
 *   POST /manager/drinks/delete                                 xóa
 * </pre>
 */
@WebServlet("/manager/drinks/*")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class DrinkServlet extends HttpServlet {

    static final String LIST = "/WEB-INF/views/manager/drink/list.jsp";
    static final String FORM = "/WEB-INF/views/manager/drink/form.jsp";
    static final int NAME_MAX = 100;

    private final DrinkDAO drinkDAO;
    private final CategoryDAO categoryDAO;

    public DrinkServlet() {
        this(new DrinkDAO(), new CategoryDAO());
    }

    DrinkServlet(DrinkDAO drinkDAO, CategoryDAO categoryDAO) {
        this.drinkDAO = drinkDAO;
        this.categoryDAO = categoryDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "":
            case "/":
                list(req, resp);
                break;
            case "/new":
                Drink fresh = new Drink();
                fresh.setAvailable(true);
                showForm(req, resp, fresh, null);
                break;
            case "/edit":
                Drink d = drinkDAO.findById(ParamUtil.getInt(req, "id", 0));
                if (d == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đồ uống");
                    return;
                }
                showForm(req, resp, d, null);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        switch (path) {
            case "/save":
                save(req, resp);
                break;
            case "/delete":
                delete(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------ danh sách

    private void list(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = ParamUtil.getString(req, "keyword", "");
        int categoryId = ParamUtil.getInt(req, "categoryId", 0);
        String status = ParamUtil.getString(req, "status", "");
        Boolean available = status.equals("1") ? Boolean.TRUE : status.equals("0") ? Boolean.FALSE : null;
        if (!status.equals("1") && !status.equals("0")) {
            status = "";
        }
        int page = ParamUtil.getInt(req, "page", 1);

        Page<Drink> result = drinkDAO.search(keyword.isEmpty() ? null : keyword,
                categoryId > 0 ? categoryId : null, available, page, Page.SIZE);

        List<Category> categories = categoryDAO.findAll();
        Map<Integer, String> categoryNames = new LinkedHashMap<>();
        for (Category c : categories) {
            categoryNames.put(c.getId(), c.getName());
        }
        req.setAttribute("pg", result);
        req.setAttribute("categories", categories);
        req.setAttribute("categoryNames", categoryNames);
        req.setAttribute("keyword", keyword);
        req.setAttribute("categoryId", categoryId);
        req.setAttribute("status", status);
        req.setAttribute("pagerBase", req.getContextPath() + "/manager/drinks?keyword="
                + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&categoryId=" + (categoryId > 0 ? categoryId : "")
                + "&status=" + status + "&");
        req.getRequestDispatcher(LIST).forward(req, resp);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Drink drink, String error)
            throws ServletException, IOException {
        req.setAttribute("drink", drink);
        req.setAttribute("categories", categoryDAO.findAll());
        req.setAttribute("error", error);
        req.getRequestDispatcher(FORM).forward(req, resp);
    }

    // ------------------------------------------------------------------ lưu

    private void save(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int id = ParamUtil.getInt(req, "id", 0);
        Drink existing = null;
        if (id != 0) {
            existing = drinkDAO.findById(id);
            if (existing == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đồ uống");
                return;
            }
        }

        Drink d = new Drink();
        d.setId(id);
        d.setName(ParamUtil.getString(req, "name", ""));
        d.setCategoryId(ParamUtil.getInt(req, "categoryId", 0));
        d.setAvailable(ParamUtil.getBoolean(req, "available", false));
        d.setImage(existing == null ? null : existing.getImage());

        String priceText = ParamUtil.getString(req, "unitPrice", "");
        String discountText = ParamUtil.getString(req, "discount", "0");

        String error = null;
        if (d.getName().isEmpty()) {
            error = "Tên đồ uống không được để trống";
        } else if (d.getName().length() > NAME_MAX) {
            error = "Tên đồ uống tối đa " + NAME_MAX + " ký tự";
        } else if (!isNonNegativeInteger(priceText)) {
            error = "Đơn giá phải là số không âm";
        } else if (!isPercent(discountText)) {
            error = "Giảm giá phải từ 0 đến 100%";
        } else if (categoryDAO.findById(d.getCategoryId()) == null) {
            error = "Vui lòng chọn loại đồ uống";
        }
        // Giữ lại giá trị người dùng nhập để hiển thị lại form
        d.setUnitPrice(isNonNegativeInteger(priceText) ? Double.parseDouble(priceText) : 0);
        d.setDiscount(isPercent(discountText) ? Double.parseDouble(discountText) / 100 : 0);
        if (error != null) {
            showForm(req, resp, d, error);
            return;
        }

        String newImage;
        try {
            newImage = FileUtil.save(getServletContext(), imagePart(req));
        } catch (IOException | IllegalStateException e) {
            showForm(req, resp, d, "Ảnh không hợp lệ: chỉ nhận jpg, png, gif, webp, tối đa 5 MB");
            return;
        }
        String oldImage = d.getImage();
        if (newImage != null) {
            d.setImage(newImage);
        }

        if (id == 0) {
            drinkDAO.create(d);
        } else {
            drinkDAO.update(d);
            if (newImage != null) {
                FileUtil.delete(getServletContext(), oldImage);
            }
        }
        req.getSession().setAttribute("flashOk", "Đã lưu đồ uống");
        resp.sendRedirect(req.getContextPath() + "/manager/drinks");
    }

    private Part imagePart(HttpServletRequest req) throws IOException {
        String type = req.getContentType();
        if (type == null || !type.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        try {
            return req.getPart("image");
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    // ------------------------------------------------------------------ xóa

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = ParamUtil.getInt(req, "id", 0);
        Drink d = drinkDAO.findById(id);
        if (d == null) {
            req.getSession().setAttribute("flashError", "Không tìm thấy đồ uống");
        } else if (drinkDAO.countBillDetails(id) > 0) {
            req.getSession().setAttribute("flashError",
                    "Không thể xóa: đồ uống đã được bán (hãy chuyển sang ngừng bán)");
        } else {
            drinkDAO.deleteById(id);
            FileUtil.delete(getServletContext(), d.getImage());
            req.getSession().setAttribute("flashOk", "Đã xóa đồ uống");
        }
        resp.sendRedirect(req.getContextPath() + "/manager/drinks");
    }

    // ------------------------------------------------------------------ kiểm tra dữ liệu

    /** Số nguyên không âm, chỉ gồm chữ số, tối đa 15 chữ số (vừa DECIMAL(18,0) và không tràn double). */
    static boolean isNonNegativeInteger(String s) {
        return s != null && s.matches("\\d{1,15}");
    }

    /** Phần trăm từ 0 đến 100, cho phép số thập phân. */
    static boolean isPercent(String s) {
        if (s == null || !s.matches("\\d{1,3}(\\.\\d{1,4})?")) {
            return false;
        }
        double v = Double.parseDouble(s);
        return v >= 0 && v <= 100;
    }
}
