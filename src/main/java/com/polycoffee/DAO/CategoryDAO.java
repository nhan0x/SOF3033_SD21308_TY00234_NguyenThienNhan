package com.polycoffee.DAO;
import com.polycoffee.Entity.Category;
import com.polycoffee.Util.JdbcUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class CategoryDAO{
    public int insert(Category category){
        int rows  = 0;
        String sql = " INSERT INTO categories(name, active) VALUES (?, ?)";

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setBoolean(2, category.isActive());
            rows = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    public List<Category> findAll(){
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories";

        try(Connection conn = JdbcUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                boolean active = rs.getBoolean("active");

                Category c = new Category();
                c.setId(id);
                c.setName(name);
                c.setActive(active);

                list.add(c);
            }
        }
            catch(SQLException e){
                e.printStackTrace();
            }
            return list;
        } 
}

        

