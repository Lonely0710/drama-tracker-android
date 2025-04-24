package com.lonely.dramatracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.services.AppwriteWrapper;

import java.util.Random;

public class RegisterActivity extends BaseActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnRegister;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> handleRegister());
        btnLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void handleRegister() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(email)) {
            etUsername.setError("请输入邮箱");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("请确认密码");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            return;
        }

        // 显示加载动画
        showLoading(true, btnRegister, btnLogin);

        // 使用AppwriteWrapper进行注册
        new Thread(() -> {
            try {
                // 生成默认用户名：追剧人+6位随机字母
                String defaultUserName = "追剧人" + generateRandomLetters(6);
                
                // 使用邮箱作为用户名
                AppwriteWrapper.register(email, password, defaultUserName);
                
                // 注册成功，跳转到登录界面
                runOnUiThread(() -> {
                    showLoading(false, btnRegister, btnLogin);
                    showToast("注册成功，请登录");
                    navigateToLogin();
                });
            } catch (Exception e) {
                // 注册失败，显示错误信息
                runOnUiThread(() -> {
                    showLoading(false, btnRegister, btnLogin);
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 生成指定长度的随机字母
     * @param length 字母长度
     * @return 随机字母字符串
     */
    private String generateRandomLetters(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            // 生成随机小写字母 (a-z)
            char c = (char) (random.nextInt(26) + 'a');
            sb.append(c);
        }
        return sb.toString();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
} 