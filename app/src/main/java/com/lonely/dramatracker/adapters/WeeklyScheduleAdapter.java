package com.lonely.dramatracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lonely.dramatracker.R;
import com.lonely.dramatracker.models.DailyAnime;
import com.lonely.dramatracker.models.WeeklySchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * 每周放送适配器
 */
public class WeeklyScheduleAdapter extends RecyclerView.Adapter<WeeklyScheduleAdapter.WeekViewHolder> {

    private static final int SUNDAY = 0;
    private static final int MONDAY = 1;
    private static final int TUESDAY = 2;
    private static final int WEDNESDAY = 3;
    private static final int THURSDAY = 4;
    private static final int FRIDAY = 5;
    private static final int SATURDAY = 6;

    private WeeklySchedule weeklySchedule;
    private Context context;
    private DailyAnimeAdapter.OnItemClickListener animeClickListener;

    public WeeklyScheduleAdapter(Context context) {
        this.context = context;
        this.weeklySchedule = new WeeklySchedule();
    }

    public void setAnimeClickListener(DailyAnimeAdapter.OnItemClickListener listener) {
        this.animeClickListener = listener;
    }

    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_container, parent, false);
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        // 根据位置获取对应星期的数据
        List<DailyAnime> animeList = getAnimeListByPosition(position);
        if (animeList == null) {
            animeList = new ArrayList<>();
        }

        // 设置作品数量
        holder.tvAnimeCount.setText("共" + animeList.size() + "部");
        
        // 设置日文星期（仅设置星期字符部分）
        setWeekDay(holder.tvWeekDay, position);
        
        // 设置星期背景图片
        int iconResId = getWeekIconResource(position);
        if (iconResId != 0) {
            holder.llWeekContainer.setBackgroundResource(iconResId);
        }

        // 设置动漫列表
        DailyAnimeAdapter animeAdapter = new DailyAnimeAdapter(context, animeList);
        animeAdapter.setOnItemClickListener(animeClickListener);
        holder.rvDailyAnime.setAdapter(animeAdapter);
        
        // 设置左右滚动按钮点击事件
        setupScrollButtons(holder);
    }

    @Override
    public int getItemCount() {
        // 一周7天
        return 7;
    }

    /**
     * 根据位置获取对应星期的动漫列表
     */
    private List<DailyAnime> getAnimeListByPosition(int position) {
        switch (position) {
            case SUNDAY:
                return weeklySchedule.getSundayAnime();
            case MONDAY:
                return weeklySchedule.getMondayAnime();
            case TUESDAY:
                return weeklySchedule.getTuesdayAnime();
            case WEDNESDAY:
                return weeklySchedule.getWednesdayAnime();
            case THURSDAY:
                return weeklySchedule.getThursdayAnime();
            case FRIDAY:
                return weeklySchedule.getFridayAnime();
            case SATURDAY:
                return weeklySchedule.getSaturdayAnime();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 获取星期图标资源ID
     */
    private int getWeekIconResource(int position) {
        switch (position) {
            case SUNDAY:
                return R.drawable.ic_week_sun;
            case MONDAY:
                return R.drawable.ic_week_mon;
            case TUESDAY:
                return R.drawable.ic_week_tue;
            case WEDNESDAY:
                return R.drawable.ic_week_wed;
            case THURSDAY:
                return R.drawable.ic_week_thu;
            case FRIDAY:
                return R.drawable.ic_week_fri;
            case SATURDAY:
                return R.drawable.ic_week_sat;
            default:
                return 0;
        }
    }

    /**
     * 设置日文星期字符（仅设置星期字符）
     */
    private void setWeekDay(TextView textView, int position) {
        String weekChar = getJapaneseWeekChar(position);
        int weekColor = getWeekColor(position);
        
        // 设置星期字符和颜色
        textView.setText(weekChar);
        textView.setTextColor(weekColor);
        
        // 调试日志，查看颜色设置
        android.util.Log.d("WeeklyScheduleAdapter", 
                "星期" + position + ": 设置颜色 = " + Integer.toHexString(weekColor));
    }
    
    /**
     * 获取日文星期字符
     */
    private String getJapaneseWeekChar(int position) {
        switch (position) {
            case SUNDAY:
                return "日";
            case MONDAY:
                return "月";
            case TUESDAY:
                return "火";
            case WEDNESDAY:
                return "水";
            case THURSDAY:
                return "木";
            case FRIDAY:
                return "金";
            case SATURDAY:
                return "土";
            default:
                return "";
        }
    }
    
    /**
     * 获取星期对应的颜色
     */
    private int getWeekColor(int position) {
        switch (position) {
            case SUNDAY:
                return context.getResources().getColor(R.color.week_sunday, context.getTheme());
            case MONDAY:
                return context.getResources().getColor(R.color.week_monday, context.getTheme());
            case TUESDAY:
                return context.getResources().getColor(R.color.week_tuesday, context.getTheme());
            case WEDNESDAY:
                return context.getResources().getColor(R.color.week_wednesday, context.getTheme());
            case THURSDAY:
                return context.getResources().getColor(R.color.week_thursday, context.getTheme());
            case FRIDAY:
                return context.getResources().getColor(R.color.week_friday, context.getTheme());
            case SATURDAY:
                return context.getResources().getColor(R.color.week_saturday, context.getTheme());
            default:
                return context.getResources().getColor(R.color.week_weekday, context.getTheme());
        }
    }

    /**
     * 更新数据
     */
    public void updateData(WeeklySchedule newSchedule) {
        this.weeklySchedule = newSchedule;
        notifyDataSetChanged();
    }

    /**
     * 设置滚动按钮的点击事件
     */
    private void setupScrollButtons(WeekViewHolder holder) {
        // 左滚动按钮
        holder.btnScrollLeft.setOnClickListener(v -> {
            // 向左滚动一个item宽度
            LinearLayoutManager layoutManager = (LinearLayoutManager) holder.rvDailyAnime.getLayoutManager();
            if (layoutManager != null) {
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                if (firstVisibleItemPosition > 0) {
                    holder.rvDailyAnime.smoothScrollToPosition(firstVisibleItemPosition - 1);
                } else {
                    holder.rvDailyAnime.smoothScrollToPosition(0);
                }
            }
        });
        
        // 右滚动按钮
        holder.btnScrollRight.setOnClickListener(v -> {
            // 向右滚动一个item宽度
            LinearLayoutManager layoutManager = (LinearLayoutManager) holder.rvDailyAnime.getLayoutManager();
            if (layoutManager != null) {
                int itemCount = holder.rvDailyAnime.getAdapter() != null ? 
                                holder.rvDailyAnime.getAdapter().getItemCount() : 0;
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                
                if (lastVisibleItemPosition < itemCount - 1) {
                    holder.rvDailyAnime.smoothScrollToPosition(lastVisibleItemPosition + 1);
                } else {
                    holder.rvDailyAnime.smoothScrollToPosition(itemCount - 1);
                }
            }
        });
    }

    static class WeekViewHolder extends RecyclerView.ViewHolder {
        TextView tvAnimeCount;
        TextView tvWeekDay;
        View llWeekContainer;
        RecyclerView rvDailyAnime;
        ImageButton btnScrollLeft;
        ImageButton btnScrollRight;

        WeekViewHolder(@NonNull View itemView) {
            super(itemView);
            View headerView = itemView.findViewById(R.id.header_week);
            llWeekContainer = headerView;
            tvAnimeCount = headerView.findViewById(R.id.tv_anime_count);
            tvWeekDay = headerView.findViewById(R.id.tv_week_day);
            rvDailyAnime = itemView.findViewById(R.id.rv_daily_anime);
            btnScrollLeft = itemView.findViewById(R.id.btn_scroll_left);
            btnScrollRight = itemView.findViewById(R.id.btn_scroll_right);
        }
    }
} 