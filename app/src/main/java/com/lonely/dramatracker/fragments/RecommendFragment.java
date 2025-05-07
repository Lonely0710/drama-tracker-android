package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.fragments.BaseFragment;
import com.lonely.dramatracker.adapters.RecommendPagerAdapter;

public class RecommendFragment extends BaseFragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private RecommendPagerAdapter pagerAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_recommend;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setTitle(getString(R.string.title_recommend));
        showLogo();
        showNotification(true);
        
        // 初始化推荐页面内容
        initContent();
    }
    
    private void initContent() {
        // 初始化ViewPager2和TabLayout
        tabLayout = mRootView.findViewById(R.id.tab_layout);
        viewPager = mRootView.findViewById(R.id.view_pager);
        
        // 设置ViewPager适配器
        pagerAdapter = new RecommendPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // 连接TabLayout和ViewPager2
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case RecommendPagerAdapter.TAB_RECENT:
                        tab.setText("近期上映");
                        break;
                    case RecommendPagerAdapter.TAB_DAILY:
                        tab.setText("每日放送");
                        break;
                    case RecommendPagerAdapter.TAB_TOP_RATED:
                        tab.setText("高分推荐");
                        break;
                }
            }
        );
        mediator.attach();
        
        // 减少预加载页面数量，提高性能
        viewPager.setOffscreenPageLimit(1);
    }
}
