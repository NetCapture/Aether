package cn.demo.appq.activity;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.github.clans.fab.FloatingActionMenu;
import com.github.megatronking.netbare.NetBareListener;
import com.github.megatronking.netbare.ssl.JKS;
import com.google.android.material.snackbar.Snackbar;

import android.util.Log;

import java.io.File;
import java.util.List;

import cn.demo.appq.R;
import cn.demo.appq.adapter.UrlAdapter;
import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.presenter.NetBarePresenter;
import cn.demo.appq.utils.CertificateSaver;
import cn.demo.appq.utils.DBManager;
import cn.demo.appq.utils.TrafficHttpServer;
import cn.demo.appq.view.NetBareView;
import cn.demo.appq.App;
import cn.demo.appq.vpn.JksManager;
import cn.demo.appq.vpn.VpnServiceManager;

public class VPNActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_PREPARE = 1;
    private static final int REQUEST_CODE_INSTALL_CERT = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private NetBarePresenter netBarePresenter;
    private FloatingActionMenu fab;
    private MenuItem searchItem;
    private RecyclerView rv_logs;
    private UrlAdapter adapter;
    private TextView tv_notice;
    private MenuItem update;
    private SwipeRefreshLayout srl_ref;
    private View menu_setting;
    private View menu_delete;
    private View menu_stop;
    private View menu_start;
    private View menu_info;
    private View menu_setting_vpn;
    private View menu_install_cert;

    // 证书保存工具
    private CertificateSaver certificateSaver;

    // VPN服务管理器
    private VpnServiceManager vpnServiceManager;

    // 流量统计HTTP服务器
    private TrafficHttpServer trafficHttpServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v_p_n);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化证书保存工具
        certificateSaver = new CertificateSaver(this);

        // 初始化VPN服务管理器
        vpnServiceManager = VpnServiceManager.getInstance(getApplicationContext());

        // 初始化流量统计HTTP服务器
        trafficHttpServer = TrafficHttpServer.getInstance();

        netBarePresenter = new NetBarePresenter(this, new NetBareListener() {
            @Override
            public void onServiceStarted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_notice.setVisibility(View.GONE);
                        srl_ref.setVisibility(View.VISIBLE);
                        Toast.makeText(VPNActivity.this, "Vpn Service Started", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onServiceStopped() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        srl_ref.setVisibility(View.GONE);
                        tv_notice.setVisibility(View.VISIBLE);
                        Toast.makeText(VPNActivity.this, "Vpn Service Stopped", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, new NetBareView() {
            @Override
            public void onQueryReqlogResult(List<ReqEntity> reqEntities) {
                adapter.submitList(reqEntities);
                srl_ref.setRefreshing(false);
            }
        });
        netBarePresenter.start();
        findView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 确保证书已生成
        ensureJksReady();
        updateUIByVpnActive();
    }

    /**
     * 确保证书已准备好
     */
    private void ensureJksReady() {
        JksManager jksManager = JksManager.getInstance(getApplicationContext());
        if (!jksManager.isReady()) {
            // 异步生成证书
            jksManager.initializeAsync(
                    App.JSK_ALIAS,
                    App.JSK_ALIAS.toCharArray(),
                    "Aether CA",
                    "Aether",
                    "Aether Tool",
                    "Aether",
                    "Aether Tool",
                    new JksManager.JksCallback() {
                        @Override
                        public void onSuccess(JKS jks) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(VPNActivity.this, "证书生成成功", Toast.LENGTH_SHORT).show();
                                    updateUIByVpnActive();
                                }
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(VPNActivity.this, "证书生成失败: " + error, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
        }
    }

    private void updateUIByVpnActive() {
        if (netBarePresenter.isActive()) {
            tv_notice.setVisibility(View.GONE);
            srl_ref.setVisibility(View.VISIBLE);
            menu_start.setVisibility(View.GONE);
            if(fab.isOpened()){
                menu_stop.setVisibility(View.VISIBLE);
            }
        } else {
            tv_notice.setVisibility(View.VISIBLE);
            srl_ref.setVisibility(View.GONE);
          if(fab.isOpened()){
              menu_start.setVisibility(View.VISIBLE);
          }
            menu_stop.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (fab.isOpened()) {
            fab.close(true);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void findView() {
        rv_logs = findViewById(R.id.rv_logs);
        tv_notice = findViewById(R.id.tv_notice);
        srl_ref = findViewById(R.id.srl_ref);
        srl_ref.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                netBarePresenter.queryByUrl(null);
            }
        });
        rv_logs.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
        adapter = new UrlAdapter();
        rv_logs.setAdapter(adapter);
        fab = findViewById(R.id.fab);
        fab.setClosedOnTouchOutside(true);
        fab.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                if (netBarePresenter.isJksFileGenerated()) {
                    menu_setting.setVisibility(View.GONE);
                } else {
                    menu_setting.setVisibility(View.VISIBLE);
                }
                if (opened) {
                    updateUIByVpnActive();
                }
            }
        });
        menu_delete = findViewById(R.id.menu_delete);
        menu_stop = findViewById(R.id.menu_stop);
        menu_start = findViewById(R.id.menu_start);
        menu_setting = findViewById(R.id.menu_setting);
        menu_info = findViewById(R.id.menu_info);
        menu_setting_vpn = findViewById(R.id.menu_setting_vpn);
        menu_install_cert = findViewById(R.id.menu_install_cert);
        menu_setting_vpn.setOnClickListener(this);
        menu_delete.setOnClickListener(this);
        menu_stop.setOnClickListener(this);
        menu_start.setOnClickListener(this);
        menu_info.setOnClickListener(this);
        menu_setting.setOnClickListener(this);
        menu_install_cert.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        fab.close(true);
        switch (v.getId()) {
            case R.id.menu_delete:
                DBManager.getInstance().getReqEntityDao().deleteAll();
                netBarePresenter.queryByUrl(null);
                break;
            case R.id.menu_stop:
                stopVpn();
                break;
            case R.id.menu_start:
                // 启动VPN前需要先请求权限
                requestVpnPermission();
                break;
            case R.id.menu_setting:
                if (!netBarePresenter.isJksFileGenerated()) {
                    // 首次点击：生成证书
                    netBarePresenter.prepareJks();
                    Snackbar.make(v, "证书已生成，请再次点击安装CA证书", Snackbar.LENGTH_LONG)
                            .setAction("知道了", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // 用户确认
                                }
                            }).show();
                } else {
                    // 再次点击：安装证书
                    netBarePresenter.installJks();
                    Snackbar.make(v, "请在弹出的系统界面中安装证书", Snackbar.LENGTH_LONG)
                            .setAction("知道了", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // 用户确认
                                }
                            }).show();
                }
                break;
            case R.id.menu_setting_vpn:
                requestVpnPermission();
                break;
            case R.id.menu_info:
                // 启动流量统计Web服务器
                openTrafficStatisticsWeb();
                break;
            case R.id.menu_install_cert:
                installCertificateToDownloads();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }

    /**
     * 安装证书到Downloads目录（修复版本）
     */
    private void installCertificateToDownloads() {
        // Android 10+ 使用MediaStore API，不需要特殊权限
        // Android 9及以下需要WRITE_EXTERNAL_STORAGE权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // 确保证书已生成
        if (!netBarePresenter.isJksFileGenerated()) {
            Toast.makeText(this, "正在生成证书，请稍候...", Toast.LENGTH_SHORT).show();
            netBarePresenter.prepareJks();

            // 等待证书生成
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int attempts = 0;
                        while (!netBarePresenter.isJksFileGenerated() && attempts < 10) {
                            Thread.sleep(500);
                            attempts++;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (netBarePresenter.isJksFileGenerated()) {
                                    saveCertificateToDownloads();
                                } else {
                                    Toast.makeText(VPNActivity.this, "证书生成失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            saveCertificateToDownloads();
        }
    }

    /**
     * 使用CertificateSaver保存证书到Downloads
     */
    private void saveCertificateToDownloads() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 获取证书文件
                    File cacheDir = getApplicationContext().getCacheDir();
                    File pemFile = new File(cacheDir, App.JSK_ALIAS + ".pem");
                    File jksFile = new File(cacheDir, App.JSK_ALIAS + ".jks");

                    if (!pemFile.exists()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(VPNActivity.this, "证书文件未找到", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    // 读取PEM证书数据
                    byte[] certData = new byte[(int) pemFile.length()];
                    java.io.FileInputStream fis = new java.io.FileInputStream(pemFile);
                    fis.read(certData);
                    fis.close();

                    // 使用CertificateSaver保存
                    CertificateSaver.SaveResult result = certificateSaver.saveCertificateToDownloads(
                            certData,
                            "Aether_CA",
                            "PEM"
                    );

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result.isSuccess) {
                                // 成功保存证书，显示操作指引
                                String message = "证书已保存到:\n" + result.savedFilePath +
                                        "\n\n即将跳转到安全设置页面，请按以下步骤操作：\n" +
                                        "1. 点击'从存储设备安装证书'\n" +
                                        "2. 选择名为 'Aether_CA.pem' 的证书文件\n" +
                                        "3. 按提示完成安装";

                                new android.app.AlertDialog.Builder(VPNActivity.this)
                                    .setTitle("证书已保存")
                                    .setMessage(message)
                                    .setPositiveButton("知道了", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            try {
                                                // 跳转到"加密与凭证"设置页面
                                                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                                                startActivity(intent);
                                            } catch (Exception e) {
                                                Toast.makeText(VPNActivity.this, "无法打开设置页面: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    })
                                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            // 如果用户点击外部区域关闭，也尝试跳转到设置页面
                                            try {
                                                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                                                startActivity(intent);
                                            } catch (Exception e) {
                                                Toast.makeText(VPNActivity.this, "无法打开设置页面: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    })
                                    .setCancelable(true)
                                    .show();
                            } else {
                                Toast.makeText(VPNActivity.this, "保存失败: " + result.errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VPNActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 请求VPN权限
     */
    private void requestVpnPermission() {
        VpnServiceManager.getInstance(getApplicationContext())
            .prepareVpnPermission(this);
    }

    /**
     * 启动VPN（修复版本）
     */
    private void startVpn() {
        // 检查证书
        JksManager jksManager = JksManager.getInstance(getApplicationContext());
        if (!jksManager.isReady() || jksManager.getJksSafe() == null) {
            Toast.makeText(this, "证书未准备好，正在生成...", Toast.LENGTH_SHORT).show();
            ensureJksReady();
            return;
        }

        // 使用VpnServiceManager启动VPN
        try {
            Log.d("VPNActivity", "Starting VPN...");
            vpnServiceManager.startVpn();
            if (netBarePresenter != null) {
                netBarePresenter.queryByUrl(null);
            }
            Toast.makeText(this, "VPN已启动", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("VPNActivity", "启动VPN失败", e);
            Toast.makeText(this, "启动VPN失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 停止VPN
     */
    private void stopVpn() {
        try {
            vpnServiceManager.stopVpn();
            netBarePresenter.stopVpn();
            Toast.makeText(this, "VPN已停止", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "停止VPN失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PREPARE) {
            // 处理VPN权限申请结果
            VpnServiceManager.getInstance(getApplicationContext())
                .handlePermissionResult(resultCode);

            if (resultCode == android.app.Activity.RESULT_OK) {
                // 权限申请成功，等待一下再启动VPN
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startVpn();
                    }
                }, 500);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，重新执行安装证书操作
                installCertificateToDownloads();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要存储权限才能保存证书到Downloads目录", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 打开流量统计Web界面
     */
    private void openTrafficStatisticsWeb() {
        try {
            // 启动HTTP服务器
            if (!trafficHttpServer.isRunning()) {
                trafficHttpServer.start();
            }

            String serverUrl = trafficHttpServer.getServerUrl();

            // 显示访问信息对话框
            new android.app.AlertDialog.Builder(VPNActivity.this)
                .setTitle("流量统计Web界面")
                .setMessage("Web服务器已启动！\n\n" +
                        "访问地址：" + serverUrl + "\n\n" +
                        "使用说明：\n" +
                        "1. 确保手机和电脑在同一WiFi网络\n" +
                        "2. 在电脑浏览器中打开上述地址\n" +
                        "3. 可以查看详细的流量统计排行\n\n" +
                        "功能特色：\n" +
                        "• 按应用查看流量排行\n" +
                        "• 按域名查看流量排行\n" +
                        "• 最近请求记录\n" +
                        "• 实时数据统计")
                .setPositiveButton("在浏览器中打开", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            // 尝试跳转到浏览器
                            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(serverUrl));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(VPNActivity.this, "无法打开浏览器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("仅启动服务器", null)
                .setNeutralButton("复制地址", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 复制地址到剪贴板
                        android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) {
                            android.content.ClipData clip = android.content.ClipData.newPlainText("Traffic Stats URL", serverUrl);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(VPNActivity.this, "地址已复制到剪贴板", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();

        } catch (Exception e) {
            Log.e("VPNActivity", "Failed to start traffic statistics web server", e);
            Toast.makeText(this, "启动Web服务器失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止HTTP服务器
        if (trafficHttpServer != null && trafficHttpServer.isRunning()) {
            trafficHttpServer.stop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bottom_nav_menu, menu);
        searchItem = menu.findItem(R.id.search);
        update = menu.findItem(R.id.update);
        update.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                netBarePresenter.queryByUrl(null);
                return true;
            }
        });

        // 添加空指针检查
        if (searchItem != null) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            final SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null && searchManager != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                searchView.setFocusable(false);
                searchView.setIconifiedByDefault(true);
                searchView.setMaxWidth(Integer.MAX_VALUE);
                searchView.setQueryHint("请输入URL关键字...");
                searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            netBarePresenter.resetCurrentQuery();
                        }
                    }
                });
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        netBarePresenter.queryByUrl(query);
                        KeyboardUtils.hideSoftInput(searchView);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        netBarePresenter.queryByUrl(newText);
                        return true;
                    }
                });
            }
        }
        return true;
    }

}
