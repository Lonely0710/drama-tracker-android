package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.util.Log;
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
import androidx.recyclerview.widget.RecyclerView;

import com.lonely.dramatracker.R;
import com.lonely.dramatracker.adapters.RecentAdapter;
import com.lonely.dramatracker.models.MovieInfo;
import com.lonely.dramatracker.models.WebSite;
import com.lonely.dramatracker.utils.MaoYanCrawler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 近期上映标签页的Fragment
 */
public class RecentTabFragment extends Fragment {
    private static final String TAG = "RecentTabFragment";

    // 视图组件
    private RecyclerView rvTabContent;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    
    // 适配器
    private RecentAdapter adapter;
    
    // 猫眼爬虫
    private MaoYanCrawler maoYanCrawler;

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
        initAdapter();
        
        // 初始化猫眼爬虫
        maoYanCrawler = MaoYanCrawler.getInstance();
        
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
        
        // 设置文本内容
        tvEmpty.setText(R.string.empty_recommend);
    }
    
    /**
     * 初始化适配器
     */
    private void initAdapter() {
        adapter = new RecentAdapter(getContext());
        rvTabContent.setAdapter(adapter);
        
        // 设置点击监听器
        adapter.setOnItemClickListener((position, item) -> {
            // 打开猫眼电影详情页
            openMaoyanMovieDetail(item);
        });
    }
    
    /**
     * 加载近期上映电影数据
     */
    private void loadData() {
        // 显示加载状态
        showLoading(true);
        
        // 使用猫眼爬虫获取近期上映电影
        CompletableFuture<List<MovieInfo>> future = maoYanCrawler.getCurrentMovies();
        future.thenAccept(movies -> {
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                // 更新UI
                if (movies != null && !movies.isEmpty()) {
                    adapter.setItems(movies);
                    showLoading(false);
                } else {
                    showEmpty();
                }
                Log.d(TAG, "加载了 " + (movies != null ? movies.size() : 0) + " 部近期上映电影");
            });
        }).exceptionally(throwable -> {
            if (getActivity() == null) return null;
            
            getActivity().runOnUiThread(() -> {
                // 显示错误状态
                showEmpty();
                Toast.makeText(getContext(), R.string.load_failed, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "加载近期上映电影失败", throwable);
            });
            return null;
        });
    }
    
    /**
     * 打开猫眼电影详情页
     */
    private void openMaoyanMovieDetail(MovieInfo movie) {
        if (movie == null || getActivity() == null) return;
        
        try {
            // 获取猫眼电影ID
            String movieId = String.valueOf(movie.getId());
            if (movieId == null || movieId.isEmpty() || "0".equals(movieId)) {
                Toast.makeText(getContext(), "无法获取电影ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 构建猫眼电影详情页URL
            String url = "https://m.maoyan.com/movie/" + movieId;
            
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
                args.putString("site_name", "MAOYAN");
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
            Log.e(TAG, "打开猫眼电影详情页失败", e);
            Toast.makeText(getContext(), "打开详情页失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查是否显示空状态
     */
    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTabContent.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTabContent.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 显示或隐藏加载状态
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            rvTabContent.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            if (adapter.getItemCount() == 0) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvTabContent.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvTabContent.setVisibility(View.VISIBLE);
            }
        }
    }
} 