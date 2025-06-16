package jscommunity.db;

import jscommunity.dbmember.Comment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    // 특정 게시글의 댓글 전체 조회
    public static List<Comment> getCommentsByPostId(int postId) {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT id, post_id, author, content, email, date FROM comments WHERE post_id = ? ORDER BY date ASC";

        try (ResultSet rs = DB.executeQuery(sql, postId)) {
            while (rs != null && rs.next()) {
                Comment comment = new Comment(
                        rs.getInt("id"),
                        rs.getInt("post_id"),
                        rs.getString("author"),
                        rs.getString("content"),
                        rs.getString("date"),
                        rs.getString("email")
                );
                comments.add(comment);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // 🔁 실운영 시 로깅 시스템으로 교체 권장
        }

        return comments;
    }
}
