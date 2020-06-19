package cn.demo.appq;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.blankj.utilcode.util.NetworkUtils;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.github.megatronking.netbare.NetBareListener;

import cn.demo.appq.utils.DBManager;

public class VPNActivity extends AppCompatActivity implements View.OnClickListener {

    private NetBarePresenter netBarePresenter;
    private FloatingActionMenu fab;

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
                        Toast.makeText(VPNActivity.this, "Vpn Service Started", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onServiceStopped() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(VPNActivity.this, "Vpn Service Stopped", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        netBarePresenter.start();

        fab = findViewById(R.id.fab);

        FloatingActionButton menu_delete = findViewById(R.id.menu_delete);
        FloatingActionButton menu_stop = findViewById(R.id.menu_stop);
        FloatingActionButton menu_start = findViewById(R.id.menu_start);
        FloatingActionButton menu_setting = findViewById(R.id.menu_setting);
        FloatingActionButton menu_info = findViewById(R.id.menu_info);
        FloatingActionButton menu_setting_vpn = findViewById(R.id.menu_setting_vpn);
        menu_setting_vpn.setOnClickListener(this);
        menu_delete.setOnClickListener(this);
        menu_stop.setOnClickListener(this);
        menu_start.setOnClickListener(this);
        menu_info.setOnClickListener(this);
        menu_setting.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        fab.close(true);
        switch (v.getId()) {
            case R.id.menu_delete:
                DBManager.getInstance().getReqEntityDao().deleteAll();
                break;
            case R.id.menu_stop:
                netBarePresenter.stopVpn();
                break;
            case R.id.menu_start:
                netBarePresenter.startVpn();
                break;
            case R.id.menu_setting:
                netBarePresenter.prepareJks();
                break;
            case R.id.menu_setting_vpn:
                netBarePresenter.prepareVpn();
                break;
            case R.id.menu_info:
                Snackbar.make(v, "See http://" + NetworkUtils.getIpAddressByWifi() + ":8080", Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        }).show();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }
}
