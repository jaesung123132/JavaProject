package jscommunity.utillity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.MultipleGradientPaint.CycleMethod; // 그라데이션을 위해 추가
import java.awt.RadialGradientPaint; // 그라데이션을 위해 추가
import java.awt.geom.RoundRectangle2D;

public class TabButton extends JButton {
    private boolean isSelected; // 이 탭이 현재 선택되었는지 여부

    private static final Color SELECTED_TOP_GRADIENT = new Color(225, 235, 250); // 선택된 탭 상단 그라데이션
    private static final Color SELECTED_BOTTOM_GRADIENT = new Color(190, 210, 240); // 선택된 탭 하단 그라데이션
    private static final Color UNSELECTED_TOP_GRADIENT = new Color(245, 245, 245); // 선택되지 않은 탭 상단 그라데이션
    private static final Color UNSELECTED_BOTTOM_GRADIENT = new Color(230, 230, 230); // 선택되지 않은 탭 하단 그라데이션

    private static final Color BORDER_COLOR_LIGHT = new Color(170, 190, 210, 150); // 밝은 테두리 (투명도 적용)
    private static final Color BORDER_COLOR_DARK = new Color(120, 140, 160, 150); // 어두운 테두리 (투명도 적용)

    private static final Color TEXT_COLOR_SELECTED = new Color(40, 40, 40); // 선택된 탭 텍스트 색상
    private static final Color TEXT_COLOR_UNSELECTED = new Color(80, 80, 80); // 선택되지 않은 탭 텍스트 색상

    private static final int ARC_SIZE = 16; // 둥근 모서리 크기 증가
    private static final int POP_OUT_OFFSET = 4; // 튀어나오는 느낌을 줄 오프셋 (픽셀) 증가
    private static final int SHADOW_SIZE = 5; // 그림자 크기
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 40); // 그림자 색상 (투명도)

    private boolean isHovered = false; // 마우스 오버 상태

    public TabButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false); // 투명하게 설정
        setFont(new Font("맑은 고딕", Font.BOLD, 14)); // 폰트 유지 (필요 시 더 세련된 폰트로 변경 가능)
        setForeground(TEXT_COLOR_UNSELECTED); // 초기 텍스트 색상 설정

        // 마우스 리스너 추가 (Hover 효과)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setSelectedTab(boolean selected) {
        this.isSelected = selected;
        // 선택 상태에 따라 텍스트 색상 변경
        setForeground(isSelected ? TEXT_COLOR_SELECTED : TEXT_COLOR_UNSELECTED);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY); // 렌더링 품질 향상

        int width = getWidth();
        int height = getHeight();
        int offsetY = 0; // 초기 Y 오프셋

        if (isSelected) {
            offsetY = POP_OUT_OFFSET; // 선택된 탭은 아래로 오프셋
        }

        // 탭 모양 경로 생성 (그림자 및 실제 탭 모양)
        Path2D path = new Path2D.Double();
        // 왼쪽 하단 (오프셋 적용)
        path.moveTo(0, height + offsetY);
        // 왼쪽 상단 둥근 모서리 (오프셋 적용)
        path.quadTo(0, 0 + offsetY, ARC_SIZE, 0 + offsetY);
        // 상단 가로선 (선택 여부에 따라 길이를 조절하여 살짝 겹치는 느낌, 오프셋 적용)
        if (isSelected) {
            path.lineTo(width - ARC_SIZE, 0 + offsetY);
        } else {
            // 선택되지 않은 탭은 약간 뒤로 물러선 느낌
            path.lineTo(width - ARC_SIZE - 5, 0 + offsetY);
        }
        // 오른쪽 상단 둥근 모서리 (오프셋 적용)
        path.quadTo(width, 0 + offsetY, width, ARC_SIZE + offsetY);
        // 오른쪽 하단 (오프셋 적용)
        path.lineTo(width, height + offsetY);
        path.closePath();


        // 그림자 그리기
        if (isSelected || isHovered) {
            g2.setColor(SHADOW_COLOR);
            g2.fill(new RoundRectangle2D.Double(
                    SHADOW_SIZE / 2,
                    height - SHADOW_SIZE / 2 + offsetY,
                    width - SHADOW_SIZE,
                    SHADOW_SIZE,
                    ARC_SIZE, ARC_SIZE
            ));
        }

        // 그라데이션 배경 채우기
        Paint oldPaint = g2.getPaint();
        if (isSelected) {
            g2.setPaint(new GradientPaint(
                    0, 0 + offsetY, SELECTED_TOP_GRADIENT,
                    0, height + offsetY, SELECTED_BOTTOM_GRADIENT
            ));
        } else {
            g2.setPaint(new GradientPaint(
                    0, 0 + offsetY, UNSELECTED_TOP_GRADIENT,
                    0, height + offsetY, UNSELECTED_BOTTOM_GRADIENT
            ));
        }
        g2.fill(path);
        g2.setPaint(oldPaint);

        // 테두리 그리기
        g2.setStroke(new BasicStroke(1.0f)); // 테두리 두께 1.0f
        if (isSelected) {
            g2.setColor(BORDER_COLOR_DARK); // 선택된 탭은 약간 어두운 테두리
            Path2D borderPath = new Path2D.Double();
            borderPath.moveTo(0, height + offsetY); // 왼쪽 아래
            borderPath.quadTo(0, 0 + offsetY, ARC_SIZE, 0 + offsetY); // 왼쪽 위 둥근 모서리
            borderPath.lineTo(width - ARC_SIZE, 0 + offsetY); // 위쪽 선
            borderPath.quadTo(width, 0 + offsetY, width, ARC_SIZE + offsetY); // 오른쪽 위 둥근 모서리
            borderPath.lineTo(width, height + offsetY); // 오른쪽 아래
            g2.draw(borderPath);
        } else {
            g2.setColor(BORDER_COLOR_LIGHT);
            g2.draw(path);
        }

        // 마우스 오버 시 하이라이트 효과
        if (isHovered && !isSelected) {
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fill(path);
        }

        // 텍스트 그리기 (가운데 정렬, 오프셋 적용)
        FontMetrics fm = g2.getFontMetrics();
        int stringWidth = fm.stringWidth(getText());
        int stringAscent = fm.getAscent();
        int x = (width - stringWidth) / 2;
        int y = (height - fm.getHeight()) / 2 + stringAscent + offsetY;
        g2.setColor(getForeground());
        g2.drawString(getText(), x, y);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(getText());
        int textHeight = fm.getHeight();
        return new Dimension(Math.max(120, textWidth + 40), textHeight + 25); // 패딩 증가
    }
}
