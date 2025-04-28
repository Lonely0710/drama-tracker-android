package com.lonely.dramatracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.models.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<SearchResult> results = new ArrayList<>();
    private OnItemClickListener listener;
    private OnCollectClickListener collectListener;

    public interface OnItemClickListener {
        void onItemClick(SearchResult result);
    }

    public interface OnCollectClickListener {
        void onCollectClick(SearchResult result, boolean isCollect);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnCollectClickListener(OnCollectClickListener listener) {
        this.collectListener = listener;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        
        // 设置标题
        holder.tvTitleZh.setText(result.getTitleZh());
        holder.tvTitleOriginal.setText(result.getTitleOriginal());
        
        // 设置简介(使用summary字段)
        if (holder.tvDescription != null) {
            String summary = result.getSummary();
            if (summary != null && !summary.isEmpty()) {
                holder.tvDescription.setText(summary);
                holder.tvDescription.setVisibility(View.VISIBLE);
            } else {
                holder.tvDescription.setVisibility(View.GONE);
            }
        }
        
        // 处理年份和日期
        String year = result.getYear();
        String releaseDate = result.getReleaseDate();
        
        // 如果有releaseDate则优先使用,并从中提取年份
        if (releaseDate != null && !releaseDate.isEmpty()) {
            // 尝试从releaseDate中提取年份
            Pattern yearPattern = Pattern.compile("^(\\d{4})");
            Matcher matcher = yearPattern.matcher(releaseDate);
            if (matcher.find()) {
                year = matcher.group(1);
            }
            
            // 设置完整日期(格式化为带括号的形式)
            holder.tvReleaseDate.setText("(" + releaseDate + ")");
            holder.tvReleaseDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvReleaseDate.setVisibility(View.GONE);
        }
        
        // 设置年份(加大显示)
        if (year != null && !year.isEmpty()) {
            holder.tvYear.setText(year);
            holder.tvYear.setVisibility(View.VISIBLE);
        } else {
            holder.tvYear.setVisibility(View.GONE);
        }
        
        // 设置时长
        if (holder.tvDuration != null) {
            String duration = result.getDuration();
            if (duration != null && !duration.isEmpty()) {
                // 根据媒体类型格式化显示时长
                String formattedDuration;
                if ("anime".equals(result.getMediaType()) || "tv".equals(result.getMediaType())) {
                    // 动漫/电视剧，显示为"xx集"
                    formattedDuration = duration + "集";
                } else {
                    // 电影，尝试转换为小时+分钟格式
                    try {
                        int minutes = Integer.parseInt(duration.trim());
                        if (minutes >= 60) {
                            int hours = minutes / 60;
                            int remainingMinutes = minutes % 60;
                            formattedDuration = hours + "时" + (remainingMinutes > 0 ? remainingMinutes + "分" : "");
                        } else {
                            formattedDuration = minutes + "分钟";
                        }
                    } catch (NumberFormatException e) {
                        // 如果无法解析为数字，直接返回原始值
                        formattedDuration = duration;
                    }
                }
                holder.tvDuration.setText(formattedDuration);
                holder.tvDuration.setVisibility(View.VISIBLE);
            } else {
                holder.tvDuration.setVisibility(View.GONE);
            }
        }
        
        // 设置各平台评分
        // 豆瓣评分
        if (result.getRatingDouban() > 0) {
            holder.tvRatingDouban.setText(String.format("%.1f", result.getRatingDouban()));
            holder.layoutRatingDouban.setVisibility(View.VISIBLE);
        } else {
            holder.layoutRatingDouban.setVisibility(View.GONE);
        }
        
        // IMDb评分
        if (result.getRatingImdb() > 0) {
            holder.tvRatingImdb.setText(String.format("%.1f", result.getRatingImdb()));
            holder.layoutRatingImdb.setVisibility(View.VISIBLE);
        } else {
            holder.layoutRatingImdb.setVisibility(View.GONE);
        }
        
        // Bangumi评分
        if (result.getRatingBangumi() > 0) {
            holder.tvRatingBangumi.setText(String.format("%.1f", result.getRatingBangumi()));
            holder.layoutRatingBangumi.setVisibility(View.VISIBLE);
        } else {
            holder.layoutRatingBangumi.setVisibility(View.GONE);
        }
        
        // 设置制作人员
        if (holder.tvStaff != null && holder.layoutStaff != null) {
            String staff = result.getStaff();
            if (staff != null && !staff.isEmpty()) {
                holder.tvStaff.setText(staff);
                holder.layoutStaff.setVisibility(View.VISIBLE);
            } else {
                holder.layoutStaff.setVisibility(View.GONE);
            }
        }
        
        // 设置收藏状态
        holder.btnCollect.setImageResource(result.isCollected() 
                ? R.drawable.ic_favorite 
                : R.drawable.ic_favorite_border);
        
        // 加载海报图片
        if (result.getPosterUrl() != null && !result.getPosterUrl().isEmpty()) {
            Glide.with(holder.ivPoster)
                    .load(result.getPosterUrl())
                    .placeholder(R.drawable.placeholder_poster)
                    .error(R.drawable.placeholder_poster)
                    .into(holder.ivPoster);
        } else {
            holder.ivPoster.setImageResource(R.drawable.placeholder_poster);
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(result);
            }
        });

        holder.btnCollect.setOnClickListener(v -> {
            if (collectListener != null) {
                collectListener.onCollectClick(result, !result.isCollected());
                // 不直接setCollected和notifyItemChanged，由外部刷新数据
            }
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitleZh;
        TextView tvTitleOriginal;
        TextView tvDescription;
        TextView tvYear;
        TextView tvReleaseDate;
        TextView tvDuration;
        
        // 豆瓣评分
        LinearLayout layoutRatingDouban;
        TextView tvRatingDouban;
        
        // IMDb评分
        LinearLayout layoutRatingImdb;
        TextView tvRatingImdb;
        
        // Bangumi评分
        LinearLayout layoutRatingBangumi;
        TextView tvRatingBangumi;
        
        // 制作人员
        LinearLayout layoutStaff;
        TextView tvStaff;
        
        ImageButton btnCollect;

        ViewHolder(View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvTitleZh = itemView.findViewById(R.id.tv_title_zh);
            tvTitleOriginal = itemView.findViewById(R.id.tv_title_original);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvYear = itemView.findViewById(R.id.tv_year);
            tvReleaseDate = itemView.findViewById(R.id.tv_release_date);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            
            // 评分相关
            layoutRatingDouban = itemView.findViewById(R.id.layout_rating_douban);
            tvRatingDouban = itemView.findViewById(R.id.tv_rating_douban);
            
            layoutRatingImdb = itemView.findViewById(R.id.layout_rating_imdb);
            tvRatingImdb = itemView.findViewById(R.id.tv_rating_imdb);
            
            layoutRatingBangumi = itemView.findViewById(R.id.layout_rating_bangumi);
            tvRatingBangumi = itemView.findViewById(R.id.tv_rating_bangumi);
            
            // 制作人员
            layoutStaff = itemView.findViewById(R.id.layout_staff);
            tvStaff = itemView.findViewById(R.id.tv_staff);
            
            btnCollect = itemView.findViewById(R.id.btn_collect);
        }
    }
} 