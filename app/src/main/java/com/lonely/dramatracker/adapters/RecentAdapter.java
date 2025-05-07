package com.lonely.dramatracker.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.models.MovieInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 近期上映页面适配器
 */
public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.ViewHolder> {
    private static final String TAG = "RecentAdapter";
    
    // 上下文
    private Context mContext;
    
    // 数据
    private List<MovieInfo> mItems = new ArrayList<>();
    
    // 点击监听
    private OnItemClickListener mOnItemClickListener;
    
    /**
     * 构造函数
     */
    public RecentAdapter(Context context) {
        this.mContext = context;
    }
    
    /**
     * 设置点击监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }
    
    /**
     * 设置数据
     */
    public void setItems(List<MovieInfo> items) {
        this.mItems.clear();
        if (items != null) {
            this.mItems.addAll(items);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_recommend, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieInfo item = mItems.get(position);
        
        // 设置基本信息
        if (item.getMovieName() != null) {
            holder.tvTitle.setText(item.getMovieName());
        }
        
        // 设置原始标题
        if (!TextUtils.isEmpty(item.getOriginalName())) {
            holder.tvOriginalTitle.setText(item.getOriginalName());
            holder.tvOriginalTitle.setVisibility(View.VISIBLE);
        } else {
            holder.tvOriginalTitle.setVisibility(View.GONE);
        }
        
        // 设置评分
        if (item.getScore() > 0) {
            holder.tvRating.setText(String.format("%.1f", item.getScore()));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }
        
        // 设置上映日期
        if (!TextUtils.isEmpty(item.getReleaseDate())) {
            holder.tvReleaseDate.setText(item.getReleaseDate());
            holder.tvReleaseDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvReleaseDate.setVisibility(View.GONE);
        }
        
        // 设置演员
        if (!TextUtils.isEmpty(item.getFormattedActors())) {
            holder.tvCast.setText(item.getFormattedActors());
            holder.tvCast.setVisibility(View.VISIBLE);
        } else {
            holder.tvCast.setVisibility(View.GONE);
        }
        
        // 设置简介
        if (!TextUtils.isEmpty(item.getSummary())) {
            holder.tvOverview.setText(item.getSummary());
            holder.tvOverview.setVisibility(View.VISIBLE);
        } else {
            holder.tvOverview.setText(R.string.no_summary);
            holder.tvOverview.setVisibility(View.VISIBLE);
        }
        
        // 设置类型
        if (item.getGenres() != null && !item.getGenres().isEmpty()) {
            holder.tvGenres.setText(item.getFormattedGenres());
            holder.tvGenres.setVisibility(View.VISIBLE);
        } else {
            holder.tvGenres.setVisibility(View.GONE);
        }
        
        // 加载海报
        loadPoster(holder.ivPoster, item.getPoster());
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(holder.getAdapterPosition(), item);
            }
        });
    }
    
    /**
     * 加载海报
     */
    private void loadPoster(ImageView imageView, String posterUrl) {
        if (posterUrl != null && !posterUrl.isEmpty()) {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error);
            
            Glide.with(mContext)
                    .load(posterUrl)
                    .apply(options)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
        }
    }
    
    @Override
    public int getItemCount() {
        return mItems.size();
    }
    
    /**
     * ViewHolder
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle;
        TextView tvOriginalTitle;
        TextView tvRating;
        TextView tvReleaseDate;
        TextView tvGenres;
        TextView tvCast;
        TextView tvOverview;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvOriginalTitle = itemView.findViewById(R.id.tv_original_title);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvReleaseDate = itemView.findViewById(R.id.tv_release_date);
            tvGenres = itemView.findViewById(R.id.tv_genres);
            tvCast = itemView.findViewById(R.id.tv_cast);
            tvOverview = itemView.findViewById(R.id.tv_overview);
        }
    }
    
    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(int position, MovieInfo item);
    }
} 