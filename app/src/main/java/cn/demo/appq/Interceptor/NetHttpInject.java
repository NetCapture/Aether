package cn.demo.appq.Interceptor;

import android.util.Log;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.github.megatronking.netbare.http.HttpBody;
import com.github.megatronking.netbare.http.HttpRequest;
import com.github.megatronking.netbare.http.HttpRequestHeaderPart;
import com.github.megatronking.netbare.http.HttpResponse;
import com.github.megatronking.netbare.http.HttpResponseHeaderPart;
import com.github.megatronking.netbare.injector.HttpInjector;
import com.github.megatronking.netbare.injector.InjectorCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import cn.demo.appq.App;
import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.greendao.ReqEntityDao;
import cn.demo.appq.utils.DBManager;
import cn.demo.appq.utils.IOUtils;

public class NetHttpInject implements HttpInjector {
    private static final String TAG = "TestHttpInject";
    private static final boolean RECORD_REQUEST_BODY = true;
    // SQLite单行最大容量约2MB，使用1.5MB作为安全阈值
    private static final int MAX_CONTENT_SIZE = 1536 * 1024;

    @Override
    public boolean sniffRequest(@NonNull HttpRequest request) {
        return true;
    }

    @Override
    public boolean sniffResponse(@NonNull HttpResponse response) {
        return true;
    }

    @Override
    public void onRequestInject(@NonNull HttpRequestHeaderPart header, @NonNull InjectorCallback callback) throws IOException {
        callback.onFinished(header);
    }

    @Override
    public void onResponseInject(@NonNull HttpResponseHeaderPart header, @NonNull InjectorCallback callback) throws IOException {
        callback.onFinished(header);
    }

    @Override
    public void onRequestInject(@NonNull HttpRequest request, @NonNull HttpBody body, @NonNull InjectorCallback callback) throws IOException {
        List<ReqEntity> reqEntities = DBManager.getInstance()
                .getReqEntityDao()
                .queryBuilder()
                .where(ReqEntityDao.Properties.SessionId.eq(request.id()))
                .list();
        if (reqEntities != null && reqEntities.size() > 0) {
            ReqEntity entity = reqEntities.get(0);
            if (RECORD_REQUEST_BODY){
                if (entity.getReqContent() == null) {
                    entity.setReqContent("");
                }
                ByteBuffer prebuffer = IOUtils.string2ByteBuffer(entity.getReqContent());
                ByteBuffer nextBuffer = body.toBuffer().asReadOnlyBuffer();
                ByteBuffer newBuffer = ByteBuffer.allocate(prebuffer.limit() + nextBuffer.limit());
                newBuffer.put(prebuffer);
                newBuffer.put(nextBuffer);
                // 始终统计流量大小，无论是否记录内容
                entity.setLength(entity.getLength() + nextBuffer.limit());
                if(newBuffer.limit() >= MAX_CONTENT_SIZE){
                    // 数据量超过SQLite单行最大容量，不再记录，但允许请求继续
                    Log.w(TAG, "Request content exceeds maximum size, skipping record: " + request.id());
                    DBManager.getInstance().getReqEntityDao().update(entity);
                } else {
                    entity.setReqContent(IOUtils.byteBuffer2String(newBuffer));
                    DBManager.getInstance().getReqEntityDao().update(entity);
                }
            }
        } else {
            String packagename = App.getProcessNameByUid(request.uid());
            String appName = request.appName() != null ? request.appName() : AppUtils.getAppName(packagename);
            Log.d(TAG, "Create ReqEntity: uid=" + request.uid() + ", appName=" + appName + ", packagename=" + packagename);
            ReqEntity reqEntity = new ReqEntity(
                    null,
                    request.id(),
                    appName,
                    packagename,
                    AppUtils.getAppVersionName(packagename),
                    String.valueOf(AppUtils.getAppVersionCode(packagename)),
                    request.url(),
                    request.host(),
                    request.port(),
                    0,
                    request.ip(),
                    request.protocol().toString(),
                    request.httpProtocol().toString(),
                    request.method().name(),
                    request.path(),
                    request.isHttps(),
                    request.time(),
                    request.uid(),
                    body.toBuffer().limit(),
                    request.streamId(),
                    GsonUtils.toJson(request.requestHeaders()),
                    null,
                    "",
                    "",
                    RECORD_REQUEST_BODY ? IOUtils.byteBuffer2String(body.toBuffer().asReadOnlyBuffer()) : null,
                    null,
                    null,
                    null,
                    null,
                    NetworkUtils.getNetworkType().name(),
                    request.requestBodyOffset(),
                    0);
            DBManager.getInstance().getReqEntityDao().insert(reqEntity);
        }
        callback.onFinished(body);
    }

    @Override
    public void onResponseInject(@NonNull HttpResponse response, @NonNull HttpBody body, @NonNull InjectorCallback callback) throws IOException {
        List<ReqEntity> reqEntities = DBManager.getInstance()
                .getReqEntityDao()
                .queryBuilder()
                .where(ReqEntityDao.Properties.SessionId.eq(response.id()))
                .list();

        if (reqEntities != null && reqEntities.size() > 0) {
            ReqEntity entity = reqEntities.get(0);
            entity.setRespCode(response.code());
            entity.setResponseHeaders(GsonUtils.toJson(response.responseHeaders()));
            entity.setResponseBodyOffset(response.responseBodyOffset());
            entity.setRequestBodyOffset(response.requestBodyOffset());
            if (RECORD_REQUEST_BODY) {
                if (entity.getRespContent() == null) {
                    entity.setRespContent("");
                }
                ByteBuffer prebuffer = IOUtils.string2ByteBuffer(entity.getRespContent());
                ByteBuffer nextBuffer = body.toBuffer().asReadOnlyBuffer();
                ByteBuffer newBuffer = ByteBuffer.allocate(prebuffer.limit() + nextBuffer.limit());
                newBuffer.put(prebuffer);
                newBuffer.put(nextBuffer);
                if(newBuffer.limit() >= MAX_CONTENT_SIZE){
                    // 数据量超过SQLite单行最大容量，不再记录，但允许响应继续
                    Log.w(TAG, "Response content exceeds maximum size, skipping record: " + response.id());
                } else {
                    entity.setRespContent(IOUtils.byteBuffer2String(newBuffer));
                }
            }
            entity.setRespMessage(response.message());
            entity.setIsWebSocket(response.isWebSocket());
            entity.setIndex(0);
            entity.setLength(entity.getLength() + body.toBuffer().limit());
            DBManager.getInstance().getReqEntityDao().update(entity);
        }
        callback.onFinished(body);
    }

    @Override
    public void onRequestFinished(@NonNull HttpRequest request) {
        Log.d(TAG, "onRequestFinished: " + request.uid());
    }

    @Override
    public void onResponseFinished(@NonNull HttpResponse response) {
        Log.d(TAG, "onRequestFinished: " + response.uid());
    }
}
