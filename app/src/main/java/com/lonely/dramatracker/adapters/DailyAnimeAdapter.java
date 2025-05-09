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
import com.lonely.dramatracker.models.DailyAnime;

import java.util.List;

/**
 * 每日放送动漫横向列表适配器
 */
public class DailyAnimeAdapter extends RecyclerView.Adapter<DailyAnimeAdapter.AnimeViewHolder> {

    private List<DailyAnime> animeList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DailyAnime anime);
    }

    public DailyAnimeAdapter(Context context, List<DailyAnime> animeList) {
        this.context = context;
        this.animeList = animeList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AnimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_anime, parent, false);
        return new AnimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimeViewHolder holder, int position) {
        DailyAnime anime = animeList.get(position);
        
        // 设置标题
        holder.tvTitleZh.setText(anime.getTitleZh());
        
        // 设置原标题（如果有）
        if (anime.getTitleOriginal() != null && !anime.getTitleOriginal().isEmpty()) {
            holder.tvTitleOriginal.setVisibility(View.VISIBLE);
            holder.tvTitleOriginal.setText(anime.getTitleOriginal());
        } else {
            holder.tvTitleOriginal.setVisibility(View.GONE);
        }
        
        // 加载海报
        if (anime.getPosterUrl() != null && !anime.getPosterUrl().isEmpty()) {
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.placeholder_poster)
                    .error(R.drawable.placeholder_poster)
                    .transform(new CenterCrop());
                    
            Glide.with(context)
                    .load(anime.getPosterUrl())
                    .apply(requestOptions)
                    .into(holder.ivPoster);
        } else {
            holder.ivPoster.setImageResource(R.drawable.placeholder_poster);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(anime);
            }
        });
    }

    @Override
    public int getItemCount() {
        return animeList != null ? animeList.size() : 0;
    }

    public void updateData(List<DailyAnime> newAnimeList) {
        this.animeList = newAnimeList;
        notifyDataSetChanged();
    }

    static class AnimeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitleZh;
        TextView tvTitleOriginal;

        AnimeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_anime_poster);
            tvTitleZh = itemView.findViewById(R.id.tv_anime_title_zh);
            tvTitleOriginal = itemView.findViewById(R.id.tv_anime_title_original);
        }
    }
} 