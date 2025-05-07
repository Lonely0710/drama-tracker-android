package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.lonely.dramatracker.R;

/**
 * 每日放送标签页的Fragment
 */
public class DailyTabFragment extends Fragment {
    private static final String TAG = "DailyTabFragment";

    // 视图组件
    private RecyclerView rvTabContent;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommend_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化视图
        initViews(view);
        
        // 显示暂无内容（当前每日放送功能尚未实现）
        showEmpty();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        rvTabContent = view.findViewById(R.id.rv_tab_content);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        
        // 设置文本内容
        tvEmpty.setText("每日放送功能即将上线");
    }
    
    /**
     * 显示空状态
     */
    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        rvTabContent.setVisibility(View.GONE);
    }
} 