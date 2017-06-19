package com.idevicesinc.sweetblue;


import android.app.Activity;
import java.util.HashMap;
import java.util.Map;


public final class P_StringHandler
{
    private static final String BASE = "sb_%s";

    public static final String DENYING_LOCATION_ACCESS = String.format(BASE, "denying_location_access");
    public static final String APP_NEEDS_PERMISSION = String.format(BASE, "app_needs_permission");
    public static final String LOCATION_PERMISSION_TOAST = String.format(BASE, "location_permission_toast");
    public static final String REQUIRES_LOCATION_PERMISSION = String.format(BASE, "requires_location_permission");
    public static final String REQUIRES_LOCATION_PERMISSION_AND_SERVICES = String.format(BASE, "requires_location_permission_and_services");
    public static final String LOCATION_SERVICES_NEEDS_ENABLING = String.format(BASE, "location_services_needs_enabling");
    public static final String LOCATION_SERVICES_TOAST = String.format(BASE, "location_services_toast");
    public static final String OK = String.format(BASE, "ok");
    public static final String DENY = String.format(BASE, "deny");
    public static final String ACCEPT = String.format(BASE, "accept");

    private static Map<String, String> fallBackStrings;

    private static void initMap() {
        fallBackStrings = new HashMap<String, String>();
        fallBackStrings.put(DENYING_LOCATION_ACCESS, "Denying location access means low-energy scanning will not work.");
        fallBackStrings.put(APP_NEEDS_PERMISSION, "App needs android.permission.ACCESS_COARSE_LOCATION or android.permission.ACCESS_FINE_LOCATION in its AndroidManifest.xml!");
        fallBackStrings.put(LOCATION_PERMISSION_TOAST, "Please click the Permissions button, then enable Location, then press back twice.");
        fallBackStrings.put(REQUIRES_LOCATION_PERMISSION, "Android Marshmallow (6.0+) requires Location Permission to be able to scan for Bluetooth devices. Please accept to allow Location Permission.");
        fallBackStrings.put(REQUIRES_LOCATION_PERMISSION_AND_SERVICES, "Android Marshmallow (6.0+) requires Location Permission to the app to be able to scan for Bluetooth devices.\n" +
                "\n" + "Marshmallow also requires Location Services to improve Bluetooth device discovery.  While it is not required for use in this app, it is recommended to better discover devices.\n" +
                "\n" + "Please accept to allow Location Permission and Services.");
        fallBackStrings.put(LOCATION_SERVICES_NEEDS_ENABLING, "Android Marshmallow (6.0+) requires Location Services for improved Bluetooth device scanning. While it is not required, it is recommended that Location Services are turned on to improve device discovery.");
        fallBackStrings.put(LOCATION_SERVICES_TOAST, "Please enable Location Services then press back.");
        fallBackStrings.put(OK, "OK");
        fallBackStrings.put(DENY, "Deny");
        fallBackStrings.put(ACCEPT, "Accept");
    }

    private static void checkMap() {
        if (fallBackStrings == null)
        {
            initMap();
        }
    }

    public static String getString(Activity context, String stringKey)
    {
        int res = context.getResources().getIdentifier(stringKey, "string", context.getPackageName());
        if (res == 0) {
            checkMap();
            return fallBackStrings.get(stringKey);
        }
        return context.getString(res);
    }

}
