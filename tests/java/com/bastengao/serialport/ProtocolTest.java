package com.bastengao.serialport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProtocolTest {
    private static int assertions;

    public static void main(String[] args) {
        ProtocolConfig original = config(0x55, 0xaa, 10, 3, 2, "little", 0, "bcc", 3, 2, 2);
        byte[] frame = {0x55, 1, 1, 10, 0, 2, 3, 4, 15, (byte) 0xaa};
        List<byte[]> packets = new ArrayList<>();
        CmdProtocolParser parser = new CmdProtocolParser(packets::add, 128, original);
        parser.handleData(Arrays.copyOfRange(frame, 0, 4), 4);
        check(packets.isEmpty(), "partial frame must wait");
        parser.handleData(Arrays.copyOfRange(frame, 4, 10), 6);
        check(packets.size() == 1 && Arrays.equals(packets.get(0), frame), "original protocol defaults");
        byte[] joined = new byte[21];
        joined[0] = 0x12;
        System.arraycopy(frame, 0, joined, 1, 10);
        System.arraycopy(frame, 0, joined, 11, 10);
        parser.handleData(joined, joined.length);
        check(packets.size() == 3, "noise and concatenated frames");
        byte[] bad = frame.clone(); bad[8] ^= 1;
        parser.handleData(bad, bad.length);
        check(packets.size() == 3, "invalid BCC rejected");
        bad = frame.clone(); bad[9] = 0;
        parser.handleData(bad, bad.length);
        check(packets.size() == 3, "invalid tail rejected");

        // Length is payload length, stored big endian at offset 1.
        ProtocolConfig big = config(0x7e, 0x7f, 6, 1, 2, "big", 4, "none", 3, 2, 2);
        packets.clear();
        parser = new CmdProtocolParser(packets::add, 128, big);
        byte[] custom = {0x7e, 0, 2, 0x11, 0x22, 0x7f};
        parser.handleData(custom, custom.length);
        check(packets.size() == 1 && Arrays.equals(packets.get(0), custom), "custom framing, endian and adjustment without checksum");

        ProtocolConfig bcc = config(0x55, 0xaa, 6, 1, 1, "little", 0, "bcc", 2, 3, 3);
        packets.clear(); parser = new CmdProtocolParser(packets::add, 128, bcc);
        byte[] ranged = {0x55, 6, 0x33, 0x33, 0x44, (byte) 0xaa};
        parser.handleData(ranged, ranged.length);
        check(packets.size() == 1, "custom BCC range and location");

        for (String endian : new String[]{"little", "big"}) {
            packets.clear();
            parser = new CmdProtocolParser(packets::add, 128,
                    config(0x55, 0xaa, 7, 1, 4, endian, 0, "none", 0, 1, 2));
            byte[] four = endian.equals("little")
                    ? new byte[]{0x55, 7, 0, 0, 0, 42, (byte) 0xaa}
                    : new byte[]{0x55, 0, 0, 0, 7, 42, (byte) 0xaa};
            byte[] oversized = {0x55, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
            parser.handleData(oversized, oversized.length);
            parser.handleData(four, four.length);
            check(packets.size() == 1, "unsigned four-byte oversized length recovery: " + endian);
        }

        packets.clear(); parser = new CmdProtocolParser(packets::add, 128, original);
        byte[] invalidLength = {0x55, 0, 0, 1, 0};
        parser.handleData(invalidLength, invalidLength.length);
        parser.handleData(frame, frame.length);
        check(packets.size() == 1, "short length recovery");

        invalid(() -> config(256, 0xaa, 10, 3, 2, "little", 0, "bcc", 3, 2, 2));
        invalid(() -> config(0x55, -1, 10, 3, 2, "little", 0, "bcc", 3, 2, 2));
        invalid(() -> config(0x55, 0xaa, 4, 3, 2, "little", 0, "none", 3, 2, 2));
        invalid(() -> config(0x55, 0xaa, 10, 3, 3, "little", 0, "bcc", 3, 2, 2));
        invalid(() -> config(0x55, 0xaa, 10, -1, 2, "little", 0, "bcc", 3, 2, 2));
        invalid(() -> config(0x55, 0xaa, 10, 3, 2, "middle", 0, "bcc", 3, 2, 2));
        invalid(() -> config(0x55, 0xaa, 10, 3, 2, "little", 0, "crc", 3, 2, 2));
        invalid(() -> config(0x55, 0xaa, 10, 3, 2, "little", 0, "bcc", 9, 2, 2));
        invalid(() -> config(0x55, 0xaa, 10, 3, 2, "little", 0, "bcc", 3, 2, 1));
        invalid(() -> new CmdProtocolParser(packet -> {}, 8, original));
        System.out.println("PASS: " + assertions + " protocol assertions");
    }

    private static ProtocolConfig config(int header, int tail, int min, int offset, int size,
            String endian, int adjustment, String checksum, int start, int end, int position) {
        return new ProtocolConfig(header, tail, min, offset, size, endian, adjustment,
                checksum, start, end, position);
    }

    private static void invalid(Runnable action) {
        try { action.run(); throw new AssertionError("expected invalid configuration"); }
        catch (IllegalArgumentException expected) { assertions++; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }
}
