package cn.demo.appq.vpn;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

import com.github.megatronking.netbare.NetBare;
import com.github.megatronking.netbare.NetBareConfig;
import com.github.megatronking.netbare.http.HttpInjectInterceptor;
import com.github.megatronking.netbare.http.HttpInterceptorFactory;
import com.github.megatronking.netbare.ssl.JKS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.demo.appq.Interceptor.NetHttpInject;

/**
 * VPN服务管理器 - 简化的VPN服务管理
 */
public class VpnServiceManager {
    private static final String TAG = "VpnServiceManager";

    private static VpnServiceManager sInstance;
    private final Context mContext;
    private final NetBare mNetBare;
    private final AtomicBoolean mIsVpnActive = new AtomicBoolean(false);
    private VpnStateListener mListener;

    public enum VpnState {
        IDLE,
        PREPARING,
        ACTIVE,
        STOPPING,
        ERROR
    }

    private VpnState mCurrentState = VpnState.IDLE;

    public interface VpnStateListener {
        void onVpnStateChanged(VpnState state);
        void onVpnStarted();
        void onVpnStopped();
        void onVpnError(String error);
    }

    private VpnServiceManager(Context context) {
        mContext = context.getApplicationContext();
        mNetBare = NetBare.get();
    }

    public static synchronized VpnServiceManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new VpnServiceManager(context);
        }
        return sInstance;
    }

    /**
     * 设置状态监听器
     */
    public void setVpnStateListener(VpnStateListener listener) {
        mListener = listener;
    }

    /**
     * 准备VPN权限
     */
    public void prepareVpnPermission(android.app.Activity activity) {
        try {
            Intent intent = mNetBare.prepare();
            if (intent != null) {
                mCurrentState = VpnState.PREPARING;
                notifyStateChanged();
                activity.startActivityForResult(intent, 1);
            } else {
                // NetBare.prepare()返回null表示权限已存在，直接启动VPN
                Log.i(TAG, "VPN permission already granted, starting VPN...");
                // 需要在UI线程上执行
                new android.os.Handler(activity.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        startVpn();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing VPN", e);
            mCurrentState = VpnState.ERROR;
            notifyStateChanged();
            if (mListener != null) {
                mListener.onVpnError(e.getMessage());
            }
        }
    }

    /**
     * 处理权限结果
     */
    public void handlePermissionResult(int resultCode) {
        if (resultCode == android.app.Activity.RESULT_OK) {
            Log.i(TAG, "VPN permission granted");
        } else {
            Log.w(TAG, "VPN permission denied");
            mCurrentState = VpnState.ERROR;
            notifyStateChanged();
            if (mListener != null) {
                mListener.onVpnError("VPN permission denied");
            }
        }
    }

    /**
     * 启动VPN
     */
    public void startVpn() {
        try {
            if (mIsVpnActive.get()) {
                Log.w(TAG, "VPN already active");
                return;
            }

            // 获取JKS实例
            JksManager jksManager = JksManager.getInstance(mContext);
            JKS jks = jksManager.getJksSafe();

            if (jks == null) {
                String error = "JKS not ready - " + jksManager.getDiagnosticInfo();
                Log.e(TAG, error);
                mCurrentState = VpnState.ERROR;
                notifyStateChanged();
                if (mListener != null) {
                    mListener.onVpnError(error);
                }
                return;
            }

            Log.i(TAG, "Starting VPN with JKS...");

            // 创建默认配置
            NetBareConfig config = NetBareConfig.defaultHttpConfig(jks, interceptorFactories());
            if (config == null) {
                throw new IllegalStateException("Failed to create NetBareConfig");
            }

            Log.i(TAG, "NetBareConfig created successfully, starting service...");

            // 启动NetBare服务
            mNetBare.start(config);

            mIsVpnActive.set(true);
            mCurrentState = VpnState.ACTIVE;
            notifyStateChanged();

            Log.i(TAG, "VPN started successfully");
            if (mListener != null) {
                mListener.onVpnStarted();
            }

        } catch (IllegalStateException e) {
            Log.e(TAG, "Illegal state error: " + e.getMessage(), e);
            mIsVpnActive.set(false);
            mCurrentState = VpnState.ERROR;
            notifyStateChanged();
            if (mListener != null) {
                mListener.onVpnError("配置错误: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start VPN: " + e.getMessage(), e);
            mIsVpnActive.set(false);
            mCurrentState = VpnState.ERROR;
            notifyStateChanged();
            if (mListener != null) {
                mListener.onVpnError(e.getMessage());
            }
        }
    }

    /**
     * 停止VPN
     */
    public void stopVpn() {
        try {
            if (!mIsVpnActive.get()) {
                Log.w(TAG, "VPN not active");
                return;
            }

            mCurrentState = VpnState.STOPPING;
            notifyStateChanged();

            mNetBare.stop();

            mIsVpnActive.set(false);
            mCurrentState = VpnState.IDLE;
            notifyStateChanged();

            Log.i(TAG, "VPN stopped");
            if (mListener != null) {
                mListener.onVpnStopped();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop VPN", e);
            mCurrentState = VpnState.ERROR;
            notifyStateChanged();
            if (mListener != null) {
                mListener.onVpnError(e.getMessage());
            }
        }
    }

    /**
     * 检查VPN是否激活
     */
    public boolean isVpnActive() {
        return mIsVpnActive.get();
    }

    /**
     * 获取当前状态
     */
    public VpnState getCurrentState() {
        return mCurrentState;
    }

    private void notifyStateChanged() {
        if (mListener != null) {
            mListener.onVpnStateChanged(mCurrentState);
        }
    }

    public String getDiagnosticInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("VPN状态: ").append(mCurrentState).append("\n");
        sb.append("VPN激活: ").append(mIsVpnActive.get()).append("\n");
        return sb.toString();
    }

    private List<HttpInterceptorFactory> interceptorFactories() {
        List<HttpInterceptorFactory> hfs = new ArrayList<>();
        hfs.add(HttpInjectInterceptor.createFactory(new NetHttpInject()));
        return hfs;
    }
}
