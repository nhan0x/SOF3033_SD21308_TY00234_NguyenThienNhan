package com.polycoffee.Util;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
public class JdbcUtil {
    static String driver = "com.mysql.cj.jdbc.Driver";
    static String dburl = "jdbc:mysql://localhost:3306/polycoffee";
    static String username  = "root";
    static String password = "Bin041209@@";

    static{
        try{
            Class.forName(driver);
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }
    public  static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(dburl, username, password);
    }

}
