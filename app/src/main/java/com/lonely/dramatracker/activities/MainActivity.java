package com.lonely.dramatracker.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.fragments.HomeFragment;
import com.lonely.dramatracker.fragments.RecordFragment;
import com.lonely.dramatracker.fragments.RecommendFragment;
import com.lonely.dramatracker.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    
    private FragmentManager fragmentManager;
    private Fragment currentFragment;
    private FloatingActionButton fabAdd;
    
    private View navHome;
    private View navRecord;
    private View navRecommend;
    private View navSettings;
    
    private HomeFragment homeFragment;
    private RecordFragment recordFragment;
    private RecommendFragment recommendFragment;
    private SettingsFragment settingsFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initView();
        initFragment();
    }
    
    private void initView() {
        fabAdd = findViewById(R.id.fab_add);
        navHome = findViewById(R.id.nav_home);
        navRecord = findViewById(R.id.nav_record);
        navRecommend = findViewById(R.id.nav_recommend);
        navSettings = findViewById(R.id.nav_settings);
        
        // 设置导航项点击事件
        navHome.setOnClickListener(v -> {
            updateNavSelection(navHome);
            switchFragment(homeFragment);
        });
        
        navRecord.setOnClickListener(v -> {
            updateNavSelection(navRecord);
            switchFragment(recordFragment);
        });
        
        navRecommend.setOnClickListener(v -> {
            updateNavSelection(navRecommend);
            switchFragment(recommendFragment);
        });
        
        navSettings.setOnClickListener(v -> {
            updateNavSelection(navSettings);
            switchFragment(settingsFragment);
        });
        
        // 处理中间的添加按钮
        fabAdd.setOnClickListener(v -> {
            // TODO: 处理添加按钮点击事件
        });
    }
    
    private void updateNavSelection(View selectedNav) {
        // 重置所有导航项的颜色
        resetNavColors();
        
        // 更新选中项的颜色
        int primaryColor = getResources().getColor(R.color.primary, getTheme());
        
        if (selectedNav.getId() == R.id.nav_home) {
            ImageView icon = selectedNav.findViewById(R.id.nav_home_icon);
            TextView text = selectedNav.findViewById(R.id.nav_home_text);
            
            if (icon != null) {
                icon.setColorFilter(primaryColor);
            }
            
            if (text != null) {
                text.setTextColor(primaryColor);
            }
        } else if (selectedNav.getId() == R.id.nav_record) {
            ImageView icon = selectedNav.findViewById(R.id.nav_record_icon);
            TextView text = selectedNav.findViewById(R.id.nav_record_text);
            
            if (icon != null) {
                icon.setColorFilter(primaryColor);
            }
            
            if (text != null) {
                text.setTextColor(primaryColor);
            }
        } else if (selectedNav.getId() == R.id.nav_recommend) {
            ImageView icon = selectedNav.findViewById(R.id.nav_recommend_icon);
            TextView text = selectedNav.findViewById(R.id.nav_recommend_text);
            
            if (icon != null) {
                icon.setColorFilter(primaryColor);
            }
            
            if (text != null) {
                text.setTextColor(primaryColor);
            }
        } else if (selectedNav.getId() == R.id.nav_settings) {
            ImageView icon = selectedNav.findViewById(R.id.nav_settings_icon);
            TextView text = selectedNav.findViewById(R.id.nav_settings_text);
            
            if (icon != null) {
                icon.setColorFilter(primaryColor);
            }
            
            if (text != null) {
                text.setTextColor(primaryColor);
            }
        }
    }
    
    private void resetNavColors() {
        int secondaryColor = getResources().getColor(R.color.text_secondary, getTheme());
        
        // 首页
        ImageView homeIcon = navHome.findViewById(R.id.nav_home_icon);
        TextView homeText = navHome.findViewById(R.id.nav_home_text);
        if (homeIcon != null) {
            homeIcon.setColorFilter(secondaryColor);
        }
        if (homeText != null) {
            homeText.setTextColor(secondaryColor);
        }
        
        // 记录
        ImageView recordIcon = navRecord.findViewById(R.id.nav_record_icon);
        TextView recordText = navRecord.findViewById(R.id.nav_record_text);
        if (recordIcon != null) {
            recordIcon.setColorFilter(secondaryColor);
        }
        if (recordText != null) {
            recordText.setTextColor(secondaryColor);
        }
        
        // 推荐
        ImageView recommendIcon = navRecommend.findViewById(R.id.nav_recommend_icon);
        TextView recommendText = navRecommend.findViewById(R.id.nav_recommend_text);
        if (recommendIcon != null) {
            recommendIcon.setColorFilter(secondaryColor);
        }
        if (recommendText != null) {
            recommendText.setTextColor(secondaryColor);
        }
        
        // 设置
        ImageView settingsIcon = navSettings.findViewById(R.id.nav_settings_icon);
        TextView settingsText = navSettings.findViewById(R.id.nav_settings_text);
        if (settingsIcon != null) {
            settingsIcon.setColorFilter(secondaryColor);
        }
        if (settingsText != null) {
            settingsText.setTextColor(secondaryColor);
        }
    }
    
    private void initFragment() {
        fragmentManager = getSupportFragmentManager();
        homeFragment = new HomeFragment();
        recordFragment = new RecordFragment();
        recommendFragment = new RecommendFragment();
        settingsFragment = new SettingsFragment();
        
        // 默认显示首页
        switchFragment(homeFragment);
        updateNavSelection(navHome);
    }
    
    private void switchFragment(Fragment targetFragment) {
        if (currentFragment == targetFragment) {
            return;
        }
        
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // 隐藏当前Fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        
        // 显示目标Fragment
        if (targetFragment.isAdded()) {
            transaction.show(targetFragment);
        } else {
            transaction.add(R.id.fragment_container, targetFragment);
        }
        
        transaction.commit();
        currentFragment = targetFragment;
    }
}
