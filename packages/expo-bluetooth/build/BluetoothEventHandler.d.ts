import { Subscription } from 'expo-core';
import { Central, NativePeripheral } from './Bluetooth.types';
export declare function firePeripheralObservers(): void;
export declare function fireMultiEventHandlers(event: string, { central, peripheral }: {
    central?: Central | null;
    peripheral?: NativePeripheral | null;
}): void;
export declare function resetHandlersForKey(key: any): void;
export declare function addHandlerForKey(key: string, callback: (updates: any) => void): Subscription;
export declare function getHandlersForKey(key: any): any;
export declare function addListener(listener: (event: any) => void): Subscription;
export declare function removeAllListeners(): void;
