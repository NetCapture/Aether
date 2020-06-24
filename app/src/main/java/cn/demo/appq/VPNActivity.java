package cn.demo.appq;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.github.megatronking.netbare.NetBareListener;

import java.util.List;

import cn.demo.appq.adapter.UrlAdapter;
import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.utils.DBManager;
import cn.demo.appq.view.NetBareView;

public class VPNActivity extends AppCompatActivity implements View.OnClickListener {

    private NetBarePresenter netBarePresenter;
    private FloatingActionMenu fab;
    private MenuItem searchItem;
    private RecyclerView rv_logs;
    private UrlAdapter adapter;
    private TextView tv_notice;
    private MenuItem update;
    private SwipeRefreshLayout srl_ref;

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
                netBarePresenter.queryByUrl(null);
                break;
            case R.id.menu_stop:
                netBarePresenter.stopVpn();
                break;
            case R.id.menu_start:
                netBarePresenter.startVpn();
                netBarePresenter.queryByUrl(null);
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
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setFocusable(false);
        searchView.setIconifiedByDefault(true);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint("请输入URL关键字...");

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
        return true;
    }

}
