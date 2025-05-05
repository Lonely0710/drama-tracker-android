package com.lonely.dramatracker.models;

public enum WebSite {
    BANGUMI("Bangumi", "https://bgm.tv"),
    DOUBAN("豆瓣电影", "https://movie.douban.com"),
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