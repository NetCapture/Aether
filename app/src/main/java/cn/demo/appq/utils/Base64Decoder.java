package cn.demo.appq.utils;

import com.blankj.utilcode.util.EncodeUtils;

import cn.demo.appq.entity.ReqEntity;

public class Base64Decoder implements IDecoder {
    @Override
    public ReqEntity decode(ReqEntity entity) {
        try {
            String reqContent = new String(EncodeUtils.base64Decode(entity.getReqContent()));
            entity.setReqContent(reqContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String respContent = new String(EncodeUtils.base64Decode(entity.getRespContent()));
            entity.setRespContent(respContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entity;
    }
}
