package jscommunity.db;

import jscommunity.dbmember.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;     // LocalDate 임포트 추가
import java.time.LocalDateTime;

public class UserDAO {

    public static boolean insertUser(User user) {
        // user 테이블의 컬럼 순서 및 타입에 맞게 SQL 문 작성
        // suspension_end_date 컬럼은 기본적으로 NULL이거나, User 객체에서 제공되는 값이 들어갈 수 있습니다.
        String sql = "INSERT INTO user (email, password, name, gender, birthday, phone, role, last_pw_change, profile_image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int res = DB.exceuteUpdate(sql,
                user.getEmail(),
                user.getPassword(),
                user.getName(),
                user.getGender(),
                user.getBirthday(),
                user.getPhone(),
                user.getRole(),
                user.getLastPasswordChange(),
                user.getProfileImagePath()

        );
        return res > 0;
    }

    public static User login(String email, String password) {
        // SQL 쿼리에 suspension_end_date 컬럼 추가
        String sql = "SELECT email, password, name, gender, birthday, phone, role, last_pw_change, profile_image_path, suspension_end_date FROM user WHERE email = ? AND password = ?";
        User user = null;
        try (ResultSet rs = DB.executeQuery(sql, email, password)) {
            if (rs != null && rs.next()) {
                String name = rs.getString("name");
                String gender = rs.getString("gender");
                String birthday = rs.getString("birthday");
                String phone = rs.getString("phone");
                String role = rs.getString("role");
                Timestamp lastPasswordChangeTimestamp = rs.getTimestamp("last_pw_change");
                LocalDateTime lastPasswordChange = null;
                if (lastPasswordChangeTimestamp != null) {
                    lastPasswordChange = lastPasswordChangeTimestamp.toLocalDateTime();
                }
                String profileImagePath = rs.getString("profile_image_path");

                // ✅ suspension_end_date 로드 (ResultSet에서 SQL Date를 LocalDate로 변환)
                LocalDate suspensionEndDate = rs.getDate("suspension_end_date") != null ? rs.getDate("suspension_end_date").toLocalDate() : null;

                // 디버깅: login 메서드에서 읽어온 profileImagePath와 suspensionEndDate 값 출력
                System.out.println("UserDAO 디버깅 (login): 이메일: " + email +
                        ", ResultSet에서 읽은 profile_image_path: " + profileImagePath +
                        ", suspension_end_date: " + suspensionEndDate);

                // User 객체 생성 시 suspensionEndDate 포함하여 생성
                // User 클래스 생성자 시그니처에 맞게 필드 순서와 개수를 확인하세요.
                // 예시: User(email, password, name, gender, birthday, phone, role, lastPasswordChange, profileImagePath, suspensionEndDate)
                user = new User(email, password, name, gender, birthday, phone, role, lastPasswordChange, profileImagePath, suspensionEndDate);
            }
        } catch (SQLException ex) {
            System.err.println("로그인 오류: " + ex.getMessage());
            ex.printStackTrace();
        }
        return user;
    }

    public static User findByEmail(String email) {
        // SQL 쿼리에 suspension_end_date 컬럼 추가
        String sql = "SELECT email, password, name, gender, birthday, phone, role, last_pw_change, profile_image_path, suspension_end_date FROM user WHERE email = ?";
        User user = null;
        try (ResultSet rs = DB.executeQuery(sql, email)) {
            if (rs != null && rs.next()) {
                String password = rs.getString("password");
                String name = rs.getString("name");
                String gender = rs.getString("gender");
                String birthday = rs.getString("birthday");
                String phone = rs.getString("phone");
                String role = rs.getString("role");
                Timestamp lastPasswordChangeTimestamp = rs.getTimestamp("last_pw_change");
                LocalDateTime lastPasswordChange = null;
                if (lastPasswordChangeTimestamp != null) {
                    lastPasswordChange = lastPasswordChangeTimestamp.toLocalDateTime();
                }
                String profileImagePath = rs.getString("profile_image_path");

                // ✅ suspension_end_date 로드 (ResultSet에서 SQL Date를 LocalDate로 변환)
                LocalDate suspensionEndDate = rs.getDate("suspension_end_date") != null ? rs.getDate("suspension_end_date").toLocalDate() : null;

                // 디버깅: findByEmail 메서드에서 읽어온 profileImagePath와 suspensionEndDate 값 출력
                System.out.println("UserDAO 디버깅 (findByEmail): 이메일: " + email +
                        ", ResultSet에서 읽은 profile_image_path: " + profileImagePath +
                        ", suspension_end_date: " + suspensionEndDate);

                // User 객체 생성 시 suspensionEndDate 포함하여 생성
                // User 클래스 생성자 시그니처에 맞게 필드 순서와 개수를 확인하세요.
                // 예시: User(email, password, name, gender, birthday, phone, role, lastPasswordChange, profileImagePath, suspensionEndDate)
                user = new User(email, password, name, gender, birthday, phone, role, lastPasswordChange, profileImagePath, suspensionEndDate);
            }
        } catch (SQLException e) {
            System.err.println("이메일로 사용자 찾기 오류: " + e.getMessage());
            e.printStackTrace();
        }
        return user;
    }

    public static boolean updatePassword(String email, String newPassword) {
        String sql = "UPDATE user SET password = ?, last_pw_change = ? WHERE email = ?";
        int rowsAffected = DB.exceuteUpdate(sql, newPassword, LocalDateTime.now(), email);
        return rowsAffected > 0;
    }

    public static boolean updateProfileImagePath(String email, String imagePath) {
        String sql = "UPDATE user SET profile_image_path = ? WHERE email = ?";
        int res = DB.exceuteUpdate(sql, imagePath, email);
        return res > 0;
    }

    /**
     * 사용자 정지 종료일을 업데이트합니다.
     * @param email 정지할 사용자의 이메일
     * @param suspensionEndDate 정지 종료일 (LocalDate 타입). 정지를 해제하려면 null을 전달합니다.
     * @return 업데이트 성공 여부
     */
    public static boolean updateUserSuspension(String email, LocalDate suspensionEndDate) {
        String sql = "UPDATE user SET suspension_end_date = ? WHERE email = ?";
        try {
            // LocalDate를 java.sql.Date로 변환. null이면 DB에 NULL로 저장됩니다.
            java.sql.Date sqlSuspensionDate = (suspensionEndDate != null) ? java.sql.Date.valueOf(suspensionEndDate) : null;
            int affectedRows = DB.exceuteUpdate(sql, sqlSuspensionDate, email);
            return affectedRows > 0;
        } catch (Exception e) { // DB.exceuteUpdate에서 발생할 수 있는 모든 예외를 잡음
            System.err.println("UserDAO.updateUserSuspension 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}