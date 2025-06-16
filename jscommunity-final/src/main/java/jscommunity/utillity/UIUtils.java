package jscommunity.utillity;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener; // ChangeListener는 GeneralButton에만 필요하므로, UIUtils에서는 필요없을 수 있습니다. 하지만 현재 코드에는 있어 유지합니다.
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

/**
 * UIUtils 클래스는 프로젝트 전반에서 공통적으로 사용되는
 * 사용자 인터페이스 유틸리티 기능들을 제공합니다.
 * 예: 둥근 테두리, placeholder 처리, 비밀번호 보기 토글 등
 */
public class UIUtils {

    /**
     * 둥근 테두리를 그려주는 커스텀 Border 클래스입니다.
     */
    public static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            for (int i = 0; i < thickness; i++) {
                g2.drawRoundRect(x + i, y + i, width - i * 2 - 1, height - i * 2 - 1, radius, radius);
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(thickness, thickness, thickness, thickness);
            return insets;
        }
    }

    // ===== 공통으로 사용하는 테두리(Border) 정의 =====

    /** 일반 상태의 입력 필드 테두리 */
    private static final CompoundBorder normalBorder = BorderFactory.createCompoundBorder(
            new RoundedLineBorder(Color.LIGHT_GRAY, 1, 30),
            BorderFactory.createEmptyBorder(8, 11, 8, 11)
    );

    /** 입력 중(active) 상태의 테두리 */
    private static final CompoundBorder insertBorder = BorderFactory.createCompoundBorder(
            new RoundedLineBorder(Color.BLUE, 1, 30),
            BorderFactory.createEmptyBorder(8, 11, 8, 11)
    );

    /** 에러 발생 시 테두리 */
    private static final CompoundBorder errorBorder = BorderFactory.createCompoundBorder(
            new RoundedLineBorder(Color.RED, 1, 30),
            BorderFactory.createEmptyBorder(8, 11, 8, 11)
    );

    /** Toss 스타일 버튼용 둥근 테두리 및 패딩 */
    private static final CompoundBorder tossButtonBorder = BorderFactory.createCompoundBorder(
            new RoundedLineBorder(new Color(200, 220, 255), 1, 25),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
    );


    /** @return 일반 상태 테두리 반환 */
    public static Border getNormalBorder() {
        return normalBorder;
    }

    /** @return 입력 중 테두리 반환 */
    public static Border getInsertBorder() {
        return insertBorder;
    }

    /** @return 에러 상태 테두리 반환 */
    public static Border getErrorBorder() {
        return errorBorder;
    }

    /**
     * 일반 텍스트 필드에 placeholder, border, 입력 감지 기능을 설정
     *
     * @param field       설정할 JTextField
     * @param placeholder placeholder 텍스트
     * @param errorLabel  에러 메시지용 JLabel (null 가능)
     * @param onChange    텍스트 변경 시 실행할 콜백 함수 (null 가능)
     */
    public static void setupTextField(JTextField field, String placeholder, JLabel errorLabel, Runnable onChange) {
        // 초기 상태 설정
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.setBorder(getNormalBorder());

        // 포커스 리스너: 입력 시 스타일 변경 및 placeholder 제거
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(getInsertBorder());
                if (errorLabel != null) errorLabel.setVisible(false);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        field.setText(placeholder);
                        field.setForeground(Color.GRAY);
                        field.setCaretPosition(0);
                    });
                }
                field.setBorder(getNormalBorder());
            }
        });

        // 키 입력 처리
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    field.setText(String.valueOf(e.getKeyChar()));
                    e.consume();
                    if (onChange != null) onChange.run();
                }
            }
        });

        // 입력 변경 감지 리스너
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void check() {
                SwingUtilities.invokeLater(() -> {
                    String text = field.getText().trim();
                    if (text.isEmpty() && !field.getText().equals(placeholder)) {
                        field.setText(placeholder);
                        field.setForeground(Color.LIGHT_GRAY);
                        field.setCaretPosition(0);
                        field.setBorder(getInsertBorder());
                        if (errorLabel != null) errorLabel.setVisible(false);
                        if (onChange != null) onChange.run();
                    }
                });
            }
            @Override public void insertUpdate(DocumentEvent e) { check(); }
            @Override public void removeUpdate(DocumentEvent e) { check(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });

        SwingUtilities.invokeLater(() -> field.setCaretPosition(0));
    }

    /**
     * 비밀번호 필드에 placeholder, 보기 토글 버튼, 스타일을 설정
     *
     * @param passwordField  JPasswordField 객체
     * @param toggleCheckBox 비밀번호 보기 토글용 체크박스
     * @param placeholder    placeholder 텍스트
     * @param onChange       입력 감지 시 실행할 콜백 (null 가능)
     */
    public static void setupPasswordField(JPasswordField passwordField, JCheckBox toggleCheckBox, String placeholder, Runnable onChange) {
        char defaultEchoChar = passwordField.getEchoChar();

        passwordField.setText(placeholder);
        passwordField.setForeground(Color.GRAY);
        passwordField.setEchoChar((placeholder != null && !placeholder.isEmpty()) ? (char) 0 : defaultEchoChar);
        passwordField.setBorder(getNormalBorder());

        // 아이콘 설정
        ImageIcon eyeClosedIcon = new ImageIcon(UIUtils.class.getResource("/비밀번호보기_취소.png"));
        ImageIcon eyeOpenIcon = new ImageIcon(UIUtils.class.getResource("/비밀번호보기.png"));
        int iconSize = 20;

        toggleCheckBox.setIcon(new ImageIcon(eyeClosedIcon.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)));
        toggleCheckBox.setSelectedIcon(new ImageIcon(eyeOpenIcon.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)));

        toggleCheckBox.setBorderPainted(false);
        toggleCheckBox.setContentAreaFilled(false);
        toggleCheckBox.setFocusPainted(false);
        toggleCheckBox.setOpaque(false);
        toggleCheckBox.setPreferredSize(new Dimension(24, 24));

        // 보기 토글
        toggleCheckBox.addActionListener(e -> {
            if (!String.valueOf(passwordField.getPassword()).equals(placeholder)) {
                passwordField.setEchoChar(toggleCheckBox.isSelected() ? (char) 0 : defaultEchoChar);
            }
        });

        // 포커스 처리
        passwordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                passwordField.setBorder(getInsertBorder());
                if (String.valueOf(passwordField.getPassword()).equals(placeholder)) {
                    passwordField.setCaretPosition(0);
                }
            }
            public void focusLost(FocusEvent e) {
                passwordField.setBorder(getNormalBorder());
                if (String.valueOf(passwordField.getPassword()).isEmpty()) {
                    passwordField.setText(placeholder);
                    passwordField.setForeground(Color.GRAY);
                    passwordField.setEchoChar((char) 0);
                    toggleCheckBox.setSelected(false);
                }
            }
        });

        // 입력 이벤트
        passwordField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (String.valueOf(passwordField.getPassword()).equals(placeholder)) {
                    passwordField.setText("");
                    passwordField.setForeground(Color.BLACK);
                    passwordField.setEchoChar(toggleCheckBox.isSelected() ? (char) 0 : defaultEchoChar);
                }
                if (onChange != null) SwingUtilities.invokeLater(onChange);
            }
        });

        // 빈값일 경우 placeholder 복구
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            private void check() {
                SwingUtilities.invokeLater(() -> {
                    String text = String.valueOf(passwordField.getPassword()).trim();
                    if (text.isEmpty() && !passwordField.getText().equals(placeholder)) {
                        passwordField.setText(placeholder);
                        passwordField.setForeground(Color.GRAY);
                        passwordField.setCaretPosition(0);
                        passwordField.setEchoChar((char) 0);
                        toggleCheckBox.setSelected(false);
                    }
                });
            }

            public void insertUpdate(DocumentEvent e) { check(); }
            public void removeUpdate(DocumentEvent e) { check(); }
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    /**
     * 이미지 기반 둥근 버튼을 설정
     *
     * @param button         설정할 버튼
     * @param defaultImagePath 기본 이미지 경로
     * @param hoverImagePath   호버 시 이미지 경로
     * @param width          이미지 너비
     * @param height         이미지 높이
     * @param radius         테두리 둥근 정도
     * @param listener       버튼 클릭 시 리스너
     */
    public static void setupRoundedImageButton(JButton button, String defaultImagePath, String hoverImagePath, int width, int height, int radius, ActionListener listener) {
        ImageIcon defaultIcon = createResizedIcon(defaultImagePath, width, height);
        ImageIcon hoverIcon = createResizedIcon(hoverImagePath, width, height);

        button.setIcon(defaultIcon);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(width, height));
        button.setBorder(new RoundedLineBorder(Color.LIGHT_GRAY, 1, radius));

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setIcon(hoverIcon); }
            @Override public void mouseExited(MouseEvent e) { button.setIcon(defaultIcon); }
        });

        if (listener != null) button.addActionListener(listener);
    }

    /**
     * 이미지 경로에서 지정 크기로 아이콘을 로드
     *
     * @param imagePath 리소스 경로
     * @param width     너비
     * @param height    높이
     * @return 리사이즈된 ImageIcon 객체
     */
    public static ImageIcon createResizedIcon(String imagePath, int width, int height) {
        URL resource = UIUtils.class.getResource(imagePath);
        if (resource == null) {
            System.err.println("이미지 경로 오류: " + imagePath);
            return null;
        }
        ImageIcon rawIcon = new ImageIcon(resource);
        Image scaled = rawIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}

