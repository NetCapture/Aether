package cn.demo.appq.utils;

import cn.demo.appq.entity.ReqEntity;

public class CustomizeDecoder implements IDecoder {
    @Override
    public ReqEntity decode(ReqEntity entity) {
        // TODO: 2020/7/23 自定义实现
        return entity;
    }
}
