package com.lonely.dramatracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.lonely.dramatracker.R;

public class SplashActivity extends BaseActivity {
    
    private static final long SPLASH_DELAY = 2000; // 2秒延迟
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // 延迟跳转到登录页面
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }, SPLASH_DELAY);
    }
}
