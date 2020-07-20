package cn.demo.appq.Interceptor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.github.megatronking.netbare.http.HttpIndexedInterceptor;
import com.github.megatronking.netbare.http.HttpInterceptor;
import com.github.megatronking.netbare.http.HttpInterceptorFactory;
import com.github.megatronking.netbare.http.HttpRequest;
import com.github.megatronking.netbare.http.HttpRequestChain;
import com.github.megatronking.netbare.http.HttpResponse;
import com.github.megatronking.netbare.http.HttpResponseChain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import cn.demo.appq.App;
import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.greendao.ReqEntityDao;
import cn.demo.appq.utils.DBManager;
import cn.demo.appq.utils.IOUtils;

public class Ainterceptor extends HttpIndexedInterceptor {


    private static final String TAG = "Ainterceptor";
    private static final boolean RECORD_REQUEST_BODY = true;

    public static HttpInterceptorFactory createFactory(Context context) {
        return new HttpInterceptorFactory() {
            @NonNull
            @Override
            public HttpInterceptor create() {
                return new Ainterceptor();
            }
        };
    }

    private SparseArray<ReqEntity> uid_appname_cache = new SparseArray<>();

    @Override
    protected void intercept(@NonNull HttpRequestChain chain, @NonNull ByteBuffer buffer, int index) throws IOException {
        HttpRequest request = chain.request();
        List<ReqEntity> reqEntities = DBManager.getInstance()
                .getReqEntityDao()
                .queryBuilder()
                .where(ReqEntityDao.Properties.SessionId.eq(request.id()))
                .list();
        if (reqEntities != null && reqEntities.size() > 0) {
            ReqEntity entity = reqEntities.get(0);
            if (RECORD_REQUEST_BODY) {
                String reqContent = entity.getReqContent();
                if (reqContent == null) {
                    entity.setRespContent(IOUtils.byteBuffer2String(buffer.asReadOnlyBuffer()));
                } else {
                    entity.setRespContent(reqContent + IOUtils.byteBuffer2String(buffer.asReadOnlyBuffer()));
                }
            }
            entity.setIndex(index);
            entity.setLength(entity.getLength() + buffer.limit());
            DBManager.getInstance().getReqEntityDao().update(entity);
        } else {
            String packagename = App.getProcessNameByUid(request.uid());
            ReqEntity reqEntity = new ReqEntity(
                    null,
                    request.id(),
                    AppUtils.getAppName(packagename),
                    packagename,
                    AppUtils.getAppVersionName(packagename),
                    String.valueOf(AppUtils.getAppVersionCode(packagename)),
                    request.url(),
                    request.host(),
                    request.port(),
                    index,
                    request.ip(),
                    request.protocol().toString(),
                    request.httpProtocol().toString(),
                    request.method().name(),
                    request.path(),
                    request.isHttps(),
                    request.time(),
                    request.uid(),
                    buffer.limit(),
                    request.streamId(),
                    GsonUtils.toJson(request.requestHeaders()),
                    "",
                    "",
                    RECORD_REQUEST_BODY ? IOUtils.byteBuffer2String(buffer.asReadOnlyBuffer()) : "",
                    null,
                    null,
                    null,
                    null,
                    NetworkUtils.getNetworkType().name());
            DBManager.getInstance().getReqEntityDao().insert(reqEntity);
        }
        chain.process(buffer);
    }


    @Override
    protected void intercept(@NonNull HttpResponseChain chain, @NonNull ByteBuffer buffer, int index) throws IOException {
        HttpResponse response = chain.response();
        List<ReqEntity> reqEntities = DBManager.getInstance()
                .getReqEntityDao()
                .queryBuilder()
                .where(ReqEntityDao.Properties.SessionId.eq(response.id()))
                .list();

        if (reqEntities != null && reqEntities.size() > 0) {
            ReqEntity entity = reqEntities.get(0);
            entity.setRespCode(response.code());
            if (RECORD_REQUEST_BODY) {
                String s = entity.getRespContent();
                if (s == null) {
                    entity.setRespContent(IOUtils.byteBuffer2String(buffer.asReadOnlyBuffer()));
                } else {
                    entity.setRespContent(s + IOUtils.byteBuffer2String(buffer.asReadOnlyBuffer()));
                }
            }
            entity.setRespMessage(response.message());
            entity.setIsWebSocket(response.isWebSocket());
            entity.setIndex(index);
            entity.setLength(entity.getLength() + buffer.limit());
            DBManager.getInstance().getReqEntityDao().update(entity);
        }

        chain.process(buffer);
    }
}
