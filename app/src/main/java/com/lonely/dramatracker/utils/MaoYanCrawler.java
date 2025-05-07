package com.lonely.dramatracker.utils;

import android.util.Log;

import com.lonely.dramatracker.models.MovieInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 猫眼电影爬虫
 * 负责从猫眼电影网站API获取电影信息
 */
public class MaoYanCrawler {
    private static final String TAG = "MaoYanCrawler";
    
    // 猫眼API接口
    private static final String MOVIE_ON_SHOWING_URL = "https://m.maoyan.com/ajax/movieOnInfoList"; // 正在热映
    private static final String MOVIE_COMING_URL = "https://m.maoyan.com/ajax/comingList?ci=1&token=&limit=10"; // 即将上映
    private static final String MOVIE_DETAIL_URL = "https://m.maoyan.com/ajax/detailmovie?movieId="; // 电影详情
    
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36";
    
    private static MaoYanCrawler instance;
    
    private MaoYanCrawler() {
        // 私有构造函数
    }
    
    public static synchronized MaoYanCrawler getInstance() {
        if (instance == null) {
            instance = new MaoYanCrawler();
        }
        return instance;
    }
    
    /**
     * 获取近期上映的电影列表（兼容RecentTabFragment）
     * @return 电影列表的Future
     */
    public CompletableFuture<List<MovieInfo>> getCurrentMovies() {
        return getMoviesOnShowing();
    }
    
    /**
     * 获取正在热映的电影列表
     * @return 电影列表的Future
     */
    public CompletableFuture<List<MovieInfo>> getMoviesOnShowing() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 使用工具类发送请求
                String jsonData = CrawlerUtils.httpGet(MOVIE_ON_SHOWING_URL);
                return parseMoviesOnShowing(jsonData);
            } catch (Exception e) {
                Log.e(TAG, "获取正在热映电影失败", e);
                throw new RuntimeException("获取电影列表失败", e);
            }
        });
    }
    
    /**
     * 解析正在热映的电影JSON数据
     */
    private List<MovieInfo> parseMoviesOnShowing(String jsonData) throws JSONException {
        List<MovieInfo> movies = new ArrayList<>();
        
        JSONObject jsonObject = new JSONObject(jsonData);
        
        // 解析电影列表数据
        if (jsonObject.has("movieList")) {
            JSONArray movieArray = jsonObject.getJSONArray("movieList");
            
            for (int i = 0; i < movieArray.length(); i++) {
                JSONObject movieObj = movieArray.getJSONObject(i);
                MovieInfo movie = new MovieInfo();
                
                // 电影ID
                movie.setId(movieObj.optInt("id"));
                
                // 电影名称
                movie.setMovieName(movieObj.optString("nm"));
                
                // 评分
                if (!movieObj.isNull("sc")) {
                    movie.setScore(movieObj.optDouble("sc"));
                } else {
                    movie.setScore(0); // 暂无评分
                }
                
                // 海报URL - 修复：检查URL是否已经包含http前缀
                String posterPath = movieObj.optString("img");
                if (!posterPath.isEmpty()) {
                    if (posterPath.startsWith("http")) {
                        movie.setPoster(posterPath); // 已经是完整URL
                    } else {
                        movie.setPoster("https://p0.meituan.net/movie/" + posterPath);
                    }
                }
                
                // 主演
                if (movieObj.has("star")) {
                    String[] actors = movieObj.optString("star").split(",");
                    movie.setActors(Arrays.asList(actors));
                }
                
                // 时长
                movie.setDuration(movieObj.optString("dur") + "分钟");
                
                // 上映日期
                movie.setReleaseDate(movieObj.optString("rt"));
                
                // 是否新上映
                movie.setNew(movieObj.optBoolean("isNew"));
                
                // 电影类型/标签 - 修复：确保cat字段存在，并正确解析
                if (movieObj.has("cat")) {
                    String catStr = movieObj.optString("cat");
                    if (catStr != null && !catStr.isEmpty()) {
                        String[] tags = catStr.split(",");
                        movie.setGenres(Arrays.asList(tags));
                    } else {
                        movie.setGenres(new ArrayList<>()); // 设置空列表，避免空指针
                    }
                } else {
                    movie.setGenres(new ArrayList<>()); // 设置空列表，避免空指针
                }
                
                // 想看人数
                movie.setWish(movieObj.optString("wish"));
                
                // 获取原始名称或设置与中文名相同
                movie.setOriginalName(movieObj.optString("enm", movie.getMovieName()));
                
                // 电影简介需要从详情接口获取，这里先置空
                movie.setSummary("");
                
                // 获取电影详情以补充简介
                try {
                    String detailJson = CrawlerUtils.httpGet(MOVIE_DETAIL_URL + movie.getId());
                    extractMovieSummary(movie, detailJson);
                } catch (IOException e) {
                    Log.e(TAG, "获取电影" + movie.getId() + "详情失败", e);
                }
                
                movies.add(movie);
            }
        }
        
        return movies;
    }
    
    /**
     * 从电影详情中提取简介
     */
    private void extractMovieSummary(MovieInfo movie, String detailJson) {
        try {
            JSONObject jsonObject = new JSONObject(detailJson);
            if (jsonObject.has("detailMovie")) {
                JSONObject detailObj = jsonObject.getJSONObject("detailMovie");
                
                // 提取简介
                if (detailObj.has("dra")) {
                    movie.setSummary(detailObj.optString("dra"));
                }
                
                // 如果没有原始标题，尝试从详情页获取
                if (movie.getOriginalName() == null || movie.getOriginalName().isEmpty() || 
                        movie.getOriginalName().equals(movie.getMovieName())) {
                    movie.setOriginalName(detailObj.optString("enm", movie.getMovieName()));
                }
                
                // 如果电影类型为空，尝试从详情页获取
                if (movie.getGenres() == null || movie.getGenres().isEmpty()) {
                    if (detailObj.has("cat")) {
                        String catStr = detailObj.optString("cat");
                        if (catStr != null && !catStr.isEmpty()) {
                            String[] tags = catStr.split(",");
                            movie.setGenres(Arrays.asList(tags));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析电影详情失败", e);
        }
    }
    
    /**
     * 获取电影详情
     * @param movieId 电影ID
     * @return 电影详情的Future
     */
    public CompletableFuture<MovieInfo> getMovieDetail(int movieId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonData = CrawlerUtils.httpGet(MOVIE_DETAIL_URL + movieId);
                return parseMovieDetail(jsonData);
            } catch (Exception e) {
                Log.e(TAG, "获取电影详情失败", e);
                throw new RuntimeException("获取电影详情失败", e);
            }
        });
    }
    
    /**
     * 解析电影详情JSON数据
     */
    private MovieInfo parseMovieDetail(String jsonData) throws JSONException {
        MovieInfo movie = new MovieInfo();
        
        JSONObject jsonObject = new JSONObject(jsonData);
        
        if (jsonObject.has("detailMovie")) {
            JSONObject detailObj = jsonObject.getJSONObject("detailMovie");
            
            // 电影ID
            movie.setId(detailObj.optInt("id"));
            
            // 电影名称
            movie.setMovieName(detailObj.optString("nm"));
            
            // 评分
            if (!detailObj.isNull("sc")) {
                movie.setScore(detailObj.optDouble("sc"));
            } else {
                movie.setScore(0); // 暂无评分
            }
            
            // 海报URL - 修复：检查URL是否已经包含http前缀
            String posterPath = detailObj.optString("img");
            if (!posterPath.isEmpty()) {
                if (posterPath.startsWith("http")) {
                    movie.setPoster(posterPath); // 已经是完整URL
                } else {
                    movie.setPoster("https://p0.meituan.net/movie/" + posterPath);
                }
            }
            
            // 电影简介
            movie.setSummary(detailObj.optString("dra"));
            
            // 主演
            if (detailObj.has("star")) {
                String[] actors = detailObj.optString("star").split(",");
                movie.setActors(Arrays.asList(actors));
            }
            
            // 原始名称
            movie.setOriginalName(detailObj.optString("enm", movie.getMovieName()));
            
            // 电影类型 - 修复：确保cat字段正确解析
            if (detailObj.has("cat")) {
                String catStr = detailObj.optString("cat");
                if (catStr != null && !catStr.isEmpty()) {
                    String[] genres = catStr.split(",");
                    movie.setGenres(Arrays.asList(genres));
                } else {
                    movie.setGenres(new ArrayList<>()); // 设置空列表，避免空指针
                }
            } else {
                movie.setGenres(new ArrayList<>()); // 设置空列表，避免空指针
            }
            
            // 时长
            movie.setDuration(detailObj.optString("dur") + "分钟");
            
            // 上映日期
            movie.setReleaseDate(detailObj.optString("rt"));
            
            // 是否新上映
            movie.setNew(detailObj.optBoolean("isNew", false));
            
            // 想看人数
            movie.setWish(detailObj.optString("wish"));
        }
        
        return movie;
    }
} 