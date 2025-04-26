package com.lonely.dramatracker.services.impl;

import com.google.gson.Gson;
import com.lonely.dramatracker.api.ApiService;
import com.lonely.dramatracker.models.MediaInfo;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.services.SearchService;

import java.util.concurrent.CompletableFuture;

public class SearchServiceImpl implements SearchService {
    private static final String TAG = "SearchServiceImpl";
    
    private final ApiService apiService;
    private final Gson gson = new Gson();
    
    public SearchServiceImpl(ApiService apiService) {
        this.apiService = apiService;
    }
    
    @Override
    public void search(String keyword, String type, SearchCallback callback) {
        apiService.search(keyword, type).thenAccept(results -> {
            // 收藏状态相关逻辑留空
            callback.onSearchComplete(results);
        });
    }
    
    @Override
    public void search(String keyword, String type, JsonSearchCallback callback) {
        apiService.search(keyword, type).thenAccept(results -> {
            // 将结果转换为JSON字符串
            String jsonResults = gson.toJson(results);
            callback.onSearchComplete(jsonResults);
        });
    }

    @Override
    public void addToCollection(SearchResult result) {
        // 收藏逻辑留空
    }

    @Override
    public void removeFromCollection(SearchResult result) {
        // 取消收藏逻辑留空
    }

    @Override
    public CompletableFuture<MediaInfo> getMediaInfo(String sourceType, String sourceId) {
        throw new UnsupportedOperationException("getMediaInfo not implemented");
    }

    @Override
    public CompletableFuture<Boolean> isCollected(String mediaId) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> collect(String mediaId) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> uncollect(String mediaId) {
        return CompletableFuture.completedFuture(false);
    }
} 