package cn.demo.appq.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.demo.appq.R;
import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.greendao.ReqEntityDao;
import cn.demo.appq.utils.DBManager;

/**
 * 本地流量统计列表视图
 * 参考Charles/Fiddler等主流抓包软件的界面设计
 */
public class TrafficListActivity extends AppCompatActivity {
    private static final String TAG = "TrafficListActivity";

    private RecyclerView recyclerView;
    private TrafficAdapter adapter;
    private ReqEntityDao dao;
    private List<ReqEntity> requestList = new ArrayList<>();
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_list);

        // 设置ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("流量统计详情");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 初始化
        packageManager = getPackageManager();
        dao = DBManager.getReqEntityDao();

        // 设置RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrafficAdapter(this, requestList);
        recyclerView.setAdapter(adapter);

        // 加载数据
        loadData();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData() {
        try {
            // 查询所有请求，按时间倒序
            List<ReqEntity> allRequests = dao.queryBuilder()
                .orderDesc(ReqEntityDao.Properties.Time)
                .limit(100) // 限制最新100条
                .list();

            requestList.clear();
            requestList.addAll(allRequests);
            adapter.notifyDataSetChanged();

            if (requestList.isEmpty()) {
                Toast.makeText(this, "暂无流量数据", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading data", e);
            Toast.makeText(this, "加载数据失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取应用信息 - 通过包名
     */
    private AppInfo getAppInfoByPackage(String packageName) {
        AppInfo appInfo = new AppInfo();
        appInfo.packageName = packageName != null ? packageName : "unknown";
        appInfo.appName = packageName != null ? packageName : "Unknown";
        appInfo.versionName = "";
        appInfo.versionCode = "";
        appInfo.icon = null;

        // 检查包名是否有效
        if (packageName == null || packageName.trim().isEmpty() || "unknown".equals(packageName)) {
            Log.w(TAG, "Invalid package name: " + packageName);
            appInfo.appName = "Unknown";
            appInfo.packageName = "unknown";
            return appInfo;
        }

        try {
            // 通过包名查找应用
            android.content.pm.ApplicationInfo app = packageManager.getApplicationInfo(packageName, 0);
            appInfo.appName = packageManager.getApplicationLabel(app).toString();
            appInfo.icon = packageManager.getApplicationIcon(app);

            // 获取版本信息
            try {
                android.content.pm.PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                appInfo.versionName = packageInfo.versionName;
                appInfo.versionCode = String.valueOf(packageInfo.versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Package info not found: " + packageName, e);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "App not found: " + packageName, e);
            appInfo.appName = "Unknown";
            appInfo.packageName = packageName;
        }

        return appInfo;
    }

    /**
     * 获取应用信息 - 通过UID（参考UidDumper的逻辑）
     */
    private AppInfo getAppInfoByUid(Integer uid) {
        AppInfo appInfo = new AppInfo();
        appInfo.packageName = "";
        appInfo.appName = "";
        appInfo.versionName = "";
        appInfo.versionCode = "";
        appInfo.icon = null;

        // 检查UID是否有效
        if (uid == null || uid <= 0) {
            Log.w(TAG, "Invalid UID: " + uid);
            appInfo.appName = "Unknown";
            appInfo.packageName = "unknown";
            return appInfo;
        }

        try {
            // 通过UID查找包名（参考UidDumper逻辑）
            String[] packages = packageManager.getPackagesForUid(uid);
            if (packages != null && packages.length > 0) {
                String packageName = packages[0];
                appInfo.packageName = packageName;

                // 通过包名获取应用信息
                android.content.pm.ApplicationInfo app = packageManager.getApplicationInfo(packageName, 0);
                appInfo.appName = packageManager.getApplicationLabel(app).toString();
                appInfo.icon = packageManager.getApplicationIcon(app);

                // 获取版本信息
                try {
                    android.content.pm.PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                    appInfo.versionName = packageInfo.versionName;
                    appInfo.versionCode = String.valueOf(packageInfo.versionCode);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Package not found: " + packageName, e);
                }
            } else {
                // 未找到对应的包名
                Log.w(TAG, "No packages found for UID: " + uid);
                appInfo.appName = "UID_" + uid;
                appInfo.packageName = "unknown";
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get app info for UID: " + uid, e);
            appInfo.appName = "UID_" + uid;
            appInfo.packageName = "unknown";
        }

        return appInfo;
    }

    /**
     * 综合获取应用信息 - 优先UID，然后包名，最后appName字段
     */
    private AppInfo getAppInfo(ReqEntity req) {
        // 1. 优先使用UID获取（最准确）
        if (req.getUid() != null && req.getUid() > 0) {
            AppInfo info = getAppInfoByUid(req.getUid());
            // 检查是否成功获取到有效信息
            if (info != null && !info.appName.startsWith("UID_") &&
                !"Unknown".equals(info.appName) && !"unknown".equals(info.appName)) {
                Log.d(TAG, "Got app info by UID: " + info.appName + " (" + info.packageName + ")");
                return info;
            }
        }

        // 2. UID获取失败或无效，则使用包名
        if (req.getAppPackage() != null && !req.getAppPackage().trim().isEmpty() &&
            !"unknown".equals(req.getAppPackage())) {
            AppInfo info = getAppInfoByPackage(req.getAppPackage());
            if (info != null && !"Unknown".equals(info.appName) && !"unknown".equals(info.appName)) {
                Log.d(TAG, "Got app info by package: " + info.appName + " (" + info.packageName + ")");
                return info;
            }
        }

        // 3. 包名也失败，则使用数据库中的appName字段（可能是之前存储的）
        AppInfo appInfo = new AppInfo();
        appInfo.packageName = req.getAppPackage() != null ? req.getAppPackage() : "unknown";
        appInfo.appName = req.getAppName() != null ? req.getAppName() : "Unknown";
        appInfo.versionName = "";
        appInfo.versionCode = "";
        appInfo.icon = null;
        Log.w(TAG, "Using fallback app name: " + appInfo.appName + " from database");
        return appInfo;
    }

    /**
     * 应用信息类
     */
    private static class AppInfo {
        String packageName;
        String appName;
        String versionName;
        String versionCode;
        Drawable icon;
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(int bytes) {
        if (bytes == 0) return "0 B";
        double k = 1024;
        String[] sizes = {"B", "KB", "MB", "GB", "TB"};
        int i = (int) Math.floor(Math.log(bytes) / Math.log(k));
        return String.format(Locale.getDefault(), "%.2f %s", bytes / Math.pow(k, i), sizes[i]);
    }

    /**
     * 格式化时间
     */
    private String formatTime(long timeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date(timeMillis));
    }

    /**
     * 格式化耗时
     */
    private String formatDuration(long timeMillis) {
        if (timeMillis < 1000) {
            return timeMillis + "ms";
        } else {
            return String.format(Locale.getDefault(), "%.2fs", timeMillis / 1000.0);
        }
    }

    /**
     * 适配器
     */
    private class TrafficAdapter extends RecyclerView.Adapter<TrafficAdapter.ViewHolder> {
        private Context context;
        private List<ReqEntity> data;
        private SparseBooleanArray expandedPositions = new SparseBooleanArray();

        public TrafficAdapter(Context context, List<ReqEntity> data) {
            this.context = context;
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_traffic_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReqEntity req = data.get(position);
            AppInfo appInfo = getAppInfo(req);

            // 设置应用图标
            if (appInfo.icon != null) {
                holder.ivAppIcon.setImageDrawable(appInfo.icon);
            } else {
                holder.ivAppIcon.setImageResource(R.mipmap.ic_launcher_unknow);
            }

            // 设置应用名
            holder.tvAppName.setText(appInfo.appName);

            // 设置状态指示器（HTTPS用绿色勾，错误用红色X）
            if (Boolean.TRUE.equals(req.getIsHttps())) {
                holder.ivStatusIndicator.setImageResource(android.R.drawable.checkbox_on_background);
            } else {
                holder.ivStatusIndicator.setImageResource(android.R.drawable.checkbox_off_background);
            }

            // 设置方法（不同颜色）
            holder.tvMethod.setText(req.getMethod());
            holder.tvMethod.setBackgroundColor(getMethodColor(req.getMethod()));

            // 设置URL
            holder.tvUrl.setText(req.getUrl());

            // 设置时间
            holder.tvTime.setText(formatTime(req.getTime()));

            // 设置流量
            holder.tvTraffic.setText(formatBytes(req.getLength()));

            // 设置应用信息
            holder.tvAppInfo.setText(String.format(Locale.getDefault(),
                "包名: %s\n版本: %s (%s)\nUID: %d",
                appInfo.packageName,
                appInfo.versionName.isEmpty() ? "未知" : appInfo.versionName,
                appInfo.versionCode.isEmpty() ? "未知" : appInfo.versionCode,
                req.getUid() != null ? req.getUid() : -1));

            // 设置请求头（格式化JSON）
            if (req.getRequestHeaders() != null && !req.getRequestHeaders().isEmpty()) {
                holder.tvRequestHeaders.setText(formatHeaders(req.getRequestHeaders()));
            } else {
                holder.tvRequestHeaders.setText("无");
            }

            // 设置请求体（格式化JSON或显示Base64摘要）
            if (req.getReqContent() != null && !req.getReqContent().isEmpty()) {
                holder.tvRequestBody.setText(formatBody(req.getReqContent()));
            } else {
                holder.tvRequestBody.setText("无");
            }

            // 设置响应头（格式化JSON）
            if (req.getResponseHeaders() != null && !req.getResponseHeaders().isEmpty()) {
                holder.tvResponseHeaders.setText(formatHeaders(req.getResponseHeaders()));
            } else {
                holder.tvResponseHeaders.setText("无");
            }

            // 设置响应体（格式化JSON或显示Base64摘要）
            if (req.getRespContent() != null && !req.getRespContent().isEmpty()) {
                holder.tvResponseBody.setText(formatBody(req.getRespContent()));
            } else {
                holder.tvResponseBody.setText("无");
            }

            // 设置网络状态
            String protocol = req.getHttpProtocol() != null ? req.getHttpProtocol() : "HTTP";
            Integer respCode = req.getRespCode();
            String respMessage = req.getRespMessage();
            String netType = req.getNetType();
            Integer uid = req.getUid();

            String status = respCode != null ? String.valueOf(respCode) : "未知";
            if (respMessage != null) {
                status += " " + respMessage;
            }

            holder.tvNetworkStatus.setText(String.format(Locale.getDefault(),
                "协议: %s | 状态: %s | 网络类型: %s | UID: %s",
                protocol,
                status,
                netType != null ? netType : "未知",
                uid != null ? uid.toString() : "未知"));

            // 处理展开/收起
            boolean isExpanded = expandedPositions.get(position, false);
            holder.llDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.ivExpand.setRotation(isExpanded ? 90 : 270);

            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                if (isExpanded) {
                    expandedPositions.delete(position);
                } else {
                    expandedPositions.put(position, true);
                }
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private int getMethodColor(String method) {
            switch (method) {
                case "GET": return 0xFF007AFF; // 蓝色
                case "POST": return 0xFF34C759; // 绿色
                case "PUT": return 0xFFFF9500; // 橙色
                case "DELETE": return 0xFFFF3B30; // 红色
                case "PATCH": return 0xFFAF52DE; // 紫色
                default: return 0xFF8E8E93; // 灰色
            }
        }

        /**
         * 格式化请求头/响应头 - 将JSON格式转换为更易读的形式
         */
        private String formatHeaders(String headers) {
            try {
                // 尝试解析JSON格式
                if (headers.trim().startsWith("{")) {
                    // 简单处理JSON格式的headers
                    StringBuilder sb = new StringBuilder();
                    String[] lines = headers.split("\n");
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;

                        // 移除JSON格式的符号
                        line = line.replaceAll("[{}\"]", "").trim();
                        if (line.isEmpty()) continue;

                        // 分离key和value
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim().replaceAll(",$", "");
                            sb.append("• ").append(key).append(": ").append(value).append("\n");
                        } else {
                            sb.append(line).append("\n");
                        }
                    }
                    return sb.toString();
                } else {
                    // 原始格式
                    return headers;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to format headers", e);
                return headers;
            }
        }

        /**
         * 格式化请求体/响应体 - JSON格式化或Base64摘要
         */
        private String formatBody(String body) {
            try {
                // 检查是否是Base64（通常很长且包含特殊字符）
                if (body.length() > 200 && isBase64(body)) {
                    // 显示Base64摘要
                    return "📦 Base64数据 (" + body.length() + " 字符)\n" +
                           "起始: " + body.substring(0, Math.min(50, body.length())) + "...\n" +
                           "完整数据可通过Web界面查看";
                }

                // 尝试JSON格式化
                if (body.trim().startsWith("{")) {
                    // 简单缩进JSON
                    return prettyPrintJson(body);
                }

                return body;
            } catch (Exception e) {
                Log.w(TAG, "Failed to format body", e);
                return body;
            }
        }

        /**
         * 检查字符串是否为Base64
         */
        private boolean isBase64(String str) {
            // Base64字符集
            String base64Pattern = "^[A-Za-z0-9+/]*={0,2}$";
            return str.length() % 4 == 0 && str.matches(base64Pattern);
        }

        /**
         * 简单JSON格式化
         */
        private String prettyPrintJson(String json) {
            StringBuilder sb = new StringBuilder();
            String[] lines = json.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                int indent = 0;
                for (char c : line.toCharArray()) {
                    if (c == '{' || c == '[') indent++;
                    else if (c == '}' || c == ']') indent--;
                }

                StringBuilder indentStr = new StringBuilder();
                for (int i = 0; i < indent; i++) {
                    indentStr.append("  ");
                }

                sb.append(indentStr).append(line).append("\n");
            }
            return sb.toString();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAppIcon;
            TextView tvAppName;
            ImageView ivStatusIndicator;
            TextView tvMethod;
            TextView tvUrl;
            TextView tvTime;
            TextView tvDuration;
            TextView tvTraffic;
            ImageView ivExpand;
            LinearLayout llDetails;

            TextView tvAppInfo;
            TextView tvRequestHeaders;
            TextView tvRequestBody;
            TextView tvResponseHeaders;
            TextView tvResponseBody;
            TextView tvNetworkStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
                tvAppName = itemView.findViewById(R.id.tv_app_name);
                ivStatusIndicator = itemView.findViewById(R.id.iv_status_indicator);
                tvMethod = itemView.findViewById(R.id.tv_method);
                tvUrl = itemView.findViewById(R.id.tv_url);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                tvTraffic = itemView.findViewById(R.id.tv_traffic);
                ivExpand = itemView.findViewById(R.id.iv_expand);
                llDetails = itemView.findViewById(R.id.ll_details);

                tvAppInfo = itemView.findViewById(R.id.tv_app_info);
                tvRequestHeaders = itemView.findViewById(R.id.tv_request_headers);
                tvRequestBody = itemView.findViewById(R.id.tv_request_body);
                tvResponseHeaders = itemView.findViewById(R.id.tv_response_headers);
                tvResponseBody = itemView.findViewById(R.id.tv_response_body);
                tvNetworkStatus = itemView.findViewById(R.id.tv_network_status);
            }
        }
    }
}
