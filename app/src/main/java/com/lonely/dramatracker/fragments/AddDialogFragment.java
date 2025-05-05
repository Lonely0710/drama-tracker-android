package com.lonely.dramatracker.fragments;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.services.QuickSearchService;

import java.util.Map;

/**
 * 添加作品的搜索对话框
 */
public class AddDialogFragment extends DialogFragment {
    
    private QuickSearchService searchService;
    
    private TextInputLayout tilSearch;
    private TextInputEditText etSearch;
    private Button btnCancel;
    private Button btnSearch;
    private FrameLayout loadingContainer;
    private ProgressBar progressBar;
    private TextView tvLoading;
    
    public static AddDialogFragment newInstance() {
        return new AddDialogFragment();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Dialog_FullWidth);
        if (searchService == null) {
            searchService = new QuickSearchService();
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_dialog, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
            
            // 设置背景透明
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            
            // 设置状态栏透明
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getDialog().getWindow().setStatusBarColor(Color.TRANSPARENT);
            
            // 根据主题设置状态栏图标颜色
            android.util.TypedValue typedValue = new android.util.TypedValue();
            boolean isLightTheme = requireContext().getTheme()
                    .resolveAttribute(android.R.attr.isLightTheme, typedValue, true)
                    && typedValue.data != 0;
            
            // 设置全屏显示，隐藏状态栏和导航栏
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            
            if (isLightTheme) {
                // 浅色主题，设置深色状态栏图标（仅当状态栏可见时才会显示）
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            
            getDialog().getWindow().getDecorView().setSystemUiVisibility(flags);
            
            // 设置动画
            getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }
    
    private void initViews(View view) {
        tilSearch = view.findViewById(R.id.til_search);
        etSearch = view.findViewById(R.id.et_search);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSearch = view.findViewById(R.id.btn_search);
        loadingContainer = view.findViewById(R.id.loading_container);
        progressBar = view.findViewById(R.id.progress_bar);
        tvLoading = view.findViewById(R.id.tv_loading);
    }
    
    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnSearch.setOnClickListener(v -> performSearch());
        
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }
    
    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            tilSearch.setError("请输入搜索内容");
            return;
        }
        
        // 清除错误并显示加载中状态
        tilSearch.setError(null);
        showLoading(true);
        
        // 执行搜索
        searchService.searchFromMultipleSources(keyword).thenAcceptAsync(results -> {
            if (getActivity() == null || !isAdded()) return;
            
            getActivity().runOnUiThread(() -> {
                showLoading(false);
                showSearchResults(keyword, results);
            });
        }).exceptionally(e -> {
            if (getActivity() == null || !isAdded()) return null;
            
            getActivity().runOnUiThread(() -> {
                showLoading(false);
                tilSearch.setError("搜索失败，请重试");
            });
            return null;
        });
    }
    
    private void showLoading(boolean isLoading) {
        btnSearch.setEnabled(!isLoading);
        loadingContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
    
    private void showSearchResults(String keyword, Map<String, SearchResult> results) {
        if (results.isEmpty()) {
            tilSearch.setError("未找到相关结果");
            return;
        }
        
        // 显示搜索结果界面
        FragmentManager fragmentManager = getParentFragmentManager();
        AddResultFragment resultFragment = AddResultFragment.newInstance(keyword, results);
        
        // 使用Fragment事务替代简单的show方法，以支持动画
        fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_slide_enter_right,
                R.anim.fragment_slide_exit_left,
                R.anim.fragment_slide_enter_left,
                R.anim.fragment_slide_exit_right
            )
            .replace(R.id.fragment_container, resultFragment)
            .addToBackStack(null)
            .commit();
        
        // 关闭当前对话框
        dismiss();
    }
} 