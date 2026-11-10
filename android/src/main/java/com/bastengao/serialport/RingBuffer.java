package com.bastengao.serialport;

/**
 * 一个简单的字节循环缓冲区 (Ring Buffer).
 * 此实现忠实于经典的嵌入式C代码逻辑，通过牺牲一个字节的空间来区分“满”和“空”状态.
 * 这不是线程安全的，应在单个线程中访问.
 */
public class RingBuffer {
    private final byte[] buffer;
    private final int capacity;
    private int head = 0; // 读取指针 (指向第一个有效字节)
    private int tail = 0; // 写入指针 (指向下一个可写入的位置)

    public RingBuffer(int capacity) {
        // 实际容量需要比期望容量大1，因为有一个位置是保留的
        this.capacity = capacity + 1; 
        this.buffer = new byte[this.capacity];
    }

    /**
     * 向缓冲区写入数据块.
     * @param data 要写入的字节数组.
     * @param offset 数据的起始偏移量.
     * @param length 要写入的长度.
     * @return 如果缓冲区空间不足，则返回false.
     */
    public boolean write(byte[] data, int offset, int length) {
        if (length > getFreeSize()) {
            // 缓冲区空间不足
            return false;
        }
        for (int i = 0; i < length; i++) {
            buffer[tail] = data[offset + i];
            tail = (tail + 1) % capacity;
        }
        return true;
    }

    /**
     * 从缓冲区读取指定长度的数据.
     * @param length 要读取的字节数.
     * @return 读取到的字节数组，如果数据不足则返回null.
     */
    public byte[] read(int length) {
        if (length > size()) {
            return null;
        }
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = buffer[head];
            // 模仿C代码的行为，清空已读取的字节 (可选，但有助于调试)
            buffer[head] = 0; 
            head = (head + 1) % capacity;
        }
        return data;
    }

    /**
     * 查找指定字节在缓冲区中的第一个出现位置 (相对于head的偏移).
     * @param value 要查找的字节值.
     * @return 字节的索引，如果未找到则返回-1.
     */
    public int indexOf(byte value) {
        if (isEmpty()) {
            return -1;
        }
        int currentSize = size();
        for (int i = 0; i < currentSize; i++) {
            if (buffer[(head + i) % capacity] == value) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 预览缓冲区中从指定偏移量开始的数据，但不移除它们.
     * @param offset 起始偏移量.
     * @param length 要预览的长度.
     * @return 预览到的字节数组，如果数据不足则返回null.
     */
    public byte[] peek(int offset, int length) {
        if (offset + length > size()) {
            return null;
        }
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = buffer[(head + offset + i) % capacity];
        }
        return data;
    }

    /**
     * 丢弃缓冲区前面的N个字节.
     * @param count 要丢弃的字节数.
     */
    public void discard(int count) {
        int currentSize = size();
        if (count > currentSize) {
            count = currentSize;
        }
        head = (head + count) % capacity;
    }

    /**
     * 检查缓冲区是否已满.
     * @return 如果已满则返回true.
     */
    public boolean isFull() {
        return (tail + 1) % capacity == head;
    }

    /**
     * 检查缓冲区是否为空.
     * @return 如果为空则返回true.
     */
    public boolean isEmpty() {
        return head == tail;
    }
    
    /**
     * 获取当前缓冲区中已使用的数据大小.
     * @return 当前数据大小.
     */
    public int size() {
        return (tail - head + capacity) % capacity;
    }

    /**
     * 获取缓冲区的剩余可用空间.
     * @return 剩余空间大小.
     */
    public int getFreeSize() {
        // 总容量减去已用大小，再减去1（保留的空位）
        return (capacity - 1) - size();
    }


    public void clear() {
        this.head = 0;
        this.tail = 0;
        // 如果您在read方法中没有清零，可以在这里清零帮助调试
        // Arrays.fill(buffer, (byte)0);
    }
}
