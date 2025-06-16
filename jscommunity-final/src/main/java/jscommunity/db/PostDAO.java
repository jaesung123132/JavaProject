package jscommunity.db;

import jscommunity.dbmember.Post;

public class PostDAO {

    public static boolean insertPost(Post post) {
        String sql = "INSERT INTO posts (title, author, content, email, date, likes, views, comments_count, board_id) " +
                "VALUES (?, ?, ?, ?, NOW(), 0, 0, 0, ?)";

        try {
            int result = DB.exceuteUpdate(sql,
                    post.getTitle(),
                    post.getAuthor(),
                    post.getContent(),
                    post.getEmail(),
                    post.getBoardId()
            );
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
