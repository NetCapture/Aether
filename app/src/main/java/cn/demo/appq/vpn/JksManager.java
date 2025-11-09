package cn.demo.appq.vpn;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.github.megatronking.netbare.ssl.JKS;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cn.demo.appq.App;

/**
 * JKS证书管理器 - 简化的JKS证书管理
 * 解决JKS初始化失败导致的崩溃问题
 */
public class JksManager {
    private static final String TAG = "JksManager";

    private static JksManager sInstance;
    private final Context mContext;
    private JKS mJks;
    private AtomicBoolean mIsReady = new AtomicBoolean(false);

    private JksManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized JksManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new JksManager(context);
        }
        return sInstance;
    }

    /**
     * 初始化JKS证书
     */
    public void initializeAsync(String alias, char[] password, String commonName,
                               String organization, String organizationalUnitName,
                               String certOrganization, String certOrganizationalUnitName,
                               final JksCallback callback) {

        new Thread(() -> {
            try {
                // 创建新的JKS
                mJks = new JKS(mContext, alias, password, commonName, organization,
                        organizationalUnitName, certOrganization, certOrganizationalUnitName);

                // 同步设置App.mJKS，确保两个管理器使用同一个实例
                App.setJKS(mJks);

                mIsReady.set(true);

                Log.i(TAG, "JKS initialized successfully and synced to App");

                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(mJks));
                }
            } catch (Exception e) {
                Log.e(TAG, "JKS initialization failed", e);
                mIsReady.set(false);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e.getMessage()));
                }
            }
        }, "JksInitializer").start();
    }

    /**
     * 检查是否就绪
     */
    public boolean isReady() {
        // 优先使用App.mJKS，如果已设置则直接返回true
        if (App.getJKS() != null) {
            return true;
        }
        return mIsReady.get() && mJks != null;
    }

    /**
     * 安全获取JKS
     */
    public JKS getJksSafe() {
        // 优先使用App.mJKS
        JKS appJks = App.getJKS();
        if (appJks != null) {
            return appJks;
        }
        if (isReady()) {
            return mJks;
        }
        Log.w(TAG, "JKS not ready, returning null");
        return null;
    }

    /**
     * 获取诊断信息
     */
    public String getDiagnosticInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("JKS状态: ").append(mIsReady.get() ? "就绪" : "未就绪").append("\n");
        sb.append("JKS实例: ").append(mJks != null ? "已创建" : "未创建").append("\n");
        sb.append("Android版本: ").append(Build.VERSION.SDK_INT).append("\n");
        return sb.toString();
    }

    public interface JksCallback {
        void onSuccess(JKS jks);
        void onFailure(String error);
    }
}
