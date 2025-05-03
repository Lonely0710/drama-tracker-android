package com.lonely.dramatracker.api.impl;

import com.lonely.dramatracker.api.ApiService;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.utils.BangumiCrawler;
import com.lonely.dramatracker.utils.DoubanCrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

public class ApiServiceImpl implements ApiService {
    private final BangumiCrawler bangumiCrawler;
    private final DoubanCrawler doubanCrawler;
    // 用于缓存搜索结果，避免重复获取
    private final CacheManager<String, List<SearchResult>> searchCache = new CacheManager<>(60 * 5); // 5分钟缓存

    public ApiServiceImpl() {
        this.bangumiCrawler = new BangumiCrawler();
        this.doubanCrawler = new DoubanCrawler();
    }
    
    @Override
    public CompletableFuture<Integer> getTotalCount(String keyword, String type) {
        // 生成缓存键
        String cacheKey = keyword + "_" + type;
        
        // 检查缓存中是否已有搜索结果
        if (searchCache.containsKey(cacheKey)) {
            List<SearchResult> cachedResults = searchCache.get(cacheKey);
            return CompletableFuture.completedFuture(cachedResults.size());
        }
        
        // 没有缓存时，执行搜索获取总数
        try {
            CompletableFuture<List<SearchResult>> searchFuture;
            
            if ("anime".equals(type)) {
                searchFuture = bangumiCrawler.search(keyword);
            } else if ("movie".equals(type) || "tv".equals(type)) {
                searchFuture = doubanCrawler.search(keyword);
            } else {
                searchFuture = doubanCrawler.search(keyword);
            }
            
            return searchFuture.thenApply(results -> {
                // 将结果保存到缓存
                searchCache.put(cacheKey, results);
                return results.size();
            });
        } catch (Exception e) {
            CompletableFuture<Integer> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("获取总数失败", e));
            return future;
        }
    }

    @Override
    public CompletableFuture<List<SearchResult>> search(String keyword, String type, int page, int limit) {
        try {
            // 生成缓存键
            String cacheKey = keyword + "_" + type;
            
            // 检查缓存中是否已有搜索结果
            if (searchCache.containsKey(cacheKey)) {
                List<SearchResult> cachedResults = searchCache.get(cacheKey);
                return CompletableFuture.completedFuture(applyPagination(cachedResults, page, limit));
            }
            
            // 根据搜索框类型选择不同的搜索来源
            CompletableFuture<List<SearchResult>> originalFuture;
            
            if ("anime".equals(type)) {
                originalFuture = bangumiCrawler.search(keyword);
            } else if ("movie".equals(type) || "tv".equals(type)) {
                originalFuture = doubanCrawler.search(keyword);
            } else {
                originalFuture = doubanCrawler.search(keyword);
            }
            
            // 保存结果到缓存并应用分页
            return originalFuture.thenApply(results -> {
                searchCache.put(cacheKey, results);
                return applyPagination(results, page, limit);
            });
        } catch (Exception e) {
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
        if (allResults == null || allResults.isEmpty()) {
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
        return allResults.subList(startIndex, endIndex);
    }
    
    /**
     * 简单的内存缓存管理器
     */
    private static class CacheManager<K, V> {
        private final Map<K, CacheEntry<V>> cache = new java.util.HashMap<>();
        private final long ttlMillis; // 缓存生存时间（毫秒）
        
        public CacheManager(long ttlSeconds) {
            this.ttlMillis = ttlSeconds * 1000;
        }
        
        public void put(K key, V value) {
            cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMillis));
        }
        
        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                return entry.getValue();
            }
            cache.remove(key); // 自动清理过期项
            return null;
        }
        
        public boolean containsKey(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                return true;
            }
            cache.remove(key); // 自动清理过期项
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