package jp.co.sss.lms.util;

import org.junit.jupiter.api.Test;

import jp.co.sss.lms.util.PasswordUtil;

public class PasswordUtilTest {

    @Test
    public void testPasswordHash() {
        PasswordUtil util = new PasswordUtil();

        String userId = "16"; // 受講生AA01のID
        String plain = "systemsss"; // tisdbのパスワード

        String hash = util.getSaltedAndStrechedPassword(plain, userId);
        System.out.println("ハッシュ結果: " + hash);
    }
}
