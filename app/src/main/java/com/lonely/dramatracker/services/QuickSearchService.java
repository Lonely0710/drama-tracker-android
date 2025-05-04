package com.lonely.dramatracker.services;

import android.util.Log;

import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.utils.BangumiCrawler;
import com.lonely.dramatracker.utils.DoubanCrawler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 快速搜索服务
 * 用于底部导航栏"添加"按钮，实现多来源同时搜索并只返回最匹配的结果
 */
public class QuickSearchService {
    private static final String TAG = "QuickSearchService";
    
    private final DoubanCrawler doubanCrawler;
    private final BangumiCrawler bangumiCrawler;
    // 未来可添加 IMDBCrawler imdbCrawler;
    
    public QuickSearchService() {
        this.doubanCrawler = new DoubanCrawler();
        this.bangumiCrawler = new BangumiCrawler();
    }
    
    /**
     * 从多个来源同时搜索，并只返回每个来源的第一个结果
     * @param keyword 搜索关键词
     * @return 包含每个来源最匹配结果的Map，键为来源类型
     */
    public CompletableFuture<Map<String, SearchResult>> quickSearch(String keyword) {
        Log.d(TAG, "开始快速搜索: " + keyword);
        
        // 创建多个并行搜索任务
        CompletableFuture<SearchResult> doubanFuture = doubanCrawler.search(keyword)
            .thenApply(results -> {
                Log.d(TAG, "豆瓣搜索完成，结果数量: " + (results != null ? results.size() : 0));
                return results.isEmpty() ? null : results.get(0);
            })
            .exceptionally(e -> {
                Log.e(TAG, "豆瓣搜索失败: " + e.getMessage(), e);
                return null;
            });
            
        CompletableFuture<SearchResult> bangumiFuture = bangumiCrawler.search(keyword)
            .thenApply(results -> {
                Log.d(TAG, "Bangumi搜索完成，结果数量: " + (results != null ? results.size() : 0));
                return results.isEmpty() ? null : results.get(0);
            })
            .exceptionally(e -> {
                Log.e(TAG, "Bangumi搜索失败: " + e.getMessage(), e);
                return null;
            });
            
        // 等待所有搜索完成并整合结果
        return CompletableFuture.allOf(doubanFuture, bangumiFuture)
            .thenApply(v -> {
                Map<String, SearchResult> results = new HashMap<>();
                try {
                    SearchResult doubanResult = doubanFuture.get();
                    SearchResult bangumiResult = bangumiFuture.get();
                    
                    if (doubanResult != null) {
                        results.put("douban", doubanResult);
                        Log.d(TAG, "添加豆瓣结果: " + doubanResult.getTitleZh());
                    }
                    
                    if (bangumiResult != null) {
                        results.put("bgm", bangumiResult);
                        Log.d(TAG, "添加Bangumi结果: " + bangumiResult.getTitleZh());
                    }
                    
                    Log.d(TAG, "快速搜索完成，共有结果: " + results.size());
                } catch (Exception e) {
                    Log.e(TAG, "合并搜索结果失败: " + e.getMessage(), e);
                }
                return results;
            })
            .exceptionally(e -> {
                Log.e(TAG, "快速搜索失败: " + e.getMessage(), e);
                return new HashMap<>();
            });
    }
} 