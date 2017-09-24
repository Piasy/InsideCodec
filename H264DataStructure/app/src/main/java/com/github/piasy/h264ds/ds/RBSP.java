package com.github.piasy.h264ds.ds;

import java.nio.ByteBuffer;

/**
 * Created by Piasy{github.com/Piasy} on 24/09/2017.
 */

public class RBSP {
    public static RBSP parse(ByteBuffer buffer, int offset, int size, int type) {
        buffer.position(offset);

        switch (type) {
            case NALU.NALU_TYPE_SLICE_NORMAL:
            case NALU.NALU_TYPE_SLICE_IDR:
                return Slice.parse(buffer);
            default:
                return null;
        }
    }
}
