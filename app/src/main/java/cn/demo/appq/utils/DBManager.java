package cn.demo.appq.utils;

import android.database.Cursor;

import cn.demo.appq.greendao.ReqEntityDao;

/**
 * 临时 Mock DBManager，用于编译
 * TODO: 需要选择替代 GreenDAO 的数据库方案
 */
public class DBManager {
    private static volatile ReqEntityDao reqEntityDao = null;

    private DBManager() {
    }

    public static ReqEntityDao getReqEntityDao() {
        if (reqEntityDao == null) {
            synchronized (DBManager.class) {
                if (reqEntityDao == null) {
                    // TODO: 暂时返回空实现，后续需要选择替代数据库方案
                    reqEntityDao = new ReqEntityDao(null);
                }
            }
        }
        return reqEntityDao;
    }

    public static ReqEntityDao getInstance() {
        return getReqEntityDao();
    }

    // Mock database methods
    public static Cursor getDatabase() {
        return null;
    }

    public static Cursor rawQuery(String sql, String[] args) {
        return null;
    }
}
