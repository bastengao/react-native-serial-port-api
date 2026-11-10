const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const root = path.resolve(__dirname, '..');
const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'serial-port-tests-'));
function run(command, args) {
  const result = spawnSync(command, args, { cwd: root, encoding: 'utf8' });
  if (result.stdout) process.stdout.write(result.stdout);
  if (result.stderr) process.stderr.write(result.stderr);
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${command} exited with ${result.status}`);
}
try {
  const stub = path.join(temp, 'android/util/Log.java');
  fs.mkdirSync(path.dirname(stub), { recursive: true });
  fs.writeFileSync(stub, 'package android.util; public class Log { public static int e(String t, String m) { return 0; } public static int i(String t, String m) { return 0; } }');
  // Minimal platform stubs let the actual bridge and read thread run without an Android SDK.
  const stubs = {
    'com/facebook/react/bridge/ReadableType.java': `package com.facebook.react.bridge; public enum ReadableType { Null, Boolean, Number, String, Map, Array }`,
    'com/facebook/react/bridge/ReadableMap.java': `package com.facebook.react.bridge; public interface ReadableMap { boolean hasKey(String k); boolean isNull(String k); ReadableType getType(String k); ReadableMap getMap(String k); double getDouble(String k); String getString(String k); }`,
    'com/facebook/react/bridge/WritableMap.java': `package com.facebook.react.bridge; public class WritableMap extends java.util.HashMap<String,Object> implements ReadableMap { public void putString(String k,String v){put(k,v);} public boolean hasKey(String k){return containsKey(k);} public boolean isNull(String k){return get(k)==null;} public ReadableType getType(String k){Object v=get(k); return v instanceof ReadableMap ? ReadableType.Map : v instanceof Number ? ReadableType.Number : v instanceof String ? ReadableType.String : ReadableType.Null;} public ReadableMap getMap(String k){return (ReadableMap)get(k);} public double getDouble(String k){return ((Number)get(k)).doubleValue();} public String getString(String k){return (String)get(k);} }`,
    'com/facebook/react/bridge/WritableArray.java': `package com.facebook.react.bridge; public class WritableArray { public void pushString(String s){} }`,
    'com/facebook/react/bridge/Arguments.java': `package com.facebook.react.bridge; public class Arguments { public static WritableMap createMap(){return new WritableMap();} public static WritableArray createArray(){return new WritableArray();} }`,
    'com/facebook/react/bridge/Callback.java': `package com.facebook.react.bridge; public interface Callback { void invoke(Object value); }`,
    'com/facebook/react/bridge/Promise.java': `package com.facebook.react.bridge; public interface Promise { void resolve(Object value); void reject(String code,String message); }`,
    'com/facebook/react/bridge/ReactMethod.java': `package com.facebook.react.bridge; public @interface ReactMethod {}`,
    'com/facebook/react/bridge/ReactContextBaseJavaModule.java': `package com.facebook.react.bridge; public abstract class ReactContextBaseJavaModule { public ReactContextBaseJavaModule(ReactApplicationContext c){} public abstract String getName(); }`,
    'com/facebook/react/bridge/ReactApplicationContext.java': `package com.facebook.react.bridge; public class ReactApplicationContext { public Object emitter; public void runOnUiQueueThread(Runnable r){r.run();} public <T> T getJSModule(Class<T> c){return c.cast(emitter);} }`,
    'com/facebook/react/modules/core/DeviceEventManagerModule.java': `package com.facebook.react.modules.core; public class DeviceEventManagerModule { public interface RCTDeviceEventEmitter { void emit(String name,Object event); } }`,
    'android/serialport/SerialPortFinder.java': `package android.serialport; public class SerialPortFinder { public String[] getAllDevices(){return new String[0];} public String[] getAllDevicesPath(){return new String[0];} }`,
    'android/serialport/SerialPort.java': `package android.serialport; import java.io.*; public class SerialPort { public static int opens; public static InputStream input = new ByteArrayInputStream(new byte[0]); public InputStream getInputStream(){return input;} public OutputStream getOutputStream(){return new ByteArrayOutputStream();} public static void setSuPath(String s){} public static String getSuPath(){return "su";} public static Builder newBuilder(String p,int b){opens++;return new Builder();} public static class Builder { public Builder parity(int p){return this;} public Builder dataBits(int b){return this;} public Builder stopBits(int b){return this;} public SerialPort build() throws IOException {return new SerialPort();} } }`,
  };
  const stubFiles = Object.entries(stubs).map(([name, text]) => {
    const file = path.join(temp, name);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, text);
    return file;
  });
  const source = path.join(root, 'android/src/main/java/com/bastengao/serialport');
  const files = ['RingBuffer.java', 'CmdProtocolParser.java', 'ProtocolConfig.java',
    'SerialPortWrapper.java', 'SerialPortApiModule.java', 'EventSender.java', 'Remover.java']
    .map(name => path.join(source, name)).filter(file => fs.existsSync(file));
  run('javac', ['-encoding', 'UTF-8', '--release', '8', '-d', temp, stub, ...stubFiles, ...files,
    path.join(__dirname, 'java/com/bastengao/serialport/ProtocolTest.java'),
    path.join(__dirname, 'java/com/bastengao/serialport/NativeTest.java')]);
  run('java', ['-cp', temp, 'com.bastengao.serialport.ProtocolTest']);
  run('java', ['-cp', temp, 'com.bastengao.serialport.NativeTest']);
} finally {
  fs.rmSync(temp, { recursive: true, force: true });
}
