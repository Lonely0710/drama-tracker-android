package com.lonely.dramatracker.models;

public class MediaInfo {
    private String documentId; // Appwrite document ID
    private String mediaType; // movie/tv/anime
    private String titleZh;
    private String titleOriginal;
    private String releaseDate;
    private String duration;
    private double ratingDouban;
    private double ratingImdb;
    private double ratingBangumi;
    private String posterUrl;
    private String summary;
    private String staff;

    // 构造函数
    public MediaInfo() {
        this.ratingDouban = -1;
        this.ratingImdb = -1;
        this.ratingBangumi = -1;
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