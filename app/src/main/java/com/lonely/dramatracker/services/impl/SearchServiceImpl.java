package com.lonely.dramatracker.services.impl;

import com.google.gson.Gson;
import com.lonely.dramatracker.api.ApiService;
import com.lonely.dramatracker.models.MediaInfo;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.services.SearchService;
import com.lonely.dramatracker.config.AppConfig;
import com.lonely.dramatracker.services.AppwriteWrapper;
import io.appwrite.ID;
import io.appwrite.Query;
import io.appwrite.models.Document;
import io.appwrite.services.Databases;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.CompletableFuture;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class SearchServiceImpl implements SearchService {
    private static final String TAG = "SearchServiceImpl";
    
    private final ApiService apiService;
    private final Gson gson = new Gson();
    
    public SearchServiceImpl(ApiService apiService) {
        this.apiService = apiService;
    }
    
    @Override
    public void getTotalItems(String keyword, String type, TotalItemsCallback callback) {
        // 调用API获取总项目数
        // 这里我们可以复用search方法，但设置一个小的pageSize以快速获取第一页
        // 通常API会在响应中提供总数信息
        apiService.getTotalCount(keyword, type).thenAccept(count -> {
            callback.onTotalItemsResult(count);
        }).exceptionally(throwable -> {
            Log.e(TAG, "获取总项目数失败: " + throwable.getMessage(), throwable);
            // 失败时返回0
            callback.onTotalItemsResult(0);
            return null;
        });
    }
    
    @Override
    // 更新方法签名以包含 page 和 limit
    public void search(String keyword, String type, int page, int limit, SearchCallback callback) {
        // 将 page 和 limit 传递给 apiService
        apiService.search(keyword, type, page, limit).thenAccept(results -> {
            // 检查每个结果的收藏状态
            for (SearchResult result : results) {
                try {
                    boolean isCollected = AppwriteWrapper.isSourceIdCollected(result.getSourceId());
                    result.setCollected(isCollected);
                } catch (Exception e) {
                    Log.e(TAG, "检查收藏状态失败: " + e.getMessage(), e);
                    // 默认为未收藏
                    result.setCollected(false);
                }
            }
            callback.onSearchComplete(results);
        }).exceptionally(throwable -> {
            Log.e(TAG, "搜索失败: " + throwable.getMessage(), throwable);
            callback.onSearchComplete(new ArrayList<>());
            return null;
        });
    }
    
    @Override
    // 更新方法签名以包含 page 和 limit
    public void search(String keyword, String type, int page, int limit, JsonSearchCallback callback) {
        // 将 page 和 limit 传递给 apiService
        apiService.search(keyword, type, page, limit).thenAccept(results -> {
            // 检查每个结果的收藏状态
            for (SearchResult result : results) {
                try {
                    boolean isCollected = AppwriteWrapper.isSourceIdCollected(result.getSourceId());
                    result.setCollected(isCollected);
                } catch (Exception e) {
                    Log.e(TAG, "检查收藏状态失败: " + e.getMessage(), e);
                    // 默认为未收藏
                    result.setCollected(false);
                }
            }
            // 转换结果为JSON字符串
            String jsonResults = gson.toJson(results);
            callback.onSearchComplete(jsonResults);
        }).exceptionally(throwable -> {
            Log.e(TAG, "搜索失败: " + throwable.getMessage(), throwable);
            callback.onSearchComplete("[]");
            return null;
        });
    }

    @Override
    public void addToCollection(SearchResult result) {
        addToCollection(result, null);
    }

    @Override
    public void addToCollection(SearchResult result, Runnable onSuccess) {
        addToCollection(result, onSuccess, null);
    }

    @Override
    public void addToCollection(SearchResult result, Runnable onSuccess, Runnable onFailure) {
        try {
            AppwriteWrapper.addToCollection(result, () -> {
                result.setCollected(true);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }, (Runnable) () -> {
                Log.e(TAG, "Failed to add to collection");
                if (onFailure != null) {
                    onFailure.run();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception when adding to collection: " + e.getMessage());
            if (onFailure != null) {
                onFailure.run();
            }
        }
    }

    @Override
    public void removeFromCollection(SearchResult result) {
        removeFromCollection(result, null);
    }

    @Override
    public void removeFromCollection(SearchResult result, Runnable onSuccess) {
        removeFromCollection(result, onSuccess, null);
    }

    @Override
    public void removeFromCollection(SearchResult result, Runnable onSuccess, Runnable onFailure) {
        try {
            String collectionId = result.getCollectionId();
            // 如果collectionId为空，尝试通过sourceId查询
            if (collectionId == null || collectionId.isEmpty()) {
                Log.d(TAG, "缺少收藏ID，尝试通过sourceId查询");
                // 使用sourceId查询已收藏记录
                CompletableFuture.runAsync(() -> {
                    try {
                        String userId = AppwriteWrapper.getCurrentUserId();
                        if (userId.isEmpty()) {
                            throw new Exception("未获取到当前用户ID");
                        }
                        
                        Log.d(TAG, "查询sourceId=" + result.getSourceId() + "的媒体源记录");
                        // 查询媒体源表，获取mediaId
                        List<Map<String, Object>> mediaSources = AppwriteWrapper.listDocuments(
                            AppConfig.DATABASE_ID_STATIC,
                            AppConfig.COLLECTION_MEDIA_SOURCE_ID_STATIC,
                            new String[]{"source_id", "equal", result.getSourceId()}
                        );
                        
                        if (mediaSources.isEmpty()) {
                            throw new Exception("未找到媒体源记录");
                        }
                        
                        String mediaId = (String) mediaSources.get(0).get("media_id");
                        Log.d(TAG, "找到mediaId=" + mediaId);
                        
                        if (mediaId == null || mediaId.isEmpty()) {
                            throw new Exception("媒体源记录中未找到media_id");
                        }
                        
                        Log.d(TAG, "查询当前用户媒体收藏记录: mediaId=" + mediaId + ", userId=" + userId);
                        // 查询当前用户的收藏记录
                        List<Map<String, Object>> collections = AppwriteWrapper.listDocuments(
                            AppConfig.DATABASE_ID_STATIC,
                            AppConfig.COLLECTION_COLLECTIONS_ID_STATIC,
                            new String[]{
                                "media_id", "equal", mediaId,
                                "user_id", "equal", userId
                            }
                        );
                        
                        if (collections.isEmpty()) {
                            Log.d(TAG, "该用户没有收藏此媒体");
                            // 如果没有收藏记录，则视为操作成功（已经不在收藏中了）
                            result.setCollected(false);
                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                            return;
                        }
                        
                        // 获取收藏文档ID
                        String foundCollectionId = (String) collections.get(0).get("$id");
                        Log.d(TAG, "获取到收藏文档ID: " + foundCollectionId);
                        
                        if (foundCollectionId == null || foundCollectionId.isEmpty()) {
                            throw new Exception("收藏记录中未找到文档ID");
                        }
                        
                        // 保存找到的ID以便未来使用
                        result.setCollectionId(foundCollectionId);
                        
                        // 执行删除操作
                        AppwriteWrapper.removeFromCollection(foundCollectionId, () -> {
                            result.setCollected(false);
                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                        }, (Runnable) () -> {
                            Log.e(TAG, "移除收藏失败");
                            if (onFailure != null) {
                                onFailure.run();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "查询收藏记录失败: " + e.getMessage(), e);
                        if (onFailure != null) {
                            onFailure.run();
                        }
                    }
                });
            } else {
                // 直接使用现有的收藏ID删除
                Log.d(TAG, "使用已有的收藏ID删除: " + collectionId);
                AppwriteWrapper.removeFromCollection(collectionId, () -> {
                    result.setCollected(false);
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }, (Runnable) () -> {
                    Log.e(TAG, "移除收藏失败");
                    if (onFailure != null) {
                        onFailure.run();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "移除收藏过程中发生异常: " + e.getMessage(), e);
            if (onFailure != null) {
                onFailure.run();
            }
        }
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