package com.lonely.dramatracker.fragments;

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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.List;

public class SearchFragment extends BaseFragment {
    private static final String TAG = "SearchFragment";
    
    private RecyclerView rvSearchResults;
    private FrameLayout loadingView;
    private LinearLayout emptyView;
    private TextView tvSearchTitle;
    private ImageButton btnClose;
    private EditText etSearch;
    private ImageButton btnClear;
    
    private SearchResultAdapter adapter;
    private SearchService searchService = new SearchServiceImpl(new ApiServiceImpl());
    private OnSearchResultClickListener resultClickListener;
    private OnCloseListener closeListener;
    private String searchType;
    private boolean isInputMode = true; // 标记当前是输入模式还是提交模式
    
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
    }
    
    private void setupRecyclerView() {
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
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
    }
    
    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            return;
        }
        
        showLoading();
        
        // 执行搜索后,切换为清除模式
        switchToClearMode();
        
        searchService.search(keyword, searchType, (JsonSearchCallback) jsonResults -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    try {
                        // 将JSON字符串转换为List<SearchResult>对象
                        List<SearchResult> results = parseJsonResults(jsonResults);
                        
                        if (results == null || results.isEmpty()) {
                            showEmpty();
                        } else {
                            showResults(results);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析搜索结果失败", e);
                        showEmpty();
                    }
                });
            }
        });
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
    
    public void showResults(List<SearchResult> results) {
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
        if (rvSearchResults != null) {
            rvSearchResults.setVisibility(View.VISIBLE);
        }
        if (adapter != null) {
            adapter.setResults(results);
        }
    }
} 