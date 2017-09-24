package com.github.piasy.h264ds.ds;

import java.nio.ByteBuffer;

/**
 * Created by Piasy{github.com/Piasy} on 24/09/2017.
 */

public class NALU {
    public static final int NALU_TYPE_SLICE_NORMAL = 1;
    public static final int NALU_TYPE_SLICE_A = 2;
    public static final int NALU_TYPE_SLICE_B = 3;
    public static final int NALU_TYPE_SLICE_C = 4;
    public static final int NALU_TYPE_SLICE_IDR = 5;
    public static final int NALU_TYPE_SEI = 6;
    public static final int NALU_TYPE_SPS = 7;
    public static final int NALU_TYPE_PPS = 8;
    public static final int NALU_TYPE_DELIMITER = 9;
    public static final int NALU_TYPE_END_SEQUENCE = 10;
    public static final int NALU_TYPE_END_STREAM = 11;
    public static final int NALU_TYPE_PADDING = 12;

    private final int forbidden;
    private final int nri;
    private final int type;
    private final RBSP rbsp;

    public NALU(final int forbidden, final int nri, final int type,
            final RBSP rbsp) {
        this.forbidden = forbidden;
        this.nri = nri;
        this.type = type;
        this.rbsp = rbsp;
    }

    public static NALU parse(ByteBuffer buffer, int offset, int size) {
        buffer.position(offset);

        int startCode = buffer.getInt();
        if (startCode != 0x00000001) {
            return null;
        }
        byte header = buffer.get();
        int forbidden = (header & 0x80) >> 7;
        int nri = (header & 0x60) >> 5;
        int type = header & 0x1F;

        return new NALU(forbidden, nri, type, RBSP.parse(buffer, offset + 5, size, type));
    }

    private static String type(int type) {
        switch (type) {
            case 0:
                return "UNUSED";
            case NALU_TYPE_SLICE_NORMAL:
                return "SLICE_NORMAL";
            case NALU_TYPE_SLICE_A:
                return "SLICE_A";
            case NALU_TYPE_SLICE_B:
                return "SLICE_B";
            case NALU_TYPE_SLICE_C:
                return "SLICE_C";
            case NALU_TYPE_SLICE_IDR:
                return "SLICE_IDR";
            case NALU_TYPE_SEI:
                return "SEI";
            case NALU_TYPE_SPS:
                return "SPS";
            case NALU_TYPE_PPS:
                return "PPS";
            case NALU_TYPE_DELIMITER:
                return "DELIMITER";
            case NALU_TYPE_END_SEQUENCE:
                return "END_SEQUENCE";
            case NALU_TYPE_END_STREAM:
                return "END_STREAM";
            case NALU_TYPE_PADDING:
                return "PADDING";
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                return "RESERVED";
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
                return "UNUSED";
            default:
                return "INVALID";
        }
    }

    @Override
    public String toString() {
        return "NALU{" +
               "forbidden=" + forbidden +
               ", nri=" + nri +
               ", type=" + type(type) +
               ", rbsp=" + rbsp +
               '}';
    }
}
