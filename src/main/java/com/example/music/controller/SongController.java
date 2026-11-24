package com.example.music.controller;

import com.example.music.entity.Song;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin // 允许前端跨域访问（本地演示用）
public class SongController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 前端请求路径：http://localhost:8080/api/playlist/1
    // 改造点：多表 JOIN 已封装进视图 v_playlist_detail，这里直接查视图
    @GetMapping("/playlist/{id}")
    public List<Song> getPlaylistSongs(@PathVariable Integer id) {
        // 视图替我们完成了 playlist -> playlist_song -> song 的三表 JOIN
        // 注意：仍要 AS 别名，因为 BeanPropertyRowMapper 不会把下划线列名映射成驼峰属性
        String sql = "SELECT song_id AS songId, song_name AS songName, " +
                "artist_name AS artistName, audio_url AS audioUrl, cover_url AS coverUrl " +
                "FROM v_playlist_detail WHERE playlist_id = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Song.class), id);
    }
}