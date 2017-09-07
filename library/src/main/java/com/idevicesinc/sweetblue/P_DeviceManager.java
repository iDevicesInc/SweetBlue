package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.idevicesinc.sweetblue.BleDevice.BondListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Status;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;

final class P_DeviceManager
{
    private final HashMap<String, BleDevice> m_map = new HashMap<String, BleDevice>();
    private final ArrayList<BleDevice> m_list = new ArrayList<BleDevice>();

    private final P_Logger m_logger;
    private final BleManager m_mngr;

    private boolean m_updating = false;


    P_DeviceManager(BleManager mngr)
    {
        m_mngr = mngr;
        m_logger = m_mngr.getLogger();
    }

    public ArrayList<BleDevice> getList()
    {
        return getList_private(false);
    }

    public ArrayList<BleDevice> getList_sorted()
    {
        return getList_private(true);
    }

    private ArrayList<BleDevice> getList_private(boolean sort)
    {
        if (sort && m_mngr.m_config.defaultListComparator != null)
        {
            Collections.sort(m_list, m_mngr.m_config.defaultListComparator);
        }
        return m_list;
    }

    void forEach(final Object forEach, final Object... query)
    {
        final boolean isQueryValid = query != null && query.length > 0;

        for (int i = 0; i < m_mngr.getDeviceCount(); i++)
        {
            final BleDevice ith = m_mngr.getDeviceAt(i);

            if (isQueryValid)
            {
                if (ith.is(query))
                {
                    if (!forEach_invoke(forEach, ith))
                    {
                        break;
                    }
                }
            }
            else
            {
                if (!forEach_invoke(forEach, ith))
                {
                    break;
                }
            }
        }
    }

    private boolean forEach_invoke(final Object forEach, final BleDevice device)
    {
        if (forEach instanceof ForEach_Breakable)
        {
            ForEach_Breakable<BleDevice> forEach_cast = (ForEach_Breakable<BleDevice>) forEach;

            final ForEach_Breakable.Please please = forEach_cast.next(device);

            return please.shouldContinue();
        }
        else if (forEach instanceof ForEach_Void)
        {
            ForEach_Void<BleDevice> forEach_cast = (ForEach_Void<BleDevice>) forEach;

            forEach_cast.next(device);

            return true;
        }

        return false;
    }

    BleDevice getDevice_offset(final BleDevice device, final int offset, Object... query)
    {
        final int index = m_mngr.getDeviceIndex(device);
        final int offset_override = offset < 0 ? -1 : 1;
        final boolean isQueryValid = query != null && query.length > 0;

        if (index >= 0)
        {
            BleDevice device_ith = BleDevice.NULL;
            int nextIndex = index + offset_override;
            do
            {
                if (nextIndex < 0)
                {
                    nextIndex = m_mngr.getDeviceCount() - 1;
                }
                else if (nextIndex >= m_mngr.getDeviceCount())
                {
                    nextIndex = 0;
                }
                else
                {
                    nextIndex = nextIndex;
                }

                device_ith = m_mngr.getDeviceAt(nextIndex);

                if (isQueryValid)
                {
                    if (device_ith.is(query))
                    {
                        return device_ith;
                    }
                }
                else
                {
                    return device_ith;
                }

                nextIndex += offset_override;
            }
            while (!device_ith.equals(device) && !device_ith.equals(BleDevice.NULL));
        }
        else
        {
            if (isQueryValid)
            {
                if (m_mngr.hasDevice(query))
                {
                    return m_mngr.getDevice(query);
                }
                else
                {
                    return BleDevice.NULL;
                }
            }
            else
            {
                if (m_mngr.hasDevices())
                {
                    return m_mngr.getDevice();
                }
                else
                {
                    return BleDevice.NULL;
                }
            }
        }

        return BleDevice.NULL;
    }

    public BleDevice getDevice(final int mask_BleDeviceState)
    {
        for (int i = 0; i < getCount(); i++)
        {
            BleDevice device = get(i);

            if (device.isAny(mask_BleDeviceState))
            {
                return device;
            }
        }

        return BleDevice.NULL;
    }

    public List<BleDevice> getDevices_List(boolean sort, Object... query)
    {
        final ArrayList<BleDevice> toReturn = new ArrayList<BleDevice>();

        for (int i = 0; i < this.getCount(); i++)
        {
            final BleDevice device_ith = this.get(i);

            if (device_ith.is(query))
            {
                toReturn.add(device_ith);
            }
        }
        if (sort && m_mngr.m_config.defaultListComparator != null)
        {
            Collections.sort(toReturn, m_mngr.m_config.defaultListComparator);
        }
        return toReturn;
    }

    public List<BleDevice> getDevices_List(boolean sort, final BleDeviceState state)
    {
        final ArrayList<BleDevice> toReturn = new ArrayList<BleDevice>();

        for (int i = 0; i < this.getCount(); i++)
        {
            final BleDevice device_ith = this.get(i);

            if (device_ith.is(state))
            {
                toReturn.add(device_ith);
            }
        }
        if (sort && m_mngr.m_config.defaultListComparator != null)
        {
            Collections.sort(toReturn, m_mngr.m_config.defaultListComparator);
        }
        return toReturn;
    }

    public List<BleDevice> getDevices_List(boolean sort, final int mask_BleDeviceState)
    {
        final ArrayList<BleDevice> toReturn = new ArrayList<BleDevice>();

        for (int i = 0; i < this.getCount(); i++)
        {
            final BleDevice device_ith = this.get(i);

            if (device_ith.isAny(mask_BleDeviceState))
            {
                toReturn.add(device_ith);
            }
        }
        if (sort && m_mngr.m_config.defaultListComparator != null)
        {
            Collections.sort(toReturn, m_mngr.m_config.defaultListComparator);
        }
        return toReturn;
    }

    public boolean has(BleDevice device)
    {
        for (int i = 0; i < m_list.size(); i++)
        {
            BleDevice device_ith = m_list.get(i);

            if (device_ith == device) return true;
        }

        return false;
    }

    public BleDevice get(int i)
    {
        return m_list.get(i);
    }

    int getCount(Object[] query)
    {
        int count = 0;

        for (int i = 0; i < m_list.size(); i++)
        {
            BleDevice device_ith = m_list.get(i);

            if (device_ith.is(query))
            {
                count++;
            }
        }

        return count;
    }

    int getCount(BleDeviceState state)
    {
        int count = 0;

        for (int i = 0; i < m_list.size(); i++)
        {
            BleDevice device_ith = m_list.get(i);

            if (device_ith.is(state))
            {
                count++;
            }
        }

        return count;
    }

    int getCount()
    {
        return m_list.size();
    }

    public BleDevice get(String uniqueId)
    {
        return m_map.get(uniqueId);
    }

    void add(final BleDevice device)
    {
        m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
        {
            @Override public void run()
            {
                if (m_map.containsKey(device.getMacAddress()))
                {
                    m_logger.e("Already registered device " + device.getMacAddress());
                    return;
                }

                m_list.add(device);
                m_map.put(device.getMacAddress(), device);
            }
        });
    }

    void remove(final BleDevice device, final P_DeviceManager cache)
    {
        // Seeing as this is being posted to the update thread, this check is now unnecessary
        // m_mngr.ASSERT(!m_updating, "Removing device while updating!");
        m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
        {
            @Override public void run()
            {
                doRemoval(device, cache);
            }
        });
    }

    void removeAll(final P_DeviceManager cache)
    {
        m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
        {
            @Override public void run()
            {
                final List<BleDevice> list = new ArrayList<>(m_list);
                for (BleDevice d : list)
                {
                    remove(d, cache);
                }
            }
        });
    }

    private void doRemoval(BleDevice device, P_DeviceManager cache)
    {
        m_mngr.ASSERT(m_map.containsKey(device.getMacAddress()));

        m_list.remove(device);
        m_map.remove(device.getMacAddress());

        final boolean cacheDevice = BleDeviceConfig.bool(device.conf_device().cacheDeviceOnUndiscovery, device.conf_mngr().cacheDeviceOnUndiscovery);

        if (cacheDevice && cache != null)
        {
            cache.add(device);
        }
    }

    void update(double timeStep)
    {
        //--- DRK > The asserts here and keeping track of "is updating" is because
        //---		once upon a time we iterated forward through the list with an end
        //---		condition based on the length assigned to a local variable before
        //---		looping (i.e. not checking the length of the array itself every iteration).
        //---		On the last iteration we got an out of bounds exception, so it seems somehow
        //---		that the array was modified up the call stack from this method, or from another
        //---		thread. After heavily auditing the code it's not clear how either situation could
        //---		happen. Note that we were using Collections.serializedList (or something, check SVN),
        //---		and not plain old ArrayList like we are now, if that has anything to do with it.

        if (m_updating)
        {
            m_mngr.ASSERT(false, "Already updating.");

            return;
        }

        m_updating = true;

        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            BleDevice ithDevice = m_list.get(i);
            ithDevice.update(timeStep);
        }

        m_updating = false;
    }

    void unbondAll(PE_TaskPriority priority, BondListener.Status status)
    {
        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            BleDevice device = get(i);

            if (device.m_bondMngr.isNativelyBondingOrBonded())
            {
                device.unbond_internal(priority, status);
            }
        }
    }

    void undiscoverAll()
    {
        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            final BleDevice device = get(i);

            device.undiscover();
        }
    }

    void disconnectAll()
    {
        final ArrayList<BleDevice> list = new ArrayList<>(m_list);
        for (int i = list.size() - 1; i >= 0; i--)
        {
            final BleDevice device = list.get(i);

            device.disconnect();
        }
    }

    void disconnectAll_remote()
    {
        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            final BleDevice device = get(i);

            device.disconnect_remote();
        }
    }

    void disconnectAllForTurnOff(PE_TaskPriority priority)
    {
        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            final BleDevice device = get(i);

            //--- DRK > Just an early-out performance check here.
            if (device.isAny(BleDeviceState.CONNECTING_OVERALL, BleDeviceState.CONNECTED))
            {
                device.disconnectWithReason(priority, Status.BLE_TURNING_OFF, ConnectionFailListener.Timing.NOT_APPLICABLE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, false, device.NULL_READWRITE_EVENT());
            }
        }
    }

    void rediscoverDevicesAfterBleTurningBackOn()
    {
        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            BleDevice device = (BleDevice) m_list.get(i);

            if (!device.is(BleDeviceState.DISCOVERED))
            {
                device.onNewlyDiscovered(device.layerManager().getDeviceLayer(), null, device.getRssi(), device.getScanRecord(), device.getOrigin());

                if (m_mngr.m_discoveryListener != null)
                {
                    DiscoveryEvent event = new DiscoveryEvent(device, LifeCycle.DISCOVERED);
                    m_mngr.m_discoveryListener.onEvent(event);
                }
            }
        }
    }

    void reconnectDevicesAfterBleTurningBackOn()
    {
        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            final BleDevice device = (BleDevice) m_list.get(i);

            final boolean autoReconnectDeviceWhenBleTurnsBackOn = BleDeviceConfig.bool(device.conf_device().autoReconnectDeviceWhenBleTurnsBackOn, device.conf_mngr().autoReconnectDeviceWhenBleTurnsBackOn);

            if (autoReconnectDeviceWhenBleTurnsBackOn && device.lastDisconnectWasBecauseOfBleTurnOff())
            {
                device.connect();
            }
        }
    }

    void undiscoverAllForTurnOff(final P_DeviceManager cache, final PA_StateTracker.E_Intent intent)
    {
        m_mngr.ASSERT(!m_updating, "Undiscovering devices while updating!");

        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            final BleDevice device_ith = m_list.get(i);

            if (device_ith.is(BleDeviceState.CONNECTED))
            {
                device_ith.m_nativeWrapper.updateNativeConnectionState(device_ith.getNativeGatt());
                device_ith.onNativeDisconnect(false, BleStatuses.GATT_STATUS_NOT_APPLICABLE, false, true);
            }

            final boolean retainDeviceWhenBleTurnsOff = BleDeviceConfig.bool(device_ith.conf_device().retainDeviceWhenBleTurnsOff, device_ith.conf_mngr().retainDeviceWhenBleTurnsOff);

            if (false == retainDeviceWhenBleTurnsOff)
            {
                undiscoverAndRemove(device_ith, m_mngr.m_discoveryListener, cache, intent);
            }
            else
            {
                final boolean undiscoverDeviceWhenBleTurnsOff = BleDeviceConfig.bool(device_ith.conf_device().undiscoverDeviceWhenBleTurnsOff, device_ith.conf_mngr().undiscoverDeviceWhenBleTurnsOff);

                if (true == undiscoverDeviceWhenBleTurnsOff)
                {
                    undiscoverDevice(device_ith, m_mngr.m_discoveryListener, intent);
                }
            }
        }
    }

    private static void undiscoverDevice(BleDevice device, BleManager.DiscoveryListener listener, PA_StateTracker.E_Intent intent)
    {
        if (!device.is(BleDeviceState.DISCOVERED)) return;

        device.onUndiscovered(intent);

        if (listener != null)
        {
            DiscoveryEvent event = new DiscoveryEvent(device, LifeCycle.UNDISCOVERED);
            listener.onEvent(event);
        }
    }

    void undiscoverAndRemove(BleDevice device, BleManager.DiscoveryListener discoveryListener, P_DeviceManager cache, E_Intent intent)
    {
        remove(device, cache);

        undiscoverDevice(device, discoveryListener, intent);
    }

    void purgeStaleDevices(final double scanTime, final P_DeviceManager cache, final BleManager.DiscoveryListener listener)
    {
        if (m_updating)
        {
            m_mngr.ASSERT(false, "Purging devices in middle of updating!");

            return;
        }

        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            BleDevice device = get(i);

            Interval minScanTimeToInvokeUndiscovery = BleDeviceConfig.interval(device.conf_device().minScanTimeNeededForUndiscovery, device.conf_mngr().minScanTimeNeededForUndiscovery);
            if (Interval.isDisabled(minScanTimeToInvokeUndiscovery)) continue;

            Interval scanKeepAlive_interval = BleDeviceConfig.interval(device.conf_device().undiscoveryKeepAlive, device.conf_mngr().undiscoveryKeepAlive);
            if (Interval.isDisabled(scanKeepAlive_interval)) continue;

            if (scanTime < Interval.secs(minScanTimeToInvokeUndiscovery)) continue;

            final boolean purgeable = device.getOrigin() != BleDeviceOrigin.EXPLICIT && ((device.getStateMask() & ~BleDeviceState.PURGEABLE_MASK) == 0x0);

            if (purgeable)
            {
                if (device.getTimeSinceLastDiscovery() > scanKeepAlive_interval.secs())
                {
                    undiscoverAndRemove(device, listener, cache, E_Intent.UNINTENTIONAL);
                }
            }
        }
    }

    boolean hasDevice(BleDeviceState... filter)
    {
        if (filter == null || filter.length == 0)
        {
            return m_list.size() > 0;
        }

        for (int i = m_list.size() - 1; i >= 0; i--)
        {
            BleDevice device = get(i);

            if (device.isAny(filter))
            {
                return true;
            }
        }

        return false;
    }
}
