package com.lonely.dramatracker.utils;

import android.util.Log;

import com.lonely.dramatracker.models.MediaInfo;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.models.DailyAnime;
import com.lonely.dramatracker.models.WeeklySchedule;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Bangumi网站爬虫
 * 负责从Bangumi网站爬取动漫信息
 */
public class BangumiCrawler {
    private static final String TAG = "BangumiCrawler";
    private static final String BASE_URL = "https://bgm.tv";
    private static final String CALENDAR_URL = "https://chii.in/calendar";

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

    /**
     * 获取每日放送表
     * @return 每周放送数据
     */
    public CompletableFuture<WeeklySchedule> getWeeklySchedule() {
        return CompletableFuture.supplyAsync(() -> {
            WeeklySchedule weeklySchedule = new WeeklySchedule();
            try {
                // 增加超时时间和重试次数
                Log.d(TAG, "开始获取每日放送表，URL: " + CALENDAR_URL);
                Document doc = CrawlerUtils.parseHtml(CALENDAR_URL, 5);
                
                // 打印HTML内容以便调试
                CrawlerUtils.logHtmlContent(doc, TAG, 1000);
                
                // 根据实际的HTML结构修改选择器
                // 可以看到，HTML中的结构是 #colunmSingle .BgmCalendar .large > li.week
                Elements weekdayElements = doc.select("#colunmSingle .BgmCalendar ul.large > li.week");
                Log.d(TAG, "找到星期容器数量: " + weekdayElements.size());
                
                if (weekdayElements.isEmpty()) {
                    Log.e(TAG, "未找到任何星期容器，尝试其他选择器");
                    // 尝试更宽松的选择器
                    weekdayElements = doc.select("li.week");
                    Log.d(TAG, "使用宽松选择器找到星期容器数量: " + weekdayElements.size());
                }
                
                // 遍历星期容器
                for (Element weekdayElement : weekdayElements) {
                    // 星期标题在dt元素中，如<dt class="Sun"><div><h3>星期日</h3></div></dt>
                    Element titleElement = weekdayElement.selectFirst("dt div h3");
                    if (titleElement == null) {
                        Log.d(TAG, "在星期容器中未找到标题元素dt div h3，尝试其他选择器");
                        titleElement = weekdayElement.selectFirst("dt");
                        if (titleElement == null) {
                            Log.d(TAG, "在星期容器中未找到标题元素dt，跳过");
                            continue;
                        }
                    }
                    
                    String weekdayText = titleElement.text();
                    Log.d(TAG, "找到星期标题: " + weekdayText);
                    
                    String dayOfWeek = extractDayOfWeek(weekdayText);
                    if (dayOfWeek == null) {
                        Log.d(TAG, "无法识别的星期: " + weekdayText);
                        continue;
                    }
                    
                    // 动漫列表在dd元素下的ul.coverList下的li元素
                    Elements animeItems = weekdayElement.select("dd ul.coverList > li");
                    Log.d(TAG, "找到 " + dayOfWeek + " 的 " + animeItems.size() + " 个动漫项");
                    
                    List<DailyAnime> animeList = new ArrayList<>();
                    
                    for (Element animeItem : animeItems) {
                        try {
                            DailyAnime anime = new DailyAnime();
                            
                            // 调试每个动漫项的HTML
                            Log.v(TAG, "动漫项HTML: " + animeItem.outerHtml().substring(0, Math.min(100, animeItem.outerHtml().length())) + "...");
                            
                            // 解析动漫标题，在info div中的第一个a标签
                            Element titleLinkElement = animeItem.selectFirst(".info p a.nav");
                            if (titleLinkElement != null) {
                                String title = titleLinkElement.text();
                                if (title != null && !title.isEmpty()) {
                                    anime.setTitleZh(title);
                                } else {
                                    Log.d(TAG, "标题为空，尝试获取下一个标题");
                                    // 尝试获取第二个标签中的标题
                                    Element secondTitleElement = animeItem.select(".info p a.nav").size() > 1 
                                        ? animeItem.select(".info p a.nav").get(1) : null;
                                    if (secondTitleElement != null) {
                                        Element smallEmElement = secondTitleElement.selectFirst("small em");
                                        if (smallEmElement != null) {
                                            anime.setTitleZh(smallEmElement.text());
                                        } else {
                                            anime.setTitleZh(secondTitleElement.text());
                                        }
                                    }
                                }
                                
                                // 提取ID和URL
                                String href = titleLinkElement.attr("href");
                                if (href.contains("/subject/")) {
                                    String id = href.substring(href.lastIndexOf("/") + 1);
                                    anime.setSourceId(id);
                                    // 确保URL完整
                                    anime.setSourceUrl(CrawlerUtils.ensureFullUrl(href, "https://chii.in"));
                                }
                            } else {
                                // 尝试其他选择器
                                Element altTitleElement = animeItem.selectFirst(".info a");
                                if (altTitleElement != null) {
                                    anime.setTitleZh(altTitleElement.text());
                                    
                                    // 提取ID和URL
                                    String href = altTitleElement.attr("href");
                                    if (href.contains("/subject/")) {
                                        String id = href.substring(href.lastIndexOf("/") + 1);
                                        anime.setSourceId(id);
                                        anime.setSourceUrl(CrawlerUtils.ensureFullUrl(href, "https://chii.in"));
                                    }
                                }
                            }
                            
                            // 解析原始标题（日文/英文标题）
                            Element subtitleElement = animeItem.selectFirst(".info p:nth-child(2) a.nav small em");
                            if (subtitleElement != null) {
                                anime.setTitleOriginal(subtitleElement.text());
                            }
                            
                            // 从背景样式中提取海报URL
                            String inlineStyle = animeItem.attr("style");
                            if (inlineStyle != null && !inlineStyle.isEmpty()) {
                                Log.v(TAG, "动漫项style属性: " + inlineStyle);
                                if (inlineStyle.contains("url(") && inlineStyle.contains(")")) {
                                    String posterUrl = inlineStyle.substring(inlineStyle.indexOf("url(") + 4, inlineStyle.indexOf(")"));
                                    // 移除可能存在的引号
                                    posterUrl = posterUrl.replace("'", "").replace("\"", "");
                                    // 解码HTML实体
                                    posterUrl = posterUrl.replace("&#39;", "'");
                                    Log.d(TAG, "提取到海报URL: " + posterUrl);
                                    anime.setPosterUrl(CrawlerUtils.ensureFullUrl(posterUrl, "https:"));
                                }
                            }
                            
                            if (anime.getPosterUrl() == null || anime.getPosterUrl().isEmpty()) {
                                Log.d(TAG, "未从样式中找到海报URL，尝试从详情页获取");
                                tryToGetPosterFromDetail(anime);
                            }
                            
                            // 添加到列表前检查是否有基本信息
                            if ((anime.getTitleZh() != null && !anime.getTitleZh().isEmpty()) || 
                                (anime.getTitleOriginal() != null && !anime.getTitleOriginal().isEmpty())) {
                                animeList.add(anime);
                                Log.d(TAG, "添加动漫: " + anime.getTitleZh() + ", ID: " + anime.getSourceId());
                            } else {
                                Log.w(TAG, "动漫标题为空，不添加");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析动漫项时出错: " + e.getMessage(), e);
                        }
                    }
                    
                    // 将当天动漫添加到对应的星期
                    if (!animeList.isEmpty()) {
                        Log.d(TAG, dayOfWeek + " 添加了 " + animeList.size() + " 个动漫");
                        switch (dayOfWeek) {
                            case "星期日":
                                weeklySchedule.setSundayAnime(animeList);
                                break;
                            case "星期一":
                                weeklySchedule.setMondayAnime(animeList);
                                break;
                            case "星期二":
                                weeklySchedule.setTuesdayAnime(animeList);
                                break;
                            case "星期三":
                                weeklySchedule.setWednesdayAnime(animeList);
                                break;
                            case "星期四":
                                weeklySchedule.setThursdayAnime(animeList);
                                break;
                            case "星期五":
                                weeklySchedule.setFridayAnime(animeList);
                                break;
                            case "星期六":
                                weeklySchedule.setSaturdayAnime(animeList);
                                break;
                        }
                    } else {
                        Log.w(TAG, dayOfWeek + " 没有找到任何动漫");
                    }
                }
                
                // 检查是否成功获取到任何数据
                Log.d(TAG, "爬取完成，检查数据: 星期日=" + 
                      (weeklySchedule.getSundayAnime() != null ? weeklySchedule.getSundayAnime().size() : 0) + 
                      ", 星期一=" + 
                      (weeklySchedule.getMondayAnime() != null ? weeklySchedule.getMondayAnime().size() : 0));
                      
            } catch (Exception e) {
                Log.e(TAG, "获取每日放送表失败: " + e.getMessage(), e);
                throw new RuntimeException("获取每日放送表失败", e);
            }
            return weeklySchedule;
        });
    }
    
    /**
     * 尝试从详情页获取海报
     */
    private void tryToGetPosterFromDetail(DailyAnime anime) {
        if (anime.getSourceId() == null || anime.getSourceId().isEmpty()) {
            return;
        }
        
        try {
            String detailUrl = "https://chii.in/subject/" + anime.getSourceId();
            Document detailDoc = CrawlerUtils.parseHtml(detailUrl, 3);
            
            Element posterElement = detailDoc.selectFirst("img.cover");
            if (posterElement != null) {
                String posterUrl = posterElement.attr("src");
                anime.setPosterUrl(CrawlerUtils.ensureFullUrl(posterUrl, "https:"));
            }
        } catch (Exception e) {
            Log.w(TAG, "获取详情页海报失败: " + e.getMessage());
        }
    }
    
    /**
     * 从标题中提取星期几
     */
    private String extractDayOfWeek(String weekdayText) {
        if (weekdayText == null) return null;
        
        weekdayText = weekdayText.toLowerCase();
        
        if (weekdayText.contains("星期日") || weekdayText.contains("sunday")) {
            return "星期日";
        } else if (weekdayText.contains("星期一") || weekdayText.contains("monday")) {
            return "星期一";
        } else if (weekdayText.contains("星期二") || weekdayText.contains("tuesday")) {
            return "星期二";
        } else if (weekdayText.contains("星期三") || weekdayText.contains("wednesday")) {
            return "星期三";
        } else if (weekdayText.contains("星期四") || weekdayText.contains("thursday")) {
            return "星期四";
        } else if (weekdayText.contains("星期五") || weekdayText.contains("friday")) {
            return "星期五";
        } else if (weekdayText.contains("星期六") || weekdayText.contains("saturday")) {
            return "星期六";
        }
        return null;
    }
}