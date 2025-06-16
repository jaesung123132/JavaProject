package jscommunity.mainform;

import jscommunity.db.DB;
import jscommunity.db.UserDAO;
import jscommunity.dbmember.Post;
import jscommunity.dbmember.User;
import jscommunity.dialog.PWFindDialog;
import jscommunity.utillity.GeneralButton;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 설정 및 개인 정보, 작성 게시글을 관리하는 UI 패널
 * 프로필 사진 변경, 비밀번호 변경, 작성 게시글 조회 및 삭제 기능을 제공
 */
public class SettingUI extends JPanel {
    private final JPanel profileCircle; // 프로필 이미지를 표시하는 원형 패널
    private JLabel nameLabel, emailLabel, birthLabel, phoneLabel, profileLabel; // 사용자 정보 표시 레이블
    private JPanel myPostsPanel; // 사용자가 작성한 게시글 목록을 담는 패널
    private User currentUser; // 현재 로그인된 사용자 정보
    private final PostClickListener postClickListener; // 게시글 클릭 이벤트를 외부로 전달하기 위한 리스너

    /**
     * 게시글 더블 클릭 이벤트를 처리하기 위한 인터페이스
     */
    public interface PostClickListener {
        /**
         * 게시글이 더블 클릭되었을 때 호출
         * @param boardId 클릭된 게시글이 속한 게시판 ID
         * @param postId 클릭된 게시글의 ID
         */
        void onPostDoubleClick(int boardId, int postId);
    }

    /**
     * SettingUI 패널의 생성자
     *
     * @param onBack 뒤로 가기 버튼 클릭 시 실행될 액션
     * @param listener 게시글 클릭 이벤트를 처리할 PostClickListener 구현체
     */
    public SettingUI(Runnable onBack, PostClickListener listener) {
        this.postClickListener = listener;
        setLayout(new MigLayout("insets 0, fill", "[grow]", "[]10[grow][]"));
        setBackground(Color.WHITE);
        JPanel topPanel = new JPanel(new MigLayout("insets 10, center, gapx 10", "[][][]", "center"));
        topPanel.setOpaque(false);

        profileCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // JPanel의 기본 배경 및 UI 그리기
                Graphics2D g2 = (Graphics2D) g.create(); // 그래픽 컨텍스트 복사 (원본 훼손 방지)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 현재 로그인된 사용자가 있고, 프로필 이미지 경로가 유효한지 확인
                if (currentUser != null && currentUser.getProfileImagePath() != null && !currentUser.getProfileImagePath().isEmpty()) {
                    File imageFile = new File(getProfileImageDirectory(), currentUser.getProfileImagePath()); // 이미지 파일 경로 생성
                    if (imageFile.exists()) { // 이미지 파일이 실제로 존재하는지 확인
                        try {
                            Image img = new ImageIcon(imageFile.getAbsolutePath()).getImage(); // 파일에서 이미지 로드
                            // 원형 클리핑 영역 설정: 패널 크기만큼의 원형으로 이미지를 자릅니다.
                            g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                            // 이미지를 패널 크기에 맞게 그립니다. (비율 유지 대신 패널에 채움)
                            g2.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                        } catch (Exception e) {
                            // 이미지 로드 실패 시 기본 프로필 아이콘을 표시
                            System.err.println("SettingUI: 프로필 이미지 로드 오류: " + e.getMessage()); // 오류는 개발자에게만 표시
                            drawDefaultProfile(g2);
                        }
                    } else {
                        // 파일이 존재하지 않으면 기본 프로필 아이콘을 표시
                        drawDefaultProfile(g2);
                    }
                } else {
                    // 프로필 이미지 경로가 없으면 기본 프로필 아이콘을 표시
                    drawDefaultProfile(g2);
                }
                g2.dispose(); // 그래픽 컨텍스트 자원 해제
            }

            /**
             * 프로필 이미지가 없거나 로드에 실패했을 때 기본 프로필 아이콘(회색 원 + "프로필" 텍스트)을 그림
             * @param g2 그리기 위한 Graphics2D 객체
             */
            private void drawDefaultProfile(Graphics2D g2) {
                g2.setColor(new Color(220, 220, 220)); // 밝은 회색으로 원 채우기
                g2.fillOval(0, 0, getWidth(), getHeight()); // 패널 크기만큼 원형 그리기

                g2.setColor(Color.GRAY); // 회색으로 텍스트 색상 설정
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 12)); // 폰트 설정
                String text = "프로필";
                FontMetrics fm = g2.getFontMetrics(); // 폰트 메트릭스를 사용하여 텍스트 크기 측정
                int x = (getWidth() - fm.stringWidth(text)) / 2; // 텍스트를 원의 가로 중앙에 배치
                int y = (getHeight() + fm.getAscent()) / 2 - 3; // 텍스트를 원의 세로 중앙에 배치
                g2.drawString(text, x, y); // 텍스트 그리기
            }
        };

        // 프로필 이미지 클릭 리스너: 파일 선택 다이얼로그를 열어 프로필 사진을 변경
        profileCircle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentUser == null || currentUser.getEmail() == null) {
                    JOptionPane.showMessageDialog(SettingUI.this, "로그인된 사용자 정보가 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("프로필 사진 선택");
                // 이미지 파일만 선택 가능하도록 필터 설정
                fileChooser.setFileFilter(new FileNameExtensionFilter("이미지 파일", "jpg", "jpeg", "png", "gif"));

                int userSelection = fileChooser.showOpenDialog(SettingUI.this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String originalFileName = selectedFile.getName();
                    String fileExtension = "";
                    int dotIndex = originalFileName.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                        fileExtension = originalFileName.substring(dotIndex);
                    }

                    // 고유한 파일 이름 생성 (UUID + 확장자)하여 파일명 중복 방지
                    String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                    File profileDir = getProfileImageDirectory(); // 프로필 이미지 저장 디렉토리 경로 가져오기
                    Path destinationPath = Paths.get(profileDir.getAbsolutePath(), uniqueFileName);

                    try {
                        // 선택된 파일을 지정된 목적지 경로로 복사
                        Files.copy(selectedFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

                        // DB에 변경된 프로필 이미지 파일 경로 업데이트
                        String imagePathToSave = uniqueFileName; // DB에는 파일 이름만 저장
                        boolean success = UserDAO.updateProfileImagePath(currentUser.getEmail(), imagePathToSave);

                        if (success) {
                            currentUser.setProfileImagePath(imagePathToSave); // 현재 User 객체에도 변경된 경로 업데이트
                            profileCircle.repaint(); // UI를 다시 그려 새 이미지를 반영
                            JOptionPane.showMessageDialog(SettingUI.this, "프로필 사진이 성공적으로 변경되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(SettingUI.this, "프로필 사진 업데이트에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                        }

                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(SettingUI.this, "파일 저장 중 오류가 발생했습니다: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                        System.err.println("SettingUI: 프로필 사진 파일 저장 중 오류: " + ex.getMessage()); // 오류는 개발자에게만 표시
                        ex.printStackTrace();
                    }
                }
            }
        });

        profileCircle.setPreferredSize(new Dimension(60, 60)); // 프로필 원의 크기 설정
        profileCircle.setOpaque(false);
        profileLabel = new JLabel();
        profileLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18)); // 폰트 이름 "마란 고딕" -> "맑은 고딕"으로 통일

        // 상단 패널에 프로필 원과 사용자 이름 레이블 추가
        topPanel.add(profileCircle, "align center, wrap"); // 중앙 정렬, 줄 바꿈
        topPanel.add(profileLabel, "aligny center"); // 세로 중앙 정렬

        // 2. 사용자 정보 표시 패널
        JPanel infoPanel = new JPanel(new MigLayout("wrap 2, insets 5", "[][grow]"));
        infoPanel.setOpaque(false); // 배경 투명 설정
        infoPanel.setBorder(BorderFactory.createTitledBorder("사용자 정보")); // 제목 있는 테두리 추가

        // 이름, 이메일, 생년월일, 전화번호 레이블 초기화 및 추가
        nameLabel = new JLabel();
        emailLabel = new JLabel();
        birthLabel = new JLabel();
        phoneLabel = new JLabel();
        infoPanel.add(new JLabel("이름:")); infoPanel.add(nameLabel);
        infoPanel.add(new JLabel("이메일:")); infoPanel.add(emailLabel);
        infoPanel.add(new JLabel("생년월일:")); infoPanel.add(birthLabel);
        infoPanel.add(new JLabel("전화번호:")); infoPanel.add(phoneLabel);

        // 비밀번호 변경 버튼
        JButton pwChangeBtn = new JButton("비밀번호 변경");
        pwChangeBtn.setMargin(new Insets(0, 0, 0, 0)); // 여백 제거
        pwChangeBtn.setFocusPainted(false); // 포커스 테두리 제거
        pwChangeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 마우스 오버 시 손가락 커서
        pwChangeBtn.setBorderPainted(false); // 테두리 제거
        pwChangeBtn.setContentAreaFilled(false); // 배경 채움 비활성화
        pwChangeBtn.setForeground(new Color(0x007BFF)); // 글자색 설정
        pwChangeBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 13)); // 폰트 설정 (마란 고딕 -> 맑은 고딕)
        pwChangeBtn.addActionListener(e -> {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(SettingUI.this);
            if (currentUser != null) {
                new PWFindDialog(parentFrame, currentUser.getEmail()).setVisible(true); // 비밀번호 찾기/변경 다이얼로그 띄우기
            }
        });
        infoPanel.add(pwChangeBtn, "align left"); // 왼쪽 정렬로 추가

        // 3. 사용자가 작성한 게시글 목록 섹션
        myPostsPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 0", "[grow]", ""));
        myPostsPanel.setOpaque(false); // 배경 투명 설정
        myPostsPanel.setBorder(BorderFactory.createTitledBorder("게시글 수정 및 삭제")); // 제목 있는 테두리 추가

        // '더 보기' 버튼 제거됨

        // 게시글 목록 스크롤 패널
        JScrollPane scrollPane = new JScrollPane(myPostsPanel);
        scrollPane.setPreferredSize(new Dimension(1200, 200)); // 스크롤 패널의 선호 크기 설정
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // 테두리 제거
        scrollPane.getVerticalScrollBar().setUnitIncrement(10); // 스크롤 속도 조절

        // 4. 전체 SettingUI 패널에 컴포넌트 배치
        add(topPanel, "split 2"); // 상단 패널을 왼쪽에 배치하고, 같은 행에 다음 컴포넌트 배치
        add(infoPanel, "growx, wrap"); // 정보 패널을 가로로 확장, 줄 바꿈
        add(scrollPane, "grow, pushy, wrap"); // 스크롤 패널을 전체 남은 공간을 차지하도록 확장, 줄 바꿈

        // 뒤로가기 버튼 생성 (GeneralButton 활용)
        JButton backBtn = new GeneralButton("←", 45, 30, e -> onBack.run());
        add(backBtn, "width 50!, align right, pushy, dock south, gapright 25, gaptop 10"); // 크기, 정렬, 도킹, 여백 설정
    }

    /**
     * 현재 로그인된 사용자 정보를 업데이트하고 UI에 반영합니다.
     *
     * @param user 새로운 사용자 정보 객체
     */
    public void updateUser(User user) {
        this.currentUser = user;
        if (user != null) {
            profileLabel.setText(user.getName() + "님"); // 프로필 제목 업데이트
            nameLabel.setText(user.getName()); // 이름 레이블 업데이트
            emailLabel.setText(user.getEmail()); // 이메일 레이블 업데이트
            // 생년월일과 전화번호는 null 체크 후 업데이트
            birthLabel.setText(user.getBirthday() != null ? user.getBirthday() : "-");
            phoneLabel.setText(user.getPhone() != null ? user.getPhone() : "-");

            profileCircle.repaint(); // 프로필 이미지를 다시 그려 새 사용자의 이미지를 반영
            myPostsPanel.removeAll(); // 기존 게시글 목록을 지웁니다.
            // '더 보기' 버튼이 없으므로 다시 추가할 필요 없음
            // currentPage = 0; // 페이지 초기화도 필요 없음 (모두 로드)
            loadPostsAsync(user.getEmail()); // 해당 사용자의 게시글 목록을 비동기로 로드
        }
    }

    /**
     * 사용자가 작성한 게시글 목록을 비동기적으로 로드하여 UI에 추가합니다.
     * 이제 모든 게시글을 한 번에 로드합니다.
     *
     * @param email 게시글을 로드할 사용자의 이메일 주소
     */
    private void loadPostsAsync(String email) {

        SwingWorker<List<Post>, Void> worker = new SwingWorker<List<Post>, Void>() {
            @Override
            protected List<Post> doInBackground() {
                List<Post> result = new ArrayList<>();
                ResultSet rs = null;
                try {
                    // 게시글 가져오기
                    String sql = "SELECT id, title, author, board_id FROM posts WHERE email = ? ORDER BY id DESC";
                    rs = DB.executeQuery(sql, email);
                    while (rs != null && rs.next()) {
                        Post post = new Post();
                        post.setId(rs.getInt("id"));
                        post.setTitle(rs.getString("title"));
                        post.setAuthor(rs.getString("author"));
                        post.setBoardId(rs.getInt("board_id"));
                        result.add(post);
                    }
                } catch (SQLException e) {
                    System.err.println("SettingUI: 게시글 로드 중 DB 오류 발생: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        if (rs != null) rs.close();
                    } catch (SQLException e) {
                        System.err.println("SettingUI: ResultSet 닫기 중 오류: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    List<Post> posts = get();
                    if (posts.isEmpty()) {
                        JLabel noPostsLabel = new JLabel("작성한 게시글이 없습니다.");
                        noPostsLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        noPostsLabel.setForeground(Color.GRAY);
                        myPostsPanel.add(noPostsLabel, "growx, wrap, span, gaptop 20");
                        myPostsPanel.revalidate();
                        myPostsPanel.repaint();
                        return;
                    }

                    // 로드된 게시글들 추가
                    for (Post post : posts) {
                        JLabel titleLabel = new JLabel(post.getTitle());
                        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                        // 게시글 제목 클릭 리스너 (더블 클릭 시 게시글 상세 보기)
                        titleLabel.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                                    postClickListener.onPostDoubleClick(post.getBoardId(), post.getId());
                                }
                            }
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                titleLabel.setForeground(Color.BLUE.darker());
                            }
                            @Override
                            public void mouseExited(MouseEvent e) {
                                titleLabel.setForeground(Color.BLACK);
                            }
                        });

                        // 게시글 번호
                        JLabel postIdLabel = new JLabel(String.valueOf(post.getId()));
                        postIdLabel.setForeground(Color.GRAY);
                        postIdLabel.setFont(postIdLabel.getFont().deriveFont(Font.PLAIN, 12));

                        // 삭제 버튼 생성 (GeneralButton 활용)
                        JButton deleteBtn = new GeneralButton("삭제", 80, 30,new Color(25, 100, 230),Color.WHITE);

                        // 각 게시글 항목을 담을 패널
                        JPanel row = new JPanel(new MigLayout("fillx, insets 5 0 5 0", "[50!]10[grow, fill][]", "")); // 번호, 제목, 버튼
                        row.setOpaque(false);

                        // 각 게시글 항목에 밑줄을 추가
                        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230))); // 얇은 회색 밑줄

                        row.add(postIdLabel);
                        row.add(titleLabel);
                        row.add(deleteBtn, "width 80!, height 30!, align right");

                        // 삭제 버튼 클릭 리스너
                        deleteBtn.addActionListener(e -> {
                            int result = JOptionPane.showConfirmDialog(
                                    null,
                                    "정말 삭제하시겠습니까?",
                                    "삭제 확인",
                                    JOptionPane.YES_NO_OPTION
                            );
                            if (result == JOptionPane.YES_OPTION) {
                                String sql = "DELETE FROM posts WHERE id = ?";
                                int res = DB.exceuteUpdate(sql, post.getId());
                                if (res > 0) {
                                    JOptionPane.showMessageDialog(null, "삭제 완료되었습니다.");
                                    myPostsPanel.remove(row);
                                    myPostsPanel.revalidate();
                                    myPostsPanel.repaint();
                                } else {
                                    JOptionPane.showMessageDialog(null, "삭제에 실패했습니다.");
                                }
                            }
                        });

                        myPostsPanel.add(row, "growx, wrap");
                    }
                    myPostsPanel.revalidate();
                    myPostsPanel.repaint();
                } catch (Exception e) {
                    System.err.println("SettingUI: 게시글 로드 후 처리 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    /**
     * 프로필 이미지 파일을 저장하거나 로드할 디렉토리 경로를 반환합니다.
     * 이 디렉토리가 없으면 새로 생성합니다.
     * @return 프로필 이미지 디렉토리 File 객체
     */
    private File getProfileImageDirectory() {
        String appPath = System.getProperty("user.dir");
        File profileDirectory = new File(appPath, "user_profiles");

        if (!profileDirectory.exists()) {
            profileDirectory.mkdirs();
        }
        return profileDirectory;
    }
}