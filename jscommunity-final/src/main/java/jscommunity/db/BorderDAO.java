package jscommunity.db;

import jscommunity.dbmember.Border;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BorderDAO {

    // 게시판 전체 목록 조회
    public static List<Border> findAll() {
        List<Border> list = new ArrayList<>();
        String sql = "SELECT * FROM board";

        try (ResultSet rs = DB.executeQuery(sql)) {
            while (rs != null && rs.next()) {
                Border border = new Border();
                border.setId(rs.getInt("id"));
                border.setName(rs.getString("name"));
                border.setAnonymous(rs.getBoolean("is_anonymous"));
                border.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(border);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static boolean addBoard(String boardName, boolean isAnonymous) {
        // SQL 쿼리에서 id는 AUTO_INCREMENT이므로 제외하고, created_at은 CURDATE() 함수 사용
        String sql = "INSERT INTO board (name, is_anonymous, created_at) VALUES (?, ?, CURDATE())";
        // isAnonymous boolean 값을 DB에 1 (true) 또는 0 (false)으로 저장하기 위해 삼항 연산자 사용
        int res = DB.exceuteUpdate(sql, boardName, isAnonymous ? 1 : 0);
        return res > 0;
    }

    public static boolean deleteBoard(int boardId) {
        boolean success = false;
        try {
            // 1. 해당 게시판의 모든 게시글에 달린 댓글 먼저 삭제
            // comments 테이블에 post_id가 있고, posts 테이블에 board_id가 있는 구조를 가정합니다.
            String deleteCommentsSql = "DELETE FROM comments WHERE post_id IN (SELECT id FROM posts WHERE board_id = ?)";
            DB.exceuteUpdate(deleteCommentsSql, boardId);

            // 2. 해당 게시판의 모든 게시글 삭제
            String deletePostsSql = "DELETE FROM posts WHERE board_id = ?";
            DB.exceuteUpdate(deletePostsSql, boardId);

            // 3. 마지막으로 게시판 자체 삭제
            String deleteBoardSql = "DELETE FROM board WHERE id = ?";
            int affectedRows = DB.exceuteUpdate(deleteBoardSql, boardId);

            success = (affectedRows > 0);

        } catch (Exception e) {
            System.err.println("게시판 삭제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            success = false;
        }
        return success;
    }
}
