package com.lonely.dramatracker.services;

import android.util.Log;

import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.utils.BangumiCrawler;
import com.lonely.dramatracker.utils.DoubanCrawler;
import com.lonely.dramatracker.utils.TMDbCrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 快速搜索服务
 * 用于底部导航栏"添加"按钮，实现多来源同时搜索并只返回最匹配的结果
 */
public class QuickSearchService {
    private static final String TAG = "QuickSearchService";
    
    private final DoubanCrawler doubanCrawler;
    private final BangumiCrawler bangumiCrawler;
    private final TMDbCrawler tmdbCrawler;
    
    public QuickSearchService() {
        this.doubanCrawler = new DoubanCrawler();
        this.bangumiCrawler = new BangumiCrawler();
        this.tmdbCrawler = new TMDbCrawler();
    }
    
    /**
     * 从多个来源同时搜索，并只返回每个来源的第一个结果
     * @param keyword 搜索关键词
     * @return 包含每个来源最匹配结果的Map，键为来源类型 ("douban", "bangumi", "tmdb")
     */
    public CompletableFuture<Map<String, SearchResult>> searchFromMultipleSources(String keyword) {
        Log.d(TAG, "开始多来源快速搜索: " + keyword);
        
        // 修改单个 Future 以处理异常并返回空列表
        CompletableFuture<List<SearchResult>> doubanFuture = doubanCrawler.search(keyword)
            .exceptionally(e -> {
                Log.e(TAG, "豆瓣快速搜索失败: " + e.getMessage());
                return new ArrayList<>(); // 失败时返回空列表
            });
        CompletableFuture<List<SearchResult>> bangumiFuture = bangumiCrawler.search(keyword)
            .exceptionally(e -> {
                Log.e(TAG, "Bangumi快速搜索失败: " + e.getMessage());
                return new ArrayList<>(); // 失败时返回空列表
            });
        CompletableFuture<List<SearchResult>> tmdbFuture = tmdbCrawler.search(keyword)
            .exceptionally(e -> {
                Log.e(TAG, "TMDb快速搜索失败: " + e.getMessage());
                return new ArrayList<>(); // 失败时返回空列表
            });

        // 合并这些经过异常处理的 Future
        List<CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();
        futures.add(doubanFuture);
        futures.add(bangumiFuture);
        futures.add(tmdbFuture);

        // allOf 现在应该总是成功完成（除非allOf本身出错）
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApplyAsync(v -> { // 使用 thenApplyAsync 避免阻塞
                Map<String, SearchResult> firstResults = new HashMap<>();
                
                // 处理豆瓣结果 (join现在是安全的，因为异常已被处理)
                List<SearchResult> doubanResults = doubanFuture.join();
                if (!doubanResults.isEmpty()) {
                    firstResults.put("douban", doubanResults.get(0));
                    Log.d(TAG, "获取到豆瓣首个结果: " + doubanResults.get(0).getTitleZh());
                } else {
                    Log.d(TAG, "豆瓣无结果或失败");
                }

                // 处理Bangumi结果
                List<SearchResult> bangumiResults = bangumiFuture.join();
                if (!bangumiResults.isEmpty()) {
                    firstResults.put("bangumi", bangumiResults.get(0));
                    Log.d(TAG, "获取到Bangumi首个结果: " + bangumiResults.get(0).getTitleZh());
                } else {
                    Log.d(TAG, "Bangumi无结果或失败");
                }
                
                 // 处理TMDb结果
                List<SearchResult> tmdbResults = tmdbFuture.join();
                if (!tmdbResults.isEmpty()) {
                     tmdbResults.stream()
                         .filter(r -> "movie".equals(r.getMediaType()) || "tv".equals(r.getMediaType()))
                         .findFirst()
                         .ifPresent(result -> {
                             firstResults.put("tmdb", result);
                             Log.d(TAG, "获取到TMDb首个结果: " + result.getTitleZh());
                         });
                     // Log if TMDb returned results but none matched the filter
                     if (!firstResults.containsKey("tmdb")) {
                         Log.d(TAG, "TMDb有结果但无电影/电视剧类型");
                     }
                 } else {
                     Log.d(TAG, "TMDb无结果或失败");
                 }

                Log.d(TAG, "多来源快速搜索完成，找到 " + firstResults.size() + " 个来源的结果");
                return firstResults;
            }); // 不再需要外部的 exceptionally，因为内部已处理
    }
} 