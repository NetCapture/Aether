package cn.demo.appq.utils;

import cn.demo.appq.entity.ReqEntity;

public class DecoderHandler {
    public static final IDecoder DEFAULT = new IDecoder() {
        @Override
        public ReqEntity decode(ReqEntity entity) {
            return entity;
        }
    };
    private static volatile IDecoder instance = null;

    private DecoderHandler() {
    }

    public static IDecoder getDecoder() {
        if (instance == null) {
            synchronized (IDecoder.class) {
                if (instance == null) {
                    instance = DEFAULT;
                }
            }
        }
        return instance;
    }

    public static void focusDecoder(IDecoder iDecoder) {
        if (iDecoder == null) {
            return;
        }
        instance = iDecoder;
    }
}
