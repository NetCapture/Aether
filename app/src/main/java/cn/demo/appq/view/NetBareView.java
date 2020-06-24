package cn.demo.appq.view;

import java.util.List;

import cn.demo.appq.entity.ReqEntity;

public interface NetBareView {
 void onQueryReqlogResult(List<ReqEntity> reqEntities);
}
