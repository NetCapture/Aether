package cn.demo.appq.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.github.megatronking.netbare.NetBare;
import com.github.megatronking.netbare.NetBareListener;
import com.github.megatronking.netbare.NetBareService;

import cn.demo.appq.R;
import cn.demo.appq.activity.VPNActivity;
import cn.demo.appq.utils.TrafficHttpServer;

public class AppService extends NetBareService implements NetBareListener {

    private TrafficHttpServer mHttpServer;


    private String CHANNEL_ID = "cn.demo.appq.NOTIFICATION_CHANNEL_ID";


    @Override
    public void onCreate() {
        super.onCreate();

        // 注册NetBare监听器
        NetBare.get().registerNetBareListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(
                        new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW));
            }
        }
    }

    @Override
    protected int notificationId() {
        return 200;
    }

    @Override
    protected Notification createNotification() {

        Intent intent = new Intent(this, VPNActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setAction(Intent.ACTION_MAIN);

        // Android 31+ requires FLAG_IMMUTABLE or FLAG_MUTABLE
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, flags);


        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.netbare_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_name))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        // 取消注册监听器
        NetBare.get().unregisterNetBareListener(this);

        // 停止HTTP服务器
        if (mHttpServer != null) {
            mHttpServer.stop();
            mHttpServer = null;
        }

        super.onDestroy();
    }

    @Override
    public void onServiceStarted() {
        // 启动HTTP服务器
        mHttpServer = TrafficHttpServer.getInstance();
        mHttpServer.start();
    }

    @Override
    public void onServiceStopped() {
        // 停止HTTP服务器
        if (mHttpServer != null) {
            mHttpServer.stop();
            mHttpServer = null;
        }
    }

}
