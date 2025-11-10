package cn.demo.appq.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import cn.demo.appq.App;

/**
 * 通过UID获取应用名称的工具类
 */
public class AppNameResolver {
    private static final String TAG = "AppNameResolver";
    
    /**
     * 通过UID获取应用名称
     * @param uid 应用UID
     * @return 应用名称，如果获取失败则返回null
     */
    public static String getAppNameByUid(int uid) {
        if (uid <= 0) return null;
        
        try {
            String packageName = getPackageNameByUid(uid);
            if (!TextUtils.isEmpty(packageName)) {
                // 使用NetBare.get().getApplication()获取Context
                android.content.Context context = com.github.megatronking.netbare.NetBare.get().getApplication();
                if (context != null) {
                    PackageManager pm = context.getPackageManager();
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    CharSequence appLabel = pm.getApplicationLabel(appInfo);
                    return appLabel != null ? appLabel.toString() : packageName;
                }
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to resolve app name for UID=" + uid, e);
        }
        return null;
    }
    
    /**
     * 通过UID获取包名
     * @param uid 应用UID
     * @return 包名，如果获取失败则返回null
     */
    public static String getPackageNameByUid(int uid) {
        try {
            // 使用NetBare.get().getApplication()获取Context
            android.content.Context context = com.github.megatronking.netbare.NetBare.get().getApplication();
            if (context == null) return null;
            
            String[] packages = context.getPackageManager().getPackagesForUid(uid);
            if (packages != null && packages.length > 0) {
                return packages[0];
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to get package name for UID=" + uid, e);
        }
        return null;
    }
}