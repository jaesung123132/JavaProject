package jscommunity.dbmember;

import java.time.LocalDateTime;

public class Border {
    private int id;
    private String name;
    private boolean isAnonymous;
    private LocalDateTime createdAt;

    // 생성자 (선택)
    public Border() {}

    public Border(int id, String name, boolean isAnonymous, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.isAnonymous = isAnonymous;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // toString - 디버깅 시 유용
    @Override
    public String toString() {
        return "Border{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isAnonymous=" + isAnonymous +
                ", createdAt=" + createdAt +
                '}';
    }
}
