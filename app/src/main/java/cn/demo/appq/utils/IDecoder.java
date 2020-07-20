package cn.demo.appq.utils;

import cn.demo.appq.entity.ReqEntity;

public interface IDecoder {
    ReqEntity decode(ReqEntity entity);
}
