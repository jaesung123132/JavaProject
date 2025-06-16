package jscommunity.mainform;

import jscommunity.Panel.PostPanel;
import jscommunity.db.BorderDAO;
import jscommunity.dbmember.Border;
import jscommunity.dbmember.User;
import jscommunity.utillity.UIUtils;
import jscommunity.utillity.TabButton;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 관리자 전용 UI 프레임
public class AdminUI extends JFrame {
    private User adminUser;
    private JscommunityUI parentUI;

    private CardLayout centerCardLayout;
    private JPanel centerCardPanel;
    private Map<Integer, PostPanel> postPanelMap;
    private List<Border> boardList;
    private JPanel menuBar;
    private JButton addBoardBtn;

    private JPanel boardManagementPanel;
    private TabButton currentSelectedTabButton = null;


    private int currentPage = 0;
    private final int boardsPerPage = 10; 
    private JButton prevPageButton;
    private JButton nextPageButton;


    private JPanel userManagementPanel;


    public AdminUI(User adminUser, JscommunityUI parentUI) {
        this.adminUser = adminUser;
        this.parentUI = parentUI;

        initializeUI();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("JsCommunity 관리자 패널");
        setSize(1800, 1000);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new MigLayout("fill", "[grow]", "[]0[grow]0[]"));
        getContentPane().setBackground(Color.WHITE);
        UIManager.put("Panel.background", Color.WHITE);

        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("Label.background", Color.WHITE);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("Button.background", Color.WHITE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(AdminUI.this,
                        "관리자 패널을 닫으시겠습니까? 로그아웃 됩니다.",
                        "확인", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    performLogout();
                }
            }
        });

        addHeader();
        getContentPane().add(createThickSeparator(), "growx, wrap");

        addCenter();
        getContentPane().add(createThickSeparator(), "growx, wrap");

        addFooter();
    }

    private void addHeader() {
        JPanel headerPanel = new JPanel(new MigLayout("insets 10, fillx", "[grow][][]", "[]"));
        headerPanel.setBackground(new Color(240, 240, 255));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        JLabel lblAdminWelcome = new JLabel(adminUser.getName() + " 관리자님 안녕하세요!");
        lblAdminWelcome.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        headerPanel.add(lblAdminWelcome, "growx, pushx");

        JButton logoutBtn = new JButton("로그아웃");
        logoutBtn.addActionListener(e -> performLogout());
        headerPanel.add(logoutBtn);

        JButton userManagementBtn = new JButton("유저 관리");
        userManagementBtn.addActionListener(e -> {
            if (currentSelectedTabButton != null) {
                currentSelectedTabButton.setSelectedTab(false); 
            }
            centerCardLayout.show(centerCardPanel, "UserManagement"); 
        });
        headerPanel.add(userManagementBtn);

        getContentPane().add(headerPanel, "north, wrap");
    }

    private void addCenter() {
        centerCardLayout = new CardLayout();
        centerCardPanel = new JPanel(centerCardLayout);
        postPanelMap = new HashMap<>();

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 20 20 20 20", "[grow]", "[60!][][grow]"));


        menuBar = new JPanel(new MigLayout("fillx", "[left][grow, center][right]", "[60!]"));

        addBoardBtn = new JButton("게시판 추가");
        addBoardBtn.setPreferredSize(new Dimension(160, 40));
        addBoardBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        addBoardBtn.addActionListener(e -> {
            JTextField boardNameField = new JTextField();
            JTextField isAnonymousField = new JTextField();

            UIUtils.setupTextField(boardNameField, "게시판 이름을 입력하세요", null, null);
            UIUtils.setupTextField(isAnonymousField, "익명이면 1, 실명이면 0을 입력하세요", null, null);

            JPanel inputPanel = new JPanel(new MigLayout("wrap 2", "[][grow]", "[]10[]"));
            inputPanel.add(new JLabel("게시판 이름:"), "");
            inputPanel.add(boardNameField, "growx, wrap");
            inputPanel.add(new JLabel("익명 여부 (0/1):"), "");
            inputPanel.add(isAnonymousField, "growx");

            int result = JOptionPane.showConfirmDialog(this, inputPanel,
                    "새 게시판 추가", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String boardName = boardNameField.getText().trim();
                String isAnonymousStr = isAnonymousField.getText().trim();

                if (boardName.isEmpty() || boardName.equals("게시판 이름을 입력하세요")) {
                    JOptionPane.showMessageDialog(this, "게시판 이름을 입력해야 합니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                boolean isAnonymous;
                try {
                    int anonymousValue = Integer.parseInt(isAnonymousStr);
                    if (anonymousValue == 0) {
                        isAnonymous = false;
                    } else if (anonymousValue == 1) {
                        isAnonymous = true;
                    } else {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "익명/실명 여부는 0 (실명) 또는 1 (익명)으로 입력해야 합니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    boolean success = BorderDAO.addBoard(boardName, isAnonymous);
                    if (success) {
                        JOptionPane.showMessageDialog(this, "'" + boardName + "' 게시판이 성공적으로 추가되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                        currentPage = 0; 
                        refreshBoardPanels();
                    } else {
                        JOptionPane.showMessageDialog(this, "게시판 추가에 실패했습니다. (DB 오류 또는 중복 이름)", "오류", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "게시판 추가 중 예상치 못한 오류 발생: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });
        menuBar.add(addBoardBtn, "cell 0 0, align left");


        JPanel paginationControlsPanel = new JPanel(new MigLayout("insets 0", "[]5[]5[]", "[]"));
        paginationControlsPanel.setOpaque(false);

        prevPageButton = new JButton("<");
        prevPageButton.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        prevPageButton.setPreferredSize(new Dimension(40, 30));
        prevPageButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshBoardPanels();
            }
        });
        paginationControlsPanel.add(prevPageButton);

        boardManagementPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0)); 
        boardManagementPanel.setOpaque(false);
        paginationControlsPanel.add(boardManagementPanel);

        nextPageButton = new JButton(">");
        nextPageButton.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        nextPageButton.setPreferredSize(new Dimension(40, 30));
        nextPageButton.addActionListener(e -> {
            int totalPages = (int) Math.ceil((double) boardList.size() / boardsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                refreshBoardPanels();
            }
        });
        paginationControlsPanel.add(nextPageButton);

        menuBar.add(paginationControlsPanel, "cell 1 0, growx, pushx, align center");


        refreshBoardPanels();


        userManagementPanel = new JPanel(new MigLayout("fill"));
        userManagementPanel.add(new JLabel("<html><center>여기에 사용자 목록 및 정지 기능이 표시됩니다.<br>추후 구현 예정.</center></html>", SwingConstants.CENTER), "grow, push");
        centerCardPanel.add(userManagementPanel, "UserManagement"); // Add User Management Panel to CardLayout


        mainPanel.add(menuBar, "wrap, align center");
        mainPanel.add(centerCardPanel, "grow, push, span");
        getContentPane().add(mainPanel, "grow, wrap");
    }

    private void addFooter() {
        JPanel footPanel = new JPanel(new MigLayout("center, insets 10"));
        footPanel.setBackground(new Color(230, 230, 230));
        JLabel lblContact = new JLabel("ⓒ 2025 JsCommunity Inc | 문의사항 : 202245059@itc.ac.kr & 202245061@itc.ac.kr");
        lblContact.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        footPanel.add(lblContact);
        getContentPane().add(footPanel, "south");
    }

    private void refreshBoardPanels() {
        boardManagementPanel.removeAll(); 
        postPanelMap.clear();
        Component[] components = centerCardPanel.getComponents();
        for (Component comp : components) {

            if (comp != userManagementPanel) {
                centerCardPanel.remove(comp);
            }
        }


        try {
            boardList = BorderDAO.findAll(); 

            if (boardList != null && !boardList.isEmpty()) {
                for (Border border : boardList) {
                    int borderId = border.getId();
                    PostPanel postPanel = new PostPanel(adminUser, borderId);
                    postPanelMap.put(borderId, postPanel);
                    centerCardPanel.add(postPanel, String.valueOf(borderId));
                }

                int totalPages = (int) Math.ceil((double) boardList.size() / boardsPerPage);

                 if (currentPage >= totalPages && totalPages > 0) {
                    currentPage = totalPages - 1;
                } else if (totalPages == 0) {
                    currentPage = 0;
                }

                int startIndex = currentPage * boardsPerPage;
                int endIndex = Math.min(startIndex + boardsPerPage, boardList.size());

                List<Border> boardsForCurrentPage = boardList.subList(startIndex, endIndex);

                TabButton firstButtonOnPage = null; 

                for (Border border : boardsForCurrentPage) {
                    int borderId = border.getId();
                    String borderName = border.getName();

                    JPanel boardButtonContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
                    boardButtonContainer.setOpaque(false);

                    TabButton boardTabBtn = new TabButton(borderName);
                    boardTabBtn.setPreferredSize(new Dimension(120, 35));
                    boardTabBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));

                    if (firstButtonOnPage == null) {
                        firstButtonOnPage = boardTabBtn;  
                    }

                    JButton deleteBoardBtn = new JButton("X");
                    deleteBoardBtn.setPreferredSize(new Dimension(30, 30));
                    deleteBoardBtn.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                    deleteBoardBtn.setForeground(Color.RED);
                    deleteBoardBtn.setMargin(new Insets(0, 0, 0, 0));
                    deleteBoardBtn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                    deleteBoardBtn.setBackground(new Color(255, 240, 240));
                    deleteBoardBtn.setOpaque(true);
                    deleteBoardBtn.setFocusPainted(false);

                    int finalBoardId = borderId;
                    String finalBoardName = borderName;

                    boardTabBtn.addActionListener(e -> {
                        if (currentSelectedTabButton != null) {
                            currentSelectedTabButton.setSelectedTab(false);
                        }
                        boardTabBtn.setSelectedTab(true);
                        currentSelectedTabButton = boardTabBtn;

                        centerCardLayout.show(centerCardPanel, String.valueOf(finalBoardId));
                    });

                    deleteBoardBtn.addActionListener(e -> {
                        int confirm = JOptionPane.showConfirmDialog(
                                this,
                                "'" + finalBoardName + "' 게시판을 정말 삭제하시겠습니까?\n(게시판 내의 모든 게시글과 댓글이 함께 삭제됩니다.)",
                                "게시판 삭제 확인",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        );

                        if (confirm == JOptionPane.YES_OPTION) {
                            boolean success = BorderDAO.deleteBoard(finalBoardId);
                            if (success) {
                                JOptionPane.showMessageDialog(this, "'" + finalBoardName + "' 게시판이 성공적으로 삭제되었습니다.", "성공", JOptionPane.INFORMATION_MESSAGE);
                                refreshBoardPanels(); // UI refresh
                            } else {
                                JOptionPane.showMessageDialog(this, "게시판 삭제에 실패했습니다. (DB 오류)", "오류", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });

                    boardButtonContainer.add(boardTabBtn);
                    boardButtonContainer.add(deleteBoardBtn);
                    boardManagementPanel.add(boardButtonContainer);
                }


                prevPageButton.setEnabled(currentPage > 0);
                nextPageButton.setEnabled(currentPage < totalPages - 1);


                if (!boardsForCurrentPage.isEmpty()) {
                    boolean foundSelected = false;

                    if (currentSelectedTabButton != null) {
                        for (Border b : boardsForCurrentPage) {
                            if (currentSelectedTabButton.getText().equals(b.getName())) {
                                currentSelectedTabButton.setSelectedTab(true);
                                centerCardLayout.show(centerCardPanel, String.valueOf(b.getId()));
                                foundSelected = true;
                                break;
                            }
                        }
                    }
                     if (!foundSelected && firstButtonOnPage != null) {
                        if (currentSelectedTabButton != null) {
                            currentSelectedTabButton.setSelectedTab(false); // Deselect the old one if it existed
                        }
                        firstButtonOnPage.setSelectedTab(true);
                        currentSelectedTabButton = firstButtonOnPage;
                        centerCardLayout.show(centerCardPanel, String.valueOf(boardsForCurrentPage.get(0).getId()));
                    }
                } else {
                   
                    if (currentSelectedTabButton != null) {
                        currentSelectedTabButton.setSelectedTab(false);
                        currentSelectedTabButton = null;
                    }
                    centerCardPanel.add(new JLabel("<html><center>관리할 게시판이 없습니다.<br>새 게시판을 추가하세요!</center></html>", SwingConstants.CENTER), "no_boards_empty");
                    centerCardLayout.show(centerCardPanel, "no_boards_empty");
                }


            } else { 
                currentSelectedTabButton = null;
                centerCardPanel.add(new JLabel("<html><center>관리할 게시판이 없습니다.<br>새 게시판을 추가하세요!</center></html>", SwingConstants.CENTER), "no_boards");
                centerCardLayout.show(centerCardPanel, "no_boards");
                prevPageButton.setEnabled(false);
                nextPageButton.setEnabled(false);
            }

            
            menuBar.revalidate();
            menuBar.repaint();
            boardManagementPanel.revalidate();
            boardManagementPanel.repaint();
            centerCardPanel.revalidate();
            centerCardPanel.repaint();

        } catch (Exception e) {
            System.err.println("게시판 새로고침 오류: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "게시판 새로고침 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            centerCardPanel.add(new JLabel("<html><center>게시판을 불러오는 데 실패했습니다.<br>잠시 후 다시 시도해주세요.</center></html>", SwingConstants.CENTER), "error_board_load");
            centerCardLayout.show(centerCardPanel, "error_board_load");
            prevPageButton.setEnabled(false);
            nextPageButton.setEnabled(false);

            menuBar.revalidate();
            menuBar.repaint();
            boardManagementPanel.revalidate();
            boardManagementPanel.repaint();
            centerCardPanel.revalidate();
            centerCardPanel.repaint();
        }
    }

    private JSeparator createThickSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(Color.BLACK);
        separator.setPreferredSize(new Dimension(1, 2));
        return separator;
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "로그아웃 하시겠습니까?",
                "로그아웃 확인",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            if (parentUI != null) {
                parentUI.refreshBoardPanels(); // Refresh JscommunityUI's board list
                parentUI.setVisible(true); // Show JscommunityUI again
            }
        }
    }
}