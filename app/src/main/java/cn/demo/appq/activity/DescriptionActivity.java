package cn.demo.appq.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import com.blankj.utilcode.util.GsonUtils;

import java.util.Map;
import java.util.Objects;

import cn.demo.appq.R;
import cn.demo.appq.entity.ReqEntity;
import cn.demo.appq.utils.DBManager;

public class DescriptionActivity extends AppCompatActivity implements MenuItem.OnMenuItemClickListener, SwipeRefreshLayout.OnRefreshListener {


    private Handler mHandler;
    private AppBarLayout appbar;
    private Toolbar toolbar;
    private SwipeRefreshLayout swRfl;
    private ScrollView scrollView;
    private TextView tvReqHeader;
    private TextView tvReqHeaderValue;
    private TextView tvReqContent;
    private TextView tvReqContentValue;
    private TextView tvRespCode;
    private TextView tvRespHeader;
    private TextView tvRespHeaderValue;
    private TextView tvRespContent;
    private TextView tvRespContentValue;
    private Group groupResp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        findview();
        setSupportActionBar(toolbar);
        //左侧添加一个默认的返回图标
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        //设置返回键可用
        getSupportActionBar().setHomeButtonEnabled(true);
        newIntent(getIntent());

        swRfl.setOnRefreshListener(this);
    }

    private void findview() {
        appbar = (AppBarLayout) findViewById(R.id.appbar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        swRfl = (SwipeRefreshLayout) findViewById(R.id.sw_rfl);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        tvReqHeader = (TextView) findViewById(R.id.tv_req_header);
        tvReqHeaderValue = (TextView) findViewById(R.id.tv_req_header_value);
        tvReqContent = (TextView) findViewById(R.id.tv_req_content);
        tvReqContentValue = (TextView) findViewById(R.id.tv_req_content_value);
        tvRespCode = (TextView) findViewById(R.id.tv_resp_code);
        tvRespHeader = (TextView) findViewById(R.id.tv_resp_header);
        tvRespHeaderValue = (TextView) findViewById(R.id.tv_resp_header_value);
        tvRespContent = (TextView) findViewById(R.id.tv_resp_content);
        tvRespContentValue = (TextView) findViewById(R.id.tv_resp_content_value);
        groupResp = (Group) findViewById(R.id.group_resp);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        newIntent(intent);
    }

    private void newIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            return;
        }
        long id = intent.getExtras().getLong("id");
        ReqEntity entity = DBManager.getInstance().getReqEntityDao().loadByRowId(id);

        if (entity.getRespCode() == null) {
            if (mHandler == null) {
                //没返回则开启刷新，定时刷新数据展示
                mHandler = new Handler(Looper.getMainLooper());
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    newIntent(getIntent());
                }
            }, 500);
        } else {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }
        }
        update(entity);
    }

    private void update(ReqEntity entity) {
        String reqContent = entity.getReqContent();
        if (reqContent != null) {
            tvReqContentValue.setText(reqContent);
        }
        Map<Object, Object> map = GsonUtils.fromJson(entity.getRequestHeaders(), Map.class);
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<Object, Object> entry :
                map.entrySet()) {
            stringBuilder
                    .append(entry.getKey().toString())
                    .append(":")
                    .append(entry.getValue().toString())
                    .append("\n");
        }
        tvReqHeaderValue.setText(stringBuilder.toString());
        tvRespHeaderValue.setText(stringBuilder.toString());
        String respContent = entity.getRespContent();
        if (respContent != null) {
            tvRespContentValue.setText(respContent);
        }
        tvRespCode.setText("Resp Code:" + entity.getRespCode());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.desc_activity_menu, menu);
        menu.findItem(R.id.action_fanghzou).setOnMenuItemClickListener(this);
        menu.findItem(R.id.action_qianfan).setOnMenuItemClickListener(this);
        menu.findItem(R.id.action_nil).setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //Toolbar的事件---返回
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_fanghzou:
                break;
            case R.id.action_qianfan:
                break;
            case R.id.action_nil:
                break;
            default:
        }
        return true;
    }

    @Override
    public void onRefresh() {
        newIntent(getIntent());
        swRfl.setRefreshing(false);
    }
}
