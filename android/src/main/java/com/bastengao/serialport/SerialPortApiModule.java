package com.bastengao.serialport;

import android.serialport.SerialPort;
import android.serialport.SerialPortFinder;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SerialPortApiModule extends ReactContextBaseJavaModule implements EventSender {

    private final ReactApplicationContext reactContext;
    private final SerialPortFinder finder = new SerialPortFinder();
    private final Map<String, SerialPortWrapper> serialPorts = new HashMap<String, SerialPortWrapper>();

    public SerialPortApiModule(final ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "SerialPortAPI";
    }

    @ReactMethod
    public void deviceNames(final Callback callback) {
        WritableArray a = Arguments.createArray();
        String[] names = finder.getAllDevices();
        for (String name : names) {
            a.pushString(name);
        }
        callback.invoke(a);
    }

    @ReactMethod
    public void devicePaths(final Callback callback) {
        WritableArray p = Arguments.createArray();
        String[] paths = finder.getAllDevicesPath();
        for (String path : paths) {
            p.pushString(path);
        }
        callback.invoke(p);
    }

    @ReactMethod
    public void setSuPath(final String suPath) {
        SerialPort.setSuPath(suPath);
    }

    @ReactMethod
    public void getSuPath(final Callback callback) {
        callback.invoke(SerialPort.getSuPath());
    }

    @ReactMethod
    public void open(final String path, int baudRate, int parity, int dataBits, int stopBits, int readBufferSize, Promise promise) {
        openPort(path, baudRate, parity, dataBits, stopBits, readBufferSize, null, promise);
    }

    @ReactMethod
    public void openWithProtocol(final String path, int baudRate, int parity, int dataBits,
            int stopBits, int readBufferSize, ReadableMap options, Promise promise) {
        final ProtocolConfig config;
        try {
            if (options == null) throw new IllegalArgumentException("protocol options are required");
            ReadableMap length = optionalMap(options, "lengthField");
            ReadableMap checksum = optionalMap(options, "checksum");
            config = new ProtocolConfig(
                    integer(options, "frameHeader", null), integer(options, "frameTail", null),
                    integer(options, "minPacketLength", null),
                    integer(length, "offset", 3), integer(length, "size", 2),
                    text(length, "endian", "little"), integer(length, "adjustment", 0),
                    text(checksum, "type", "bcc"), integer(checksum, "startOffset", 3),
                    integer(checksum, "endOffsetFromEnd", 2), integer(checksum, "offsetFromEnd", 2));
            if (readBufferSize <= 0 || readBufferSize > (Integer.MAX_VALUE - 1) / 2) {
                throw new IllegalArgumentException("readBufferSize must be positive and allow a doubled ring buffer capacity");
            }
            config.validateCapacity(readBufferSize * 2);
        } catch (IllegalArgumentException e) {
            promise.reject("E_INVALID_PROTOCOL", e.getMessage());
            return;
        }
        openPort(path, baudRate, parity, dataBits, stopBits, readBufferSize, config, promise);
    }

    private static ReadableMap optionalMap(ReadableMap options, String key) {
        if (!options.hasKey(key)) return null;
        if (options.isNull(key) || options.getType(key) != ReadableType.Map) {
            throw new IllegalArgumentException(key + " must be an object");
        }
        return options.getMap(key);
    }

    private static int integer(ReadableMap options, String key, Integer fallback) {
        if (options == null || !options.hasKey(key)) {
            if (fallback != null) return fallback;
            throw new IllegalArgumentException(key + " is required when protocol parsing is enabled");
        }
        if (options.isNull(key) || options.getType(key) != ReadableType.Number) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        double value = options.getDouble(key);
        if (Double.isNaN(value) || Double.isInfinite(value) || value != Math.rint(value)
                || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key + " must be a 32-bit integer");
        }
        return (int) value;
    }

    private static String text(ReadableMap options, String key, String fallback) {
        if (options == null || !options.hasKey(key)) return fallback;
        if (options.isNull(key) || options.getType(key) != ReadableType.String) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return options.getString(key);
    }

    private void openPort(final String path, int baudRate, int parity, int dataBits, int stopBits,
            int readBufferSize, ProtocolConfig config, Promise promise) {
        if (readBufferSize <= 0) {
            promise.reject("E_INVALID_OPTIONS", "readBufferSize must be positive");
            return;
        }
        if (serialPorts.containsKey(path)) {
            promise.resolve(serialPorts.get(path).toJS());
            return;
        }

        Remover remover = new Remover() {
            @Override
            public void remove() {
                serialPorts.remove(path);
            }
        };

        try {
            SerialPort serialPort = SerialPort.newBuilder(path, baudRate)
                    .parity(parity)
                    .dataBits(dataBits)
                    .stopBits(stopBits)
                    .build();

            SerialPortWrapper wrapper = new SerialPortWrapper(path, readBufferSize, serialPort, this, remover, config);

            // TODO: handle previous value
            serialPorts.put(path, wrapper);
            promise.resolve(wrapper.toJS());
        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
            e.printStackTrace();
            promise.reject(null, e.getMessage());
        } catch (SecurityException e) {
            System.out.println("error: " + e.getMessage());
            e.printStackTrace();
            promise.reject(null, "no permission to read or write this serial port");
        }
    }

    @ReactMethod
    public void send(String path, String hexStr, Promise promise) {
        SerialPortWrapper wrapper = serialPorts.get(path);
        if (wrapper == null) {
            promise.reject(null, "serialport not open");
            return; // 添加return语句，防止继续执行
        }

        byte[] buffer = hexStringToByteArray(hexStr);
        try {
            wrapper.write(buffer);
            promise.resolve(true);
            Log.i("serialport", "send: " + hexStr);
        } catch (IOException e) {
            e.printStackTrace();
            promise.reject(null, e.getMessage());
        }
    }

    @ReactMethod
    public void close(String path, Promise promise) {
        Log.i("serialport", "close: " + path);
        SerialPortWrapper wrapper = serialPorts.get(path);
        if (wrapper == null) {
            promise.reject(null, "serialport not open");
            return; // 添加return语句，防止继续执行
        }
        wrapper.close();
        promise.resolve(true);
    }

    public void sendEvent(final String eventName, final WritableMap event) {
        reactContext.runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {
                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(eventName, event);
            }
        });
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes, int length) {
        char[] hexChars = new char[length * 2];
        for (int j = 0; j < length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
