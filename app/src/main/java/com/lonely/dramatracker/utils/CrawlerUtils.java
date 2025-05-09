package com.lonely.dramatracker.utils;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 爬虫工具类
 * 提供基础的网络请求和解析功能
 */
public class CrawlerUtils {
    private static final String TAG = "CrawlerUtils";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
            .build();

    /**
     * 使用OkHttp发送GET请求
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException 请求失败时抛出异常
     */
    public static String httpGet(String url) throws IOException {
        return httpGet(url, 3);
    }
    
    /**
     * 使用OkHttp发送GET请求，带重试机制
     * @param url 请求URL
     * @param maxRetries 最大重试次数
     * @return 响应内容
     * @throws IOException 所有重试都失败时抛出异常
     */
    public static String httpGet(String url, int maxRetries) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build();
        
        IOException lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Response response = okHttpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + ": " + response.message());
                }
                
                return response.body().string();
            } catch (IOException e) {
                lastException = e;
                Log.w(TAG, "HTTP GET 请求失败，尝试 " + (attempt + 1) + " 次，URL: " + url + "，错误: " + e.getMessage());
                
                if (attempt < maxRetries) {
                    // 等待一段时间再重试，使用退避策略
                    try {
                        int backoffMs = 1000 * (attempt + 1);
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                }
            }
        }
        
        throw lastException;
    }

    /**
     * 使用Jsoup解析HTML
     * @param url 网页URL
     * @return Document对象
     * @throws IOException 请求或解析失败时抛出异常
     */
    public static Document parseHtml(String url) throws IOException {
        return parseHtml(url, 3);
    }
    
    /**
     * 使用Jsoup解析HTML，带重试机制
     * @param url 网页URL
     * @param maxRetries 最大重试次数
     * @return Document对象
     * @throws IOException 所有重试都失败时抛出异常
     */
    public static Document parseHtml(String url, int maxRetries) throws IOException {
        IOException lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // 尝试直接使用Jsoup连接
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(30000) // 30秒超时
                        .get();
            } catch (IOException e) {
                lastException = e;
                Log.w(TAG, "Jsoup解析失败，尝试 " + (attempt + 1) + " 次，URL: " + url + "，错误: " + e.getMessage());
                
                if (attempt < maxRetries) {
                    // 等待一段时间再重试，使用退避策略
                    try {
                        int backoffMs = 1000 * (attempt + 1);
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                }
            }
        }
        
        // 如果Jsoup直接连接全部失败，尝试使用OkHttp获取内容再解析
        try {
            String html = httpGet(url, maxRetries);
            return Jsoup.parse(html, url);
        } catch (IOException e) {
            Log.e(TAG, "使用OkHttp+Jsoup解析也失败: " + e.getMessage(), e);
            throw lastException; // 抛出最后一次Jsoup直接连接的异常
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

    /**
     * 打印HTML内容的一部分，用于调试
     * @param doc HTML文档
     * @param tag 日志标签
     * @param maxLength 最大长度限制
     */
    public static void logHtmlContent(Document doc, String tag, int maxLength) {
        if (doc == null) {
            Log.e(tag, "文档为空，无法打印HTML");
            return;
        }
        
        String html = doc.outerHtml();
        int length = Math.min(html.length(), maxLength);
        Log.d(tag, "HTML内容前" + length + "字符: " + html.substring(0, length));
        
        // 也打印一些重要的结构信息
        Log.d(tag, "页面标题: " + doc.title());
        Log.d(tag, "Body子元素数量: " + doc.body().children().size());
        
        // 尝试打印一些关键元素
        Log.d(tag, "#colunmSingle 元素: " + (doc.selectFirst("#colunmSingle") != null ? "存在" : "不存在"));
        Log.d(tag, ".BgmCalendar 元素: " + (doc.selectFirst(".BgmCalendar") != null ? "存在" : "不存在"));
        Log.d(tag, "ul.large 元素: " + (doc.selectFirst("ul.large") != null ? "存在" : "不存在"));
        Log.d(tag, "li.week 元素: " + (doc.selectFirst("li.week") != null ? "存在" : "不存在"));
    }
}
