package com.bastengao.serialport;

import android.serialport.SerialPort;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
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

    private AtomicBoolean closed = new AtomicBoolean(false);

    public SerialPortWrapper(String path, SerialPort serialPort, final EventSender sender, Remover remover) {
        this.path = path;
        this.serialPort = serialPort;
        this.sender = sender;
        this.remover = remover;
        this.out = this.serialPort.getOutputStream();
        this.in = this.serialPort.getInputStream();

        this.readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!closed.get()) {
                    try {
                        int size;
                        byte[] buffer = new byte[64];
                        if (in == null) return;
                        size = in.read(buffer);
                        if (size > 0) {
                            WritableMap event = Arguments.createMap();
                            byte[] data = Arrays.copyOf(buffer, size);
                            String hex = SerialPortApiModule.bytesToHex(data);
                            event.putString("data", hex);
                            sender.sendEvent(DataReceivedEvent, event);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });
        this.readThread.start();
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
        closed.set(true);
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.remover != null) {
            this.remover.remove();
        }
        Log.i("serialport", "close " + this.path);
    }
}
