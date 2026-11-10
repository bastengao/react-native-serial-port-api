import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import SerialPort, { SerialPortWrapper } from './SerialPort';

const { SerialPortAPI } = NativeModules;
const eventEmitter = Platform.OS === 'android' ? new NativeEventEmitter(SerialPortAPI) : null;

export interface LengthFieldOptions {
  /** Zero-based byte offset. Default: 3. Must follow the frame header. */
  offset?: number;
  /** Number of unsigned length bytes. Default: 2. */
  size?: 1 | 2 | 4;
  endian?: 'little' | 'big';
  /** Total frame length = unsigned field value + adjustment. Default: 0. */
  adjustment?: number;
}

export interface ChecksumOptions {
  /** Default: bcc (bytewise XOR). */
  type?: 'bcc' | 'none';
  /** Inclusive, zero-based start. Default: 3. */
  startOffset?: number;
  /** Exclusive end = frame length minus this value. Default: 2. */
  endOffsetFromEnd?: number;
  /** Checksum index = frame length minus this value. Default: 2. */
  offsetFromEnd?: number;
}

export interface OpenOptions {
  baudRate: number;
  parity?: number;
  dataBits?: number;
  stopBits?: number;
  readBufferSize?: number;
  /** Supplying protocol options enables framing; all three frame settings are required. */
  frameHeader?: number;
  frameTail?: number;
  minPacketLength?: number;
  lengthField?: LengthFieldOptions;
  checksum?: ChecksumOptions;
}

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
   * @property {number} [readBufferSize=64] set read buffer size
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
  static open(devicePath: string, {
    baudRate, parity = 0, dataBits = 8, stopBits = 1, readBufferSize = 64,
    frameHeader, frameTail, minPacketLength, lengthField, checksum,
  }: OpenOptions): Promise<SerialPort> {
    if (Platform.OS !== 'android') throw new Error(`Not support ${Platform.OS}`)
    const protocol = { frameHeader, frameTail, minPacketLength, lengthField, checksum };
    // Omit undefined values before crossing the React Native bridge.
    const configuredProtocol: { [key: string]: unknown } = {};
    Object.keys(protocol).forEach(key => {
      const value = protocol[key as keyof typeof protocol];
      if (value !== undefined) configuredProtocol[key] = value;
    });
    const result = Object.keys(configuredProtocol).length === 0
      ? SerialPortAPI.open(devicePath, baudRate, parity, dataBits, stopBits, readBufferSize)
      : SerialPortAPI.openWithProtocol(devicePath, baudRate, parity, dataBits, stopBits, readBufferSize, configuredProtocol);
    return result
      .then((serialPort: SerialPortWrapper) => {
        return Promise.resolve(new SerialPort(serialPort, eventEmitter!));
      })
  }
}
