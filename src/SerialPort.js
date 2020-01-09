import { NativeEventEmitter, NativeModules } from 'react-native';
import Wrapper from './Wrapper';

const { SerialPortAPI } = NativeModules;
const eventEmitter = new NativeEventEmitter(SerialPortAPI);

export default class SerialPort {
  static deviceNames(callback) {
    SerialPortAPI.deviceNames(callback);
  }

  static devicePaths(callback) {
    SerialPortAPI.devicePaths(callback);
  }

  static open(devicePath, {baudRate, parity = 0, dataBits = 8, stopBits = 1}) {
    return SerialPortAPI.open(devicePath, baudRate, parity, dataBits, stopBits)
      .then(serialPort => {
        return Promise.resolve(new Wrapper(serialPort, eventEmitter));
      })
  }
}
