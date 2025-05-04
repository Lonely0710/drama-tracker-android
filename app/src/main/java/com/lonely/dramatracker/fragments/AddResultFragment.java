package com.lonely.dramatracker.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.api.impl.ApiServiceImpl;
import com.lonely.dramatracker.models.SearchResult;
import com.lonely.dramatracker.services.SearchService;
import com.lonely.dramatracker.services.impl.SearchServiceImpl;
import com.lonely.dramatracker.fragments.WebViewFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索结果展示Fragment
 */
public class AddResultFragment extends Fragment {
    
    private static final String ARG_KEYWORD = "keyword";
    private static final String ARG_RESULT_DOUBAN = "result_douban";
    private static final String ARG_RESULT_BANGUMI = "result_bangumi";
    private static final String ARG_RESULT_IMDB = "result_imdb";
    
    private String keyword;
    private Map<String, SearchResult> searchResults = new HashMap<>();
    private SearchService searchService;
    
    private ImageView ivBack;
    private CardView cardDouban;
    private CardView cardBangumi;
    private CardView cardImdb;
    private LinearLayout layoutNoResult;
    
    public static AddResultFragment newInstance(String keyword, Map<String, SearchResult> results) {
        AddResultFragment fragment = new AddResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEYWORD, keyword);
        
        // 将搜索结果放入Bundle
        if (results.containsKey("douban")) {
            args.putParcelable(ARG_RESULT_DOUBAN, results.get("douban"));
        }
        if (results.containsKey("bgm")) {
            args.putParcelable(ARG_RESULT_BANGUMI, results.get("bgm"));
        }
        if (results.containsKey("imdb")) {
            args.putParcelable(ARG_RESULT_IMDB, results.get("imdb"));
        }
        
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchService = new SearchServiceImpl(new ApiServiceImpl());
        
        if (getArguments() != null) {
            keyword = getArguments().getString(ARG_KEYWORD, "");
            
            // 获取搜索结果
            SearchResult doubanResult = getArguments().getParcelable(ARG_RESULT_DOUBAN);
            SearchResult bangumiResult = getArguments().getParcelable(ARG_RESULT_BANGUMI);
            SearchResult imdbResult = getArguments().getParcelable(ARG_RESULT_IMDB);
            
            if (doubanResult != null) {
                searchResults.put("douban", doubanResult);
            }
            if (bangumiResult != null) {
                searchResults.put("bgm", bangumiResult);
            }
            if (imdbResult != null) {
                searchResults.put("imdb", imdbResult);
            }
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_result, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 设置全屏显示，隐藏状态栏和导航栏
        setFullscreenMode();
        
        initViews(view);
        setupListeners();
        displaySearchResults();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 确保在页面返回时也保持全屏模式
        setFullscreenMode();
    }
    
    /**
     * 设置全屏模式，隐藏状态栏和导航栏
     */
    private void setFullscreenMode() {
        if (getActivity() != null && getActivity().getWindow() != null) {
            // 设置状态栏透明
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
            
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
            
            getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }
    
    private void initViews(View view) {
        ivBack = view.findViewById(R.id.iv_back);
        cardDouban = view.findViewById(R.id.card_douban);
        cardBangumi = view.findViewById(R.id.card_bangumi);
        cardImdb = view.findViewById(R.id.card_imdb);
        layoutNoResult = view.findViewById(R.id.layout_no_result);
    }
    
    private void setupListeners() {
        ivBack.setOnClickListener(v -> {
            // 返回搜索对话框
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }
    
    private void displaySearchResults() {
        // 如果没有搜索结果，显示提示
        if (searchResults.isEmpty()) {
            layoutNoResult.setVisibility(View.VISIBLE);
            return;
        }
        
        // 显示豆瓣结果
        if (searchResults.containsKey("douban")) {
            SearchResult result = searchResults.get("douban");
            setupResultCard(cardDouban, result, R.id.result_douban, "豆瓣", 
                    R.drawable.ic_douban_green, R.color.douban_green);
        }
        
        // 显示Bangumi结果
        if (searchResults.containsKey("bgm")) {
            SearchResult result = searchResults.get("bgm");
            setupResultCard(cardBangumi, result, R.id.result_bangumi, "Bangumi", 
                    R.drawable.ic_bangumi, R.color.bangumi_pink);
        }
        
        // 显示IMDb结果
        if (searchResults.containsKey("imdb")) {
            SearchResult result = searchResults.get("imdb");
            setupResultCard(cardImdb, result, R.id.result_imdb, "IMDb", 
                    R.drawable.ic_imdb, R.color.imdb_yellow);
        }
    }
    
    private void setupResultCard(CardView cardView, SearchResult result, int resultViewId, 
                                String sourceName, int sourceIconResId, int sourceColorResId) {
        if (result == null) return;
        
        // 显示卡片
        cardView.setVisibility(View.VISIBLE);
        
        // 获取结果项视图
        View resultView = cardView.findViewById(resultViewId);
        if (resultView == null) return;
        
        // 设置来源标识栏
        LinearLayout sourceHeader = resultView.findViewById(R.id.source_header);
        ImageView sourceIcon = resultView.findViewById(R.id.source_icon);
        TextView sourceNameText = resultView.findViewById(R.id.source_name);
        
        // 设置来源标识栏颜色和图标
        sourceHeader.setBackgroundResource(sourceColorResId);
        sourceIcon.setImageResource(sourceIconResId);
        sourceNameText.setText(sourceName);
        
        // 设置UI元素
        ImageView ivPoster = resultView.findViewById(R.id.iv_poster);
        TextView tvTitle = resultView.findViewById(R.id.tv_title);
        TextView tvOriginalTitle = resultView.findViewById(R.id.tv_original_title);
        LinearLayout layoutRating = resultView.findViewById(R.id.layout_rating);
        TextView tvRating = resultView.findViewById(R.id.tv_rating);
        TextView tvInfo = resultView.findViewById(R.id.tv_info);
        Button btnAdd = resultView.findViewById(R.id.btn_add);
        Button btnView = resultView.findViewById(R.id.btn_view);
        
        // 加载海报
        if (result.getPosterUrl() != null && !result.getPosterUrl().isEmpty()) {
            Glide.with(this)
                    .load(result.getPosterUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.placeholder_poster)
                    .into(ivPoster);
        } else {
            ivPoster.setImageResource(R.drawable.placeholder_poster);
        }
        
        // 设置标题
        String title = result.getTitleZh();
        if (result.getYear() != null && !result.getYear().isEmpty()) {
            title += " (" + result.getYear() + ")";
        }
        tvTitle.setText(title);
        
        // 设置原始标题
        if (result.getTitleOriginal() != null && !result.getTitleOriginal().isEmpty()) {
            tvOriginalTitle.setText(result.getTitleOriginal());
            tvOriginalTitle.setVisibility(View.VISIBLE);
        } else {
            tvOriginalTitle.setVisibility(View.GONE);
        }
        
        // 设置评分
        double rating = -1;
        switch (result.getSourceType()) {
            case "douban":
                rating = result.getRatingDouban();
                break;
            case "bgm":
                rating = result.getRatingBangumi();
                break;
            case "imdb":
                rating = result.getRatingImdb();
                break;
            default:
                rating = result.getRating();
        }
        
        if (rating > 0) {
            tvRating.setText(String.format("%.1f", rating));
            layoutRating.setVisibility(View.VISIBLE);
        } else {
            layoutRating.setVisibility(View.GONE);
        }
        
        // 设置类型/时长信息
        StringBuilder infoBuilder = new StringBuilder();
        
        // 添加媒体类型
        switch (result.getMediaType()) {
            case "movie":
                infoBuilder.append("电影");
                break;
            case "tv":
                infoBuilder.append("电视剧");
                break;
            case "anime":
                infoBuilder.append("动画");
                break;
        }
        
        // 添加时长/集数
        if (result.getDuration() != null && !result.getDuration().isEmpty()) {
            infoBuilder.append(" | ").append(result.getDuration());
            if (result.getMediaType().equals("movie")) {
                infoBuilder.append("分钟");
            } else if (result.getMediaType().equals("anime") || result.getMediaType().equals("tv")) {
                infoBuilder.append("话");
            }
        }
        
        tvInfo.setText(infoBuilder.toString());
        
        // 设置添加按钮
        if (result.isCollected()) {
            btnAdd.setText("已添加");
            btnAdd.setEnabled(false);
        } else {
            btnAdd.setText("添加");
            btnAdd.setEnabled(true);
            
            btnAdd.setOnClickListener(v -> {
                // 添加到收藏
                addToCollection(result);
            });
        }
        
        // 设置查看详情按钮
        btnView.setOnClickListener(v -> {
            // 跳转到原页面
            openWebViewWithSourceId(result.getSourceType(), result.getSourceId());
        });
    }
    
    private void addToCollection(SearchResult result) {
        if (result == null) return;
        
        // 调用SearchService添加到收藏
        searchService.addToCollection(result, () -> {
            // 添加成功回调
            if (getActivity() == null || !isAdded()) return;
            
            getActivity().runOnUiThread(() -> {
                // 更新UI
                Toast.makeText(requireContext(), 
                        "已添加《" + result.getTitleZh() + "》到影库", 
                        Toast.LENGTH_SHORT).show();
                // 返回主页面
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }, () -> {
            // 添加失败回调
            if (getActivity() == null || !isAdded()) return;
            
            getActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), 
                        "添加失败，请重试", 
                        Toast.LENGTH_SHORT).show();
            });
        });
    }
    
    /**
     * 打开WebView显示源网站详情页
     * @param sourceType 来源类型（douban/imdb/bgm）
     * @param sourceId 来源ID
     */
    private void openWebViewWithSourceId(String sourceType, String sourceId) {
        String siteName;
        String customUrl = null;
        
        // 根据sourceType确定对应的WebSite枚举值
        switch (sourceType.toLowerCase()) {
            case "douban":
                siteName = "DOUBAN";
                customUrl = "https://movie.douban.com/subject/" + sourceId + "/";
                break;
            case "imdb":
                siteName = "IMDB";
                customUrl = "https://www.imdb.com/title/" + sourceId + "/";
                break;
            case "bgm":
                siteName = "BANGUMI";
                customUrl = "https://bgm.tv/subject/" + sourceId;
                break;
            default:
                // 未知类型，无法打开
                Toast.makeText(requireContext(), "无法打开未知来源: " + sourceType, Toast.LENGTH_SHORT).show();
                return;
        }
        
        // 创建WebViewFragment并传递参数
        WebViewFragment webViewFragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString("site_name", siteName);
        
        // 如果有自定义URL，也传递过去
        if (customUrl != null) {
            args.putString("custom_url", customUrl);
        }
        
        webViewFragment.setArguments(args);
        
        // 执行Fragment事务
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
            R.anim.fragment_slide_enter_right,
            R.anim.fragment_slide_exit_left,
            R.anim.fragment_slide_enter_left,
            R.anim.fragment_slide_exit_right
        );
        transaction.replace(R.id.fragment_container, webViewFragment);
        transaction.addToBackStack(null);
        
        // 提交事务
        transaction.commit();
    }
}