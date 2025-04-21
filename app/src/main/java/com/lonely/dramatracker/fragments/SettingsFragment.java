package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.View;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.fragments.BaseFragment;

public class SettingsFragment extends BaseFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_settings;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setTitle(getString(R.string.title_settings));
        showLogo();
        showNotification(true);
        
        // 初始化设置页面内容
        initContent();
    }
    
    private void initContent() {
        // TODO: 初始化设置页面内容
    }
}
