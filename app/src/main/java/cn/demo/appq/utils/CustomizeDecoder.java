package cn.demo.appq.utils;

import com.blankj.utilcode.util.EncodeUtils;

import cn.demo.appq.entity.ReqEntity;

public class CustomizeDecoder implements IDecoder {
    @Override
    public ReqEntity decode(ReqEntity entity) {
        try {
            entity.setReqContent(EncodeUtils.urlDecode(entity.getReqContent()));
            entity.setRespContent(EncodeUtils.urlDecode(entity.getRespContent()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entity;
    }
}
