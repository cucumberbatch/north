package org.north.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joml.Matrix4f;

import java.nio.*;
import java.util.HashMap;
import java.util.Map;

public class BufferUtils {
    private static final Logger log = LoggerFactory.getLogger(BufferUtils.class);

    private static final Map<Integer, ByteBuffer>  BYTE_BUFFER_STORAGE  = new HashMap<>();
    private static final Map<Integer, IntBuffer>   INT_BUFFER_STORAGE   = new HashMap<>();
    private static final Map<Integer, FloatBuffer> FLOAT_BUFFER_STORAGE = new HashMap<>();

    private static final float[] M4F_BUFFER_ARRAY = new float[16];

    private static final boolean newMemoryAllocation = true;

    public static ByteBuffer createByteBuffer(int size) {
        if (newMemoryAllocation) {
            return org.lwjgl.BufferUtils.createByteBuffer(size);
        }
        return ByteBuffer.allocateDirect(size);
    }

    public static ByteBuffer createByteBuffer(byte[] array) {
        ByteBuffer buffer;
        if ((buffer = BYTE_BUFFER_STORAGE.get(array.length)) != null) {
            buffer.clear().put(array).flip();
            return buffer;
        }

        if (newMemoryAllocation) {
            buffer = org.lwjgl.BufferUtils.createByteBuffer(array.length);
        } else {
            buffer = ByteBuffer.allocateDirect(array.length);
        }

        //log.info(String.format("ByteBuffer of size %d created", buffer.capacity()));
        buffer.order(ByteOrder.nativeOrder()).put(array).flip();
        BYTE_BUFFER_STORAGE.put(array.length, buffer);
        return buffer;
    }

    public static IntBuffer createIntBuffer(int[] array) {
        IntBuffer buffer;
        if ((buffer = INT_BUFFER_STORAGE.get(array.length)) != null) {
            buffer.clear().put(array).flip();
            return buffer;
        }
        buffer = ByteBuffer.allocateDirect(array.length << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
        //log.info("IntBuffer of size {} created", buffer.capacity());
        buffer.put(array).flip();
        INT_BUFFER_STORAGE.put(array.length, buffer);
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(float[] array) {
        FloatBuffer buffer;
        if ((buffer = FLOAT_BUFFER_STORAGE.get(array.length)) != null) {
            buffer.clear().put(array).flip();
            return buffer;
        }
        buffer = ByteBuffer.allocateDirect(array.length << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        //log.info(String.format("FloatBuffer of size %d created", buffer.capacity()));
        buffer.put(array).flip();
        FLOAT_BUFFER_STORAGE.put(array.length, buffer);
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Matrix4f matrix4f) {
        FloatBuffer buffer;
        if ((buffer = FLOAT_BUFFER_STORAGE.get(16)) != null) {
            buffer.clear();
            return matrix4f.get(buffer);
        }

        if (newMemoryAllocation) {
            buffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
        } else {
            buffer = ByteBuffer.allocateDirect(16 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        }

        //log.info(String.format("FloatBuffer of size %d created", buffer.capacity()));
        matrix4f.get(buffer);
        FLOAT_BUFFER_STORAGE.put(16, buffer);
        return buffer;
    }
}
