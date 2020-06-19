package cn.demo.appq.utils;

import com.github.megatronking.netbare.L;

import org.greenrobot.greendao.database.Database;

import cn.demo.appq.greendao.DaoMaster;
import cn.demo.appq.greendao.DaoSession;

public class DBManager {
    private static volatile DaoSession instance = null;

    private DBManager() {
    }

    public static DaoSession getInstance() {
        if (instance == null) {
            synchronized (DBManager.class) {
                if (instance == null) {
                    Database db = new DaoMaster.DevOpenHelper(L.getContext(null), "log_db").getWritableDb();
                    DaoMaster daoMaster = new DaoMaster(db);
                    //汇总APP使用流量排行
                    daoMaster.getDatabase().execSQL(
                           "CREATE VIEW IF NOT EXISTS APP_USAGE_TRAFFIC_RANK \n" +
                                   "AS\n" +
                                   "SELECT APP_NAME AS app_name," +
                                   "COUNT(*) AS req_count, " +
                                   "SUM(LENGTH) AS usage_net," +
                                   "MIN(TIME) AS begin_time\n" +
                                   "FROM NETWORK_REQUEST_DETAILED \n" +
                                   "GROUP BY APP_NAME \n" +
                                   "ORDER BY SUM(LENGTH) DESC;");
                    daoMaster.getDatabase().execSQL(
                           "CREATE VIEW IF NOT EXISTS [HOST_USAGE_TRAFFIC_RANK]\n" +
                                   "AS\n" +
                                   "SELECT \n" +
                                   "       APP_NAME AS app_name, \n" +
                                   "       HOST AS host, \n" +
                                   "       COUNT(*) AS req_count, \n" +
                                   "       SUM(LENGTH)  AS usage_net, \n" +
                                   "       MIN(TIME) AS begin_time\n" +
                                   "FROM   NETWORK_REQUEST_DETAILED\n" +
                                   "GROUP  BY HOST\n" +
                                   "ORDER  BY SUM(LENGTH) DESC;");
                    instance = daoMaster.newSession();
                }
            }
        }
        return instance;
    }
}
