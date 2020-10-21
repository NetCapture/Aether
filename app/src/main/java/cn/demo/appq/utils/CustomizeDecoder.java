package cn.demo.appq.utils;

import com.blankj.utilcode.util.EncodeUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.JsonUtils;

import java.util.List;
import java.util.Map;

import cn.demo.appq.entity.ReqEntity;

public class CustomizeDecoder implements IDecoder {
    @Override
    public ReqEntity decode(ReqEntity entity) {
        try {
            entity.setReqContent(JsonUtils.formatJson(
                    EncodeUtils.urlDecode(new String(Base64Utils.decode(entity.getReqContent())))
                    , 2));
            byte[] respData = Base64Utils.decode(entity.getRespContent());

            Map<String, List<String>> map = GsonUtils.fromJson(entity.getResponseHeaders(), Map.class);
            if (map.containsKey("Content-Encoding")) {
                String encodeType = map.get("Content-Encoding").get(0);
                if (encodeType.toLowerCase().equals("gzip")) {
                    String s = ZLibUtils.uncompress(respData);
                    entity.setRespContent(
                            JsonUtils.formatJson(
                                    EncodeUtils.urlDecode(s), 2));
                } else {
                    entity.setRespContent(
                            EncodeUtils.urlDecode(
                                    JsonUtils.formatJson(new String(respData), 2)));
                }
            } else {
                entity.setRespContent(
                        EncodeUtils.urlDecode(
                                JsonUtils.formatJson(new String(respData), 2)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entity;
    }

}
