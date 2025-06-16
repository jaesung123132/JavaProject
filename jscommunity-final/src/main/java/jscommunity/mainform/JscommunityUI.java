package jscommunity.mainform;

import jscommunity.Panel.PostPanel;
import jscommunity.dialog.JoinDialog;
import jscommunity.dialog.LoginDialog;
import jscommunity.db.BorderDAO;
import jscommunity.dbmember.Border;
import jscommunity.dbmember.User;
import jscommunity.dialog.PWFindDialog;
import net.miginfocom.swing.MigLayout;
import jscommunity.utillity.TabButton;
import jscommunity.utillity.GeneralButton; // GeneralButton 임포트
import jscommunity.utillity.UIUtils; // UIUtils 임포트 추가
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * JscommunityUI 클래스는 애플리케이션의 메인 프레임을 담당
 * 사용자 로그인/로그아웃, 게시판 목록 표시, 게시글 조회, 설정 화면 전환 등 주요 기능
 */
public class JscommunityUI extends JFrame {
    private JPanel loginPanel;                  // 로그인 전/후 상태를 전환하는 패널
    private JLabel lblWelcome;                  // 환영 메시지를 표시하는 레이블
    private User currentUser;                   // 현재 로그인된 사용자 정보

    private CardLayout centerCardLayout;        // 중앙 콘텐츠 패널의 레이아웃 (게시판, 설정 화면 전환용)
    private JPanel centerCardPanel;             // 게시판 및 설정 화면을 담는 중앙 패널
    private SettingUI settingUI;                // 사용자 설정 화면

    private Map<Integer, PostPanel> postPanelMap = new HashMap<>(); // 게시판 ID와 PostPanel 인스턴스를 매핑
    private JLabel logoLabel;                   // 애플리케이션 로고를 표시하는 레이블
    private List<Border> borders;               // DB에서 로드된 게시판 목록

    private JPanel menuBar;                     // 게시판 탭 버튼과 페이지 이동 버튼을 포함하는 메뉴바
    private int currentPage = 0;                // 현재 게시판 페이지 (페이지네이션용)
    private final int boardsPerPage = 10;       // 한 페이지에 표시될 게시판 버튼의 수
    private JButton prevPageButton;             // 이전 페이지로 이동하는 버튼
    private JButton nextPageButton;             // 다음 페이지로 이동하는 버튼
    private JPanel boardButtonsPanel;           // 게시판 탭 버튼들을 담는 패널
    private TabButton currentSelectedTabButton = null; // 현재 선택된 게시판 탭 버튼
    private int currentBoardId = -1;            // 현재 화면에 표시된 게시판의 ID (초기값 -1: 게시판 없음 또는 초기 상태)

    // 설정 화면으로 이동하기 전의 게시판 ID를 저장하는 변수.
    private int previousBoardId = -1;

    /**
     * JscommunityUI 애플리케이션의 메인 메서드
     * EventQueue.invokeLater를 사용하여 모든 UI 작업이 EDT(Event Dispatch Thread)에서 실행
     * @param args 커맨드 라인 인수 (사용되지 않음)
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                new JscommunityUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * JscommunityUI의 생성자
     * UI 컴포넌트들을 초기화하고 화면에 표시
     */
    public JscommunityUI() {
        initialize();
        setVisible(true);
    }

    /**
     * 메인 프레임의 기본 설정을 초기화
     * 창 제목, 크기, 위치, 종료 동작, 레이아웃 및 배경색을 설정
     * 헤더, 중앙 콘텐츠, 푸터 패널을 추가
     */
    private void initialize() {
        setTitle("JsCommunity");
        setSize(2000, 1200);
        setLocationRelativeTo(null); // 화면 중앙에 위치
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // X 버튼 클릭 시 애플리케이션 종료
        getContentPane().setLayout(new MigLayout("fill", "[grow]", "[]0[grow]0[]")); // MigLayout 설정
        getContentPane().setBackground(Color.WHITE); // 배경색 설정
        UIManager.put("Panel.background", Color.WHITE); // 모든 JPanel의 기본 배경색 설정

        addHeader(); // 헤더 패널 추가
        getContentPane().add(createThickSeparator(), "growx, wrap"); // 구분선 추가

        addCenter(); // 중앙 콘텐츠 패널 추가
        getContentPane().add(createThickSeparator(), "growx, wrap"); // 구분선 추가

        addFooter(); // 푸터 패널 추가
    }

    /**
     * 애플리케이션의 헤더 영역을 구성
     * 로고와 로그인/회원가입 버튼 또는 로그인 후 정보를 표시하는 패널을 포함
     */
    private void addHeader() {
        JPanel headerPanel = new JPanel(new MigLayout("insets 10, fillx", "[left][right]", "[]"));
        getContentPane().add(headerPanel, "dock north"); // 프레임 상단에 고정

        logoLabel = new JLabel();
        // UIUtils의 createResizedIcon 메서드를 사용하여 로고 이미지 로드
        ImageIcon logo = UIUtils.createResizedIcon("/logo.png", 160, 40);
        if (logo != null) {
            logoLabel.setIcon(logo);
        } else {
            logoLabel.setText("JsCommunity"); // 이미지 로드 실패 시 텍스트 표시
        }

        headerPanel.add(logoLabel, "left, aligny top"); // 로고를 왼쪽 상단에 배치

        loginPanel = new JPanel(new CardLayout()); // 로그인 전/후 UI 전환을 위한 CardLayout
        loginPanel.add(createBeforeLoginPanel(), "before"); // 로그인 전 패널 추가
        loginPanel.add(createAfterLoginPanel(), "after"); // 로그인 후 패널 추가
        headerPanel.add(loginPanel, "right, aligny top"); // 로그인 패널을 오른쪽 상단에 배치
    }

    /**
     * 애플리케이션의 중앙 콘텐츠 영역을 구성
     * 게시판 목록을 표시하는 메뉴바와 실제 게시글 목록/상세보기를 보여주는 카드 패널을 포함
     */
    private void addCenter() {
        centerCardLayout = new CardLayout();
        centerCardPanel = new JPanel(centerCardLayout);
        centerCardPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // 상단 여백 추가

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 20 20 20 20", "[grow]", "[60!][grow, fill]")); // 메인 중앙 패널

        menuBar = new JPanel(new MigLayout("center, gapx 5, insets 10 0 10 0", "[]5[grow, fill]5[]", "[]")); // 메뉴바 패널

        // 이전 페이지 버튼 설정
        prevPageButton = new JButton("◀");
        prevPageButton.setPreferredSize(new Dimension(50, 40));
        prevPageButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                updateBoardPaginationUI(); // 페이지 UI 업데이트
            }
        });
        menuBar.add(prevPageButton, "cell 0 0");

        // 게시판 버튼들을 담을 패널 설정
        boardButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        menuBar.add(boardButtonsPanel, "cell 1 0, growx, pushx");

        // 다음 페이지 버튼 설정
        nextPageButton = new JButton("▶");
        nextPageButton.setPreferredSize(new Dimension(50, 40));
        nextPageButton.addActionListener(e -> {
            // 총 페이지 수 = (전체 게시글 수 ÷ 페이지당 표시 수)를 올림한 값
            int totalPages = (int) Math.ceil((double) borders.size() / boardsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateBoardPaginationUI(); // 페이지 UI 업데이트
            }
        });
        menuBar.add(nextPageButton, "cell 2 0");

        borders = null; // 게시판 목록 초기화
        try {
            borders = BorderDAO.findAll(); // 데이터베이스에서 모든 게시판 정보 로드

            if (borders != null) {
                for (Border border : borders) {
                    int borderId = border.getId();
                    PostPanel postPanel = new PostPanel(currentUser, borderId); // 각 게시판에 대한 PostPanel 생성
                    postPanelMap.put(borderId, postPanel); // 맵에 저장
                    centerCardPanel.add(postPanel, String.valueOf(borderId)); // 카드 레이아웃에 추가 (카드 이름은 게시판 ID)
                }
            }

            // 초기 게시판 표시 로직
            if (borders != null && !borders.isEmpty()) {
                int initialBoardId = borders.get(0).getId(); // 첫 번째 게시판 ID
                centerCardLayout.show(centerCardPanel, String.valueOf(initialBoardId)); // 첫 번째 게시판으로 이동
                currentBoardId = initialBoardId; // 현재 게시판 ID 업데이트
            } else {
                // 게시판이 없는 경우 메시지 표시
                centerCardPanel.add(new JLabel("<html><center>아직 생성된 게시판이 없습니다.</center></html>", SwingConstants.CENTER), "no_boards_user");
                centerCardLayout.show(centerCardPanel, "no_boards_user");
                currentBoardId = -1; // 게시판 없음 상태 ID
                prevPageButton.setEnabled(false); // 페이지 버튼 비활성화
                nextPageButton.setEnabled(false);
            }

            updateBoardPaginationUI(); // 초기 게시판 UI 업데이트 호출
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "게시판 로드 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            // 게시판 로드 실패 시 에러 메시지 표시
            centerCardPanel.add(new JLabel("<html><center>게시판을 불러오는 데 실패했습니다.<br>잠시 후 다시 시도해주세요.</center></html>", SwingConstants.CENTER), "error_board_load");
            currentBoardId = -2; // 오류 상태 ID
            centerCardLayout.show(centerCardPanel, "error_board_load");
            prevPageButton.setEnabled(false); // 페이지 버튼 비활성화
            nextPageButton.setEnabled(false);
        }

        mainPanel.add(menuBar, "wrap, align center"); // 메뉴바를 메인 패널에 추가
        mainPanel.add(centerCardPanel, "grow, push, span"); // 중앙 카드 패널을 메인 패널에 추가
        add(mainPanel, "grow, wrap"); // 메인 패널을 프레임 콘텐츠 팬에 추가

        /** SettingUI 초기화:
         * onBack 람다: 설정 화면에서 '뒤로가기' 버튼 클릭 시 실행될 로직
         * previousBoardId에 저장된 원래 게시판으로 돌아가거나,
         * 게시판이 없었거나 로드 오류 상태였을 때의 적절한 화면으로 복귀
         * 2. PostClickListener 람다: SettingUI 내에서 게시글 클릭 시 해당 게시글의 상세 화면으로 이동
         */
        settingUI = new SettingUI(
                () -> {
                    if (previousBoardId > 0) { // 유효한 게시판 ID인 경우
                        centerCardLayout.show(centerCardPanel, String.valueOf(previousBoardId));
                        currentBoardId = previousBoardId; // currentBoardId도 원래 값으로 복원
                    } else if (previousBoardId == -1) { // 게시판이 없는 상태에서 설정으로 갔을 경우
                        centerCardLayout.show(centerCardPanel, "no_boards_user");
                        currentBoardId = -1;
                    }
                    else {
                        // 예상치 못한 previousBoardId 값 처리 (예: 초기 로드 전이거나 잘못된 값)
                        if (borders != null && !borders.isEmpty()) {
                            int initialBoardId = borders.get(0).getId();
                            centerCardLayout.show(centerCardPanel, String.valueOf(initialBoardId));
                            currentBoardId = initialBoardId;
                        } else {
                            centerCardLayout.show(centerCardPanel, "no_boards_user");
                            currentBoardId = -1;
                        }
                        System.err.println("SettingUI에서 돌아갈 previousBoardId 값이 유효하지 않습니다: " + previousBoardId);
                    }
                },
                (boardId, postId) -> {
                    PostPanel panel = postPanelMap.get(boardId);
                    if (panel != null) {
                        panel.showPostDetail(postId); // 해당 게시글 상세 보기
                        centerCardLayout.show(centerCardPanel, String.valueOf(boardId)); // 해당 게시판으로 전환
                        currentBoardId = boardId; // 현재 활성화된 게시판 ID 업데이트
                        previousBoardId = boardId; // SettingUI에서 게시글 클릭으로 PostPanel로 돌아올 경우, previousBoardId도 업데이트
                    } else {
                        System.err.println("해당 boardId에 대한 PostPanel이 없습니다: " + boardId);
                    }
                }
        );

        // SettingUI를 메인 CardLayout에 "setting"이라는 카드 이름으로 추가
        centerCardPanel.add(settingUI, "setting");
    }

    /**
     * 애플리케이션의 푸터 영역을 구성
     * 저작권 정보 및 문의 메일 주소를 표시
     */
    private void addFooter() {
        JPanel footPanel = new JPanel(new MigLayout("center, insets 10")); // 푸터 패널
        add(footPanel, "dock south"); // 프레임 하단에 도킹

        JLabel lblContact = new JLabel("ⓒ 2025 JsCommunity Inc | 문의사항 : 202245059@itc.ac.kr & 202245061@itc.ac.kr");
        lblContact.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        footPanel.add(lblContact);
    }

    /**
     * 로그인 전 상태의 패널을 생성
     * 로그인 버튼과 회원가입 버튼을 포함
     * @return 로그인 전 상태를 나타내는 JPanel
     */
    private JPanel createBeforeLoginPanel() {
        JPanel panel = new JPanel(new MigLayout("align right", "[]5[]", "[]"));
        JButton loginBtn = new JButton();
        UIUtils.setupRoundedImageButton(
                loginBtn,
                "/로그인.png",
                "/로그인_명암.png",
                90, 30,    // 버튼 크기
                10,        // radius: 둥근 정도
                e -> {
                    LoginDialog dialog = new LoginDialog(this);
                    dialog.setVisible(true);
                    User user = dialog.getLoginUser();
                    this.currentUser = user; // 로그인된 사용자 정보 업데이트
                    if (user != null) {
                        System.out.println("로그인 시도: 이메일 = " + user.getEmail() + ", 역할 = " + user.getRole());

                        if ("USER".equalsIgnoreCase(user.getRole())) {
                            switchToAfterLogin(user.getName());
                            settingUI.updateUser(user);
                            for (PostPanel panel1 : postPanelMap.values()) {
                                panel1.setCurrentUser(user);
                            }

                            if (user.getLastPasswordChange() != null) {
                                long daysSinceChange = ChronoUnit.DAYS.between(
                                        user.getLastPasswordChange().toLocalDate(), LocalDate.now()
                                );
                                if (daysSinceChange >= 30) {
                                    new PWFindDialog(this, user.getEmail()).setVisible(true);
                                }
                            }
                        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                            System.out.println("관리자 로그인 감지. AdminUI를 시작합니다.");
                            AdminUI adminUI = new AdminUI(user, this);
                            adminUI.setVisible(true);
                            this.setVisible(false);
                        } else {
                            JOptionPane.showMessageDialog(this, "알 수 없는 사용자 역할입니다: " + user.getRole(), "로그인 오류", JOptionPane.ERROR_MESSAGE);
                            performLogout();
                        }
                    }
                }
        );

        JButton joinBtn = new JButton();

        UIUtils.setupRoundedImageButton(
                joinBtn,
                "/회원가입.png",
                "/회원가입_명암.png",
                90, 30,     // 버튼 크기
                10,         // radius
                e -> new JoinDialog(this).setVisible(true) // 액션 리스너 (회원가입 다이얼로그 실행)
        );

        panel.add(loginBtn);
        panel.add(joinBtn);
        return panel;
    }

    /**
     * 로그인 후 상태의 패널을 생성
     * 환영 메시지, 로그아웃 버튼, 설정 버튼을 포함
     * @return 로그인 후 상태를 나타내는 JPanel
     */
    private JPanel createAfterLoginPanel() {
        JPanel panel = new JPanel(new MigLayout("", "[]20[]20[]", "[]"));
        lblWelcome = new JLabel(" "); // 환영 메시지 레이블

        // GeneralButton을 사용한 로그아웃 버튼
        JButton logoutBtn = new GeneralButton("로그아웃", 80, 30, Color.DARK_GRAY, Color.WHITE);
        logoutBtn.addActionListener(e -> {
            performLogout(); // 로그인 전 UI로 전환
        });

        // GeneralButton을 사용한 설정 버튼
        JButton settingBtn = new GeneralButton("설정", 80, 30, Color.DARK_GRAY, Color.WHITE);
        settingBtn.addActionListener(e -> {
            if (currentUser != null) {
                settingUI.updateUser(currentUser); // 설정 화면에 현재 사용자 정보 업데이트
                previousBoardId = currentBoardId; // 설정 화면으로 가기 전 현재 게시판 ID 저장
                centerCardLayout.show(centerCardPanel, "setting"); // 설정 화면으로 전환
                currentBoardId = -3; // 설정 화면 진입 시 currentBoardId를 설정 화면용으로 변경(추적용 플래그)
            }
        });

        panel.add(lblWelcome);
        panel.add(logoutBtn);
        panel.add(settingBtn);
        return panel;
    }

    /**
     * 로그아웃을 수행하고 UI를 로그인 전 상태로 전환
     * 현재 사용자 정보를 초기화하고, 모든 PostPanel의 사용자 정보도 초기화
     */
    public void performLogout() {
        this.currentUser = null;
        previousBoardId = -1;
        currentPage = 0;

        // 사용자 정보 초기화
        switchToBeforeLogin();
        for (PostPanel panel : postPanelMap.values()) {
            panel.setCurrentUser(null);
        }

        // 게시판 결정
        if (borders != null && !borders.isEmpty()) {
            currentBoardId = borders.get(0).getId();
        } else {
            currentBoardId = -1;
        }

        SwingUtilities.invokeLater(() -> {
            if (currentBoardId != -1) {
                centerCardLayout.show(centerCardPanel, String.valueOf(currentBoardId));
            } else {
                centerCardLayout.show(centerCardPanel, "no_boards_user");
            }
            centerCardPanel.revalidate();
            centerCardPanel.repaint();
        });

        updateBoardPaginationUI();
    }


    /**
     * UI를 로그인 후 상태로 전환하고 환영 메시지를 설정
     * @param name 로그인한 사용자의 이름
     */
    private void switchToAfterLogin(String name) {
        lblWelcome.setText(name + "님 안녕하세요!"); // 환영 메시지 설정
        CardLayout cl = (CardLayout) loginPanel.getLayout();
        cl.show(loginPanel, "after"); // 'after' 카드 표시
    }

    /**
     * UI를 로그인 전 상태로 전환
     */
    private void switchToBeforeLogin() {
        CardLayout cl = (CardLayout) loginPanel.getLayout();
        cl.show(loginPanel, "before"); // 'before' 카드 표시
    }

    /**
     * 두꺼운 가로 구분선을 생성하여 반환
     * @return 생성된 JSeparator
     */
    private JSeparator createThickSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(Color.BLACK); // 구분선 색상 검정
        separator.setPreferredSize(new Dimension(1, 2)); // 높이 2픽셀로 설정
        return separator;
    }

    /**
     * 게시판 목록을 새로고침하고 UI를 업데이트
     * 새로운 게시판이 추가되거나 기존 게시판이 변경되었을 때 호출
     */
    public void refreshBoardPanels() {
        try {
            borders = BorderDAO.findAll(); // 최신 게시판 목록을 DB에서 다시 로드

            if (borders != null) {
                for (Border border : borders) {
                    // 새로 추가된 게시판이 있다면 PostPanel을 생성하여 맵과 카드 레이아웃에 추가
                    if (!postPanelMap.containsKey(border.getId())) {
                        PostPanel newPostPanel = new PostPanel(currentUser, border.getId());
                        postPanelMap.put(border.getId(), newPostPanel);
                        centerCardPanel.add(newPostPanel, String.valueOf(border.getId()));
                    }
                }
            }

            currentPage = 0; // 페이지네이션 초기화

            // currentBoardId를 초기화
            if (borders != null && !borders.isEmpty()) {
                currentBoardId = borders.get(0).getId();
                centerCardLayout.show(centerCardPanel, String.valueOf(currentBoardId));
            } else {
                currentBoardId = -1;
                centerCardLayout.show(centerCardPanel, "no_boards_user");
            }
            previousBoardId = -1; // 새로고침 시 previousBoardId도 초기화
            updateBoardPaginationUI(); // 게시판 페이지네이션 UI 업데이트
        } catch (Exception e) {
            System.err.println("게시판 새로고침 오류: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "게시판 새로고침 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 게시판 페이지네이션 UI (이전/다음 버튼 및 게시판 탭 버튼)를 업데이트
     * 현재 페이지에 해당하는 게시판 버튼들을 생성하고, 활성화/비활성화 상태를 관리
     */
    private void updateBoardPaginationUI() {
        boardButtonsPanel.removeAll(); // 기존 버튼 모두 제거
        currentSelectedTabButton = null; // 페이지 전환 시 이전 선택된 탭 초기화

        if (borders == null || borders.isEmpty()) {
            prevPageButton.setEnabled(false); // 게시판이 없으면 버튼 비활성화
            nextPageButton.setEnabled(false);
            boardButtonsPanel.revalidate(); // UI 갱신
            boardButtonsPanel.repaint();
            return;
        }

        int totalBoards = borders.size();
        int totalPages = (int) Math.ceil((double) totalBoards / boardsPerPage); // 총 페이지 수 계산

        int startIndex = currentPage * boardsPerPage; // 현재 페이지의 시작 인덱스
        int endIndex = Math.min(startIndex + boardsPerPage, totalBoards); // 현재 페이지의 끝 인덱스

        // 현재 페이지에 해당하는 게시판 버튼들 생성 및 추가
        for (int i = startIndex; i < endIndex; i++) {
            Border border = borders.get(i);
            int borderId = border.getId();
            String borderName = border.getName();

            TabButton btn = new TabButton(borderName); // TabButton 생성
            int finalBoardId = borderId;

            btn.addActionListener(e -> {
                if (currentSelectedTabButton != null) {
                    currentSelectedTabButton.setSelectedTab(false); // 이전 탭 선택 해제
                }
                btn.setSelectedTab(true); // 새 탭 선택
                currentSelectedTabButton = btn; // 현재 선택된 탭 업데이트

                centerCardLayout.show(centerCardPanel, String.valueOf(finalBoardId)); // 해당 게시판으로 전환
                currentBoardId = finalBoardId; // 탭 클릭 시 currentBoardId 업데이트
            });
            boardButtonsPanel.add(btn);

            // 현재 'currentBoardId'와 일치하는 버튼을 선택 상태로 설정
            if (finalBoardId == currentBoardId) {
                btn.setSelectedTab(true);
                currentSelectedTabButton = btn;
            }
        }

        // 만약 현재 페이지에 표시된 버튼 중 아무것도 선택되지 않았다면, 첫 번째 버튼을 선택
        if (currentSelectedTabButton == null && boardButtonsPanel.getComponentCount() > 0) {
            TabButton firstButtonOnPage = (TabButton) boardButtonsPanel.getComponent(0);
            firstButtonOnPage.setSelectedTab(true);
            currentSelectedTabButton = firstButtonOnPage;
        }

        prevPageButton.setEnabled(currentPage > 0); // 이전 페이지 버튼 활성화/비활성화
        nextPageButton.setEnabled(currentPage < totalPages - 1); // 다음 페이지 버튼 활성화/비활성화

        // UI 컴포넌트 갱신
        boardButtonsPanel.revalidate();
        boardButtonsPanel.repaint();
        menuBar.revalidate();
        menuBar.repaint();
        centerCardPanel.revalidate();
        centerCardPanel.repaint();
    }
}