package com.lonely.dramatracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.services.AppwriteWrapper;

public class LoginActivity extends BaseActivity {
    
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnRegister;
    private MaterialButton btnForgotPassword;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);
    }
    
    private void setupListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v -> navigateToRegister());
        btnForgotPassword.setOnClickListener(v -> {
            showToast("忘记密码功能开发中");
        });
    }
    
    private void handleLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            etUsername.setError("请输入邮箱");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            return;
        }
        
        // 显示加载动画
        showLoading(true, btnLogin, btnRegister, btnForgotPassword);
        
        // 使用AppwriteWrapper进行登录
        new Thread(() -> {
            try {
                AppwriteWrapper.login(email, password);
                
                // 登录成功，跳转到主页面
                runOnUiThread(() -> {
                    showLoading(false, btnLogin, btnRegister, btnForgotPassword);
                    showToast("登录成功");
                    navigateToMain();
                });
            } catch (Exception e) {
                // 登录失败，显示错误信息
                runOnUiThread(() -> {
                    showLoading(false, btnLogin, btnRegister, btnForgotPassword);
                    showError(e.getMessage());
                });
            }
        }).start();
    }
    
    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
