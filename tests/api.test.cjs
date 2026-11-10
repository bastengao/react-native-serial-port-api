const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const { test } = require('node:test');

function loadAPI() {
  const calls = [];
  const native = {};
  for (const name of ['open', 'openWithProtocol']) {
    native[name] = (...args) => {
      calls.push({ name, args });
      return Promise.resolve({ path: args[0] });
    };
  }
  const source = fs.readFileSync(path.join(__dirname, '../src/API.ts'), 'utf8');
  const output = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2018 },
  }).outputText;
  const exports = {};
  vm.runInNewContext(output, {
    exports,
    require(name) {
      if (name === 'react-native') return {
        NativeModules: { SerialPortAPI: native }, Platform: { OS: 'android' },
        NativeEventEmitter: class {},
      };
      if (name === './SerialPort') return { default: class {
        constructor(port) { this.path = port.path; }
      } };
      throw new Error(`Unexpected import: ${name}`);
    },
  });
  return { API: exports.default, calls };
}

test('ordinary options use the existing native open entry point', async () => {
  const { API, calls } = loadAPI();
  const port = await API.open('/dev/raw', { baudRate: 9600 });
  assert.equal(port.path, '/dev/raw');
  assert.equal(calls[0].name, 'open');
  assert.deepEqual(Array.from(calls[0].args), ['/dev/raw', 9600, 0, 8, 1, 64]);
});

test('protocol options pass custom framing and checksum rules to native', async () => {
  const { API, calls } = loadAPI();
  const options = {
    baudRate: 19200, parity: 2, dataBits: 7, stopBits: 2, readBufferSize: 1024,
    frameHeader: 0, frameTail: 255, minPacketLength: 8,
    lengthField: { offset: 1, size: 2, endian: 'big', adjustment: 4 },
    checksum: { type: 'none' },
  };
  await API.open('/dev/framed', options);
  assert.equal(calls[0].name, 'openWithProtocol');
  assert.deepEqual(Array.from(calls[0].args.slice(0, 6)), ['/dev/framed', 19200, 2, 7, 2, 1024]);
  assert.deepEqual(JSON.parse(JSON.stringify(calls[0].args[6])), {
    frameHeader: 0, frameTail: 255, minPacketLength: 8,
    lengthField: options.lengthField, checksum: options.checksum,
  });
});

test('partial protocol settings reach native validation without undefined values', async () => {
  const { API, calls } = loadAPI();
  await API.open('/dev/partial', { baudRate: 9600, frameHeader: 85 });
  assert.equal(calls[0].name, 'openWithProtocol');
  assert.deepEqual(Object.keys(calls[0].args[6]), ['frameHeader']);
});
