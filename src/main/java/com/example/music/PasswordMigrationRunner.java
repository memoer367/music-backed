package com.example.music;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public PasswordMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // 启动时把仍是明文的旧密码升级为 BCrypt 哈希。
        // 幂等：BCrypt 哈希以 "$2" 开头，已经加密过的会自动跳过，重复启动不会二次加密。
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT user_id, password FROM t_user");
        for (Map<String, Object> u : users) {
            int userId = ((Number) u.get("user_id")).intValue();
            String pwd = (String) u.get("password");
            if (pwd != null && !pwd.startsWith("$2")) {
                String hash = encoder.encode(pwd);
                jdbcTemplate.update("UPDATE t_user SET password = ? WHERE user_id = ?", hash, userId);
                System.out.println("已迁移用户 " + userId + " 的密码为 BCrypt 哈希");
            }
        }
    }
}