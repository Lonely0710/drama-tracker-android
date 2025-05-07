package com.lonely.dramatracker.utils;

import android.util.Log;
import android.util.Pair;
import com.lonely.dramatracker.config.AppConfig;
import com.lonely.dramatracker.models.MediaInfo;
import com.lonely.dramatracker.models.SearchResult;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * TMDb API 爬虫
 * 负责从 The Movie Database (TMDb) API 获取电影和电视剧信息
 */
public class TMDbCrawler {
    private static final String TAG = "TMDbCrawler";
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    // 图片基础URL，w500是常见的图片尺寸
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    // 从AppConfig获取API Key
    private static final String API_KEY = AppConfig.TMDB_API_KEY_STATIC;
    
    private static TMDbCrawler instance;
    
    private TMDbCrawler() {
        // 私有构造函数
    }
    
    public static synchronized TMDbCrawler getInstance() {
        if (instance == null) {
            instance = new TMDbCrawler();
        }
        return instance;
    }

    /**
     * 获取高分电影列表
     * @param page 页码（从1开始）
     * @return 包含电影列表和总页数的Pair
     */
    public CompletableFuture<Pair<List<MediaInfo>, Integer>> getTopRatedMovies(int page) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建获取高分电影的URL
                String url = BASE_URL + "/movie/top_rated?api_key=" + API_KEY + "&language=zh-CN&page=" + page;
                
                String jsonResponse = CrawlerUtils.httpGet(url);
                return parseMediaList(jsonResponse, MediaInfo.TYPE_MOVIE);
            } catch (Exception e) {
                Log.e(TAG, "获取高分电影失败", e);
                throw new RuntimeException("获取高分电影失败", e);
            }
        });
    }
    
    /**
     * 获取高分电视剧列表
     * @param page 页码（从1开始）
     * @return 包含电视剧列表和总页数的Pair
     */
    public CompletableFuture<Pair<List<MediaInfo>, Integer>> getTopRatedTVShows(int page) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 构建获取高分电视剧的URL
                String url = BASE_URL + "/tv/top_rated?api_key=" + API_KEY + "&language=zh-CN&page=" + page;
                
                String jsonResponse = CrawlerUtils.httpGet(url);
                return parseMediaList(jsonResponse, MediaInfo.TYPE_TV);
            } catch (Exception e) {
                Log.e(TAG, "获取高分电视剧失败", e);
                throw new RuntimeException("获取高分电视剧失败", e);
            }
        });
    }
    
    /**
     * 解析媒体列表JSON数据
     * @return 包含媒体列表和总页数的Pair
     */
    private Pair<List<MediaInfo>, Integer> parseMediaList(String jsonData, String mediaType) throws Exception {
        List<MediaInfo> mediaList = new ArrayList<>();
        
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray results = jsonObject.getJSONArray("results");
        
        // 解析总页数
        int totalPages = jsonObject.optInt("total_pages", 1);
        
        // 遍历结果数组
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);
            MediaInfo media = convertToMediaInfo(item, mediaType, i + 1);
            mediaList.add(media);
        }
        
        return new Pair<>(mediaList, totalPages);
    }
    
    /**
     * 将JSON对象转换为MediaInfo对象
     */
    private MediaInfo convertToMediaInfo(JSONObject json, String mediaType, int rank) throws Exception {
        MediaInfo mediaInfo = new MediaInfo();
        
        // 获取TMDb ID
        int tmdbId = json.optInt("id", 0);
        
        // 设置排名（用于全部类别显示）
        mediaInfo.setRank(rank);
        mediaInfo.setId(tmdbId);
        
        // 设置媒体类型
        mediaInfo.setMediaType(mediaType);
        
        // 设置标题（根据不同媒体类型获取不同字段）
        if (mediaType.equals(MediaInfo.TYPE_MOVIE)) {
            mediaInfo.setTitleZh(json.optString("title"));
            mediaInfo.setTitleOriginal(json.optString("original_title"));
        } else {
            mediaInfo.setTitleZh(json.optString("name"));
            mediaInfo.setTitleOriginal(json.optString("original_name"));
        }
        
        // 设置评分（TMDb的评分范围是0-10）
        double voteAverage = json.optDouble("vote_average");
        mediaInfo.setRating((float) voteAverage);
        
        // 设置发布日期
        if (mediaType.equals(MediaInfo.TYPE_MOVIE)) {
            mediaInfo.setReleaseDate(json.optString("release_date"));
        } else {
            mediaInfo.setReleaseDate(json.optString("first_air_date"));
        }
        
        // 提取年份信息
        String releaseDate = mediaInfo.getReleaseDate();
        if (releaseDate != null && releaseDate.length() >= 4) {
            mediaInfo.setYear(releaseDate.substring(0, 4));
        }
        
        // 设置海报URL
        String posterPath = json.optString("poster_path");
        if (posterPath != null && !posterPath.isEmpty()) {
            mediaInfo.setPosterUrl(IMAGE_BASE_URL + posterPath);
        }
        
        // 设置简介
        mediaInfo.setSummary(json.optString("overview"));
        
        return mediaInfo;
    }
    
    /**
     * 获取高分电影和电视剧的混合列表（用于"全部"分类）
     * @param moviePage 电影页码
     * @param tvPage 电视剧页码
     * @return 包含混合列表和总页数的Pair
     */
    public CompletableFuture<Pair<List<MediaInfo>, Integer>> getTopRatedAll(int moviePage, int tvPage) {
        CompletableFuture<Pair<List<MediaInfo>, Integer>> moviesFuture = getTopRatedMovies(moviePage);
        CompletableFuture<Pair<List<MediaInfo>, Integer>> tvShowsFuture = getTopRatedTVShows(tvPage);
        
        return CompletableFuture.allOf(moviesFuture, tvShowsFuture)
                .thenApply(v -> {
                    try {
                        Pair<List<MediaInfo>, Integer> moviesResult = moviesFuture.get();
                        Pair<List<MediaInfo>, Integer> tvShowsResult = tvShowsFuture.get();
                        
                        List<MediaInfo> movies = moviesResult.first;
                        List<MediaInfo> tvShows = tvShowsResult.first;
                        
                        int totalMoviesPages = moviesResult.second;
                        int totalTVShowsPages = tvShowsResult.second;
                        
                        // 合并两个列表并按评分排序
                        List<MediaInfo> combined = new ArrayList<>();
                        combined.addAll(movies);
                        combined.addAll(tvShows);
                        
                        // 按评分降序排序
                        combined.sort((a, b) -> Float.compare(b.getRating(), a.getRating()));
                        
                        // 重新设置排名
                        for (int i = 0; i < combined.size(); i++) {
                            combined.get(i).setRank(i + 1);
                        }
                        
                        // 取两个分类中页数较多的作为总页数
                        int maxTotalPages = Math.max(totalMoviesPages, totalTVShowsPages);
                        
                        return new Pair<>(combined, maxTotalPages);
                    } catch (Exception e) {
                        Log.e(TAG, "合并高分内容失败", e);
                        return new Pair<>(new ArrayList<>(), 1);
                    }
                });
    }

    /**
     * 使用TMDb API搜索电影和电视剧
     * @param keyword 搜索关键词
     * @return 搜索结果列表的Future
     */
    public CompletableFuture<List<SearchResult>> search(String keyword) {
        // 使用CompletableFuture.supplyAsync在后台线程执行网络请求和解析
        return CompletableFuture.supplyAsync(() -> {
            List<SearchResult> results = new ArrayList<>();
            String encodedKeyword;
            try {
                // 对关键词进行URL编码
                encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                Log.e(TAG, "关键词URL编码失败", e);
                // 抛出运行时异常，中断执行
                throw new RuntimeException("Keyword encoding failed", e);
            }

            // 构建TMDb多重搜索API的URL
            // language=zh-CN 请求中文信息
            // include_adult=false 排除成人内容
            String searchUrl = BASE_URL + "/search/multi?query=" + encodedKeyword + "&api_key=" + API_KEY + "&language=zh-CN&include_adult=false";
            Log.d(TAG, "搜索TMDb URL: " + searchUrl);

            try {
                // 使用CrawlerUtils中的httpGet方法发起网络请求
                String jsonResponse = CrawlerUtils.httpGet(searchUrl);
                // 解析JSON响应
                JSONObject searchJsonObject = new JSONObject(jsonResponse);
                JSONArray items = searchJsonObject.getJSONArray("results");

                // 遍历搜索结果
                for (int i = 0; i < items.length(); i++) {
                    try {
                        JSONObject item = items.getJSONObject(i);
                        String mediaType = item.optString("media_type");

                        // 只处理电影(movie)和电视剧(tv)类型的结果
                        if (!"movie".equals(mediaType) && !"tv".equals(mediaType)) {
                            continue; // 跳过其他类型 (如 person)
                        }

                        SearchResult.Builder builder = new SearchResult.Builder();
                        int id = item.getInt("id");

                        // --- Fetch Details --- 
                        String detailsUrl = BASE_URL + "/" + mediaType + "/" + id + "?api_key=" + API_KEY + "&language=zh-CN&append_to_response=credits";
                        Log.d(TAG, "Fetching details: " + detailsUrl);
                        JSONObject detailsJson = null;
                        String detailsResponse = null;
                        try {
                            detailsResponse = CrawlerUtils.httpGet(detailsUrl);
                            detailsJson = new JSONObject(detailsResponse);
                        } catch (IOException | org.json.JSONException e) {
                            Log.e(TAG, "获取或解析TMDb详情失败 for ID " + id + ": " + e.getMessage());
                            // Fallback to search result data if details fail
                            detailsJson = item; // Use basic search data as fallback
                        }
                        
                        // Use detailsJson (or item as fallback) to populate builder
                        builder.setSourceType("tmdb");
                        builder.setSourceId(String.valueOf(id));
                        builder.setSourceUrl("https://www.themoviedb.org/" + mediaType + "/" + id);
                        builder.setMediaType(mediaType);

                        String title = detailsJson.optString("title", detailsJson.optString("name"));
                        builder.setTitleZh(title);
                        String originalTitle = detailsJson.optString("original_title", detailsJson.optString("original_name"));
                        builder.setTitleOriginal(originalTitle);
                        String releaseDate = detailsJson.optString("release_date", detailsJson.optString("first_air_date"));
                        builder.setReleaseDate(releaseDate);
                        // Year is set automatically by setReleaseDate in Builder

                        String posterPath = detailsJson.optString("poster_path", null);
                        if (posterPath != null && !posterPath.equals("null") && !posterPath.isEmpty()) {
                            builder.setPosterUrl(IMAGE_BASE_URL + posterPath);
                        } else {
                            builder.setPosterUrl(null);
                        }

                        double rating = detailsJson.optDouble("vote_average", -1.0);
                        if (rating >= 0) {
                            builder.setRatingImdb(rating);
                        }

                        builder.setSummary(detailsJson.optString("overview"));

                        // Extract Duration
                        String durationStr = "";
                        if ("movie".equals(mediaType)) {
                            int runtime = detailsJson.optInt("runtime", 0);
                            if (runtime > 0) {
                                durationStr = runtime + "分钟"; // Keep format for movies
                            }
                        } else if ("tv".equals(mediaType)) {
                            int episodes = detailsJson.optInt("number_of_episodes", 0);
                            if (episodes > 0) {
                                durationStr = String.valueOf(episodes); // Store only the number for TV shows
                            }
                            // Could also add season count: int seasons = detailsJson.optInt("number_of_seasons", 0);
                        }
                        builder.setDuration(durationStr);

                        // Extract Staff (Credits)
                        String staffStr = "";
                        JSONObject credits = detailsJson.optJSONObject("credits");
                        if (credits != null) {
                            // Get Directors (from Crew)
                            JSONArray crew = credits.optJSONArray("crew");
                            List<String> directors = new ArrayList<>();
                            if (crew != null) {
                                for (int j = 0; j < crew.length(); j++) {
                                    JSONObject crewMember = crew.getJSONObject(j);
                                    if ("Director".equals(crewMember.optString("job"))) {
                                        directors.add(crewMember.optString("name"));
                                    }
                                }
                            }
                            
                            // Get Actors (from Cast)
                            JSONArray cast = credits.optJSONArray("cast");
                            List<String> actors = new ArrayList<>();
                            if (cast != null) {
                                int limit = Math.min(cast.length(), 5); // Limit to 5 actors
                                for (int j = 0; j < limit; j++) {
                                    actors.add(cast.getJSONObject(j).optString("name"));
                                }
                            }

                            // Format Staff String
                            StringBuilder staffBuilder = new StringBuilder();
                            if (!directors.isEmpty()) {
                                staffBuilder.append("导演: ").append(String.join(", ", directors));
                            }
                            if (!actors.isEmpty()) {
                                if (staffBuilder.length() > 0) {
                                    staffBuilder.append(" | ");
                                }
                                staffBuilder.append("主演: ").append(String.join(", ", actors));
                            }
                            staffStr = staffBuilder.toString();
                        }
                        builder.setStaff(staffStr);
                        
                        results.add(builder.build());

                    } catch (Exception e) {
                        // 记录解析单个结果项时的错误，但继续处理下一个
                        Log.e(TAG, "处理TMDb结果项(包括详情)时出错 (Index: " + i + ")", e);
                    }
                }
            } catch (IOException e) {
                // 网络请求失败
                Log.e(TAG, "TMDb搜索HTTP请求失败", e);
                throw new RuntimeException("Search failed", e); // 抛出异常，由调用者处理
            } catch (Exception e) {
                // JSON解析失败或其他未知错误
                Log.e(TAG, "解析TMDb搜索JSON响应或处理时出错", e);
                 throw new RuntimeException("JSON parsing/processing failed", e); // 抛出异常
            }
            // 返回解析后的结果列表
            return results;
        });
    }
} 