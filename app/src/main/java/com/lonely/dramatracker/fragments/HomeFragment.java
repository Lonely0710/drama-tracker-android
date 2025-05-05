package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.Fragment;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.activities.MainActivity;

import java.util.Random;

public class HomeFragment extends BaseFragment {
    private LinearLayout ll_bangumi;
    private LinearLayout ll_douban;
    private LinearLayout ll_imdb;
    private LinearLayout ll_collection;
    private LinearLayout movie_search_bar;
    private LinearLayout anime_search_bar;
    private Button btn_explore;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setTitle(getString(R.string.title_home));
        showLogo();
        showNotification(true);
        
        // 初始化视图
        ll_bangumi = view.findViewById(R.id.ll_bangumi);
        ll_douban = view.findViewById(R.id.ll_douban);
        ll_imdb = view.findViewById(R.id.ll_imdb);
        ll_collection = view.findViewById(R.id.ll_collection);
        movie_search_bar = view.findViewById(R.id.movie_search_bar);
        anime_search_bar = view.findViewById(R.id.anime_search_bar);
        btn_explore = view.findViewById(R.id.btn_explore);
        
        // 初始化点击事件
        setupClickListeners();
        
        // 初始化首页内容
        initContent();
    }
    
    private void initContent() {
        // TODO: 初始化首页内容，如热门推荐等
    }

    private void setupClickListeners() {
        ll_bangumi.setOnClickListener(v -> {
            animateButtonClick(v, () -> openWebView("BANGUMI"));
        });
        
        ll_douban.setOnClickListener(v -> {
            animateButtonClick(v, () -> openWebView("DOUBAN"));
        });
        
        ll_imdb.setOnClickListener(v -> {
            animateButtonClick(v, () -> openWebView("IMDB"));
        });
        
        ll_collection.setOnClickListener(v -> {
            animateButtonClick(v, () -> openRecordFragment());
        });
        
        movie_search_bar.setOnClickListener(v -> {
            animateButtonClick(v, () -> openSearch("movie"));
        });
        
        anime_search_bar.setOnClickListener(v -> {
            animateButtonClick(v, () -> openSearch("anime"));
        });
        
        // 添加"开始浏览"按钮点击事件
        btn_explore.setOnClickListener(v -> {
            animateButtonClick(v, () -> openRandomWebsite());
        });
    }
    
    /**
     * 随机选择一个网站并跳转
     */
    private void openRandomWebsite() {
        Random random = new Random();
        String[] sites = {"DOUBAN", "IMDB", "BANGUMI"};
        String randomSite = sites[random.nextInt(sites.length)];
        
        // 生成随机ID
        String randomId = generateRandomId(randomSite);
        
        // 构建完整URL
        String fullUrl = buildFullUrl(randomSite, randomId);
        
        // 跳转到WebViewFragment
        openWebViewWithUrl(randomSite, fullUrl);
    }
    
    /**
     * 根据网站类型生成随机ID
     */
    private String generateRandomId(String site) {
        Random random = new Random();
        
        switch (site) {
            case "DOUBAN":
                // 豆瓣ID通常是7-8位数字
                return String.valueOf(random.nextInt(90000000) + 10000000);
                
            case "IMDB":
                // IMDb ID通常是"tt"开头加7-8位数字
                return "tt" + String.format("%07d", random.nextInt(9999999) + 1);
                
            case "BANGUMI":
                // Bangumi ID通常是1-6位数字
                return String.valueOf(random.nextInt(300000) + 1);
                
            default:
                return "1";
        }
    }
    
    /**
     * 构建完整URL
     */
    private String buildFullUrl(String site, String id) {
        switch (site) {
            case "DOUBAN":
                return "https://movie.douban.com/subject/" + id;
                
            case "IMDB":
                return "https://www.imdb.com/title/" + id;
                
            case "BANGUMI":
                return "https://bgm.tv/subject/" + id;
                
            default:
                return "";
        }
    }
    
    /**
     * 使用自定义URL打开WebViewFragment
     */
    private void openWebViewWithUrl(String siteName, String url) {
        FragmentManager fragmentManager = getParentFragmentManager();
        
        WebViewFragment webViewFragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString("site_name", siteName);
        args.putString("custom_url", url);
        webViewFragment.setArguments(args);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
            R.anim.fragment_slide_enter_right,
            R.anim.fragment_slide_exit_left,
            R.anim.fragment_slide_enter_left,
            R.anim.fragment_slide_exit_right
        );
        transaction.replace(R.id.fragment_container, webViewFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openWebView(String siteName) {
        FragmentManager fragmentManager = getParentFragmentManager();
        
        // 检查是否已经存在WebViewFragment
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof WebViewFragment) {
            WebViewFragment webViewFragment = (WebViewFragment) currentFragment;
            if (siteName.equals(webViewFragment.getCurrentSite())) {
                return;
            }
        }

        WebViewFragment webViewFragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString("site_name", siteName);
        webViewFragment.setArguments(args);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
            R.anim.fragment_slide_enter_right,
            R.anim.fragment_slide_exit_left,
            R.anim.fragment_slide_enter_left,
            R.anim.fragment_slide_exit_right
        );
        transaction.replace(R.id.fragment_container, webViewFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openRecordFragment() {
        FragmentManager fragmentManager = getParentFragmentManager();
        
        // 检查是否已经存在RecordFragment
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof RecordFragment) {
            return;
        }

        // 更新底部导航栏状态
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNavigationState("RECORD");
            return; // MainActivity会处理Fragment切换
        }

        // 如果不是MainActivity，使用默认的Fragment切换逻辑
        RecordFragment recordFragment = new RecordFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
            R.anim.fragment_slide_enter_right,
            R.anim.fragment_slide_exit_left,
            R.anim.fragment_slide_enter_left,
            R.anim.fragment_slide_exit_right
        );
        transaction.replace(R.id.fragment_container, recordFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openSearch(String type) {
        SearchFragment searchFragment = new SearchFragment();
        searchFragment.setSearchType(type);
        searchFragment.setOnSearchResultClickListener(result -> {
            // TODO: 处理搜索结果点击
        });
        searchFragment.setOnCloseListener(() -> {
            // 关闭搜索页面
            getParentFragmentManager().popBackStack();
        });
        
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
            R.anim.fragment_slide_enter_right,
            R.anim.fragment_slide_exit_left,
            R.anim.fragment_slide_enter_left,
            R.anim.fragment_slide_exit_right
        );
        transaction.add(R.id.fragment_container, searchFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}