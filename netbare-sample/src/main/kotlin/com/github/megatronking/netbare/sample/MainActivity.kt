package com.github.megatronking.netbare.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import com.github.megatronking.netbare.NetBare
import com.github.megatronking.netbare.NetBareConfig
import com.github.megatronking.netbare.NetBareListener
import com.github.megatronking.netbare.http.HttpInjectInterceptor
import com.github.megatronking.netbare.http.HttpInterceptorFactory
import com.github.megatronking.netbare.ssl.JKS
import java.io.IOException

class MainActivity : AppCompatActivity(), NetBareListener {

    companion object {
        private const val REQUEST_CODE_PREPARE = 1
    }

    private lateinit var mNetBare : NetBare

    private lateinit var mActionButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mNetBare = NetBare.get()

        mActionButton = findViewById(R.id.action)
        mActionButton.setOnClickListener {
            if (mNetBare.isActive) {
                mNetBare.stop()
            } else{
                prepareNetBare()
            }
        }

        // 监听NetBare服务的启动和停止
        mNetBare.registerNetBareListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mNetBare.unregisterNetBareListener(this)
        mNetBare.stop()
    }

    override fun onServiceStarted() {
        runOnUiThread {
            mActionButton.setText(R.string.stop_netbare)
        }
    }

    override fun onServiceStopped() {
        // 启动NetBare服务
        mNetBare.start(NetBareConfig.defaultHttpConfig(App.getInstance().getJSK(),
                interceptorFactories()))
        runOnUiThread {
            mActionButton.setText(R.string.start_netbare)
        }
    }

    private fun prepareNetBare() {
        // 安装自签证书
        if (!JKS.isInstalled(this, App.JSK_ALIAS)) {
            try {
                JKS.install(this, App.JSK_ALIAS, App.JSK_ALIAS)
            } catch(e : IOException) {
                // 安装失败
            }
            return
        }
        // 配置VPN
        val intent = NetBare.get().prepare()
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_PREPARE)
            return
        }
        // 启动NetBare服务
        mNetBare.start(NetBareConfig.defaultHttpConfig(App.getInstance().getJSK(),
                interceptorFactories()))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PREPARE) {
            prepareNetBare()
        }
    }

    private fun interceptorFactories() : List<HttpInterceptorFactory> {
        // 拦截器范例1：打印请求url
        val interceptor1 = HttpUrlPrintInterceptor.createFactory()
        // 注入器范例1：替换百度首页logo
        val injector1 = HttpInjectInterceptor.createFactory(BaiduLogoInjector())
        // 注入器范例2：修改发朋友圈定位
        val injector2 = HttpInjectInterceptor.createFactory(WechatLocationInjector())
        // 可以添加其它的拦截器，注入器
        // ...
        return listOf(interceptor1, injector1, injector2)
    }


}
