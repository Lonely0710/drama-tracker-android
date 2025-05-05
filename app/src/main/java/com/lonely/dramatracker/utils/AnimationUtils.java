package com.lonely.dramatracker.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

/**
 * 动画工具类，提供各种UI元素的动画效果
 */
public class AnimationUtils {

    /**
     * 按钮点击动画效果
     * 实现按钮按下时的缩放和透明度变化动画
     *
     * @param view 需要添加动画的视图（按钮）
     * @param onAnimationEnd 动画结束时的回调（可选）
     */
    public static void playButtonClickAnimation(View view, Runnable onAnimationEnd) {
        // 禁用按钮以防止动画过程中重复点击
        view.setEnabled(false);
        
        // 创建缩放动画
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1f);
        
        // 创建透明度动画
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f);
        
        // 组合动画
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(300); // 动画持续时间，单位毫秒
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator()); // 设置插值器
        
        // 设置动画结束回调
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // 动画结束后重新启用按钮
                view.setEnabled(true);
                
                // 执行回调
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }
        });
        
        // 开始动画
        animatorSet.start();
    }
    
    /**
     * 无回调的按钮点击动画
     * 
     * @param view 需要添加动画的视图（按钮）
     */
    public static void playButtonClickAnimation(View view) {
        playButtonClickAnimation(view, null);
    }
    
    /**
     * 按钮波纹效果（用于Material风格按钮）
     * 如果按钮已经有ripple效果，则不需要调用此方法
     *
     * @param view 需要添加波纹效果的视图
     */
    public static void addRippleEffect(View view) {
        // 为按钮添加ripple效果
        // 注意：Android 5.0+已内置Ripple效果，通常在XML中使用
        // android:background="?attr/selectableItemBackground"或
        // android:foreground="?attr/selectableItemBackground"
        
        // 这里提供代码方式添加ripple效果
        view.setClickable(true);
        view.setFocusable(true);
        
        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
        android.content.res.TypedArray typedArray = view.getContext().obtainStyledAttributes(attrs);
        android.graphics.drawable.Drawable drawable = typedArray.getDrawable(0);
        typedArray.recycle();
        
        // 设置背景
        view.setForeground(drawable);
    }
    
    /**
     * 元素进入动画（从下方滑入）
     *
     * @param view 需要添加动画的视图
     * @param duration 动画持续时间，单位毫秒
     * @param delay 动画延迟时间，单位毫秒
     */
    public static void slideInFromBottom(View view, int duration, int delay) {
        view.setVisibility(View.VISIBLE);
        view.setTranslationY(view.getHeight());
        view.setAlpha(0f);
        
        view.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
    
    /**
     * 元素淡入动画
     *
     * @param view 需要添加动画的视图
     * @param duration 动画持续时间，单位毫秒
     */
    public static void fadeIn(View view, int duration) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
} 