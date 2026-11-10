package com.bastengao.serialport;

import android.util.Log;

/**
 * 串口命令协议解析器.
 * 负责从原始字节流中解析出完整的数据帧.
 */
public class CmdProtocolParser {

    private static final byte FRAME_HEADER = (byte) 0x55;
    private static final byte FRAME_TAIL = (byte) 0xAA;
    private static final int MIN_PACKET_LENGTH = 10; // 协议定义的最短包长度

    private final RingBuffer ringBuffer;
    private final PacketListener listener;

    public interface PacketListener {
        void onPacketReceived(byte[] packet);
    }

    public CmdProtocolParser(PacketListener listener, int bufferCapacity) {
        this.listener = listener;
        this.ringBuffer = new RingBuffer(bufferCapacity);
    }
    
    /**
     * 计算BCC校验和.
     * @param data 参与计算的数据.
     * @param offset 起始偏移量.
     * @param len 长度.
     * @return 校验和.
     */
    private byte calculateBcc(byte[] data, int offset, int len) {
        byte checksum = 0;
        for (int i = 0; i < len; i++) {
            checksum ^= data[offset + i];
        }
        return checksum;
    }

    /**
     * 处理新接收到的数据.
     * @param data 新数据.
     * @param size 数据长度.
     */
    public void handleData(byte[] data, int size) {
        // 将新数据写入环形缓冲区
        boolean success = ringBuffer.write(data, 0, size);
        if (!success) {
            Log.e("serialport", "RingBuffer 溢出！丢弃旧数据");
            // 策略：清空缓冲区
            ringBuffer.clear();

            // 再次尝试写入
            // 这次写入仍然可能失败，如果单次接收的数据块大小超过了缓冲区的总容量
            success = ringBuffer.write(data, 0, size);
            if (!success) {
                // 这是一个严重问题，意味着你的缓冲区总容量设置得太小，
                // 连一个基本的数据块都装不下。
                Log.e("serialport", "CRITICAL：传入数据块大小 (" + size + ") 大于总缓冲区容量。数据丢失！");
                // 在这种情况下，除了记录日志，我们无能为力，只能等待下一个数据块
                return;
            }
        }

        // 循环解析，直到缓冲区中没有完整的数据包
        while (true) {
            if (!parse()) {
                break;
            }
        }
    }
    /**
     * 尝试从缓冲区解析一个数据包.
     * @return 如果成功解析了一个包，返回true，否则返回false.
     */
    private boolean parse() {
        // 1. 查找帧头
        int headerIndex = ringBuffer.indexOf(FRAME_HEADER);
        if (headerIndex == -1) {
            // 没有找到帧头，无法继续
            Log.e("serialport", "parse: 没有找到帧头，无法继续");
            return false;
        }

        // 2. 丢弃帧头前的所有无效数据
        if (headerIndex > 0) {
            ringBuffer.discard(headerIndex);
        }

        // 3. 检查是否有足够的数据来读取包长度
        // 帧头(1) + 协议类型(1) + 协议版本(1) + 长度(2) = 5字节
        if (ringBuffer.size() < 5) {
            return false;
        }

        // 4. 读取包长度
        byte[] lengthBytes = ringBuffer.peek(3, 2); // 长度在第3和第4个字节 (0-indexed)
        // C代码是小端字节序 (Low byte first)
        int packetLength = (lengthBytes[0] & 0xFF) | ((lengthBytes[1] & 0xFF) << 8);

        // 5. 验证包长度
        if (packetLength < MIN_PACKET_LENGTH) {
             Log.e("serialport", "数据包长度无效: " + packetLength + ". 丢弃标头.");
             ringBuffer.discard(1); // 丢弃无效的帧头，继续寻找下一个
             return true; // 继续尝试
        }

        // 6. 检查缓冲区中是否有完整的包
        if (ringBuffer.size() < packetLength) {
            // 数据包不完整，等待更多数据
            Log.e("serialport", "数据包不完整，等待更多数据");
            return false;
        }

        // 7. 读取完整的包数据进行验证
        byte[] potentialPacket = ringBuffer.read(packetLength);
        if (potentialPacket == null) {
            // 理论上不会发生，因为前面已检查过长度
            Log.e("serialport", "理论上不会发生，因为前面已检查过长度");
            return false;
        }

        // 8. 验证帧尾
        if (potentialPacket[packetLength - 1] != FRAME_TAIL) {
            Log.e("serialport", "无效的框架尾部。丢弃的数据包.");
            // 帧尾不匹配，说明这个包是错误的。从下一个字节开始重新寻找帧头
            // 因为我们已经消耗了数据，所以返回true让外层循环继续
            return true;
        }

        // 9. 验证校验和
        // 根据C代码: BCC_CheckSum(tempbuffer + 3, CmdLenT - 5);
        // 校验和计算范围是从长度字段开始，到校验和字段之前
        byte expectedBcc = calculateBcc(potentialPacket, 3, packetLength - 5);
        byte actualBcc = potentialPacket[packetLength - 2];

        if (expectedBcc != actualBcc) {
            Log.e("serialport", "BCC 校验和失败。丢弃数据包。");
            return true;
        }

        // 10. 校验通过，这是一个完整的包
        Log.i("serialport", "找到完整的数据包，长度: " + packetLength);
        if (listener != null) {
            listener.onPacketReceived(potentialPacket);
        }
        return true;
    }
}
