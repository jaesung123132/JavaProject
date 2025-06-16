package jscommunity.Panel;

import jscommunity.db.PostDAO;
import jscommunity.db.DB; // 게시판 이름을 가져오기 위해 DB 클래스 임포트
import jscommunity.dbmember.Post;
import jscommunity.dbmember.User;
import jscommunity.utillity.GeneralButton; // 사용자 정의 버튼 활용
import jscommunity.utillity.UIUtils; // UIUtils 활용
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*; // JTextPane 및 스타일링 관련 클래스들을 위해 임포트
import javax.swing.text.html.HTMLEditorKit; //  추가: HTML Editor Kit 임포트
import java.io.StringWriter; // ️ 추가: StringWriter 임포트
import java.io.IOException; //  추가: IOException 임포트

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
// InputMethodEvent 및 InputMethodListener 임포트 제거 (불필요한 코드 제거)
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays; // Arrays.asList 사용을 위해 추가
import java.util.List;
// AttributedCharacterIterator 임포트 제거 (불필요한 코드 제거)

public class Write extends JPanel {
    private int borderId;
    private User currentUser;
    private Runnable onBack;

    private JTextField titleField;
    private PlaceholderTextPane contentTextPane; //  JTextArea 대신 JTextPane 사용 (새로운 클래스 적용)
    private JLabel boardNameLabel;     //  게시판 이름 표시용
    private JLabel authorLabel;        //  작성자 표시용

    //  폰트 스타일링 컨트롤 컴포넌트들
    private JComboBox<Integer> fontSizeCombo;
    private JComboBox<String> fontFamilyCombo; // 추가: 글꼴 선택 콤보박스
    private JToggleButton boldButton;
    private JToggleButton italicButton;
    private JButton textColorButton;
    // private JButton backgroundColorButton; //  배경색 버튼 제거


    private static final String TITLE_PLACEHOLDER = "제목을 입력해주세요.";
    private static final String CONTENT_PLACEHOLDER = "여기에 게시물 내용을 입력하세요.";

    // 사진처럼
    private static final Color[] PREDEFINED_COLORS = {
            Color.BLACK, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY, Color.WHITE,
            Color.RED, new Color(255, 128, 0), Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA,
            new Color(128, 0, 0), new Color(128, 64, 0), new Color(128, 128, 0), new Color(0, 128, 0),
            new Color(0, 128, 128), new Color(0, 0, 128), new Color(128, 0, 128)
    };

    public Write(int borderId, User user, Runnable onBack) {
        this.borderId = borderId;
        this.currentUser = user;
        this.onBack = onBack;

        // MigLayout: 전체 화면 채우기, 여백 20, 컬럼 1개(grow), 행 간격 조정
        //  본문이 나머지 공간을 전부 차지하도록 행 제약 조건 조정
        //  각 행당 갭을 10으로 조정하여 공간 절약
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[]10[]10[]10[grow, push]10[]")); // 'wrap' 값 조정
        setBackground(Color.WHITE);

        // 1.  게시판 이름 (맨 위)
        boardNameLabel = new JLabel("게시판 이름 로딩 중..."); // 초기 텍스트
        boardNameLabel.setFont(new Font("SansSerif", Font.BOLD, 20)); //  폰트 변경
        boardNameLabel.setHorizontalAlignment(SwingConstants.CENTER); // 가운데 정렬
        loadBoardName(borderId); // DB에서 게시판 이름 로드
        add(boardNameLabel, "align center, wrap 10"); //  wrap 10으로 조정

        // 2.  작성자 정보
        authorLabel = new JLabel("작성자: " + (currentUser != null ? currentUser.getName() : "로그인 필요"));
        authorLabel.setFont(new Font("SansSerif", Font.PLAIN, 14)); // 폰트 변경

        // 3. 제목 입력 필드 (UIUtils.setupTextField 활용)
        titleField = new JTextField();
        titleField.setFont(new Font("SansSerif", Font.PLAIN, 14)); // 폰트 변경
        titleField.setMargin(new Insets(5, 5, 5, 5)); // 패딩
        // ️ UIUtils.setupTextField 적용
        UIUtils.setupTextField(titleField, TITLE_PLACEHOLDER, null, null);
        add(titleField, "split 2, growx");
        add(authorLabel, "align right, wrap 10");


        // 4.  폰트 스타일링 툴바
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5)); // 간격 조정
        toolbar.setBackground(new Color(240, 240, 240)); // 툴바 배경색
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 툴바 내부 여백

        // 글자 크기 JComboBox
        fontSizeCombo = new JComboBox<>(new Integer[]{10, 12, 14, 16, 18, 20, 24, 28, 32});
        fontSizeCombo.setSelectedItem(14); // 기본 크기 설정
        fontSizeCombo.setFont(new Font("SansSerif", Font.PLAIN, 12)); //  폰트 변경
        fontSizeCombo.setToolTipText("글자 크기");
        fontSizeCombo.addActionListener(e -> {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setFontSize(attrs, (Integer) fontSizeCombo.getSelectedItem());
            contentTextPane.setCharacterAttributes(attrs, false);
        });
        toolbar.add(fontSizeCombo);

        //  글꼴 선택 JComboBox
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] allFontNames = ge.getAvailableFontFamilyNames();
        List<String> availableFontNames = Arrays.asList(allFontNames); // 전체 시스템 폰트 리스트로 변환

        List<String> preferredFonts = new ArrayList<>();
        // 일반적으로 사용되는 논리적 폰트 추가 (OS 독립적)
        preferredFonts.add("SansSerif");
        preferredFonts.add("Serif");
        preferredFonts.add("Monospaced");
        preferredFonts.add("Dialog");
        preferredFonts.add("DialogInput");

        // 자주 사용되는 한국어 폰트 추가 (시스템에 존재할 경우)
        String[] koreanFonts = {"맑은 고딕", "Malgun Gothic", "나눔고딕", "NanumGothic", "굴림", "Dotum", "돋움", "Batang", "바탕", "궁서", "Gungsuh"};
        for (String fontName : koreanFonts) {
            if (availableFontNames.contains(fontName) && !preferredFonts.contains(fontName)) {
                preferredFonts.add(fontName);
            }
        }

        fontFamilyCombo = new JComboBox<>(preferredFonts.toArray(new String[0]));
        fontFamilyCombo.setSelectedItem("SansSerif"); // 기본 글꼴 설정
        fontFamilyCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fontFamilyCombo.setToolTipText("글꼴 선택");
        fontFamilyCombo.addActionListener(e -> {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setFontFamily(attrs, (String) fontFamilyCombo.getSelectedItem());
            contentTextPane.setCharacterAttributes(attrs, false);
        });
        toolbar.add(fontFamilyCombo);


        // 볼드 버튼 (JToggleButton 사용)
        boldButton = new JToggleButton("B");
        boldButton.setFont(new Font("SansSerif", Font.BOLD, 14)); // 폰트 변경
        boldButton.setToolTipText("볼드체");
        boldButton.addActionListener(e -> {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setBold(attrs, boldButton.isSelected());
            contentTextPane.setCharacterAttributes(attrs, false);
        });
        toolbar.add(boldButton);

        // 이탤릭 버튼 (JToggleButton 사용)
        italicButton = new JToggleButton("I");
        italicButton.setFont(new Font("SansSerif", Font.ITALIC, 14)); // 폰트 변경
        italicButton.setToolTipText("이탤릭체");
        italicButton.addActionListener(e -> {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setItalic(attrs, italicButton.isSelected());
            contentTextPane.setCharacterAttributes(attrs, false);
        });
        toolbar.add(italicButton);

        // 글자 색상 버튼 (커스텀 팔레트 사용)
        textColorButton = new JButton("글자색");
        textColorButton.setFont(new Font("SansSerif", Font.PLAIN, 12)); //  폰트 변경
        textColorButton.setToolTipText("글자 색상 변경");
        textColorButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JPopupMenu popup = new JPopupMenu();
                popup.setLayout(new BorderLayout()); // 중앙에 ColorPalettePanel 배치
                popup.add(createColorPalettePanel(contentTextPane), BorderLayout.CENTER); // isBackground 파라미터 제거
                popup.show(textColorButton, 0, textColorButton.getHeight());
            }
        });
        toolbar.add(textColorButton);

        add(toolbar, "growx, wrap 10"); // wrap 10으로 조정

        // 5. 본문 내용 입력 (JTextPane)
        contentTextPane = new PlaceholderTextPane(CONTENT_PLACEHOLDER); // placeholderTextPane 사용
        HTMLEditorKit htmlKit = new HTMLEditorKit(); // HTMLEditorKit 인스턴스 생성
        contentTextPane.setEditorKit(htmlKit); // JTextPane이 HTML을 처리하도록 EditorKit 설정
        // 중요: HTMLEditorKit이 생성하는 기본 문서 (HTMLDocument)를 사용
        contentTextPane.setDocument(htmlKit.createDefaultDocument()); // 수정된 코드

        contentTextPane.setFont(new Font("SansSerif", Font.PLAIN, 14)); // JTextPane 자체의 기본 폰트 설정


        contentTextPane.setMargin(new Insets(10, 10, 10, 10)); // 내부 여백 추가
        contentTextPane.setCursor(new Cursor(Cursor.TEXT_CURSOR)); // 커서 모양 변경

        JScrollPane contentScroll = new JScrollPane(contentTextPane);
        contentScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true)); // 둥근 테두리 (UIUtils의 RoundedLineBorder를 여기에 직접 적용하기는 어려움)

        //  내용 작성하는 곳이 나머지 공간을 전부 차지하도록 'grow, push' 사용
        add(contentScroll, "grow, push, wrap 15"); // 'grow, push'는 이미 되어있었고, wrap 15는 유지 또는 10으로 조정 가능. 여기서는 15 유지 (아래 버튼과의 최소 여백)

        // 6. 작성 완료, 뒤로가기 버튼
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);

        //  GeneralButton 활용
        GeneralButton submitBtn = new GeneralButton("작성 완료");
        submitBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            // JTextPane에서 플레이스홀더 텍스트 제거 후 실제 텍스트 가져오기
            String rawContent = contentTextPane.getCleanContent(); // 순수 텍스트 내용

            // JTextPane의 StyledDocument에서 HTML 내용 추출
            String htmlContent = "";
            if (!rawContent.isEmpty()) { // 텍스트가 있을 때만 HTML로 변환 시도
                HTMLEditorKit kit = (HTMLEditorKit) contentTextPane.getEditorKit(); // 여기서 contentTextPane의 kit을 사용
                StringWriter writer = new StringWriter();
                try {
                    // StyledDocument의 전체 내용을 HTML로 작성
                    kit.write(writer, contentTextPane.getDocument(), 0, contentTextPane.getDocument().getLength()); // getStyledDocument() -> getDocument()로 변경
                    htmlContent = writer.toString();
                    // 디버깅 메시지 추가 (여기서 HTML 내용을 확인)
                    System.out.println("DEBUG: Generated HTML Content for saving:\n" + htmlContent);
                } catch (IOException | BadLocationException ex) {
                    ex.printStackTrace();
                    System.err.println("게시글 HTML 변환 중 오류 발생: " + ex.getMessage());
                    // 오류 발생 시 순수 텍스트라도 저장하도록 폴백 (추가: 사용자에게 알림)
                    JOptionPane.showMessageDialog(this, "게시글 서식 변환에 실패했습니다. 일반 텍스트로 저장됩니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    htmlContent = rawContent;
                }
            }


            if (title.isEmpty() || title.equals(TITLE_PLACEHOLDER) || rawContent.isEmpty()) { // rawContent.isEmpty()로 변경
                JOptionPane.showMessageDialog(this, "제목과 내용을 모두 입력해주세요.");
                return;
            }
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "로그인이 필요합니다.");
                return;
            }

            Post post = new Post();
            post.setTitle(title);
            post.setContent(htmlContent); // HTML 내용 저장
            post.setAuthor(currentUser.getName());
            post.setEmail(currentUser.getEmail());
            post.setBoardId(borderId);

            boolean success = PostDAO.insertPost(post);
            if (success) {
                JOptionPane.showMessageDialog(this, "게시글이 작성되었습니다.");
                onBack.run(); // 게시글 목록 화면으로 돌아가기
            } else {
                JOptionPane.showMessageDialog(this, "작성에 실패했습니다. 다시 시도해주세요.");
            }
        });

        // GeneralButton 활용
        GeneralButton cancelBtn = new GeneralButton("뒤로가기");
        cancelBtn.addActionListener(e -> onBack.run()); // 게시글 목록 화면으로 돌아가기

        buttonPanel.add(submitBtn);
        buttonPanel.add(cancelBtn);
        add(buttonPanel, "align right, wrap"); // 버튼 패널 오른쪽 정렬
    }

    /**
     * 색상 팔레트 패널을 생성합니다. (글자색만 적용)
     * @param targetTextPane 스타일을 적용할 JTextPane
     * @return 색상 팔레트 JPanel
     */
    private JPanel createColorPalettePanel(JTextPane targetTextPane) { // isBackground 파라미터 제거
        JPanel palettePanel = new JPanel(new GridLayout(3, 7, 2, 2)); // 3행 7열 그리드, 간격 2px
        palettePanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        for (Color color : PREDEFINED_COLORS) {
            JLabel colorSwatch = new JLabel();
            colorSwatch.setOpaque(true);
            colorSwatch.setBackground(color);
            colorSwatch.setPreferredSize(new Dimension(25, 25)); // 각 색상 스와치 크기
            colorSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1)); // 테두리

            colorSwatch.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    MutableAttributeSet attrs = new SimpleAttributeSet();
                    StyleConstants.setForeground(attrs, color); // 항상 글자색만 변경
                    targetTextPane.setCharacterAttributes(attrs, false);
                    ((JPopupMenu) SwingUtilities.getAncestorOfClass(JPopupMenu.class, palettePanel)).setVisible(false); // 팝업 닫기
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    colorSwatch.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2)); // 호버 시 테두리 강조
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    colorSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1)); // 호버 해제 시 원복
                }
            });
            palettePanel.add(colorSwatch);
        }

        // "다른 색상 선택" 버튼 추가
        JButton otherColorButton = new JButton("다른 색상 선택");
        otherColorButton.setFont(new Font("SansSerif", Font.PLAIN, 12)); // 폰트 변경
        otherColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "색상 선택", targetTextPane.getForeground()); // 배경색 대신 글자색 기준으로 JColorChooser 열기
            if (newColor != null) {
                MutableAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, newColor); // 항상 글자색만 변경
                targetTextPane.setCharacterAttributes(attrs, false);
            }
            ((JPopupMenu) SwingUtilities.getAncestorOfClass(JPopupMenu.class, palettePanel)).setVisible(false); // 팝업 닫기
        });

        // 투명으로 설정/검은색으로 설정 버튼 추가 (글자색에 맞춰 "검은색으로 설정"만 남김)
        JButton blackColorButton = new JButton("검은색으로 설정");
        blackColorButton.setFont(new Font("SansSerif", Font.PLAIN, 12)); //  폰트 변경
        blackColorButton.addActionListener(e -> {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, Color.BLACK);
            targetTextPane.setCharacterAttributes(attrs, false);
            ((JPopupMenu) SwingUtilities.getAncestorOfClass(JPopupMenu.class, palettePanel)).setVisible(false); // 팝업 닫기
        });

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0)); // 버튼 2개를 위한 그리드
        buttonPanel.add(blackColorButton); // 투명 버튼 대신 검은색 버튼
        buttonPanel.add(otherColorButton);

        JPanel fullPalettePanel = new JPanel(new BorderLayout());
        fullPalettePanel.add(palettePanel, BorderLayout.CENTER);
        fullPalettePanel.add(buttonPanel, BorderLayout.SOUTH);

        return fullPalettePanel;
    }


    /**
     * DB에서 게시판 ID에 해당하는 이름을 로드하여 boardNameLabel에 설정합니다.
     * @param boardId 현재 게시판의 ID
     */
    private void loadBoardName(int boardId) {
        String boardName = "알 수 없는 게시판";
        String sql = "SELECT name FROM board WHERE id = ?";
        ResultSet rs = null;
        try {
            rs = DB.executeQuery(sql, boardId);
            if (rs != null && rs.next()) {
                boardName = rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("게시판 이름 로드 중 오류 발생: " + e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            // DB 연결은 DB 클래스에서 관리하므로 여기서 닫지 않습니다.
        }
        boardNameLabel.setText("📝 게시판: " + boardName); // "게시판: [이름]" 형태로 표시
    }

    //  새로운 내부 클래스: PlaceholderTextPane
    private static class PlaceholderTextPane extends JTextPane {
        private String placeholderText;

        public PlaceholderTextPane(String placeholderText) {
            this.placeholderText = placeholderText;
            // 문서 리스너를 추가하여 텍스트 변경 시 repaint 호출
            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    // 실시간 스타일 반영 로직 제거 (현재는 드래그 후 스타일 적용을 기본으로 합니다.)
                    repaint(); // 변경 사항을 즉시 반영하기 위해 repaint 호출
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    repaint();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    // 속성 변경 시 (폰트 스타일 등)
                    repaint();
                }
            });
            // 포커스 리스너를 추가하여 포커스 변경 시 플레이스홀더 업데이트
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) { repaint(); }
                @Override
                public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 문서가 비어 있고, 포커스를 가지고 있지 않을 때만 플레이스홀더를 그립니다.
            if (getDocument().getLength() == 0 && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.LIGHT_GRAY); // 플레이스홀더 색상
                g2.setFont(getFont()); // JTextPane의 현재 폰트 사용

                Insets insets = getInsets();
                int x = insets.left;
                int y = insets.top + g2.getFontMetrics().getAscent();

                g2.drawString(placeholderText, x, y);
                g2.dispose();
            }
        }

        /**
         * JTextPane에서 플레이스홀더 텍스트를 제외한 실제 내용을 반환합니다.
         * 이 메서드는 이제 문서의 실제 길이만 확인합니다.
         * @return 플레이스홀더가 아닌 실제 텍스트 내용
         */
        public String getCleanContent() {
            if (getDocument().getLength() == 0) {
                return ""; // 문서가 비어 있으면 빈 문자열 반환
            }
            try {
                // JTextPane의 getText()는 HTML이 아닌 순수 텍스트를 반환합니다.
                // 이 메서드는 플레이스홀더 체크용으로만 사용되므로 그대로 유지합니다.
                return getDocument().getText(0, getDocument().getLength()).trim();
            } catch (BadLocationException e) {
                e.printStackTrace();
                return "";
            }
        }
    }
}
