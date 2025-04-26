package com.lonely.dramatracker.api.impl;

import com.lonely.dramatracker.api.ApiService;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.utils.BangumiCrawler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiServiceImpl implements ApiService {
    private final BangumiCrawler bangumiCrawler;

    public ApiServiceImpl() {
        this.bangumiCrawler = new BangumiCrawler();
    }

    @Override
    public CompletableFuture<List<SearchResult>> search(String keyword, String type) {
        try {
            return bangumiCrawler.search(keyword);
        } catch (Exception e) {
            CompletableFuture<List<SearchResult>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("搜索失败", e));
            return future;
        }
    }
} 