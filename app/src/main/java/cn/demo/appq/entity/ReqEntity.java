package cn.demo.appq.entity;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 *
 */
@Entity(nameInDb = "NETWORK_REQUEST_DETAILED")
public class ReqEntity {
    @Id(autoincrement = true)
    private Long id;
    private String sessionId;
    private String appName;
    private String appPackage;
    private String appVersion;
    private String appVersionCode;
    private String url;
    private String host;
    private Integer port;
    private Integer index;
    private String ip;
    private String protocol;
    private String httpProtocol;
    private String method;
    private String path;
    private Boolean isHttps;
    private Long time;
    private Integer uid;
    private Integer length;
    private Integer streamId;
    private String requestHeaders;
    private String responseHeaders;
    private String clientHttp2Settings;
    private String peerHttp2Settings;
    private String reqContent;
    private String respContent;
    private String respMessage;
    private Integer respCode;
    private Boolean isWebSocket;
    private String netType;
    private int requestBodyOffset;
    private int responseBodyOffset;

    @Generated(hash = 16580686)
    public ReqEntity(Long id, String sessionId, String appName, String appPackage,
            String appVersion, String appVersionCode, String url, String host, Integer port,
            Integer index, String ip, String protocol, String httpProtocol, String method,
            String path, Boolean isHttps, Long time, Integer uid, Integer length,
            Integer streamId, String requestHeaders, String responseHeaders,
            String clientHttp2Settings, String peerHttp2Settings, String reqContent,
            String respContent, String respMessage, Integer respCode, Boolean isWebSocket,
            String netType, int requestBodyOffset, int responseBodyOffset) {
        this.id = id;
        this.sessionId = sessionId;
        this.appName = appName;
        this.appPackage = appPackage;
        this.appVersion = appVersion;
        this.appVersionCode = appVersionCode;
        this.url = url;
        this.host = host;
        this.port = port;
        this.index = index;
        this.ip = ip;
        this.protocol = protocol;
        this.httpProtocol = httpProtocol;
        this.method = method;
        this.path = path;
        this.isHttps = isHttps;
        this.time = time;
        this.uid = uid;
        this.length = length;
        this.streamId = streamId;
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
        this.clientHttp2Settings = clientHttp2Settings;
        this.peerHttp2Settings = peerHttp2Settings;
        this.reqContent = reqContent;
        this.respContent = respContent;
        this.respMessage = respMessage;
        this.respCode = respCode;
        this.isWebSocket = isWebSocket;
        this.netType = netType;
        this.requestBodyOffset = requestBodyOffset;
        this.responseBodyOffset = responseBodyOffset;
    }

    @Generated(hash = 1573136)
    public ReqEntity() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return this.port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getIndex() {
        return this.index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHttpProtocol() {
        return this.httpProtocol;
    }

    public void setHttpProtocol(String httpProtocol) {
        this.httpProtocol = httpProtocol;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getIsHttps() {
        return this.isHttps;
    }

    public void setIsHttps(Boolean isHttps) {
        this.isHttps = isHttps;
    }

    public Long getTime() {
        return this.time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Integer getUid() {
        return this.uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getLength() {
        return this.length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getStreamId() {
        return this.streamId;
    }

    public void setStreamId(Integer streamId) {
        this.streamId = streamId;
    }

    public String getRequestHeaders() {
        return this.requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getClientHttp2Settings() {
        return this.clientHttp2Settings;
    }

    public void setClientHttp2Settings(String clientHttp2Settings) {
        this.clientHttp2Settings = clientHttp2Settings;
    }

    public String getPeerHttp2Settings() {
        return this.peerHttp2Settings;
    }

    public void setPeerHttp2Settings(String peerHttp2Settings) {
        this.peerHttp2Settings = peerHttp2Settings;
    }

    public String getReqContent() {
        return this.reqContent;
    }

    public void setReqContent(String reqContent) {
        this.reqContent = reqContent;
    }

    public String getRespContent() {
        return this.respContent;
    }

    public void setRespContent(String respContent) {
        this.respContent = respContent;
    }

    public String getRespMessage() {
        return this.respMessage;
    }

    public void setRespMessage(String respMessage) {
        this.respMessage = respMessage;
    }

    public Integer getRespCode() {
        return this.respCode;
    }

    public void setRespCode(Integer respCode) {
        this.respCode = respCode;
    }

    public Boolean getIsWebSocket() {
        return this.isWebSocket;
    }

    public void setIsWebSocket(Boolean isWebSocket) {
        this.isWebSocket = isWebSocket;
    }

    public String getAppPackage() {
        return this.appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
    }

    public String getAppVersion() {
        return this.appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAppVersionCode() {
        return this.appVersionCode;
    }

    public void setAppVersionCode(String appVersionCode) {
        this.appVersionCode = appVersionCode;
    }

    public String getNetType() {
        return this.netType;
    }

    public void setNetType(String netType) {
        this.netType = netType;
    }

    public int getRequestBodyOffset() {
        return this.requestBodyOffset;
    }

    public void setRequestBodyOffset(int requestBodyOffset) {
        this.requestBodyOffset = requestBodyOffset;
    }

    public int getResponseBodyOffset() {
        return this.responseBodyOffset;
    }

    public void setResponseBodyOffset(int responseBodyOffset) {
        this.responseBodyOffset = responseBodyOffset;
    }

    public void setRequestBodyOffset(Integer requestBodyOffset) {
        this.requestBodyOffset = requestBodyOffset;
    }

    public void setResponseBodyOffset(Integer responseBodyOffset) {
        this.responseBodyOffset = responseBodyOffset;
    }

    public String getResponseHeaders() {
        return this.responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }


}
