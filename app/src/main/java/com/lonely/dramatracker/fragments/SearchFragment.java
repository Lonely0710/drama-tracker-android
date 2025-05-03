package com.lonely.dramatracker.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.adapters.SearchResultAdapter;
import com.lonely.dramatracker.api.impl.ApiServiceImpl;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.services.SearchService;
import com.lonely.dramatracker.services.SearchService.JsonSearchCallback;
import com.lonely.dramatracker.services.impl.SearchServiceImpl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFragment extends BaseFragment {
    private static final String TAG = "SearchFragment";
    private static final int PAGE_LIMIT = 5; // 定义每页加载的数量
    private static final int MAX_PAGE_BUTTONS = 5; // 最多显示的页码按钮数量
    
    private RecyclerView rvSearchResults;
    private FrameLayout loadingView;
    private LinearLayout emptyView;
    private TextView tvSearchTitle;
    private ImageButton btnClose;
    private EditText etSearch;
    private ImageButton btnClear;
    
    // 分页导航相关视图
    private View paginationLayout;
    private LinearLayout pageNumberContainer;
    private ImageButton btnPrevPage;
    private ImageButton btnNextPage;
    private TextView tvTotalPages;
    private ProgressBar paginationLoading;
    
    private SearchResultAdapter adapter;
    private SearchService searchService = new SearchServiceImpl(new ApiServiceImpl());
    private OnSearchResultClickListener resultClickListener;
    private OnCloseListener closeListener;
    private String searchType;
    private boolean isInputMode = true; // 标记当前是输入模式还是提交模式
    
    // --- 分页相关状态变量 ---
    private int currentPage = 1; // 当前页码，从1开始
    private int totalPages = 1; // 总页数
    private boolean isLoading = false; // 是否正在加载数据
    private boolean isLastPage = false; // 是否已加载到最后一页
    private LinearLayoutManager layoutManager; // RecyclerView的布局管理器，用于判断滚动位置
    // --- 分页相关状态变量结束 ---

    // 在 SearchFragment 类中添加一个新变量来记录总结果数
    private int totalResults = 0; // 搜索结果总数

    // 添加缓存相关变量
    private Map<Integer, List<SearchResult>> pageCache = new HashMap<>(); // 页面缓存
    private int totalItems = 0; // 搜索结果总数

    // 添加一个标志来标记是否是从搜索或翻页操作触发的数据加载
    private boolean isPageOperation = false;

    public interface OnSearchResultClickListener {
        void onSearchResultClick(SearchResult result);
    }
    
    public interface OnCloseListener {
        void onClose();
    }
    
    public void setOnSearchResultClickListener(OnSearchResultClickListener listener) {
        this.resultClickListener = listener;
    }
    
    public void setOnCloseListener(OnCloseListener listener) {
        this.closeListener = listener;
    }
    
    public void setSearchType(String type) {
        this.searchType = type;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_search;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new SearchResultAdapter();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        
        // 自动显示软键盘
        etSearch.requestFocus();
    }
    
    private void initViews(View view) {
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        loadingView = view.findViewById(R.id.loading_view);
        emptyView = view.findViewById(R.id.empty_view);
        tvSearchTitle = view.findViewById(R.id.tv_search_title);
        btnClose = view.findViewById(R.id.btn_close);
        etSearch = view.findViewById(R.id.et_search);
        btnClear = view.findViewById(R.id.btn_clear);
        
        // 初始化分页导航相关视图
        paginationLayout = view.findViewById(R.id.pagination_layout);
        if (paginationLayout != null) {
            pageNumberContainer = paginationLayout.findViewById(R.id.page_number_container);
            btnPrevPage = paginationLayout.findViewById(R.id.btn_prev_page);
            btnNextPage = paginationLayout.findViewById(R.id.btn_next_page);
            tvTotalPages = paginationLayout.findViewById(R.id.tv_total_pages);
            paginationLoading = paginationLayout.findViewById(R.id.pagination_loading);
            
            // 设置上一页/下一页按钮点击事件
            setupPaginationButtons();
        }
    }
    
    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(requireContext()); // 初始化布局管理器
        rvSearchResults.setLayoutManager(layoutManager);
        rvSearchResults.setAdapter(adapter);
        
        adapter.setOnItemClickListener(result -> {
            if (resultClickListener != null) {
                resultClickListener.onSearchResultClick(result);
            }
        });
        
        adapter.setOnCollectClickListener((result, isCollect) -> {
            if (isCollect) {
                searchService.addToCollection(result, new Runnable() {
                    @Override
                    public void run() {
                        requireActivity().runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            // 收藏成功提示
                            Toast.makeText(requireContext(), "收藏成功", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                searchService.removeFromCollection(result, new Runnable() {
                    @Override
                    public void run() {
                        requireActivity().runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            // 取消收藏提示
                            Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
    }
    
    private void setupListeners() {
        btnClose.setOnClickListener(v -> {
            if (closeListener != null) {
                closeListener.onClose();
            }
        });
        
        // 搜索框文本变化监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                updateSearchButtonState(s.toString());
            }
        });
        
        // 搜索框动作监听
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
        
        // 清除/提交按钮点击监听
        btnClear.setOnClickListener(v -> {
            if (isInputMode) {
                // 清除模式
                etSearch.setText("");
                etSearch.requestFocus();
            } else {
                // 提交模式
                performSearch();
            }
        });

        // 修改 RecyclerView 滚动监听
        rvSearchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // 如果是主动翻页操作，不触发自动加载
                if (isPageOperation) {
                    isPageOperation = false;
                    return;
                }

                // 如果用户正在向下滚动
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // 判断是否需要加载更多：不在加载中、不是最后一页、滚动到接近底部
                    if (!isLoading && !isLastPage) {
                        // 条件：(可见数 + 第一个可见项位置 >= 总数) && (第一个可见项位置 >= 0) && (总数 >= 每页限制数 - 避免不满一页时也触发)
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= PAGE_LIMIT) { 
                            loadMoreItems(); // 调用加载更多
                        }
                    }
                }
            }
        });
        // --- 滚动监听结束 ---
    }
    
    // 执行新的搜索
    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            return;
        }
        
        // 重置分页和缓存状态
        currentPage = 1;
        isLastPage = false;
        adapter.submitList(null);
        pageCache.clear(); // 清空缓存
        showLoading();
        
        // 切换为清除模式
        switchToClearMode();
        
        // 标记这是一次主动操作，防止自动加载更多页
        isPageOperation = true;
        
        // 先获取总项目数
        fetchTotalItems(keyword);
    }
    
    // 新增获取总项目数的方法
    private void fetchTotalItems(String keyword) {
        isLoading = true;
        searchService.getTotalItems(keyword, searchType, count -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    totalItems = count;
                    // 计算总页数
                    totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PAGE_LIMIT));
                    // 更新UI
                    if (totalItems > 0) {
                        // 有结果，加载第一页 - 始终重置为第一页
                        currentPage = 1;
                        loadSearchResults(keyword, currentPage);
                    } else {
                        // 无结果
                        isLoading = false;
                        showEmpty();
                        paginationLayout.setVisibility(View.GONE);
                    }
                });
            }
        });
    }
    
    // 加载更多数据（下一页）
    private void loadMoreItems() {
        currentPage++; // 页码+1
        isLoading = true; // 标记为正在加载
        String keyword = etSearch.getText().toString().trim(); // 获取当前搜索关键词
        loadSearchResults(keyword, currentPage); // 加载下一页
        // 可以在这里让Adapter显示一个Footer View作为加载提示
    }
    
    // 根据关键词和页码加载搜索结果
    private void loadSearchResults(String keyword, int page) {
        // 确认page是有效的
        if (page < 1 || (totalPages > 0 && page > totalPages)) {
            isLoading = false;
            Toast.makeText(requireContext(), "无效的页码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isLoading = true;
        
        // 记录当前正在加载的页码
        currentPage = page;
        
        // 标记这是一次主动操作，防止自动加载更多页
        isPageOperation = true;
        
        // 首先检查缓存中是否已有该页数据
        if (pageCache.containsKey(page)) {
            List<SearchResult> cachedResults = pageCache.get(page);
            handleCachedResults(cachedResults, page);
            return;
        }
        
        if (page == 1) {
            showLoading();
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.GONE);
            }
        } else {
            if (paginationLoading != null) {
                paginationLoading.setVisibility(View.VISIBLE);
            }
        }
        
        searchService.search(keyword, searchType, page, PAGE_LIMIT, (JsonSearchCallback) jsonResults -> {
            if (isAdded()) {
                new Thread(() -> {
                    try {
                        List<SearchResult> results = parseJsonResults(jsonResults);
                        // 将结果保存到缓存
                        if (results != null && !results.isEmpty()) {
                            pageCache.put(page, results);
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            if (paginationLoading != null) {
                                paginationLoading.setVisibility(View.GONE);
                            }
                            handleSearchResults(results, page);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "解析搜索结果失败或处理时出错", e);
                        requireActivity().runOnUiThread(() -> {
                            if (paginationLoading != null) {
                                paginationLoading.setVisibility(View.GONE);
                            }
                            if (page == 1) {
                                showEmpty();
                            } else {
                                Toast.makeText(requireContext(), "加载更多数据失败", Toast.LENGTH_SHORT).show();
                            }
                            isLoading = false;
                        });
                    }
                }).start();
            } else {
                isLoading = false;
            }
        });
    }

    // 新增处理缓存结果的方法
    private void handleCachedResults(List<SearchResult> results, int page) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                if (paginationLoading != null) {
                    paginationLoading.setVisibility(View.GONE);
                }
                
                // 设置当前页码，确保页码与内容一致
                currentPage = page;
                
                // 显示数据
                adapter.submitList(results);
                showResultsContainer();
                
                // 更新分页UI
                updatePaginationUI();
                
                isLoading = false;
            });
        }
    }

    // 修改处理搜索结果方法
    private void handleSearchResults(List<SearchResult> results, int page) {
        isLoading = false;

        if (results == null) {
            if (page == 1) {
                showEmpty();
                paginationLayout.setVisibility(View.GONE);
            }
            Toast.makeText(requireContext(), "加载失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        if (results.isEmpty()) {
            if (page == 1) {
                // 第一页没有结果，显示空状态
                adapter.submitList(null);
                showEmpty();
                isLastPage = true;
                paginationLayout.setVisibility(View.GONE);
            } else {
                // 后续页为空，说明到了末尾
                isLastPage = true;
                // 最后一页为空的情况，更新总页数
                if (page <= totalPages) {
                    totalPages = page - 1;
                    updatePaginationUI();
                }
            }
            return;
        }

        // 有结果的情况，直接显示
        adapter.submitList(results);
        showResultsContainer();
        
        // 检查是否到达最后一页
        if (results.size() < PAGE_LIMIT) {
            isLastPage = true;
            // 更新实际总页数（如果当前估计不准确）
            if (page != totalPages) {
                totalPages = page;
            }
        }
        
        // 更新分页UI
        updatePaginationUI();
    }
    
    // 更新分页导航UI
    private void updatePaginationUI() {
        if (paginationLayout == null || pageNumberContainer == null) {
            return;
        }
        
        // 显示分页导航栏
        paginationLayout.setVisibility(View.VISIBLE);
        
        // 设置总页数提示
        tvTotalPages.setText(String.format("共 %d 页 (%d项)", totalPages, totalItems));
        
        // 上一页按钮状态
        btnPrevPage.setEnabled(currentPage > 1);
        btnPrevPage.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
        
        // 下一页按钮状态 - 除非确认是最后一页，否则始终可点击
        btnNextPage.setEnabled(!isLastPage || currentPage < totalPages);
        btnNextPage.setAlpha(!isLastPage || currentPage < totalPages ? 1.0f : 0.5f);
        
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
        
        // 显示第一页
        if (startPage > 1) {
            addPageButton("1", 1);
        }
        
        // 显示省略号
        if (startPage > 2) {
            addPageButton("...", -1);
        }
        
        // 显示中间的页码
        for (int i = startPage; i <= endPage; i++) {
            if (i != 1 && i != totalPages) { // 避免重复显示第一页和最后一页
                addPageButton(String.valueOf(i), i);
            }
        }
        
        // 显示省略号
        if (endPage < totalPages - 1) {
            addPageButton("...", -1);
        }
        
        // 显示最后一页
        if (endPage < totalPages) {
            addPageButton(String.valueOf(totalPages), totalPages);
        }
    }
    
    // 添加页码按钮
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
                    // 直接加载对应页码内容，让loadSearchResults方法内部设置currentPage
                    loadSearchResults(etSearch.getText().toString().trim(), pageNum);
                }
            });
        } else {
            pageButton.setEnabled(false);
        }
        
        // 添加到容器
        pageNumberContainer.addView(pageButton);
    }
    
    // 切换到清除模式
    private void switchToClearMode() {
        if (!isInputMode) {
            btnClear.setImageResource(R.drawable.ic_clear);
            isInputMode = true;
        }
    }
    
    // 更新搜索按钮状态
    private void updateSearchButtonState(String text) {
        if (text.trim().isEmpty()) {
            // 输入为空，隐藏按钮
            btnClear.setVisibility(View.GONE);
            isInputMode = true;
        } else {
            // 输入不为空
            btnClear.setVisibility(View.VISIBLE);
            
            // 根据当前模式设置不同图标
            if (isInputMode) {
                // 从输入模式切换到提交模式
                btnClear.setImageResource(R.drawable.ic_commit);
                isInputMode = false;
            }
        }
    }
    
    // 解析JSON格式的搜索结果
    private List<SearchResult> parseJsonResults(String jsonResults) {
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<SearchResult>>(){}.getType();
            return gson.fromJson(jsonResults, listType);
        } catch (Exception e) {
            Log.e(TAG, "JSON解析失败: " + e.getMessage());
            return null;
        }
    }
    
    public void setTitle(String title) {
        if (tvSearchTitle != null) {
            tvSearchTitle.setText(title);
        }
    }
    
    public void showLoading() {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
        if (rvSearchResults != null) {
            rvSearchResults.setVisibility(View.GONE);
        }
    }
    
    public void showEmpty() {
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        if (rvSearchResults != null) {
            rvSearchResults.setVisibility(View.GONE);
        }
    }
    
    // 改名为 showResultsContainer，因为旧的 showResults 包含了 adapter.setResults 的逻辑
    public void showResultsContainer() {
         if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
         }
         if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
         }
         if (rvSearchResults != null) {
            rvSearchResults.setVisibility(View.VISIBLE);
         }
         // Adapter update is handled in handleSearchResults
    }

    // 设置分页按钮的点击事件
    private void setupPaginationButtons() {
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1 && !isLoading) {
                // 直接设置新页码并加载内容
                loadSearchResults(etSearch.getText().toString().trim(), currentPage - 1);
            }
        });
        
        btnNextPage.setOnClickListener(v -> {
            if (!isLoading) {
                // 检查是否有下一页
                if (isLastPage) {
                    // 已经是最后一页
                    Toast.makeText(requireContext(), "已经是最后一页", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (currentPage < totalPages) {
                    // 直接设置新页码并加载内容
                    loadSearchResults(etSearch.getText().toString().trim(), currentPage + 1);
                } else if (!isLastPage) {
                    // 总页数可能不准确，但尚未到达最后一页，尝试加载下一页
                    loadSearchResults(etSearch.getText().toString().trim(), currentPage + 1);
                }
            }
        });
    }
} 