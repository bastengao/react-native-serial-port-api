# react-native-serial-port-api

Only for android platform based on [Android-SerialPort-API](https://github.com/licheedev/Android-SerialPort-API).

## Getting started

`$ npm install react-native-serial-port-api --save`

### Mostly automatic installation

`$ npx react-native link react-native-serial-port-api`

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

See [documentation](https://bastengao.com/react-native-serial-port-api/) for details.

## Development

Generate API docs.

    npm install -g jsdoc
    jsdoc src -r -R README.md -t node_modules/docdash -d ./docs/
