package cn.demo.appq;

import android.app.Activity;
import android.content.Intent;

import com.github.megatronking.netbare.NetBare;
import com.github.megatronking.netbare.NetBareConfig;
import com.github.megatronking.netbare.NetBareListener;
import com.github.megatronking.netbare.http.HttpInterceptorFactory;
import com.github.megatronking.netbare.ssl.JKS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NetBarePresenter implements BasePresenter {

    private final NetBareListener listener;
    private NetBare mNetBare;


    private final Activity activity;

    public NetBarePresenter(Activity activity, NetBareListener listener) {
        this.activity = activity;
        this.listener = listener;
    }


    private int REQUEST_CODE_PREPARE = 1;

    public void prepareJks() {
        if (!mNetBare.isActive()) {
            // 安装自签证书
            if (!JKS.isInstalled(activity.getApplicationContext(),
                    App.JSK_ALIAS)) {
                try {
                    JKS.install(activity.getApplicationContext(),
                            App.JSK_ALIAS, App.JSK_ALIAS);
                } catch (IOException e) {
                    // 安装失败
                }
            }
        }
    }

    public void prepareVpn() {
        if (!mNetBare.isActive()) {
            // 配置VPN
            Intent intent = NetBare.get().prepare();
            if (intent != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_PREPARE);
            }
        }
    }

    public void startVpn() {
        if (!mNetBare.isActive()) {
            // 启动NetBare服务
            mNetBare.start(NetBareConfig.defaultHttpConfig(App.getJKS(),
                    interceptorFactories()));
        }
    }

    public void stopVpn() {
        if (mNetBare.isActive()) {
            // 启动NetBare服务
            mNetBare.stop();
        }
    }

    private List<HttpInterceptorFactory> interceptorFactories() {
        List<HttpInterceptorFactory> hfs = new ArrayList<>();
        hfs.add(Ainterceptor.createFactory(activity.getApplicationContext()));
        return hfs;
    }

    @Override
    public void start() {
        mNetBare = NetBare.get();
        // 监听NetBare服务的启动和停止
        mNetBare.registerNetBareListener(listener);
    }
}
