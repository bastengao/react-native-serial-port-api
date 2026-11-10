package com.bastengao.serialport;

import android.serialport.SerialPort;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;


public class SerialPortWrapper {
    public final static String DataReceivedEvent = "dataReceived";

    private SerialPort serialPort;
    private EventSender sender;
    private String path;
    private OutputStream out;
    private InputStream in;
    private Thread readThread;
    private Remover remover;
    private final CmdProtocolParser protocolParser;

    private AtomicBoolean closed = new AtomicBoolean(false);

    public SerialPortWrapper(final String path, final int readBufferSize, SerialPort serialPort, final EventSender sender, Remover remover) {
        this(path, readBufferSize, serialPort, sender, remover, null);
    }

    public SerialPortWrapper(final String path, final int readBufferSize, SerialPort serialPort,
            final EventSender sender, Remover remover, ProtocolConfig config) {
        this.path = path;
        this.serialPort = serialPort;
        this.sender = sender;
        this.remover = remover;
        this.out = this.serialPort.getOutputStream();
        this.in = this.serialPort.getInputStream();
        
        this.protocolParser = config == null ? null : new CmdProtocolParser(new CmdProtocolParser.PacketListener() {
            @Override
            public void onPacketReceived(byte[] packet) {
                emitData(packet, packet.length);
            }
        }, readBufferSize * 2, config);

        this.readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 这个buffer只是用于in.read()的临时存储
                byte[] buffer = new byte[readBufferSize];
                while (!closed.get()) {
                    try {
                        if (in == null) return;
                        
                        // 从输入流读取数据到临时buffer
                        int size = in.read(buffer);
                        if (size < 0) return;
                        if (size > 0) {
                            if (protocolParser == null) {
                                emitData(buffer, size);
                            } else {
                                protocolParser.handleData(buffer, size);
                            }
                        }
                    } catch (IOException e) {
                        if (!closed.get()) {
                           Log.e("serialport", "Error reading data: " + e.getMessage());
                           e.printStackTrace();
                        }
                        return;
                    }
                }
            }
        });
        this.readThread.start();
    }

    private void emitData(byte[] data, int size) {
        WritableMap event = Arguments.createMap();
        String hex = SerialPortApiModule.bytesToHex(data, size);
        event.putString("data", hex);
        event.putString("path", path);
        Log.i("serialport", "read size: " + size + ", hex: " + hex);
        sender.sendEvent(DataReceivedEvent, event);
    }

    public WritableMap toJS() {
        WritableMap js = Arguments.createMap();
        js.putString("path", path);
        return js;
    }

    public void write(byte[] buffer) throws IOException {
        this.out.write(buffer);
    }

    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        
        if (readThread != null) {
            readThread.interrupt();
        }
        
        try {
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (this.remover != null) {
            this.remover.remove();
        }
        Log.i("serialport", "close " + this.path);
    }
}
