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

See [documentation](https://bastengao.com/react-native-serial-port-api/) for details.

## Development

Generate API docs.

    npm install -g jsdoc
    jsdoc src -r -R README.md -t node_modules/docdash -d ./docs/
