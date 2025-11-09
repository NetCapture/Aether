package cn.demo.appq.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import cn.demo.appq.R;
import cn.demo.appq.entity.ReqEntity;

public class UrlAdapter extends ListAdapter<ReqEntity, UrlViewHolder> {

    public UrlAdapter() {
        this(new DiffUtil.ItemCallback<ReqEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull ReqEntity reqEntity, @NonNull ReqEntity t1) {
                return reqEntity.getId().equals(t1.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ReqEntity reqEntity, @NonNull ReqEntity t1) {
                if (reqEntity.getId() != null && !reqEntity.getId().equals(t1.getId())) {
                    return false;
                }
                if (reqEntity.getTime() != null && !reqEntity.getTime().equals(t1.getTime())) {
                    return false;
                }
                if (reqEntity.getLength() != null && !reqEntity.getLength().equals(t1.getLength())) {
                    return false;
                }
                if (reqEntity.getRespCode() != null && !reqEntity.getRespCode().equals(t1.getRespCode())) {
                    return false;
                }
                return true;
            }
        });
    }

    public UrlAdapter(@NonNull DiffUtil.ItemCallback<ReqEntity> diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public UrlViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_url_logs,
                        null, false);
        return new UrlViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UrlViewHolder urlViewHolder, int i) {
        urlViewHolder.setData(getItem(i));
    }
}
