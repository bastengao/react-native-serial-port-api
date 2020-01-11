# react-native-serial-port-api

Only for android platform based on [Android-SerialPort-API](https://github.com/licheedev/Android-SerialPort-API)

## Getting started

`$ npm install react-native-serial-port-api --save`

### Mostly automatic installation

`$ react-native link react-native-serial-port-api`

## Usage

```javascript
import SerialPortAPI from 'react-native-serial-port-api';

SerialPortAPI.open("/dev/ttyS4", { baudRate: 38400 }).then(serialPort => {
  console.log(serialPort.getPath());
});
```
