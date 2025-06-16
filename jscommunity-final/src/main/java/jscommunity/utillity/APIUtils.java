package jscommunity.utillity;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*; // Timer, JLabel을 위해 Swing 임포트 추가
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * APIUtils 클래스는 애플리케이션의 API 및 타이머 관련 설정 값을
 * 'config.properties' 파일에서 읽어와 제공하며,
 * 공통적인 API 호출 로직 (예: 이메일 발송)과
 * 타이머 제어 유틸리티 메서드도 포함
 */
public class APIUtils {
    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "config.properties";
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().build();
    private static final Gson gson = new Gson(); // Gson 인스턴스도 static으로 관리

    static {
        try (InputStream input = APIUtils.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println(CONFIG_FILE + " 파일이 존재하지 않습니다.");
                throw new RuntimeException("설정 파일을 찾을 수 없습니다: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException ex) {
            System.err.println("설정 파일을 읽는 중 오류가 발생했습니다: " + ex.getMessage());
            throw new RuntimeException("설정 파일 로딩에 실패했습니다.", ex);
        }
    }


    /**
     * Brevo API 키를 반환
     * 'brevo.api.key' 속성이 설정 파일에 없으면 빈 문자열을 반환
     * @return Brevo API 키 문자열
     */
    public static String getBrevoApiKey() {
        return properties.getProperty("brevo.api.key", "");
    }

    /**
     * Brevo 이메일 API의 URL을 반환
     * 'brevo.email.api.url' 속성이 설정 파일에 없으면 빈 문자열을 반환
     * @return Brevo 이메일 API URL 문자열
     */
    public static String getBrevoEmailApiUrl() {
        return properties.getProperty("brevo.email.api.url", "");
    }

    /**
     * Brevo 이메일 발송 시 사용될 발신자 이메일 주소를 반환
     * 'brevo.sender.email' 속성이 설정 파일에 없으면 빈 문자열을 반환
     * @return 발신자 이메일 주소 문자열
     */
    public static String getBrevoSenderEmail() {
        return properties.getProperty("brevo.sender.email", "");
    }

    /**
     * Brevo 이메일 발송 시 사용될 발신자 이름을 반환
     * 'brevo.sender.name' 속성이 설정 파일에 없으면 빈 문자열을 반환
     * @return 발신자 이름 문자열
     */
    public static String getBrevoSenderName() {
        return properties.getProperty("brevo.sender.name", "");
    }

    /**
     * 인증 코드 유효 시간을 밀리초(ms) 단위로 반환
     * 'config.properties' 파일에서 'verification.code.valid.duration.seconds' 값을 읽음
     * 설정 값이 없거나 숫자로 변환할 수 없는 경우 기본값인 180초 (3분)를 사용
     * @return 인증 코드의 유효 시간
     */
    public static long getVerificationCodeValidDurationMillis() {
        String durationStr = properties.getProperty("verification.code.valid.duration.seconds", "180");
        try {
            return Long.parseLong(durationStr) * 1000L;
        } catch (NumberFormatException e) {
            System.err.println("값이 숫자로 설정되어 있지 않아, 기본값인 180초로 설정");
            return 180 * 1000L; // 파싱 실패 시 기본값 (3분)
        }
    }
    /**
     * Brevo API를 사용하여 이메일을 발송합니다.
     * 발신자 정보는 config.properties에서 APIUtils를 통해 가져옵니다.
     *
     * @param recipientEmail 수신자 이메일 주소
     * @param subject        이메일 제목
     * @param htmlContent    이메일 HTML 본문 내용
     * @throws IOException API 호출 실패 시 발생
     */
    public static void sendEmailViaBrevo(String recipientEmail, String RecipienName, String subject, String htmlContent) throws IOException {
        JsonObject requestBody = new JsonObject();
        JsonObject sender = new JsonObject();
        sender.addProperty("name", getBrevoSenderName());
        sender.addProperty("email", getBrevoSenderEmail());

        JsonObject to = new JsonObject();
        to.addProperty("email", recipientEmail);
        to.addProperty("name", RecipienName); // 수신자 이름은 이메일로 설정 (필요에 따라 변경 가능)

        requestBody.add("sender", sender);
        requestBody.add("to", gson.toJsonTree(new JsonObject[]{to}));
        requestBody.addProperty("subject", subject);
        requestBody.addProperty("htmlContent", htmlContent);

        RequestBody body = RequestBody.create(gson.toJson(requestBody), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(getBrevoEmailApiUrl())
                .post(body)
                .addHeader("accept", "application/json")
                .addHeader("api-key", getBrevoApiKey())
                .addHeader("content-type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "응답 본문이 없습니다.";
                throw new IOException("Brevo API 호출 실패: HTTP " + response.code() + " - " + responseBody);
            }
            System.out.println("Brevo 이메일 발송 성공.");
        }
    }

    /**
     * 지정된 Timer를 시작하고 연결된 JLabel을 출력
     *
     * @param timer      제어할 Timer 객체
     * @param timerLabel 타이머 상태를 표시할 JLabel 객체
     */
    public static void startAndShowTimer(Timer timer, JLabel timerLabel) {
        if (!timer.isRunning()) {
            timer.start();
        }
        timerLabel.setVisible(true);
    }

    /**
     * 지정된 Timer를 중지하고 연결된 JLabel을 숨김
     *
     * @param timer      제어할 Timer 객체
     * @param timerLabel 타이머 상태를 표시할 JLabel 객체
     */
    public static void stopAndHideTimer(Timer timer, JLabel timerLabel) {
        if (timer.isRunning()) {
            timer.stop();
        }
        timerLabel.setVisible(false);
    }
}
