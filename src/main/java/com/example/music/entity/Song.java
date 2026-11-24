package com.example.music.entity;

public class Song {
    private Integer songId;
    private String songName;
    private String artistName;
    private String audioUrl;
    private String coverUrl;

    // 快速生成 Getter 和 Setter：在代码里右键 -> Generate -> Getter and Setter -> 全选
    public Integer getSongId() { return songId; }
    public void setSongId(Integer songId) { this.songId = songId; }
    public String getSongName() { return songName; }
    public void setSongName(String songName) { this.songName = songName; }
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
}