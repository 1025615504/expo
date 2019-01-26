import { CentralState, CharacteristicProperty, PeripheralState, TransactionType, } from './Bluetooth.types';
import { BLUETOOTH_EVENT, DELIMINATOR, EVENTS, TYPES } from './BluetoothConstants';
import { addHandlerForKey, addListener, fireMultiEventHandlers, firePeripheralObservers, getHandlersForKey, resetHandlersForKey, } from './BluetoothEventHandler';
import { invariantAvailability, invariantUUID } from './BluetoothInvariant';
import { clearPeripherals, getPeripherals, updateStateWithPeripheral } from './BluetoothLocalState';
import { peripheralIdFromId } from './BluetoothTransactions';
import ExpoBluetooth from './ExpoBluetooth';
import Transaction from './Transaction';
export { CentralState, PeripheralState, TransactionType, CharacteristicProperty, };
/*
initializeManagerAsync
deallocateManagerAsync

getPeripheralsAsync
getCentralAsync
startScanningAsync
stopScanningAsync
connectPeripheralAsync
readRSSIAsync
readDescriptorAsync
writeDescriptorAsync
writeCharacteristicAsync
readCharacteristicAsync
setNotifyCharacteristicAsync

discoverDescriptorsForCharacteristicAsync
discoverCharacteristicsForServiceAsync
discoverIncludedServicesForServiceAsync
disconnectPeripheralAsync
*/
export { BLUETOOTH_EVENT, TYPES, EVENTS };
export async function startScanAsync(scanSettings = {}) {
    invariantAvailability('startScanningAsync');
    const { serviceUUIDsToQuery = [], scanningOptions = {}, callback = function () { } } = scanSettings;
    /* Prevents the need for CBCentralManagerScanOptionAllowDuplicatesKey in the info.plist */
    const serviceUUIDsWithoutDuplicates = [...new Set(serviceUUIDsToQuery)];
    /* iOS:
     *
     * Although strongly discouraged,
     * if <i>serviceUUIDs</i> is <i>nil</i> all discovered peripherals will be returned.
     * If the central is already scanning with different
     * <i>serviceUUIDs</i> or <i>options</i>, the provided parameters will replace them.
     */
    return await ExpoBluetooth.startScanningAsync(serviceUUIDsWithoutDuplicates, scanningOptions);
    // return addHandlerForKey(EVENTS.CENTRAL_DID_DISCOVER_PERIPHERAL, callback)
}
export async function stopScanAsync() {
    invariantAvailability('stopScanningAsync');
    // Remove all callbacks
    resetHandlersForKey(EVENTS.CENTRAL_DID_DISCOVER_PERIPHERAL);
    await ExpoBluetooth.stopScanningAsync();
}
// Avoiding using "start" in passive method names
export async function observeUpdatesAsync(callback) {
    return addHandlerForKey('everything', callback);
}
export async function observeStateAsync(callback) {
    const central = await getCentralAsync();
    callback(central.state);
    return addHandlerForKey(EVENTS.CENTRAL_DID_UPDATE_STATE, callback);
}
export async function connectAsync(peripheralUUID, options = {}) {
    invariantAvailability('connectPeripheralAsync');
    invariantUUID(peripheralUUID);
    const { onDisconnect } = options;
    if (onDisconnect) {
        addHandlerForKey(EVENTS.CENTRAL_DID_DISCONNECT_PERIPHERAL, onDisconnect);
    }
    let timeoutTag;
    if (options.timeout) {
        timeoutTag = setTimeout(() => {
            disconnectAsync(peripheralUUID);
            throw new Error('request timeout');
        }, options.timeout);
    }
    let result;
    try {
        result = await ExpoBluetooth.connectPeripheralAsync(peripheralUUID, options.options);
    }
    catch (error) {
        throw error;
    }
    finally {
        clearTimeout(timeoutTag);
    }
    return result;
}
export async function disconnectAsync(peripheralUUID) {
    invariantAvailability('disconnectPeripheralAsync');
    invariantUUID(peripheralUUID);
    return await ExpoBluetooth.disconnectPeripheralAsync(peripheralUUID);
}
/* TODO: Bacon: Add a return type */
export async function readDescriptorAsync({ peripheralUUID, serviceUUID, characteristicUUID, descriptorUUID, }) {
    const { descriptor } = await ExpoBluetooth.readDescriptorAsync({
        peripheralUUID,
        serviceUUID,
        characteristicUUID,
        descriptorUUID,
        characteristicProperties: CharacteristicProperty.Read,
    });
    return descriptor.value;
}
/* TODO: Bacon: Add a return type */
export async function writeDescriptorAsync({ peripheralUUID, serviceUUID, characteristicUUID, descriptorUUID, data, }) {
    invariantAvailability('writeDescriptorAsync');
    const { descriptor } = await ExpoBluetooth.writeDescriptorAsync({
        peripheralUUID,
        serviceUUID,
        characteristicUUID,
        descriptorUUID,
        data,
        characteristicProperties: CharacteristicProperty.Write,
    });
    return descriptor;
}
export async function shouldNotifyDescriptorAsync({ peripheralUUID, serviceUUID, characteristicUUID, descriptorUUID, shouldNotify, }) {
    invariantAvailability('setNotifyCharacteristicAsync');
    const { descriptor } = await ExpoBluetooth.setNotifyCharacteristicAsync({
        peripheralUUID,
        serviceUUID,
        characteristicUUID,
        descriptorUUID,
        shouldNotify,
    });
    return descriptor;
}
/* TODO: Bacon: Add a return type */
export async function readCharacteristicAsync({ peripheralUUID, serviceUUID, characteristicUUID, }) {
    const { characteristic } = await ExpoBluetooth.readCharacteristicAsync({
        peripheralUUID,
        serviceUUID,
        characteristicUUID,
        characteristicProperties: CharacteristicProperty.Read,
    });
    return characteristic.value;
}
/* TODO: Bacon: Add a return type */
export async function writeCharacteristicAsync({ peripheralUUID, serviceUUID, characteristicUUID, data, }) {
    const { characteristic } = await ExpoBluetooth.writeCharacteristicAsync({
        peripheralUUID,
        serviceUUID,
        characteristicUUID,
        data,
        characteristicProperties: CharacteristicProperty.Write,
    });
    return characteristic;
}
/* TODO: Bacon: Why would anyone use this? */
/* TODO: Bacon: Test if this works */
/* TODO: Bacon: Add a return type */
export async function writeCharacteristicWithoutResponseAsync({ peripheralUUID, serviceUUID, characteristicUUID, data, }) {
    const { characteristic } = await ExpoBluetooth.writeCharacteristicAsync({
        peripheralUUID,
        serviceUUID,
        characteristicUUID,
        data,
        characteristicProperties: CharacteristicProperty.WriteWithoutResponse,
    });
    return characteristic;
}
export async function readRSSIAsync(peripheralUUID) {
    invariantAvailability('readRSSIAsync');
    invariantUUID(peripheralUUID);
    return await ExpoBluetooth.readRSSIAsync(peripheralUUID);
}
export async function requestMTUAsync(peripheralUUID, MTU) {
    invariantAvailability('requestMTUAsync');
    invariantUUID(peripheralUUID);
    return await ExpoBluetooth.requestMTUAsync(peripheralUUID, MTU);
}
export async function getPeripheralsAsync() {
    invariantAvailability('getPeripheralsAsync');
    return await ExpoBluetooth.getPeripheralsAsync();
}
export async function getCentralAsync() {
    invariantAvailability('getCentralAsync');
    return await ExpoBluetooth.getCentralAsync();
}
export async function isScanningAsync() {
    const { isScanning } = await getCentralAsync();
    return isScanning;
}
// TODO: Bacon: Add serviceUUIDs
export async function discoverServicesForPeripheralAsync(options) {
    invariantAvailability('discoverServicesForPeripheralAsync');
    const transaction = Transaction.fromTransactionId(options.id);
    return await ExpoBluetooth.discoverServicesForPeripheralAsync({
        ...transaction.getUUIDs(),
        serviceUUIDs: options.serviceUUIDs,
        characteristicProperties: options.characteristicProperties,
    });
}
export async function discoverIncludedServicesForServiceAsync(options) {
    invariantAvailability('discoverIncludedServicesForServiceAsync');
    const transaction = Transaction.fromTransactionId(options.id);
    return await ExpoBluetooth.discoverIncludedServicesForServiceAsync({
        ...transaction.getUUIDs(),
        serviceUUIDs: options.serviceUUIDs,
    });
}
export async function discoverCharacteristicsForServiceAsync(options) {
    invariantAvailability('discoverCharacteristicsForServiceAsync');
    const transaction = Transaction.fromTransactionId(options.id);
    return await ExpoBluetooth.discoverCharacteristicsForServiceAsync({
        ...transaction.getUUIDs(),
        serviceUUIDs: options.serviceUUIDs,
        characteristicProperties: options.characteristicProperties,
    });
}
export async function discoverDescriptorsForCharacteristicAsync(options) {
    invariantAvailability('discoverDescriptorsForCharacteristicAsync');
    const transaction = Transaction.fromTransactionId(options.id);
    return await ExpoBluetooth.discoverDescriptorsForCharacteristicAsync({
        ...transaction.getUUIDs(),
        serviceUUIDs: options.serviceUUIDs,
        characteristicProperties: options.characteristicProperties,
    });
    // return await discoverAsync({ id });
}
export async function loadPeripheralAsync({ id }, skipConnecting = false) {
    const peripheralId = peripheralIdFromId(id);
    const peripheral = getPeripherals()[peripheralId];
    if (!peripheral) {
        throw new Error('Not a peripheral ' + peripheralId);
    }
    if (peripheral.state !== 'connected') {
        if (!skipConnecting) {
            const connectedPeripheral = await connectAsync(peripheralId, {
                onDisconnect: (...props) => {
                    console.log('On Disconnect public callback', ...props);
                },
            });
            console.log('loadPeripheralAsync(): connected!');
            return loadPeripheralAsync(connectedPeripheral, true);
        }
        else {
            // This should never be called because in theory connectAsync would throw an error.
        }
    }
    else if (peripheral.state === 'connected') {
        console.log('loadPeripheralAsync(): loadChildrenRecursivelyAsync!');
        await loadChildrenRecursivelyAsync({ id: peripheralId });
    }
    // In case any updates occured during this function.
    return getPeripherals()[peripheralId];
}
export async function loadChildrenRecursivelyAsync({ id }) {
    const components = id.split(DELIMINATOR);
    console.log({ components });
    if (components.length === 4) {
        // Descriptor ID
        throw new Error('Descriptors have no children');
    }
    else if (components.length === 3) {
        // Characteristic ID
        console.log('Load Characteristic ', id);
        // DEBUG
        // console.warn('DISABLE ME');
        // return [];
        const { characteristic: { descriptors }, } = await discoverDescriptorsForCharacteristicAsync({ id });
        return descriptors;
    }
    else if (components.length === 2) {
        // Service ID
        console.log('Load Service ', id);
        const { service } = await discoverCharacteristicsForServiceAsync({ id });
        console.log('LOADED CHARACTERISTICS FROM SERVICE', service);
        return await Promise.all(service.characteristics.map(characteristic => loadChildrenRecursivelyAsync(characteristic)));
    }
    else if (components.length === 1) {
        // Peripheral ID
        console.log('Load Peripheral ', id);
        const { peripheral: { services }, } = await discoverServicesForPeripheralAsync({ id });
        return await Promise.all(services.map(service => loadChildrenRecursivelyAsync(service)));
    }
    else {
        throw new Error(`Unknown ID ${id}`);
    }
}
addListener(({ data, event }) => {
    const { transactionId, peripheral, peripherals, central, advertisementData, RSSI, error } = data;
    console.log('GOT EVENT: ', { data, event });
    if (event === 'UPDATE') {
        clearPeripherals();
        if (peripherals) {
            for (const peripheral of peripherals) {
                updateStateWithPeripheral(peripheral);
            }
        }
        firePeripheralObservers();
        return;
    }
    switch (event) {
        case EVENTS.CENTRAL_DID_DISCONNECT_PERIPHERAL:
        case EVENTS.CENTRAL_DID_DISCOVER_PERIPHERAL:
            fireMultiEventHandlers(event, { central, peripheral });
            firePeripheralObservers();
            return;
        case EVENTS.CENTRAL_DID_UPDATE_STATE:
            if (!central) {
                throw new Error('EXBluetooth: Central not defined while processing: ' + event);
            }
            for (const callback of getHandlersForKey(event)) {
                callback(central.state);
            }
            return;
        case EVENTS.CENTRAL_DID_RETRIEVE_CONNECTED_PERIPHERALS:
        case EVENTS.CENTRAL_DID_RETRIEVE_PERIPHERALS:
            return;
        default:
            throw new Error('EXBluetooth: Unhandled event: ' + event);
    }
});
//# sourceMappingURL=Bluetooth.js.map