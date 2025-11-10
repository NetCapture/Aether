package cn.demo.appq.presenter;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.github.megatronking.netbare.NetBare;
import com.github.megatronking.netbare.NetBareConfig;
import com.github.megatronking.netbare.NetBareListener;
import com.github.megatronking.netbare.http.HttpInjectInterceptor;
import com.github.megatronking.netbare.http.HttpInterceptorFactory;
import com.github.megatronking.netbare.ssl.JKS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.demo.appq.App;
import cn.demo.appq.Interceptor.NetHttpInject;
import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.greendao.ReqEntityDao;
import cn.demo.appq.utils.DBManager;
import cn.demo.appq.view.NetBareView;

public class NetBarePresenter implements BasePresenter {

    private final NetBareListener listener;
    private final NetBareView view;
    private NetBare mNetBare;
    private final Activity activity;

    public NetBarePresenter(Activity activity, NetBareListener listener, NetBareView view) {
        this.activity = activity;
        this.listener = listener;
        this.view = view;
    }


    private int REQUEST_CODE_PREPARE = 1;

    public void prepareJks() {
        if (!mNetBare.isActive()) {
            // 生成自签证书（不自动安装）
            if (!JKS.isInstalled(activity.getApplicationContext(),
                    App.JSK_ALIAS)) {
                // 创建JKS对象会自动生成证书文件
                new JKS(activity.getApplicationContext(),
                        App.JSK_ALIAS,
                        App.JSK_ALIAS.toCharArray(),
                        "Aether CA",
                        "Aether",
                        "Aether Tool",
                        "Aether",
                        "Aether Tool");
            }
        }
    }

    public boolean isPrepareJks() {
        return JKS.isInstalled(activity.getApplicationContext(),
                App.JSK_ALIAS);
    }

    public boolean isJksFileGenerated() {
        // 检查证书文件是否已生成（.p12和.pem文件）
        File p12File = new File(activity.getApplicationContext().getCacheDir(),
                App.JSK_ALIAS + JKS.KEY_STORE_FILE_EXTENSION);
        File pemFile = new File(activity.getApplicationContext().getCacheDir(),
                App.JSK_ALIAS + JKS.KEY_PEM_FILE_EXTENSION);
        return p12File.exists() && pemFile.exists();
    }

    public void installJks() {
        if (!mNetBare.isActive()) {
            // 安装自签证书到系统
            if (JKS.isInstalled(activity.getApplicationContext(),
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
            // 确保证书JKS已正确初始化
            if (App.getJKS() == null) {
                throw new IllegalStateException("JKS not initialized");
            }

            // 启动NetBare服务
            mNetBare.start(NetBareConfig.defaultHttpConfig(App.getJKS(),
                    interceptorFactories()));
        }
    }

    /**
     * 当前VPN是否已经开启
     *
     * @return
     */
    public boolean isActive() {
        return mNetBare.isActive();
    }

    public void stopVpn() {
        if (mNetBare.isActive()) {
            // 启动NetBare服务
            mNetBare.stop();
        }
    }

    private List<HttpInterceptorFactory> interceptorFactories() {
        List<HttpInterceptorFactory> hfs = new ArrayList<>();
//        hfs.add(Ainterceptor.createFactory(activity.getApplicationContext()));
        hfs.add(HttpInjectInterceptor.createFactory(new NetHttpInject()));
        return hfs;
    }

    @Override
    public void start() {
        mNetBare = NetBare.get();
        // 监听NetBare服务的启动和停止
        mNetBare.registerNetBareListener(listener);
    }

    String query;

    public void resetCurrentQuery() {
        query = null;
    }

    public void queryByUrl(String queryStr) {
        if (!isActive()) {
            return;
        }
        if (queryStr != null) {
            this.query = queryStr;
        }
        List<ReqEntity> data = new ArrayList<>();
        //略过缓存，保证取到的数据是最新的
        DBManager.getReqEntityDao().detachAll();
        if (TextUtils.isEmpty(query)) {
            data.addAll(DBManager.getReqEntityDao()
                    .queryBuilder().orderDesc(ReqEntityDao.Properties.Id).list());
        } else {
            //URL 匹配搜索
            data.addAll(DBManager.getReqEntityDao()
                    .queryBuilder()
                    .where(ReqEntityDao.Properties.Url.like("%" + query + "%"))
                    .orderDesc(ReqEntityDao.Properties.Id)
                    .list());
            //应用名匹配搜索
            data.addAll(DBManager.getReqEntityDao()
                    .queryBuilder()
                    .where(ReqEntityDao.Properties.AppName.like("%" + query + "%"))
                    .orderDesc(ReqEntityDao.Properties.Id)
                    .list());
            //包名匹配搜索
            data.addAll(DBManager.getReqEntityDao()
                    .queryBuilder()
                    .where(ReqEntityDao.Properties.AppPackage.like("%" + query + "%"))
                    .orderDesc(ReqEntityDao.Properties.Id)
                    .list());
        }
        view.onQueryReqlogResult(data);
    }
}
