package com.lonely.dramatracker.api.impl;

import com.lonely.dramatracker.api.ApiService;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.utils.BangumiCrawler;
import com.lonely.dramatracker.utils.DoubanCrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiServiceImpl implements ApiService {
    private final BangumiCrawler bangumiCrawler;
    private final DoubanCrawler doubanCrawler;

    public ApiServiceImpl() {
        this.bangumiCrawler = new BangumiCrawler();
        this.doubanCrawler = new DoubanCrawler();
    }

    @Override
    public CompletableFuture<List<SearchResult>> search(String keyword, String type) {
        try {
            // 根据搜索框类型选择不同的搜索来源
            // type参数在这里表示搜索框类型，而不是媒体类型
            
            // anime_search_bar - 使用Bangumi搜索
            if ("anime".equals(type)) {
                return bangumiCrawler.search(keyword);
            } 
            // movie_search_bar - 使用豆瓣搜索
            else if ("movie".equals(type) || "tv".equals(type)) {
                return doubanCrawler.search(keyword);
            } 
            // 默认情况，使用豆瓣搜索
            else {
                return doubanCrawler.search(keyword);
            }
        } catch (Exception e) {
            CompletableFuture<List<SearchResult>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("搜索失败", e));
            return future;
        }
    }
} 