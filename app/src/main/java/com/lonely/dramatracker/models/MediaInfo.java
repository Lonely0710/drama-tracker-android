package com.lonely.dramatracker.models;

public class MediaInfo {
    // 常量定义
    public static final String TYPE_MOVIE = "movie";
    public static final String TYPE_TV = "tv";
    public static final String TYPE_ANIME = "anime";
    
    private long id; // 内部ID，用于高分内容排名
    private int rank; // 排名，用于展示
    private String documentId; // Appwrite document ID
    private String mediaType; // movie/tv/anime
    private String titleZh;
    private String titleOriginal;
    private String releaseDate;
    private String year; // 年份
    private String duration;
    private double ratingDouban;
    private double ratingImdb;
    private double ratingBangumi;
    private float rating; // 通用评分字段，用于高分推荐
    private String posterUrl;
    private String summary;
    private String staff;

    // 构造函数
    public MediaInfo() {
        this.ratingDouban = -1;
        this.ratingImdb = -1;
        this.ratingBangumi = -1;
        this.rating = -1f;
    }
    
    // 获取媒体名称（优先返回中文标题）
    public String getMediaName() {
        return titleZh != null && !titleZh.isEmpty() ? titleZh : titleOriginal;
    }
    
    // ID相关
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    // 排名相关
    public int getRank() {
        return rank;
    }
    
    public void setRank(int rank) {
        this.rank = rank;
    }

    // Getters and Setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getTitleZh() {
        return titleZh;
    }

    public void setTitleZh(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    public String getYear() {
        return year;
    }
    
    public void setYear(String year) {
        this.year = year;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public double getRatingDouban() {
        return ratingDouban;
    }

    public void setRatingDouban(double ratingDouban) {
        this.ratingDouban = ratingDouban;
    }

    public double getRatingImdb() {
        return ratingImdb;
    }

    public void setRatingImdb(double ratingImdb) {
        this.ratingImdb = ratingImdb;
    }

    public double getRatingBangumi() {
        return ratingBangumi;
    }

    public void setRatingBangumi(double ratingBangumi) {
        this.ratingBangumi = ratingBangumi;
    }
    
    public float getRating() {
        return rating;
    }
    
    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStaff() {
        return staff;
    }

    public void setStaff(String staff) {
        this.staff = staff;
    }
} 