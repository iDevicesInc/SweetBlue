package com.idevicesinc.sweetblue;

import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

/**
 * 
 * 
 *
 */
class P_Characteristic
{
	private final BluetoothGattCharacteristic m_native;
	private final P_Service m_service;
	private final BleDevice m_device;
	
	public P_Characteristic(P_Service service, BluetoothGattCharacteristic characteristic_native)
	{
		m_service = service;
		m_device = m_service.getDevice();
		m_native = characteristic_native;
	}
	
//	public BleCharacteristic(BleDevice device, BluetoothGattCharacteristic characteristic_native)
//	{
//		m_service = null;
//		m_device = device;
//		m_native = characteristic_native;
//	}
	
	public UUID getUuid()
	{
		return m_native.getUuid();
	}
	
	public BluetoothGattCharacteristic getNative()
	{
		return m_native;
	}
	
	public BleDevice getDevice()
	{
		return m_device;
	}
	
	public P_Service getService()
	{
		return m_service;
	}
	
	boolean isSupported(int BluetoothGattCharacteristic_PROPERTY)
	{
		BluetoothGattCharacteristic char_native = getGuaranteedNative();
		
		if( char_native == null )  return false;
		
		return (char_native.getProperties() & BluetoothGattCharacteristic_PROPERTY) != 0x0;
	}
	
	BluetoothGattCharacteristic getGuaranteedNative()
	{
		if( m_service == null )  return m_native;
		
		BluetoothGatt gatt = m_service.getDevice().getNativeGatt();
		
		if( gatt == null )  return m_native;
		
		BluetoothGattService service = gatt.getService(m_service.getUuid());
		
		if( service == null )  return m_native;
		
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(getUuid());
		
		if( characteristic == null )  return m_native;
		
		return characteristic;
	}
}
