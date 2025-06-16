package jscommunity.Panel;

import jscommunity.db.CommentDAO;
import jscommunity.db.DB;
import jscommunity.dbmember.Comment;
import jscommunity.dbmember.Post; // Post 클래스 임포트
import jscommunity.dbmember.User;
import javax.swing.text.html.HTMLEditorKit; //  HTML 렌더링을 위해 필요
import javax.swing.text.StyledDocument; //  StyledDocument 임포트

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors; // Collectors 임포트 추가

import jscommunity.Panel.UserProfilePanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PostPanel extends JPanel {
    private boolean currentBoardIsAnonymous;
    private static final long serialVersionUID = 1L;
    private JPanel commentListPanel;  // 댓글이 붙을 실제 패널
    private int borderId;  // 현재 게시판 ID
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JPanel listViewPanel;
    private JPanel detailViewPanel;
    private JTable lineup;
    private JTextField textField;
    private DefaultTableModel model;
    private String sortOrder = "DESC";
    private String sortTarget = "date";
    private String keyword = "";
    private String searchColumn = "";
    private int currentPage = 1;
    private int currentPostId;
    private JLabel titleLabel;
    private JLabel metaLabel;
    private JEditorPane contentPane;
    private JTextArea commentInput;
    private JScrollPane commentListScroll;
    private User currentUser;
    private JPanel writePanelWrapper = null;
    private JPanel userProfilePanel = null;
    private String currentPostEmail = "";


    private static final int PAGE_SIZE = 20;
    private CardLayout centerCardLayout;
    private JPanel centerCardPanel;
    private Set<Integer> likedPosts = new HashSet<>();
    private Map<String, PostPanel> postPanels = new HashMap<>();
    private JPanel topInfoPanel;

    public PostPanel(User currentUser, int borderId) {
        this.currentUser = currentUser;
        this.borderId = borderId;

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        listViewPanel = createListViewPanel();
        detailViewPanel = createDetailViewPanel();

        cardPanel.add(listViewPanel, "list");
        cardPanel.add(detailViewPanel, "detail");

        setLayout(new BorderLayout());
        add(cardPanel, BorderLayout.CENTER);

        reloadPostList();
    }

    private JPanel createListViewPanel() {
        // 실제 내용이 담기는 패널
        JPanel contentPanel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[][grow][][grow]"));

        Font bigFont = new Font("Malgun Gothic", Font.BOLD, 16);
        Font smallFont = new Font("Malgun Gothic", Font.PLAIN, 12);

        // 글쓰기 + 새로고침 버튼
        JButton writeBtn = new JButton("글 쓰기");
        writeBtn.setFont(bigFont);
        writeBtn.addActionListener(e -> {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "로그인이 필요합니다.");
                return;
            }

            if (writePanelWrapper == null) {
                writePanelWrapper = new Write(borderId, currentUser, () -> {
                    cardPanel.remove(writePanelWrapper);
                    writePanelWrapper = null;
                    reloadPostList();
                    cardLayout.show(cardPanel, "list");
                });
                cardPanel.add(writePanelWrapper, "write");
            }
            cardLayout.show(cardPanel, "write");
        });


        JButton refreshBtn = new JButton(loadIcon("/새로고침.png", 24, 24));
        refreshBtn.addActionListener(e -> {
            sortOrder = "DESC";
            sortTarget = "date";
            keyword = "";
            searchColumn = "";
            currentPage = 1;
            reloadPostList();
        });

        JPanel writePanel = new JPanel(new MigLayout("insets 0", "[]10[]", "center"));
        writePanel.add(writeBtn);
        writePanel.add(refreshBtn);
        contentPanel.add(writePanel, "align right, wrap");

        // 테이블 생성
        model = new DefaultTableModel(new Object[][] {}, new String[] {
                "글 번호", "제목", "작성자", "시간", "추천", "조회수"
        }) {
            public boolean isCellEditable(int row, int column) { return false; }
        };

        lineup = new JTable(model);
        lineup.setRowHeight(45);
        lineup.getTableHeader().setFont(bigFont);
        // 자동 크기 조절 모드를 OFF로 설정
        lineup.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // 컬럼 너비 비율 설정 (글 번호:제목:작성자:시간:추천:조회수 = 1:5:1:1:0.5:1)
        // 전체 테이블의 너비를 700px로 가정하고 비율에 맞춰 분배
        // 실제 너비는 부모 컨테이너에 따라 유동적으로 조정될 수 있습니다.
        TableColumnModel columnModel = lineup.getColumnModel();
        int totalTableWidth = 700; // 이 값을 부모 컨테이너의 예상 너비에 맞춰 조정하세요.

        // 각 컬럼의 너비 비율 (총 합 9.5)
        columnModel.getColumn(0).setPreferredWidth((int)(totalTableWidth * (0.2/9.5)));
        columnModel.getColumn(1).setPreferredWidth((int)(totalTableWidth * (8.5/9.5)));
        columnModel.getColumn(2).setPreferredWidth((int)(totalTableWidth * (0.2/9.5)));
        columnModel.getColumn(3).setPreferredWidth((int)(totalTableWidth * (0.2/9.5)));
        columnModel.getColumn(4).setPreferredWidth((int)(totalTableWidth * (0.2/9.5)));
        columnModel.getColumn(5).setPreferredWidth((int)(totalTableWidth * (0.2/9.5)));


        // 인기글 표시를 위한 커스텀 렌더러 (DefaultTableCellRenderer 상속)
        TableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            Font titleFont = new Font("Malgun Gothic", Font.BOLD, 16);
            Font hotTitleFont = new Font("Malgun Gothic", Font.BOLD, 17); // 인기글 제목 폰트 (조금 더 크게)
            Color hotTitleColor = new Color(200, 0, 0); // 인기글 제목 색상 (빨강)

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);

                if (column == 1) {
                    String titleText = value != null ? value.toString() : "";
                    if (titleText.startsWith("★")) { // 인기글 표시 확인
                        label.setFont(hotTitleFont);
                        label.setForeground(hotTitleColor);
                    } else {
                        label.setFont(titleFont);
                        label.setForeground(Color.BLACK);
                    }
                } else {
                    label.setFont(smallFont);
                    label.setForeground(Color.DARK_GRAY);
                }
                return label;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) {
            lineup.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }

        // 정렬 기능
        lineup.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int column = lineup.columnAtPoint(e.getPoint());
                String columnName = lineup.getColumnName(column);
                switch (columnName) {
                    case "시간": sortTarget = "date"; break;
                    case "추천": sortTarget = "likes"; break;
                    case "조회수": sortTarget = "views"; break;
                    default: return; // 다른 컬럼 클릭 시 정렬 안함
                }
                sortOrder = sortOrder.equals("DESC") ? "ASC" : "DESC";
                reloadPostList();
            }
        });

        // 더블클릭 시 상세보기 열기
        lineup.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = lineup.getSelectedRow();
                    if (row != -1) {
                        // 테이블 모델에서 직접 글 번호 가져오기
                        int postId = (int) model.getValueAt(row, 0);
                        showPostDetail(postId);
                    }
                }
            }
        });

        contentPanel.add(lineup.getTableHeader(), "growx, wrap");
        contentPanel.add(lineup, "grow, push, wrap");

        // ⬅ ➡
        JButton prevBtn = new JButton("이전");
        JButton nextBtn = new JButton("다음");
        prevBtn.setFont(smallFont);
        nextBtn.setFont(smallFont);
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                reloadPostList();
            } else {
                JOptionPane.showMessageDialog(this, "첫 페이지입니다.");
            }
        });
        nextBtn.addActionListener(e -> {
            int totalPosts = getTotalPostCount();
            // reloadPostList에서 getTotalPostCountExcludingPopular를 사용하기 때문입니다.
            int maxPage = (int) Math.ceil((double) totalPosts / PAGE_SIZE);

            if (currentPage < maxPage) {
                currentPage++;
                reloadPostList();
            } else {
                JOptionPane.showMessageDialog(this, "마지막 페이지입니다.");
            }
        });

        JPanel pagingPanel = new JPanel(new MigLayout("", "[]20[]", "center"));
        pagingPanel.add(prevBtn);
        pagingPanel.add(nextBtn);
        contentPanel.add(pagingPanel, "align center, wrap");

        // 검색창 (맨 아래)
        JPanel searchPanel = new JPanel(new MigLayout("", "[]10[]10[]", "center"));
        JComboBox<String> searchCombo = new JComboBox<>(new String[] { "제목", "작성자", "내용" });
        searchCombo.setFont(bigFont);
        textField = new JTextField(15);
        textField.setFont(bigFont);
        JButton searchBtn = new JButton("검색");
        searchBtn.setFont(bigFont);
        searchBtn.addActionListener(e -> {
            keyword = textField.getText().trim();
            String cat = (String) searchCombo.getSelectedItem();
            searchColumn = cat.equals("제목") ? "title" : cat.equals("작성자") ? "author" : "content";
            currentPage = 1;
            reloadPostList();
        });
        searchPanel.add(searchCombo);
        searchPanel.add(textField);
        searchPanel.add(searchBtn);
        contentPanel.add(searchPanel, "align center");

        // contentPanel을 스크롤 가능하게 감싼 JScrollPane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 최종 패널 (스크롤 포함한 JPanel 리턴)
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }


    private JPanel createDetailViewPanel() {
        System.out.println("createDetailViewPanel 호출됨");
        JPanel detailPanel = new JPanel(new BorderLayout(15, 15));

        // ===== 상단 정보 패널 =====
        topInfoPanel = new JPanel(new BorderLayout());
        topInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        titleLabel = new JLabel();
        titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 18));

        metaLabel = new JLabel();
        metaLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        metaLabel.setForeground(Color.GRAY);

        JButton editButton = new JButton("수정");
        editButton.addActionListener(new ActionListener() {
            private boolean isEditing = false;
            private JTextField editableTitleField;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentUser == null) {
                    JOptionPane.showMessageDialog(null, "로그인이 필요합니다. 작성자만 수정할 수 있습니다.");
                    return;
                }

                if (!currentUser.getEmail().equalsIgnoreCase(currentPostEmail)) {
                    JOptionPane.showMessageDialog(null, "수정 권한이 없습니다. 작성자만 수정할 수 있습니다.");
                    return;
                }


                if (!isEditing) {
                    isEditing = true;
                    editButton.setText("저장");

                    editableTitleField = new JTextField(titleLabel.getText());
                    editableTitleField.setFont(titleLabel.getFont());

                    JPanel titlePanel = (JPanel) titleLabel.getParent();
                    titlePanel.remove(titleLabel);
                    titlePanel.add(editableTitleField, BorderLayout.WEST);
                    titlePanel.revalidate();
                    titlePanel.repaint();

                    // JEditorPane 수정 가능하게 설정
                    contentPane.setEditable(true);
                    contentPane.setCursor(new Cursor(Cursor.TEXT_CURSOR));
                    contentPane.setFocusable(true);
                    contentPane.requestFocus();
                } else {
                    String newTitle = editableTitleField.getText().trim();
                    // EditorPane에서 HTML 내용 가져오기
                    String newContent = contentPane.getText().trim(); // getText()는 HTML 내용을 반환함

                    if (newTitle.isEmpty() || newContent.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "제목과 내용은 비워둘 수 없습니다.");
                        return;
                    }

                    // DB.exceuteUpdate를 사용하여 쿼리 실행
                    String sql = "UPDATE posts SET title=?, content=? WHERE id=?";
                    DB.exceuteUpdate(sql, newTitle, newContent, currentPostId);

                    reloadPostList();
                    showPostDetail(currentPostId);

                    isEditing = false;
                    editButton.setText("수정");
                    contentPane.setEditable(false);
                    contentPane.setFocusable(false);
                    contentPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    JOptionPane.showMessageDialog(null, "수정 완료!");
                }
            }
        });
        editButton.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        editButton.setMargin(new Insets(5, 10, 5, 10));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(editButton, BorderLayout.EAST);

        topInfoPanel.add(titlePanel, BorderLayout.NORTH);

        topInfoPanel.add(metaLabel, BorderLayout.SOUTH);
        detailPanel.add(topInfoPanel, BorderLayout.NORTH);

        // ===== 본문 영역 =====
        contentPane = new JEditorPane(); //  JEditorPane으로 변경
        contentPane.setContentType("text/html"); // HTML 콘텐츠를 표시하도록 설정
        contentPane.setEditable(false);
        contentPane.setFont(new Font("Malgun Gothic", Font.PLAIN, 14)); // JEditorPane의 기본 폰트 설정
        contentPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        contentPane.setFocusable(false);

        JScrollPane contentScroll = new JScrollPane(contentPane); // JEditorPane으로 변경
        contentScroll.setPreferredSize(new Dimension(600, 300));
        contentScroll.setMinimumSize(new Dimension(600, 300));
        contentScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // ===== 추천 버튼 =====
        ImageIcon activeIcon = loadIcon("/추천_활성화.png", 24, 24);
        JButton likeButton = new JButton(loadIcon("/추천_기본.png", 24, 24));
        likeButton.setBorderPainted(false);
        likeButton.setContentAreaFilled(false);
        likeButton.setFocusPainted(false);

        likeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // 로그인 여부 먼저 확인
                if (currentUser == null) {
                    JOptionPane.showMessageDialog(null, "로그인 후 추천할 수 있습니다.");
                    return;
                }

                if (likedPosts.contains(currentPostId)) {
                    JOptionPane.showMessageDialog(null, "이미 추천하셨습니다.");
                    return;
                }

                JOptionPane.showMessageDialog(null, "추천했습니다!");
                // DB.exceuteUpdate를 사용하여 쿼리 실행
                DB.exceuteUpdate("UPDATE posts SET likes = likes + 1 WHERE id = ?", currentPostId);
                likedPosts.add(currentPostId);

                likeButton.setIcon(activeIcon);
                showPostDetail(currentPostId);
            }
        });


        // ===== 댓글 목록 패널 =====
        commentListPanel = new JPanel();
        commentListPanel.setLayout(new BoxLayout(commentListPanel, BoxLayout.Y_AXIS));
        commentListPanel.setBackground(Color.WHITE);

        commentListScroll = new JScrollPane(commentListPanel);
        commentListScroll.setMinimumSize(new Dimension(600, 50));
        commentListScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        commentListScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // ===== 댓글 입력창 =====
        commentInput = new JTextArea(3, 50);
        commentInput.setLineWrap(true);
        commentInput.setWrapStyleWord(true);
        commentInput.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        JScrollPane commentInputScroll = new JScrollPane(commentInput);
        commentInputScroll.setPreferredSize(new Dimension(600, 60));
        commentInputScroll.setMinimumSize(new Dimension(600, 60));
        commentInputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        commentInputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JButton commentSubmit = new JButton("댓글 달기");
        commentSubmit.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        commentSubmit.setPreferredSize(new Dimension(80, 60));

        commentSubmit.addActionListener(e -> {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "로그인 후 이용해주세요.");
                return;
            }

            String commentText = commentInput.getText().trim();
            if (!commentText.isEmpty()) {
                String authorToUse;
                String emailToUse;

                if (this.currentBoardIsAnonymous) { // 게시판이 익명인지 확인
                    authorToUse = "익명";
                    emailToUse = "비공개"; // 익명 게시판의 경우 이메일도 비공개
                } else {
                    authorToUse = currentUser.getName();
                    emailToUse = currentUser.getEmail();
                }

                String sql = "INSERT INTO comments (post_id, author, email, content, date) VALUES (?, ?, ?, ?, NOW())";
                DB.exceuteUpdate(sql, currentPostId, authorToUse, emailToUse, commentText);

                commentInput.setText("");
                loadComments(currentPostId);
            }
        });

        // 댓글 입력창 패널도 창 크기에 맞춰 너비 조정
        JPanel commentInputPanel = new JPanel(new MigLayout("insets 0, fill", "[grow][82!]", "[60!]"));
        commentInputPanel.add(commentInputScroll, "grow");
        commentInputPanel.add(commentSubmit, "align right, gapright 15");

        // ===== 본문 + 추천 + 댓글 패널 =====
        JPanel middlePanel = new JPanel(new MigLayout("insets 10 15 10 15, fill, wrap", "[grow]", "[][24!]10[60!]10[grow, push, fill]"));
        middlePanel.add(contentScroll, "grow");
        middlePanel.add(likeButton, "align center");
        middlePanel.add(commentInputPanel, "grow");
        middlePanel.add(commentListScroll, "grow, push");

        detailPanel.add(middlePanel, BorderLayout.CENTER);

        // ===== 하단 닫기 버튼 =====
        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> cardLayout.show(cardPanel, "list"));
        JButton deleteButton = new JButton("삭제");
        deleteButton.addActionListener(e -> {
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "로그인이 필요합니다.");
                return;
            }

            if (!currentUser.getEmail().equalsIgnoreCase(currentPostEmail)) {
                JOptionPane.showMessageDialog(this, "작성자만 삭제할 수 있습니다.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "정말 이 글을 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int result = DB.exceuteUpdate("DELETE FROM posts WHERE id = ?", currentPostId);
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "글이 삭제되었습니다.");
                    reloadPostList();
                    cardLayout.show(cardPanel, "list");
                } else {
                    JOptionPane.showMessageDialog(this, "삭제에 실패했습니다.");
                }
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        detailPanel.add(buttonPanel, BorderLayout.SOUTH);
        return detailPanel;
    }


    private JPanel createCommentItem(Comment comment) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel authorLabel = new JLabel(comment.getAuthor() + " (" + comment.getEmail() + ")");
        authorLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 12));

        JTextArea contentArea = new JTextArea(comment.getContent());
        contentArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setFocusable(false);
        contentArea.setOpaque(true);
        contentArea.setBackground(new Color(245, 245, 245));
        contentArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));

        contentArea.setRows(Math.max(3, comment.getContent().split("\n").length));

        JLabel dateLabel = new JLabel(comment.getDate());
        dateLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 10));
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel top = new JPanel(new BorderLayout());
        top.add(authorLabel, BorderLayout.WEST);
        top.add(dateLabel, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(contentArea, BorderLayout.CENTER);

        if (currentUser != null && currentUser.getEmail().equals(comment.getEmail())) {
            JButton editBtn = new JButton("수정");
            JButton deleteBtn = new JButton("삭제");

            final boolean[] isEditing = {false};
            final JTextArea editArea = new JTextArea(comment.getContent());
            editArea.setLineWrap(true);
            editArea.setWrapStyleWord(true);
            editArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            editArea.setBackground(new Color(245, 245, 245));
            editArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));

            editBtn.addActionListener(e -> {
                if (!isEditing[0]) {
                    panel.remove(contentArea);
                    panel.add(editArea, BorderLayout.CENTER);
                    panel.revalidate();
                    panel.repaint();
                    editArea.requestFocus();
                    editBtn.setText("저장");
                    isEditing[0] = true;
                } else {
                    String newContent = editArea.getText().trim();
                    if (!newContent.isEmpty()) {
                        // DB.exceuteUpdate를 사용하여 쿼리 실행
                        String updateSql = "UPDATE comments SET content=? WHERE id=?";
                        DB.exceuteUpdate(updateSql, newContent, comment.getId());
                        contentArea.setText(newContent);
                        panel.remove(editArea);
                        panel.add(contentArea, BorderLayout.CENTER);
                        panel.revalidate();
                        panel.repaint();
                        isEditing[0] = false;
                        editBtn.setText("수정");
                        PostPanel.this.loadComments(currentPostId);
                    } else {
                        JOptionPane.showMessageDialog(null, "내용을 비워둘 수 없습니다.");
                    }
                }
            });

            deleteBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(null, "댓글을 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    // DB.exceuteUpdate를 사용하여 쿼리 실행
                    DB.exceuteUpdate("DELETE FROM comments WHERE id = ?", comment.getId());
                    PostPanel.this.loadComments(currentPostId);
                }
            });

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            buttonPanel.add(editBtn);
            buttonPanel.add(deleteBtn);
            panel.add(buttonPanel, BorderLayout.SOUTH);
        }

        return panel;
    }

    private void loadComments(int postId) {
        List<Comment> comments = CommentDAO.getCommentsByPostId(postId);
        commentListPanel.removeAll();

        for (int i = 0; i < comments.size(); i++) {
            commentListPanel.add(createCommentItem(comments.get(i)));
        }

        commentListPanel.revalidate();
        commentListPanel.repaint();
    }


    public void showPostDetail(int postId) {
        currentPostId = postId;

        // 조회수 증가
        // DB.exceuteUpdate를 사용하여 쿼리 실행
        DB.exceuteUpdate("UPDATE posts SET views = views + 1 WHERE id = ?", postId);

        // board 테이블과 조인하여 익명 여부도 같이 가져오기
        String sql = "SELECT p.title, p.author, p.date, p.content, p.views, p.likes, p.email, b.is_anonymous " +
                "FROM posts p " +
                "JOIN board b ON p.board_id = b.id " +
                "WHERE p.id = ?";
        ResultSet rs = DB.executeQuery(sql, postId);

        try {
            if (rs != null && rs.next()) {
                String title = rs.getString("title");
                String author = rs.getString("author");
                String date = rs.getString("date");
                String content = rs.getString("content"); // HTML content
                int views = rs.getInt("views");
                int likes = rs.getInt("likes");
                String email = rs.getString("email");
                boolean isAnonymous = rs.getBoolean("is_anonymous");
                this.currentPostEmail = email;
                this.currentBoardIsAnonymous = isAnonymous;

                // 익명 여부에 따라 표시 이름/이메일 결정
                String displayAuthor = isAnonymous ? "익명" : author;
                String displayEmail = isAnonymous ? "비공개" : email;

                // UI 반영
                titleLabel.setText(title);
                // 새로운 작성자 정보 UI 구성
                JLabel labelPrefix = new JLabel("작성자: ");
                labelPrefix.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));

                JButton authorBtn = new JButton(displayAuthor);
                authorBtn.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
                authorBtn.setForeground(Color.BLUE);
                authorBtn.setContentAreaFilled(false);
                authorBtn.setBorderPainted(false);
                authorBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                authorBtn.addActionListener(e -> {
                    if (!isAnonymous) {
                        if (userProfilePanel != null) {
                            cardPanel.remove(userProfilePanel);
                        }

                        userProfilePanel = new UserProfilePanel(
                                email,
                                () -> cardLayout.show(cardPanel, "detail"),
                                id -> showPostDetail(id),
                                currentUser.getRole().equals("ADMIN")

                        );


                        cardPanel.add(userProfilePanel, "userProfile");
                        cardLayout.show(cardPanel, "userProfile");
                    }
                });



                JLabel restMeta = new JLabel(" | 날짜: " + date + " | 조회수: " + views + " | 추천: " + likes);
                restMeta.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
                restMeta.setForeground(Color.GRAY);

                JPanel metaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                metaPanel.setOpaque(false);
                metaPanel.add(labelPrefix);
                metaPanel.add(authorBtn);
                metaPanel.add(restMeta);

                // 기존 SOUTH 컴포넌트 제거
                Component oldMeta = topInfoPanel.getLayout() instanceof BorderLayout
                        ? ((BorderLayout) topInfoPanel.getLayout()).getLayoutComponent(BorderLayout.SOUTH)
                        : null;

                if (oldMeta != null) {
                    topInfoPanel.remove(oldMeta);
                }

                topInfoPanel.add(metaPanel, BorderLayout.SOUTH);

                // JEditorPane에 HTML content 설정
                contentPane.setText(content);
                // HTML 렌더링 후 스크롤바를 맨 위로 이동
                contentPane.setCaretPosition(0);

                loadComments(postId);

                cardLayout.show(cardPanel, "detail");

            } else {
                JOptionPane.showMessageDialog(this, "해당 글을 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "글을 불러오는 중 오류가 발생했습니다.", "DB 오류", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private int getCommentCount(int postId) {
        String sql = "SELECT COUNT(*) FROM comments WHERE post_id = ?";
        ResultSet rs = DB.executeQuery(sql, postId);
        try {
            if (rs != null && rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }


    private void reloadPostList() {
        model.setRowCount(0); // 기존 데이터 초기화
        Set<Integer> popularPostIds = new HashSet<>(); // 인기글 ID를 저장할 Set

        // 1. 인기글 (추천수 높은 상위 5개) 조회
        String popularSql = "SELECT id, title, author, date, likes, views, email FROM posts WHERE board_id = ? ORDER BY likes DESC LIMIT 5";
        try (ResultSet rsPopular = DB.executeQuery(popularSql, borderId)) {
            while (rsPopular.next()) {
                int postId = rsPopular.getInt("id");
                popularPostIds.add(postId); // 인기글 ID 저장

                int commentCount = getCommentCount(postId);
                String rawTitle = rsPopular.getString("title");
                // 인기글임을 표시하는 마커
                String displayTitle = "★[인기글] " + rawTitle + " [" + commentCount + "]";
                String fullDate = rsPopular.getString("date");
                String shortDate = fullDate.length() >= 10 ? fullDate.substring(5, 10) : fullDate;

                Object[] row = new Object[] {
                        postId,
                        displayTitle,
                        rsPopular.getString("author"),
                        shortDate,
                        rsPopular.getInt("likes"),
                        rsPopular.getInt("views")
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "인기글을 불러오는 중 오류가 발생했습니다.", "DB 오류", JOptionPane.ERROR_MESSAGE);
        }

        // 2. 일반 게시글 (최신순) 조회
        StringBuilder normalSql = new StringBuilder("SELECT id, title, author, date, likes, views, email FROM posts WHERE board_id = ?");

        // 검색어가 있을 경우 조건 추가
        if (!keyword.isEmpty() && !searchColumn.isEmpty()) {
            normalSql.append(" AND ").append(searchColumn).append(" LIKE ?");
        }
        // 인기글로 이미 출력된 게시글은 제외
        if (!popularPostIds.isEmpty()) {
            String idsToExclude = popularPostIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            normalSql.append(" AND id NOT IN (").append(idsToExclude).append(")");
        }

        normalSql.append(" ORDER BY ").append(sortTarget).append(" ").append(sortOrder);
        normalSql.append(" LIMIT ?, ?");

        List<Object> params = new ArrayList<>();
        params.add(borderId);
        if (!keyword.isEmpty() && !searchColumn.isEmpty()) {
            params.add("%" + keyword + "%");
        }
        params.add((currentPage - 1) * PAGE_SIZE);
        params.add(PAGE_SIZE);

        try (ResultSet rsNormal = DB.executeQuery(normalSql.toString(), params.toArray())) {
            while (rsNormal.next()) {
                int postId = rsNormal.getInt("id");
                int commentCount = getCommentCount(postId);
                String rawTitle = rsNormal.getString("title");
                String displayTitle = rawTitle + " [" + commentCount + "]";
                String fullDate = rsNormal.getString("date");
                String shortDate = fullDate.length() >= 10 ? fullDate.substring(5, 10) : fullDate;

                Object[] row = new Object[] {
                        postId,
                        displayTitle,
                        rsNormal.getString("author"),
                        shortDate,
                        rsNormal.getInt("likes"),
                        rsNormal.getInt("views")
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "게시글 목록을 불러오는 중 오류가 발생했습니다.", "DB 오류", JOptionPane.ERROR_MESSAGE);
        }

        // 페이지네이션 조정 (총 게시글 수 계산은 인기글 제외해야 함)
        int totalPosts = getTotalPostCountExcludingPopular(popularPostIds);
        int maxPage = (int) Math.ceil((double) totalPosts / PAGE_SIZE);
        if (currentPage > maxPage && maxPage > 0) {
            currentPage = maxPage;
            // 페이지가 너무 커졌을 경우 (예: 검색 결과가 줄었을 때) 재로드
            if (model.getRowCount() == 0 && maxPage > 0) {
                reloadPostList();
            }
        }
    }

    // 인기글을 제외한 총 게시글 수를 가져오는 새로운 메서드
    private int getTotalPostCountExcludingPopular(Set<Integer> popularPostIds) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM posts WHERE board_id = " + borderId);

        if (!keyword.isEmpty() && !searchColumn.isEmpty()) {
            sql.append(" AND ").append(searchColumn).append(" LIKE '%").append(keyword).append("%'");
        }
        if (!popularPostIds.isEmpty()) {
            String idsToExclude = popularPostIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            sql.append(" AND id NOT IN (").append(idsToExclude).append(")");
        }

        ResultSet rs = DB.executeQuery(sql.toString());

        try {
            if (rs != null && rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }


    // 기존 getTotalPostCount는 이제 사용하지 않을 수도 있지만, 혹시 몰라 유지합니다.
    private int getTotalPostCount() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM posts WHERE board_id = " + borderId);

        if (!keyword.isEmpty() && !searchColumn.isEmpty()) {
            sql.append(" AND ").append(searchColumn).append(" LIKE '%").append(keyword).append("%'");
        }

        ResultSet rs = DB.executeQuery(sql.toString());

        try {
            if (rs != null && rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }


    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    private ImageIcon loadIcon(String path, int width, int height) {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            System.err.println("이미지 경로 오류: " + path);
            return null;
        }
        ImageIcon originalIcon = new ImageIcon(resource);
        Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }
}
