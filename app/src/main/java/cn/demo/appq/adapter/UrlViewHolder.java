package cn.demo.appq.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.TimeUtils;

import java.util.Date;

import cn.demo.appq.R;
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

    public void setData(ReqEntity data) {
        if (data == null) {
            return;
        }
        iv_icon.setImageDrawable(AppUtils.getAppIcon(data.getAppPackage()));
        tv_status.setText(new StringBuilder()
                .append("Status:").append(data.getRespCode())
                .append(" Size:").append(data.getLength())
                .append(" Time:").append(TimeUtils.date2String(new Date(data.getTime()), "yyyy-MM-dd HH:mm:ss")).toString());
        tv_url.setText(data.getUrl());
    }
}
