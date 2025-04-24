package com.lonely.dramatracker.activities;

import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.lonely.dramatracker.R;

public abstract class BaseActivity extends AppCompatActivity {
    
    protected CircularProgressIndicator progressIndicator;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
    
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // 针对Android 11以下版本
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    /**
     * 显示或隐藏加载动画
     * @param show 是否显示加载动画
     * @param buttons 需要控制的按钮数组
     */
    protected void showLoading(boolean show, MaterialButton... buttons) {
        if (progressIndicator == null) {
            progressIndicator = findViewById(R.id.progress_indicator);
        }
        
        if (show) {
            // 显示加载动画，隐藏按钮
            progressIndicator.setVisibility(View.VISIBLE);
            // 设置按钮状态和透明度
            for (MaterialButton button : buttons) {
                if (button != null) {
                    button.setEnabled(false);
                    button.setAlpha(0.5f);
                }
            }
        } else {
            // 隐藏加载动画，显示按钮
            progressIndicator.setVisibility(View.GONE);
            // 恢复按钮状态和透明度
            for (MaterialButton button : buttons) {
                if (button != null) {
                    button.setEnabled(true);
                    button.setAlpha(1.0f);
                }
            }
        }
    }

    /**
     * 显示Toast消息
     * @param message 消息内容
     */
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示错误Toast消息
     * @param message 错误消息
     */
    protected void showError(String message) {
        Toast.makeText(this, "错误: " + message, Toast.LENGTH_SHORT).show();
    }
} 