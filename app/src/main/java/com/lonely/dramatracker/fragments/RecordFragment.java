package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.View;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.fragments.BaseFragment;

public class RecordFragment extends BaseFragment {

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
        
        // 初始化记录页面内容
        initContent();
    }
    
    private void initContent() {
        // TODO: 初始化记录页面内容
    }
}
