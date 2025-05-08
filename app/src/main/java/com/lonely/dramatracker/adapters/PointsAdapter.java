package com.lonely.dramatracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.lonely.dramatracker.R;
import com.lonely.dramatracker.fragments.PointsTabFragment;
import com.lonely.dramatracker.models.MediaInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 高分推荐页面适配器
 */
public class PointsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "PointsAdapter";
    
    // 视图类型
    private static final int VIEW_TYPE_ALL = 1;
    private static final int VIEW_TYPE_NORMAL = 2;
    
    // 上下文
    private Context mContext;
    
    // 数据
    private List<MediaInfo> mItems = new ArrayList<>();
    
    // 当前类型(全部/电影/电视剧)
    private int mCurrentType = PointsTabFragment.TYPE_ALL;
    
    // 当前页码和每页条数（用于正确计算排名）
    private int mCurrentPage = 1;
    private int mPageSize = 21;
    
    // 点击监听
    private OnItemClickListener mOnItemClickListener;
    
    /**
     * 构造函数
     */
    public PointsAdapter(Context context) {
        this.mContext = context;
    }
    
    /**
     * 设置当前页码和页面大小（用于正确计算排名）
     */
    public void setPageInfo(int page, int pageSize) {
        this.mCurrentPage = page;
        this.mPageSize = pageSize;
        notifyDataSetChanged();
    }
    
    /**
     * 设置点击监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }
    
    /**
     * 设置当前类型
     */
    public void setCurrentType(int type) {
        this.mCurrentType = type;
        notifyDataSetChanged();
    }
    
    /**
     * 设置数据
     */
    public void setItems(List<MediaInfo> items) {
        this.mItems.clear();
        if (items != null) {
            this.mItems.addAll(items);
        }
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        return mCurrentType == PointsTabFragment.TYPE_ALL ? VIEW_TYPE_ALL : VIEW_TYPE_NORMAL;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ALL) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_points_all, parent, false);
            return new AllViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_points, parent, false);
            return new NormalViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MediaInfo item = mItems.get(position);
        
        if (holder instanceof AllViewHolder) {
            setupAllViewHolder((AllViewHolder) holder, item, position);
        } else if (holder instanceof NormalViewHolder) {
            setupNormalViewHolder((NormalViewHolder) holder, item, position);
        }
    }
    
    /**
     * 设置全部分类视图
     */
    private void setupAllViewHolder(AllViewHolder holder, MediaInfo item, int position) {
        // 设置基本信息
        holder.tvTitle.setText(item.getMediaName());
        
        // 设置评分
        if (item.getRating() > 0) {
            holder.tvRating.setText(String.format("%.1f", item.getRating()));
        } else {
            holder.tvRating.setText("--");
        }
        
        // 设置年份
        holder.tvYear.setText(item.getReleaseDate());
        
        // 设置媒体类型
        if (item.getMediaType().equals(MediaInfo.TYPE_MOVIE)) {
            holder.tvMediaType.setText(R.string.top_rated_movie_type);
        } else {
            holder.tvMediaType.setText(R.string.top_rated_tv_type);
        }
        
        // 全部分类下的项目也显示排名
        setupAllRankDisplay(holder, position);
        
        // 加载海报
        loadPoster(holder.ivPoster, item.getPosterUrl());
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(position, item);
            }
        });
    }
    
    /**
     * 设置"全部"分类下的排名显示
     */
    private void setupAllRankDisplay(AllViewHolder holder, int position) {
        // 计算排名（从1开始）
        int rank = position + 1;
        
        // 为1-3名提供特殊样式
        if (rank <= 3) {
            // 根据排名设置特殊背景或颜色
            switch (rank) {
                case 1:
                    // 金色背景
                    holder.tvRating.setBackgroundResource(R.drawable.bg_rating_gold);
                    break;
                case 2:
                    // 银色背景
                    holder.tvRating.setBackgroundResource(R.drawable.bg_rating_silver);
                    break;
                case 3:
                    // 铜色背景
                    holder.tvRating.setBackgroundResource(R.drawable.bg_rating_bronze);
                    break;
            }
        } else {
            // 4及以后使用普通样式
            holder.tvRating.setBackgroundResource(R.drawable.bg_rating_normal);
        }
    }
    
    /**
     * 设置电影/电视剧分类视图
     */
    private void setupNormalViewHolder(NormalViewHolder holder, MediaInfo item, int position) {
        // 设置基本信息
        holder.tvTitle.setText(item.getMediaName());
        
        // 设置评分
        if (item.getRating() > 0) {
            holder.tvRating.setText(String.format("%.1f", item.getRating()));
        } else {
            holder.tvRating.setText("--");
        }
        
        // 设置年份
        holder.tvYear.setText(item.getReleaseDate());
        
        // 设置排名
        setupRankDisplay(holder, position);
        
        // 加载海报
        loadPoster(holder.ivPoster, item.getPosterUrl());
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(position, item);
            }
        });
    }
    
    /**
     * 设置排名显示
     */
    private void setupRankDisplay(NormalViewHolder holder, int position) {
        // 计算全局排名
        int rank = position + 1;
        
        if (rank <= 3) {
            // 前三名使用特殊图标
            holder.ivRankSpecial.setVisibility(View.VISIBLE);
            holder.tvRank.setVisibility(View.GONE);
            
            // 设置对应的特殊图标
            switch (rank) {
                case 1:
                    holder.ivRankSpecial.setImageResource(R.drawable.ic_rk1);
                    // 设置第一名的其他特殊样式
                    holder.tvRating.setBackgroundResource(R.drawable.bg_rating_gold);
                    break;
                case 2:
                    holder.ivRankSpecial.setImageResource(R.drawable.ic_rk2);
                    // 设置第二名的其他特殊样式
                    holder.tvRating.setBackgroundResource(R.drawable.bg_rating_silver);
                    break;
                case 3:
                    holder.ivRankSpecial.setImageResource(R.drawable.ic_rk3);
                    // 设置第三名的其他特殊样式
                    holder.tvRating.setBackgroundResource(R.drawable.bg_rating_bronze);
                    break;
            }
        } else {
            // 其他排名使用普通数字显示
            holder.ivRankSpecial.setVisibility(View.GONE);
            holder.tvRank.setVisibility(View.VISIBLE);
            holder.tvRank.setText(String.valueOf(rank));
            holder.tvRating.setBackgroundResource(R.drawable.bg_rating_normal);
        }
    }
    
    /**
     * 加载海报
     */
    private void loadPoster(ImageView imageView, String posterUrl) {
        if (posterUrl != null && !posterUrl.isEmpty()) {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .transform(new CenterCrop(), new RoundedCorners(8));
            
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
     * 全部分类ViewHolder
     */
    static class AllViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvRating;
        TextView tvTitle;
        TextView tvMediaType;
        TextView tvYear;
        
        public AllViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMediaType = itemView.findViewById(R.id.tv_media_type);
            tvYear = itemView.findViewById(R.id.tv_year);
        }
    }
    
    /**
     * 电影/电视剧分类ViewHolder
     */
    static class NormalViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        ImageView ivRankSpecial;
        TextView tvRank;
        TextView tvRating;
        TextView tvTitle;
        TextView tvYear;
        
        public NormalViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            ivRankSpecial = itemView.findViewById(R.id.iv_rank_special);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvYear = itemView.findViewById(R.id.tv_year);
        }
    }
    
    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(int position, MediaInfo item);
    }
} 