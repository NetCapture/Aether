package cn.demo.appq;

import android.app.Application;
import android.content.Context;

import com.github.megatronking.netbare.NetBare;
import com.github.megatronking.netbare.NetBareUtils;
import com.github.megatronking.netbare.ssl.JKS;
import cn.demo.appq.utils.MinRefPlanA;

//import me.weishu.reflection.Reflection;

public class App extends Application {

    public static final String JSK_ALIAS = "MyVPNSample";


    private static JKS mJKS = null;

    private static Context mContext;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
         long result = MinRefPlanA.unseal(App.this);
        //   Log.i("sanbo","MinRefPlanA.unseal result: " + result);
        // 创建自签证书
        mJKS = new JKS(this, JSK_ALIAS, JSK_ALIAS.toCharArray(), JSK_ALIAS, JSK_ALIAS,
                JSK_ALIAS, JSK_ALIAS, JSK_ALIAS);
        // 初始化NetBare
        NetBare.get().attachApplication(this, true);
    }

    public static JKS getJKS() {
        return mJKS;
    }

    /**
     * 设置JKS实例 - 供JksManager使用以确保两个管理器使用同一个实例
     */
    public static void setJKS(JKS jks) {
        mJKS = jks;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // On android Q, we can't access Java8EngineWrapper with reflect.
        if (NetBareUtils.isAndroidQ()) {
            //Reflection.unseal(base);
        }
    }

    public static String getProcessNameByUid(int uid) {
        return mContext.getPackageManager().getNameForUid(uid);
    }

}
