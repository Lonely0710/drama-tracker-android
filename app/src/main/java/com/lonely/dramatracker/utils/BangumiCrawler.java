package com.lonely.dramatracker.utils;

import android.util.Log;

import com.lonely.dramatracker.models.MediaInfo;
import com.lonely.dramatracker.models.SearchResult;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Bangumi网站爬虫
 * 负责从Bangumi网站爬取动漫信息
 */
public class BangumiCrawler {
    private static final String TAG = "BangumiCrawler";
    private static final String BASE_URL = "https://bgm.tv";

    /**
     * 搜索动漫
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    public CompletableFuture<List<SearchResult>> search(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            List<SearchResult> results = new ArrayList<>();
            try {
                String url = BASE_URL + "/subject_search/" + keyword + "?cat=2"; // cat=2 表示只搜索动画
                Document doc = CrawlerUtils.parseHtml(url);
                
                Elements items = doc.select("#browserItemList .item");
                
                for (Element item : items) {
                    try {
                        SearchResult.Builder builder = new SearchResult.Builder()
                            .setSourceType("bgm")
                            .setMediaType("anime");

                        // 解析ID和URL
                        Element titleLink = item.selectFirst("h3 a");
                        if (titleLink != null) {
                            String href = titleLink.attr("href");
                            builder.setSourceId(href.substring(href.lastIndexOf("/") + 1))
                                  .setSourceUrl(BASE_URL + href);
                        }

                        // 解析标题
                        Element titleElement = item.selectFirst("h3 a");
                        if (titleElement != null) {
                            builder.setTitleZh(titleElement.text());
                        }

                        // 解析原标题
                        Element originalTitleElement = item.selectFirst("h3 small.grey");
                        if (originalTitleElement != null) {
                            String originalTitle = originalTitleElement.text();
                            builder.setTitleOriginal(originalTitle);
                        }

                        // 解析原标题和年份
                        Element infoElement = item.selectFirst(".info");
                        if (infoElement != null) {
                            String info = infoElement.text();
                            
                            // 提取发布日期 (通常位于info开头的年月日部分)
                            if (info.matches("^\\d{4}年\\d{1,2}月\\d{1,2}日.*")) {
                                String releaseDate = info.substring(0, info.indexOf(" / "));
                                builder.setReleaseDate(releaseDate);
                            }
                            
                            // 提取制作人员信息 (通常位于日期之后)
                            if (info.contains(" / ")) {
                                String staff = info.substring(info.indexOf(" / ") + 3);
                                builder.setStaff(staff);
                            }
                            
                            // 提取年份
                            if (info.matches(".*\\d{4}.*")) {
                                String year = info.replaceAll(".*?(\\d{4}).*", "$1");
                                builder.setYear(year);
                            }
                        }

                        // 解析海报URL
                        Element posterElement = item.selectFirst("img.cover");
                        if (posterElement != null) {
                            String posterUrl = posterElement.attr("src");
                            builder.setPosterUrl(CrawlerUtils.ensureFullUrl(posterUrl, "https:"));
                        }

                        // 解析评分
                        Element ratingElement = item.selectFirst(".fade");
                        if (ratingElement != null) {
                            try {
                                double rating = Double.parseDouble(ratingElement.text());
                                builder.setRating(rating);
                                builder.setRatingBangumi(rating);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "解析评分失败: " + e.getMessage());
                            }
                        }

                        // 提取简介
                        // 由于搜索结果页可能没有简介，我们获取ID后立即请求详情页获取简介
                        if (titleLink != null) {
                            String href = titleLink.attr("href");
                            String sourceId = href.substring(href.lastIndexOf("/") + 1);
                            try {
                                String detailUrl = BASE_URL + "/subject/" + sourceId;
                                Document detailDoc = CrawlerUtils.parseHtml(detailUrl);
                                
                                // 获取简介
                                Element summaryElement = detailDoc.selectFirst("#subject_summary");
                                if (summaryElement != null) {
                                    String summary = summaryElement.text();
                                    // 参考Bangumi.js的处理：替换&nbsp和多个空格为换行符
                                    summary = summary.replaceAll("&nbsp", "\n").trim();
                                    summary = summary.replaceAll("\\s{4,}", "\n");
                                    builder.setSummary(summary);
                                }
                                
                                // 获取话数
                                Element episodeElement = detailDoc.selectFirst("#infobox li:contains(话数)");
                                if (episodeElement != null) {
                                    String episodes = episodeElement.text().replace("话数: ", "");
                                    builder.setDuration(episodes);
                                    Log.d(TAG, "获取到动漫话数: " + episodes + " 媒体ID: " + sourceId);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "获取详情页信息失败: " + e.getMessage());
                            }
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
     * 获取动漫详细信息
     * @param sourceId Bangumi的作品ID
     * @return 媒体详细信息
     */
    public CompletableFuture<MediaInfo> getMediaInfo(String sourceId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = BASE_URL + "/subject/" + sourceId;
                Document doc = CrawlerUtils.parseHtml(url);

                MediaInfo mediaInfo = new MediaInfo();
                mediaInfo.setMediaType("anime");

                // 解析中文标题
                Element titleElement = doc.selectFirst("h1.nameSingle a");
                if (titleElement != null) {
                    mediaInfo.setTitleZh(titleElement.text());
                }

                // 解析原标题
                Element originalTitleElement = doc.selectFirst("#infobox li:contains(原名)");
                if (originalTitleElement != null) {
                    String originalTitle = originalTitleElement.text().replace("原名: ", "");
                    mediaInfo.setTitleOriginal(originalTitle);
                }

                // 解析放送开始日期
                Element dateElement = doc.selectFirst("#infobox li:contains(放送开始)");
                if (dateElement != null) {
                    String date = dateElement.text().replace("放送开始: ", "");
                    mediaInfo.setReleaseDate(date);
                }

                // 解析话数
                Element episodeElement = doc.selectFirst("#infobox li:contains(话数)");
                if (episodeElement != null) {
                    String episodes = episodeElement.text().replace("话数: ", "");
                    mediaInfo.setDuration(episodes);
                }

                // 解析评分
                Element ratingElement = doc.selectFirst(".global_score .number");
                if (ratingElement != null) {
                    try {
                        double rating = Double.parseDouble(ratingElement.text());
                        mediaInfo.setRatingBangumi(rating);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "解析评分失败: " + e.getMessage());
                    }
                }

                // 解析海报URL
                Element posterElement = doc.selectFirst("img.cover");
                if (posterElement != null) {
                    String posterUrl = posterElement.attr("src");
                    mediaInfo.setPosterUrl(CrawlerUtils.ensureFullUrl(posterUrl, "https:"));
                }

                // 解析简介
                Element summaryElement = doc.selectFirst("#subject_summary");
                if (summaryElement != null) {
                    String summary = summaryElement.text();
                    // 参考Bangumi.js的处理：替换&nbsp和多个空格为换行符
                    summary = summary.replaceAll("&nbsp", "\n").trim();
                    summary = summary.replaceAll("\\s{4,}", "\n");
                    mediaInfo.setSummary(summary);
                } else {
                    mediaInfo.setSummary("暂无简介");
                }

                // 解析制作人员信息
                JSONObject staffJson = new JSONObject();
                Elements staffElements = doc.select("#infobox li:contains(导演), #infobox li:contains(脚本), #infobox li:contains(音乐), #infobox li:contains(原作)");
                for (Element staff : staffElements) {
                    String text = staff.text();
                    String[] parts = text.split(": ", 2);
                    if (parts.length == 2) {
                        staffJson.put(parts[0], parts[1]);
                    }
                }
                mediaInfo.setStaff(staffJson.toString());

                return mediaInfo;
            } catch (Exception e) {
                Log.e(TAG, "获取详细信息失败: " + e.getMessage());
                throw new RuntimeException("获取详细信息失败", e);
            }
        });
    }
}