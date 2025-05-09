package com.lonely.dramatracker.models;

/**
 * 每日放送动漫信息
 */
public class DailyAnime {
    private String sourceId;      // Bangumi ID
    private String titleZh;       // 中文标题
    private String titleOriginal; // 原始标题
    private String posterUrl;     // 海报URL
    private String sourceUrl;     // 源站URL

    public DailyAnime() {
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
} 