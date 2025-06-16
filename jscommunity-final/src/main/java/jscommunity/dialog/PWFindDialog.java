package jscommunity.dialog;

import jscommunity.db.DB;
import jscommunity.utillity.UIUtils;
import jscommunity.utillity.GeneralButton;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PWFindDialog extends JDialog {
    // UI 컴포넌트
    private JLabel titleLabel;
    private JLabel passwordMatchLabel;
    private JPasswordField tfCurrentPw;
    private JPasswordField tfNewPw;
    private JPasswordField tfConfirmPw;
    private GeneralButton checkButton;
    private GeneralButton changeButton;
    private CardLayout centerCardLayout;
    private JPanel cardPanel;

    private JCheckBox showCurrentPasswordCheck;
    private JCheckBox showNewPasswordCheck;
    private JCheckBox showConfirmPasswordCheck;

    // 생성자 인자
    private String userEmail;

    public PWFindDialog(JFrame parent, String email) {
        super(parent, "비밀번호 변경", true);
        this.userEmail = email;

        //다이얼로그 기본 설정
        setSize(550, 350);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
        setLayout(new MigLayout("fill, insets 20, align center", "[center, grow]", "[]10[grow]"));
        setBackground(Color.WHITE);

        //타이틀 설정
        titleLabel = new JLabel("비밀번호 변경");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        add(titleLabel, "wrap, align center, gapbottom 15");

        //메인 카드 레이아웃 패널 설정
        centerCardLayout = new CardLayout();
        cardPanel = new JPanel(centerCardLayout);
        add(cardPanel, "grow, wrap");

        //현재 비밀번호 확인 패널 (checkPanel) 생성 및 구성
        JPanel checkPanel = createCurrentPasswordCheckPanel();
        cardPanel.add(checkPanel, "check");

        //새로운 비밀번호 변경 패널 (changePanel) 생성 및 구성
        JPanel changePanel = createNewPasswordChangePanel();
        cardPanel.add(changePanel, "change");

        //초기 화면 설정: 현재 비밀번호 확인 패널을 먼저 보여줌
        centerCardLayout.show(cardPanel, "check");
    }

    /**
     * 현재 비밀번호 확인 패널을 생성하고 구성
     * @return 현재 비밀번호 확인을 위한 JPanel
     */
    private JPanel createCurrentPasswordCheckPanel() {
        JPanel checkPanel = new JPanel(new MigLayout("align center, insets 20", "[grow, center]", "[]10[]"));
        checkPanel.setOpaque(false);

        // 현재 비밀번호 입력 필드
        tfCurrentPw = new JPasswordField(20);
        // 현재 비밀번호 보기 체크박스 초기화
        showCurrentPasswordCheck = new JCheckBox();
        // UIUtils.setupPasswordField 호출 시 showCurrentPasswordCheck 전달
        UIUtils.setupPasswordField(tfCurrentPw, showCurrentPasswordCheck, "현재 비밀번호 입력", null);

        // 확인 버튼 액션 리스너 정의
        ActionListener checkButtonListener = e -> {
            String enteredPassword = new String(tfCurrentPw.getPassword());

            if (enteredPassword.equals("현재 비밀번호 입력") || enteredPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "현재 비밀번호를 입력해주세요.");
                tfCurrentPw.requestFocusInWindow();
                tfCurrentPw.setBorder(UIUtils.getErrorBorder());
                return;
            }

            String sql = "SELECT password FROM user WHERE email = ?";
            try (ResultSet rs = DB.executeQuery(sql, userEmail)) {
                if (rs != null && rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (enteredPassword.equals(storedPassword)) {
                        getRootPane().setDefaultButton(changeButton);
                        centerCardLayout.show(cardPanel, "change");
                        showNewPasswordCheck = new JCheckBox();
                        showConfirmPasswordCheck = new JCheckBox();

                        UIUtils.setupPasswordField(tfNewPw, showNewPasswordCheck, "새로운 비밀번호 입력", this::checkPasswordMatch);
                        UIUtils.setupPasswordField(tfConfirmPw, showConfirmPasswordCheck, "비밀번호 확인", this::checkPasswordMatch);

                        passwordMatchLabel.setText(" ");
                        passwordMatchLabel.setVisible(false);
                        SwingUtilities.invokeLater(tfNewPw::requestFocusInWindow);
                    } else {
                        JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.");
                        tfCurrentPw.setBorder(UIUtils.getErrorBorder());
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "사용자 정보를 찾을 수 없습니다.");
                    tfCurrentPw.setBorder(UIUtils.getErrorBorder());
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "데이터베이스 오류가 발생했습니다: " + ex.getMessage());
                tfCurrentPw.setBorder(UIUtils.getErrorBorder());
            }
        };

        checkButton = new GeneralButton("확인", 80, 30, checkButtonListener);
        getRootPane().setDefaultButton(checkButton);

        checkPanel.add(tfCurrentPw, "width 300!, height 35!, split 2");
        checkPanel.add(showCurrentPasswordCheck, "wrap 30");
        checkPanel.add(checkButton, "width 80!, height 30!, align center");

        return checkPanel;
    }

    /**
     * 새로운 비밀번호 변경 패널을 생성하고 구성
     * @return 새로운 비밀번호 설정을 위한 JPanel
     */
    private JPanel createNewPasswordChangePanel() {
        JPanel changePanel = new JPanel(new MigLayout("wrap 2, align center, insets 20", "[grow, center]", "[]10[]10[]"));
        changePanel.setOpaque(false);

        // 새로운 비밀번호 입력 필드
        tfNewPw = new JPasswordField(20);

        // 새로운 비밀번호 확인 필드
        tfConfirmPw = new JPasswordField(20);

        // 비밀번호 일치 메시지 라벨
        passwordMatchLabel = new JLabel(" ");
        passwordMatchLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        passwordMatchLabel.setVisible(false);

        // 변경 버튼 액션 리스너 정의
        ActionListener changeButtonListener = e -> {
            String newPassword = new String(tfNewPw.getPassword());
            String confirmPassword = new String(tfConfirmPw.getPassword());

            // UIUtils의 플레이스홀더 텍스트와 비교하여 실제 입력이 있는지 확인
            if (newPassword.equals("새로운 비밀번호 입력") || newPassword.isEmpty() ||
                    confirmPassword.equals("비밀번호 확인") || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "새로운 비밀번호를 정확히 입력해주세요.");
                tfNewPw.requestFocusInWindow();
                tfNewPw.setBorder(UIUtils.getErrorBorder());
                tfConfirmPw.setBorder(UIUtils.getErrorBorder());
                return;
            }

            String sqlCheckCurrent = "SELECT password FROM user WHERE email = ?";
            String currentPasswordFromDB = null;
            try (ResultSet rs = DB.executeQuery(sqlCheckCurrent, userEmail)) {
                if (rs != null && rs.next()) {
                    currentPasswordFromDB = rs.getString("password");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "현재 비밀번호 확인 중 데이터베이스 오류가 발생했습니다.");
                return;
            }

            if (currentPasswordFromDB != null && newPassword.equals(currentPasswordFromDB)) {
                JOptionPane.showMessageDialog(this, "현재 사용중인 비밀번호와 동일합니다. 다른 비밀번호를 입력해주세요.");
                tfNewPw.requestFocusInWindow();
                tfNewPw.setBorder(UIUtils.getErrorBorder());
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "새 비밀번호가 일치하지 않습니다. 다시 확인해주세요.");
                tfNewPw.setBorder(UIUtils.getErrorBorder());
                tfConfirmPw.setBorder(UIUtils.getErrorBorder());
                return;
            }

            String sqlUpdate = "UPDATE user SET password = ?, last_pw_change = NOW() WHERE email = ?";
            int result = DB.exceuteUpdate(sqlUpdate, newPassword, userEmail);

            if (result > 0) {
                JOptionPane.showMessageDialog(this, "비밀번호가 성공적으로 변경되었습니다.");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "비밀번호 변경에 실패했습니다. 다시 시도해주세요.");
            }
        };

        changeButton = new GeneralButton("변경", 80, 30, changeButtonListener);
        changeButton.setEnabled(false);

        changePanel.add(tfNewPw, "width 300!, height 35!, wrap 15");

        changePanel.add(tfConfirmPw, "width 300!, height 35!, wrap 15");

        changePanel.add(passwordMatchLabel, "wrap 10");
        changePanel.add(changeButton, "span 2, align center, width 80!, height 30!");

        return changePanel;
    }

    /**
     * 새로운 비밀번호와 확인 비밀번호 필드의 일치 여부를 확인하고 라벨을 업데이트
     * 이 메서드는 tfNewPw와 tfConfirmPw의 DocumentListener에 의해 호출
     */
    private void checkPasswordMatch() {
        String newPw = String.valueOf(tfNewPw.getPassword());
        String confirmPw = String.valueOf(tfConfirmPw.getPassword());

        boolean isNewPwPlaceholder = newPw.equals("새로운 비밀번호 입력");
        boolean isConfirmPwPlaceholder = confirmPw.equals("비밀번호 확인");

        // 비밀번호 입력이 없거나 플레이스홀더 상태면 라벨 숨김 및 버튼 비활성화
        if (newPw.isEmpty() || confirmPw.isEmpty() || isNewPwPlaceholder || isConfirmPwPlaceholder) {
            passwordMatchLabel.setText(" ");
            passwordMatchLabel.setVisible(false);
            changeButton.setEnabled(false);
            return;
        }

        if (newPw.equals(confirmPw)) {
            passwordMatchLabel.setText("비밀번호가 일치합니다.");
            passwordMatchLabel.setForeground(new Color(0, 120, 215));
            passwordMatchLabel.setVisible(true);
            changeButton.setEnabled(true);
        } else {
            passwordMatchLabel.setText("비밀번호가 일치하지 않습니다.");
            passwordMatchLabel.setForeground(Color.RED);
            passwordMatchLabel.setVisible(true);
            changeButton.setEnabled(false);
        }
    }
}
