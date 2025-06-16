package jscommunity.utillity;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 프로젝트 전반에 걸쳐 사용되는 사용자 정의 JButton 클래스입니다.
 * 둥근 모서리, 사용자 정의 가능한 색상, 호버 및 클릭 효과를 제공합니다.
 */
public class GeneralButton extends JButton {

    // 기본 색상 정의: 사용자 요청에 맞춘 더 진한 파란색 팔레트
    private static final Color DEFAULT_BLUE = new Color(25, 100, 230);   // 기본 파란색
    private static final Color HOVER_BLUE = new Color(45, 120, 250);     // 호버 시 약간 밝아진 파란색
    private static final Color PRESSED_BLUE = new Color(10, 80, 200);   // 클릭 시 더 어두운 파란색
    private static final Color DEFAULT_TEXT_COLOR = Color.WHITE;         // 기본 텍스트 색상 (흰색)

    // 비활성화 상태 색상
    private static final Color DISABLED_BG_COLOR = new Color(220, 220, 220); // 비활성화 시 배경색 (옅은 회색)
    private static final Color DISABLED_BORDER_COLOR = new Color(180, 180, 180); // 비활성화 시 테두리색
    private static final Color DISABLED_TEXT_COLOR = new Color(120, 120, 120); // 비활성화 시 텍스트색

    // 테두리 속성 정의
    private static final int BORDER_THICKNESS = 1; // 테두리 두께

    // 인스턴스별 속성
    private int explicitWidth = -1; // 명시적으로 설정된 버튼 너비
    private int explicitHeight = -1; // 명시적으로 설정된 버튼 높이
    private int instanceBorderRadius; // 인스턴스별 둥근 모서리 크기
    private ActionListener buttonListener; // 버튼에 연결될 ActionListener

    // 추가: 사용자 정의 배경색 및 텍스트 색상
    private Color customBackgroundColor = null; // 사용자가 설정한 배경색
    private Color customTextColor = null;       // 사용자가 설정한 텍스트 색상


    public GeneralButton(String text) {
        this(text, -1, -1, 25, null, null, null);
    }

    public GeneralButton(String text, int width, int height) {
        this(text, width, height, 25, null, null, null);
    }

    public GeneralButton(String text, int width, int height, Color backgroundColor, Color textColor) {
        this(text, width, height, 25, null, backgroundColor, textColor);
    }

    public GeneralButton(String text, int width, int height, ActionListener listener) {
        this(text, width, height, 25, listener, null, null);
    }

    public GeneralButton(String text, int width, int height, int boderRadius, ActionListener listener) {
        this(text, width, height, boderRadius, listener, null, null);
    }

    /**
     * 모든 매개변수를 받는 최종 생성자
     * @param text 버튼에 표시될 텍스트
     * @param width 버튼의 너비 (-1이면 기본 크기 계산)
     * @param height 버튼의 높이 (-1이면 기본 크기 계산)
     * @param borderRadius 버튼 모서리의 둥근 정도 (픽셀 단위)
     * @param listener 버튼 클릭 시 실행될 ActionListener
     * @param backgroundColor 사용자가 지정할 배경색 (null이면 기본 파란색 사용)
     * @param textColor 사용자가 지정할 텍스트 색상 (null이면 기본 흰색 사용)
     */
    public GeneralButton(String text, int width, int height, int borderRadius, ActionListener listener, Color backgroundColor, Color textColor) {
        super(text); // JButton의 텍스트 설정
        this.explicitWidth = width; // 명시적 너비 저장
        this.explicitHeight = height; // 명시적 높이 저장
        this.instanceBorderRadius = borderRadius; // 둥근 모서리 크기 저장
        this.buttonListener = listener; // 액션 리스너 저장
        this.customBackgroundColor = backgroundColor; // 사용자 정의 배경색 저장
        this.customTextColor = textColor; // 사용자 정의 텍스트 색상 저장
        initStyle(); // 버튼 스타일 초기화
    }

    /**
     * 버튼의 기본 스타일을 초기화합니다.
     */
    private void initStyle() {
        setContentAreaFilled(false); // 배경을 직접 그릴 것이므로 기본 배경 채우기 비활성화
        setFocusPainted(false); // 포커스 테두리 그리기 비활성화
        setBorderPainted(false); // 기본 테두리 그리기 비활성화
        setOpaque(false); // 패널을 투명하게 설정 (paintComponent에서 직접 채우기)

        setFont(new Font("맑은 고딕", Font.BOLD, 14)); // 기본 폰트 설정
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 마우스 오버 시 손가락 커서로 변경

        // 액션 리스너가 제공된 경우에만 추가
        if (buttonListener != null) {
            addActionListener(buttonListener);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); // 그래픽 컨텍스트 복사
        // 렌더링 힌트 설정: 안티앨리어싱(부드러운 선), 고품질 렌더링
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth(); // 버튼의 현재 너비
        int height = getHeight(); // 버튼의 현재 높이

        Color currentBgColor; // 현재 배경색
        Color currentBorderColor; // 현재 테두리색
        Color currentTextColor; // 현재 텍스트색

        // 버튼 상태에 따른 색상 결정
        if (!isEnabled()) { // 버튼이 비활성화 상태일 때
            currentBgColor = DISABLED_BG_COLOR;
            currentBorderColor = DISABLED_BORDER_COLOR;
            currentTextColor = DISABLED_TEXT_COLOR;
        } else {
            // 사용자 정의 배경색이 설정되어 있다면 해당 색상을 기본으로 사용
            Color baseColor = (customBackgroundColor != null) ? customBackgroundColor : DEFAULT_BLUE;

            if (getModel().isPressed()) { // 버튼이 눌렸을 때
                currentBgColor = baseColor.darker().darker(); // 더 어둡게
                currentBorderColor = currentBgColor; // 배경색과 동일하게
                currentTextColor = (customTextColor != null) ? customTextColor : DEFAULT_TEXT_COLOR;
            } else if (getModel().isRollover()) { // 마우스 오버(호버) 상태일 때
                currentBgColor = baseColor.brighter(); // 약간 밝게
                currentBorderColor = currentBgColor; // 배경색과 동일하게
                currentTextColor = (customTextColor != null) ? customTextColor : DEFAULT_TEXT_COLOR;
            } else { // 기본 상태일 때
                currentBgColor = baseColor;
                // 기본 파란색일 경우에만 테두리색을 다르게 설정, 아니면 배경색과 동일하게
                currentBorderColor = (customBackgroundColor == null) ? new Color(100, 160, 255) : baseColor;
                currentTextColor = (customTextColor != null) ? customTextColor : DEFAULT_TEXT_COLOR;
            }
        }

        // 1. 배경 그리기 (둥근 사각형)
        g2.setColor(currentBgColor);
        g2.fillRoundRect(0, 0, width - 1, height - 1, instanceBorderRadius, instanceBorderRadius);

        // 2. 테두리 그리기 (둥근 사각형)
        g2.setColor(currentBorderColor);
        g2.setStroke(new BasicStroke(BORDER_THICKNESS)); // 테두리 두께 설정
        g2.drawRoundRect(0, 0, width - 1, height - 1, instanceBorderRadius, instanceBorderRadius);

        // 3. 텍스트 그리기 (가운데 정렬)
        FontMetrics fm = g2.getFontMetrics(); // 폰트 메트릭스
        int stringWidth = fm.stringWidth(getText()); // 텍스트 너비
        int stringAscent = fm.getAscent(); // 텍스트 베이스라인부터 최상단까지의 높이
        int x = (width - stringWidth) / 2; // 가로 중앙 정렬
        int y = (height - fm.getHeight()) / 2 + stringAscent; // 세로 중앙 정렬
        g2.setColor(currentTextColor); // 텍스트 색상 설정
        g2.drawString(getText(), x, y); // 텍스트 그리기

        g2.dispose(); // Graphics2D 자원 해제
    }

    @Override
    public Dimension getPreferredSize() {
        // 사용자가 명시적으로 너비와 높이를 설정했다면 해당 값 반환
        if (explicitWidth != -1 && explicitHeight != -1) {
            return new Dimension(explicitWidth, explicitHeight);
        }

        // 명시적 크기가 없을 경우 텍스트 기반으로 선호 크기 계산
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(getText());
        int textHeight = fm.getHeight();
        // 텍스트에 패딩을 더해서 버튼 크기를 적절하게 설정
        return new Dimension(Math.max(120, textWidth + 40), textHeight + 25);
    }
}