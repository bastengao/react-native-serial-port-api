import { NativeModules } from 'react-native';
import { Buffer } from 'buffer';

const { SerialPortAPI } = NativeModules;
const DataReceivedEvent = 'dataReceived';

export default class Wrapper {
  constructor(serialPort, eventEmitter) {
    this.path = serialPort.path;
    this.serialPort = serialPort;
    this.eventEmitter = eventEmitter;
    this.listeners = [];
  }

  getPath() {
    return this.path;
  }

  send(hex) {
    return SerialPortAPI.send(this.path, hex)
  }

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

  close() {
    for (var i = 0; i < this.listeners.length; i++) {
      let {event, listener} = this.listeners[i];
      this.eventEmitter.removeListener(event, listener)
    }
    return SerialPortAPI.close(this.path)
  }
}
