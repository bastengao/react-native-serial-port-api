import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import SerialPort, { SerialPortWrapper } from './SerialPort';

const { SerialPortAPI } = NativeModules;
const eventEmitter = Platform.OS === 'android' ? new NativeEventEmitter(SerialPortAPI) : null;

export default class API {
  /**
   * @callback stringArrayCallback
   * @param {Array<string>} values
   */

  /**
   * @callback stringCallback
   * @param {string} value
   */


  /**
   * @typedef {Object} openOptions
   * @property {number} baudRate such as 9600
   * @property {number} [parity=0] 0: none, 1: odd, 2: even
   * @property {number} [dataBits=8] 5~8
   * @property {number} [stopBits=1] 1 or 2
   */

  /**
   * Get serial port device names
   * @param {stringArrayCallback} callback
   */
  static deviceNames(callback: (names: string[]) => void) {
    if (Platform.OS !== 'android') throw new Error(`Not support ${Platform.OS}`)
    SerialPortAPI.deviceNames(callback);
  }

  /**
   * Get serial port device paths
   * @param {stringArrayCallback} callback
   */
  static devicePaths(callback: (paths: string[]) => void) {
    if (Platform.OS !== 'android') throw new Error(`Not support ${Platform.OS}`)
    SerialPortAPI.devicePaths(callback);
  }

  /**
   * Get serial port device paths
   * @returns {Promise<Array<string>>} paths
   */
  static devicePathsAsync() {
    if (Platform.OS !== 'android') throw new Error(`Not support ${Platform.OS}`)
    return new Promise((resolve) => {
      SerialPortAPI.devicePaths(resolve);
    });
  }

  /**
   * set su binary path
   * @param {string} suPath
   */
  static setSuPath(suPath: string) {
    if (Platform.OS !== 'android') throw new Error(`Not support ${Platform.OS}`)
    SerialPortAPI.setSuPath(suPath);
  }

  /**
   * get su binary path
   * @param {stringCallback} callback
   */
  static getSuPath(callback: (suPath: string) => void) {
    if (Platform.OS !== 'android') throw new Error(`Not support ${Platform.OS}`)
    SerialPortAPI.getSuPath(callback)
  }

  /**
   * Open serial port
   * @param {string} devicePath device path
   * @param {openOptions} options
   * @returns {Promise<SerialPort>} connected serial port
   */
  static open(devicePath: string, {baudRate, parity = 0, dataBits = 8, stopBits = 1}: { baudRate: number, parity?: number, dataBits?: number, stopBits?: number }): Promise<SerialPort> {
    if (Platform.OS !== 'android') throw new Error(`Not support ${Platform.OS}`)
    return SerialPortAPI.open(devicePath, baudRate, parity, dataBits, stopBits)
      .then((serialPort: SerialPortWrapper) => {
        return Promise.resolve(new SerialPort(serialPort, eventEmitter!));
      })
  }
}
