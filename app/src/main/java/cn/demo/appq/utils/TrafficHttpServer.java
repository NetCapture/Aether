package cn.demo.appq.utils;

import android.util.Log;

import android.database.Cursor;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;
import java.util.List;

import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.greendao.ReqEntityDao;
import cn.demo.appq.greendao.ReqEntityDao.Query;

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
                } else if ("/api/traffic/url".equals(uri)) {
                    String limitStr = params.get("limit");
                    int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;
                    return serveUrlTrafficRank(limit);
                } else if ("/api/traffic/detail".equals(uri)) {
                    String offsetStr = params.get("offset");
                    String limitStr = params.get("limit");
                    int offset = offsetStr != null ? Integer.parseInt(offsetStr) : 0;
                    int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;
                    return serveRequestDetails(offset, limit);
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
                ReqEntityDao dao = DBManager.getReqEntityDao();
                StringBuilder json = new StringBuilder();
                json.append("{\"data\":[");

                // 使用原生SQL查询视图
                Cursor cursor = DBManager.rawQuery(
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

                Cursor cursor = DBManager.rawQuery("SELECT host, COUNT(*) as req_count, SUM(LENGTH) as usage_net FROM NETWORK_REQUEST_DETAILED GROUP BY host ORDER BY SUM(LENGTH) DESC LIMIT ?",
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
         * 获取按URL汇总的流量排行
         */
        private Response serveUrlTrafficRank(int limit) {
            try {
                Cursor cursor = DBManager.rawQuery("SELECT URL, COUNT(*) as req_count, SUM(LENGTH) as usage_net, APP_NAME FROM NETWORK_REQUEST_DETAILED GROUP BY URL ORDER BY SUM(LENGTH) DESC LIMIT ?",
                        new String[]{String.valueOf(limit)});

                boolean first = true;
                StringBuilder json = new StringBuilder();
                json.append("{\"data\":[");

                while (cursor.moveToNext()) {
                    if (!first) json.append(",");
                    first = false;

                    String url = cursor.getString(0);
                    long reqCount = cursor.getLong(1);
                    long usageNet = cursor.getLong(2);
                    String appName = cursor.getString(3);

                    json.append("{");
                    json.append("\"url\":\"").append(escapeJson(url)).append("\",");
                    json.append("\"reqCount\":").append(reqCount).append(",");
                    json.append("\"usageNet\":").append(usageNet).append(",");
                    json.append("\"usageNetFormatted\":\"").append(formatBytes(usageNet)).append("\",");
                    json.append("\"appName\":\"").append(escapeJson(appName)).append("\"");
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
                Log.e(TAG, "Error serving url traffic rank", e);
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "application/json",
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"
                );
            }
        }

        /**
         * 获取详细请求信息
         */
        private Response serveRequestDetails(int offset, int limit) {
            try {
                ReqEntityDao dao = DBManager.getReqEntityDao();
                Query<ReqEntity> query = dao.queryBuilder()
                    .orderDesc(ReqEntityDao.Properties.Time)
                    .offset(offset)
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
                    json.append("\"appName\":\"").append(escapeJson(req.getAppName())).append("\",");
                    json.append("\"url\":\"").append(escapeJson(req.getUrl())).append("\",");
                    json.append("\"method\":\"").append(escapeJson(req.getMethod())).append("\",");
                    json.append("\"host\":\"").append(escapeJson(req.getHost())).append("\",");
                    json.append("\"path\":\"").append(escapeJson(req.getPath())).append("\",");
                    json.append("\"isHttps\":").append(req.getIsHttps() != null && req.getIsHttps()).append(",");
                    json.append("\"httpProtocol\":\"").append(escapeJson(req.getHttpProtocol())).append("\",");
                    json.append("\"length\":").append(req.getLength()).append(",");
                    json.append("\"time\":").append(req.getTime());

                    // 添加请求头和响应头
                    if (req.getRequestHeaders() != null) {
                        json.append(",\"requestHeaders\":\"").append(escapeJson(req.getRequestHeaders())).append("\"");
                    }
                    if (req.getResponseHeaders() != null) {
                        json.append(",\"responseHeaders\":\"").append(escapeJson(req.getResponseHeaders())).append("\"");
                    }
                    if (req.getReqContent() != null) {
                        json.append(",\"requestBody\":\"").append(escapeJson(req.getReqContent())).append("\"");
                    }
                    if (req.getRespContent() != null) {
                        json.append(",\"responseBody\":\"").append(escapeJson(req.getRespContent())).append("\"");
                    }

                    json.append("}");
                }

                json.append("]}");
                return newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json; charset=UTF-8",
                        json.toString()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error serving request details", e);
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
                long totalRequests = DBManager.getReqEntityDao().queryBuilder().count();

                // 获取总流量
                Cursor cursor = DBManager.rawQuery("SELECT SUM(LENGTH) FROM NETWORK_REQUEST_DETAILED", null);
                long totalTraffic = 0;
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    totalTraffic = cursor.getLong(0);
                }
                cursor.close();

                // 获取不同应用数
                Cursor cursor2 = DBManager.rawQuery("SELECT COUNT(DISTINCT APP_NAME) FROM NETWORK_REQUEST_DETAILED", null);
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
                "        .tab-header { display: flex; background: #f8f8f8; border-bottom: 1px solid #e0e0e0; flex-wrap: wrap; }\n" +
                "        .tab-button { flex: 1; min-width: 120px; padding: 12px 8px; background: none; border: none; cursor: pointer; font-size: 14px; color: #666; transition: all 0.3s; }\n" +
                "        .tab-button.active { background: #007AFF; color: white; }\n" +
                "        .tab-content { display: none; padding: 20px; }\n" +
                "        .tab-content.active { display: block; }\n" +
                "        .table { width: 100%; border-collapse: collapse; }\n" +
                "        .table th, .table td { padding: 12px; text-align: left; border-bottom: 1px solid #e0e0e0; }\n" +
                "        .table th { background: #f8f8f8; font-weight: 600; color: #333; }\n" +
                "        .table tr:hover { background: #f8f8f8; }\n" +
                "        .loading { text-align: center; padding: 40px; color: #999; }\n" +
                "        .error { color: #ff3b30; padding: 20px; text-align: center; }\n" +
                "        .refresh-btn { background: #007AFF; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; margin-bottom: 15px; margin-right: 10px; }\n" +
                "        .refresh-btn:hover { background: #0056cc; }\n" +
                "        .detail-modal { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 1000; align-items: center; justify-content: center; }\n" +
                "        .detail-content { background: white; padding: 20px; border-radius: 8px; max-width: 800px; width: 90%; max-height: 80vh; overflow-y: auto; }\n" +
                "        .detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n" +
                "        .detail-section { margin-bottom: 20px; }\n" +
                "        .detail-section h3 { color: #333; margin-bottom: 10px; font-size: 16px; }\n" +
                "        .detail-section pre { background: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto; font-size: 12px; }\n" +
                "        .close-btn { background: #ff3b30; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }\n" +
                "        .close-btn:hover { background: #d70015; }\n" +
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
                "                <button class=\"tab-button\" onclick=\"switchTab('url')\">按URL汇总</button>\n" +
                "                <button class=\"tab-button\" onclick=\"switchTab('detail')\">详细请求</button>\n" +
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

                "            <div id=\"url-tab\" class=\"tab-content\">\n" +
                "                <button class=\"refresh-btn\" onclick=\"loadUrlRank()\">🔄 刷新</button>\n" +
                "                <table class=\"table\">\n" +
                "                    <thead>\n" +
                "                        <tr>\n" +
                "                            <th>URL</th>\n" +
                "                            <th>请求数</th>\n" +
                "                            <th>流量</th>\n" +
                "                            <th>应用</th>\n" +
                "                        </tr>\n" +
                "                    </thead>\n" +
                "                    <tbody id=\"urlRankBody\">\n" +
                "                        <tr><td colspan=\"4\" class=\"loading\">加载中...</td></tr>\n" +
                "                    </tbody>\n" +
                "                </table>\n" +
                "            </div>\n" +

                "            <div id=\"detail-tab\" class=\"tab-content\">\n" +
                "                <div style=\"display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;\">\n" +
                "                    <button class=\"refresh-btn\" onclick=\"loadRequestDetails()\">🔄 刷新</button>\n" +
                "                    <div>\n" +
                "                        <button class=\"refresh-btn\" onclick=\"prevPage()\" style=\"background: #666;\">← 上一页</button>\n" +
                "                        <button class=\"refresh-btn\" onclick=\"nextPage()\" style=\"background: #666;\">下一页 →</button>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "                <table class=\"table\">\n" +
                "                    <thead>\n" +
                "                        <tr>\n" +
                "                            <th>应用</th>\n" +
                "                            <th>方法</th>\n" +
                "                            <th>URL</th>\n" +
                "                            <th>流量</th>\n" +
                "                            <th>时间</th>\n" +
                "                            <th>操作</th>\n" +
                "                        </tr>\n" +
                "                    </thead>\n" +
                "                    <tbody id=\"detailBody\">\n" +
                "                        <tr><td colspan=\"6\" class=\"loading\">加载中...</td></tr>\n" +
                "                    </tbody>\n" +
                "                </table>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +

                "    <!-- 详情模态框 -->\n" +
                "    <div id=\"detailModal\" class=\"detail-modal\">\n" +
                "        <div class=\"detail-content\">\n" +
                "            <div class=\"detail-header\">\n" +
                "                <h2>请求详情</h2>\n" +
                "                <button class=\"close-btn\" onclick=\"closeDetailModal()\">关闭</button>\n" +
                "            </div>\n" +
                "            <div id=\"detailContent\"></div>\n" +
                "        </div>\n" +
                "    </div>\n" +

                "    <script>\n" +
                "        let currentOffset = 0;\n" +
                "        const pageSize = 20;\n" +
                "\n" +
                "        // 切换标签页\n" +
                "        function switchTab(tab) {\n" +
                "            document.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));\n" +
                "            event.target.classList.add('active');\n" +
                "\n" +
                "            document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));\n" +
                "            document.getElementById(tab + '-tab').classList.add('active');\n" +
                "\n" +
                "            if (tab === 'app') loadAppRank();\n" +
                "            if (tab === 'host') loadHostRank();\n" +
                "            if (tab === 'url') loadUrlRank();\n" +
                "            if (tab === 'detail') loadRequestDetails();\n" +
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

                "        // 加载URL排行\n" +
                "        async function loadUrlRank() {\n" +
                "            const tbody = document.getElementById('urlRankBody');\n" +
                "            tbody.innerHTML = '<tr><td colspan=\"4\" class=\"loading\">加载中...</td></tr>';\n" +
                "            try {\n" +
                "                const response = await fetch('/api/traffic/url?limit=20');\n" +
                "                const data = await response.json();\n" +
                "                if (data.data.length === 0) {\n" +
                "                    tbody.innerHTML = '<tr><td colspan=\"4\" class=\"loading\">暂无数据</td></tr>';\n" +
                "                    return;\n" +
                "                }\n" +
                "                tbody.innerHTML = data.data.map(item => `\n" +
                "                    <tr>\n" +
                "                        <td title=\"${item.url}\">${item.url.length > 50 ? item.url.substring(0, 50) + '...' : item.url}</td>\n" +
                "                        <td>${item.reqCount.toLocaleString()}</td>\n" +
                "                        <td>${item.usageNetFormatted}</td>\n" +
                "                        <td>${item.appName}</td>\n" +
                "                    </tr>\n" +
                "                `).join('');\n" +
                "            } catch (error) {\n" +
                "                tbody.innerHTML = '<tr><td colspan=\"4\" class=\"error\">加载失败: ' + error.message + '</td></tr>';\n" +
                "            }\n" +
                "        }\n" +

                "        // 加载详细请求\n" +
                "        async function loadRequestDetails() {\n" +
                "            const tbody = document.getElementById('detailBody');\n" +
                "            tbody.innerHTML = '<tr><td colspan=\"6\" class=\"loading\">加载中...</td></tr>';\n" +
                "            try {\n" +
                "                const response = await fetch(`/api/traffic/detail?offset=${currentOffset}&limit=${pageSize}`);\n" +
                "                const data = await response.json();\n" +
                "                if (data.data.length === 0) {\n" +
                "                    tbody.innerHTML = '<tr><td colspan=\"6\" class=\"loading\">暂无数据</td></tr>';\n" +
                "                    return;\n" +
                "                }\n" +
                "                tbody.innerHTML = data.data.map(item => `\n" +
                "                    <tr>\n" +
                "                        <td>${item.appName}</td>\n" +
                "                        <td><span style=\"color: #007AFF; font-weight: bold;\">${item.method}</span></td>\n" +
                "                        <td title=\"${item.url}\">${item.url.length > 60 ? item.url.substring(0, 60) + '...' : item.url}</td>\n" +
                "                        <td>${formatBytes(item.length)}</td>\n" +
                "                        <td>${new Date(item.time).toLocaleString()}</td>\n" +
                "                        <td><button class=\"refresh-btn\" style=\"padding: 4px 8px; font-size: 12px;\" onclick=\"showDetail(${item.id})\">查看</button></td>\n" +
                "                    </tr>\n" +
                "                `).join('');\n" +
                "            } catch (error) {\n" +
                "                tbody.innerHTML = '<tr><td colspan=\"6\" class=\"error\">加载失败: ' + error.message + '</td></tr>';\n" +
                "            }\n" +
                "        }\n" +

                "        // 上一页\n" +
                "        function prevPage() {\n" +
                "            if (currentOffset > 0) {\n" +
                "                currentOffset -= pageSize;\n" +
                "                loadRequestDetails();\n" +
                "            }\n" +
                "        }\n" +

                "        // 下一页\n" +
                "        function nextPage() {\n" +
                "            currentOffset += pageSize;\n" +
                "            loadRequestDetails();\n" +
                "        }\n" +

                "        // 显示详情\n" +
                "        function showDetail(id) {\n" +
                "            fetch(`/api/traffic/detail?offset=0&limit=1&id=${id}`)\n" +
                "                .then(response => response.json())\n" +
                "                .then(data => {\n" +
                "                    if (data.data.length > 0) {\n" +
                "                        const item = data.data[0];\n" +
                "                        let content = `\n" +
                "                            <div class=\"detail-section\">\n" +
                "                                <h3>基本信息</h3>\n" +
                "                                <p><strong>应用:</strong> ${item.appName}</p>\n" +
                "                                <p><strong>方法:</strong> <span style=\"color: #007AFF; font-weight: bold;\">${item.method}</span></p>\n" +
                "                                <p><strong>URL:</strong> <a href=\"${item.url}\" target=\"_blank\" style=\"color: #007AFF;\">${item.url}</a></p>\n" +
                "                                <p><strong>Host:</strong> ${item.host}</p>\n" +
                "                                <p><strong>Path:</strong> ${item.path}</p>\n" +
                "                                <p><strong>HTTPS:</strong> ${item.isHttps ? '是' : '否'}</p>\n" +
                "                                <p><strong>HTTP协议:</strong> ${item.httpProtocol}</p>\n" +
                "                                <p><strong>流量:</strong> ${formatBytes(item.length)}</p>\n" +
                "                                <p><strong>时间:</strong> ${new Date(item.time).toLocaleString()}</p>\n" +
                "                            </div>\n" +
                "                        `;\n" +
                "                        if (item.requestHeaders) {\n" +
                "                            content += `\n" +
                "                                <div class=\"detail-section\">\n" +
                "                                    <h3>请求头</h3>\n" +
                "                                    <pre>${escapeHtml(item.requestHeaders)}</pre>\n" +
                "                                </div>\n" +
                "                            `;\n" +
                "                        }\n" +
                "                        if (item.responseHeaders) {\n" +
                "                            content += `\n" +
                "                                <div class=\"detail-section\">\n" +
                "                                    <h3>响应头</h3>\n" +
                "                                    <pre>${escapeHtml(item.responseHeaders)}</pre>\n" +
                "                                </div>\n" +
                "                            `;\n" +
                "                        }\n" +
                "                        if (item.requestBody) {\n" +
                "                            content += `\n" +
                "                                <div class=\"detail-section\">\n" +
                "                                    <h3>请求体</h3>\n" +
                "                                    <pre>${escapeHtml(item.requestBody)}</pre>\n" +
                "                                </div>\n" +
                "                            `;\n" +
                "                        }\n" +
                "                        if (item.responseBody) {\n" +
                "                            content += `\n" +
                "                                <div class=\"detail-section\">\n" +
                "                                    <h3>响应体</h3>\n" +
                "                                    <pre>${escapeHtml(item.responseBody)}</pre>\n" +
                "                                </div>\n" +
                "                            `;\n" +
                "                        }\n" +
                "                        document.getElementById('detailContent').innerHTML = content;\n" +
                "                        document.getElementById('detailModal').style.display = 'flex';\n" +
                "                    }\n" +
                "                });\n" +
                "        }\n" +

                "        // 关闭详情模态框\n" +
                "        function closeDetailModal() {\n" +
                "            document.getElementById('detailModal').style.display = 'none';\n" +
                "        }\n" +

                "        // 点击模态框背景关闭\n" +
                "        document.getElementById('detailModal').addEventListener('click', function(e) {\n" +
                "            if (e.target === this) {\n" +
                "                closeDetailModal();\n" +
                "            }\n" +
                "        });\n" +

                "        // 格式化字节数\n" +
                "        function formatBytes(bytes) {\n" +
                "            if (bytes === 0) return '0 B';\n" +
                "            const k = 1024;\n" +
                "            const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];\n" +
                "            const i = Math.floor(Math.log(bytes) / Math.log(k));\n" +
                "            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];\n" +
                "        }\n" +

                "        // 转义HTML\n" +
                "        function escapeHtml(text) {\n" +
                "            if (!text) return '';\n" +
                "            const div = document.createElement('div');\n" +
                "            div.textContent = text;\n" +
                "            return div.innerHTML;\n" +
                "        }\n" +

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
