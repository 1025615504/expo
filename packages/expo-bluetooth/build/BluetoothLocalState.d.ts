import { NativePeripheral } from './Bluetooth.types';
import { UUID } from './Bluetooth';
export declare function getPeripherals(): {
    [peripheralId: string]: NativePeripheral;
};
export declare function getPeripheralForId(id: string): any;
export declare function clearPeripherals(): void;
export declare function removePeripheral(uuid: UUID): void;
export declare function updateStateWithPeripheral(peripheral: NativePeripheral): void;
export declare function updateAdvertismentDataStore(peripheralId: string, advertisementData: any): void;
