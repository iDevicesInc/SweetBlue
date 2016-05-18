package com.idevicesinc.sweetblue;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class P_PersistanceManager
{

    private final static String CONNECTED_DEVICES_NAMESPACE = "sb_connected_2dj-O";


    private final BleManager mManager;


    public P_PersistanceManager(BleManager mgr)
    {
        mManager = mgr;
    }

    public void storePreviouslyConnectedDevices(Set<String> macAddresses)
    {
        SharedPreferences.Editor prefs = getEdit(CONNECTED_DEVICES_NAMESPACE);
        JSONArray jsonArray = new JSONArray(macAddresses);
        prefs.putString("devices", jsonArray.toString());
        prefs.commit();
    }

    public Set<String> getPreviouslyConnectedDevices()
    {
        HashSet<String> devices = new HashSet<>(0);
        SharedPreferences prefs = getPrefs(CONNECTED_DEVICES_NAMESPACE);
        String stringList = prefs.getString("devices", "");
        if (!TextUtils.isEmpty(stringList))
        {
            try
            {
                JSONArray array = new JSONArray(stringList);
                int size = array.length();
                for (int i = 0; i < size; i++)
                {
                    devices.add(array.getString(i));
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        return devices;
    }

    private SharedPreferences getPrefs(String namespace)
    {
        return mManager.getAppContext().getSharedPreferences(namespace, Context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor getEdit(String namespace)
    {
        return mManager.getAppContext().getSharedPreferences(namespace, Context.MODE_PRIVATE).edit();
    }

}
