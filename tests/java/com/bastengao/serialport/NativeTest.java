package com.bastengao.serialport;

import android.serialport.SerialPort;
import com.facebook.react.bridge.*;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.*;

/** Uses test-only platform stubs; serial parsing and bridge validation are production code. */
public class NativeTest {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        List<WritableMap> events = Collections.synchronizedList(new ArrayList<WritableMap>());
        // Bytes that the old hardcoded parser would silently drop.
        SerialPort.input = new ByteArrayInputStream(new byte[]{0, 1, 2, 3, 4});
        SerialPortWrapper raw = new SerialPortWrapper("raw", 2, new SerialPort(),
                (name, event) -> events.add(event), () -> {});
        awaitRead(raw);
        check(events.size() == 3, "raw reads preserve readBufferSize chunk boundaries");
        check("0001".equals(events.get(0).getString("data")), "raw bytes emitted without parsing");
        check("04".equals(events.get(2).getString("data")), "raw final partial read");
        check("raw".equals(events.get(0).getString("path")), "event includes port path");
        raw.close();

        events.clear();
        byte[] frame = {0x55, 1, 1, 10, 0, 2, 3, 4, 15, (byte) 0xaa};
        SerialPort.input = new ByteArrayInputStream(frame);
        ProtocolConfig config = new ProtocolConfig(0x55, 0xaa, 10, 3, 2, "little", 0, "bcc", 3, 2, 2);
        SerialPortWrapper framed = new SerialPortWrapper("framed", 5, new SerialPort(),
                (name, event) -> events.add(event), () -> {}, config);
        awaitRead(framed);
        check(events.size() == 1, "framing combines reads into one event");
        check("5501010A000203040FAA".equals(events.get(0).getString("data")), "framing preserves full packet");
        framed.close();

        SerialPortApiModule module = new SerialPortApiModule(new ReactApplicationContext());
        WritableMap base = map("frameHeader", 85, "frameTail", 170, "minPacketLength", 10);
        rejected(module, new WritableMap(), 64);
        rejected(module, map("frameHeader", 85), 64);
        rejected(module, map("frameHeader", 85.5, "frameTail", 170, "minPacketLength", 10), 64);
        rejected(module, map("frameHeader", "55", "frameTail", 170, "minPacketLength", 10), 64);
        rejected(module, map("frameHeader", Double.NaN, "frameTail", 170, "minPacketLength", 10), 64);
        rejected(module, map("frameHeader", 85, "frameTail", 170, "minPacketLength", 10, "lengthField", null), 64);
        rejected(module, map("frameHeader", 85, "frameTail", 170, "minPacketLength", 10,
                "lengthField", map("offset", -1)), 64);
        rejected(module, map("frameHeader", 85, "frameTail", 170, "minPacketLength", 10,
                "checksum", map("type", "crc")), 64);
        rejected(module, base, 4);
        rejected(module, base, Integer.MAX_VALUE);
        rejected(module, base, 0);

        // Empty streams prevent asynchronous events while testing open entry points.
        SerialPort.input = new ByteArrayInputStream(new byte[0]);
        Result result = new Result();
        module.openWithProtocol("configured", 9600, 0, 8, 1, 64, base, result);
        check(result.value != null && result.code == null, "default nested configuration accepted by bridge");
        module.close("configured", new Result());
        result = new Result();
        module.open("ordinary", 9600, 0, 8, 1, 64, result);
        check(result.value != null && result.code == null, "legacy native open signature preserved");
        module.close("ordinary", new Result());
        System.out.println("PASS: " + assertions + " native bridge/read assertions");
    }

    private static void awaitRead(SerialPortWrapper wrapper) throws Exception {
        Field field = SerialPortWrapper.class.getDeclaredField("readThread");
        field.setAccessible(true);
        Thread thread = (Thread) field.get(wrapper);
        thread.join(2000);
        check(!thread.isAlive(), "read thread stops at EOF");
    }
    private static WritableMap map(Object... entries) {
        WritableMap map = new WritableMap();
        for (int i = 0; i < entries.length; i += 2) map.put((String) entries[i], entries[i + 1]);
        return map;
    }
    private static void rejected(SerialPortApiModule module, WritableMap options, int bufferSize) {
        int opens = SerialPort.opens;
        Result result = new Result();
        module.openWithProtocol("invalid", 9600, 0, 8, 1, bufferSize, options, result);
        check("E_INVALID_PROTOCOL".equals(result.code), "invalid protocol returns error: " + result.message);
        check(SerialPort.opens == opens, "invalid protocol must not open a device");
    }
    private static class Result implements Promise {
        Object value; String code; String message;
        public void resolve(Object value) { this.value = value; }
        public void reject(String code, String message) { this.code = code; this.message = message; }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }
}
