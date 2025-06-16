package jscommunity.mainform;

import com.formdev.flatlaf.FlatLightLaf;
import jscommunity.db.DB;

import javax.swing.UIManager;

public class Main {
	public static void main(String[] args) {
		// 1. FlatLaf 테마 적용
		try {
			UIManager.setLookAndFeel(new FlatLightLaf());
		} catch (Exception ex) {
			System.err.println("❌ FlatLaf 적용 실패: " + ex.getMessage());
		}

		// 2. DB 초기화
		DB.init();

		// 3. UI 시작
		javax.swing.SwingUtilities.invokeLater(() -> {
			new JscommunityUI();
		});
	}
}
