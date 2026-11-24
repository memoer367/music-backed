package com.example.music.controller;

import com.example.music.entity.Song;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin // 允许前端跨域访问
public class SongController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 前端请求路径：http://localhost:8080/api/playlist/1
    @GetMapping("/playlist/{id}")
    public List<Song> getPlaylistSongs(@PathVariable Integer id) {
        // 多表联合查询（关联歌单、中间表、歌曲表）
        String sql = "SELECT s.song_id, s.song_name, s.artist_name, s.audio_url, s.cover_url " +
                "FROM t_playlist_song ps " +
                "JOIN t_song s ON ps.song_id = s.song_id " +
                "WHERE ps.playlist_id = ?";
        // 自动将查出的数据封装成 Song 列表
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Song.class), id);
    }
}