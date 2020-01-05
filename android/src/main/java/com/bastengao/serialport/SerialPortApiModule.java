package com.bastengao.serialport;

import android.serialport.SerialPortFinder;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;

public class SerialPortApiModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private final SerialPortFinder finder = new SerialPortFinder();

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
}
