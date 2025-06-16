package jscommunity.db;

import java.sql.*;

public class DB {
    private static Connection conn;

    public static void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/jscommunity?serverTimezone=UTC&useUniCode=yes&characterEncoding=UTF-8"
                    ,"root", "rootroot");
            System.out.println("DB 연결 성공 !");

        } catch (ClassNotFoundException e) {
            System.out.println("해당 드라이버를 찾지 못했습니다 : ");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("SQL 오류 발생 !!");
            e.printStackTrace();
        }
    }
    // SELECT
    public static ResultSet executeQuery (String sql, Object... params) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            setParams(pstmt, params);
            return pstmt.executeQuery();

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // UPDATE, INSERT DELETE
    public static int exceuteUpdate (String sql, Object... params) {
        try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParams(pstmt, params);
            return pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
    
 // DB.java 내부에 추가
    public static boolean isEmailDuplicated(String email) { 
        String sql = "SELECT email FROM user WHERE email = ?";
        try (ResultSet rs = executeQuery(sql, email)) {
            return rs != null && rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }




    private static void setParams(PreparedStatement pstmt, Object... params) throws SQLException {
        for(int i=0; i < params.length; i++) {
            pstmt.setObject(i+1, params[i]);
        }
    }
}
