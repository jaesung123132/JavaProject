package jscommunity.dbmember;

import java.time.LocalDateTime;
import java.time.LocalDate; // ✅ LocalDate 임포트 추가

public class User {
    private String email;
    private String password;
    private String name;
    private String gender;
    private String birthday;
    private String phone;
    private String role;
    private LocalDateTime lastPasswordChange;
    private String profileImagePath;
    private LocalDate suspensionEndDate; // ✅ 새로 추가된 필드: 정지 종료일

    // 🔹 기본 생성자 (필요하다면 유지)
    public User() {}

    // 🔹 기존 생성자 (lastPasswordChange 없이) - profileImagePath, suspensionEndDate 추가
    public User(String email, String password, String name,
                String gender, String birthday, String phone, String role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.gender = gender;
        this.birthday = birthday;
        this.phone = phone;
        this.role = role;
        this.lastPasswordChange = LocalDateTime.now(); // 기본값 설정 (회원가입 시점)
        this.profileImagePath = null; // 초기 프로필 이미지 경로 없음
        this.suspensionEndDate = null; // ✅ 초기 정지 종료일 없음 (정지되지 않음)
    }

    // 🔹 추가된 생성자 (lastPasswordChange 포함) - profileImagePath, suspensionEndDate 추가
    public User(String email, String password, String name,
                String gender, String birthday, String phone, String role,
                LocalDateTime lastPasswordChange) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.gender = gender;
        this.birthday = birthday;
        this.phone = phone;
        this.role = role;
        this.lastPasswordChange = lastPasswordChange;
        this.profileImagePath = null; // 초기 프로필 이미지 경로 없음
        this.suspensionEndDate = null; // ✅ 초기 정지 종료일 없음 (정지되지 않음)
    }

    // ⭐️ 모든 필드를 포함하는 생성자 (DB에서 모든 데이터를 로드할 때 주로 사용)
    // suspensionEndDate 필드 추가
    public User(String email, String password, String name,
                String gender, String birthday, String phone, String role,
                LocalDateTime lastPasswordChange, String profileImagePath,
                LocalDate suspensionEndDate) { // ✅ suspensionEndDate 매개변수 추가
        this.email = email;
        this.password = password;
        this.name = name;
        this.gender = gender;
        this.birthday = birthday;
        this.phone = phone;
        this.role = role;
        this.lastPasswordChange = lastPasswordChange;
        this.profileImagePath = profileImagePath;
        this.suspensionEndDate = suspensionEndDate; // ✅ 필드 초기화
    }


    // Getter
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getBirthday() { return birthday; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public LocalDateTime getLastPasswordChange() { return lastPasswordChange; }
    public String getProfileImagePath() { return profileImagePath; }

    // ✅ 새로 추가된 Getter: suspensionEndDate
    public LocalDate getSuspensionEndDate() { return suspensionEndDate; }

    // Setter
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setLastPasswordChange(LocalDateTime lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    // ✅ 새로 추가된 Setter: suspensionEndDate
    public void setSuspensionEndDate(LocalDate suspensionEndDate) {
        this.suspensionEndDate = suspensionEndDate;
    }

    // 편의를 위한 isAdmin() 메서드 (role 기반)
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }
}