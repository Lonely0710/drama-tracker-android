package com.lonely.dramatracker.models;

public class RecordItem {
    private String mediaId;
    private String title;
    private String subtitle;
    private String posterUrl;
    private String mediaType;
    private String rating;       // 通用评分展示字段
    private String ratingBangumi; // Bangumi评分
    private String ratingDouban;  // 豆瓣评分
    private String ratingImdb;    // IMDb评分
    private String year;
    private String duration;
    private boolean watched;

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
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

    public String getRatingBangumi() {
        return ratingBangumi;
    }

    public void setRatingBangumi(String ratingBangumi) {
        this.ratingBangumi = ratingBangumi;
    }

    public String getRatingDouban() {
        return ratingDouban;
    }

    public void setRatingDouban(String ratingDouban) {
        this.ratingDouban = ratingDouban;
    }

    public String getRatingImdb() {
        return ratingImdb;
    }

    public void setRatingImdb(String ratingImdb) {
        this.ratingImdb = ratingImdb;
    }

    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }
} 