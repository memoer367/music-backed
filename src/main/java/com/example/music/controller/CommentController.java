package com.example.music.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
@CrossOrigin // 允许前端跨域访问（本地演示用）
public class CommentController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 获取某首歌的评论 —— 改造点：改用视图 v_comment_detail（评论和用户名已 JOIN 好）
    @GetMapping("/{songId}")
    public List<Map<String, Object>> getComments(@PathVariable Integer songId) {
        String sql = "SELECT content, create_time, username " +
                "FROM v_comment_detail WHERE song_id = ? ORDER BY create_time DESC";
        return jdbcTemplate.queryForList(sql, songId);
    }

    // 发表评论 —— 改造点：改用存储过程 sp_add_comment（事务在过程内，comment_count 由触发器维护）
    @PostMapping("/add")
    public Map<String, Object> addComment(@RequestBody Map<String, Object> body) {
        // 参数校验：防止缺字段/非数字直接 500
        if (body.get("songId") == null || body.get("userId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "songId 和 userId 不能为空");
        }
        Integer songId;
        Integer userId;
        try {
            songId = Integer.valueOf(body.get("songId").toString());
            userId = Integer.valueOf(body.get("userId").toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "songId / userId 必须是数字");
        }

        String content = body.get("content") == null ? "" : body.get("content").toString().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论内容过长（最多 500 字）");
        }

        // 调用数据库存储过程（事务已在过程内处理）
        jdbcTemplate.update("CALL sp_add_comment(?, ?, ?)", songId, userId, content);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("msg", "评论成功！");
        return result;
    }
}