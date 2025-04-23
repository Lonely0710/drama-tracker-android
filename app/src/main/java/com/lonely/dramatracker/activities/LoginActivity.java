package com.lonely.dramatracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lonely.dramatracker.R;

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
        
        initView();
        initListeners();
    }
    
    private void initView() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);
    }
    
    private void initListeners() {
        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            
            if (TextUtils.isEmpty(username)) {
                etUsername.setError("请输入用户名");
                return;
            }
            
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("请输入密码");
                return;
            }
            
            // TODO: 实现实际的登录逻辑
            // 这里暂时直接跳转到主界面
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        
        // 注册按钮点击事件
        btnRegister.setOnClickListener(v -> {
            // TODO: 实现注册功能
            Toast.makeText(this, "注册功能开发中", Toast.LENGTH_SHORT).show();
        });
        
        // 忘记密码按钮点击事件
        btnForgotPassword.setOnClickListener(v -> {
            // TODO: 实现忘记密码功能
            Toast.makeText(this, "忘记密码功能开发中", Toast.LENGTH_SHORT).show();
        });
    }
}
