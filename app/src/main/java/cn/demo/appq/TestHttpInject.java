package cn.demo.appq;

import android.support.annotation.NonNull;
import android.util.Log;

import com.github.megatronking.netbare.http.HttpBody;
import com.github.megatronking.netbare.http.HttpRequest;
import com.github.megatronking.netbare.http.HttpRequestHeaderPart;
import com.github.megatronking.netbare.http.HttpResponse;
import com.github.megatronking.netbare.http.HttpResponseHeaderPart;
import com.github.megatronking.netbare.injector.HttpInjector;
import com.github.megatronking.netbare.injector.InjectorCallback;

import java.io.IOException;

public class TestHttpInject implements HttpInjector {
    private static final String TAG = "TestHttpInject";

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
        Log.d(TAG, "onRequestInject: " + request.uid());
        callback.onFinished(body);
    }

    @Override
    public void onResponseInject(@NonNull HttpResponse response, @NonNull HttpBody body, @NonNull InjectorCallback callback) throws IOException {
        Log.d(TAG, "onResponseInject: " + response.uid());
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
