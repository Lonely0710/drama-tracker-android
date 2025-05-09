package com.lonely.dramatracker.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lonely.dramatracker.R;
import com.lonely.dramatracker.adapters.WeeklyScheduleAdapter;
import com.lonely.dramatracker.models.DailyAnime;
import com.lonely.dramatracker.models.WeeklySchedule;
import com.lonely.dramatracker.utils.BangumiCrawler;

import java.util.concurrent.CompletableFuture;

/**
 * 每日放送标签页的Fragment
 */
public class DailyTabFragment extends Fragment {
    private static final String TAG = "DailyTabFragment";

    // 视图组件
    private RecyclerView rvTabContent;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    
    // 适配器和爬虫
    private WeeklyScheduleAdapter adapter;
    private BangumiCrawler bangumiCrawler;

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
        
        // 初始化适配器
        setupRecyclerView();
        
        // 初始化爬虫
        bangumiCrawler = new BangumiCrawler();
        
        // 加载数据
        loadData();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        rvTabContent = view.findViewById(R.id.rv_tab_content);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        
        // 设置空视图文本
        tvEmpty.setText(R.string.daily_empty);
    }
    
    /**
     * 设置RecyclerView和适配器
     */
    private void setupRecyclerView() {
        adapter = new WeeklyScheduleAdapter(getContext());
        adapter.setAnimeClickListener(anime -> {
            // 点击动漫项时打开WebView
            openBangumiWebView(anime);
        });
        
        rvTabContent.setAdapter(adapter);
        rvTabContent.setLayoutManager(new LinearLayoutManager(getContext()));
    }
    
    /**
     * 打开Bangumi WebView
     */
    private void openBangumiWebView(DailyAnime anime) {
        if (getActivity() == null) return;
        
        try {
            // 构建Bangumi详情页URL
            String url = anime.getSourceUrl();
            if (url == null || url.isEmpty()) {
                url = "https://chii.in/subject/" + anime.getSourceId();
            }
            
            // 创建WebViewFragment
            WebViewFragment webViewFragment = new WebViewFragment();
            Bundle args = new Bundle();
            args.putString("site_name", "BANGUMI");
            args.putString("custom_url", url);
            webViewFragment.setArguments(args);
            
            // 使用主Activity中的fragment_container
            // 获取MainActivity中的fragment_container
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            fragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fragment_slide_enter_right,
                    R.anim.fragment_slide_exit_left,
                    R.anim.fragment_slide_enter_left,
                    R.anim.fragment_slide_exit_right
                )
                .add(R.id.fragment_container, webViewFragment)
                .addToBackStack(null)
                .commit();
                
        } catch (Exception e) {
            Toast.makeText(getContext(), getString(R.string.load_failed) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 加载每日放送数据
     */
    private void loadData() {
        // 显示加载中
        showLoading();
        
        // 使用BangumiCrawler获取每日放送数据
        CompletableFuture<WeeklySchedule> future = bangumiCrawler.getWeeklySchedule();
        future.thenAccept(weeklySchedule -> {
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                boolean hasData = hasAnyAnime(weeklySchedule);
                if (hasData) {
                    adapter.updateData(weeklySchedule);
                    showContent();
                } else {
                    showEmpty();
                }
            });
        }).exceptionally(e -> {
            if (getActivity() == null) return null;
            
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), getString(R.string.load_failed), Toast.LENGTH_SHORT).show();
                showEmpty();
            });
            return null;
        });
    }
    
    /**
     * 检查是否有任何动漫数据
     */
    private boolean hasAnyAnime(WeeklySchedule schedule) {
        return (schedule.getSundayAnime() != null && !schedule.getSundayAnime().isEmpty())
            || (schedule.getMondayAnime() != null && !schedule.getMondayAnime().isEmpty())
            || (schedule.getTuesdayAnime() != null && !schedule.getTuesdayAnime().isEmpty())
            || (schedule.getWednesdayAnime() != null && !schedule.getWednesdayAnime().isEmpty())
            || (schedule.getThursdayAnime() != null && !schedule.getThursdayAnime().isEmpty())
            || (schedule.getFridayAnime() != null && !schedule.getFridayAnime().isEmpty())
            || (schedule.getSaturdayAnime() != null && !schedule.getSaturdayAnime().isEmpty());
    }
    
    /**
     * 显示加载中状态
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvTabContent.setVisibility(View.GONE);
    }
    
    /**
     * 显示内容
     */
    private void showContent() {
        progressBar.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        rvTabContent.setVisibility(View.VISIBLE);
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