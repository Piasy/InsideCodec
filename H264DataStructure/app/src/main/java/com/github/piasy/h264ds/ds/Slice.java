package com.github.piasy.h264ds.ds;

import android.util.Pair;
import java.nio.ByteBuffer;

/**
 * Created by Piasy{github.com/Piasy} on 24/09/2017.
 */

public class Slice extends RBSP {
    private final int firstMbAddr;
    private final int type;
    private final int frameNum;

    public Slice(final int firstMbAddr, final int type, final int frameNum) {
        this.firstMbAddr = firstMbAddr;
        this.type = type;
        this.frameNum = frameNum;
    }

    public static Slice parse(ByteBuffer buffer) {
        Pair<Integer, Integer> firstMbAddr = readExpGolomb(buffer, 0);
        Pair<Integer, Integer> type = readExpGolomb(buffer, firstMbAddr.second);
        int frameNum = 0;
        return new Slice(firstMbAddr.first, type.first % 5, frameNum);
    }

    private static Pair<Integer, Integer> readExpGolomb(ByteBuffer buffer, int bitOffset) {
        int oldPosition = buffer.position();
        int oldBitOffset = bitOffset;

        // consume leading garbage bytes
        while (bitOffset >= 8) {
            buffer.get();
            bitOffset -= 8;
        }

        long i64 = buffer.getLong();
        int leadingZeros = leadingZeros(i64, bitOffset);
        int value = extractBits(i64, bitOffset + leadingZeros, leadingZeros + 1) - 1;

        buffer.position(oldPosition);
        return Pair.create(value, oldBitOffset + leadingZeros * 2 + 1);
    }

    private static int leadingZeros(long i64, int offset) {
        long value = i64 << offset;
        int count = 0;
        while ((value & 0x8000000000000000L) == 0) {
            count++;
            value = value << 1;
        }
        return count;
    }

    private static int extractBits(long i64, int offset, int size) {
        long value = i64 >>> (64 - offset - size);
        long mask = 0xFFFFFFFFFFFFFFFFL >>> (64 - size);
        return (int) (value & mask);
    }

    private static String type(int type) {
        switch (type) {
            case 0:
                return "P";
            case 1:
                return "B";
            case 2:
                return "I";
            case 3:
                return "SP";
            case 4:
                return "SI";
            default:
                return "INVALID";
        }
    }

    @Override
    public String toString() {
        return "Slice{" +
               "firstMbAddr=" + firstMbAddr +
               ", type=" + type(type) +
               ", frameNum=" + frameNum +
               '}';
    }
}
