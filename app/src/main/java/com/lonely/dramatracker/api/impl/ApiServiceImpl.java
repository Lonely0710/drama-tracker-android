package com.lonely.dramatracker.api.impl;

import android.util.Log;
import com.lonely.dramatracker.api.ApiService;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.utils.BangumiCrawler;
import com.lonely.dramatracker.utils.DoubanCrawler;
import com.lonely.dramatracker.utils.TMDbCrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ApiServiceImpl implements ApiService {
    private static final String TAG = "ApiServiceImpl";
    private final BangumiCrawler bangumiCrawler;
    private final DoubanCrawler doubanCrawler;
    private final TMDbCrawler tmdbCrawler;
    private final CacheManager<String, List<SearchResult>> searchResultCache = new CacheManager<>(60 * 5); // 5分钟缓存原始结果
    private final CacheManager<String, Integer> totalCountCache = new CacheManager<>(60 * 5); // 5分钟缓存总数

    public ApiServiceImpl() {
        this.doubanCrawler = new DoubanCrawler();
        this.bangumiCrawler = new BangumiCrawler();
        this.tmdbCrawler = TMDbCrawler.getInstance();
    }
    
    @Override
    public CompletableFuture<Integer> getTotalCount(String keyword, String type) {
        // 缓存键只包含关键词和类型
        String cacheKey = keyword + "_" + type + "_count";
        
        // 检查缓存
        Integer cachedCount = totalCountCache.get(cacheKey);
        if (cachedCount != null) {
            Log.d(TAG, "返回缓存的总数: " + cacheKey + " -> " + cachedCount);
            return CompletableFuture.completedFuture(cachedCount);
        }
        
        // 根据类型选择数据源获取总数
        Log.d(TAG, "开始获取总数，类型: " + type);
        CompletableFuture<Integer> countFuture;
        try {
            if ("anime".equals(type)) {
                // 动漫使用 Bangumi
                countFuture = bangumiCrawler.search(keyword)
                    .thenApplyAsync(results -> (results != null) ? results.size() : 0);
            } else if ("movie".equals(type) || "tv".equals(type)) {
                 // 电影/电视剧: 优先获取TMDb计数，如果为0则获取豆瓣计数
                countFuture = tmdbCrawler.search(keyword).thenComposeAsync(tmdbResults -> {
                    if (tmdbResults != null && !tmdbResults.isEmpty()) {
                        Log.d(TAG, "TMDb 找到 " + tmdbResults.size() + " 个结果 (计数).");
                        return CompletableFuture.completedFuture(tmdbResults.size());
                    } else {
                        Log.d(TAG, "TMDb 未找到结果 (计数), 尝试 Douban.");
                        return doubanCrawler.search(keyword)
                            .thenApplyAsync(doubanResults -> (doubanResults != null) ? doubanResults.size() : 0);
                    }
                });
            } else {
                // 其他未知类型，默认使用TMDb计数
                Log.w(TAG, "未知的搜索类型 (计数)，默认使用TMDb: " + type);
                countFuture = tmdbCrawler.search(keyword)
                    .thenApplyAsync(results -> (results != null) ? results.size() : 0);
            }
            
            // 处理最终的Future，缓存总数
            return countFuture.thenApplyAsync(count -> { // 使用 Async
                Log.d(TAG, "获取到最终总数: " + count + " for key: " + cacheKey);
                totalCountCache.put(cacheKey, count); // 缓存最终计数
                return count;
            }).exceptionally(e -> { // 添加异常处理
                Log.e(TAG, "获取总数失败 for key: " + cacheKey, e);
                return 0; // 失败时返回0
            });
        } catch (Exception e) { // 捕获同步异常
            Log.e(TAG, "启动获取总数失败 for key: " + cacheKey, e);
            CompletableFuture<Integer> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("获取总数失败", e));
            return future;
        }
    }

    @Override
    public CompletableFuture<List<SearchResult>> search(String keyword, String type, int page, int limit) {
        // 缓存键只包含关键词和类型，缓存的是原始未分页列表
        String cacheKey = keyword + "_" + type + "_results";

        // 尝试从缓存获取原始列表
        List<SearchResult> cachedResults = searchResultCache.get(cacheKey);
        if (cachedResults != null) {
            Log.d(TAG, "返回缓存的搜索结果: " + cacheKey);
            // 应用分页并返回
            return CompletableFuture.completedFuture(applyPagination(cachedResults, page, limit));
        }

        Log.d(TAG, "缓存未命中，执行搜索: " + cacheKey);
        // 根据类型选择搜索逻辑
        CompletableFuture<List<SearchResult>> finalResultFuture;
        try {
            if ("anime".equals(type)) {
                 // 动漫直接用Bangumi
                finalResultFuture = bangumiCrawler.search(keyword);
            } else if ("movie".equals(type) || "tv".equals(type)) {
                // 电影/电视剧: 优先TMDb，失败则回退到豆瓣
                finalResultFuture = tmdbCrawler.search(keyword).thenComposeAsync(tmdbResults -> {
                    if (tmdbResults != null && !tmdbResults.isEmpty()) {
                        Log.d(TAG, "TMDb 找到结果 (" + tmdbResults.size() + "), 使用 TMDb 结果 for " + keyword);
                        return CompletableFuture.completedFuture(tmdbResults); // 直接使用 TMDb 结果
                    } else {
                        Log.d(TAG, "TMDb 未找到结果, 尝试 Douban for " + keyword);
                        return doubanCrawler.search(keyword); // 回退到豆瓣搜索
                    }
                });
            } else {
                // 其他未知类型，默认仅使用TMDb
                Log.w(TAG, "未知的搜索类型，默认使用TMDb: " + type);
                finalResultFuture = tmdbCrawler.search(keyword);
            }

            // 处理最终的Future，缓存原始结果，然后应用分页
            return finalResultFuture.thenApplyAsync(finalResults -> { // 使用 Async 避免阻塞UI线程
                Log.d(TAG, "搜索完成，最终结果数: " + (finalResults != null ? finalResults.size() : 0) + " for key: " + cacheKey);
                if (finalResults != null) { // 缓存最终获取的结果（可能是TMDb、豆瓣或Bangumi）
                    searchResultCache.put(cacheKey, finalResults); 
                }
                return applyPagination(finalResults, page, limit); // 应用分页
            }).exceptionally(e -> { // 添加异常处理
                Log.e(TAG, "搜索执行失败 for key: " + cacheKey, e);
                return new ArrayList<>(); // 失败时返回空列表
            });
        } catch (Exception e) { // 捕获同步异常
             Log.e(TAG, "启动搜索失败 for key: " + cacheKey, e);
            CompletableFuture<List<SearchResult>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("搜索失败", e));
            return future;
        }
    }
    
    /**
     * 对搜索结果应用分页逻辑
     * @param allResults 所有搜索结果
     * @param page 页码（从1开始）
     * @param limit 每页数量
     * @return 分页后的结果
     */
    private List<SearchResult> applyPagination(List<SearchResult> allResults, int page, int limit) {
        if (allResults == null || allResults.isEmpty() || limit <= 0 || page <= 0) {
            return new ArrayList<>();
        }
        
        // 计算起始索引
        int startIndex = (page - 1) * limit;
        
        // 如果起始索引超出结果范围，返回空列表
        if (startIndex >= allResults.size()) {
            return new ArrayList<>();
        }
        
        // 计算结束索引（不包含）
        int endIndex = Math.min(startIndex + limit, allResults.size());
        
        // 返回请求的分页结果
        Log.d(TAG, "应用分页: page=" + page + ", limit=" + limit + ", startIndex=" + startIndex + ", endIndex=" + endIndex + ", total=" + allResults.size());
        return allResults.subList(startIndex, endIndex);
    }
    
    /**
     * 简单的内存缓存管理器 (Copy from original, add TAG)
     */
    private static class CacheManager<K, V> {
        private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>(); // 使用 ConcurrentHashMap 保证线程安全
        private final long ttlMillis; // 缓存生存时间（毫秒）
        
        public CacheManager(long ttlSeconds) {
            this.ttlMillis = ttlSeconds * 1000;
             // 可以添加一个后台线程定期清理过期缓存，但简单实现下，get/containsKey时清理
        }
        
        public synchronized void put(K key, V value) { // 加 synchronized 保护写入
            if (key == null) return;
            Log.d(TAG, "缓存写入: " + key);
            cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMillis));
        }
        
        public synchronized V get(K key) { // 加 synchronized 保护读和删除
            if (key == null) return null;
            CacheEntry<V> entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                 Log.d(TAG, "缓存命中: " + key);
                return entry.getValue();
            }
            if (entry != null) { // 过期了
                Log.d(TAG, "缓存过期并移除: " + key);
                 cache.remove(key);
            } else {
                 Log.d(TAG, "缓存未找到: " + key);
            }
            return null;
        }
        
        public synchronized boolean containsKey(K key) { // 加 synchronized 保护读和删除
             if (key == null) return false;
            CacheEntry<V> entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                return true;
            }
             if (entry != null) { // 过期了
                 cache.remove(key);
             }
            return false;
        }
        
        private static class CacheEntry<V> {
            private final V value;
            private final long expiryTime;
            
            public CacheEntry(V value, long expiryTime) {
                this.value = value;
                this.expiryTime = expiryTime;
            }
            
            public V getValue() {
                return value;
            }
            
            public boolean isExpired() {
                return System.currentTimeMillis() > expiryTime;
            }
        }
    }
} 