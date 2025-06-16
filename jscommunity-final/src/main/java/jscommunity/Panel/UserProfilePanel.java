package jscommunity.Panel;

import jscommunity.db.DB;
import jscommunity.dbmember.User;
import jscommunity.dbmember.Post;
import jscommunity.utillity.GeneralButton;
import jscommunity.utillity.UIUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 사용자 프로필 정보를 표시하고, 해당 사용자가 작성한 게시글 목록을 보여주는 패널
 * 다른 사용자의 프로필을 조회할 때 사용
 */
public class UserProfilePanel extends JPanel {

    // 프로필 이미지 패널의 목표 크기 (정사각형)
    private static final int PROFILE_IMAGE_SIZE = 150;

    private User user; // 현재 표시될 사용자 정보 객체
    private Consumer<Integer> postDetailConsumer; // 게시글 상세 보기 요청을 처리할 콜백

    /**
     * UserProfilePanel의 생성자
     * @param email 사용자 프로필을 조회할 이메일 주소
     * @param onBack 뒤로 가기 버튼 클릭 시 실행될 액션 (예: 메인 화면으로 돌아가기)
     * @param postDetailConsumer 게시글 목록에서 게시글 더블 클릭 시 해당 게시글 ID를 전달받아 처리할 Consumer
     */
    public UserProfilePanel(String email, Runnable onBack, Consumer<Integer> postDetailConsumer, boolean isAdmin) {
        this.postDetailConsumer = postDetailConsumer;
        setLayout(new MigLayout("insets 20, fill", "[grow]", "[]10[grow, fill][]"));
        setBackground(Color.WHITE);

        // 사용자 정보 로드: DB에서 이메일로 사용자 정보와 프로필 이미지 경로를 가져옴
        this.user = getUserByEmail(email);
        if (this.user == null) {
            // 사용자 정보를 불러오지 못했을 경우 오류 메시지 표시 후 패널 생성 중단
            add(new JLabel("사용자 정보를 불러올 수 없습니다."), "span, center");
            return;
        }

        // 사용자가 작성한 게시글 목록 로드
        List<Post> posts = getPostsByEmail(email);

        // 상단 프로필 섹션
        JPanel topProfileSection = new JPanel(new MigLayout("insets 10, center, gapx 10", "[align center]", "[]10[]10[]"));
        topProfileSection.setOpaque(false);

        // 프로필 이미지 표시를 위한 원형 패널
        JPanel profileCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // 부모 클래스의 paintComponent를 호출하여 JPanel의 기본 배경 및 UI를 그림
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create(); // 그래픽 컨텍스트 복사 (원본 훼손 방지)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int panelWidth = getWidth();
                int panelHeight = getHeight();

                if (user != null && user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
                    File imageFile = new File(getProfileImageDirectory(), user.getProfileImagePath());

                    if (imageFile.exists()) { // 이미지 파일이 실제로 존재하는지 확인
                        try {
                            // 파일 경로로부터 이미지 로드
                            Image img = new ImageIcon(imageFile.getAbsolutePath()).getImage();

                            // MediaTracker를 사용하여 이미지가 완전히 로드될 때까지 대기
                            // 비동기 이미지 로딩으로 인한 렌더링 문제 방지
                            MediaTracker tracker = new MediaTracker(this);
                            tracker.addImage(img, 0); // 이미지 추가, ID는 0
                            tracker.waitForID(0); // ID 0번 이미지가 로드될 때까지 현재 스레드 대기

                            // 이미지 로딩 중 오류가 발생했거나 로딩이 실패했는지 확인
                            if (tracker.isErrorAny() || !tracker.checkID(0, true)) {
                                drawDefaultProfile(g2); // 오류 시 기본 프로필 그리기
                                g2.dispose();
                                return;
                            }

                            // 로드된 이미지의 실제 크기 확인
                            int imgWidth = img.getWidth(null);
                            int imgHeight = img.getHeight(null);

                            if (imgWidth <= 0 || imgHeight <= 0) {
                                // 이미지 크기가 유효하지 않으면 기본 프로필 그리기
                                drawDefaultProfile(g2);
                                g2.dispose();
                                return;
                            }

                            // 원형 클리핑 영역 설정: 패널 크기만큼의 원형으로 이미지를 자르기
                            g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, panelWidth, panelHeight));
                            // 이미지를 패널 크기에 맞게 직접 그리기 (비율 유지 대신 패널에 채움)
                            g2.drawImage(img, 0, 0, panelWidth, panelHeight, null);

                        } catch (Exception e) {
                            // 이미지 로드 또는 그리기 중 예외 발생 시 오류 출력 및 기본 프로필 그리기
                            System.err.println("프로필 이미지 로드/그리기 중 오류 발생: " + e.getMessage());
                            e.printStackTrace();
                            drawDefaultProfile(g2);
                        }
                    } else {
                        // 이미지 파일이 존재하지 않으면 기본 프로필 그리기
                        drawDefaultProfile(g2);
                    }
                } else {
                    // 사용자 정보나 프로필 이미지 경로가 없으면 기본 프로필 그리기
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
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 14)); // 폰트 설정
                String text = "프로필";
                FontMetrics fm = g2.getFontMetrics(); // 폰트 메트릭스를 사용하여 텍스트 크기 측정
                int x = (getWidth() - fm.stringWidth(text)) / 2; // 텍스트를 원의 가로 중앙에 배치
                int y = (getHeight() + fm.getAscent()) / 2 - 3; // 텍스트를 원의 세로 중앙에 배치
                g2.drawString(text, x, y); // 텍스트 그리기
            }
        };

        // profileCircle 패널의 선호 크기를 정의된 상수에 따라 설정
        profileCircle.setPreferredSize(new Dimension(PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE));
        profileCircle.setOpaque(false);
        topProfileSection.add(profileCircle, "wrap, w " + PROFILE_IMAGE_SIZE + "!, h " + PROFILE_IMAGE_SIZE + "!");

        // "OO님의 프로필" 제목 레이블
        JLabel profileTitleLabel = new JLabel(user.getName() + "님의 프로필");
        profileTitleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        profileTitleLabel.setHorizontalAlignment(SwingConstants.CENTER); // 가운데 정렬
        topProfileSection.add(profileTitleLabel, "wrap 20"); // wrap 20: 20px 아래로 줄 바꿈

        // 사용자 상세 정보 표시 패널 (이름, 이메일)
        JPanel userInfoPanel = new JPanel(new MigLayout("wrap 2, insets 5", "[][grow]", ""));
        userInfoPanel.setOpaque(false);
        userInfoPanel.add(new JLabel("이름:")); userInfoPanel.add(new JLabel(user.getName()));
        userInfoPanel.add(new JLabel("이메일:")); userInfoPanel.add(new JLabel(user.getEmail()));
        JButton suspensionbtn = new GeneralButton("정지", 80, 40);
        suspensionbtn.setVisible(false);
        if(isAdmin) {
            suspensionbtn.setVisible(true);
        }

        suspensionbtn.addActionListener(e -> {
            LocalDate suspensionEndDate = user.getSuspensionEndDate();
            boolean isCurrentlySuspended = suspensionEndDate != null && suspensionEndDate.isAfter(LocalDate.now());

            String initialMessage;
            String dialogTitle;
            String placeholderText = "정지 기간 (일)";

            if (isCurrentlySuspended) { // 이미 정지 상태인 경우
                initialMessage = user.getName() + "님은 " + suspensionEndDate + "까지 정지 상태입니다.";
                dialogTitle = "정지 기간 연장/해제";
            } else { // 정지 상태가 아닌 경우
                initialMessage = user.getName() + "님을 정지하시겠습니까?";
                dialogTitle = "사용자 정지";
            }

            // --- 커스텀 다이얼로그 생성 ---
            JDialog inputDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), dialogTitle, true);
            inputDialog.setLayout(new MigLayout("insets 15", "[grow]", "[]10[]10[]"));
            inputDialog.setBackground(Color.WHITE);

            JLabel messageLabel = new JLabel("<html>" + initialMessage + "<br>며칠 동안 정지하시겠습니까? (0 입력 시 정지 해제)</html>");
            messageLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            inputDialog.add(messageLabel, "wrap, growx");

            JTextField daysField = new JTextField();
            JLabel errorLabel = new JLabel(""); // 에러 메시지를 표시할 레이블
            errorLabel.setForeground(Color.RED);
            errorLabel.setVisible(false); // 초기에는 숨김

            // Runnable onChange는 여기서는 입력값 변경 시 다른 로직을 수행할 필요가 없으므로 null
            UIUtils.setupTextField(daysField, placeholderText, errorLabel, null);
            inputDialog.add(daysField, "wrap, growx");
            inputDialog.add(errorLabel, "wrap, growx"); // 에러 레이블 추가

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setOpaque(false);

            JButton okButton = new GeneralButton("확인", 70, 30);
            JButton cancelButton = new GeneralButton("취소", 70, 30);

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            inputDialog.add(buttonPanel, "wrap, align right");

            // 다이얼로그 크기 및 위치 설정
            inputDialog.pack();
            inputDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));

            // 확인 버튼 액션
            okButton.addActionListener(okEvent -> {
                String daysStr = daysField.getText().trim();
                if (daysStr.equals(placeholderText) || daysStr.isEmpty()) {
                    errorLabel.setText("기간을 입력해주세요.");
                    errorLabel.setVisible(true);
                    daysField.setBorder(UIUtils.getErrorBorder()); // 에러 시 빨간 테두리
                    return;
                }

                try {
                    int days = Integer.parseInt(daysStr);
                    LocalDate newSuspensionEndDate = null;

                    if (days > 0) { // 양수 입력: 정지 기간 설정
                        newSuspensionEndDate = LocalDate.now().plusDays(days);
                    } else if (days == 0) { // 0일 입력: 정지 해제 확인
                        int confirm = JOptionPane.showConfirmDialog(inputDialog,
                                user.getName() + "님의 정지를 해제하시겠습니까?",
                                "정지 해제 확인", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.NO_OPTION) {
                            return; // 사용자가 '아니오'를 선택하면 다이얼로그 유지
                        }
                    } else { // 음수 입력: 유효하지 않은 기간
                        errorLabel.setText("정지 기간은 0일 이상이어야 합니다.");
                        errorLabel.setVisible(true);
                        daysField.setBorder(UIUtils.getErrorBorder()); // 에러 시 빨간 테두리
                        return;
                    }

                    // UserDAO.updateUserSuspension() 호출
                    boolean updateSuccess = jscommunity.db.UserDAO.updateUserSuspension(user.getEmail(), newSuspensionEndDate);

                    if (updateSuccess) {
                        user.setSuspensionEndDate(newSuspensionEndDate); // user 객체 업데이트

                        if (newSuspensionEndDate == null) {
                            JOptionPane.showMessageDialog(this,
                                    user.getName() + " 사용자의 정지가 해제되었습니다.",
                                    "정지 해제 성공", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    user.getName() + " 사용자가 " + days + "일 동안 정지되었습니다.\n정지 종료일: " + newSuspensionEndDate,
                                    "정지 성공", JOptionPane.INFORMATION_MESSAGE);
                        }
                        revalidate();
                        repaint();
                        inputDialog.dispose(); // 다이얼로그 닫기
                    } else {
                        JOptionPane.showMessageDialog(this, "사용자 정지에 실패했습니다. (DB 오류)", "정지 오류", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (NumberFormatException ex) {
                    errorLabel.setText("올바른 숫자를 입력해주세요.");
                    errorLabel.setVisible(true);
                    daysField.setBorder(UIUtils.getErrorBorder()); // 에러 시 빨간 테두리
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "정지 처리 중 오류 발생: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            // 취소 버튼 액션
            cancelButton.addActionListener(cancelEvent -> {
                inputDialog.dispose(); // 다이얼로그 닫기
            });

            inputDialog.setVisible(true); // 다이얼로그 표시
        });
        userInfoPanel.add(suspensionbtn);
        // (필요하다면 생년월일, 전화번호 등 추가)

        topProfileSection.add(userInfoPanel, "wrap 20"); // wrap 20: 20px 아래로 줄 바꿈

        // 4. 게시글 목록 섹션
        JPanel postsListPanel = new JPanel(new MigLayout("wrap 1, fillx, insets 0", "[grow]", ""));
        postsListPanel.setOpaque(false); // 투명하게 설정
        postsListPanel.setBorder(BorderFactory.createTitledBorder("게시글 목록")); // 제목 있는 테두리 추가

        if (posts.isEmpty()) {
            postsListPanel.add(new JLabel("작성한 게시글이 없습니다."), "center, gaptop 20"); // 게시글이 없을 경우 메시지
        } else {
            // 게시글 목록을 순회하며 각 게시글 엔트리 생성 및 추가
            for (Post p : posts) {
                JPanel postEntryPanel = new JPanel(new MigLayout("fillx, insets 5 10 5 10", "[grow][]", ""));
                postEntryPanel.setBackground(new Color(245, 245, 245)); // 게시글 엔트리 배경색
                postEntryPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220))); // 하단 테두리

                JLabel titleLabel = new JLabel("<html><b>" + p.getTitle() + "</b></html>"); // HTML을 사용하여 굵게 표시
                titleLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
                titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 마우스 오버 시 손가락 커서로 변경

                JLabel infoLabel = new JLabel("<html><font color='gray'>" + p.getDate() + " | 조회수: " + p.getViews() + "</font></html>");
                infoLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
                infoLabel.setHorizontalAlignment(SwingConstants.RIGHT); // 오른쪽에 정렬

                postEntryPanel.add(titleLabel, "growx, pushx"); // 제목 레이블이 가로 공간을 최대한 차지하도록 설정
                postEntryPanel.add(infoLabel, "align right, wrap"); // 정보 레이블을 오른쪽에 정렬하고 줄 바꿈

                // 게시글 엔트리 클릭/호버 이벤트 리스너 추가
                postEntryPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // 더블 클릭 시 게시글 상세 보기 콜백 실행
                        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                            postDetailConsumer.accept(p.getId());
                        }
                    }
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        // 마우스 진입 시 배경색 및 제목 색상 변경 (시각적 피드백)
                        postEntryPanel.setBackground(new Color(230, 230, 230));
                        titleLabel.setForeground(new Color(0, 100, 200));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        // 마우스 이탈 시 배경색 및 제목 색상 원복
                        postEntryPanel.setBackground(new Color(245, 245, 245));
                        titleLabel.setForeground(Color.BLACK);
                    }
                });
                postsListPanel.add(postEntryPanel, "growx"); // 게시글 엔트리를 게시글 목록 패널에 추가
            }
        }

        // 게시글 목록을 스크롤 가능하게 만듬
        JScrollPane postsScrollPane = new JScrollPane(postsListPanel);
        postsScrollPane.setBorder(new EmptyBorder(0,0,0,0)); // 스크롤 패널 자체의 테두리 제거
        postsScrollPane.getVerticalScrollBar().setUnitIncrement(16); // 스크롤 속도 조절 (휠 당 이동 거리)

        // 전체 UserProfilePanel에 주요 컴포넌트 배치
        add(topProfileSection, "north, wrap"); // 상단 프로필 섹션을 최상단에 배치하고 줄 바꿈
        add(postsScrollPane, "grow, push, wrap"); // 게시글 스크롤 패널이 남은 공간을 모두 차지하도록 확장

        // 뒤로가기 버튼 생성 및 배치
        JButton backButton = new GeneralButton("←", 50, 30, e -> onBack.run());
        add(backButton, "south, align right, gaptop 15, width 50!"); // 하단, 오른쪽 정렬, 상단 여백, 너비 고정
    }

    /**
     * 이메일 주소를 기반으로 DB에서 사용자 정보를 조회합니다.
     * @param email 조회할 사용자의 이메일 주소
     * @return 조회된 User 객체 또는 정보가 없을 경우 null
     */
    private User getUserByEmail(String email) {
        return jscommunity.db.UserDAO.findByEmail(email);
    }

    /**
     * 특정 사용자가 작성한 게시글 목록을 DB에서 조회합니다.
     * 익명 게시판의 게시글은 제외됩니다.
     * @param email 게시글을 조회할 사용자의 이메일 주소
     * @return 해당 사용자가 작성한 게시글 목록 (Post 객체 리스트)
     */
    private List<Post> getPostsByEmail(String email) {
        List<Post> postList = new ArrayList<>();

        String sql = "SELECT p.id, p.title, p.date, p.views " +
                "FROM posts p " +
                "JOIN board b ON p.board_id = b.id " +
                "WHERE p.email = ? AND b.is_anonymous = 0 " + // 익명 게시판 제외
                "ORDER BY p.date DESC"; // 최신순 정렬

        ResultSet rs = null;
        try {
            rs = DB.executeQuery(sql, email);
            if (rs != null) {
                while (rs.next()) {
                    Post post = new Post();
                    post.setId(rs.getInt("id"));
                    post.setTitle(rs.getString("title"));
                    post.setDate(rs.getString("date"));
                    post.setViews(rs.getInt("views"));
                    postList.add(post);
                }
            }
        } catch (SQLException e) {
            System.err.println("게시글 목록 조회 중 DB 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                System.err.println("ResultSet 닫기 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return postList;
    }

    /**
     * 프로필 이미지 파일을 저장하거나 로드할 디렉토리 경로를 반환
     * 이 디렉토리가 없으면 새로 생성
     * @return 프로필 이미지 디렉토리 File 객체
     */
    private File getProfileImageDirectory() {
        // 애플리케이션이 실행되는 현재 디렉토리를 기준으로 'user_profiles' 폴더를 사용합니다.
        String appPath = System.getProperty("user.dir");
        File profileDirectory = new File(appPath, "user_profiles");

        // 디렉토리가 존재하지 않으면 생성합니다.
        if (!profileDirectory.exists()) {
            profileDirectory.mkdirs();
        }
        return profileDirectory;
    }
}