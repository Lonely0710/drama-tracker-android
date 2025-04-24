package com.lonely.dramatracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.lonely.dramatracker.R;
import com.lonely.dramatracker.config.AppConfig;
import com.lonely.dramatracker.services.Appwrite;

public class SplashActivity extends BaseActivity {
    
    private static final long SPLASH_DELAY = 3000; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // 初始化AppConfig
        AppConfig.INSTANCE.init(getApplicationContext());
        
        // 初始化Appwrite服务
        Appwrite.INSTANCE.init(getApplicationContext());
        
        // 延迟跳转到登录页面
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }, SPLASH_DELAY);
    }
}
