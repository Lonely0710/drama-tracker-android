package com.lonely.dramatracker.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.cardview.widget.CardView;

import com.lonely.dramatracker.R;
import com.lonely.dramatracker.activities.LoginActivity;
import com.lonely.dramatracker.config.AppConfig;
import com.lonely.dramatracker.services.Appwrite;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class SettingsFragment extends BaseFragment {

    private CardView btnLogout;

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
        // 初始化退出登录按钮
        btnLogout = requireView().findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        try {
            // 初始化配置
            AppConfig.INSTANCE.init(requireContext());

            // 显示加载动画
            showLoading(true);

            // 调用 Appwrite 的登出方法
            Appwrite.INSTANCE.logoutWithCallback(
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        // 登出成功
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showLoading(false);
                                showToast("已退出登录");
                                
                                // 跳转到登录页面
                                Intent intent = new Intent(requireContext(), LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                requireActivity().finish();
                            }
                        });
                        return Unit.INSTANCE;
                    }
                },
                new Function1<Exception, Unit>() {
                    @Override
                    public Unit invoke(Exception e) {
                        // 登出失败
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showLoading(false);
                                showToast("退出登录失败: " + e.getMessage());
                            }
                        });
                        return Unit.INSTANCE;
                    }
                }
            );
        } catch (Exception e) {
            showLoading(false);
            showToast("初始化失败: " + e.getMessage());
        }
    }
}
