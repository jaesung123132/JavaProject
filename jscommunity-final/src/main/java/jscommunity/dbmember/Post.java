package jscommunity.dbmember;

public class Post {
    private int id;
    private String title;
    private String author;
    private String content;
    private String email;
    private String date;
    private int likes;
    private int views;
    private int comments;
    private int boardId;

    // 기본 생성자
    public Post() {}

    // 전체 필드를 포함한 생성자
    public Post(int id, String title, String author, String content, String email,
                String date, int likes, int views, int comments, int boardId) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.content = content;
        this.email = email;
        this.date = date;
        this.likes = likes;
        this.views = views;
        this.comments = comments;
        this.boardId = boardId;
    }

    // Getter & Setter
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public int getLikes() {
        return likes;
    }
    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getViews() {
        return views;
    }
    public void setViews(int views) {
        this.views = views;
    }

    public int getComments() {
        return comments;
    }
    public void setComments(int comments) {
        this.comments = comments;
    }

    public int getBoardId() {
        return boardId;
    }
    public void setBoardId(int boardId) {
        this.boardId = boardId;
    }


}
