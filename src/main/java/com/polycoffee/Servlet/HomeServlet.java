package com.polycoffee.Servlet;

import com.polycoffee.DAO.CategoryDAO;
import com.polycoffee.DAO.DrinkDAO;
import com.polycoffee.Entity.Category;
import com.polycoffee.Entity.Drink;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Trang chủ công khai: thực đơn gồm các đồ uống đang bán, nhóm theo loại (BR-08). */
@WebServlet({"/home", ""})
public class HomeServlet extends HttpServlet {

    private final DrinkDAO drinkDAO;
    private final CategoryDAO categoryDAO;

    public HomeServlet() {
        this(new DrinkDAO(), new CategoryDAO());
    }

    HomeServlet(DrinkDAO drinkDAO, CategoryDAO categoryDAO) {
        this.drinkDAO = drinkDAO;
        this.categoryDAO = categoryDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Drink> drinks = drinkDAO.findAvailable();
        Map<Category, List<Drink>> menu = new LinkedHashMap<>();
        for (Category c : categoryDAO.findAll()) {
            List<Drink> inCategory = drinks.stream().filter(d -> d.getCategoryId() == c.getId()).toList();
            if (!inCategory.isEmpty()) {
                menu.put(c, inCategory);
            }
        }
        req.setAttribute("menu", menu);
        req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req, resp);
    }
}
