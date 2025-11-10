package cn.demo.appq.activity;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.List;

import cn.demo.appq.R;
import cn.demo.appq.adapter.UrlAdapter;
import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.presenter.NetBarePresenter;
import cn.demo.appq.utils.DBManager;
import cn.demo.appq.view.NetBareView;
import cn.demo.appq.App;

public class VPNActivity extends AppCompatActivity implements View.OnClickListener {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v_p_n);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        netBarePresenter.prepareJks();
        netBarePresenter.prepareVpn();
        updateUIByVpnActive();
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
        int viewId = v.getId();
        
        if (viewId == R.id.menu_delete) {
            DBManager.getReqEntityDao().deleteAll();
            netBarePresenter.queryByUrl(null);
        } else if (viewId == R.id.menu_stop) {
            netBarePresenter.stopVpn();
        } else if (viewId == R.id.menu_start) {
            // 确保证书已生成
            if (!netBarePresenter.isJksFileGenerated()) {
                // 如果证书未生成，先生成证书
                netBarePresenter.prepareJks();

                // 显示等待提示
                Snackbar.make(v, "正在生成证书，请稍候...", Snackbar.LENGTH_LONG)
                        .setAction("重试", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 重新尝试启动VPN
                                startVpnWithCheck();
                            }
                        }).show();

                // 等待证书生成
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 等待证书生成完成
                            while (!netBarePresenter.isJksFileGenerated()) {
                                Thread.sleep(500);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // 证书生成完成，启动VPN
                                    startVpnWithCheck();
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else {
                // 证书已生成，直接启动VPN
                startVpnWithCheck();
            }
        } else if (viewId == R.id.menu_setting) {
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
        } else if (viewId == R.id.menu_setting_vpn) {
            netBarePresenter.prepareVpn();
        } else if (viewId == R.id.menu_info) {
            Snackbar.make(v, "See http://" + NetworkUtils.getIpAddressByWifi() + ":8080", Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    }).show();
        } else if (viewId == R.id.menu_install_cert) {
            installCertificate();
        } else {
            throw new IllegalStateException("Unexpected value: " + viewId);
        }
    }

    /**
     * 安装证书到 DOWNLOAD 目录并跳转到设置页面
     */
    private void installCertificate() {
        // 检查存储权限
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求权限
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            return;
        }

        // 检查证书是否已生成
        if (!netBarePresenter.isJksFileGenerated()) {
            // 如果证书未先生成，先生成证书
            netBarePresenter.prepareJks();
            // 等待证书生成完成
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 等待证书文件生成
                        while (!netBarePresenter.isJksFileGenerated()) {
                            Thread.sleep(500);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                copyCertToDownloadAndOpenSettings();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            copyCertToDownloadAndOpenSettings();
        }
    }

    /**
     * 复制证书到 DOWNLOAD 目录并跳转到设置页面
     */
    private void copyCertToDownloadAndOpenSettings() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 先尝试使用系统KeyChain安装（推荐方式）
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // 使用KeyChain系统安装
                                netBarePresenter.installJks();
                                Toast.makeText(VPNActivity.this,
                                    "请在弹出的系统对话框中安装证书",
                                    Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                // 如果KeyChain安装失败，使用文件保存方式
                                saveCertToFile();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VPNActivity.this, "安装证书失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 保存证书到文件（备用方案）
     */
    private void saveCertToFile() {
        try {
            // 获取证书文件
            File cacheDir = getApplicationContext().getCacheDir();
            File pemFile = new File(cacheDir, App.JSK_ALIAS + ".pem");
            File p12File = new File(cacheDir, App.JSK_ALIAS + ".p12");
            File jksFile = new File(cacheDir, App.JSK_ALIAS + ".jks");

            if (!pemFile.exists()) {
                Toast.makeText(VPNActivity.this, "证书文件未找到，请稍后重试", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存到私有存储目录
            File appDir = new File(getApplicationContext().getExternalFilesDir(null), "certificates");
            if (!appDir.exists()) {
                boolean created = appDir.mkdirs();
                if (!created) {
                    Toast.makeText(VPNActivity.this, "无法创建目录", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 保存多种格式的证书文件
            File destPem = new File(appDir, "Aether_CA.pem");
            File destCrt = new File(appDir, "Aether_CA.crt");
            File destP12 = new File(appDir, "Aether_CA.p12");
            File destJks = new File(appDir, "Aether_CA.jks");

            // 复制文件
            copyFileUsingStream(pemFile, destPem);
            copyFileUsingStream(pemFile, destCrt); // CRT 格式
            if (p12File.exists()) {
                copyFileUsingStream(p12File, destP12);
            }
            if (jksFile.exists()) {
                copyFileUsingStream(jksFile, destJks);
            }

            final String savedPath = "证书已保存到：\n" + appDir.getAbsolutePath() + "\n\n" +
                    "包含以下格式：\n" +
                    "- Aether_CA.pem (PEM格式)\n" +
                    "- Aether_CA.crt (CRT格式)\n" +
                    "- Aether_CA.p12 (PKCS12格式，如存在)\n" +
                    "- Aether_CA.jks (JKS格式，如存在)\n\n" +
                    "请使用任意一种格式安装证书。";

            // 显示结果
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new android.app.AlertDialog.Builder(VPNActivity.this)
                        .setTitle("证书已保存")
                        .setMessage(savedPath)
                        .setPositiveButton("确定", null)
                        .setNeutralButton("打开文件夹", new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openFileManager(appDir);
                            }
                        })
                        .show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VPNActivity.this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 打开文件管理器
     */
    private void openFileManager(File dir) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(android.net.Uri.fromFile(dir), "resource/folder");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 使用流复制文件
     */
    private void copyFileUsingStream(File source, File dest) throws Exception {
        java.io.InputStream is = null;
        java.io.OutputStream os = null;
        try {
            is = new java.io.FileInputStream(source);
            os = new java.io.FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) is.close();
            if (os != null) os.close();
        }
    }

    /**
     * 启动VPN（包含检查和错误处理）
     */
    private void startVpnWithCheck() {
        try {
            // 检查证书是否已生成
            if (!netBarePresenter.isJksFileGenerated()) {
                Toast.makeText(this, "证书未生成完成，请稍后重试", Toast.LENGTH_SHORT).show();
                return;
            }

            // 配置VPN（使用startActivityForResult等待结果）
            netBarePresenter.prepareVpn();

            // 注意：prepareVpn 是异步的，实际的启动会在 onActivityResult 中处理
            Toast.makeText(this, "正在配置VPN...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "启动VPN失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PREPARE) {
            if (resultCode == RESULT_OK) {
                // VPN权限配置成功，现在可以启动VPN
                try {
                    netBarePresenter.startVpn();
                    netBarePresenter.queryByUrl(null);
                    Toast.makeText(this, "VPN已启动", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "启动VPN失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "VPN权限配置被取消", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 添加REQUEST_CODE常量
    private static final int REQUEST_CODE_PREPARE = 1;

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，重新执行安装证书操作
                installCertificate();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要存储权限才能保存证书到 DOWNLOAD 目录", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
