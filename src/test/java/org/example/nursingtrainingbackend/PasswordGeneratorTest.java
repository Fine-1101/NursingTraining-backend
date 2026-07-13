package org.example.nursingtrainingbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
public class PasswordGeneratorTest {
    
    @Test
    public void generatePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456";
        String hash = encoder.encode(password);
        System.out.println("===========================================");
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt哈希: " + hash);
        System.out.println("===========================================");
        
        // 验证
        boolean matches = encoder.matches(password, hash);
        System.out.println("验证结果: " + (matches ? "成功" : "失败"));
        System.out.println("===========================================");
    }
}
