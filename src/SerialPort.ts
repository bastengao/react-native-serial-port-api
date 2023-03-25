import { EventEmitter, EventSubscription, NativeModules } from 'react-native';
import { Buffer } from 'buffer';

const { SerialPortAPI } = NativeModules;
const DataReceivedEvent = 'dataReceived';

export interface SerialPortWrapper {
  path: string;
}

export interface EventData {
  data: string;
  path: string;
}

export interface ListenerProxy {
  event: string;
  listener: (data: EventData) => void;
}

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
  private path: string;
  private eventEmitter: any;
  private listeners: ListenerProxy[];
  private subscriptions: EventSubscription[];

  constructor(serialPort: SerialPortWrapper, eventEmitter: EventEmitter) {
    this.path = serialPort.path;
    this.eventEmitter = eventEmitter;
    this.listeners = [];
    this.subscriptions = [];
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
  send(hex: string): Promise<boolean> {
    return SerialPortAPI.send(this.path, hex)
  }

  /**
   * Add data received listener
   * @param {listener} listener
   * @returns {Subscription} subscription
   */
  onReceived(listener: (buffer: Buffer) => void): EventSubscription {
    const listenerProxy = (event: EventData) => {
      if (!event.path || this.path !== event.path) {
        return;
      }
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
    const sub = this.eventEmitter.addListener(DataReceivedEvent, listenerProxy)
    this.subscriptions.push(sub);
    return sub;
  }

  /**
   * Close serial port
   */
  close() {
    for (var i = 0; i < this.subscriptions.length; i++) {
      const sub = this.subscriptions[i];
      sub.remove();
    }
    return SerialPortAPI.close(this.path)
  }
}

export default SerialPort;
