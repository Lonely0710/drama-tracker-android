package com.lonely.dramatracker.utils;

import android.util.Log;

import com.lonely.dramatracker.models.MediaInfo;
import com.lonely.dramatracker.models.SearchResult;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 豆瓣网站爬虫
 * 负责从豆瓣网站爬取电影、电视剧信息
 */
public class DoubanCrawler {
    private static final String TAG = "DoubanCrawler";
    private static final String BASE_URL = "https://movie.douban.com";
    private static final String SEARCH_URL = "https://www.douban.com/search?cat=1002&q=";
    
    // 请求头，参考doubanMovies.js中的headers
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36";

    /**
     * 搜索豆瓣电影
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    public CompletableFuture<List<SearchResult>> search(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            List<SearchResult> results = new ArrayList<>();
            try {
                String url = SEARCH_URL + keyword;
                Document doc = CrawlerUtils.parseHtml(url);
                
                Elements items = doc.select(".result-list .result");
                
                for (Element item : items) {
                    try {
                        Element titleLink = item.selectFirst("h3 a");
                        if (titleLink == null) continue;
                        
                        String onclickAttr = titleLink.attr("onclick");
                        
                        // 只处理电影类型的结果
                        if (!onclickAttr.contains("movie")) {
                            continue;
                        }
                        
                        // 提取ID
                        Pattern pattern = Pattern.compile("\\d+(?=,)");
                        Matcher matcher = pattern.matcher(onclickAttr);
                        String sourceId = matcher.find() ? matcher.group() : "";
                        
                        if (sourceId.isEmpty()) {
                            continue;
                        }
                        
                        SearchResult.Builder builder = new SearchResult.Builder()
                            .setSourceType("douban")
                            .setSourceId(sourceId)
                            .setSourceUrl(BASE_URL + "/subject/" + sourceId);
                        
                        // 设置媒体类型，默认为电影
                        builder.setMediaType("movie");
                        
                        // 解析标题
                        String title = titleLink.text().trim();
                        // 通常豆瓣搜索结果的标题格式为：《标题》，需要去除《》
                        if (title.startsWith("《") && title.contains("》")) {
                            title = title.substring(title.indexOf("《") + 1, title.indexOf("》"));
                        }
                        builder.setTitleZh(title);
                        
                        // 解析制作人员和年份
                        Element castElement = item.selectFirst(".subject-cast");
                        if (castElement != null) {
                            String castText = castElement.text().trim();
                            
                            // 清理staff信息，只保留人名相关内容
                            String cleanedStaff = cleanStaffInfo(castText);
                            builder.setStaff(cleanedStaff);
                            
                            // 尝试从演职员信息中提取年份
                            Pattern yearPattern = Pattern.compile("\\d{4}");
                            Matcher yearMatcher = yearPattern.matcher(castText);
                            if (yearMatcher.find()) {
                                builder.setYear(yearMatcher.group());
                            }
                        }
                        
                        // 搜索结果页没有海报，评分等信息
                        // 为了获取完整信息，我们需要获取电影详情页
                        try {
                            String detailUrl = BASE_URL + "/subject/" + sourceId;
                            // 获取电影详情
                            MediaInfo mediaInfo = getMediaInfoSync(sourceId);
                            if (mediaInfo != null) {
                                // 使用详情页信息补充搜索结果
                                builder.setPosterUrl(mediaInfo.getPosterUrl())
                                       .setRatingDouban(mediaInfo.getRatingDouban())
                                       .setReleaseDate(mediaInfo.getReleaseDate())
                                       .setDuration(mediaInfo.getDuration())
                                       .setSummary(mediaInfo.getSummary())
                                       .setTitleOriginal(mediaInfo.getTitleOriginal());
                                
                                // 设置评分（用于兼容旧版本）
                                if (mediaInfo.getRatingDouban() > 0) {
                                    builder.setRating(mediaInfo.getRatingDouban());
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "获取电影详情失败: " + e.getMessage());
                        }
                        
                        SearchResult result = builder.build();
                        results.add(result);
                    } catch (Exception e) {
                        Log.e(TAG, "解析搜索结果项时出错: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "搜索请求失败: " + e.getMessage());
                throw new RuntimeException("搜索失败", e);
            }
            return results;
        });
    }

    /**
     * 清理演职员信息，只保留人名相关部分
     * @param staffText 原始的staff文本
     * @return 清理后的staff文本
     */
    private String cleanStaffInfo(String staffText) {
        if (staffText == null || staffText.isEmpty()) {
            return "";
        }
        
        // 移除原名信息 (通常格式为 "原名: xxx / ")
        String cleaned = staffText.replaceAll("原名:.*?(?=/\\s|$)", "").trim();
        
        // 如果以 " / " 开头，去掉开头的分隔符
        if (cleaned.startsWith("/ ")) {
            cleaned = cleaned.substring(2).trim();
        }
        
        return cleaned;
    }

    /**
     * 获取电影详细信息 (同步版本，给search方法内部使用)
     * @param sourceId 豆瓣的电影ID
     * @return 媒体详细信息
     */
    private MediaInfo getMediaInfoSync(String sourceId) {
        try {
            String url = BASE_URL + "/subject/" + sourceId;
            Document doc = CrawlerUtils.parseHtml(url);
            
            MediaInfo mediaInfo = new MediaInfo();
            mediaInfo.setMediaType("movie");
            
            // 解析标题
            Element titleElement = doc.selectFirst("h1 span[property='v:itemreviewed']");
            if (titleElement != null) {
                String fullTitle = titleElement.text().trim();
                // 豆瓣通常将中文标题和原标题用空格分隔
                if (fullTitle.contains(" ")) {
                    String[] titles = fullTitle.split(" ", 2);
                    mediaInfo.setTitleZh(titles[0].trim());
                    mediaInfo.setTitleOriginal(titles[1].trim());
                } else {
                    mediaInfo.setTitleZh(fullTitle);
                }
            }
            
            // 解析评分
            Element ratingElement = doc.selectFirst("strong[property='v:average']");
            if (ratingElement != null) {
                try {
                    double rating = Double.parseDouble(ratingElement.text());
                    mediaInfo.setRatingDouban(rating);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "解析评分失败: " + e.getMessage());
                }
            }
            
            // 解析简介
            Element summaryElement = doc.selectFirst("span[property='v:summary']");
            if (summaryElement != null) {
                String summary = summaryElement.text().trim();
                // 处理简介中的特殊字符
                summary = summary.replace("(展开全部)", "");
                summary = summary.replace("\r\n", "\n");
                summary = summary.replaceAll("\\s\\s\\s\\s", "\n");
                mediaInfo.setSummary(summary);
            }
            
            // 解析上映日期，并移除括号及其内容
            Element releaseDateElement = doc.selectFirst("span[property='v:initialReleaseDate']");
            if (releaseDateElement != null) {
                String releaseDate = releaseDateElement.text().trim();
                // 移除括号及其内容，例如 "2024-06-21(中国大陆)" -> "2024-06-21"
                if (releaseDate.contains("(")) {
                    releaseDate = releaseDate.substring(0, releaseDate.indexOf("("));
                }
                mediaInfo.setReleaseDate(releaseDate);
            }
            
            // 解析片长
            Element runtimeElement = doc.selectFirst("span[property='v:runtime']");
            if (runtimeElement != null) {
                mediaInfo.setDuration(runtimeElement.text().trim());
            }
            
            // 解析海报
            Element posterElement = doc.selectFirst("img[rel='v:image']");
            if (posterElement != null) {
                mediaInfo.setPosterUrl(posterElement.attr("src"));
            }
            
            // 解析导演
            StringBuilder staffBuilder = new StringBuilder();
            Elements directorElements = doc.select("a[rel='v:directedBy']");
            if (!directorElements.isEmpty()) {
                staffBuilder.append("导演: ");
                for (int i = 0; i < directorElements.size(); i++) {
                    if (i > 0) staffBuilder.append(", ");
                    staffBuilder.append(directorElements.get(i).text());
                }
            }
            
            // 解析主演
            Elements actorElements = doc.select("a[rel='v:starring']");
            if (!actorElements.isEmpty()) {
                if (staffBuilder.length() > 0) {
                    staffBuilder.append(" | ");
                }
                staffBuilder.append("主演: ");
                // 只取前5个主演
                int limit = Math.min(actorElements.size(), 5);
                for (int i = 0; i < limit; i++) {
                    if (i > 0) staffBuilder.append(", ");
                    staffBuilder.append(actorElements.get(i).text());
                }
            }
            
            mediaInfo.setStaff(staffBuilder.toString());
            
            return mediaInfo;
        } catch (Exception e) {
            Log.e(TAG, "获取电影详情失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取电影详细信息 (异步版本，供外部调用)
     * @param sourceId 豆瓣的电影ID
     * @return 媒体详细信息的Future
     */
    public CompletableFuture<MediaInfo> getMediaInfo(String sourceId) {
        return CompletableFuture.supplyAsync(() -> getMediaInfoSync(sourceId));
    }
} 