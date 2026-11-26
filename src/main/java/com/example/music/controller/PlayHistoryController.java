package com.example.music.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@CrossOrigin // 允许前端跨域访问（本地演示用）
public class PlayHistoryController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private volatile String cachedPkColumn;
    private volatile boolean pkChecked;

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
        String pkCol = resolveHistoryPkColumn();
        if (pkCol != null) {
            String sql = "SELECT h." + pkCol + " AS historyId, s.song_id AS songId, s.song_name AS songName, " +
                    "s.artist_name AS artistName, s.audio_url AS audioUrl, s.cover_url AS coverUrl, " +
                    "h.play_time AS playTime " +
                    "FROM t_play_history h JOIN t_song s ON h.song_id = s.song_id " +
                    "WHERE h.user_id = ? ORDER BY h.play_time DESC, h." + pkCol + " DESC LIMIT 50";
            return jdbcTemplate.queryForList(sql, userId);
        }
        String sql = "SELECT s.song_id AS songId, s.song_name AS songName, " +
                "s.artist_name AS artistName, s.audio_url AS audioUrl, s.cover_url AS coverUrl, " +
                "h.play_time AS playTime " +
                "FROM t_play_history h JOIN t_song s ON h.song_id = s.song_id " +
                "WHERE h.user_id = ? ORDER BY h.play_time DESC LIMIT 50";
        return jdbcTemplate.queryForList(sql, userId);
    }

    // 删除指定播放记录（POST 方式，与收藏取消接口风格一致）
    @PostMapping("/remove")
    public Map<String, Object> removeHistory(@RequestBody Map<String, Object> body) {
        Integer userId = parseInt(body, "userId");
        Long historyId = parseOptionalLong(body, "historyId");
        Integer songId = parseOptionalInt(body, "songId");
        Object playTimeObj = body.get("playTime");

        return deleteHistoryRecord(userId, historyId, songId, playTimeObj);
    }

    // 删除指定播放记录（RESTful DELETE 方式）
    @DeleteMapping("/{userId}/{historyId}")
    public Map<String, Object> deleteHistoryById(@PathVariable Integer userId, @PathVariable Long historyId) {
        return deleteHistoryRecord(userId, historyId, null, null);
    }

    private Map<String, Object> deleteHistoryRecord(Integer userId, Long historyId, Integer songId, Object playTimeObj) {
        String pkCol = resolveHistoryPkColumn();
        int rows;

        if (pkCol != null && historyId != null) {
            rows = jdbcTemplate.update(
                    "DELETE FROM t_play_history WHERE " + pkCol + " = ? AND user_id = ?",
                    historyId, userId);
        } else {
            if (songId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "historyId 或 songId 不能为空");
            }
            Timestamp playTime = parseOptionalTimestamp(playTimeObj);
            if (playTime != null) {
                rows = jdbcTemplate.update(
                        "DELETE FROM t_play_history WHERE user_id = ? AND song_id = ? AND play_time = ? LIMIT 1",
                        userId, songId, playTime);
            } else {
                rows = jdbcTemplate.update(
                        "DELETE FROM t_play_history WHERE user_id = ? AND song_id = ? ORDER BY play_time DESC LIMIT 1",
                        userId, songId);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", rows > 0 ? 200 : 404);
        result.put("msg", rows > 0 ? "已删除该播放记录" : "未找到对应的播放记录");
        result.put("deleted", rows > 0);
        return result;
    }

    private String resolveHistoryPkColumn() {
        if (pkChecked) {
            return cachedPkColumn;
        }
        synchronized (this) {
            if (pkChecked) {
                return cachedPkColumn;
            }
            try {
                String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_play_history' " +
                        "AND (COLUMN_KEY = 'PRI' OR EXTRA LIKE '%auto_increment%') " +
                        "ORDER BY CASE WHEN EXTRA LIKE '%auto_increment%' THEN 0 ELSE 1 END, ORDINAL_POSITION";
                List<String> cols = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("COLUMN_NAME"));
                for (String col : cols) {
                    if (col != null
                            && !"user_id".equalsIgnoreCase(col)
                            && !"song_id".equalsIgnoreCase(col)
                            && !"play_time".equalsIgnoreCase(col)
                            && col.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                        cachedPkColumn = col;
                        break;
                    }
                }
                pkChecked = true;
            } catch (Exception e) {
                // 查询元数据异常时不缓存，避免临时连接失败锁死
                return null;
            }
            return cachedPkColumn;
        }
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

    private Integer parseOptionalInt(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " 必须是数字");
        }
    }

    private Long parseOptionalLong(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " 必须是数字");
        }
    }

    private Timestamp parseOptionalTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Timestamp.from(OffsetDateTime.parse(text).toInstant());
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Timestamp.from(Instant.parse(text));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(text));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}