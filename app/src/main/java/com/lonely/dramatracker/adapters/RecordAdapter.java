package com.lonely.dramatracker.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.models.RecordItem;
import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
    private static final String TAG = "RecordAdapter";
    private List<RecordItem> items = new ArrayList<>();
    private boolean isGridMode = true;

    public RecordAdapter(boolean isGridMode) {
        this.isGridMode = isGridMode;
    }

    public void setGridMode(boolean isGridMode) {
        this.isGridMode = isGridMode;
    }

    public void setItems(List<RecordItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGridMode ? R.layout.item_record : R.layout.item_record_list;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(view, isGridMode);
    }

    @Override
    public int getItemViewType(int position) {
        // 返回不同的视图类型，确保在布局模式改变时强制重建ViewHolder
        return isGridMode ? 1 : 2;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecordItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "RecordAdapter";
        ImageView ivPoster;
        TextView tvTitle;
        TextView tvSubtitle;
        // 列表模式下的额外字段
        TextView tvRating, tvType, tvYear, tvDuration;
        boolean isGridMode;

        ViewHolder(View itemView, boolean isGridMode) {
            super(itemView);
            this.isGridMode = isGridMode;
            ivPoster = itemView.findViewById(R.id.iv_poster);
            
            if (isGridMode) {
                // 网格模式(item_record.xml)
                tvTitle = itemView.findViewById(R.id.tv_title_original);
                tvRating = itemView.findViewById(R.id.tv_rating);
                tvYear = itemView.findViewById(R.id.tv_year);
            } else {
                // 列表模式(item_record_list.xml)
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvRating = itemView.findViewById(R.id.tv_rating);
                tvType = itemView.findViewById(R.id.tv_type);
                tvYear = itemView.findViewById(R.id.tv_year);
                tvDuration = itemView.findViewById(R.id.tv_duration);
            }
        }

        void bind(RecordItem item) {
            // 设置标题 - 根据不同媒体类型选择标题
            if (tvTitle != null) {
                String title;
                if (isGridMode) {
                    // 网格模式：anime类型使用中文标题，其他类型使用原标题
                    if ("anime".equals(item.getMediaType())) {
                        title = item.getTitle(); // 动漫使用中文标题
                    } else {
                        title = item.getSubtitle(); // 其他类型使用原标题
                    }
                } else {
                    // 列表模式始终使用中文标题
                    title = item.getTitle();
                }
                tvTitle.setText(title != null ? title : "");
            }
            
            // 使用Glide加载海报图片
            if (ivPoster != null) {
                Glide.with(itemView.getContext())
                        .load(item.getPosterUrl())
                        .placeholder(R.drawable.placeholder_poster)
                        .error(R.drawable.placeholder_poster)
                        .into(ivPoster);
            }
            
            // 设置评分
            if (tvRating != null && item.getRating() != null) {
                tvRating.setText(item.getRating());
            }
            
            // 设置年份/日期 - 根据视图类型选择不同格式
            if (tvYear != null && item.getYear() != null) {
                String dateText;
                if (isGridMode) {
                    // 网格视图只显示年份
                    if (item.getYear().length() >= 4) {
                        dateText = item.getYear().substring(0, 4);
                    } else {
                        dateText = item.getYear();
                    }
                } else {
                    // 列表视图显示完整日期
                    dateText = item.getYear();
                }
                tvYear.setText(dateText);
            }
            
            // 在列表模式下设置额外信息
            if (!isGridMode) {
                // 设置媒体类型
                if (tvType != null && item.getMediaType() != null) {
                    String displayType = "";
                    switch (item.getMediaType()) {
                        case "movie": displayType = "电影"; break;
                        case "tv": displayType = "电视剧"; break;
                        case "anime": displayType = "动漫"; break;
                        default: displayType = item.getMediaType();
                    }
                    tvType.setText(displayType);
                }
                
                // 设置时长
                if (tvDuration != null) {
                    String duration = item.getDuration();
                    if (duration != null && !duration.isEmpty()) {
                        // 格式化时长显示
                        String formattedDuration = formatDuration(duration, item.getMediaType());
                        tvDuration.setText(formattedDuration);
                    } else {
                        tvDuration.setText("");
                    }
                }
            }
        }
        
        /**
         * 格式化时长显示
         * @param duration 原始时长数据
         * @param mediaType 媒体类型
         * @return 格式化后的时长文本
         */
        private String formatDuration(String duration, String mediaType) {
            if (duration == null || duration.isEmpty()) {
                return "";
            }
            
            try {
                // 根据媒体类型选择格式
                if ("anime".equals(mediaType) || "tv".equals(mediaType)) {
                    // 动漫/电视剧，显示为"xx集"
                    return duration + "集";
                } else {
                    // 电影，尝试转换为小时+分钟格式
                    int minutes = Integer.parseInt(duration.trim());
                    if (minutes >= 60) {
                        int hours = minutes / 60;
                        int remainingMinutes = minutes % 60;
                        return hours + "时" + (remainingMinutes > 0 ? remainingMinutes + "分" : "");
                    } else {
                        return minutes + "分钟";
                    }
                }
            } catch (NumberFormatException e) {
                // 如果无法解析为数字，直接返回原始值
                return duration;
            }
        }
    }
} 