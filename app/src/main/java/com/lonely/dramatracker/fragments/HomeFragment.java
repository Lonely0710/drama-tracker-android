package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.Fragment;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.activities.MainActivity;

public class HomeFragment extends BaseFragment {
    private LinearLayout ll_bangumi;
    private LinearLayout ll_douban;
    private LinearLayout ll_imdb;
    private LinearLayout ll_collection;
    private LinearLayout movie_search_bar;
    private LinearLayout anime_search_bar;

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
        
        // 初始化点击事件
        setupClickListeners();
        
        // 初始化首页内容
        initContent();
    }
    
    private void initContent() {
        // TODO: 初始化首页内容，如热门推荐等
    }

    private void setupClickListeners() {
        ll_bangumi.setOnClickListener(v -> openWebView("BANGUMI"));
        ll_douban.setOnClickListener(v -> openWebView("DOUBAN"));
        ll_imdb.setOnClickListener(v -> openWebView("IMDB"));
        ll_collection.setOnClickListener(v -> openRecordFragment());
        
        movie_search_bar.setOnClickListener(v -> openSearch("movie"));
        anime_search_bar.setOnClickListener(v -> openSearch("anime"));
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