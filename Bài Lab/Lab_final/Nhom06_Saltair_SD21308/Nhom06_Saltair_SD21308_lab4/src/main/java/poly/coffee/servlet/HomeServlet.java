package poly.coffee.servlet;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import poly.coffee.dao.CategoryDAO;
import poly.coffee.dao.DrinkDAO;
import poly.coffee.entity.Drink;

/** Trang chu cong khai: khach hang xem do uong (hinh anh + gia). */
@WebServlet({"/home", ""})
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Drink> drinks = new DrinkDAO().findAll().stream()
                .filter(Drink::isAvailable).collect(Collectors.toList());
        req.setAttribute("drinks", drinks);
        req.setAttribute("categories", new CategoryDAO().findAll());
        req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req, resp);
    }
}
