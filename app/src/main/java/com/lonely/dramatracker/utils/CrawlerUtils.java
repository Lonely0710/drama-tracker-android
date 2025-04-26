package com.lonely.dramatracker.utils;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 爬虫工具类
 * 提供基础的网络请求和解析功能
 */
public class CrawlerUtils {
    private static final String TAG = "CrawlerUtils";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36";
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * 使用OkHttp发送GET请求
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException 请求失败时抛出异常
     */
    public static String httpGet(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            return response.body().string();
        }
    }

    /**
     * 使用Jsoup解析HTML
     * @param url 网页URL
     * @return Document对象
     * @throws IOException 请求或解析失败时抛出异常
     */
    public static Document parseHtml(String url) throws IOException {
        try {
            return Jsoup.connect(url)
                    .userAgent(DEFAULT_USER_AGENT)
                    .timeout(10000)
                    .get();
        } catch (IOException e) {
            Log.e(TAG, "解析HTML失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 从URL中提取ID
     * @param url URL字符串
     * @param pattern ID的正则表达式模式
     * @return 提取的ID，如果未找到返回null
     */
    public static String extractId(String url, String pattern) {
        if (url == null || pattern == null) {
            return null;
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 确保URL是完整的
     * @param url 可能不完整的URL
     * @param baseUrl 基础URL
     * @return 完整的URL
     */
    public static String ensureFullUrl(String url, String baseUrl) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("http")) {
            return url;
        }
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        return baseUrl + (url.startsWith("/") ? url : "/" + url);
    }
}
