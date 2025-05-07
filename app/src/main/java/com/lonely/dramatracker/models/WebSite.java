package com.lonely.dramatracker.models;

/**
 * 网站枚举
 */
public enum WebSite {
    DOUBAN("豆瓣", "https://m.douban.com"),
    IMDB("IMDb", "https://www.imdb.com"),
    BANGUMI("Bangumi", "https://bgm.tv"),
    MAOYAN("猫眼", "https://m.maoyan.com"),
    TMDB("TMDb", "https://www.themoviedb.org");

    private final String name;
    private final String url;

    WebSite(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}