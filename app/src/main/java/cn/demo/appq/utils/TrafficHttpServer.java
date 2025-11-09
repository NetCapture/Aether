package cn.demo.appq.utils;

import android.util.Log;

import android.database.Cursor;

import org.greenrobot.greendao.query.Query;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;
import java.util.List;

import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.greendao.ReqEntityDao;

/**
 * 流量统计HTTP服务器
 * 提供Web界面查看流量统计排行
 */
public class TrafficHttpServer {
    private static final String TAG = "TrafficHttpServer";
    private static final int PORT = 8080;

    private nanoHttpServer server;
    private static TrafficHttpServer sInstance;

    private TrafficHttpServer() {
    }

    public static synchronized TrafficHttpServer getInstance() {
        if (sInstance == null) {
            sInstance = new TrafficHttpServer();
        }
        return sInstance;
    }

    /**
     * 启动HTTP服务器
     */
    public void start() {
        if (server != null && server.isAlive()) {
            Log.i(TAG, "Server already running on port " + PORT);
            return;
        }

        try {
            server = new nanoHttpServer(PORT);
            server.start();
            Log.i(TAG, "Traffic HTTP server started on port " + PORT);
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server", e);
        }
    }

    /**
     * 停止HTTP服务器
     */
    public void stop() {
        if (server != null) {
            server.stop();
            Log.i(TAG, "Traffic HTTP server stopped");
        }
    }

    /**
     * 检查服务器是否运行
     */
    public boolean isRunning() {
        return server != null && server.isAlive();
    }

    /**
     * 获取服务器访问地址
     */
    public String getServerUrl() {
        return "http://localhost:" + PORT;
    }

    /**
     * NanoHTTPD 服务器实现
     */
    private static class nanoHttpServer extends NanoHTTPD {
        private nanoHttpServer(int port) throws IOException {
            super(port);
        }

        @Override
        public Response serve(NanoHTTPD.IHTTPSession session) {
            String uri = session.getUri();

            try {
                // 处理根路径
                if ("/".equals(uri) || "/index.html".equals(uri)) {
                    return serveIndexPage();
                }

                // 处理API请求
                if (uri.startsWith("/api/")) {
                    return handleApiRequest(session, uri);
                }

                // 返回404
                return getNotFoundResponse();
            } catch (Exception e) {
                Log.e(TAG, "Error serving request: " + uri, e);
                return getErrorResponse(e);
            }
        }

        /**
         * 主页 - 流量统计展示
         */
        private Response serveIndexPage() {
            String html = generateIndexHtml();
            return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "text/html; charset=UTF-8",
                    html
            );
        }

        /**
         * 处理API请求
         */
        private Response handleApiRequest(NanoHTTPD.IHTTPSession session, String uri) {
            Map<String, String> params = session.getParms();

            try {
                if ("/api/traffic/app".equals(uri)) {
                    String limitStr = params.get("limit");
                    int limit = limitStr != null ? Integer.parseInt(limitStr) : 10;
                    return serveAppTrafficRank(limit);
                } else if ("/api/traffic/host".equals(uri)) {
                    String limitStr = params.get("limit");
                    int limit = limitStr != null ? Integer.parseInt(limitStr) : 10;
                    return serveHostTrafficRank(limit);
                } else if ("/api/traffic/recent".equals(uri)) {
                    String limitStr = params.get("limit");
                    int limit = limitStr != null ? Integer.parseInt(limitStr) : 50;
                    return serveRecentRequests(limit);
                } else if ("/api/stats/summary".equals(uri)) {
                    return serveSummaryStats();
                } else {
                    return getNotFoundResponse();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling API request: " + uri, e);
                return getErrorResponse(e);
            }
        }

        /**
         * 获取APP流量排行
         */
        private Response serveAppTrafficRank(int limit) {
            try {
                ReqEntityDao dao = DBManager.getInstance().getReqEntityDao();
                StringBuilder json = new StringBuilder();
                json.append("{\"data\":[");

                // 使用原生SQL查询视图
                Cursor cursor = dao.getDatabase().rawQuery(
                    "SELECT app_name, req_count, usage_net, begin_time FROM APP_USAGE_TRAFFIC_RANK LIMIT ?",
                    new String[]{String.valueOf(limit)}
                );

                boolean first = true;
                while (cursor.moveToNext()) {
                    if (!first) json.append(",");
                    first = false;

                    String appName = cursor.getString(0);
                    long reqCount = cursor.getLong(1);
                    long usageNet = cursor.getLong(2);
                    long beginTime = cursor.getLong(3);

                json.append("{");
                json.append("\"appName\":\"").append(escapeJson(appName)).append("\",");
                json.append("\"reqCount\":").append(reqCount).append(",");
                json.append("\"usageNet\":").append(usageNet).append(",");
                json.append("\"usageNetFormatted\":\"").append(formatBytes(usageNet)).append("\",");
                json.append("\"beginTime\":").append(beginTime);
                json.append("}");
                }

                cursor.close();
                json.append("]}");
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json; charset=UTF-8",
                        json.toString()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error serving app traffic rank", e);
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "application/json",
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"
                );
            }
        }

        /**
         * 获取域名流量排行
         */
        private Response serveHostTrafficRank(int limit) {
            try {
                StringBuilder json = new StringBuilder();
                json.append("{\"data\":[");

                Cursor cursor = DBManager.getInstance().getDatabase()
                    .rawQuery("SELECT host, COUNT(*) as req_count, SUM(LENGTH) as usage_net FROM NETWORK_REQUEST_DETAILED GROUP BY host ORDER BY SUM(LENGTH) DESC LIMIT ?",
                        new String[]{String.valueOf(limit)});

                boolean first = true;
                while (cursor.moveToNext()) {
                    if (!first) json.append(",");
                    first = false;

                    String host = cursor.getString(0);
                    long reqCount = cursor.getLong(1);
                    long usageNet = cursor.getLong(2);

                    json.append("{");
                    json.append("\"host\":\"").append(escapeJson(host)).append("\",");
                    json.append("\"reqCount\":").append(reqCount).append(",");
                    json.append("\"usageNet\":").append(usageNet).append(",");
                    json.append("\"usageNetFormatted\":\"").append(formatBytes(usageNet)).append("\"");
                    json.append("}");
                }

                cursor.close();
                json.append("]}");
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json; charset=UTF-8",
                        json.toString()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error serving host traffic rank", e);
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "application/json",
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"
                );
            }
        }

        /**
         * 获取最近请求
         */
        private Response serveRecentRequests(int limit) {
            try {
                ReqEntityDao dao = DBManager.getInstance().getReqEntityDao();
                Query<ReqEntity> query = dao.queryBuilder()
                    .orderDesc(ReqEntityDao.Properties.Time)
                    .limit(limit)
                    .build();

                List<ReqEntity> requests = query.list();

                StringBuilder json = new StringBuilder();
                json.append("{\"data\":[");

                boolean first = true;
                for (ReqEntity req : requests) {
                    if (!first) json.append(",");
                    first = false;

                    json.append("{");
                    json.append("\"id\":").append(req.getId()).append(",");
                    json.append("\"url\":\"").append(escapeJson(req.getUrl())).append("\",");
                    json.append("\"method\":\"").append(escapeJson(req.getMethod())).append("\",");
                    json.append("\"host\":\"").append(escapeJson(req.getHost())).append("\",");
                    json.append("\"length\":").append(req.getLength()).append(",");
                    json.append("\"time\":").append(req.getTime());
                    json.append("}");
                }

                json.append("]}");
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json; charset=UTF-8",
                        json.toString()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error serving recent requests", e);
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "application/json",
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"
                );
            }
        }

        /**
         * 获取摘要统计
         */
        private Response serveSummaryStats() {
            try {
                // 获取总请求数
                long totalRequests = DBManager.getInstance().getReqEntityDao().queryBuilder().count();

                // 获取总流量
                Cursor cursor = DBManager.getInstance().getDatabase()
                    .rawQuery("SELECT SUM(LENGTH) FROM NETWORK_REQUEST_DETAILED", null);
                long totalTraffic = 0;
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    totalTraffic = cursor.getLong(0);
                }
                cursor.close();

                // 获取不同应用数
                Cursor cursor2 = DBManager.getInstance().getDatabase()
                    .rawQuery("SELECT COUNT(DISTINCT APP_NAME) FROM NETWORK_REQUEST_DETAILED", null);
                long appCount = 0;
                if (cursor2.moveToFirst() && !cursor2.isNull(0)) {
                    appCount = cursor2.getLong(0);
                }
                cursor2.close();

                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"totalRequests\":").append(totalRequests).append(",");
                json.append("\"totalTraffic\":").append(totalTraffic).append(",");
                json.append("\"totalTrafficFormatted\":\"").append(formatBytes(totalTraffic)).append("\",");
                json.append("\"appCount\":").append(appCount);
                json.append("}");

                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json; charset=UTF-8",
                        json.toString()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error serving summary stats", e);
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "application/json",
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"
                );
            }
        }

        /**
         * 生成主页HTML
         */
        private String generateIndexHtml() {
            return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>流量统计排行 - Aether</title>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; }\n" +
                "        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }\n" +
                "        .header { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }\n" +
                "        .header h1 { color: #333; margin-bottom: 10px; }\n" +
                "        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 20px; }\n" +
                "        .stat-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; }\n" +
                "        .stat-value { font-size: 28px; font-weight: bold; color: #007AFF; margin-bottom: 5px; }\n" +
                "        .stat-label { color: #666; font-size: 14px; }\n" +
                "        .tabs { background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); overflow: hidden; }\n" +
                "        .tab-header { display: flex; background: #f8f8f8; border-bottom: 1px solid #e0e0e0; }\n" +
                "        .tab-button { flex: 1; padding: 15px; background: none; border: none; cursor: pointer; font-size: 16px; color: #666; transition: all 0.3s; }\n" +
                "        .tab-button.active { background: #007AFF; color: white; }\n" +
                "        .tab-content { display: none; padding: 20px; }\n" +
                "        .tab-content.active { display: block; }\n" +
                "        .table { width: 100%; border-collapse: collapse; }\n" +
                "        .table th, .table td { padding: 12px; text-align: left; border-bottom: 1px solid #e0e0e0; }\n" +
                "        .table th { background: #f8f8f8; font-weight: 600; color: #333; }\n" +
                "        .table tr:hover { background: #f8f8f8; }\n" +
                "        .loading { text-align: center; padding: 40px; color: #999; }\n" +
                "        .error { color: #ff3b30; padding: 20px; text-align: center; }\n" +
                "        .refresh-btn { background: #007AFF; color: white; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px; margin-bottom: 15px; }\n" +
                "        .refresh-btn:hover { background: #0056cc; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>🌐 流量统计排行</h1>\n" +
                "            <p style=\"color: #666; margin-top: 5px;\">Aether VPN 流量分析面板</p>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"stats-grid\">\n" +
                "            <div class=\"stat-card\">\n" +
                "                <div class=\"stat-value\" id=\"totalRequests\">-</div>\n" +
                "                <div class=\"stat-label\">总请求数</div>\n" +
                "            </div>\n" +
                "            <div class=\"stat-card\">\n" +
                "                <div class=\"stat-value\" id=\"totalTraffic\">-</div>\n" +
                "                <div class=\"stat-label\">总流量</div>\n" +
                "            </div>\n" +
                "            <div class=\"stat-card\">\n" +
                "                <div class=\"stat-value\" id=\"appCount\">-</div>\n" +
                "                <div class=\"stat-label\">应用数量</div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"tabs\">\n" +
                "            <div class=\"tab-header\">\n" +
                "                <button class=\"tab-button active\" onclick=\"switchTab('app')\">按应用排行</button>\n" +
                "                <button class=\"tab-button\" onclick=\"switchTab('host')\">按域名排行</button>\n" +
                "                <button class=\"tab-button\" onclick=\"switchTab('recent')\">最近请求</button>\n" +
                "            </div>\n" +
                "\n" +
                "            <div id=\"app-tab\" class=\"tab-content active\">\n" +
                "                <button class=\"refresh-btn\" onclick=\"loadAppRank()\">🔄 刷新</button>\n" +
                "                <table class=\"table\">\n" +
                "                    <thead>\n" +
                "                        <tr>\n" +
                "                            <th>应用名称</th>\n" +
                "                            <th>请求数</th>\n" +
                "                            <th>流量</th>\n" +
                "                            <th>首次请求</th>\n" +
                "                        </tr>\n" +
                "                    </thead>\n" +
                "                    <tbody id=\"appRankBody\">\n" +
                "                        <tr><td colspan=\"4\" class=\"loading\">加载中...</td></tr>\n" +
                "                    </tbody>\n" +
                "                </table>\n" +
                "            </div>\n" +
                "\n" +
                "            <div id=\"host-tab\" class=\"tab-content\">\n" +
                "                <button class=\"refresh-btn\" onclick=\"loadHostRank()\">🔄 刷新</button>\n" +
                "                <table class=\"table\">\n" +
                "                    <thead>\n" +
                "                        <tr>\n" +
                "                            <th>域名</th>\n" +
                "                            <th>请求数</th>\n" +
                "                            <th>流量</th>\n" +
                "                        </tr>\n" +
                "                    </thead>\n" +
                "                    <tbody id=\"hostRankBody\">\n" +
                "                        <tr><td colspan=\"3\" class=\"loading\">加载中...</td></tr>\n" +
                "                    </tbody>\n" +
                "                </table>\n" +
                "            </div>\n" +
                "\n" +
                "            <div id=\"recent-tab\" class=\"tab-content\">\n" +
                "                <button class=\"refresh-btn\" onclick=\"loadRecentRequests()\">🔄 刷新</button>\n" +
                "                <table class=\"table\">\n" +
                "                    <thead>\n" +
                "                        <tr>\n" +
                "                            <th>URL</th>\n" +
                "                            <th>方法</th>\n" +
                "                            <th>域名</th>\n" +
                "                            <th>流量</th>\n" +
                "                            <th>时间</th>\n" +
                "                        </tr>\n" +
                "                    </thead>\n" +
                "                    <tbody id=\"recentBody\">\n" +
                "                        <tr><td colspan=\"5\" class=\"loading\">加载中...</td></tr>\n" +
                "                    </tbody>\n" +
                "                </table>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        // 切换标签页\n" +
                "        function switchTab(tab) {\n" +
                "            // 更新按钮状态\n" +
                "            document.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));\n" +
                "            event.target.classList.add('active');\n" +
                "\n" +
                "            // 更新内容显示\n" +
                "            document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));\n" +
                "            document.getElementById(tab + '-tab').classList.add('active');\n" +
                "\n" +
                "            // 加载对应数据\n" +
                "            if (tab === 'app') loadAppRank();\n" +
                "            if (tab === 'host') loadHostRank();\n" +
                "            if (tab === 'recent') loadRecentRequests();\n" +
                "        }\n" +
                "\n" +
                "        // 加载摘要统计\n" +
                "        async function loadSummary() {\n" +
                "            try {\n" +
                "                const response = await fetch('/api/stats/summary');\n" +
                "                const data = await response.json();\n" +
                "                document.getElementById('totalRequests').textContent = data.totalRequests.toLocaleString();\n" +
                "                document.getElementById('totalTraffic').textContent = data.totalTrafficFormatted;\n" +
                "                document.getElementById('appCount').textContent = data.appCount.toLocaleString();\n" +
                "            } catch (error) {\n" +
                "                console.error('Failed to load summary:', error);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        // 加载APP排行\n" +
                "        async function loadAppRank() {\n" +
                "            const tbody = document.getElementById('appRankBody');\n" +
                "            tbody.innerHTML = '<tr><td colspan=\"4\" class=\"loading\">加载中...</td></tr>';\n" +
                "            try {\n" +
                "                const response = await fetch('/api/traffic/app?limit=20');\n" +
                "                const data = await response.json();\n" +
                "                if (data.data.length === 0) {\n" +
                "                    tbody.innerHTML = '<tr><td colspan=\"4\" class=\"loading\">暂无数据</td></tr>';\n" +
                "                    return;\n" +
                "                }\n" +
                "                tbody.innerHTML = data.data.map(item => `\n" +
                "                    <tr>\n" +
                "                        <td>${item.appName}</td>\n" +
                "                        <td>${item.reqCount.toLocaleString()}</td>\n" +
                "                        <td>${item.usageNetFormatted}</td>\n" +
                "                        <td>${new Date(item.beginTime).toLocaleString()}</td>\n" +
                "                    </tr>\n" +
                "                `).join('');\n" +
                "            } catch (error) {\n" +
                "                tbody.innerHTML = '<tr><td colspan=\"4\" class=\"error\">加载失败: ' + error.message + '</td></tr>';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        // 加载域名排行\n" +
                "        async function loadHostRank() {\n" +
                "            const tbody = document.getElementById('hostRankBody');\n" +
                "            tbody.innerHTML = '<tr><td colspan=\"3\" class=\"loading\">加载中...</td></tr>';\n" +
                "            try {\n" +
                "                const response = await fetch('/api/traffic/host?limit=20');\n" +
                "                const data = await response.json();\n" +
                "                if (data.data.length === 0) {\n" +
                "                    tbody.innerHTML = '<tr><td colspan=\"3\" class=\"loading\">暂无数据</td></tr>';\n" +
                "                    return;\n" +
                "                }\n" +
                "                tbody.innerHTML = data.data.map(item => `\n" +
                "                    <tr>\n" +
                "                        <td>${item.host}</td>\n" +
                "                        <td>${item.reqCount.toLocaleString()}</td>\n" +
                "                        <td>${item.usageNetFormatted}</td>\n" +
                "                    </tr>\n" +
                "                `).join('');\n" +
                "            } catch (error) {\n" +
                "                tbody.innerHTML = '<tr><td colspan=\"3\" class=\"error\">加载失败: ' + error.message + '</td></tr>';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        // 加载最近请求\n" +
                "        async function loadRecentRequests() {\n" +
                "            const tbody = document.getElementById('recentBody');\n" +
                "            tbody.innerHTML = '<tr><td colspan=\"5\" class=\"loading\">加载中...</td></tr>';\n" +
                "            try {\n" +
                "                const response = await fetch('/api/traffic/recent?limit=50');\n" +
                "                const data = await response.json();\n" +
                "                if (data.data.length === 0) {\n" +
                "                    tbody.innerHTML = '<tr><td colspan=\"5\" class=\"loading\">暂无数据</td></tr>';\n" +
                "                    return;\n" +
                "                }\n" +
                "                tbody.innerHTML = data.data.map(item => `\n" +
                "                    <tr>\n" +
                "                        <td title=\"${item.url}\">${item.url.length > 50 ? item.url.substring(0, 50) + '...' : item.url}</td>\n" +
                "                        <td>${item.method}</td>\n" +
                "                        <td>${item.host}</td>\n" +
                "                        <td>${formatBytes(item.length)}</td>\n" +
                "                        <td>${new Date(item.time).toLocaleString()}</td>\n" +
                "                    </tr>\n" +
                "                `).join('');\n" +
                "            } catch (error) {\n" +
                "                tbody.innerHTML = '<tr><td colspan=\"5\" class=\"error\">加载失败: ' + error.message + '</td></tr>';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        // 格式化字节数\n" +
                "        function formatBytes(bytes) {\n" +
                "            if (bytes === 0) return '0 B';\n" +
                "            const k = 1024;\n" +
                "            const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];\n" +
                "            const i = Math.floor(Math.log(bytes) / Math.log(k));\n" +
                "            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];\n" +
                "        }\n" +
                "\n" +
                "        // 页面加载时初始化\n" +
                "        window.onload = function() {\n" +
                "            loadSummary();\n" +
                "            loadAppRank();\n" +
                "            // 每30秒自动刷新\n" +
                "            setInterval(loadSummary, 30000);\n" +
                "        };\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
        }

        /**
         * 工具方法：转义JSON字符串
         */
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
        }

        /**
         * 工具方法：格式化字节数
         */
        private String formatBytes(long bytes) {
            if (bytes == 0) return "0 B";
            double k = 1024;
            String[] sizes = {"B", "KB", "MB", "GB", "TB"};
            int i = (int) Math.floor(Math.log(bytes) / Math.log(k));
            return String.format("%.2f %s", bytes / Math.pow(k, i), sizes[i]);
        }

        /**
         * 返回404响应
         */
        private Response getNotFoundResponse() {
            return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.NOT_FOUND,
                    "text/html; charset=UTF-8",
                    "<h1>404 - Not Found</h1><p>请求的资源不存在</p>"
            );
        }

        /**
         * 返回错误响应
         */
        private Response getErrorResponse(Exception e) {
            return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    "text/html; charset=UTF-8",
                    "<h1>500 - Internal Server Error</h1><p>服务器内部错误: " + escapeHtml(e.getMessage()) + "</p>"
            );
        }

        /**
         * 工具方法：转义HTML
         */
        private String escapeHtml(String str) {
            if (str == null) return "";
            return str.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;")
                     .replace("'", "&#x27;");
        }
    }
}
