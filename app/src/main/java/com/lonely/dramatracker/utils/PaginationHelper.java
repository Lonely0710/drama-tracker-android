package com.lonely.dramatracker.utils;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lonely.dramatracker.R;

/**
 * 分页辅助工具类
 * 封装分页相关逻辑，减轻Fragment代码负担
 */
public class PaginationHelper {
    private static final String TAG = "PaginationHelper";
    
    // 最大显示页码按钮数
    private static final int DEFAULT_MAX_PAGE_BUTTONS = 5;
    
    // 分页状态
    private boolean isLoading = false;
    private boolean isLastPage = false;
    
    // 当前页码和总页数
    private int currentPage = 1;
    private int totalPages = 1;
    
    // 各分类当前页码和总页数
    private int[] categoryCurrentPages;
    private int[] categoryTotalPages;
    
    // 视图组件
    private View paginationLayout;
    private LinearLayout pageNumberContainer;
    private ImageButton btnPrevPage;
    private ImageButton btnNextPage;
    private TextView tvTotalPages;
    private ProgressBar paginationLoading;
    private ProgressBar mainProgressBar;
    private RecyclerView recyclerView;
    private TextView emptyView;
    
    // 上下文
    private Context context;
    
    // 页面加载监听器
    private OnPageLoadListener onPageLoadListener;
    
    // 分页按钮样式配置
    private @DrawableRes int activePageDrawable = R.drawable.bg_page_number_active;
    private @DrawableRes int normalPageDrawable = R.drawable.bg_page_number_normal;
    private @ColorInt int activeTextColor = Color.WHITE;
    private @ColorInt int normalTextColor;
    
    // 缓冲区处理
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // 页码配置
    private int maxPageButtons = DEFAULT_MAX_PAGE_BUTTONS;
    
    /**
     * 页面加载监听器接口
     */
    public interface OnPageLoadListener {
        void onLoadPage(int category, int page);
    }
    
    /**
     * 构造函数
     * @param context 上下文
     * @param categoryCount 分类数量
     */
    public PaginationHelper(@NonNull Context context, int categoryCount) {
        this.context = context;
        
        // 初始化分类页码数组
        this.categoryCurrentPages = new int[categoryCount];
        this.categoryTotalPages = new int[categoryCount];
        
        // 初始化默认值
        for (int i = 0; i < categoryCount; i++) {
            categoryCurrentPages[i] = 1;
            categoryTotalPages[i] = 1;
        }
        
        // 设置默认文本颜色
        normalTextColor = ContextCompat.getColor(context, R.color.text_primary);
    }
    
    /**
     * 设置视图组件
     */
    public void setupViews(
            View paginationLayout,
            LinearLayout pageNumberContainer,
            ImageButton btnPrevPage,
            ImageButton btnNextPage,
            TextView tvTotalPages,
            ProgressBar paginationLoading,
            ProgressBar mainProgressBar,
            RecyclerView recyclerView,
            TextView emptyView) {
        
        this.paginationLayout = paginationLayout;
        this.pageNumberContainer = pageNumberContainer;
        this.btnPrevPage = btnPrevPage;
        this.btnNextPage = btnNextPage;
        this.tvTotalPages = tvTotalPages;
        this.paginationLoading = paginationLoading;
        this.mainProgressBar = mainProgressBar;
        this.recyclerView = recyclerView;
        this.emptyView = emptyView;
        
        // 设置分页按钮点击事件
        setupPaginationButtons();
        
        // 默认隐藏分页导航
        if (paginationLayout != null) {
            paginationLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 设置页面加载监听器
     */
    public void setOnPageLoadListener(OnPageLoadListener listener) {
        this.onPageLoadListener = listener;
    }
    
    /**
     * 设置分页按钮点击事件
     */
    private void setupPaginationButtons() {
        if (btnPrevPage == null || btnNextPage == null) return;
        
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1 && !isLoading) {
                // 先滚动到顶部
                scrollToTop();
                
                // 显示加载状态
                showLoading(true);
                
                // 使用Handler延迟执行加载
                handler.postDelayed(() -> {
                    if (onPageLoadListener != null) {
                        onPageLoadListener.onLoadPage(getCurrentCategory(), currentPage - 1);
                    }
                }, 200);
            }
        });
        
        btnNextPage.setOnClickListener(v -> {
            if (!isLoading) {
                // 先滚动到顶部
                scrollToTop();
                
                // 显示加载状态
                showLoading(true);
                
                // 使用Handler延迟执行加载
                handler.postDelayed(() -> {
                    if (currentPage < totalPages) {
                        if (onPageLoadListener != null) {
                            onPageLoadListener.onLoadPage(getCurrentCategory(), currentPage + 1);
                        }
                    } else if (!isLastPage) {
                        // 总页数可能不准确，尝试加载下一页
                        if (onPageLoadListener != null) {
                            onPageLoadListener.onLoadPage(getCurrentCategory(), currentPage + 1);
                        }
                    }
                }, 200);
            }
        });
    }
    
    /**
     * 滚动到顶部
     */
    public void scrollToTop() {
        if (recyclerView == null) return;
        
        // 确保RecyclerView可见
        recyclerView.setVisibility(View.VISIBLE);
        
        // 处理大数据列表
        if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 30) {
            recyclerView.scrollToPosition(0);
        } else {
            recyclerView.smoothScrollToPosition(0);
        }
    }
    
    /**
     * 显示初始加载动画
     */
    public void showInitialLoadingAnimation() {
        isLoading = true;
        
        if (mainProgressBar != null) {
            mainProgressBar.setVisibility(View.VISIBLE);
        }
        
        if (recyclerView != null) {
            recyclerView.setAlpha(0.7f);
        }
        
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
        
        if (paginationLayout != null) {
            paginationLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 检查当前分类是否为空
     */
    private boolean isCurrentCategoryEmpty() {
        if (onPageLoadListener instanceof EmptyStateChecker) {
            return ((EmptyStateChecker) onPageLoadListener).isCurrentCategoryEmpty();
        }
        return true;
    }
    
    /**
     * 显示或隐藏加载状态
     */
    public void showLoading(boolean isLoading) {
        this.isLoading = isLoading;
        
        if (isLoading) {
            // 显示加载状态
            if (mainProgressBar != null) {
                mainProgressBar.setVisibility(View.VISIBLE);
            }
            
            if (recyclerView != null) {
                recyclerView.setAlpha(0.7f);
            }
            
            if (paginationLayout != null) {
                paginationLayout.setAlpha(0.5f);
                if (btnPrevPage != null) btnPrevPage.setEnabled(false);
                if (btnNextPage != null) btnNextPage.setEnabled(false);
            }
        } else {
            // 加载完成
            if (mainProgressBar != null) {
                mainProgressBar.setVisibility(View.GONE);
            }
            
            // 检查是否显示空状态
            checkEmptyState();
            
            // 恢复列表
            if (recyclerView != null) {
                recyclerView.setAlpha(1.0f);
            }
            
            // 确保分页导航在有数据时可见
            if (!isCurrentCategoryEmpty() && paginationLayout != null) {
                paginationLayout.setVisibility(View.VISIBLE);
                paginationLayout.setAlpha(1.0f);
                updatePaginationUI();
            }
        }
    }
    
    /**
     * 显示页面加载动画 - 简化版
     */
    public void showPageLoading(boolean isLoading) {
        this.isLoading = isLoading;
        
        if (paginationLoading != null) {
            paginationLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        if (isLoading) {
            if (recyclerView != null) {
                recyclerView.setAlpha(0.7f);
            }
            
            if (btnPrevPage != null) btnPrevPage.setEnabled(false);
            if (btnNextPage != null) btnNextPage.setEnabled(false);
            
            if (mainProgressBar != null) {
                mainProgressBar.setVisibility(View.VISIBLE);
            }
        } else {
            if (recyclerView != null) {
                recyclerView.setAlpha(1.0f);
            }
            
            if (mainProgressBar != null) {
                mainProgressBar.setVisibility(View.GONE);
            }
            
            updatePaginationUI();
        }
    }
    
    /**
     * 显示空状态
     */
    public void showEmptyState() {
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        
        if (paginationLayout != null) {
            paginationLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 检查是否显示空状态
     */
    public void checkEmptyState() {
        boolean isEmpty = false;
        
        if (onPageLoadListener instanceof EmptyStateChecker) {
            isEmpty = ((EmptyStateChecker) onPageLoadListener).isCurrentCategoryEmpty();
        }
        
        if (isEmpty) {
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
            
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.GONE);
            }
        } else {
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }
            
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            
            if (paginationLayout != null) {
                paginationLayout.setVisibility(View.VISIBLE);
            }
            
            updatePaginationUI();
        }
    }
    
    /**
     * 更新分页导航UI
     */
    public void updatePaginationUI() {
        if (paginationLayout == null || pageNumberContainer == null) {
            return;
        }
        
        // 显示分页导航栏
        paginationLayout.setVisibility(View.VISIBLE);
        
        // 设置总页数提示
        if (tvTotalPages != null) {
            String pagesInfo = String.format("共 %d 页", totalPages);
            tvTotalPages.setText(pagesInfo);
        }
        
        // 上一页按钮状态
        if (btnPrevPage != null) {
            btnPrevPage.setEnabled(currentPage > 1);
            btnPrevPage.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
        }
        
        // 下一页按钮状态
        if (btnNextPage != null) {
            btnNextPage.setEnabled(!isLastPage && currentPage < totalPages);
            btnNextPage.setAlpha(!isLastPage && currentPage < totalPages ? 1.0f : 0.5f);
        }
        
        // 清空原有页码按钮
        pageNumberContainer.removeAllViews();
        
        // 确保总页数至少为1
        if (totalPages <= 0) totalPages = 1;
        
        // 如果总页数很小，直接显示所有页码
        if (totalPages <= maxPageButtons) {
            for (int i = 1; i <= totalPages; i++) {
                addPageButton(String.valueOf(i), i);
            }
            return;
        }
        
        // 对于较多页码的情况，使用更智能的显示逻辑
        int halfRange = maxPageButtons / 2;
        int startPage = Math.max(1, currentPage - halfRange);
        int endPage = Math.min(totalPages, startPage + maxPageButtons - 1);
        
        // 调整startPage
        if (endPage - startPage + 1 < maxPageButtons) {
            startPage = Math.max(1, endPage - maxPageButtons + 1);
        }
        
        // 显示第一页
        addPageButton("1", 1);
        
        // 显示省略号
        if (startPage > 2) {
            addPageButton("...", -1);
        }
        
        // 显示中间的页码
        for (int i = Math.max(2, startPage); i <= Math.min(endPage, totalPages - 1); i++) {
            addPageButton(String.valueOf(i), i);
        }
        
        // 显示省略号
        if (endPage < totalPages - 1) {
            addPageButton("...", -1);
        }
        
        // 显示最后一页
        if (totalPages > 1) {
            addPageButton(String.valueOf(totalPages), totalPages);
        }
    }
    
    /**
     * 添加页码按钮
     */
    private void addPageButton(String text, int pageNum) {
        boolean isActive = pageNum == currentPage;
        
        // 创建按钮
        TextView pageButton = new TextView(context);
        pageButton.setText(text);
        pageButton.setTextSize(14);
        pageButton.setGravity(Gravity.CENTER);
        
        // 设置布局参数
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                context.getResources().getDimensionPixelSize(R.dimen.page_indicator_size),
                context.getResources().getDimensionPixelSize(R.dimen.page_indicator_size)
        );
        params.setMargins(4, 0, 4, 0);
        pageButton.setLayoutParams(params);
        
        // 设置样式
        if (isActive) {
            // 当前页样式
            pageButton.setBackgroundResource(activePageDrawable);
            pageButton.setTextColor(activeTextColor);
        } else {
            // 其他页样式
            pageButton.setBackgroundResource(normalPageDrawable);
            pageButton.setTextColor(normalTextColor);
        }
        
        // 设置点击事件（只有实际页码按钮才可点击）
        if (pageNum > 0) {
            pageButton.setOnClickListener(v -> {
                if (currentPage != pageNum && !isLoading) {
                    // 先滚动到顶部
                    scrollToTop();
                    
                    // 显示加载状态
                    showLoading(true);
                    
                    // 延迟加载
                    handler.postDelayed(() -> {
                        if (onPageLoadListener != null) {
                            onPageLoadListener.onLoadPage(getCurrentCategory(), pageNum);
                        }
                    }, 200);
                }
            });
        } else {
            pageButton.setEnabled(false);
        }
        
        // 添加到容器
        pageNumberContainer.addView(pageButton);
    }
    
    /**
     * 重置分页状态
     */
    public void resetPaginationState() {
        isLastPage = false;
        isLoading = false;
    }
    
    /**
     * 获取当前页码
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * 获取总页数
     */
    public int getTotalPages() {
        return totalPages;
    }
    
    /**
     * 设置当前页码
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        // 同时更新对应分类的当前页码
        int category = getCurrentCategory();
        if (category >= 0 && category < categoryCurrentPages.length) {
            categoryCurrentPages[category] = currentPage;
        }
    }
    
    /**
     * 设置总页数
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        // 同时更新对应分类的总页数
        int category = getCurrentCategory();
        if (category >= 0 && category < categoryTotalPages.length) {
            categoryTotalPages[category] = totalPages;
        }
    }
    
    /**
     * 设置是否为最后一页
     */
    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }
    
    /**
     * 切换分类时更新分页状态
     */
    public void switchCategory(int category) {
        // 确保类型参数有效
        if (category >= 0 && category < categoryCurrentPages.length) {
            // 更新当前页码和总页数
            currentPage = categoryCurrentPages[category];
            totalPages = categoryTotalPages[category];
            
            // 确保总页数至少为1
            if (totalPages <= 0) totalPages = 1;
            
            // 重置分页状态但保留分页数据
            isLastPage = false;
            isLoading = false;
            
            // 更新分页UI
            updatePaginationVisibility();
        }
    }
    
    /**
     * 更新分页导航的显示状态
     */
    private void updatePaginationVisibility() {
        // 如果分页布局为null，直接返回
        if (paginationLayout == null) return;
        
        boolean isEmpty = false;
        // 检查当前分类是否为空
        if (onPageLoadListener instanceof EmptyStateChecker) {
            isEmpty = ((EmptyStateChecker) onPageLoadListener).isCurrentCategoryEmpty();
        }
        
        // 如果当前分类不为空，显示分页导航
        if (!isEmpty) {
            paginationLayout.setVisibility(View.VISIBLE);
            // 更新分页UI
            updatePaginationUI();
        } else {
            paginationLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 获取当前分类
     */
    private int getCurrentCategory() {
        if (onPageLoadListener instanceof CategoryProvider) {
            int category = ((CategoryProvider) onPageLoadListener).getCurrentCategory();
            return category;
        }
        return 0;
    }
    
    /**
     * 分类提供者接口
     */
    public interface CategoryProvider {
        int getCurrentCategory();
    }
    
    /**
     * 空状态检查接口
     */
    public interface EmptyStateChecker {
        boolean isCurrentCategoryEmpty();
    }
    
    /**
     * 设置所有分类的总页数信息
     * @param allPages "全部"分类的总页数
     * @param moviePages "电影"分类的总页数
     * @param tvPages "电视剧"分类的总页数
     */
    public void setTotalPagesForAllCategories(int allPages, int moviePages, int tvPages) {
        // 确保数组长度足够
        if (categoryTotalPages.length >= 3) {
            categoryTotalPages[0] = allPages;    // "全部"分类总页数
            categoryTotalPages[1] = moviePages;  // "电影"分类总页数
            categoryTotalPages[2] = tvPages;     // "电视剧"分类总页数
            
            // 更新当前分类的总页数
            totalPages = categoryTotalPages[getCurrentCategory()];
            
            // 更新分页UI
            updatePaginationUI();
        }
    }
} 