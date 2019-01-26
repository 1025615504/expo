package expo.modules.bluetooth.objects;

import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import expo.core.ModuleRegistry;
import expo.core.Promise;
import expo.modules.bluetooth.BluetoothConstants;
import expo.modules.bluetooth.BluetoothError;
import expo.modules.bluetooth.BluetoothModule;
import expo.modules.bluetooth.Serialize;
import expo.modules.bluetooth.helpers.UUIDHelper;
import expo.modules.bluetooth.objects.EXBluetoothObject;


// Wrapper for GATT because GATT can access Device
public class Peripheral extends BluetoothGattCallback implements EXBluetoothObjectInterface {

  private static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

  public static ArrayList<Bundle> listToJSON(List<Peripheral> input) {
    if (input == null) return null;

    ArrayList<Bundle> output = new ArrayList();
    for (Peripheral value : input) {
      output.add(value.toJSON());
    }
    return output;
  }

  public final BluetoothDevice device;
  public BluetoothGatt mGatt;
  private ScanRecord advertisingData;

  private byte[] advertisingDataBytes;
  public int advertisingRSSI;
  private boolean connected = false;
  public int MTU;

  Promise _didDisconnectPeripheralBlock;
  HashMap<String, Promise> _didConnectPeripheralBlock = new HashMap<String, Promise>();
  HashMap<String, Promise> _readValueBlocks = new HashMap<String, Promise>();
  HashMap<String, Promise> _writeValueBlocks = new HashMap<String, Promise>();
  HashMap<String, Promise> _didDiscoverServicesBlock = new HashMap<String, Promise>();
  HashMap<String, Promise> _didRequestMTUBlock = new HashMap<String, Promise>();

  Promise _readRSSIBlock;


  public Peripheral(BluetoothDevice device, int advertisingRSSI, byte[] scanRecord) {
    this.device = device;
    this.advertisingRSSI = advertisingRSSI;
    this.advertisingDataBytes = scanRecord;
  }

  public Peripheral(BluetoothDevice device, int advertisingRSSI, ScanRecord scanRecord) {
    this.device = device;
    this.advertisingRSSI = advertisingRSSI;
    this.advertisingData = scanRecord;
    this.advertisingDataBytes = scanRecord.getBytes();
  }

  public Peripheral(BluetoothDevice device, Activity activity) {
    this.device = device;
    assignGATT(activity);
  }

  public Peripheral(BluetoothGatt gatt) {
    this.device = gatt.getDevice();
    mGatt = gatt;
  }

  @Override
  public UUID getUUID() {
    return null;
  }

  @Override
  public Bundle toJSON() {

    Bundle output = new Bundle();

    ArrayList<Bundle> services = EXBluetoothObject.listToJSON((List)getServices());
    output.putParcelableArrayList(BluetoothConstants.JSON.SERVICES, services);
    output.putString(BluetoothConstants.JSON.NAME, device.getName());
    output.putString(BluetoothConstants.JSON.ID, getID());
    output.putString(BluetoothConstants.JSON.UUID, getID());
    output.putString(BluetoothConstants.JSON.STATE, isConnected() ? "connected" : "disconnected");

    if (mGatt != null) {
      output.putInt(BluetoothConstants.JSON.MTU, MTU);
    } else {
      output.putInt(BluetoothConstants.JSON.MTU, 576); // TODO: Bacon: annotate
    }

    return output;
  }

  public List<Service> getServices() {
    ArrayList output = new ArrayList<>();
    if (mGatt != null && mGatt.getServices() != null && mGatt.getServices().size() > 0) {
      List<BluetoothGattService> input = mGatt.getServices();
      for (BluetoothGattService value : input) {
        output.add(new Service(value, this));
      }
    }
    return output;
  }

  @Override
  public String getID() {
    return device.getAddress();
  }

  @Override
  public EXBluetoothObject getParent() {
    return null;
  }

  public void connect(Promise promise, Activity activity) {
    if (!connected) {
      assignGATT(activity);
      _didConnectPeripheralBlock.put(UUIDHelper.fromUUID(this.getUUID()), promise);
    } else {
      if (guardGATT(promise)) {
        return;
      }
      promise.resolve(toJSON());
    }
  }

  private void assignGATT(Activity activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      mGatt = device.connectGatt(activity, false, this);
    } else {
      mGatt = device.connectGatt(activity, false, this, BluetoothDevice.TRANSPORT_LE);
    }
  }

  // TODO: Bacon: Is this overriding the StateChange method
  public void disconnect(Promise promise) {
    connected = false;
    if (mGatt != null) {
      try {
        _didDisconnectPeripheralBlock = promise;
        mGatt.disconnect();
        mGatt.close();
        mGatt = null;
//        Log.d(BluetoothConstants.ERRORS.GENERAL, "Disconnect");
//        sendDisconnectedEvent(null);
      } catch (Exception e) {

        //TODO: Bacon: Add more of a standard around errors
        Bundle errorPayload = new Bundle();
        errorPayload.putString(BluetoothConstants.JSON.MESSAGE, e.getMessage());

        BluetoothError.reject(_didDisconnectPeripheralBlock, e.getMessage());
        _didDisconnectPeripheralBlock = null;
//        sendDisconnectedEvent(errorPayload);
        return;
      }
    } else {
      _didDisconnectPeripheralBlock = null;
      BluetoothError.reject(_didDisconnectPeripheralBlock, BluetoothConstants.ERRORS.GENERAL);
      Log.d(BluetoothConstants.ERRORS.GENERAL, "GATT is null");
    }
  }

  // TODO: Bacon: [iOS] Are solicitedServiceUUIDs overflowServiceUUIDs possible
  public Bundle advertisementData() {
    Bundle advertising = new Bundle();
    AdvertisementData advertisementData = AdvertisementData.parseScanResponseData(advertisingDataBytes);
    advertising.putString(BluetoothConstants.JSON.LOCAL_NAME, advertisementData.getLocalName());
    // TODO: Bacon: Should we use direct values instead: advertisingData.getTxPowerLevel()
    advertising.putInt(BluetoothConstants.JSON.TX_POWER_LEVEL, advertisementData.getTxPowerLevel());
    advertising.putBoolean(BluetoothConstants.JSON.IS_CONNECTABLE, true);
    advertising.putString(BluetoothConstants.JSON.MANUFACTURER_DATA, Base64.encodeToString(advertisementData.getManufacturerData(), Base64.NO_WRAP));
    Bundle serviceData = new Bundle();
    if (advertisementData.getServiceData() != null) {
      for (Map.Entry<UUID, byte[]> entry : advertisementData.getServiceData().entrySet()) {
        if (entry.getValue() != null) {
          serviceData.putString(UUIDHelper.fromUUID((entry.getKey())), Base64.encodeToString(entry.getValue(), Base64.NO_WRAP));
        }
      }
    }
    advertising.putBundle(BluetoothConstants.JSON.SERVICE_DATA, serviceData);

    if (advertisingData != null) {
      String deviceName = advertisingData.getDeviceName();
      if (deviceName != null) {
        advertising.putString(BluetoothConstants.JSON.LOCAL_NAME, deviceName.replace("\0", ""));
      }

      advertising.putStringArrayList(BluetoothConstants.JSON.SERVICE_UUIDS, Serialize.UUIDList_NativeToJSON(advertisementData.getServiceUUIDs()));
      advertising.putStringArrayList("solicitedServiceUUIDs", Serialize.UUIDList_NativeToJSON(advertisementData.getSolicitedServiceUUIDs()));
    }
    return advertising;
  }

  public boolean isConnected() {
    return connected;
  }

  public BluetoothDevice getDevice() {
    return device;
  }

  // didDiscoverServices
  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    super.onServicesDiscovered(gatt, status);
//    sendEvent(BluetoothConstants.OPERATIONS.SCAN, BluetoothConstants.EVENTS.CENTRAL_DID_DISCOVER_PERIPHERAL, BluetoothError.errorFromGattStatus(status));

    String id = getID();
    // TODO: EMIT STATE
    if (_didDiscoverServicesBlock.containsKey(id)) {
      Promise promise = _didDiscoverServicesBlock.get(id);
      if (autoResolvePromiseWithStatusAndData(promise, status)) {
        Bundle output = new Bundle();
        output.putBundle(BluetoothConstants.JSON.PERIPHERAL, toJSON());
        promise.resolve(output);
      }
      _didDiscoverServicesBlock.remove(id);
    }
  }

  protected void sendEvent(String transaction, String eventName, Bundle error) {
    Bundle output = new Bundle();
    output.putBundle(BluetoothConstants.JSON.PERIPHERAL, toJSON());
    output.putString(BluetoothConstants.JSON.TRANSACTION_ID, transactionIdForOperation(transaction));
    output.putBundle(BluetoothConstants.JSON.ERROR, error);
    BluetoothModule.sendEvent(eventName, output);
  }


  private boolean autoResolvePromiseWithStatusAndData(Promise promise, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      return true;
    }
    BluetoothError.rejectWithStatus(promise, status);
    return false;
  }

    @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    mGatt = gatt;

    switch (newState) {
      case BluetoothProfile.STATE_CONNECTED:
        connected = true;
        device.createBond();

        // Send Connection event
        sendEvent(BluetoothConstants.OPERATIONS.CONNECT, BluetoothConstants.EVENTS.CENTRAL_DID_CONNECT_PERIPHERAL, null);

        String UUIDString = UUIDHelper.fromUUID(getUUID());
        if (_didConnectPeripheralBlock.containsKey(UUIDString)) {
          Promise promise = _didConnectPeripheralBlock.get(UUIDString);
          if (autoResolvePromiseWithStatusAndData(promise, status)) {
            promise.resolve(toJSON());
          }
          _didConnectPeripheralBlock.remove(UUIDString);
        }

        break;
      case BluetoothProfile.STATE_DISCONNECTED:
        if (connected) {
          connected = false;
          if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            mGatt = null;
          }
        }
        if (_didDisconnectPeripheralBlock != null) {
          if (autoResolvePromiseWithStatusAndData(_didDisconnectPeripheralBlock, status)) {
            _didDisconnectPeripheralBlock.resolve(toJSON());
          }
          _didDisconnectPeripheralBlock = null;
        }
        break;
      case BluetoothProfile.STATE_CONNECTING:
      case BluetoothProfile.STATE_DISCONNECTING:
      default:
        // TODO: Bacon: Unhandled
        break;
    }
  }

  public void updateRSSI(int RSSI) {
    advertisingRSSI = RSSI;
  }

  public void updateData(byte[] data) {
    advertisingDataBytes = data;
  }

  public void updateData(ScanRecord scanRecord) {
    advertisingData = scanRecord;
    advertisingDataBytes = scanRecord.getBytes();
  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    super.onCharacteristicChanged(gatt, characteristic);
    Characteristic characteristicInstance = new Characteristic(characteristic, gatt);
    characteristicInstance.sendEvent(BluetoothConstants.OPERATIONS.READ, BluetoothConstants.EVENTS.PERIPHERAL_DID_UPDATE_VALUE_FOR_CHARACTERISTIC, BluetoothGatt.GATT_SUCCESS);
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicRead(gatt, characteristic, status);
    Characteristic characteristicInstance = new Characteristic(characteristic, gatt);

    if (_readValueBlocks.containsKey(characteristicInstance.getID())) {
      Promise promise = _readValueBlocks.get(characteristicInstance.getID());

      if (autoResolvePromiseWithStatusAndData(promise, status)) {
        Bundle output = new Bundle();
        output.putBundle(BluetoothConstants.JSON.CHARACTERISTIC, characteristicInstance.toJSON());
        output.putBundle(BluetoothConstants.JSON.PERIPHERAL, this.toJSON());
        /* peripheral, characteristic */
        promise.resolve(output);
      }
      _readValueBlocks.remove(characteristicInstance.getID());
    }

//    characteristicInstance.sendEvent(BluetoothConstants.OPERATIONS.READ, BluetoothConstants.EVENTS.PERIPHERAL_DID_UPDATE_VALUE_FOR_CHARACTERISTIC, status);
  }

  @Override
  public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicWrite(gatt, characteristic, status);
    Characteristic input = new Characteristic(characteristic, gatt);
    input.sendEvent(BluetoothConstants.OPERATIONS.WRITE, BluetoothConstants.EVENTS.PERIPHERAL_DID_WRITE_VALUE_FOR_CHARACTERISTIC, status);
  }

  @Override
  public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    super.onDescriptorRead(gatt, descriptor, status);
    Descriptor input = new Descriptor(descriptor, gatt);

    if (_readValueBlocks.containsKey(input.getID())) {
      Promise promise = _readValueBlocks.get(input.getID());

      if (autoResolvePromiseWithStatusAndData(promise, status)) {
        Bundle output = new Bundle();
        output.putBundle(BluetoothConstants.JSON.DESCRIPTOR, input.toJSON());
        output.putBundle(BluetoothConstants.JSON.PERIPHERAL, this.toJSON());
        /* peripheral, descriptor */
        promise.resolve(output);
      }
      _readValueBlocks.remove(input.getID());
    }

//    input.sendEvent(BluetoothConstants.OPERATIONS.READ, BluetoothConstants.EVENTS.PERIPHERAL_DID_UPDATE_VALUE_FOR_DESCRIPTOR, status);
  }

  @Override
  public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    super.onDescriptorWrite(gatt, descriptor, status);
    String characteristicUUIDString = UUIDHelper.fromUUID(descriptor.getCharacteristic().getUuid());
    if (_writeValueBlocks.containsKey(characteristicUUIDString)) {
      Promise promise = _writeValueBlocks.get(characteristicUUIDString);
      if (autoResolvePromiseWithStatusAndData(promise, status)) {
        Characteristic characteristic = new Characteristic(descriptor.getCharacteristic(), gatt);
        Bundle output = new Bundle();
        output.putBundle(BluetoothConstants.JSON.CHARACTERISTIC, characteristic.toJSON());
        output.putBundle(BluetoothConstants.JSON.PERIPHERAL, this.toJSON());
        /* peripheral, descriptor */
        promise.resolve(output);
      }
      _writeValueBlocks.remove(characteristicUUIDString);
    }
//    Descriptor input = new Descriptor(descriptor, gatt);
//    input.sendEvent(BluetoothConstants.OPERATIONS.WRITE, BluetoothConstants.EVENTS.PERIPHERAL_DID_WRITE_VALUE_FOR_DESCRIPTOR, status);
  }

  @Override
  public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    super.onReadRemoteRssi(gatt, rssi, status);
    if (status == BluetoothGatt.GATT_SUCCESS) {
      updateRSSI(rssi);
    }

    if (_readRSSIBlock != null) {
      if (autoResolvePromiseWithStatusAndData(_readRSSIBlock, status)) {
        _readRSSIBlock.resolve(toJSON());
      }
      _readRSSIBlock = null;
    }
    // TODO: Bacon: Send RSSI event here - not done on iOS either
  }

  public Service getService(UUID uuid) {
    BluetoothGattService child = mGatt.getService(uuid);
    if (child == null) {
      return null;
    }
    return new Service(child, this);
  }

  public Service getServiceOrReject(String serviceUUIDString, Promise promise) {
    UUID uuid = UUIDHelper.toUUID(serviceUUIDString);
    Service service = getService(uuid);
    if (service == null) {
      BluetoothError.reject(promise, new BluetoothError(BluetoothError.Codes.NO_SERVICE, BluetoothError.Messages.NO_SERVICE));
      return null;
    }
    return service;
  }

  public Characteristic getCharacteristicOrReject(Service service, String characteristicUUIDString, int characteristicProperties, Promise promise) {
    UUID uuid = UUIDHelper.toUUID(characteristicUUIDString);
    try {
      List<Characteristic> characteristics = service.getCharacteristics();
      for (Characteristic characteristic : characteristics) {
        if ((characteristic.getCharacteristic().getProperties() & characteristicProperties) != 0 && uuid.equals(characteristic.getUUID())) {
          return characteristic;
        }
      }
      if (characteristicProperties == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
        return getCharacteristicOrReject(service, characteristicUUIDString, BluetoothGattCharacteristic.PROPERTY_INDICATE, promise);
      }
    } catch (Exception e) {
      Log.e(BluetoothConstants.ERRORS.GENERAL, "Error on characteristic: " + characteristicUUIDString, e);
    }
    BluetoothError.reject(promise, new BluetoothError(BluetoothError.Codes.NO_CHARACTERISTIC, BluetoothError.Messages.NO_CHARACTERISTIC));
    return null;
  }


  public Characteristic getCharacteristicOrReject(Service service, String characteristicUUIDString, Promise promise) {
    UUID uuid = UUIDHelper.toUUID(characteristicUUIDString);
    Characteristic characteristic = service.getCharacteristic(uuid);
    if (characteristic == null) {
      BluetoothError.reject(promise, new BluetoothError(BluetoothError.Codes.NO_CHARACTERISTIC, BluetoothError.Messages.NO_CHARACTERISTIC));
      return null;
    }

    return characteristic;
  }

  public Descriptor getDescriptorOrReject(Characteristic characteristic, String descriptorUUIDString, Promise promise) {
    UUID uuid = UUIDHelper.toUUID(descriptorUUIDString);
    Descriptor descriptor = characteristic.getDescriptor(uuid);
    if (descriptor == null) {
      BluetoothError.reject(promise, new BluetoothError(BluetoothError.Codes.NO_DESCRIPTOR, BluetoothError.Messages.NO_DESCRIPTOR));
      return null;
    }
    return descriptor;
  }

  public void writeDescriptor(
      byte[] data,
      final Descriptor descriptor,
      final Promise promise
  ) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }

    descriptor.setValue(data);

    if (mGatt.writeDescriptor(descriptor.getDescriptor())) {
      _writeValueBlocks.put(descriptor.getID(), promise);
    } else {
      BluetoothError.reject(promise, "Failed to write descriptor: " + descriptor.getID());
    }
  }

  public void readDescriptor(
      final Descriptor descriptor,
      final Promise promise
  ) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }

    if (mGatt.readDescriptor(descriptor.getDescriptor())) {
      _readValueBlocks.put(descriptor.getID(), promise);
    } else {
      BluetoothError.reject(promise, "Failed to read descriptor: " + descriptor.getID());
    }
  }

  public void writeCharacteristicAsync(
      byte[] data,
      final Characteristic characteristic,
      final Promise promise
  ) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }

    characteristic.setValue(data);

    if (mGatt.writeCharacteristic(characteristic.getCharacteristic())) {
      _writeValueBlocks.put(characteristic.getID(), promise);
    } else {
      BluetoothError.reject(promise, "Failed to write characteristic: " + characteristic.getID());
    }
  }

  public void readCharacteristicAsync(
      final Characteristic characteristic,
      final Promise promise
  ) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }

    if (mGatt.readCharacteristic(characteristic.getCharacteristic())) {
      _readValueBlocks.put(characteristic.getID(), promise);
    } else {
      BluetoothError.reject(promise, "Failed to read characteristic: " + characteristic.getID());
    }
  }

  // TODO: Bacon: Use writeDescriptor
  public void setNotify(Service service, String characteristicUUID, Boolean notify, Promise promise) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }

    Characteristic characteristic = getCharacteristicOrReject(service, characteristicUUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, promise);
    if (characteristic == null) {
      return;
    }

    if (!mGatt.setCharacteristicNotification(characteristic.getCharacteristic(), notify)) {
      promise.reject(BluetoothConstants.ERRORS.GENERAL, "Failed to register notification for " + characteristicUUID);
      return;
    }

    Descriptor descriptor = characteristic.getDescriptor(UUIDHelper.toUUID(CHARACTERISTIC_NOTIFICATION_CONFIG));

    if (descriptor == null) {
      promise.reject(BluetoothConstants.ERRORS.GENERAL, "Set notification failed for " + characteristicUUID);
      return;
    }

    // try notify before indicate
    if ((characteristic.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
      descriptor.setValue(notify ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    } else if ((characteristic.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
      descriptor.setValue(notify ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    } else {
      promise.reject(BluetoothConstants.ERRORS.GENERAL, "Characteristic " + characteristicUUID + " does not have NOTIFY or INDICATE property set");
      return;
    }
    try {
      if (mGatt.writeDescriptor(descriptor.getDescriptor())) {
        _writeValueBlocks.put(characteristic.getID(), promise);
        return;
      } else {
        promise.reject(BluetoothConstants.ERRORS.GENERAL, "Failed to set client characteristic notification for " + characteristicUUID);
        return;
      }
    } catch (Exception e) {
      promise.reject(BluetoothConstants.ERRORS.GENERAL, "Failed to set client characteristic notification for " + characteristicUUID + ", error: " + e.getMessage());
      return;
    }
  }

  public void readRSSI(Promise promise) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }

    if (!mGatt.readRemoteRssi()) {
      BluetoothError.reject(promise, "Failed  to read RSSI from peripheral: " + getID());
      return;
    }
    promise.resolve(null);
  }

//  public void refreshCache(Promise promise) {
//    try {
//
//      Method localMethod = mGatt.getClass().getMethod("refresh", new Class[0]);
//      if (localMethod != null) {
//        boolean res = ((Boolean) localMethod.invoke(mGatt, new Object[0])).booleanValue();
//        promise.resolve(res);
//      } else {
//        promise.reject(BluetoothConstants.ERRORS.GENERAL, "Could not refresh cache for device.");
//      }
//    } catch (Exception localException) {
//      promise.reject(localException);
//    }
//  }

  public void retrieveServices(Promise promise) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }
    mGatt.discoverServices();
    _didDiscoverServicesBlock.put(getID(), promise);
  }

  public void requestConnectionPriority(int connectionPriority, Promise promise) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }
    if (mGatt.requestConnectionPriority(connectionPriority)) {
      BluetoothError.reject(promise, "Failed to request connection priority: " + connectionPriority);
      return;
    }
    promise.resolve(null);
  }

  public void requestMTU(int mtu, Promise promise) {
    if (guardIsConnected(promise) || guardGATT(promise)) {
      return;
    }

    String id = getID();
    if (_didRequestMTUBlock.containsKey(id)) {
      promise.reject(BluetoothConstants.ERRORS.GENERAL, "Running concurrent task");
      return;
    }
    if (mGatt.requestMtu(mtu)) {
      _didRequestMTUBlock.put(id, promise);
      return;
    }
    BluetoothError.reject(promise, "Failed to request MTU: " + mtu);

  }

  @Override
  public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
    super.onMtuChanged(gatt, mtu, status);
    MTU = mtu;

    String id = getID();

    if (_didRequestMTUBlock.containsKey(id)) {
        Promise promise = _didRequestMTUBlock.get(id);
      if (autoResolvePromiseWithStatusAndData(promise, status)) {
        promise.resolve(mtu);
        }
      _didRequestMTUBlock.remove(getID());
    }
    // TODO: Bacon: Should we do something if an event is fired and there is no promise to resolve?

//    Bundle output = new Bundle();
//    output.putInt(BluetoothConstants.JSON.MTU, mtu);
//    output.putString(BluetoothConstants.JSON.TRANSACTION_ID, transactionIdForOperation(BluetoothConstants.OPERATIONS.MTU));
//    output.putBundle(BluetoothConstants.JSON.ERROR, BluetoothError.errorFromGattStatus(status));
//    BluetoothModule.sendEvent(BluetoothConstants.EVENTS.CENTRAL_DID_CONNECT_PERIPHERAL, output);
  }

  public boolean guardIsConnected(Promise promise) {
    if (!isConnected()) {
      promise.reject(BluetoothConstants.ERRORS.GENERAL, "Peripheral is not connected: " + getID());
      return true;
    }
    return false;
  }

  public boolean guardGATT(Promise promise) {
    if (mGatt == null) {
      promise.reject(BluetoothConstants.ERRORS.GENERAL, "GATT is not defined");
      return true;
    }
    return false;
  }

  @Override
  public Peripheral getPeripheral() {
    return this;
  }

  @Override
  public String transactionIdForOperation(String operation) {
    return operation + "|" + getID();
  }


  public void tearDown() {
    // TODO: Bacon: Deallocate
  }

}
