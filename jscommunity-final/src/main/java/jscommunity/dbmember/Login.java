 package jscommunity.dbmember;

public class Login {
    private String userEmail;
    private int loginFailCount;
    private boolean accountLocked;

    // 생성자
    public Login(String userEmail, int loginFailCount, boolean accountLocked) {
        this.userEmail = userEmail;
        this.loginFailCount = loginFailCount;
        this.accountLocked = accountLocked;
    }

    // Getter 및 Setter
    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public int getLoginFailCount() {
        return loginFailCount;
    }

    public void setLoginFailCount(int loginFailCount) {
        this.loginFailCount = loginFailCount;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }
}