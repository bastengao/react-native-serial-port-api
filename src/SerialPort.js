import { NativeModules } from 'react-native';
import { Buffer } from 'buffer';

const { SerialPortAPI } = NativeModules;
const DataReceivedEvent = 'dataReceived';

/**
 * @name Subscription
 * @class
 * @hideconstructor
 */

/**
 * Unsubscribe
 * @method
 * @name  Subscription#remove
 */

/**
 * @callback listener
 * @param {Buffer} buffer data {@link https://github.com/feross/buffer|Buffer}
 */

/**
 * SerialPort wrapper
 * @class
 * @hideconstructor
 */
class SerialPort {
  constructor(serialPort, eventEmitter) {
    this.path = serialPort.path;
    this.serialPort = serialPort;
    this.eventEmitter = eventEmitter;
    this.listeners = [];
  }

  /**
   * @return {string} device path
   */
  getPath() {
    return this.path;
  }

  /**
   * Send hex data
   * @param {string} hex the hex of data
   * @returns {Promise} success promise
   */
  send(hex) {
    return SerialPortAPI.send(this.path, hex)
  }

  /**
   * Add data received listener
   * @param {listener} listener
   * @returns {Subscription} subscription
   */
  onReceived(listener) {
    const listenerProxy = (event) => {
      if (!event.data) {
        return;
      }

      const buff = Buffer.from(event.data, 'hex');
      listener(buff);
    }

    this.listeners.push({
      event: DataReceivedEvent,
      listener: listenerProxy
    });
    return this.eventEmitter.addListener(DataReceivedEvent, listenerProxy)
  }

  /**
   * Close serial port
   */
  close() {
    for (var i = 0; i < this.listeners.length; i++) {
      let {event, listener} = this.listeners[i];
      this.eventEmitter.removeListener(event, listener)
    }
    return SerialPortAPI.close(this.path)
  }
}

export default SerialPort;
