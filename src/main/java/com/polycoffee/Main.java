package com.polycoffee;
import com.polycoffee.DAO.CategoryDAO;
import com.polycoffee.Entity.Category;
import java.util.List;


public class Main {
    public static void main(String[] args) {
        CategoryDAO dao = new CategoryDAO();

        List<Category> list = dao.findAll();
        for (Category category : list) {
            System.out.println(category.getId());
            System.out.println(category.getName());
            System.out.println(category.isActive());
        }
    }

    
}