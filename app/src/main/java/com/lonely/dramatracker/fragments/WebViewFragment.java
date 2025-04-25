package com.lonely.dramatracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.widget.TextView;
import android.widget.ImageButton;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.models.WebSite;
import android.graphics.Bitmap;

public class WebViewFragment extends Fragment {
    private WebView webView;
    private ProgressBar progressBar;
    private WebSite webSite;
    private View bottomNavBg;
    private View bottomNavContainer;
    private View fabAdd;
    private View scrollContainer;
    private TextView tvTitle;
    private String currentSite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentSite = getArguments().getString("site_name");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        
        // 获取传递的网站类型
        String siteName = getArguments().getString("site_name");
        webSite = WebSite.valueOf(siteName);

        // 初始化视图
        webView = view.findViewById(R.id.webView);
        progressBar = view.findViewById(R.id.progress_bar);
        tvTitle = view.findViewById(R.id.tv_title);
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        
        // 设置标题
        tvTitle.setText(webSite.getName());
        
        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> handleBackPress());
        
        // 获取底部导航栏相关视图并隐藏
        bottomNavBg = requireActivity().findViewById(R.id.bottom_nav_bg);
        bottomNavContainer = requireActivity().findViewById(R.id.bottom_nav_container);
        fabAdd = requireActivity().findViewById(R.id.fab_add);
        scrollContainer = requireActivity().findViewById(R.id.scroll_container);
        
        hideNavigationElements();

        // 配置WebView
        configureWebView();

        return view;
    }

    private void hideNavigationElements() {
        if (bottomNavBg != null) {
            bottomNavBg.setVisibility(View.GONE);
        }
        if (bottomNavContainer != null) {
            bottomNavContainer.setVisibility(View.GONE);
        }
        if (fabAdd != null) {
            fabAdd.setVisibility(View.GONE);
        }
        // 移除底部margin
        if (scrollContainer != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) scrollContainer.getLayoutParams();
            params.bottomMargin = 0;
            scrollContainer.setLayoutParams(params);
        }
    }

    private void showNavigationElements() {
        if (bottomNavBg != null) {
            bottomNavBg.setVisibility(View.VISIBLE);
        }
        if (bottomNavContainer != null) {
            bottomNavContainer.setVisibility(View.VISIBLE);
        }
        if (fabAdd != null) {
            fabAdd.setVisibility(View.VISIBLE);
        }
        // 恢复底部margin
        if (scrollContainer != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) scrollContainer.getLayoutParams();
            params.bottomMargin = (int) getResources().getDimension(R.dimen.action_bar_size);
            scrollContainer.setLayoutParams(params);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setDefaultTextEncodingName("utf-8");
        
        // 启用硬件加速
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // 启用嵌套滚动
        webView.setNestedScrollingEnabled(true);
        
        // 优化滚动处理
        webView.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        
        // 注入CSS优化滚动
        String css = "body{margin:0;padding:0;-webkit-tap-highlight-color:transparent}" +
                    "html,body{overflow-x:hidden;overflow-y:auto;-webkit-overflow-scrolling:touch}";
        webView.loadUrl("javascript:(function() {" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.innerHTML = '" + css + "';" +
                "document.head.appendChild(style);" +
                "})()");
                
        // 设置WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // 设置WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        // 加载对应网站的URL
        webView.loadUrl(webSite.getUrl());
    }

    private void handleBackPress() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // 返回到HomeFragment
            FragmentManager fragmentManager = getParentFragmentManager();
            if (fragmentManager.getBackStackEntryCount() > 0) {
                fragmentManager.popBackStack();
            } else {
                // 如果没有回退栈，创建新的HomeFragment
                HomeFragment homeFragment = new HomeFragment();
                fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
                    .commit();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public void onDestroyView() {
        // 恢复底部导航栏显示
        showNavigationElements();
        
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
        }
    }

    public String getCurrentSite() {
        return currentSite;
    }
}