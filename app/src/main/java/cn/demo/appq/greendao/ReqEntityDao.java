package cn.demo.appq.greendao;

import android.database.Cursor;

import cn.demo.appq.entity.ReqEntity;

import java.util.List;

/**
 * 简化的 ReqEntityDao 临时实现，用于编译
 * TODO: 需要选择替代 GreenDAO 的数据库方案
 */
public class ReqEntityDao {
    public ReqEntityDao(DaoConfig config) {
        // 临时空实现
    }

    public void insert(ReqEntity entity) {
        // 临时空实现
    }

    public void update(ReqEntity entity) {
        // 临时空实现
    }

    public QueryBuilder queryBuilder() {
        return new QueryBuilder();
    }

    public static class QueryBuilder {
        public QueryBuilder where(Condition condition) {
            return this;
        }

        public QueryBuilder and(Condition condition) {
            return this;
        }

        public QueryBuilder orderDesc(Property property) {
            return this;
        }

        public QueryBuilder limit(int limit) {
            return this;
        }

        public QueryBuilder offset(int offset) {
            return this;
        }

        public Query<ReqEntity> build() {
            return null;
        }

        public List<ReqEntity> list() {
            return null;
        }

        public long count() {
            return 0;
        }
    }

    public long count() {
        return 0;
    }

    public void deleteAll() {
        // 临时空实现
    }

    public ReqEntityDao getDatabase() {
        return this;
    }

    public Cursor rawQuery(String sql, String[] args) {
        return null;
    }

    public void detachAll() {
        // 临时空实现
    }

    public ReqEntity loadByRowId(long id) {
        return null;
    }

    public Query<ReqEntity> queryRaw(String where, String... selectionArgs) {
        return null;
    }

    public static class Query<T> {
        public List<T> list() {
            return null;
        }
        public Query<T> orderDesc(Property property) {
            return this;
        }
    }

    public static class Properties {
        public static final Property Id = new Property(0, Long.class, "id", true, "_id");
        public static final Property SessionId = new Property(1, String.class, "sessionId", false, "SESSION_ID");
        public static final Property AppName = new Property(2, String.class, "appName", false, "APP_NAME");
        public static final Property Url = new Property(6, String.class, "url", false, "URL");
        public static final Property AppPackage = new Property(3, String.class, "appPackage", false, "APP_PACKAGE");
        public static final Property Time = new Property(16, Long.class, "time", false, "TIME");
    }

    public static class Property {
        public Property(int ordinal, Class<?> type, String name, boolean primaryKey, String columnName) {
            this.ordinal = ordinal;
            this.type = type;
            this.name = name;
            this.primaryKey = primaryKey;
            this.columnName = columnName;
        }

        public int ordinal;
        public Class<?> type;
        public String name;
        public boolean primaryKey;
        public String columnName;

        public Condition eq(Object value) {
            return new Condition(this, "=", value);
        }

        public Condition like(String pattern) {
            return new Condition(this, "LIKE", pattern);
        }
    }

    public static class Condition {
        private Property property;
        private String operator;
        private Object value;

        public Condition(Property property, String operator, Object value) {
            this.property = property;
            this.operator = operator;
            this.value = value;
        }
    }
}
