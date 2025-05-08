package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.lonely.dramatracker.utils.PaginationHelper;
import com.lonely.dramatracker.utils.TMDbCrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高分推荐标签页的Fragment
 */
public class PointsTabFragment extends Fragment implements 
        PaginationHelper.OnPageLoadListener,
        PaginationHelper.CategoryProvider, 
        PaginationHelper.EmptyStateChecker {
    private static final String TAG = "PointsTabFragment";

    // 内容类型常量
    public static final int TYPE_ALL = 0;
    public static final int TYPE_MOVIES = 1;
    public static final int TYPE_TV = 2;
    
    // 每页数据条数
    private static final int PAGE_SIZE = 21; // 修改为21，使页面展示更合理
    private static final int API_PAGE_SIZE = 20; // API每次返回20条数据
    
    // --- 缓存相关变量 ---
    // 分别缓存电影和电视剧的每一页数据
    private Map<Integer, List<MediaInfo>> moviePageCache = new HashMap<>();
    private Map<Integer, List<MediaInfo>> tvPageCache = new HashMap<>();
    private Map<Integer, List<MediaInfo>> allPageCache = new HashMap<>();
    
    // API临时数据缓存
    private List<MediaInfo> moviesTempCache = new ArrayList<>();
    private List<MediaInfo> tvTempCache = new ArrayList<>();
    private List<MediaInfo> allTempCache = new ArrayList<>();
    
    // 标记是否已无法从API获取更多数据
    private boolean noMoreApiDataForMovies = false;
    private boolean noMoreApiDataForTv = false;
    private boolean noMoreApiDataForAll = false;
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
    
    // 分页助手
    private PaginationHelper paginationHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_points, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化分页助手 - 3种内容类型：全部、电影、电视剧
        paginationHelper = new PaginationHelper(requireContext(), 3);
        paginationHelper.setOnPageLoadListener(this);
        
        // 初始化视图
        initViews(view);
        
        // 立即显示加载动画，给用户更好的视觉反馈
        paginationHelper.showInitialLoadingAnimation();
        
        // 设置事件监听
        setupListeners();
        
        // 初始化适配器
        initAdapter();
        
        // 初始化TMDb爬虫
        tmdbCrawler = TMDbCrawler.getInstance();
        
        // 使用Handler延迟执行加载数据，确保动画能够显示
        handler.post(this::initializePageInfo);
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
            
            // 将视图组件传递给分页助手
            paginationHelper.setupViews(
                paginationLayout,
                pageNumberContainer,
                btnPrevPage,
                btnNextPage,
                tvTotalPages,
                paginationLoading,
                progressBar,
                rvPointsContent,
                tvEmpty
            );
        }
    }
    
    /**
     * 初始化页面信息 - 先获取各分类的总页数
     */
    private void initializePageInfo() {
        // 显示加载状态
        paginationHelper.showLoading(true);
        
        // 清空所有缓存数据
        clearAllData();
        
        // 1. 获取电影分类的总页数和第一页数据
        tmdbCrawler.getTopRatedMovies(1).thenAccept(movieResult -> {
            if (getActivity() == null || !isAdded()) return;
            
            // 保存电影第一页数据
            List<MediaInfo> firstPageMovies = movieResult.first;
            int totalMoviePages = movieResult.second;
            
            // 重新计算页数，API每页20条，我们每页21条
            int adjustedMoviePages = calculateAdjustedPages(totalMoviePages);
            
            // 缓存第一页数据
            if (firstPageMovies != null && !firstPageMovies.isEmpty()) {
                // 所有电影数据添加到总集合
                moviesList.addAll(firstPageMovies);
                
                // 缓存到临时集合以供分页使用
                moviesTempCache.addAll(firstPageMovies);
                
                // 如果不满21条，需要加载下一页补充
                if (firstPageMovies.size() < PAGE_SIZE) {
                    // 继续加载下一页电影数据
                    loadMoreMovieData(2);
                } else {
                    // 足够显示第一页，直接处理分页
                    processMoviePageData();
                }
            }
            
            // 2. 获取电视剧分类的总页数和第一页数据
            tmdbCrawler.getTopRatedTVShows(1).thenAccept(tvResult -> {
                if (getActivity() == null || !isAdded()) return;
                
                // 保存电视剧第一页数据
                List<MediaInfo> firstPageTvShows = tvResult.first;
                int totalTvPages = tvResult.second;
                
                // 重新计算页数，API每页20条，我们每页21条
                int adjustedTvPages = calculateAdjustedPages(totalTvPages);
                
                // 缓存第一页数据
                if (firstPageTvShows != null && !firstPageTvShows.isEmpty()) {
                    // 所有电视剧数据添加到总集合
                    tvList.addAll(firstPageTvShows);
                    
                    // 缓存到临时集合以供分页使用
                    tvTempCache.addAll(firstPageTvShows);
                    
                    // 如果不满21条，需要加载下一页补充
                    if (firstPageTvShows.size() < PAGE_SIZE) {
                        // 继续加载下一页电视剧数据
                        loadMoreTvData(2);
                    } else {
                        // 足够显示第一页，直接处理分页
                        processTvPageData();
                    }
                }
                
                // 3. 合并电影和电视剧数据，作为"全部"分类
                if (getActivity() == null || !isAdded()) return;
                
                // 合并数据
                mergeAllMediaData();
                
                // 更新UI (在主线程)
                getActivity().runOnUiThread(() -> {
                    // 更新分页信息 - 使用调整后的页数
                    updatePaginationInfo(
                            calculateAdjustedAllPages(), 
                            adjustedMoviePages, 
                            adjustedTvPages);
                    
                    // 显示当前类型的第一页数据
                    displayCurrentCategoryFirstPage();
                    
                    // 预加载其他分类第二页数据
                    preloadSecondPageData(adjustedMoviePages, adjustedTvPages);
                });
            }).exceptionally(e -> {
                handleLoadError(e);
                return null;
            });
        }).exceptionally(e -> {
            handleLoadError(e);
            return null;
        });
    }
    
    /**
     * 根据API返回的总页数计算调整后的页数
     * API每页返回20条，我们每页显示21条
     */
    private int calculateAdjustedPages(int apiTotalPages) {
        if (apiTotalPages <= 0) return 1;
        
        // 计算总条目数
        int totalItems = apiTotalPages * API_PAGE_SIZE;
        
        // 根据显示的每页条数计算页数
        return (int) Math.ceil((double) totalItems / PAGE_SIZE);
    }
    
    /**
     * 计算"全部"分类的总页数
     */
    private int calculateAdjustedAllPages() {
        // 取电影和电视剧总数据量的总和
        int totalItems = moviesList.size() + tvList.size();
        
        // 根据显示的每页条数计算页数
        return (int) Math.ceil((double) totalItems / PAGE_SIZE);
    }
    
    /**
     * 加载更多电影数据
     */
    private void loadMoreMovieData(int apiPage) {
        if (noMoreApiDataForMovies) {
            // 已经没有更多电影数据，直接处理现有数据
            processMoviePageData();
            return;
        }
        
        tmdbCrawler.getTopRatedMovies(apiPage).thenAccept(result -> {
            if (getActivity() == null || !isAdded()) return;
            
            List<MediaInfo> movies = result.first;
            
            if (movies == null || movies.isEmpty()) {
                // 没有更多数据
                noMoreApiDataForMovies = true;
            } else {
                // 添加到总数据集
                moviesList.addAll(movies);
                
                // 添加到临时缓存
                moviesTempCache.addAll(movies);
                
                // 如果临时缓存还不够一页
                if (moviesTempCache.size() < PAGE_SIZE && !noMoreApiDataForMovies) {
                    // 继续加载下一页
                    loadMoreMovieData(apiPage + 1);
                    return;
                }
            }
            
            // 处理电影分页数据
            processMoviePageData();
            
            // 更新"全部"分类数据
            mergeAllMediaData();
        }).exceptionally(e -> {
            if (isAdded()) {
                getActivity().runOnUiThread(() -> handleLoadError(e));
            }
            return null;
        });
    }
    
    /**
     * 加载更多电视剧数据
     */
    private void loadMoreTvData(int apiPage) {
        if (noMoreApiDataForTv) {
            // 已经没有更多电视剧数据，直接处理现有数据
            processTvPageData();
            return;
        }
        
        tmdbCrawler.getTopRatedTVShows(apiPage).thenAccept(result -> {
            if (getActivity() == null || !isAdded()) return;
            
            List<MediaInfo> tvShows = result.first;
            
            if (tvShows == null || tvShows.isEmpty()) {
                // 没有更多数据
                noMoreApiDataForTv = true;
        } else {
                // 添加到总数据集
                tvList.addAll(tvShows);
                
                // 添加到临时缓存
                tvTempCache.addAll(tvShows);
                
                // 如果临时缓存还不够一页
                if (tvTempCache.size() < PAGE_SIZE && !noMoreApiDataForTv) {
                    // 继续加载下一页
                    loadMoreTvData(apiPage + 1);
                    return;
                }
            }
            
            // 处理电视剧分页数据
            processTvPageData();
            
            // 更新"全部"分类数据
            mergeAllMediaData();
        }).exceptionally(e -> {
            if (isAdded()) {
                getActivity().runOnUiThread(() -> handleLoadError(e));
            }
            return null;
        });
    }
    
    /**
     * 处理电影分页数据
     */
    private void processMoviePageData() {
        // 清空已有缓存
        moviePageCache.clear();
        
        // 按每页PAGE_SIZE条分页
        int totalPages = (int) Math.ceil((double) moviesList.size() / PAGE_SIZE);
        
        for (int i = 0; i < totalPages; i++) {
            int fromIndex = i * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, moviesList.size());
            
            // 创建当前页数据
            List<MediaInfo> pageData = new ArrayList<>(moviesList.subList(fromIndex, toIndex));
            
            // 缓存当前页
            moviePageCache.put(i + 1, pageData);
        }
        
        // 清空临时缓存
        moviesTempCache.clear();
        
        // 如果当前类型是电影，更新UI
        if (currentType == TYPE_MOVIES && isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                // 更新分页信息
                paginationHelper.setTotalPages(totalPages);
                // 显示当前页数据
                if (moviePageCache.containsKey(1)) {
                    adapter.setCurrentType(TYPE_MOVIES);
                    adapter.setItems(moviePageCache.get(1));
                }
                // 更新UI
                paginationHelper.showLoading(false);
                paginationHelper.updatePaginationUI();
            });
        }
    }
    
    /**
     * 处理电视剧分页数据
     */
    private void processTvPageData() {
        // 清空已有缓存
        tvPageCache.clear();
        
        // 按每页PAGE_SIZE条分页
        int totalPages = (int) Math.ceil((double) tvList.size() / PAGE_SIZE);
        
        for (int i = 0; i < totalPages; i++) {
            int fromIndex = i * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, tvList.size());
            
            // 创建当前页数据
            List<MediaInfo> pageData = new ArrayList<>(tvList.subList(fromIndex, toIndex));
            
            // 缓存当前页
            tvPageCache.put(i + 1, pageData);
        }
        
        // 清空临时缓存
        tvTempCache.clear();
        
        // 如果当前类型是电视剧，更新UI
        if (currentType == TYPE_TV && isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // 更新分页信息
                paginationHelper.setTotalPages(totalPages);
                // 显示当前页数据
                if (tvPageCache.containsKey(1)) {
                    adapter.setCurrentType(TYPE_TV);
                    adapter.setItems(tvPageCache.get(1));
                }
                // 更新UI
                paginationHelper.showLoading(false);
                paginationHelper.updatePaginationUI();
            });
        }
    }
    
    /**
     * 合并电影和电视剧数据，作为"全部"分类
     */
    private void mergeAllMediaData() {
        // 清空已有数据
        allMediaList.clear();
        allPageCache.clear();
        
        // 合并电影和电视剧数据
        // 这里可以实现按评分排序，先简单合并
        allMediaList.addAll(moviesList);
        allMediaList.addAll(tvList);
        
        // 按每页PAGE_SIZE条分页
        int totalPages = (int) Math.ceil((double) allMediaList.size() / PAGE_SIZE);
        
        for (int i = 0; i < totalPages; i++) {
            int fromIndex = i * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, allMediaList.size());
            
            // 创建当前页数据
            List<MediaInfo> pageData = new ArrayList<>(allMediaList.subList(fromIndex, toIndex));
            
            // 缓存当前页
            allPageCache.put(i + 1, pageData);
        }
        
        // 如果当前类型是全部，更新UI
        if (currentType == TYPE_ALL && isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                // 更新分页信息
                paginationHelper.setTotalPages(totalPages);
                // 显示当前页数据
                if (allPageCache.containsKey(1)) {
                    adapter.setCurrentType(TYPE_ALL);
                    adapter.setItems(allPageCache.get(1));
                }
                // 更新UI
                paginationHelper.showLoading(false);
                paginationHelper.updatePaginationUI();
            });
        }
    }
    
    /**
     * 清空所有数据和缓存
     */
    private void clearAllData() {
        // 清空现有数据
        allMediaList.clear();
        moviesList.clear();
        tvList.clear();
        
        // 清空缓存
        moviePageCache.clear();
        tvPageCache.clear();
        allPageCache.clear();
        
        // 清空临时缓存
        moviesTempCache.clear();
        tvTempCache.clear();
        allTempCache.clear();
        
        // 重置分页状态
        paginationHelper.resetPaginationState();
        
        // 重置API数据标记
        noMoreApiDataForMovies = false;
        noMoreApiDataForTv = false;
        noMoreApiDataForAll = false;
    }
    
    /**
     * 更新分页信息
     */
    private void updatePaginationInfo(int totalAllPages, int totalMoviePages, int totalTvPages) {
        // 直接设置所有分类的总页数
        paginationHelper.setTotalPagesForAllCategories(totalAllPages, totalMoviePages, totalTvPages);
    }
    
    /**
     * 显示当前类型的第一页数据
     */
    private void displayCurrentCategoryFirstPage() {
        // 隐藏加载动画
        paginationHelper.showLoading(false);
        
        List<MediaInfo> firstPageData = null;
        
        // 根据当前选择的类型显示对应数据
        if (currentType == TYPE_ALL) {
            firstPageData = allPageCache.get(1);
        } else if (currentType == TYPE_MOVIES) {
            firstPageData = moviePageCache.get(1);
        } else if (currentType == TYPE_TV) {
            firstPageData = tvPageCache.get(1);
        }
        
        if (firstPageData != null && !firstPageData.isEmpty()) {
            // 设置当前页为1
            paginationHelper.setCurrentPage(1);
            
            // 更新适配器内容
            adapter.setCurrentType(currentType);
            adapter.setItems(firstPageData);
        } else {
            // 显示空状态
            paginationHelper.showEmptyState();
        }
        
        // 确保分页UI更新
        paginationHelper.updatePaginationUI();
    }
    
    /**
     * 预加载第二页数据
     */
    private void preloadSecondPageData(int totalMoviePages, int totalTvPages) {
        // 电影和电视剧数据已经在初始化阶段提前加载，不需要再加载
        // 此方法保留以兼容现有代码结构
    }
    
    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 筛选Chip组监听
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            // 先拦截掉其他所有操作，显示加载动画
            paginationHelper.showInitialLoadingAnimation();
            
            // 确保页面滚动到顶部
            paginationHelper.scrollToTop();
            
            // 使用Handler延迟执行，确保先渲染加载动画
            handler.postDelayed(() -> {
                if (checkedId == R.id.chip_all) {
                    currentType = TYPE_ALL;
                    paginationHelper.switchCategory(TYPE_ALL);
                    // 显示已缓存的第1页数据
                    displayCachedPage(TYPE_ALL, 1);
                } else if (checkedId == R.id.chip_movies) {
                    currentType = TYPE_MOVIES;
                    paginationHelper.switchCategory(TYPE_MOVIES);
                    // 显示已缓存的第1页数据
                    displayCachedPage(TYPE_MOVIES, 1);
                } else if (checkedId == R.id.chip_tv) {
                    currentType = TYPE_TV;
                    paginationHelper.switchCategory(TYPE_TV);
                    // 显示已缓存的第1页数据
                    displayCachedPage(TYPE_TV, 1);
                }
            }, 200);
        });
    }
    
    /**
     * 显示已缓存的页面，如果不存在则加载
     */
    private void displayCachedPage(int contentType, int page) {
        Map<Integer, List<MediaInfo>> currentCache;
        
        if (contentType == TYPE_MOVIES) {
            currentCache = moviePageCache;
        } else if (contentType == TYPE_TV) {
            currentCache = tvPageCache;
            } else {
            currentCache = allPageCache;
        }
        
        if (currentCache.containsKey(page)) {
            // 直接显示缓存的数据
            List<MediaInfo> cachedData = currentCache.get(page);
            adapter.setCurrentType(contentType);
            adapter.setItems(cachedData);
            
            // 更新分页UI
            paginationHelper.setCurrentPage(page);
            paginationHelper.showLoading(false);
            paginationHelper.updatePaginationUI();
            } else {
            // 如果没有缓存，加载对应页面
            loadMediaData(contentType, page);
        }
    }
    
    /**
     * PaginationHelper.OnPageLoadListener的实现
     * 当用户点击分页控件时触发
     */
    @Override
    public void onLoadPage(int category, int page) {
        // 优先尝试从缓存加载
        displayCachedPage(category, page);
    }
    
    /**
     * PaginationHelper.CategoryProvider的实现
     * 提供当前分类给分页助手
     */
    @Override
    public int getCurrentCategory() {
        return currentType;
    }
    
    /**
     * PaginationHelper.EmptyStateChecker的实现
     * 检查当前分类是否为空
     */
    @Override
    public boolean isCurrentCategoryEmpty() {
        boolean isEmpty = false;
        switch (currentType) {
            case TYPE_ALL:
                isEmpty = allMediaList.isEmpty();
                break;
            case TYPE_MOVIES:
                isEmpty = moviesList.isEmpty();
                break;
            case TYPE_TV:
                isEmpty = tvList.isEmpty();
                break;
        }
        
        return isEmpty;
    }
    
    /**
     * 加载指定类型和页码的数据
     * @param contentType 内容类型（全部/电影/电视剧）
     * @param page 页码
     */
    private void loadMediaData(int contentType, int page) {
        // 确认page是有效的
        if (page < 1) {
            return;
        }
        
        // 设置加载状态
        paginationHelper.showLoading(true);
        
        // 从缓存中获取数据
        Map<Integer, List<MediaInfo>> currentCache;
        
        if (contentType == TYPE_MOVIES) {
            currentCache = moviePageCache;
        } else if (contentType == TYPE_TV) {
            currentCache = tvPageCache;
        } else {
            currentCache = allPageCache;
        }
        
        if (currentCache.containsKey(page)) {
            // 从缓存加载
            List<MediaInfo> cachedData = currentCache.get(page);
            if (cachedData != null && !cachedData.isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // 更新UI
                        adapter.setCurrentType(contentType);
                        adapter.setItems(cachedData);
                        paginationHelper.setCurrentPage(page);
                        paginationHelper.showLoading(false);
                        paginationHelper.updatePaginationUI();
                    });
                }
            } else {
                paginationHelper.showEmptyState();
            }
        } else {
            // 已经没有更多数据
            if (contentType == TYPE_MOVIES && page > moviePageCache.size()) {
                paginationHelper.setLastPage(true);
                paginationHelper.showLoading(false);
                Toast.makeText(getContext(), "已经是最后一页", Toast.LENGTH_SHORT).show();
            } else if (contentType == TYPE_TV && page > tvPageCache.size()) {
                paginationHelper.setLastPage(true);
                paginationHelper.showLoading(false);
                Toast.makeText(getContext(), "已经是最后一页", Toast.LENGTH_SHORT).show();
            } else if (contentType == TYPE_ALL && page > allPageCache.size()) {
                paginationHelper.setLastPage(true);
                paginationHelper.showLoading(false);
                Toast.makeText(getContext(), "已经是最后一页", Toast.LENGTH_SHORT).show();
            }
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
        // 隐藏加载提示
        paginationHelper.showLoading(false);
        
        Toast.makeText(getContext(), R.string.load_failed, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "加载高分内容失败", throwable);
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
            paginationHelper.updatePaginationUI();
            
            // 确保分页导航显示
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.VISIBLE);
            }
        }
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
} 