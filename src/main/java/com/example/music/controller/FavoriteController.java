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
@RequestMapping("/api/favorite")
@CrossOrigin // 允许前端跨域访问（本地演示用）
public class FavoriteController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 收藏一首歌
    @PostMapping("/add")
    public Map<String, Object> addFavorite(@RequestBody Map<String, Object> body) {
        Integer userId = parseInt(body, "userId");
        Integer songId = parseInt(body, "songId");

        // INSERT IGNORE：重复收藏不会报错，靠 UNIQUE(user_id, song_id) 兜底
        int rows = jdbcTemplate.update(
                "INSERT IGNORE INTO t_favorite(user_id, song_id) VALUES (?, ?)", userId, songId);

        Map<String, Object> result = new LinkedHashMap<>();
        if (rows > 0) {
            result.put("code", 200);
            result.put("msg", "收藏成功！");
        } else {
            result.put("code", 201);
            result.put("msg", "这首歌已经收藏过了");
        }
        return result;
    }

    // 取消收藏
    @PostMapping("/remove")
    public Map<String, Object> removeFavorite(@RequestBody Map<String, Object> body) {
        Integer userId = parseInt(body, "userId");
        Integer songId = parseInt(body, "songId");

        int rows = jdbcTemplate.update(
                "DELETE FROM t_favorite WHERE user_id = ? AND song_id = ?", userId, songId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("msg", rows > 0 ? "已取消收藏" : "没有这条收藏记录");
        return result;
    }

    // 查看我的收藏（连 t_song 查出歌曲信息）
    @GetMapping("/{userId}")
    public List<Map<String, Object>> getFavorites(@PathVariable Integer userId) {
        String sql = "SELECT s.song_id AS songId, s.song_name AS songName, " +
                "s.artist_name AS artistName, s.audio_url AS audioUrl, s.cover_url AS coverUrl, " +
                "f.create_time AS favTime " +
                "FROM t_favorite f JOIN t_song s ON f.song_id = s.song_id " +
                "WHERE f.user_id = ? ORDER BY f.create_time DESC";
        return jdbcTemplate.queryForList(sql, userId);
    }

    // 通用取数字参数（复用 CommentController 的校验思路）
    private Integer parseInt(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " 不能为空");
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " 必须是数字");
        }
    }
}