package cn.demo.appq.utils;

import java.nio.ByteBuffer;

public class IOUtils {
    public static String byteBuffer2String(ByteBuffer buffer) {
        buffer.rewind();
        byte[] bytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(bytes);
        return new String(bytes);
    }
}
