package com.bastengao.serialport;

import android.util.Log;

/** Extracts complete frames from a byte stream using per-port protocol settings. */
public class CmdProtocolParser {
    private final RingBuffer ringBuffer;
    private final int bufferCapacity;
    private final PacketListener listener;
    private final ProtocolConfig config;

    public interface PacketListener {
        void onPacketReceived(byte[] packet);
    }

    public CmdProtocolParser(PacketListener listener, int bufferCapacity, ProtocolConfig config) {
        config.validateCapacity(bufferCapacity);
        this.listener = listener;
        this.config = config;
        this.bufferCapacity = bufferCapacity;
        this.ringBuffer = new RingBuffer(bufferCapacity);
    }

    public void handleData(byte[] data, int size) {
        if (!ringBuffer.write(data, 0, size)) {
            Log.e("serialport", "RingBuffer overflow; discarding buffered data");
            ringBuffer.clear();
            if (!ringBuffer.write(data, 0, size)) {
                Log.e("serialport", "Incoming data exceeds ring buffer capacity");
                return;
            }
        }
        while (parse()) {
            // Drain all complete frames before waiting for the next read.
        }
    }

    private boolean parse() {
        int headerIndex = ringBuffer.indexOf(config.frameHeader);
        if (headerIndex == -1) {
            ringBuffer.clear();
            return false;
        }
        if (headerIndex > 0) ringBuffer.discard(headerIndex);
        if (ringBuffer.size() < config.lengthOffset + config.lengthSize) return false;

        byte[] lengthBytes = ringBuffer.peek(config.lengthOffset, config.lengthSize);
        long length = 0;
        for (int i = 0; i < config.lengthSize; i++) {
            int index = config.littleEndian ? config.lengthSize - 1 - i : i;
            length = (length << 8) | (lengthBytes[index] & 0xff);
        }
        length += config.lengthAdjustment;
        if (length < config.minPacketLength || length > bufferCapacity) {
            Log.e("serialport", "Invalid packet length: " + length);
            ringBuffer.discard(1);
            return true;
        }
        int packetLength = (int) length;
        if (ringBuffer.size() < packetLength) return false;

        byte[] packet = ringBuffer.read(packetLength);
        if (packet[packetLength - 1] != config.frameTail) {
            Log.e("serialport", "Invalid frame tail; discarding packet");
            return true;
        }
        if (config.checkBcc) {
            byte expected = 0;
            int end = packetLength - config.checksumEndFromEnd;
            for (int i = config.checksumStart; i < end; i++) expected ^= packet[i];
            if (expected != packet[packetLength - config.checksumOffsetFromEnd]) {
                Log.e("serialport", "BCC checksum failed; discarding packet");
                return true;
            }
        }
        if (listener != null) listener.onPacketReceived(packet);
        return true;
    }
}
