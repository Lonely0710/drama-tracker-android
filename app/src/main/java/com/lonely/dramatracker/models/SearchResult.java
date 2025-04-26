package com.lonely.dramatracker.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

public class SearchResult implements Parcelable {
    // media_source相关字段
    private final String sourceType; // douban/imdb/bgm
    private final String sourceId;
    private final String sourceUrl;
    
    // media相关字段
    private final String mediaType; // movie/tv/anime
    private final String titleZh;
    private final String titleOriginal;
    private final String releaseDate; // 详细首播年月日
    private final String duration; // 时长（分钟/话数）
    private final String year; // 年份(从releaseDate提取)
    private final String posterUrl;
    private final String summary; // 剧情简介
    private final String staff; // 制作人员
    
    // 评分相关
    private final double rating; // 综合评分(用于兼容旧版本)
    private final double ratingDouban; // 豆瓣评分
    private final double ratingImdb; // IMDb评分
    private final double ratingBangumi; // Bangumi评分
    
    // 状态
    private boolean isCollected;

    private SearchResult(Builder builder) {
        this.sourceType = builder.sourceType;
        this.sourceId = builder.sourceId;
        this.sourceUrl = builder.sourceUrl;
        this.titleZh = builder.titleZh;
        this.titleOriginal = builder.titleOriginal;
        this.releaseDate = builder.releaseDate;
        this.duration = builder.duration;
        this.year = builder.year;
        this.posterUrl = builder.posterUrl;
        this.mediaType = builder.mediaType;
        this.summary = builder.summary;
        this.staff = builder.staff;
        this.rating = builder.rating;
        this.ratingDouban = builder.ratingDouban;
        this.ratingImdb = builder.ratingImdb;
        this.ratingBangumi = builder.ratingBangumi;
        this.isCollected = builder.isCollected;
    }

    protected SearchResult(Parcel in) {
        sourceType = in.readString();
        sourceId = in.readString();
        sourceUrl = in.readString();
        titleZh = in.readString();
        titleOriginal = in.readString();
        releaseDate = in.readString();
        duration = in.readString();
        year = in.readString();
        posterUrl = in.readString();
        mediaType = in.readString();
        summary = in.readString();
        staff = in.readString();
        rating = in.readDouble();
        ratingDouban = in.readDouble();
        ratingImdb = in.readDouble();
        ratingBangumi = in.readDouble();
        isCollected = in.readByte() != 0;
    }

    public static final Creator<SearchResult> CREATOR = new Creator<SearchResult>() {
        @Override
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        @Override
        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

    // --- Getters ---
    
    // media_source getters
    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    // media getters
    public String getMediaType() {
        return mediaType;
    }
    
    public String getTitleZh() {
        return titleZh;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }
    
    public String getReleaseDate() {
        return releaseDate;
    }
    
    public String getDuration() {
        return duration;
    }

    public String getYear() {
        return year;
    }

    public String getPosterUrl() {
        return posterUrl;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public String getStaff() {
        return staff;
    }

    // 评分getters
    public double getRating() {
        return rating;
    }
    
    public double getRatingDouban() {
        return ratingDouban;
    }
    
    public double getRatingImdb() {
        return ratingImdb;
    }
    
    public double getRatingBangumi() {
        return ratingBangumi;
    }

    // 状态getters/setters
    public boolean isCollected() {
        return isCollected;
    }

    public void setCollected(boolean collected) {
        isCollected = collected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return Objects.equals(sourceType, that.sourceType) && 
               Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceType, sourceId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(sourceType);
        dest.writeString(sourceId);
        dest.writeString(sourceUrl);
        dest.writeString(titleZh);
        dest.writeString(titleOriginal);
        dest.writeString(releaseDate);
        dest.writeString(duration);
        dest.writeString(year);
        dest.writeString(posterUrl);
        dest.writeString(mediaType);
        dest.writeString(summary);
        dest.writeString(staff);
        dest.writeDouble(rating);
        dest.writeDouble(ratingDouban);
        dest.writeDouble(ratingImdb);
        dest.writeDouble(ratingBangumi);
        dest.writeByte((byte) (isCollected ? 1 : 0));
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "sourceType='" + sourceType + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", titleZh='" + titleZh + '\'' +
                ", titleOriginal='" + titleOriginal + '\'' +
                ", releaseDate='" + releaseDate + '\'' +
                ", duration='" + duration + '\'' +
                ", year='" + year + '\'' +
                ", posterUrl='" + posterUrl + '\'' +
                ", summary='" + (summary != null ? summary.substring(0, Math.min(summary.length(), 30)) + "..." : "null") + '\'' +
                ", staff='" + staff + '\'' +
                ", rating=" + rating +
                ", ratingDouban=" + ratingDouban +
                ", ratingImdb=" + ratingImdb +
                ", ratingBangumi=" + ratingBangumi +
                ", isCollected=" + isCollected +
                '}';
    }

    public static class Builder {
        // media_source相关字段
        private String sourceType;
        private String sourceId;
        private String sourceUrl;
        
        // media相关字段
        private String mediaType;
        private String titleZh;
        private String titleOriginal;
        private String releaseDate;
        private String duration;
        private String year;
        private String posterUrl;
        private String summary;
        private String staff;
        
        // 评分相关
        private double rating = -1;
        private double ratingDouban = -1;
        private double ratingImdb = -1;
        private double ratingBangumi = -1;
        
        // 状态
        private boolean isCollected;

        // --- Setters ---
        
        // media_source setters
        public Builder setSourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder setSourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder setSourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        // media setters
        public Builder setMediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }
        
        public Builder setTitleZh(String titleZh) {
            this.titleZh = titleZh;
            return this;
        }

        public Builder setTitleOriginal(String titleOriginal) {
            this.titleOriginal = titleOriginal;
            return this;
        }
        
        public Builder setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
            return this;
        }
        
        public Builder setDuration(String duration) {
            this.duration = duration;
            return this;
        }

        public Builder setYear(String year) {
            this.year = year;
            return this;
        }

        public Builder setPosterUrl(String posterUrl) {
            this.posterUrl = posterUrl;
            return this;
        }
        
        public Builder setSummary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public Builder setStaff(String staff) {
            this.staff = staff;
            return this;
        }

        // 评分setters
        public Builder setRating(double rating) {
            this.rating = rating;
            return this;
        }
        
        public Builder setRatingDouban(double ratingDouban) {
            this.ratingDouban = ratingDouban;
            return this;
        }
        
        public Builder setRatingImdb(double ratingImdb) {
            this.ratingImdb = ratingImdb;
            return this;
        }
        
        public Builder setRatingBangumi(double ratingBangumi) {
            this.ratingBangumi = ratingBangumi;
            return this;
        }

        // 状态setters
        public Builder setCollected(boolean collected) {
            isCollected = collected;
            return this;
        }

        public SearchResult build() {
            return new SearchResult(this);
        }
    }
} 