package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.View;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.fragments.BaseFragment;

public class RecommendFragment extends BaseFragment {

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
        // TODO: 初始化推荐页面内容
    }
}
