package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.BONDED;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDING;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Just some commented out snippets that I don't feel like digging through SVN for if I ever need them again.
 * 
 * @author dougkoellmer
 *
 */
class PS_BtSnippets
{
//	private void createBondIfNeeded(BleCharacteristic characteristic)
//	{
//		if( isInAnyState(BONDED, BONDING) )  return;
//		
//		BluetoothGattCharacteristic characteristic_native = characteristic.getNative();
//		
//		if( U_Bt.requiresBonding(characteristic_native) )
//		{
//			if( (characteristic_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0x0 )
//			{
//				m_logger.e("PROPERTY_SIGNED_WRITE " + m_logger.charName(characteristic_native.getUuid()));
//			}
//			if( (characteristic_native.getPermissions() & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) != 0x0 )
//			{
//				m_logger.e("PERMISSION_READ_ENCRYPTED " + m_logger.charName(characteristic_native.getUuid()));
//			}
//			if( (characteristic_native.getPermissions() & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) != 0x0 )
//			{
//				m_logger.e("PERMISSION_READ_ENCRYPTED_MITM " + m_logger.charName(characteristic_native.getUuid()));
//			}
//			if( (characteristic_native.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) != 0x0 )
//			{
//				m_logger.e("PERMISSION_WRITE_ENCRYPTED " + m_logger.charName(characteristic_native.getUuid()));
//			}
//			if( (characteristic_native.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) != 0x0 )
//			{
//				m_logger.e("PERMISSION_WRITE_ENCRYPTED_MITM " + m_logger.charName(characteristic_native.getUuid()));
//			}
//			if( (characteristic_native.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) != 0x0 )
//			{
//				m_logger.e("PERMISSION_WRITE_SIGNED " + m_logger.charName(characteristic_native.getUuid()));
//			}
//			if( (characteristic_native.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) != 0x0 )
//			{
//				m_logger.e("PERMISSION_WRITE_SIGNED_MITM " + m_logger.charName(characteristic_native.getUuid()));
//			}
			
//			createBond();
//		}
//	}
}
