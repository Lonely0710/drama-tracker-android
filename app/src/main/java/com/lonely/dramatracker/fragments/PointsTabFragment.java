package com.lonely.dramatracker.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.adapters.PointsAdapter;
import com.lonely.dramatracker.models.MediaInfo;
import com.lonely.dramatracker.models.WebSite;
import com.lonely.dramatracker.utils.TMDbCrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 高分推荐标签页的Fragment
 */
public class PointsTabFragment extends Fragment {
    private static final String TAG = "PointsTabFragment";

    // 内容类型常量
    public static final int TYPE_ALL = 0;
    public static final int TYPE_MOVIES = 1;
    public static final int TYPE_TV = 2;
    
    // 每页数据条数
    private static final int PAGE_SIZE = 39;
    // 最多显示的页码按钮数
    private static final int MAX_PAGE_BUTTONS = 5;
    
    // --- 分页相关状态变量 ---
    private boolean isLoading = false; // 是否正在加载数据
    private boolean isLoadingMore = false; // 是否正在加载更多
    private boolean isInitialLoading = false; // 是否初始加载
    private boolean isLastPage = false; // 是否已加载到最后一页
    private boolean isPageOperation = false; // 是否是页码操作触发的加载
    
    // 当前页码和总页数
    private int currentMoviePage = 1;
    private int currentTVPage = 1;
    private int totalMoviesPages = 1;
    private int totalTVPages = 1;
    
    // 当前显示的页码和总页数
    private int currentPage = 1;
    private int totalPages = 1;
    private int totalItems = 0;
    // --- 分页相关状态变量结束 ---
    
    // --- 缓存相关变量 ---
    // 分别缓存电影和电视剧的每一页数据
    private Map<Integer, List<MediaInfo>> moviePageCache = new HashMap<>();
    private Map<Integer, List<MediaInfo>> tvPageCache = new HashMap<>();
    private Map<Integer, List<MediaInfo>> allPageCache = new HashMap<>();
    // --- 缓存相关变量结束 ---

    // 视图组件
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipMovies, chipTv;
    private RecyclerView rvPointsContent;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    
    // 分页导航相关视图
    private View paginationLayout;
    private LinearLayout pageNumberContainer;
    private ImageButton btnPrevPage;
    private ImageButton btnNextPage;
    private TextView tvTotalPages;
    private ProgressBar paginationLoading;
    
    // 适配器
    private PointsAdapter adapter;
    
    // TMDB爬虫
    private TMDbCrawler tmdbCrawler;
    
    // 数据
    private List<MediaInfo> allMediaList = new ArrayList<>();
    private List<MediaInfo> moviesList = new ArrayList<>();
    private List<MediaInfo> tvList = new ArrayList<>();
    
    // 当前选中的内容类型
    private int currentType = TYPE_ALL;

    // 添加Handler
    private Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_points, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化视图
        initViews(view);
        
        // 立即显示加载动画，给用户更好的视觉反馈
        showInitialLoadingAnimation();
        
        // 设置事件监听
        setupListeners();
        
        // 初始化适配器
        initAdapter();
        
        // 初始化TMDb爬虫
        tmdbCrawler = TMDbCrawler.getInstance();
        
        // 使用Handler延迟执行加载数据，确保动画能够显示
        handler.post(() -> {
            // 加载数据
            loadData();
        });
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        chipGroupFilter = view.findViewById(R.id.chip_group_filter);
        chipAll = view.findViewById(R.id.chip_all);
        chipMovies = view.findViewById(R.id.chip_movies);
        chipTv = view.findViewById(R.id.chip_tv);
        
        rvPointsContent = view.findViewById(R.id.rv_points_content);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        
        // 设置文本内容
        chipAll.setText(R.string.top_rated_all);
        chipMovies.setText(R.string.top_rated_movies);
        chipTv.setText(R.string.top_rated_tv);
        tvEmpty.setText(R.string.top_rated_empty);
        
        // 初始化分页导航相关视图
        paginationLayout = view.findViewById(R.id.pagination_layout);
        if (paginationLayout != null) {
            pageNumberContainer = paginationLayout.findViewById(R.id.page_number_container);
            btnPrevPage = paginationLayout.findViewById(R.id.btn_prev_page);
            btnNextPage = paginationLayout.findViewById(R.id.btn_next_page);
            tvTotalPages = paginationLayout.findViewById(R.id.tv_total_pages);
            paginationLoading = paginationLayout.findViewById(R.id.pagination_loading);
            
            // 默认隐藏分页导航 - 数据加载后会显示
            paginationLayout.setVisibility(View.GONE);
            
            // 设置上一页/下一页按钮点击事件
            setupPaginationButtons();
        }
    }
    
    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 筛选Chip组监听
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            // 先拦截掉其他所有操作，显示加载动画
            showInitialLoadingAnimation();
            
            // 重置分页状态
            resetPaginationState();
            
            // 确保页面滚动到顶部
            scrollToTop();
            
            // 使用Handler延迟执行，确保先渲染加载动画
            handler.postDelayed(() -> {
                if (checkedId == R.id.chip_all) {
                    currentType = TYPE_ALL;
                    adapter.setCurrentType(TYPE_ALL);
                    
                    // 设置当前页码和总页数
                    currentPage = 1;
                    // 取两者中较大的页数作为总页数
                    totalPages = Math.max(totalMoviesPages, totalTVPages);
                    if (totalPages <= 0) totalPages = 1;
                    
                    // 直接加载第一页数据，确保所有类别使用相同的分页加载模式
                    loadMediaData(currentType, 1);
                    
                    // 调试日志
                    Log.d(TAG, "切换到全部类型 - 总页数: " + totalPages);
                } else if (checkedId == R.id.chip_movies) {
                    currentType = TYPE_MOVIES;
                    adapter.setCurrentType(TYPE_MOVIES);
                    
                    // 设置当前页码和总页数
                    currentPage = 1;
                    totalPages = totalMoviesPages;
                    if (totalPages <= 0) totalPages = 1;
                    
                    // 直接加载第一页数据，确保所有类别使用相同的分页加载模式
                    loadMediaData(currentType, 1);
                    
                    // 调试日志
                    Log.d(TAG, "切换到电影类型 - 总页数: " + totalPages);
                } else if (checkedId == R.id.chip_tv) {
                    currentType = TYPE_TV;
                    adapter.setCurrentType(TYPE_TV);
                    
                    // 设置当前页码和总页数
                    currentPage = 1;
                    totalPages = totalTVPages;
                    if (totalPages <= 0) totalPages = 1;
                    
                    // 直接加载第一页数据，确保所有类别使用相同的分页加载模式
                    loadMediaData(currentType, 1);
                    
                    // 调试日志
                    Log.d(TAG, "切换到电视剧类型 - 总页数: " + totalPages);
                }
            }, 200);
        });
        
        // 添加滚动监听 - 不再需要自动加载更多，改为纯粹的页码导航
        rvPointsContent.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                // 页码分页模式不需要滚动监听自动加载
                // 仅保留基本的滚动处理
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }
    
    /**
     * 初始化适配器
     */
    private void initAdapter() {
        adapter = new PointsAdapter(getContext());
        adapter.setCurrentType(TYPE_ALL);
        rvPointsContent.setAdapter(adapter);
        
        // 确保GridLayoutManager已经设置
        if (!(rvPointsContent.getLayoutManager() instanceof GridLayoutManager)) {
            rvPointsContent.setLayoutManager(new GridLayoutManager(getContext(), 3));
        }
        
        // 设置点击监听器
        adapter.setOnItemClickListener((position, item) -> {
            // 点击高分内容项打开TMDb详情页
            openTMDbDetail(item);
        });
    }
    
    /**
     * 加载高分推荐数据
     */
    private void loadData() {
        // 重置分页状态
        resetPaginationState();
        
        // 显示加载状态
        showLoading(true);
        
        // 重置页码
        currentMoviePage = 1;
        currentTVPage = 1;
        currentPage = 1;
        
        // 清空现有数据
        allMediaList.clear();
        moviesList.clear();
        tvList.clear();
        
        // 清空缓存
        moviePageCache.clear();
        tvPageCache.clear();
        allPageCache.clear();
        
        // 标记为初始加载状态
        isInitialLoading = true;
        
        // 根据当前选择的类型加载第一页数据
        loadMediaData(currentType, 1);
    }
    
    /**
     * 加载下一页数据
     */
    private void loadNextPage() {
        if (isLoading || isLastPage) return;
        
        int nextPage = currentPage + 1;
        loadMediaData(currentType, nextPage);
    }
    
    /**
     * 加载指定类型和页码的数据
     * @param contentType 内容类型（全部/电影/电视剧）
     * @param page 页码
     */
    private void loadMediaData(int contentType, int page) {
        // 确认page是有效的
        if (page < 1 || (totalPages > 0 && page > totalPages && isLastPage)) {
            return;
        }
        
        // 设置加载状态
        isLoading = true;
        isPageOperation = true;
        
        // 记录当前页码
        currentPage = page;
        
        // 判断是否是初始加载还是加载更多
        boolean isFirstPage = (page == 1);
        isLoadingMore = !isFirstPage;
        
        // 调试日志
        Log.d(TAG, String.format("加载数据 - 类型: %d, 页码: %d, 首页?: %b",
                contentType, page, isFirstPage));
        
        // 设置适当的加载提示
        if (isFirstPage) {
            // 首页加载，显示全屏加载
            showLoading(true);
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.GONE);
            }
        } else {
            // 非首页加载，显示页面加载动画
            showPageLoading(true);
        }
        
        // 根据内容类型选择不同的加载策略
        switch (contentType) {
            case TYPE_MOVIES:
                loadMoviesData(page);
                break;
            case TYPE_TV:
                loadTVData(page);
                break;
            case TYPE_ALL:
            default:
                loadAllData(page);
                break;
        }
    }
    
    /**
     * 显示页面加载动画
     */
    private void showPageLoading(boolean isLoading) {
        if (paginationLoading != null) {
            paginationLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            // 确保分页加载动画显示在最前面
            if (isLoading) paginationLoading.bringToFront();
        }
        
        if (isLoading) {
            // 显示过渡动画
            rvPointsContent.setAlpha(0.6f);
            // 禁用RecyclerView的点击和滚动
            rvPointsContent.setEnabled(false);
            
            // 禁用分页按钮
            if (btnPrevPage != null) btnPrevPage.setEnabled(false);
            if (btnNextPage != null) btnNextPage.setEnabled(false);
            
            // 显示加载动画
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.bringToFront(); // 确保显示在最前面
                progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                ));
            }
            
            // 确保在加载过程中分页导航可见（如果已添加到界面）
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.VISIBLE);
                paginationLayout.setAlpha(0.6f); // 半透明效果
            }
        } else {
            // 恢复正常显示
            rvPointsContent.setAlpha(1.0f);
            rvPointsContent.setEnabled(true);
            
            // 隐藏加载动画
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            
            // 恢复分页导航透明度
            if (paginationLayout != null) {
                paginationLayout.setAlpha(1.0f);
            }
            
            // 根据当前页码恢复分页按钮状态
            updatePaginationUI();
        }
    }
    
    /**
     * 加载所有类型混合内容
     */
    private void loadAllData(int page) {
        // 检查缓存
        if (allPageCache.containsKey(page)) {
            // 使用缓存数据
            List<MediaInfo> cachedData = allPageCache.get(page);
            handleAllDataResult(cachedData, page, totalPages);
            return;
        }
        
        // 同步电影和电视剧的页码
        currentMoviePage = page;
        currentTVPage = page;
        
        // 获取混合内容
        CompletableFuture<Pair<List<MediaInfo>, Integer>> allFuture = 
                tmdbCrawler.getTopRatedAll(currentMoviePage, currentTVPage);
        
        allFuture.thenAccept(result -> {
            if (getActivity() == null) return;
            
            try {
                List<MediaInfo> mediaList = result.first;
                int totalPages = result.second;
                
                // 更新总页数
                totalMoviesPages = Math.max(totalMoviesPages, totalPages);
                totalTVPages = Math.max(totalTVPages, totalPages);
                
                // 将结果缓存
                allPageCache.put(page, mediaList);
                
                // 处理结果
                getActivity().runOnUiThread(() -> {
                    handleAllDataResult(mediaList, page, totalPages);
                });
            } catch (Exception e) {
                handleLoadError(e);
            }
        }).exceptionally(this::handleLoadException);
    }
    
    /**
     * 处理全部类型数据加载结果
     */
    private void handleAllDataResult(List<MediaInfo> mediaList, int page, int totalPagesCount) {
        // 更新总页数
        this.totalPages = totalPagesCount;
        
        if (mediaList == null || mediaList.isEmpty()) {
            if (page == 1) {
                // 第一页没有数据，显示空状态
                showEmptyState();
            } else {
                // 后续页没有数据，标记为最后一页
                isLastPage = true;
                Toast.makeText(getContext(), "已经是最后一页", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 每页独立展示，不再累加
            // 保存到缓存中对应的页
            allPageCache.put(page, mediaList);
            
            // 如果是第一页，同时更新总列表（用于计数）
            if (page == 1) {
                allMediaList.clear();
                allMediaList.addAll(mediaList);
            } else {
                // 非第一页也需要更新总列表，确保计数准确
                if (allMediaList.isEmpty()) {
                    allMediaList.addAll(mediaList);
                }
            }
            
            // 判断是否到达最后一页
            if (mediaList.size() < PAGE_SIZE) {
                isLastPage = true;
            }
            
            // 更新UI - 只显示当前页内容
            if (currentType == TYPE_ALL) {
                adapter.setItems(mediaList);
            }
        }
        
        // 重置加载状态
        isLoading = false;
        isLoadingMore = false;
        isInitialLoading = false;
        
        // 隐藏加载提示
        if (page == 1) {
            showLoading(false);
        } else {
            showPageLoading(false);
        }
        
        // 更新分页UI
        updatePaginationUI();
        
        // 确保分页导航显示
        if (paginationLayout != null && mediaList != null && !mediaList.isEmpty()) {
            paginationLayout.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 加载电影数据
     */
    private void loadMoviesData(int page) {
        // 检查缓存
        if (moviePageCache.containsKey(page)) {
            // 使用缓存数据
            List<MediaInfo> cachedData = moviePageCache.get(page);
            handleMoviesDataResult(cachedData, page, totalMoviesPages);
            return;
        }
        
        // 设置当前电影页
        currentMoviePage = page;
        
        // 获取高分电影
        CompletableFuture<Pair<List<MediaInfo>, Integer>> moviesFuture = 
                tmdbCrawler.getTopRatedMovies(page);
        
        moviesFuture.thenAccept(result -> {
            if (getActivity() == null) return;
            
            try {
                List<MediaInfo> movies = result.first;
                int totalPages = result.second;
                
                // 更新总页数
                totalMoviesPages = totalPages;
                
                // 将结果缓存
                moviePageCache.put(page, movies);
                
                // 处理结果
                getActivity().runOnUiThread(() -> {
                    handleMoviesDataResult(movies, page, totalPages);
                });
            } catch (Exception e) {
                handleLoadError(e);
            }
        }).exceptionally(this::handleLoadException);
    }
    
    /**
     * 处理电影数据加载结果
     */
    private void handleMoviesDataResult(List<MediaInfo> movies, int page, int totalPagesCount) {
        // 更新总页数
        this.totalPages = totalPagesCount;
        
        if (movies == null || movies.isEmpty()) {
            if (page == 1) {
                // 第一页没有数据，显示空状态
                showEmptyState();
            } else {
                // 后续页没有数据，标记为最后一页
                isLastPage = true;
                Toast.makeText(getContext(), "已经是最后一页", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 每页独立展示，不再累加
            // 保存到缓存中对应的页
            moviePageCache.put(page, movies);
            
            // 如果是第一页，同时更新总列表（用于计数）
            if (page == 1) {
                moviesList.clear();
                moviesList.addAll(movies);
            } else {
                // 非第一页也需要更新总列表，确保计数准确
                if (moviesList.isEmpty()) {
                    moviesList.addAll(movies);
                }
            }
            
            // 判断是否到达最后一页
            if (movies.size() < PAGE_SIZE) {
                isLastPage = true;
            }
            
            // 更新UI - 只显示当前页内容
            if (currentType == TYPE_MOVIES) {
                adapter.setItems(movies);
            }
        }
        
        // 重置加载状态
        isLoading = false;
        isLoadingMore = false;
        isInitialLoading = false;
        
        // 隐藏加载提示
        if (page == 1) {
            showLoading(false);
        } else {
            showPageLoading(false);
        }
        
        // 更新分页UI - 无论什么情况都显示分页导航
        updatePaginationUI();
        
        // 确保分页导航显示
        if (paginationLayout != null && movies != null && !movies.isEmpty()) {
            paginationLayout.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 加载电视剧数据
     */
    private void loadTVData(int page) {
        // 检查缓存
        if (tvPageCache.containsKey(page)) {
            // 使用缓存数据
            List<MediaInfo> cachedData = tvPageCache.get(page);
            handleTVDataResult(cachedData, page, totalTVPages);
            return;
        }
        
        // 设置当前电视剧页
        currentTVPage = page;
        
        // 获取高分电视剧
        CompletableFuture<Pair<List<MediaInfo>, Integer>> tvFuture = 
                tmdbCrawler.getTopRatedTVShows(page);
        
        tvFuture.thenAccept(result -> {
            if (getActivity() == null) return;
            
            try {
                List<MediaInfo> tvShows = result.first;
                int totalPages = result.second;
                
                // 更新总页数
                totalTVPages = totalPages;
                
                // 将结果缓存
                tvPageCache.put(page, tvShows);
                
                // 处理结果
                getActivity().runOnUiThread(() -> {
                    handleTVDataResult(tvShows, page, totalPages);
                });
            } catch (Exception e) {
                handleLoadError(e);
            }
        }).exceptionally(this::handleLoadException);
    }
    
    /**
     * 处理电视剧数据加载结果
     */
    private void handleTVDataResult(List<MediaInfo> tvShows, int page, int totalPagesCount) {
        // 更新总页数
        this.totalPages = totalPagesCount;
        
        if (tvShows == null || tvShows.isEmpty()) {
            if (page == 1) {
                // 第一页没有数据，显示空状态
                showEmptyState();
            } else {
                // 后续页没有数据，标记为最后一页
                isLastPage = true;
                Toast.makeText(getContext(), "已经是最后一页", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 每页独立展示，不再累加
            // 保存到缓存中对应的页
            tvPageCache.put(page, tvShows);
            
            // 如果是第一页，同时更新总列表（用于计数）
            if (page == 1) {
                tvList.clear();
                tvList.addAll(tvShows);
            } else {
                // 非第一页也需要更新总列表，确保计数准确
                if (tvList.isEmpty()) {
                    tvList.addAll(tvShows);
                }
            }
            
            // 判断是否到达最后一页
            if (tvShows.size() < PAGE_SIZE) {
                isLastPage = true;
            }
            
            // 更新UI - 只显示当前页内容
            if (currentType == TYPE_TV) {
                adapter.setItems(tvShows);
            }
        }
        
        // 重置加载状态
        isLoading = false;
        isLoadingMore = false;
        isInitialLoading = false;
        
        // 隐藏加载提示
        if (page == 1) {
            showLoading(false);
        } else {
            showPageLoading(false);
        }
        
        // 更新分页UI - 无论什么情况都显示分页导航
        updatePaginationUI();
        
        // 确保分页导航显示
        if (paginationLayout != null && tvShows != null && !tvShows.isEmpty()) {
            paginationLayout.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 显示空状态
     */
    private void showEmptyState() {
        tvEmpty.setVisibility(View.VISIBLE);
        rvPointsContent.setVisibility(View.GONE);
        
        if (paginationLayout != null) {
            paginationLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 打开TMDb详情页
     */
    private void openTMDbDetail(MediaInfo media) {
        if (media == null || getActivity() == null) return;
        
        try {
            // 构建TMDb详情页URL
            String url;
            if (MediaInfo.TYPE_MOVIE.equals(media.getMediaType())) {
                url = "https://www.themoviedb.org/movie/" + media.getId();
            } else {
                url = "https://www.themoviedb.org/tv/" + media.getId();
            }
            
            // 检查WebViewFragment是否已存在
            FragmentManager fragmentManager = getParentFragment().getParentFragmentManager();
            WebViewFragment webViewFragment = (WebViewFragment) fragmentManager.findFragmentByTag("WebViewFragment");
            
            if (webViewFragment != null) {
                // 如果WebViewFragment已存在，更新URL
                webViewFragment.loadUrl(url);
            } else {
                // 创建并打开WebViewFragment
                webViewFragment = new WebViewFragment();
                Bundle args = new Bundle();
                args.putString("site_name", "TMDB");
                args.putString("custom_url", url);
                webViewFragment.setArguments(args);
                
                // 打开WebViewFragment
                fragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.fragment_container, webViewFragment, "WebViewFragment")
                        .addToBackStack(null)
                        .commit();
            }
        } catch (Exception e) {
            Log.e(TAG, "打开TMDb详情页失败", e);
            Toast.makeText(getContext(), "打开详情页失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查是否显示空状态
     */
    private void checkEmptyState() {
        List<?> currentList = null;
        
        switch (currentType) {
            case TYPE_ALL:
                currentList = allMediaList;
                break;
            case TYPE_MOVIES:
                currentList = moviesList;
                break;
            case TYPE_TV:
                currentList = tvList;
                break;
        }
        
        if (currentList == null || currentList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvPointsContent.setVisibility(View.GONE);
            // 空状态下隐藏分页导航
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.GONE);
            }
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvPointsContent.setVisibility(View.VISIBLE);
            // 有数据时显示分页导航
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.VISIBLE);
                // 确保更新分页UI
                updatePaginationUI();
            }
        }
    }
    
    /**
     * 显示或隐藏加载状态
     */
    private void showLoading(boolean isLoading) {
        if (isLoading && isInitialLoading) {
            // 初始加载，显示中心加载器
            progressBar.setVisibility(View.VISIBLE);
            progressBar.bringToFront(); // 确保显示在最上层
            progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ));
            rvPointsContent.setVisibility(View.VISIBLE);
            rvPointsContent.setAlpha(0.5f); // 半透明效果
            rvPointsContent.setEnabled(false); // 禁用交互
            tvEmpty.setVisibility(View.GONE);
            
            // 隐藏分页导航
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.GONE);
            }
        } else if (isLoading && isLoadingMore) {
            // 加载更多，显示底部加载器
            progressBar.setVisibility(View.VISIBLE);
            progressBar.bringToFront(); // 确保显示在最上层
            progressBar.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
            ));
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) progressBar.getLayoutParams();
            params.bottomMargin = 8;
            progressBar.setLayoutParams(params);
            
            // 显示分页导航但禁用按钮
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.VISIBLE);
                paginationLayout.setAlpha(0.5f); // 半透明效果
                if (btnPrevPage != null) btnPrevPage.setEnabled(false);
                if (btnNextPage != null) btnNextPage.setEnabled(false);
            }
        } else {
            // 加载完成
            progressBar.setVisibility(View.GONE);
            
            // 检查是否显示空状态
            checkEmptyState();
            
            // 恢复列表透明度和交互
            rvPointsContent.setAlpha(1.0f);
            rvPointsContent.setEnabled(true);
            
        }
    }

    /**
     * 重置分页状态
     */
    private void resetPaginationState() {
        isLastPage = false;
        isLoading = false;
        isLoadingMore = false;
        isPageOperation = false;
    }
    
    /**
     * 设置分页按钮的点击事件
     */
    private void setupPaginationButtons() {
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1 && !isLoading) {
                // 先滚动到顶部
                scrollToTop();
                
                // 加载上一页前先显示加载动画
                showPageLoading(true);
                
                // 使用Handler延迟执行加载，确保滚动和动画效果先显示
                handler.postDelayed(() -> {
                    // 加载上一页
                    loadMediaData(currentType, currentPage - 1);
                }, 200); // 增加延迟，确保滚动效果完成
            }
        });
        
        btnNextPage.setOnClickListener(v -> {
            if (!isLoading) {
                // 先滚动到顶部
                scrollToTop();
                
                // 加载下一页前先显示加载动画
                showPageLoading(true);
                
                // 使用Handler延迟执行加载，确保滚动和动画效果先显示
                handler.postDelayed(() -> {
                    if (currentPage < totalPages) {
                        // 加载下一页
                        loadMediaData(currentType, currentPage + 1);
                    } else if (!isLastPage) {
                        // 总页数可能不准确，尝试加载下一页
                        loadMediaData(currentType, currentPage + 1);
                    }
                }, 200); // 增加延迟，确保滚动效果完成
            }
        });
    }
    
    /**
     * 更新分页导航UI
     */
    private void updatePaginationUI() {
        if (paginationLayout == null || pageNumberContainer == null) {
            return;
        }
        
        // 显示分页导航栏
        paginationLayout.setVisibility(View.VISIBLE);
        
        // 根据当前类型设置总数和页数
        int itemCount = 0;
        switch (currentType) {
            case TYPE_ALL:
                itemCount = allMediaList.size();
                // 全部类型使用两者中较大的页数
                totalPages = Math.max(totalMoviesPages, totalTVPages);
                break;
            case TYPE_MOVIES:
                itemCount = moviesList.size();
                // 电影类型使用电影的总页数
                totalPages = totalMoviesPages;
                // 确保totalPages至少为1
                if (totalPages <= 0) totalPages = 1;
                break;
            case TYPE_TV:
                itemCount = tvList.size();
                // 电视剧类型使用电视剧的总页数
                totalPages = totalTVPages;
                // 确保totalPages至少为1
                if (totalPages <= 0) totalPages = 1;
                break;
        }
        
        // 调试日志
        Log.d(TAG, String.format("更新分页UI - 类型: %d, 当前页: %d, 总页数: %d, 项目数: %d",
                currentType, currentPage, totalPages, itemCount));
        
        // 设置总页数提示
        tvTotalPages.setText(String.format("共 %d 页 (约%d项)", totalPages, itemCount));
        
        // 上一页按钮状态
        btnPrevPage.setEnabled(currentPage > 1);
        btnPrevPage.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
        
        // 下一页按钮状态
        btnNextPage.setEnabled(!isLastPage && currentPage < totalPages);
        btnNextPage.setAlpha(!isLastPage && currentPage < totalPages ? 1.0f : 0.5f);
        
        // 清空原有页码按钮
        pageNumberContainer.removeAllViews();
        
        // 如果总页数很小，直接显示所有页码
        if (totalPages <= MAX_PAGE_BUTTONS) {
            for (int i = 1; i <= totalPages; i++) {
                addPageButton(String.valueOf(i), i);
            }
            return;
        }
        
        // 对于较多页码的情况，使用更智能的显示逻辑
        // 始终显示第一页、最后一页，以及当前页的前后共5个页码
        
        // 确定显示的页码范围
        int halfRange = MAX_PAGE_BUTTONS / 2;
        int startPage = Math.max(1, currentPage - halfRange);
        int endPage = Math.min(totalPages, startPage + MAX_PAGE_BUTTONS - 1);
        
        // 调整startPage，确保能显示足够的页码
        if (endPage - startPage + 1 < MAX_PAGE_BUTTONS) {
            startPage = Math.max(1, endPage - MAX_PAGE_BUTTONS + 1);
        }
        
        // 显示第一页（始终显示）
        addPageButton("1", 1);
        
        // 如果起始页不是第一页，显示省略号
        if (startPage > 2) {
            addPageButton("...", -1);
        }
        
        // 显示中间的页码
        for (int i = Math.max(2, startPage); i <= Math.min(endPage, totalPages - 1); i++) {
            addPageButton(String.valueOf(i), i);
        }
        
        // 显示省略号
        if (endPage < totalPages - 1) {
            addPageButton("...", -1);
        }
        
        // 显示最后一页（如果总页数大于1）
        if (totalPages > 1) {
            addPageButton(String.valueOf(totalPages), totalPages);
        }
    }
    
    /**
     * 添加页码按钮
     */
    private void addPageButton(String text, int pageNum) {
        boolean isActive = pageNum == currentPage;
        
        // 创建按钮
        TextView pageButton = new TextView(requireContext());
        pageButton.setText(text);
        pageButton.setTextSize(14);
        pageButton.setGravity(android.view.Gravity.CENTER);
        
        // 设置布局参数
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.page_indicator_size),  // 40dp
                getResources().getDimensionPixelSize(R.dimen.page_indicator_size)   // 40dp
        );
        params.setMargins(4, 0, 4, 0);
        pageButton.setLayoutParams(params);
        
        // 设置样式
        if (isActive) {
            // 当前页样式
            pageButton.setBackgroundResource(R.drawable.bg_page_number_active);
            pageButton.setTextColor(Color.WHITE);
        } else {
            // 其他页样式
            pageButton.setBackgroundResource(R.drawable.bg_page_number_normal);
            pageButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }
        
        // 设置点击事件（只有实际页码按钮才可点击）
        if (pageNum > 0) {
            pageButton.setOnClickListener(v -> {
                if (currentPage != pageNum && !isLoading) {
                    // 先滚动到顶部
                    scrollToTop();
                    
                    // 显示加载动画
                    showPageLoading(true);
                    
                    // 使用Handler延迟执行加载，确保滚动和动画效果先显示
                    handler.postDelayed(() -> {
                        // 点击页码按钮时加载对应页面
                        loadMediaData(currentType, pageNum);
                    }, 200); // 增加延迟，确保滚动效果完成
                }
            });
        } else {
            pageButton.setEnabled(false);
        }
        
        // 添加到容器
        pageNumberContainer.addView(pageButton);
    }

    /**
     * 处理加载异常
     */
    private Void handleLoadException(Throwable throwable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                handleLoadError(throwable);
            });
        }
        Log.e(TAG, "加载高分内容失败", throwable);
        return null;
    }
    
    /**
     * 处理加载错误
     */
    private void handleLoadError(Throwable throwable) {
        isInitialLoading = false;
        isLoadingMore = false;
        isLoading = false;
        
        // 根据当前加载的页码决定如何隐藏加载提示
        if (currentPage == 1) {
            showLoading(false);
        } else {
            showPageLoading(false);
        }
        
        Toast.makeText(getContext(), R.string.load_failed, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "加载高分内容失败", throwable);
    }

    /**
     * 显示初始加载动画
     */
    private void showInitialLoadingAnimation() {
        if (progressBar != null) {
            // 确保进度条居中显示
            progressBar.setVisibility(View.VISIBLE);
            // 提高progressBar的层级，确保不被遮挡
            progressBar.bringToFront();
            // 使用FrameLayout的居中布局参数
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            );
            progressBar.setLayoutParams(params);
        }
        
        // 隐藏所有内容视图，但保持半透明效果以便用户知道正在加载
        if (rvPointsContent != null) {
            rvPointsContent.setVisibility(View.VISIBLE);
            rvPointsContent.setAlpha(0.5f);
            rvPointsContent.setEnabled(false); // 禁用点击
        }
        
        if (tvEmpty != null) {
            tvEmpty.setVisibility(View.GONE);
        }
        
        // 处理分页导航 - 先隐藏，数据加载后再显示
        if (paginationLayout != null) {
            paginationLayout.setVisibility(View.GONE);
        }
        
        // 重置适配器内容，保证在切换标签时没有"闪烁"
        if (adapter != null) {
            adapter.setItems(new ArrayList<>());
        }
        
        // 调试日志
        Log.d(TAG, "显示初始加载动画");
    }

    /**
     * 滚动到顶部
     */
    private void scrollToTop() {
        if (rvPointsContent != null) {
            // 确保RecyclerView可见
            rvPointsContent.setVisibility(View.VISIBLE);
            
            // 立即滚动到顶部
            rvPointsContent.scrollToPosition(0);
            
            // 再使用平滑滚动，提供更好的视觉效果
            handler.postDelayed(() -> {
                if (rvPointsContent != null && isAdded()) {
                    rvPointsContent.smoothScrollToPosition(0);
                }
            }, 50);
        }
    }

    /**
     * 在Fragment创建完成后手动更新分页UI
     */
    @Override
    public void onResume() {
        super.onResume();
        
        // 如果已经有数据，确保分页导航显示
        if ((currentType == TYPE_ALL && !allMediaList.isEmpty()) ||
            (currentType == TYPE_MOVIES && !moviesList.isEmpty()) ||
            (currentType == TYPE_TV && !tvList.isEmpty())) {
            
            // 确保分页状态正确
            updatePaginationUI();
            
            // 显示分页导航
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.VISIBLE);
            }
            
            Log.d(TAG, "onResume: 更新分页UI，当前类型: " + currentType);
        }
    }
} 