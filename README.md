# react-native-serial-port-api

Only for android platform based on [Android-SerialPort-API](https://github.com/licheedev/Android-SerialPort-API).

## Getting started

`$ npm install react-native-serial-port-api --save`

### (Optional) React Native < 0.60

Run this command if use React Native before version 0.60.

`$ npx react-native link react-native-serial-port-api`

[Autolinking](https://github.com/react-native-community/cli/blob/master/docs/autolinking.md) will works for 0.60 or above.

## Usage

```javascript
import SerialPortAPI from 'react-native-serial-port-api';

async function example() {
  const serialPort = await SerialPortAPI.open("/dev/ttyS4", { baudRate: 38400 });

  // subscribe received data
  const sub = serialPort.onReceived(buff => {
    console.log(buff.toString('hex').toUpperCase());
  })

  // unsubscribe
  // sub.remove();

  // send data with hex format
  await serialPort.send('00FF');

  // close
  serialPort.close();
}
```

### Optional protocol framing

By default, each positive serial read is emitted immediately, with no framing or checksum validation. A read can contain part of a device message or several messages; `readBufferSize` is the maximum number of bytes per read (default: 64).

To enable the custom packet parser, supply **all three** of `frameHeader`, `frameTail` and `minPacketLength`. These options are per port. The previous hardcoded protocol is now opt-in:

```typescript
const serialPort = await SerialPortAPI.open('/dev/ttyS4', {
  baudRate: 9600,
  readBufferSize: 1024,
  frameHeader: 0x55,
  frameTail: 0xaa,
  minPacketLength: 10,
});
```

With those three options, the defaults match the previous parser: a two-byte unsigned little-endian length at offset 3, representing the total frame length, and BCC (XOR) from offset 3 up to but excluding the penultimate checksum byte. Header and tail are each one byte. Emitted packets include the header, length field, checksum and tail.

Length and checksum rules can be overridden independently:

```typescript
const serialPort = await SerialPortAPI.open('/dev/ttyS4', {
  baudRate: 9600,
  readBufferSize: 1024,
  frameHeader: 0x55,
  frameTail: 0xaa,
  minPacketLength: 10,
  lengthField: {
    offset: 3,          // Zero-based offset; default: 3
    size: 2,            // 1, 2 or 4 unsigned bytes; default: 2
    endian: 'little',   // 'little' or 'big'; default: 'little'
    adjustment: 0,      // Total frame length = field value + adjustment
  },
  checksum: {
    type: 'bcc',        // 'bcc' or 'none'; default: 'bcc'
    startOffset: 3,     // Inclusive start; default: 3
    endOffsetFromEnd: 2,// Exclusive end = frame length - 2
    offsetFromEnd: 2,   // Checksum index = frame length - 2
  },
});
```

For example, if the length field counts only the payload, set `adjustment` to the number of header, length, checksum and tail bytes excluded from that count. Use `checksum: { type: 'none' }` to disable checksum validation; the checksum offsets are then ignored. This parser supports BCC only, not CRC algorithms.

Validation rules:

- Any protocol option enables protocol configuration validation. Missing required frame options, null values, non-integers or invalid enum values reject `open` with `E_INVALID_PROTOCOL`, before opening the device.
- Header and tail must be integers in `0..255`. Length offset must be at least 1, and the minimum frame length must include the entire length field and tail.
- With BCC enabled, the checksum byte must follow the length field at the minimum frame length and precede the tail. The checksum range must be non-empty and end at or before the checksum byte. All offsets and lengths are in bytes.
- Protocol ring buffer capacity is `readBufferSize * 2` and must accommodate `minPacketLength`. Choose a read buffer large enough for the largest frame; declared frame lengths outside the minimum/capacity range are discarded. On buffer overflow, buffered data is cleared before accepting the incoming read.
- Opening an already open path returns that port and keeps its existing configuration. Close it before changing settings.

After changing native sources, rebuild the Android app. TypeScript consumers can import `OpenOptions`, `LengthFieldOptions` and `ChecksumOptions` from this package.

See [documentation](https://bastengao.com/react-native-serial-port-api/) for details.

## Development

Run `npm test` for the API routing tests and standalone Java parser/read-loop/bridge regression tests. These require Node.js, installed npm dependencies and JDK 9 or newer (sources are compiled for Java 8). Platform stubs are used for these tests; they do not replace an Android build or hardware serial-port testing.

Run `npx tsc --noEmit` to check TypeScript types.

Generate API docs.

    npm install -g jsdoc
    jsdoc src -r -R README.md -t node_modules/docdash -d ./docs/
