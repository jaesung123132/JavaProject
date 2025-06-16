package jscommunity.dialog;

import jscommunity.db.LoginDAO;
import jscommunity.db.UserDAO;
import jscommunity.dbmember.Login;
import jscommunity.dbmember.User;
import jscommunity.utillity.APIUtils;
import jscommunity.utillity.UIUtils;
import jscommunity.utillity.GeneralButton; // ⭐️ 추가: GeneralButton 클래스 임포트
import net.miginfocom.swing.MigLayout;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Random;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class LoginDialog extends JDialog {
    // 이메일 정규 표현식: 더 포괄적인 형태로 변경
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";

    // 로그인 관련 컴포넌트
    private JTextField tfEmail;
    private JLabel errorLabel;
    private JPasswordField tfPassword;
    private JLabel Title;
    private JCheckBox showPasswordCheck;
    private GeneralButton loginbtn; // ⭐️ 변경: GeneralButton으로

    // 계정 복구(비밀번호 재설정/잠금 해제) 관련 컴포넌트
    private JTextField tfRecoveryEmail;
    private JLabel errorLabel2;
    private GeneralButton btnSendRecoveryCode; // ⭐️ 변경: GeneralButton으로
    private JLabel lblRecoveryTimer;
    private JTextField tfVerifyCode;
    private GeneralButton btnVerifyCode; // ⭐️ 변경: GeneralButton으로
    private JPasswordField tfNewPassword;
    private JPasswordField tfConfirmNewPassword;
    private JLabel newPasswordMatchLabel;
    private JCheckBox showNewPasswordCheck1;
    private JCheckBox showNewPasswordCheck2;
    private GeneralButton btnResetUnlockComplete; // ⭐️ 변경: GeneralButton으로
    private JLabel lblUnlockResetStatus;
    private GeneralButton btnBack; // ⭐️ 변경: GeneralButton으로


    private CardLayout cardLayout; // 메인 다이얼로그의 카드 레이아웃
    private JPanel cardPanel;      // 메인 다이얼로그의 카드 패널

    // 복구 패널 내부의 CardLayout과 JPanel
    private CardLayout recoveryCardLayout;
    private JPanel recoveryInnerCardPanel;

    // 상태 관리 변수 (UIUtils 활용으로 불필요해진 변수들 제거)
    private String currentVerificationCode = null; // 인증코드 변수명을 통일
    private String currentRecoveryEmail = null;
    private boolean isAccountLockedStatus = false;
    private char defaultEchoChar;
    private User loginUser;

    // 복구 타이머 관련 변수
    private Timer recoveryTimer;
    private int recoveryRemainingTime;
    private static final long RECOVERY_CODE_VALID_DURATION = 3 * 60 * 1000;


    public LoginDialog(JFrame parent) {
        super(parent, "로그인", true);

        // [1] 다이얼로그 기본 설정
        setSize(550, 500);
        setLocationRelativeTo(parent);
        setLayout(new MigLayout("insets 20, wrap 1, align center", "[center]", "[]10[]"));

        // [2] 메인 카드 레이아웃 및 패널 구성
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // [3] 로그인 화면 구성
        JPanel loginPanel = createLoginPanel(cardPanel, cardLayout);

        // [4] 계정 복구 화면 (내부 카드 레이아웃 포함) 구성
        JPanel accountRecoveryPanel = createAccountRecoveryPanel(cardPanel, cardLayout);

        // [5] 카드 등록 및 초기 화면 지정
        cardPanel.add(loginPanel, "login");
        cardPanel.add(accountRecoveryPanel, "recovery"); // "findpw" 대신 "recovery" 사용
        add(cardPanel, "grow");
        cardLayout.show(cardPanel, "login");

        // 복구 타이머 초기화 (생성자에서 한 번만)
        recoveryTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recoveryRemainingTime--;
                updateRecoveryTimerDisplay();
                if (recoveryRemainingTime <= 0) {
                    APIUtils.stopAndHideTimer(recoveryTimer, lblRecoveryTimer); // APIUtils 호출로 변경
                    JOptionPane.showMessageDialog(LoginDialog.this, "인증코드 유효 시간이 만료되었습니다. 다시 인증코드를 요청해주세요.");
                    tfVerifyCode.setEnabled(false);
                    btnVerifyCode.setEnabled(false);
                    btnSendRecoveryCode.setEnabled(true);
                    tfRecoveryEmail.setEnabled(true);
                    lblUnlockResetStatus.setText("인증코드 유효 시간이 만료되었습니다. 이메일을 다시 입력하고 인증코드를 재발송하세요.");
                    recoveryCardLayout.show(recoveryInnerCardPanel, "authentication"); // 타이머 만료 시 인증 단계로 돌아감
                }
            }
        });

    }

    private JPanel createLoginPanel(JPanel cardPanel, CardLayout cardLayout) {
        JPanel loginPanel = new JPanel(new MigLayout("align center, wrap 2", "[center]", "[]10[]"));
        loginPanel.setOpaque(false);

        //타이틀
        Title = new JLabel("로그인");
        Title.setFont(new Font("맑은 고딕", Font.BOLD, 30));

        //이메일 입력 필드 및 에러 라벨 초기화
        tfEmail = new JTextField();
        errorLabel = new JLabel("올바른 이메일 형식이 아닙니다.");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font(tfEmail.getFont().getName(), Font.PLAIN, tfEmail.getFont().getSize() - 2));
        errorLabel.setVisible(false); // 초기에는 숨김
        UIUtils.setupTextField(tfEmail, "이메일을 입력하세요.", errorLabel, this::toggleLoginButton);


        //비밀번호 입력 필드
        showPasswordCheck = new JCheckBox(); // tfPassword 이전에 초기화
        tfPassword = new JPasswordField(20);
        defaultEchoChar = tfPassword.getEchoChar();
        UIUtils.setupPasswordField(tfPassword, showPasswordCheck, "비밀번호를 입력하세요.", this::toggleLoginButton);

        //로그인 버튼
        loginbtn = new GeneralButton("로그인하기", 400, 45, 30, e -> { // ⭐️ 변경: GeneralButton 생성자에 리스너 삽입
            tryLogin(); // 로그인 시도
        });

        loginbtn.setEnabled(false); // 초기에는 비활성화
        getRootPane().setDefaultButton(loginbtn);

        //비밀번호 찾기 라벨
        JLabel findPwLabel = new JLabel("비밀번호 재설정 또는 계정 잠금 해제");
        findPwLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        findPwLabel.setForeground(Color.BLUE);
        findPwLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        findPwLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                resetRecoveryPanelUI(); // 복구 패널로 전환 시 UI 초기화
                cardLayout.show(cardPanel, "recovery");
            }
        });

        // [7] 컴포넌트 배치
        loginPanel.add(Title, "span, wrap");
        loginPanel.add(tfEmail, "span, growx, wrap, width 400!, align left, gaptop 15");
        loginPanel.add(errorLabel, "span, align left, wrap");
        loginPanel.add(tfPassword, "growx, split 2");
        loginPanel.add(showPasswordCheck, "wrap");
        loginPanel.add(findPwLabel, "align right, wrap");
        loginPanel.add(loginbtn, "growx, wrap, gaptop 30, align left");

        return loginPanel;
    }

    // ========== 계정 복구(비밀번호 재설정/잠금 해제) 패널 생성 메서드 ==========
    private JPanel createAccountRecoveryPanel(JPanel mainCardPanel, CardLayout mainCardLayout) {
        JPanel recoveryPanel = new JPanel(new MigLayout("wrap 1, align center"));
        recoveryPanel.setOpaque(false);

        // 복구 패널 내부의 카드 레이아웃 및 패널 생성
        recoveryCardLayout = new CardLayout();
        recoveryInnerCardPanel = new JPanel(recoveryCardLayout);
        recoveryInnerCardPanel.setOpaque(false);

        // 공통 상단 메시지 라벨
        lblUnlockResetStatus = new JLabel("이메일을 입력하고 인증코드를 받아 계정을 복구하세요.");
        lblUnlockResetStatus.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        lblUnlockResetStatus.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel authenticationPanel = new JPanel(new MigLayout("wrap 1, align center"));
        authenticationPanel.setOpaque(false);

        tfRecoveryEmail = new JTextField(20);
        errorLabel2 = new JLabel("올바른 이메일 형식이 아닙니다.");
        errorLabel2.setForeground(Color.RED);
        errorLabel2.setFont(new Font(tfRecoveryEmail.getFont().getName(), Font.PLAIN, tfRecoveryEmail.getFont().getSize() - 2));
        errorLabel2.setVisible(false); // 초기에는 숨김

        UIUtils.setupTextField(tfRecoveryEmail, "이메일을 입력하세요.", null, this::checkRecoveryEmailFieldStatus);

        btnSendRecoveryCode = new GeneralButton("인증코드 발송", 80, 40, e -> {
            currentRecoveryEmail = tfRecoveryEmail.getText().trim();
            boolean isEmailValid = !currentRecoveryEmail.equals("이메일을 입력하세요.") && currentRecoveryEmail.matches(EMAIL_REGEX);

            if (!isEmailValid) {
                tfRecoveryEmail.setBorder(UIUtils.getErrorBorder());
                errorLabel2.setText("올바른 이메일 형식이 아닙니다.");
                errorLabel2.setVisible(true);
                return;
            } else {
                tfRecoveryEmail.setBorder(UIUtils.getNormalBorder());
                errorLabel2.setVisible(false);
            }

            if (UserDAO.findByEmail(currentRecoveryEmail) == null) {
                JOptionPane.showMessageDialog(this, "등록되지 않은 이메일입니다.");
                return;
            }

            Login login = LoginDAO.getLoginInfoByEmail(currentRecoveryEmail);
            if (login != null && login.isAccountLocked()) {
                isAccountLockedStatus = true;
                lblUnlockResetStatus.setText("계정이 잠금 상태입니다. 잠금 해제를 위해 인증코드를 확인하세요.");
            } else {
                isAccountLockedStatus = false;
                lblUnlockResetStatus.setText("비밀번호 재설정을 위해 인증코드를 확인하세요.");
            }

            currentVerificationCode = String.format("%06d", new Random().nextInt(1000000));
            User user = UserDAO.findByEmail(currentRecoveryEmail);
            String name = user != null ? user.getName() : currentRecoveryEmail;

            try {
                // APIUtils의 sendEmailViaBrevo 메서드 호출
                String subject = "[JSCampus] 계정 복구 이메일 인증";
                String htmlContent = "<html><body>"
                        + "<p>안녕하세요, <strong>" + name+ "</strong>님</p>"
                        + "<p>계정 복구 (비밀번호 재설정 또는 잠금 해제) 를 위한 인증 코드는 다음과 같습니다:</p>"
                        + "<h3>" + currentVerificationCode + "</h3>"
                        + "<p>인증 코드를 입력하여 계정 복구를 완료해주세요.</p>"
                        + "<p>감사합니다.</p>"
                        + "</body></html>";
                APIUtils.sendEmailViaBrevo(currentRecoveryEmail, name, subject, htmlContent); // APIUtils 호출

                JOptionPane.showMessageDialog(this, "인증코드가 발송되었습니다. 이메일을 확인해주세요.");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "이메일 발송에 실패했습니다. 다시 시도해주세요.");
                return;
            }

            tfVerifyCode.setVisible(true);
            btnVerifyCode.setVisible(true);
            lblRecoveryTimer.setVisible(true);
            tfVerifyCode.setText("");
            btnSendRecoveryCode.setEnabled(false);
            tfRecoveryEmail.setEnabled(false); // 이메일 필드 비활성화

            recoveryRemainingTime = (int) (APIUtils.getVerificationCodeValidDurationMillis() / 1000); // APIUtils에서 가져옴
            APIUtils.startAndShowTimer(recoveryTimer, lblRecoveryTimer); // APIUtils 호출

            authenticationPanel.revalidate();
            authenticationPanel.repaint();
            SwingUtilities.invokeLater(tfVerifyCode::requestFocusInWindow);
        });

        lblRecoveryTimer = new JLabel("3:00");
        lblRecoveryTimer.setForeground(Color.RED);
        lblRecoveryTimer.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        lblRecoveryTimer.setVisible(false);

        tfVerifyCode = new JTextField(10);
        tfVerifyCode.setVisible(false);
        tfVerifyCode.setBorder(UIUtils.getNormalBorder());

        btnVerifyCode = new GeneralButton("인증코드 확인", 80, 30, e -> {
            String inputCode = tfVerifyCode.getText().trim();

            if (recoveryRemainingTime <= 0) {
                JOptionPane.showMessageDialog(this, "인증코드 유효 시간이 만료되었습니다. 다시 인증코드를 요청해주세요.");
                return;
            }

            if (inputCode.equals(currentVerificationCode)) {
                JOptionPane.showMessageDialog(this, "인증이 완료되었습니다!");
                APIUtils.stopAndHideTimer(recoveryTimer, lblRecoveryTimer); // APIUtils 호출로 변경

                lblUnlockResetStatus.setText("새 비밀번호를 입력해주세요.");
                recoveryCardLayout.show(recoveryInnerCardPanel, "resetPassword");
                setupNewPasswordFields();
                btnResetUnlockComplete.setEnabled(false);
                btnBack.setVisible(true);

                recoveryInnerCardPanel.revalidate();
                recoveryInnerCardPanel.repaint();

            } else {
                JOptionPane.showMessageDialog(this, "인증코드가 일치하지 않습니다. 다시 시도해주세요.");
                tfVerifyCode.setBorder(UIUtils.getErrorBorder());
            }
        });
        tfVerifyCode.setVisible(false);
        btnVerifyCode.setVisible(false);

        btnBack = new GeneralButton("뒤로가기", 60, 40, e -> {
            mainCardLayout.show(mainCardPanel, "login");
            resetRecoveryPanelUI();
        });


        // 인증 단계 패널에 컴포넌트 추가
        authenticationPanel.add(lblUnlockResetStatus, "span, wrap, gaptop 0");
        authenticationPanel.add(tfRecoveryEmail, "growx, wrap, width 400!, gaptop 15");
        authenticationPanel.add(errorLabel2, "align left, wrap"); // 에러 라벨 위치
        authenticationPanel.add(btnSendRecoveryCode, "wrap, gaptop 10");
        authenticationPanel.add(tfVerifyCode, "growx, wrap, width 200!, gaptop 5");
        authenticationPanel.add(lblRecoveryTimer, "align left, wrap, gapy 0");
        authenticationPanel.add(btnVerifyCode, "wrap, gaptop 5");
        authenticationPanel.add(btnBack, "align right, wrap, gaptop 15");


        JPanel resetPasswordPanel = new JPanel(new MigLayout("wrap 1, align center"));
        resetPasswordPanel.setOpaque(false);

        // 새 비밀번호 입력 필드 초기화
        showNewPasswordCheck1 = new JCheckBox(); // tfNewPassword 이전에 초기화
        tfNewPassword = new JPasswordField(20);
        UIUtils.setupPasswordField(tfNewPassword, showNewPasswordCheck1, "비밀번호를 입력하세요.", this::checkNewPasswordMatch);

        showNewPasswordCheck2 = new JCheckBox();
        tfConfirmNewPassword = new JPasswordField(20);
        UIUtils.setupPasswordField(tfConfirmNewPassword, showNewPasswordCheck2, "비밀번호를 입력하세요.", this::checkNewPasswordMatch);

        newPasswordMatchLabel = new JLabel(" ");
        newPasswordMatchLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        // 비밀번호 재설정 완료 버튼
        btnResetUnlockComplete = new GeneralButton("비밀번호 재설정 완료", 80, 40, e -> {
            String newPw = new String(tfNewPassword.getPassword());
            String confirmPw = new String(tfConfirmNewPassword.getPassword());

            if (!newPw.equals(confirmPw) || newPw.equals("비밀번호를 입력하세요.") || newPw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않거나 유효하지 않습니다. ");
                return;
            }

            // 비밀번호 변경 및 계정 잠금 해제 통합
            if (UserDAO.updatePassword(currentRecoveryEmail, newPw)) {
                // 로그인 정보를 업데이트하여 잠금 해제 및 실패 횟수 초기화
                Login login = LoginDAO.getLoginInfoByEmail(currentRecoveryEmail);
                if (login != null) {
                    login.setAccountLocked(false);
                    login.setLoginFailCount(0);
                    LoginDAO.updateLoginInfo(login);
                }

                JOptionPane.showMessageDialog(this, "비밀번호가 성공적으로 재설정되었고 계정 잠금이 해제되었습니다. 이제 새 비밀번호로 로그인해주세요.");
                resetRecoveryPanelUI(); // 전체 복구 패널 UI 초기화
                mainCardLayout.show(mainCardPanel, "login"); // 메인 카드 레이아웃을 로그인으로 전환
            } else {
                JOptionPane.showMessageDialog(this, "비밀번호 재설정에 실패했습니다. 관리자에게 문의하세요.");
            }
        });
        btnResetUnlockComplete.setEnabled(false); // 초기에는 비활성화


        // 재설정 단계 패널에 컴포넌트 추가
        resetPasswordPanel.add(new JLabel("새 비밀번호를 입력하세요."), "wrap");
        resetPasswordPanel.add(tfNewPassword, "growx, width 400!, split 2");
        resetPasswordPanel.add(showNewPasswordCheck1, "wrap");
        resetPasswordPanel.add(tfConfirmNewPassword, "growx, width 400!, split 2");
        resetPasswordPanel.add(showNewPasswordCheck2, "wrap");
        resetPasswordPanel.add(newPasswordMatchLabel, "align left, wrap, gapy 0");
        resetPasswordPanel.add(btnResetUnlockComplete, "wrap, gaptop 20, align right");

        // recoveryInnerCardPanel에 카드들 추가
        recoveryInnerCardPanel.add(authenticationPanel, "authentication");
        recoveryInnerCardPanel.add(resetPasswordPanel, "resetPassword");

        recoveryPanel.add(recoveryInnerCardPanel, "grow, push"); // 내부 카드 패널을 recoveryPanel에 추가

        return recoveryPanel;
    }

    // 새 비밀번호 필드 초기화 메서드
    private void setupNewPasswordFields() {
        tfNewPassword.setText(""); // UIUtils.setupPasswordField가 플레이스홀더 관리
        tfConfirmNewPassword.setText("");

        showNewPasswordCheck1.setSelected(false);
        showNewPasswordCheck2.setSelected(false);

        newPasswordMatchLabel.setText(" ");
        newPasswordMatchLabel.setVisible(false);
    }

    // 복구 패널 UI 초기화 메서드 (내부 카드 전환도 포함)
    private void resetRecoveryPanelUI() {
        lblUnlockResetStatus.setText("이메일을 입력하고 인증코드를 받아 계정을 복구하세요.");
        tfRecoveryEmail.setText(""); // UIUtils가 플레이스홀더 관리

        errorLabel2.setVisible(false); // 에러 라벨 숨김

        SwingUtilities.invokeLater(this::checkRecoveryEmailFieldStatus); // 초기 상태 업데이트를 위해 호출

        btnSendRecoveryCode.setVisible(true); // 인증코드 발송 버튼 보이기


        tfVerifyCode.setVisible(false);
        btnVerifyCode.setVisible(false);
        lblRecoveryTimer.setVisible(false);
        APIUtils.stopAndHideTimer(recoveryTimer, lblRecoveryTimer); // APIUtils 호출로 변경

        setupNewPasswordFields(); // 파라미터 제거 (이전 답변에서 지시된 변경)
        btnBack.setVisible(true);

        currentVerificationCode = null;
        currentRecoveryEmail = null;
        isAccountLockedStatus = false;
        recoveryRemainingTime = 0;

        recoveryCardLayout.show(recoveryInnerCardPanel, "authentication");
    }


    private void checkNewPasswordMatch() {
        String pw1 = new String(tfNewPassword.getPassword());
        String pw2 = new String(tfConfirmNewPassword.getPassword());

        // 비밀번호 필드가 플레이스홀더이거나 비어있거나, 최소 길이(8자) 미만이면 비활성화 및 에러 숨김
        if (pw1.equals("비밀번호를 입력하세요.") || pw2.equals("비밀번호를 입력하세요.") || pw1.isEmpty() || pw2.isEmpty()) {
            newPasswordMatchLabel.setText(" ");
            newPasswordMatchLabel.setVisible(false);
            btnResetUnlockComplete.setEnabled(false);
            return;
        }

        if (pw1.equals(pw2)) {
            newPasswordMatchLabel.setVisible(true);
            newPasswordMatchLabel.setText("비밀번호가 일치합니다.");
            newPasswordMatchLabel.setForeground(new Color(0, 120, 215));
            btnResetUnlockComplete.setEnabled(true);
        } else {
            newPasswordMatchLabel.setVisible(true);
            newPasswordMatchLabel.setText("비밀번호가 일치하지 않습니다.");
            newPasswordMatchLabel.setForeground(Color.RED);
            btnResetUnlockComplete.setEnabled(false);
        }
    }

    private void checkRecoveryEmailFieldStatus() {
        String emailText = tfRecoveryEmail.getText().trim();
        boolean isPlaceholder = emailText.equals("이메일을 입력하세요.");
        boolean matchesRegex = emailText.equals("");

        boolean enableButton = !isPlaceholder && !matchesRegex;
        btnSendRecoveryCode.setEnabled(enableButton);

    }


    private void tryLogin() {
        String email = tfEmail.getText().trim();
        String password = new String(tfPassword.getPassword());

        Login loginInfo = LoginDAO.getLoginInfoByEmail(email);

        // 1. 이메일이 DB에 존재하지 않을 때 (UserDAO.findByEmail(email)도 없을 때)
        if (loginInfo == null) {
            if (UserDAO.findByEmail(email) == null) {
                errorLabel.setVisible(true);
                tfEmail.setBorder(UIUtils.getErrorBorder());
                JOptionPane.showMessageDialog(this, "존재하지 않는 이메일입니다.");
                return;
            }
            loginInfo = new Login(email, 0, false);
            if (!LoginDAO.insertLoginInfo(loginInfo)) {
                JOptionPane.showMessageDialog(this, "로그인 정보 초기화에 실패했습니다. 관리자에게 문의하세요.");
                return;
            }
        }

        // 2. 계정 잠금 상태 확인 및 통합된 복구 흐름 시작
        if (loginInfo.isAccountLocked()) {
            JOptionPane.showMessageDialog(this, "계정이 5회 이상 로그인 실패로 잠금 처리되었습니다. 비밀번호 재설정을 진행해주세요.");
            tfRecoveryEmail.setText(email); // 복구 이메일 필드에 현재 이메일 자동 채우기
            resetRecoveryPanelUI(); // 복구 패널 UI 초기화
            lblUnlockResetStatus.setText("계정이 잠겨 있습니다. 비밀번호 재설정을 위해 인증코드를 발송하세요.");
            btnSendRecoveryCode.setEnabled(true); // 바로 발송 가능하도록

            cardLayout.show(cardPanel, "recovery"); // 메인 다이얼로그의 복구 화면으로 전환
            recoveryCardLayout.show(recoveryInnerCardPanel, "authentication"); // 복구 패널 내부의 인증 단계로 전환
            return; // 로그인 시도 중단
        }

        // 3. 실제 로그인 시도
        User user = UserDAO.login(email, password);

        if (user != null) {
            // 로그인 성공 시 실패 횟수 초기화 및 잠금 해제
            LocalDate suspensionEndDate = user.getSuspensionEndDate();
            if (suspensionEndDate != null && suspensionEndDate.isAfter(LocalDate.now())) {
                // 정지 상태인 경우 메시지 표시
                JOptionPane.showMessageDialog(this,
                        user.getName() + "님은 " + suspensionEndDate + "까지 정지 상태입니다.\n" +
                                "서비스 이용에 제한이 있으며, 자세한 내용은 아래 관리자 메일로 문의주세요.",
                        "계정 정지 알림", JOptionPane.WARNING_MESSAGE);
                return; // 정지 상태이므로 로그인 성공 처리하지 않고 메서드 종료
            }

            if (loginInfo.getLoginFailCount() > 0 || loginInfo.isAccountLocked()) {
                loginInfo.setLoginFailCount(0);
                loginInfo.setAccountLocked(false);
                LoginDAO.updateLoginInfo(loginInfo);
            }

            // 비밀번호 변경 권고
            if (user.getLastPasswordChange() != null &&
                    user.getLastPasswordChange().isBefore(LocalDateTime.now().minus(30, ChronoUnit.DAYS))) {
                JOptionPane.showMessageDialog(this,
                        "마지막 비밀번호 변경일이 30일이 지났습니다.\n보안을 위해 비밀번호를 변경해주세요.",
                        "비밀번호 변경 권고", JOptionPane.WARNING_MESSAGE);
            }

            JOptionPane.showMessageDialog(this, "로그인 성공!");
            this.loginUser = user;
            dispose(); // 로그인 성공 시 다이얼로그 닫기
        } else {
            // 로그인 실패 시 실패 횟수 증가
            loginInfo.setLoginFailCount(loginInfo.getLoginFailCount() + 1);

            String message;
            if (loginInfo.getLoginFailCount() >= 5) {
                loginInfo.setAccountLocked(true); // 5회 이상 실패 시 계정 잠금
                message = "이메일 또는 비밀번호가 틀렸습니다. (5회 이상 실패하여 계정이 잠겼습니다)";
            } else {
                message = "이메일 또는 비밀번호가 틀렸습니다. (실패 횟수: " + loginInfo.getLoginFailCount() + "회)";
            }
            LoginDAO.updateLoginInfo(loginInfo); // 업데이트된 로그인 정보 저장

            JOptionPane.showMessageDialog(this, message);

            tfPassword.setText(""); // 비밀번호 필드 초기화
            showPasswordCheck.setSelected(false); // 비밀번호 보기 체크박스 해제
        }
    }

    // 로그인 버튼 활성화 여부만 결정하는 메서드
    private void toggleLoginButton() {
        String email = tfEmail.getText().trim();
        String password = new String(tfPassword.getPassword()).trim();

        boolean isEmailPlaceholder = email.equals("이메일을 입력하세요.");
        boolean isPasswordPlaceholder = password.equals("비밀번호를 입력하세요.");

        // 플레이스홀더가 아니면서 내용이 비어있지 않으면 활성화
        boolean isEmailEntered = !isEmailPlaceholder && !email.isEmpty();
        boolean isPasswordEntered = !isPasswordPlaceholder && !password.isEmpty();

        loginbtn.setEnabled(isEmailEntered && isPasswordEntered);

        // 이 메서드는 실시간 에러 라벨을 제어하지 않습니다.
    }


    public User getLoginUser() {
        return loginUser;
    }
    private void updateRecoveryTimerDisplay() {
        int minutes = recoveryRemainingTime / 60;
        int seconds = recoveryRemainingTime % 60;
        lblRecoveryTimer.setText(String.format("%02d:%02d", minutes, seconds));

        if (recoveryRemainingTime <= 0) {
            APIUtils.stopAndHideTimer(recoveryTimer, lblRecoveryTimer); // APIUtils 호출로 변경
            JOptionPane.showMessageDialog(this, "인증코드 유효 시간이 만료되었습니다. 다시 인증코드를 요청해주세요.");
            tfVerifyCode.setEnabled(false);
            btnVerifyCode.setEnabled(false);
            btnSendRecoveryCode.setEnabled(true);
            tfRecoveryEmail.setEnabled(true);
            lblUnlockResetStatus.setText("인증코드 유효 시간이 만료되었습니다. 이메일을 다시 입력하고 인증코드를 재발송하세요.");
            recoveryCardLayout.show(recoveryInnerCardPanel, "authentication");
        }
    }

}
