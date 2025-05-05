package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.lonely.dramatracker.R;
import com.lonely.dramatracker.utils.AnimationUtils;

public abstract class BaseFragment extends Fragment {
    
    protected View mRootView;
    protected Toolbar mToolbar;
    protected ImageView mIvLogo;
    protected TextView mTvTitle;
    protected FrameLayout mSearchContainer;
    protected ImageButton mBtnNotification;
    protected View mLoadingView;
    
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
        mBtnNotification = mRootView.findViewById(R.id.btn_notification);
        
        // 初始化加载动画视图
        initLoadingView();
    }
    
    private void initLoadingView() {
        // 将加载动画布局添加到根视图
        View loadingView = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_loading, (ViewGroup) mRootView, false);
        ((ViewGroup) mRootView).addView(loadingView);
        mLoadingView = loadingView;
    }
    
    protected abstract int getLayoutId();
    
    protected void setTitle(String title) {
        if (mTvTitle != null) {
            mTvTitle.setText(title);
            mTvTitle.setVisibility(View.VISIBLE);
        }
    }
    
    protected void showLogo() {
        if (mIvLogo != null && mTvTitle != null) {
            mIvLogo.setVisibility(View.VISIBLE);
        }
    }
    
    protected void showNotification(boolean show) {
        if (mBtnNotification != null) {
            mBtnNotification.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 显示或隐藏加载动画
     * @param show true 显示，false 隐藏
     */
    protected void showLoading(boolean show) {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 在主线程执行代码
     * @param runnable 要执行的代码
     */
    protected void runOnUiThread(Runnable runnable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }

    /**
     * 显示提示信息
     * @param message 提示信息内容
     */
    protected void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> 
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show()
            );
        }
    }
    
    /**
     * 按钮点击动画并执行操作
     * 
     * @param view 需要添加动画的视图（按钮）
     * @param action 动画结束后需要执行的操作
     */
    protected void animateButtonClick(View view, Runnable action) {
        AnimationUtils.playButtonClickAnimation(view, action);
    }
    
    /**
     * 只有按钮点击动画，无后续操作
     * 
     * @param view 需要添加动画的视图（按钮）
     */
    protected void animateButtonClick(View view) {
        AnimationUtils.playButtonClickAnimation(view);
    }
}