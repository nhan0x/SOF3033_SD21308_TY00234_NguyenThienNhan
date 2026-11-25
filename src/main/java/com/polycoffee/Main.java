package com.polycoffee;

import com.polycoffee.DAO.CategoryDAO;
import com.polycoffee.Entity.Category;
import java.util.List;

/**
 * Chương trình console kiểm tra nhanh kết nối CSDL: in danh sách loại đồ uống.
 * Ứng dụng web thật chạy trên Tomcat (xem docs/CAI_DAT_VA_TRIEN_KHAI.md).
 */
public class Main {
    public static void main(String[] args) {
        CategoryDAO dao = new CategoryDAO();

        List<Category> list = dao.findAll();
        for (Category category : list) {
            System.out.println(category.getId() + " - " + category.getName());
        }
    }
}
