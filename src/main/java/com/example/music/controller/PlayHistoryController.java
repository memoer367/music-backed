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
@RequestMapping("/api/history")
@CrossOrigin // 允许前端跨域访问（本地演示用）
public class PlayHistoryController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 记录一次播放：写历史 + 播放次数 +1
    @PostMapping("/add")
    public Map<String, Object> addHistory(@RequestBody Map<String, Object> body) {
        Integer userId = parseInt(body, "userId");
        Integer songId = parseInt(body, "songId");

        // 1. 插入播放历史
        jdbcTemplate.update(
                "INSERT INTO t_play_history(user_id, song_id) VALUES (?, ?)", userId, songId);
        // 2. 播放次数 +1（t_song 里已有 play_count 字段）
        jdbcTemplate.update(
                "UPDATE t_song SET play_count = play_count + 1 WHERE song_id = ?", songId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("msg", "已记录播放历史");
        return result;
    }

    // 查看播放历史（最近 50 条，连 t_song 查出歌曲信息）
    @GetMapping("/{userId}")
    public List<Map<String, Object>> getHistory(@PathVariable Integer userId) {
        String sql = "SELECT s.song_id AS songId, s.song_name AS songName, " +
                "s.artist_name AS artistName, s.audio_url AS audioUrl, s.cover_url AS coverUrl, " +
                "h.play_time AS playTime " +
                "FROM t_play_history h JOIN t_song s ON h.song_id = s.song_id " +
                "WHERE h.user_id = ? ORDER BY h.play_time DESC LIMIT 50";
        return jdbcTemplate.queryForList(sql, userId);
    }

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