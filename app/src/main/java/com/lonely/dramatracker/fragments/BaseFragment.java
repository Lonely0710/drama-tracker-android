package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.lonely.dramatracker.R;

public abstract class BaseFragment extends Fragment {
    
    protected View mRootView;
    protected Toolbar mToolbar;
    protected ImageView mIvLogo;
    protected TextView mTvTitle;
    protected FrameLayout mSearchContainer;
    protected ImageButton mBtnNotification;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutId(), container, false);
        initView();
        return mRootView;
    }
    
    protected void initView() {
        mToolbar = mRootView.findViewById(R.id.toolbar);
        mIvLogo = mRootView.findViewById(R.id.iv_logo);
        mTvTitle = mRootView.findViewById(R.id.tv_title);
//        mSearchContainer = mRootView.findViewById(R.id.search_container);
        mBtnNotification = mRootView.findViewById(R.id.btn_notification);
    }
    
    protected abstract int getLayoutId();
    
    protected void setTitle(String title) {
        if (mTvTitle != null) {
            mTvTitle.setText(title);
            mTvTitle.setVisibility(View.VISIBLE);
            mIvLogo.setVisibility(View.GONE);
        }
    }
    
    protected void showLogo() {
        if (mIvLogo != null && mTvTitle != null) {
            mIvLogo.setVisibility(View.VISIBLE);
            mTvTitle.setVisibility(View.GONE);
        }
    }
    
    protected void showSearch(boolean show) {
        if (mSearchContainer != null) {
            mSearchContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    protected void showNotification(boolean show) {
        if (mBtnNotification != null) {
            mBtnNotification.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}