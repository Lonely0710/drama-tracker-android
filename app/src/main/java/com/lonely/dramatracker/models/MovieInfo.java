package com.lonely.dramatracker.models;

import java.util.ArrayList;
import java.util.List;

/**
 * 猫眼电影信息模型类
 */
public class MovieInfo {
    private int id;                 // 电影ID
    private String movieName;       // 电影名称
    private String originalName;    // 原始名称
    private String releaseDate;     // 上映日期
    private double score;           // 评分
    private String poster;          // 海报URL
    private List<String> actors;    // 主演列表
    private String summary;         // 电影简介
    private List<String> genres;    // 电影类型
    private boolean isNew;          // 是否新上映
    private String duration;        // 片长
    private String wish;            // 想看人数
    
    public MovieInfo() {
        // 空构造函数
        // 初始化集合，避免空指针异常
        this.actors = new ArrayList<>();
        this.genres = new ArrayList<>();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public List<String> getActors() {
        return actors != null ? actors : new ArrayList<>();
    }

    public void setActors(List<String> actors) {
        this.actors = actors != null ? actors : new ArrayList<>();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getGenres() {
        return genres != null ? genres : new ArrayList<>();
    }

    public void setGenres(List<String> genres) {
        this.genres = genres != null ? genres : new ArrayList<>();
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getWish() {
        return wish;
    }

    public void setWish(String wish) {
        this.wish = wish;
    }
    
    /**
     * 获取格式化的类型字符串
     */
    public String getFormattedGenres() {
        if (genres == null || genres.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < genres.size(); i++) {
            sb.append(genres.get(i));
            if (i < genres.size() - 1) {
                sb.append(" / ");
            }
        }
        return sb.toString();
    }
    
    /**
     * 获取格式化的主演字符串
     */
    public String getFormattedActors() {
        if (actors == null || actors.isEmpty()) {
            return "暂无主演信息";
        }
        
        StringBuilder sb = new StringBuilder("主演：");
        for (int i = 0; i < Math.min(actors.size(), 3); i++) {
            sb.append(actors.get(i));
            if (i < Math.min(actors.size(), 3) - 1) {
                sb.append(" / ");
            }
        }
        return sb.toString();
    }
} 