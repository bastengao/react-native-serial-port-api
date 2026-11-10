package com.bastengao.serialport;

/** Immutable framing settings for one serial port. Offsets are measured in bytes. */
public final class ProtocolConfig {
    public final byte frameHeader;
    public final byte frameTail;
    public final int minPacketLength;
    public final int lengthOffset;
    public final int lengthSize;
    public final boolean littleEndian;
    public final int lengthAdjustment;
    public final boolean checkBcc;
    public final int checksumStart;
    public final int checksumEndFromEnd;
    public final int checksumOffsetFromEnd;

    public ProtocolConfig(int frameHeader, int frameTail, int minPacketLength,
            int lengthOffset, int lengthSize, String endian, int lengthAdjustment,
            String checksumType, int checksumStart, int checksumEndFromEnd,
            int checksumOffsetFromEnd) {
        require(frameHeader >= 0 && frameHeader <= 255, "frameHeader must be an integer from 0 to 255");
        require(frameTail >= 0 && frameTail <= 255, "frameTail must be an integer from 0 to 255");
        require(lengthOffset >= 1, "lengthField.offset must be at least 1 (after the frame header)");
        require(lengthSize == 1 || lengthSize == 2 || lengthSize == 4, "lengthField.size must be 1, 2 or 4");
        require("little".equals(endian) || "big".equals(endian), "lengthField.endian must be little or big");
        require(minPacketLength >= (long) lengthOffset + lengthSize + 1,
                "minPacketLength must include the frame header, length field and frame tail");
        require("bcc".equals(checksumType) || "none".equals(checksumType), "checksum.type must be bcc or none");
        boolean checkBcc = "bcc".equals(checksumType);
        if (checkBcc) {
            require(checksumOffsetFromEnd >= 2 && checksumOffsetFromEnd < minPacketLength,
                    "checksum.offsetFromEnd must locate a byte between the frame header and tail");
            require(checksumStart >= 0 && checksumEndFromEnd >= checksumOffsetFromEnd
                            && (long) checksumStart + checksumEndFromEnd < minPacketLength,
                    "checksum range must be non-empty and end before the checksum byte");
            long checksumIndex = (long) minPacketLength - checksumOffsetFromEnd;
            require(checksumIndex >= (long) lengthOffset + lengthSize,
                    "checksum byte must follow the length field at minPacketLength");
        }
        this.frameHeader = (byte) frameHeader;
        this.frameTail = (byte) frameTail;
        this.minPacketLength = minPacketLength;
        this.lengthOffset = lengthOffset;
        this.lengthSize = lengthSize;
        this.littleEndian = "little".equals(endian);
        this.lengthAdjustment = lengthAdjustment;
        this.checkBcc = checkBcc;
        this.checksumStart = checksumStart;
        this.checksumEndFromEnd = checksumEndFromEnd;
        this.checksumOffsetFromEnd = checksumOffsetFromEnd;
    }

    public void validateCapacity(int capacity) {
        require(capacity >= minPacketLength && capacity < Integer.MAX_VALUE,
                "protocol buffer capacity (readBufferSize * 2) must be at least minPacketLength and below 2147483647");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
