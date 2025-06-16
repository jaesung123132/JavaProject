package jscommunity.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import jscommunity.dbmember.Login;

public class LoginDAO {

    // 1. 새로운 사용자 회원가입 시 로그인 정보 초기화 (Login 테이블에 레코드 삽입)
    public static boolean insertLoginInfo(Login login) {
        String sql = "INSERT INTO login (user_email, login_fail_count, account_locked) VALUES (?, ?, ?)";
        int rowsAffected = DB.exceuteUpdate(sql, login.getUserEmail(), login.getLoginFailCount(), login.isAccountLocked());
        return rowsAffected > 0;
    }

    // 2. 로그인 시도 시 사용자 로그인 정보 조회
    public static Login getLoginInfoByEmail(String email) {
        String sql = "SELECT user_email, login_fail_count, account_locked FROM login WHERE user_email = ?";
        Login login = null;
        try (ResultSet rs = DB.executeQuery(sql, email)) {
            if (rs != null && rs.next()) {
                login = new Login(
                        rs.getString("user_email"),
                        rs.getInt("login_fail_count"),
                        rs.getBoolean("account_locked")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return login;
    }

    // 3. 로그인 실패 횟수 업데이트 및 잠금 상태 변경
    public static boolean updateLoginInfo(Login login) {
        String sql = "UPDATE login SET login_fail_count = ?, account_locked = ? WHERE user_email = ?";
        int rowsAffected = DB.exceuteUpdate(sql, login.getLoginFailCount(), login.isAccountLocked(), login.getUserEmail());
        return rowsAffected > 0;
    }

    // 4. 사용자 삭제 시 로그인 정보도 삭제
    public static boolean deleteLoginInfo(String email) {
        String sql = "DELETE FROM login WHERE user_email = ?";
        int rowsAffected = DB.exceuteUpdate(sql, email);
        return rowsAffected > 0;
    }

    // 5. 계정을 잠금 해제하고 실패 횟수를 초기화하는 메서드 추가
    public static boolean unlockAccount(String email) {
        String sql = "UPDATE login SET login_fail_count = 0, account_locked = FALSE WHERE user_email = ?";
        int rowsAffected = DB.exceuteUpdate(sql, email);
        return rowsAffected > 0;
    }
}