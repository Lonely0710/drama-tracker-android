package com.lonely.dramatracker.services;

import android.util.Log;
import io.appwrite.models.Session;
import io.appwrite.models.User;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
} 