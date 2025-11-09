package cn.demo.appq.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.TimeUtils;

import java.util.Date;

import cn.demo.appq.R;
import cn.demo.appq.activity.DescriptionActivity;
import cn.demo.appq.entity.ReqEntity;

public class UrlViewHolder extends RecyclerView.ViewHolder {
    private final ImageView iv_icon;
    private final TextView tv_status;
    private final TextView tv_url;

    public UrlViewHolder(@NonNull View itemView) {
        super(itemView);
        iv_icon = itemView.findViewById(R.id.iv_icon);
        tv_status = itemView.findViewById(R.id.tv_status);
        tv_url = itemView.findViewById(R.id.tv_url);
    }

    public void setData(final ReqEntity data) {
        if (data == null) {
            return;
        }
        Drawable drawable = AppUtils.getAppIcon(data.getAppPackage());
        if (drawable != null) {
            iv_icon.setImageDrawable(drawable);
        }
        tv_status.setText(data.getMethod() + " Status:" + data.getRespCode() +
                " Size:" + String.format("%.2f", (data.getLength() / 1024f)) + "KB" +
                " Time:" + TimeUtils.date2String(new Date(data.getTime()), "yyyy-MM-dd HH:mm:ss"));
        tv_url.setText(data.getUrl());

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle options = new Bundle();
                options.putLong("id", data.getId());
                ActivityUtils.startActivity(options, DescriptionActivity.class);
            }
        });
        if (data.getRespCode() != null) {
            if (data.getRespCode() == 200) {
                itemView.setBackgroundColor(Color.parseColor("#99FF99"));
            } else {
                itemView.setBackgroundColor(Color.parseColor("#FF9999"));
            }
        } else {
            itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
    }
}
