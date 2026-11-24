package com.example.music.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin // 允许前端跨域访问（本地演示用）
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // BCrypt 编码器（哈希单向不可逆，只能 matches 比对，不能解密）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 登录接口：用 BCrypt 比对，不再明文比较
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 参数校验
        if (username == null || username.trim().isEmpty() ||
                password == null || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名和密码不能为空");
        }

        // 从数据库取出该用户的密码（现在应是 BCrypt 哈希）
        String sql = "SELECT password FROM t_user WHERE username = ?";
        List<String> pwds = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getString("password"), username.trim());
        String storedHash = pwds.isEmpty() ? null : pwds.get(0);

        Map<String, Object> result = new LinkedHashMap<>();
        // BCrypt.matches：内部会重新加盐哈希后再比较，结果才是布尔值
        if (storedHash != null && passwordEncoder.matches(password, storedHash)) {
            result.put("code", 200);
            result.put("msg", "登录成功！欢迎 " + username);
            result.put("data", true);
        } else {
            result.put("code", 401);
            result.put("msg", "登录失败：用户名或密码错误");
            result.put("data", false);
        }
        return result;
    }
}