package com.lonely.dramatracker.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lonely.dramatracker.fragments.DailyTabFragment;
import com.lonely.dramatracker.fragments.PointsTabFragment;
import com.lonely.dramatracker.fragments.RecentTabFragment;

/**
 * 推荐页面ViewPager2适配器
 */
public class RecommendPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 3;

    public static final int TAB_RECENT = 0;
    public static final int TAB_DAILY = 1;
    public static final int TAB_TOP_RATED = 2;

    public RecommendPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    public RecommendPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_RECENT:
                return new RecentTabFragment();
            case TAB_DAILY:
                return new DailyTabFragment();
            case TAB_TOP_RATED:
                return new PointsTabFragment();
            default:
                return new RecentTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
} 