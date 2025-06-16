package jscommunity.dialog;

import jscommunity.db.DB;
import jscommunity.db.LoginDAO;
import jscommunity.db.UserDAO;
import jscommunity.dbmember.Login;
import jscommunity.dbmember.User;
import jscommunity.utillity.APIUtils;
import jscommunity.utillity.GeneralButton;
import jscommunity.utillity.UIUtils;
import net.miginfocom.swing.MigLayout;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Random;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;

public class JoinDialog extends JDialog {
    // UI 컴포넌트 관련
    private final JRadioButton maleButton; // 성별 선택: 남성 라디오 버튼
    private final JRadioButton femaleButton; // 성별 선택: 여성 라디오 버튼
    private JTextField tfEmail, tfName, tfCode, tfBirthday, tfPhoneNum; // 이메일, 이름, 인증코드, 생년월일, 전화번호 입력 필드
    private JPasswordField tfPassword, tfPasswordCheck; // 비밀번호, 비밀번호 확인 입력 필드
    private JLabel tfEmailErrorLabel, codeErrorLabel, passwordMatchLabel; // 이메일 오류, 인증코드 오류, 비밀번호 일치 여부 표시 라벨
    private JButton btnSendCode, btnJoin, btnVerify; // 인증코드 발송, 회원가입, 인증 확인 버튼
    private JCheckBox showPasswordCheck, showPasswordCheck2; // 비밀번호 보기/숨기기 체크박스
    private char defaultEchoChar; // 비밀번호 숨김 시 사용

    // 추가된 타이머 및 재전송 관련 컴포넌트
    private JLabel lblTimer; // 타이머 표시 라벨
    private JButton btnResendCode; // 인증코드 재전송 버튼 (링크처럼)

    // 인증 및 상태 관리
    private String verificationCode = ""; // 발송된 인증 코드
    private boolean isVerified = false; // 이메일 인증 완료 플래그
    private long codeExpirationTime; // 인증 코드의 만료 시간
    private static final long CODE_VALIE_DURATION = 3 * 60 * 1000; // 인증 코드 유효 시간 (3분)

    // 타이머 관련 변수
    private Timer timer;
    private int remainingTime; // 남은 시간 (초 단위)


    public JoinDialog(JFrame parent) {
        super(parent, "회원가입", true);
        setSize(700, 550);
        setLocationRelativeTo(parent);
        // debug 제거: debug는 UI를 보여줄 때 레이아웃 경계를 표시하는 옵션입니다.
        setLayout(new MigLayout("insets 20, wrap 1, align center, gapy 0", "[center]", ""));


        Font font = new Font("맑은 고딕", Font.BOLD, 15);
        JLabel title = new JLabel("회원가입");
        title.setFont(new Font("맑은 고딕", Font.BOLD, 40));
        add(title, "span 2, align center, wrap 30");

        // 2. 이메일 입력
        tfEmail = new JTextField();
        // --- btnSendCode 초기화 (기존 코드 유지) ---
        btnSendCode = new JButton();
        UIUtils.setupRoundedImageButton(
                btnSendCode,
                "/코드발송.png",          // 기본 이미지
                "/코드발송_명암.png",      // hover 이미지
                80, 30,                  // 크기
                30,                     // 둥글기
                e -> sendcode()         // 클릭 리스너
        );
        btnSendCode.setEnabled(false);
        // --- btnSendCode 초기화 (기존 코드 유지) 끝 ---

        tfEmailErrorLabel = new JLabel("올바른 이메일을 입력해주세요.");
        tfEmailErrorLabel.setForeground(Color.RED);
        tfEmailErrorLabel.setFont(new Font(tfEmail.getFont().getName(), Font.PLAIN, tfEmail.getFont().getSize() - 2));
        tfEmailErrorLabel.setVisible(false);


        UIUtils.setupTextField(tfEmail, "이메일 입력", tfEmailErrorLabel, () -> {
                    btnSendCode.setEnabled(
                            !tfEmail.getText().trim().isEmpty() &&
                                    !tfEmail.getText().equals("이메일 입력")
                    );
                    tfEmailErrorLabel.setVisible(false);
                }
        );

        add(tfEmail, "align left, width 350!, split 2");
        add(btnSendCode, "align right, gapleft 10, wrap");
        add(tfEmailErrorLabel, "align left, gapy 0, wrap"); // 에러 라벨



        // 3. 인증 코드
        tfCode = new JTextField();
        // --- btnVerify 초기화 (기존 코드 유지) ---
        btnVerify = new JButton();
        // ⭐️ 유지: btnVerify를 GeneralButton으로 초기화 (액션 리스너 포함)
        btnVerify = new GeneralButton( "확인", 80, 30,15, e -> verifyCode());
        btnVerify.setEnabled(false);

        codeErrorLabel = new JLabel("인증코드가 일치하지 않습니다.");
        codeErrorLabel.setForeground(Color.RED);
        codeErrorLabel.setFont(new Font(tfCode.getFont().getName(), Font.PLAIN, tfCode.getFont().getSize() - 2));
        codeErrorLabel.setVisible(false);

        // --- 타이머 및 재전송 라벨 초기화 시작 ---
        lblTimer = new JLabel("3:00");
        lblTimer.setForeground(Color.RED);
        lblTimer.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        lblTimer.setVisible(false); // 초기에는 숨김

        btnResendCode = new JButton("인증코드를 받지 못했습니까? 재전송");
        btnResendCode.setBorderPainted(false);
        btnResendCode.setContentAreaFilled(false);
        btnResendCode.setFocusPainted(false);
        btnResendCode.setForeground(Color.BLUE);
        btnResendCode.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnResendCode.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        btnResendCode.setVisible(false); // 초기에는 숨김
        btnResendCode.addActionListener(e -> askForResend()); // askForResend() 호출하도록 변경
        // --- 타이머 및 재전송 라벨 초기화 끝 ---


        tfCode.setEnabled(false); // 이메일 인증 발송 전엔 비활성화
        tfCode.setBackground(Color.WHITE);

        UIUtils.setupTextField(
                tfCode,
                "인증코드 입력",
                codeErrorLabel,
                () -> {
                    btnVerify.setEnabled(
                            !tfCode.getText().trim().isEmpty() &&
                                    !tfCode.getText().equals("인증코드 입력")
                    );
                    codeErrorLabel.setVisible(false);
                }
        );

        add(tfCode, "align left, width 350!, split 2");
        add(btnVerify, "align right, gapleft 10, wrap");
        add(lblTimer, "align left, gapy 0, split 2"); // 타이머 라벨 배치
        add(btnResendCode, "align left, gapleft 10, wrap"); // 재전송 버튼 배치
        add(codeErrorLabel, "align left, wrap"); // 에러 라벨 배치

        // 4. 비밀번호
        tfPassword = new JPasswordField(20);
        defaultEchoChar = tfPassword.getEchoChar();
        showPasswordCheck = new JCheckBox();
        UIUtils.setupPasswordField(tfPassword, showPasswordCheck, "비밀번호 입력", this::toggleJoinButton);


        // 4-1. 비밀번호 확인
        tfPasswordCheck = new JPasswordField(20);
        showPasswordCheck2 = new JCheckBox();
        UIUtils.setupPasswordField(tfPasswordCheck, showPasswordCheck2, "비밀번호 확인", this::toggleJoinButton);


        // 4-2. 비밀번호 일치 메시지 라벨
        passwordMatchLabel = new JLabel(" ");
        passwordMatchLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        //비밀번호 일치 여부 확인용 DocumentListener 등록
        DocumentListener passwordMatchListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { checkPasswordMatch(); }
            @Override public void removeUpdate(DocumentEvent e) { checkPasswordMatch(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        };

        tfPassword.getDocument().addDocumentListener(passwordMatchListener);
        tfPasswordCheck.getDocument().addDocumentListener(passwordMatchListener);


        add(tfPassword, "align left, width 410!, split 2, gapbottom 15");
        add(showPasswordCheck, "gapbottom 20, wrap 5");
        add(tfPasswordCheck, "align left, width 410!, split 2");
        add(showPasswordCheck2, "wrap 5");
        add(passwordMatchLabel, "align left, gapleft 5, wrap");


        // 5. 이름
        tfName = new JTextField();
        UIUtils.setupTextField(tfName, "이름 입력", null, this::toggleJoinButton);


        // 7. 성별 선택
        maleButton = new JRadioButton("남성");
        femaleButton = new JRadioButton("여성");

        maleButton.setFont(font);
        femaleButton.setFont(font);
        maleButton.setOpaque(false);
        femaleButton.setOpaque(false);
        maleButton.setSelected(true);

        ButtonGroup genderGroup = new ButtonGroup();
        genderGroup.add(maleButton);
        genderGroup.add(femaleButton);

        add(tfName, "width 200!, align left, split 3");
        add(maleButton, "gapleft 50");
        add(femaleButton,"wrap 15");

        // tfBirthday 설정
        tfBirthday = new JTextField();
        UIUtils.setupTextField(tfBirthday, "생년월일 입력", null, this::toggleJoinButton);

        // tfPhoneNum 설정
        tfPhoneNum = new JTextField();
        UIUtils.setupTextField(tfPhoneNum, "전화번호 입력('-' 까지 입력)", null, this::toggleJoinButton);

        // --- btnJoin 초기화 (기존 코드 유지) ---
        btnJoin = new JButton();
        UIUtils.setupRoundedImageButton(
                btnJoin,
                "/가입하기.png",
                "/가입하기_명암.png",
                80, 30,
                30,  // radius
                e -> tryJoin()
        );
        btnJoin.setEnabled(false);

        add(tfBirthday, "width 200!, align left, split 2, gaptop 5");
        add(tfPhoneNum, "width 190!, align right, wrap 5, gapleft 50");
        add(btnJoin, "span 2, align center, gaptop 20");

        // 타이머 초기화 (생성자 마지막 부분)
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remainingTime--;
                updateTimerDisplay();
                if (remainingTime <= 0) {
                    APIUtils.stopAndHideTimer(timer, lblTimer); // APIUtils 호출로 변경
                    JOptionPane.showMessageDialog(JoinDialog.this, "인증코드 유효 시간이 만료되었습니다. 재전송 해주세요.");
                    tfCode.setBorder(UIUtils.getErrorBorder());
                    isVerified = false; // 타이머 만료 시 인증 상태 초기화
                }
            }
        });
    }

    // 비밀번호 일치 여부 확인용 메서드
    private void checkPasswordMatch() {
        String pw1 = String.valueOf(tfPassword.getPassword());
        String pw2 = String.valueOf(tfPasswordCheck.getPassword());

        // 플레이스홀더 상태면 비교하지 않음
        if (pw1.equals("비밀번호 입력") || pw2.equals("비밀번호 확인")) {
            passwordMatchLabel.setText(" ");
            passwordMatchLabel.setVisible(false);
            return;
        }

        // 비밀번호 확인 입력란이 비어 있으면 숨김 처리
        if (pw2.isEmpty()) {
            passwordMatchLabel.setText(" ");
            passwordMatchLabel.setVisible(false);
        } else if (pw1.equals(pw2)) {
            passwordMatchLabel.setVisible(true);
            passwordMatchLabel.setText("비밀번호가 일치합니다.");
            passwordMatchLabel.setForeground(new Color(0, 120, 215));  // 파란색
        } else {
            passwordMatchLabel.setVisible(true);
            passwordMatchLabel.setText("비밀번호가 일치하지 않습니다.");
            passwordMatchLabel.setForeground(Color.RED);
        }
    }

    private void sendcode() {
        String email = tfEmail.getText().trim();
        String name = tfName.getText().trim();
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            tfEmail.setBorder(UIUtils.getErrorBorder());
            tfEmailErrorLabel.setVisible(true);
            return;
        }

        if (DB.isEmailDuplicated(email)) {
            JOptionPane.showMessageDialog(this, "이미 사용중인 이메일입니다. 다시 입력해주세요.");
            return;
        }
        btnResendCode.setVisible(true);
        // 6자리 랜덤 숫자 인증코드 생성
        verificationCode = String.format("%06d", new Random().nextInt(1000000)); // 000000 ~ 999999
        // 인증코드 만료시간 설정 (현재시간 + 3분)
        codeExpirationTime = System.currentTimeMillis() + CODE_VALIE_DURATION;
        remainingTime = (int) (CODE_VALIE_DURATION / 1000); // 초 단위로 초기화

        // Brevo API를 사용하여 이메일 발송
        try {
            String subject = "[JSCampus] 회원가입 이메일 인증";
            String htmlContent = "<html><body>"
                    + "<p>안녕하세요, <strong>" + name+ "</strong>님</p>"
                    + "<p>JSCampus 회원가입을 위한 인증 코드는 다음과 같습니다:</p>"
                    + "<h3>" + verificationCode + "</h3>"
                    + "<p>인증 코드를 입력하여 회원가입을 완료해주세요.</p>"
                    + "<p>감사합니다.</p>"
                    + "</body></html>";
            APIUtils.sendEmailViaBrevo(email, name, subject, htmlContent); // APIUtils 호출

            tfCode.setEnabled(true);
            tfEmail.setEnabled(false);
            JOptionPane.showMessageDialog(this, "인증코드가 발송되었습니다. 이메일을 확인해주세요.");

            APIUtils.startAndShowTimer(timer, lblTimer); // APIUtils 호출 (타이머 시작 및 UI 표시)

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "이메일 발송에 실패했습니다. 다시 시도해주세요.");
            tfEmail.setEnabled(true); // 실패 시 이메일 다시 활성화
            APIUtils.stopAndHideTimer(timer, lblTimer); // APIUtils 호출 (실패 시 타이머 및 재전송 버튼 숨김)
        }
    }

    private void verifyCode() {
        // 타이머가 만료되었는지 확인하거나, 남은 시간이 0 이하면 만료 처리
        if(remainingTime <= 0) { // codeExpirationTime 관련 조건 제거
            JOptionPane.showMessageDialog(this, "인증코드 유효 시간이 만료되었습니다. 재전송 버튼을 눌러주세요.");
            tfCode.setBorder(UIUtils.getErrorBorder());
            isVerified = false;
            return;
        }

        String inputCode = tfCode.getText().trim();

        if (inputCode.equals(verificationCode)) {
            JOptionPane.showMessageDialog(this, "인증이 완료되었습니다!");
            isVerified = true;
            tfEmail.setEnabled(false);
            tfCode.setEnabled(false);
            APIUtils.stopAndHideTimer(timer, lblTimer); // APIUtils 호출
            toggleJoinButton(); // 인증 성공 시 가입 버튼 활성화 조건 재확인
        } else {
            isVerified = false;
            codeErrorLabel.setVisible(true);
            tfCode.setBorder(UIUtils.getErrorBorder());
            JOptionPane.showMessageDialog(this, "인증코드가 일치하지 않습니다. 다시 입력해주세요.");
        }
    }


    private void tryJoin() {
        String birth = tfBirthday.getText().trim();
        String phone = tfPhoneNum.getText().trim();

        // 이메일 인증 여부 체크
        if (!isVerified) {
            JOptionPane.showMessageDialog(this, "이메일 인증을 완료해주세요.");
            tfCode.requestFocus();
            return;
        }

        // 생년월일 유효성 검사
        if (birth.equals("생년월일 입력") || !birth.matches("\\d{8}")) {
            JOptionPane.showMessageDialog(this, "생년월일은 숫자 8자리로 입력해주세요. 예: 19990101");
            tfBirthday.requestFocus();
            return;
        }

        // 전화번호 유효성 검사
        if (phone.equals("전화번호 입력('-' 까지 입력)") ||
                !phone.matches("^\\d{2,3}-\\d{3,4}-\\d{4}$")) {
            JOptionPane.showMessageDialog(this, "전화번호는 형식에 맞게 입력해주세요. 예: 010-1234-5678");
            tfPhoneNum.requestFocus();
            return;
        }

        // 사용자 정보 수집
        String email = tfEmail.getText().trim();
        String name = tfName.getText().trim();
        String password = new String(tfPassword.getPassword());
        String gender = maleButton.isSelected() ? "남성" : "여성";

        // YYYYMMDD → YYYY-MM-DD 포맷 변환
        String formattedBirth = birth.substring(0, 4) + "-" + birth.substring(4, 6) + "-" + birth.substring(6, 8);

        // User 객체 생성
        User user = new User(email, password, name, gender, formattedBirth, phone, "USER");

        boolean userInsertresult = UserDAO.insertUser(user);
        if (userInsertresult) {
            Login initialLoginInfo = new Login(user.getEmail(), 0, false);
            boolean loginInfoInsertResult = LoginDAO.insertLoginInfo(initialLoginInfo);
            if(loginInfoInsertResult) {
                JOptionPane.showMessageDialog(this, "회원가입이 성공적으로 완료되었습니다.");
                dispose();
            }
            else {
                // Login 정보 삽입 실패 시 (매우 드물지만, 발생할 경우 처리)
                // User 정보는 이미 삽입되었으므로, 필요한 경우 User 정보도 롤백하는 로직 고려
                JOptionPane.showMessageDialog(this, "회원가입은 성공했으나, 로그인 정보 초기화에 실패했습니다. 관리자에게 문의하세요.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "회원가입 실패 ! (이메일이 중복되었을 수 있습니다.)");
        }
    }

    private void toggleJoinButton() {
        checkPasswordMatch();

        String email = tfEmail.getText().trim();
        String code = tfCode.getText().trim();
        String pw = String.valueOf(tfPassword.getPassword()).trim();
        String pwCheck = String.valueOf(tfPasswordCheck.getPassword()).trim();
        String name = tfName.getText().trim();
        String birth = tfBirthday.getText().trim();
        String phone = tfPhoneNum.getText().trim();

        boolean isValid =
                !email.equals("") && !email.equals("이메일 입력") &&
                        !code.equals("") && !code.equals("인증코드 입력") &&
                        !pw.equals("") && !pw.equals("비밀번호 입력") &&
                        !pwCheck.equals("") && !pwCheck.equals("비밀번호 확인") &&
                        !name.equals("") && !name.equals("이름 입력") &&
                        !birth.equals("") && !birth.equals("생년월일 입력") &&
                        !phone.equals("") && !phone.equals("전화번호 입력('-' 까지 입력)");

        btnJoin.setEnabled(isValid);
    }

    private void updateTimerDisplay() {
        int minutes = remainingTime / 60;
        int seconds = remainingTime % 60;
        lblTimer.setText(String.format("%02d:%02d", minutes, seconds));

        if (remainingTime <= 0) {
            APIUtils.stopAndHideTimer(timer, lblTimer); // APIUtils 호출로 변경
            tfCode.setEnabled(false);
            btnVerify.setEnabled(false);
            tfCode.setBorder(UIUtils.getErrorBorder()); // 에러 테두리 표시
            isVerified = false; // 인증 상태 초기화
            // 이미 타이머 만료 시 메시지를 띄우므로 여기서는 추가 메시지 생략
        }
    }


    // 재전송 확인 대화상자 메서드
    private void askForResend() {
        // 이메일 필드가 비활성화되어 있거나 플레이스홀더가 아닌 실제 이메일이 입력되어 있을 때만 재전송 가능
        if (tfEmail.isEnabled() || tfEmail.getText().trim().isEmpty() || tfEmail.getText().equals("이메일 입력")) {
            JOptionPane.showMessageDialog(this, "인증코드를 재전송하려면 먼저 이메일 입력 필드를 확인하고 '코드 발송' 버튼을 눌러주세요.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "인증코드를 재전송하시겠습니까?",
                "인증코드 재전송",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            sendcode(); // 예'를 선택하면 인증코드 재발송
        }
    }
    // --- 새로운 타이머 관련 메서드 끝 ---
}