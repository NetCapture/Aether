package cn.demo.appq.utils;

import java.nio.ByteBuffer;

public class IOUtils {
    public static String byteBuffer2String(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        buffer.rewind();
        byte[] bytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(bytes);
        return Base64Utils.encode(bytes);
    }

    public static ByteBuffer string2ByteBuffer(String value) {
        if (value == null) {
            return null;
        }
        byte[] data = Base64Utils.decode(value);
        if (data == null) {
            return ByteBuffer.allocate(0);
        }
        return ByteBuffer.wrap(data);
    }
}
