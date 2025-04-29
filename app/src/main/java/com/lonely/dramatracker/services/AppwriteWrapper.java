package com.lonely.dramatracker.services;

import android.util.Log;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.config.AppConfig;
import io.appwrite.models.Session;
import io.appwrite.models.User;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import kotlin.jvm.functions.Function1;
import kotlin.Unit;

/**
 * Appwrite服务的Java包装类
 * 提供同步方法调用，方便在Java代码中使用Appwrite服务
 */
public class AppwriteWrapper {
    private static final String TAG = "AppwriteWrapper";

    /**
     * 同步登录方法
     * @param email 用户邮箱
     * @param password 用户密码
     * @return 登录会话
     * @throws Exception 登录失败时抛出异常
     */
    public static Session login(String email, String password) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Session> sessionRef = new AtomicReference<>();
        final AtomicReference<Exception> errorRef = new AtomicReference<>();

        Appwrite.INSTANCE.loginWithCallback(email, password,
            session -> {
                sessionRef.set(session);
                latch.countDown();
                return null; // 返回null以满足Kotlin的Unit类型要求
            },
            error -> {
                errorRef.set(error);
                latch.countDown();
                return null; // 返回null以满足Kotlin的Unit类型要求
            }
        );

        latch.await();
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return sessionRef.get();
    }

    /**
     * 同步注册方法
     * @param email 用户邮箱
     * @param password 用户密码
     * @param name 用户名称
     * @return 创建的用户对象
     * @throws Exception 注册失败时抛出异常
     */
    public static User<Map<String, Object>> register(String email, String password, String name) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<User<Map<String, Object>>> userRef = new AtomicReference<>();
        final AtomicReference<Exception> errorRef = new AtomicReference<>();

        Appwrite.INSTANCE.registerWithCallback(email, password, name,
            user -> {
                userRef.set(user);
                latch.countDown();
                return null; // 返回null以满足Kotlin的Unit类型要求
            },
            error -> {
                errorRef.set(error);
                latch.countDown();
                return null; // 返回null以满足Kotlin的Unit类型要求
            }
        );

        latch.await();
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return userRef.get();
    }

    /**
     * 同步获取当前用户信息
     * @return 用户信息Map
     * @throws Exception 获取失败时抛出异常
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getCurrentUser() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Map<String, Object>> userDataRef = new AtomicReference<>();
        final AtomicReference<Exception> errorRef = new AtomicReference<>();

        Appwrite.INSTANCE.getCurrentUserWithCallback(
            userData -> {
                // 使用类型转换解决泛型问题
                userDataRef.set((Map<String, Object>) userData);
                latch.countDown();
                return null; // 返回null以满足Kotlin的Unit类型要求
            },
            error -> {
                errorRef.set(error);
                latch.countDown();
                return null; // 返回null以满足Kotlin的Unit类型要求
            }
        );

        latch.await();
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return userDataRef.get();
    }

    /**
     * 同步登出方法
     * @throws Exception 登出失败时抛出异常
     */
    public static void logout() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> errorRef = new AtomicReference<>();

        Appwrite.INSTANCE.logoutWithCallback(
            () -> {
                latch.countDown();
                return null; // 返回null以满足Kotlin的Unit类型要求
            },
            error -> {
                errorRef.set(error);
                latch.countDown();
                return null; // 返回null以满足Kotlin的Unit类型要求
            }
        );

        latch.await();
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public static String getCurrentUserId() {
        return Appwrite.INSTANCE.getCurrentUserId();
    }

    /**
     * 取消所有正在进行的网络请求
     */
    public static void cancelAllRequests() {
        Appwrite.INSTANCE.cancelAllRequests();
    }

    /**
     * 检查sourceId是否已被收藏
     * @param sourceId 源站ID
     * @return 是否已收藏
     */
    public static boolean isSourceIdCollected(String sourceId) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Boolean> result = new AtomicReference<>(false);

        CompletableFuture.runAsync(() -> {
            Appwrite.INSTANCE.isSourceIdCollected(sourceId, new Continuation<Boolean>() {
                @NotNull
                @Override
                public CoroutineContext getContext() {
                    return Dispatchers.getIO();
                }

                @Override
                public void resumeWith(@NotNull Object o) {
                    try {
                        if (o instanceof Throwable) {
                            Log.e(TAG, "检查收藏状态失败", (Throwable) o);
                        } else {
                            Boolean isCollected = (Boolean) o;
                            result.set(isCollected != null && isCollected);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "处理收藏状态结果时出错", e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                Log.w(TAG, "检查收藏状态超时");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "检查收藏状态被中断", e);
            Thread.currentThread().interrupt();
        }

        return result.get();
    }

    /**
     * Java调用：收藏媒体
     * @param result 搜索结果
     * @param userId 用户ID
     * @param onSuccess 成功回调
     * @param onError 失败回调
     * 注意：
     * 1. 会自动检查media和media_source表，避免重复插入
     * 2. 会检查collection表中是否已存在user_id+media_id组合，只有不存在时才插入
     */
    public static void addMediaWithSourceAndCollection(SearchResult result, String userId, Runnable onSuccess, Runnable onError) {
        Appwrite.INSTANCE.addMediaWithSourceAndCollection(result, userId, new kotlin.jvm.functions.Function1<Boolean, kotlin.Unit>() {
            @Override
            public kotlin.Unit invoke(Boolean success) {
                if (success) {
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) onError.run();
                }
                return null;
            }
        });
    }

    /**
     * 获取用户的收藏记录
     * @param userId 用户ID
     * @return 收藏记录列表
     * @throws Exception 获取失败时抛出异常
     */
    public static List<Map<String, Object>> getUserCollections(String userId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<List<Map<String, Object>>> collectionsRef = new AtomicReference<>();
        final AtomicReference<Exception> errorRef = new AtomicReference<>();

        Appwrite.INSTANCE.getUserCollections(
            userId,
            collections -> {
                // onSuccess 回调
                collectionsRef.set((List<Map<String, Object>>) collections);
                latch.countDown();
                return null;
            },
            error -> {
                // onError 回调
                errorRef.set(error);
                latch.countDown();
                return null;
            }
        );

        latch.await();
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return collectionsRef.get();
    }

    /**
     * 根据ID获取媒体信息
     * @param mediaId 媒体ID
     * @return 媒体信息
     * @throws Exception 获取失败时抛出异常
     */
    public static Map<String, Object> getMediaById(String mediaId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Map<String, Object>> mediaRef = new AtomicReference<>();
        final AtomicReference<Exception> errorRef = new AtomicReference<>();

        Appwrite.INSTANCE.getMediaById(
            mediaId,
            media -> {
                // onSuccess 回调
                mediaRef.set((Map<String, Object>) media);
                latch.countDown();
                return null;
            },
            error -> {
                // onError 回调
                errorRef.set(error);
                latch.countDown();
                return null;
            }
        );

        latch.await();
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return mediaRef.get();
    }

    /**
     * 添加到收藏
     * @param result 搜索结果
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    public static void addToCollection(SearchResult result, Runnable onSuccess, Runnable onError) {
        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            if (onError != null) {
                onError.run();
            }
            return;
        }
        
        addMediaWithSourceAndCollection(result, userId, onSuccess, onError);
    }
    
    /**
     * 从收藏中移除（通过SearchResult对象）
     * @param result 搜索结果对象
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    public static void removeFromCollection(SearchResult result, Runnable onSuccess, Runnable onError) {
        if (result == null) {
            Log.e(TAG, "removeFromCollection: 搜索结果为空");
            if (onError != null) {
                onError.run();
            }
            return;
        }
        
        String sourceId = result.getSourceId();
        Log.d(TAG, "开始从收藏中移除: sourceId=" + sourceId);
        
        // 确保sourceId不为空
        if (sourceId == null || sourceId.isEmpty()) {
            Log.e(TAG, "removeFromCollection: sourceId为空");
            if (onError != null) {
                onError.run();
            }
            return;
        }
        
        Appwrite.INSTANCE.removeFromCollection(sourceId, new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean isRemoved) {
                Log.d(TAG, "移除收藏结果: " + isRemoved);
                if (isRemoved) {
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) onError.run();
                }
                return null;
            }
        });
    }

    /**
     * 从收藏中移除（通过collectionId - 直接从数据库查询sourceId）
     * @param collectionId 收藏记录ID
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    public static void removeFromCollection(String collectionId, Runnable onSuccess, Runnable onError) {
        if (collectionId == null || collectionId.isEmpty()) {
            if (onError != null) {
                onError.run();
            }
            return;
        }
        
        Log.d(TAG, "开始从收藏中移除(尝试查找sourceId): 收藏ID=" + collectionId);
        
        try {
            // 使用AppConfig中的常量获取数据库ID和集合ID
            String dbId = AppConfig.DATABASE_ID;
            String collectionColId = AppConfig.COLLECTION_COLLECTIONS_ID;
            String mediaSourceColId = AppConfig.COLLECTION_MEDIA_SOURCE_ID;
            
            Log.d(TAG, "使用配置: 数据库ID=" + dbId + ", 收藏集合ID=" + collectionColId + ", 来源集合ID=" + mediaSourceColId);
            
            // 先尝试从数据库通过collectionId查询对应的sourceId
            String[] queries = new String[] {"$id", "equal", collectionId};
            List<Map<String, Object>> collectionDocs = listDocuments(dbId, collectionColId, queries);
            
            if (collectionDocs == null || collectionDocs.isEmpty()) {
                Log.e(TAG, "未找到对应的收藏记录");
                if (onError != null) {
                    onError.run();
                }
                return;
            }
            
            String mediaId = (String) collectionDocs.get(0).get("media_id");
            if (mediaId == null || mediaId.isEmpty()) {
                Log.e(TAG, "收藏记录中没有media_id");
                if (onError != null) {
                    onError.run();
                }
                return;
            }
            
            // 通过mediaId查询sourceId
            String[] sourceQueries = new String[] {"media_id", "equal", mediaId};
            List<Map<String, Object>> sourceDocs = listDocuments(dbId, mediaSourceColId, sourceQueries);
            
            if (sourceDocs == null || sourceDocs.isEmpty()) {
                Log.e(TAG, "未找到对应的来源记录");
                if (onError != null) {
                    onError.run();
                }
                return;
            }
            
            String sourceId = (String) sourceDocs.get(0).get("source_id");
            if (sourceId == null || sourceId.isEmpty()) {
                Log.e(TAG, "来源记录中没有source_id");
                if (onError != null) {
                    onError.run();
                }
                return;
            }
            
            Log.d(TAG, "找到对应的sourceId: " + sourceId + "，开始移除收藏");
            
            // 使用找到的sourceId调用Appwrite的removeFromCollection方法
            Appwrite.INSTANCE.removeFromCollection(sourceId, new Function1<Boolean, Unit>() {
                @Override
                public Unit invoke(Boolean isRemoved) {
                    Log.d(TAG, "移除收藏结果: " + isRemoved);
                    if (isRemoved) {
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        if (onError != null) onError.run();
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "查询或移除收藏时发生错误", e);
            if (onError != null) {
                onError.run();
            }
        }
    }

    /**
     * 查询文档列表
     * @param databaseId 数据库ID
     * @param collectionId 集合ID
     * @param queries 查询条件，格式为：[字段, 操作符, 值, 字段, 操作符, 值...]
     * @return 文档列表
     * @throws Exception 查询失败时抛出异常
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> listDocuments(String databaseId, String collectionId, String[] queries) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<List<Map<String, Object>>> documentsRef = new AtomicReference<>(new ArrayList<>());
        final AtomicReference<Exception> errorRef = new AtomicReference<>();

        Appwrite.INSTANCE.listDocumentsForJava(
            databaseId,
            collectionId,
            queries,
            documents -> {
                documentsRef.set((List<Map<String, Object>>) documents);
                latch.countDown();
                return null;
            },
            error -> {
                errorRef.set(error);
                latch.countDown();
                return null;
            }
        );

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new Exception("查询文档超时");
            }
        } catch (InterruptedException e) {
            throw new Exception("查询文档被中断", e);
        }
        
        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        
        return documentsRef.get();
    }

    /**
     * 更新观看状态
     * @param userId 用户ID
     * @param mediaId 媒体ID
     * @param watchStatus 观看状态 true=已观看，false=未观看
     * @param onSuccess 成功回调
     * @param onError 失败回调
     */
    public static void updateWatchStatus(String userId, String mediaId, boolean watchStatus, Runnable onSuccess, Runnable onError) {
        Appwrite.INSTANCE.updateWatchStatus(userId, mediaId, watchStatus, new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean success) {
                if (success) {
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) onError.run();
                }
                return null;
            }
        });
    }
} 