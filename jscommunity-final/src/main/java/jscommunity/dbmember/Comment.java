// 📁 jscommunity/DBmember/Comment.java
package jscommunity.dbmember;

public class Comment {
    private int id;
    private int postId;
    private String author;
    private String content;
    private String date;
    private String email;

    public Comment(int id, int postId, String author, String content, String date, String email) {
        this.id = id;
        this.postId = postId;
        this.author = author;
        this.content = content;
        this.date = date;
        this.email = email;
    }

    public int getId() { return id; }
    public int getPostId() { return postId; }
    public String getAuthor() { return author; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public String getEmail() { return email; }
}
