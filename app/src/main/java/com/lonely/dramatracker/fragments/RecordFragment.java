package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.tabs.TabLayout;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.adapters.RecordAdapter;
import com.lonely.dramatracker.models.RecordItem;
import com.lonely.dramatracker.services.AppwriteWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.Log;

public class RecordFragment extends BaseFragment {
    private static final String TAG = "RecordFragment";
    private RecyclerView rvRecords;
    private LottieAnimationView lottieLoading;
    private TextView tvEmpty;
    private TabLayout tabLayout;
    private FloatingActionButton fabSwitchMode;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecordAdapter adapter;
    private List<RecordItem> allRecords = new ArrayList<>();
    private List<RecordItem> filteredRecords = new ArrayList<>();
    private boolean isGridMode = true;
    private boolean isFirstLoad = true;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_record;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setTitle(getString(R.string.title_record));
        showLogo();
        showNotification(true);
        
        initViews(view);
        setupTabLayout();
        loadRecords();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (!isFirstLoad) {
            refreshRecords();
        } else {
            isFirstLoad = false;
        }
    }
    
    private void initViews(View view) {
        rvRecords = view.findViewById(R.id.rv_records);
        lottieLoading = view.findViewById(R.id.lottie_loading);
        tvEmpty = view.findViewById(R.id.tv_empty);
        tabLayout = view.findViewById(R.id.tab_layout);
        fabSwitchMode = view.findViewById(R.id.fab_switch_mode);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // 设置RecyclerView
        rvRecords.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new RecordAdapter(isGridMode);
        rvRecords.setAdapter(adapter);

        // 设置下拉刷新
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(this::refreshRecords);

        // 修正悬浮按钮点击逻辑，确保图标与模式对应
        fabSwitchMode.setOnClickListener(v -> {
            boolean newGridMode = !isGridMode;
            setViewMode(newGridMode);
            // 根据当前模式设置图标（网格模式时显示列表图标，列表模式时显示网格图标）
            updateSwitchModeIcon();
            Log.d(TAG, "切换布局模式: isGridMode=" + isGridMode);
        });
    }

    private void updateSwitchModeIcon() {
        // 网格模式时显示列表图标（表示"点击切换到列表视图"）
        // 列表模式时显示网格图标（表示"点击切换到网格视图"）
        if (fabSwitchMode != null) {
            fabSwitchMode.setImageResource(isGridMode ? 
                    R.drawable.ic_select_list : 
                    R.drawable.ic_select_all);
        }
    }

    private void setViewMode(boolean grid) {
        if (isGridMode == grid) return;
        
        isGridMode = grid;
        
        // 先清除现有的adapter和layoutManager
        adapter = new RecordAdapter(isGridMode);
        
        if (grid) {
            rvRecords.setLayoutManager(new GridLayoutManager(getContext(), 3));
        } else {
            rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        
        // 重新设置适配器
        rvRecords.setAdapter(adapter);
        
        // 更新数据
        if (filteredRecords != null && !filteredRecords.isEmpty()) {
            adapter.setItems(filteredRecords);
        }
        
        // 更新FAB图标
        updateSwitchModeIcon();
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterRecords(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void refreshRecords() {
        // 开始刷新动画
        swipeRefreshLayout.setRefreshing(true);
        // 加载记录
        loadRecords();
    }

    private void loadRecords() {
        // 显示加载动画，但不显示空提示
        if (!swipeRefreshLayout.isRefreshing()) {
            lottieLoading.setVisibility(View.VISIBLE);
        }
        tvEmpty.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String userId = AppwriteWrapper.getCurrentUserId();
                
                List<Map<String, Object>> collections = AppwriteWrapper.getUserCollections(userId);
                
                allRecords.clear();
                if (collections != null && !collections.isEmpty()) {
                    for (Map<String, Object> collection : collections) {
                        String mediaId = (String) collection.get("media_id");
                        
                        Map<String, Object> media = AppwriteWrapper.getMediaById(mediaId);
                        if (media != null) {
                            RecordItem item = new RecordItem();
                            item.setMediaId(mediaId);
                            item.setTitle((String) media.get("title_zh"));
                            item.setSubtitle((String) media.get("title_origin"));
                            item.setPosterUrl((String) media.get("poster_url"));
                            item.setMediaType((String) media.get("media_type"));
                            
                            // 获取评分、年份、时长（按数据库字段正确映射）
                            try {
                                String mediaType = (String) media.get("media_type");
                                
                                // 根据媒体类型设置不同的评分
                                if ("anime".equals(mediaType)) {
                                    // 动漫类型，只使用bangumi评分
                                    Object ratingBangumiObj = media.get("rating_bangumi");
                                    if (ratingBangumiObj != null && !"-1.0".equals(String.valueOf(ratingBangumiObj))) {
                                        String rating = String.format("%.1f", Double.parseDouble(String.valueOf(ratingBangumiObj)));
                                        item.setRating(rating);
                                    }
                                } else {
                                    // 电影或电视剧，优先使用豆瓣评分，其次IMDb评分
                                    StringBuilder ratingBuilder = new StringBuilder();
                                    
                                    Object ratingDoubanObj = media.get("rating_douban");
                                    if (ratingDoubanObj != null && !"-1.0".equals(String.valueOf(ratingDoubanObj))) {
                                        String doubanRating = String.format("%.1f", Double.parseDouble(String.valueOf(ratingDoubanObj)));
                                        ratingBuilder.append(doubanRating);
                                    }
                                    
                                    Object ratingImdbObj = media.get("rating_imdb");
                                    if (ratingImdbObj != null && !"-1.0".equals(String.valueOf(ratingImdbObj))) {
                                        // 如果已经有豆瓣评分，只添加IMDb评分
                                        if (ratingBuilder.length() > 0) {
                                            ratingBuilder.append(" / ");
                                        }
                                        String imdbRating = String.format("%.1f", Double.parseDouble(String.valueOf(ratingImdbObj)));
                                        ratingBuilder.append(imdbRating);
                                    }
                                    
                                    if (ratingBuilder.length() > 0) {
                                        item.setRating(ratingBuilder.toString());
                                    }
                                }
                                
                                // 从release_date中提取年份
                                String releaseDate = (String) media.get("release_date");
                                if (releaseDate != null && !releaseDate.isEmpty()) {
                                    // 直接使用原始release_date
                                    item.setYear(releaseDate);
                                }
                                
                                // 获取时长
                                String duration = (String) media.get("duration");
                                if (duration != null && !duration.isEmpty()) {
                                    item.setDuration(duration);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "解析媒体数据出错: " + e.getMessage(), e);
                            }
                            
                            // 获取并设置观看状态
                            Object watchStatusObj = collection.get("watch_status");
                            if (watchStatusObj != null) {
                                boolean watchStatus = Boolean.parseBoolean(watchStatusObj.toString());
                                item.setWatched(watchStatus);
                                Log.d(TAG, "观看状态: mediaId=" + mediaId + ", watchStatus=" + watchStatus);
                            } else {
                                item.setWatched(false);
                            }
                            
                            allRecords.add(item);
                        } else {
                            Log.e(TAG, "loadRecords: 获取媒体信息失败，mediaId=" + mediaId);
                        }
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    // 停止所有加载动画
                    lottieLoading.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    
                    if (allRecords.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        filterRecords(tabLayout.getSelectedTabPosition());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "loadRecords: 加载失败", e);
                requireActivity().runOnUiThread(() -> {
                    // 停止所有加载动画
                    lottieLoading.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("加载失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void filterRecords(int position) {
        filteredRecords.clear();
        if (position == 0) { // 全部
            filteredRecords.addAll(allRecords);
        } else {
            String type = "";
            switch (position) {
                case 1: type = "movie"; break;
                case 2: type = "tv"; break;
                case 3: type = "anime"; break;
            }
            for (RecordItem item : allRecords) {
                if (type.equals(item.getMediaType())) {
                    filteredRecords.add(item);
                }
            }
        }
        adapter.setItems(filteredRecords);
        adapter.notifyDataSetChanged();
    }
}
