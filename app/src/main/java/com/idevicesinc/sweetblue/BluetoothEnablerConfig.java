package com.idevicesinc.sweetblue;

import android.app.AlertDialog;
import android.content.Context;

public class BluetoothEnablerConfig
{
    public static boolean shouldShowBluetoothDialog = false;

    public static boolean shouldShowLocationPermissionDialog = true;

    public static boolean shouldShowLocationServicesDialog = true;

    public static boolean shouldShowLocationPermissionToast = true;

    public static boolean shouldShowLocationServicesToast = true;

    /**
     * Determines whether or not to show a combined dialog for both Location permissions and Location services if
     * both are off/disabled.
     */
    public static boolean shouldShowCombinedLocationDialog = true;

    public static boolean shouldShowLocationDeniedDialog = true;

    /**
     * If {@link #shouldShowBluetoothDialog} is true and {@link #bluetoothCustomDialogBuilder} is {@code null}, this message will be displayed in
     * a dialog before the system prompt requesting bluetooth is shown.
     */
    public static String bluetoothDialogPromptMessage = null;

    /**
     * If {@link #shouldShowBluetoothDialog} is true and this isn't null, then a dialog will be built and shown from the passed
     * {@link android.app.AlertDialog.Builder}. NOTE: using this will override any PositiveButton and NegativeButton click listener.
     */
    public static AlertDialog.Builder bluetoothCustomDialogBuilder = null;

    /**
     * If {@link #shouldShowLocationPermissionToast} is true, this string will be displayed in a toast if the user
     * has selected "Never Ask Again" in the Location permission system dialog. If left {@code null} it will
     * default to the localized string {@link com.idevicesinc.sweetblue.R.string#sb_location_permission_toast}.
     */
    public static String locationPermissionToastMessage = null;

    /**
     * If {@link #shouldShowLocationPermissionDialog} is true and {@link #locationPermissionDialogBuilder} is {@code null}, this string will be displayed in a dialog before the
     * system dialog for Location permission. If left {@code null} it will default to the localized string {@link com.idevicesinc.sweetblue.R.string#sb_requires_location_permission}.
     */
    public static String locationPermissionPromptMessage = null;

    /**
     * If {@link #shouldShowLocationPermissionDialog} is true and this isn't null, then a dialog will be built and shown form the passed {@link android.app.AlertDialog.Builder}.
     * NOTE: using this will override any PositveButton and NegativeButton click listeners.
     */
    public static AlertDialog.Builder locationPermissionDialogBuilder = null;

    /**
     * If {@link #shouldShowLocationServicesToast} is true, then the {@link #locationServicesToastMessage} message will be displayed in a toast when the user moves into the
     * phone settings to enable Location services.
     */
    public static String locationServicesToastMessage = null;

    /**
     * If {@link #shouldShowLocationServicesDialog} is true and {@link #locationServicesDialogBuilder} is {@code null}, then the {@link #locationServicesPromptMessage} message will be
     * displayed in a dialog before the user is redirected to the phone settings to enable Location service, assuming they didn't deny the dialog.
     */
    public static String locationServicesPromptMessage = null;

    /**
     * If {@link #shouldShowLocationServicesDialog} is true and this isn't {@code null}, then a dialog will be built from the provided {@link android.app.AlertDialog.Builder} and shown to
     * the user. NOTE: if this is used, the PositiveButton and NegativeButton click listeners will be overridden.
     */
    public static AlertDialog.Builder locationServicesDialogBuilder = null;

    /**
     * If {@link #shouldShowCombinedLocationDialog} is true and {@link #locationPermissionAndServicesDialogBuilder} is {@code null}, then the {@link #locationPermissionAndServicesPromptMessage}
     * will be displayed in a dialog before the user is redirected to *BOTH* the Location permissions system dialog and phone settings for Location services, assuming that you are using the
     * {@link DefaultBluetoothEnablerController}.
     */
    public static String locationPermissionAndServicesPromptMessage = null;

    /**
     * If {@link #shouldShowCombinedLocationDialog} is true and this isn't {@code null}, then a dialog will be built from the builder and shown to the user.
     * NOTE: the PositiveButton and NegtiveButton click listeners will be overridden.
     */
    public static AlertDialog.Builder locationPermissionAndServicesDialogBuilder = null;

    /**
     * If {@link #shouldShowLocationDeniedDialog} is true and {@link #locationPermissionDeniedDialogBuilder} is null, then a dialog will be shown with the {@link #locationPermissionDeniedMessage}.
     * This will be shown after location permissions are denied.
     */
    public static String locationPermissionDeniedMessage = null;

    /**
     * If {@link #shouldShowLocationDeniedDialog} is true and this isn't {@code null}, then a dialog built from this builder will be shown to the user.
     */
    public static AlertDialog.Builder locationPermissionDeniedDialogBuilder = null;

    /**
     * The string you want to label the positive button on the dialog, assuming you aren't using your own custom dialog.
     */
    public static String dialogAcceptString = null;

    /**
     * The string you want to label the negative button on the dialog, assuming you aren't using your own custom dialog.
     */
    public static String dialogDenyString = null;

    /**
     * The string you want to label the dialog box's neutral button, assuming you aren't using your own custom dialog.
     */
    public static String dialogOkayString = null;


    void initStrings(Context context)
    {
        if(context != null)
        {
            bluetoothDialogPromptMessage = bluetoothDialogPromptMessage != null ? bluetoothDialogPromptMessage : ""; //TODO: figure out what this should be

            locationPermissionToastMessage = locationPermissionToastMessage != null ? locationPermissionToastMessage : context.getString(R.string.sb_location_permission_toast);

            locationPermissionPromptMessage = locationPermissionPromptMessage != null ? locationPermissionPromptMessage : context.getString(R.string.sb_requires_location_permission);

            locationServicesToastMessage = locationServicesToastMessage != null ? locationServicesToastMessage : context.getString(R.string.sb_location_services_toast);

            locationServicesPromptMessage = locationServicesPromptMessage != null ? locationServicesPromptMessage : context.getString(R.string.sb_location_services_needs_enabling);

            locationPermissionAndServicesPromptMessage = locationPermissionAndServicesPromptMessage != null ? locationPermissionAndServicesPromptMessage : context.getString(R.string.sb_requires_location_permission_and_services);

            locationPermissionDeniedMessage = locationPermissionDeniedMessage != null ? locationPermissionDeniedMessage : context.getString(R.string.sb_denying_location_access);

            dialogAcceptString = dialogAcceptString != null ? dialogAcceptString : context.getString(R.string.sb_accept);

            dialogDenyString = dialogDenyString != null ? dialogDenyString : context.getString(R.string.sb_deny);

            dialogOkayString = dialogOkayString != null ? dialogOkayString : context.getString(R.string.sb_ok);
        }
        else
        {
            bluetoothDialogPromptMessage = bluetoothDialogPromptMessage != null ? bluetoothDialogPromptMessage : ""; //TODO: figure out what this should be

            locationPermissionToastMessage = locationPermissionToastMessage != null ? locationPermissionToastMessage : P_StringHandler.getString(P_StringHandler.LOCATION_PERMISSION_TOAST);

            locationPermissionPromptMessage = locationPermissionPromptMessage != null ? locationPermissionPromptMessage : P_StringHandler.getString(P_StringHandler.REQUIRES_LOCATION_PERMISSION);

            locationServicesToastMessage = locationServicesToastMessage != null ? locationServicesToastMessage : P_StringHandler.getString(P_StringHandler.LOCATION_SERVICES_TOAST);

            locationServicesPromptMessage = locationServicesPromptMessage != null ? locationServicesPromptMessage : P_StringHandler.getString(P_StringHandler.LOCATION_SERVICES_NEEDS_ENABLING);

            locationPermissionAndServicesPromptMessage = locationPermissionAndServicesPromptMessage != null ? locationPermissionAndServicesPromptMessage : P_StringHandler.getString(P_StringHandler.REQUIRES_LOCATION_PERMISSION_AND_SERVICES);

            locationPermissionDeniedMessage = locationPermissionDeniedMessage != null ? locationPermissionDeniedMessage : P_StringHandler.getString(P_StringHandler.DENYING_LOCATION_ACCESS);

            dialogAcceptString = dialogAcceptString != null ? dialogAcceptString : P_StringHandler.getString(P_StringHandler.ACCEPT);

            dialogDenyString = dialogDenyString != null ? dialogDenyString : P_StringHandler.getString(P_StringHandler.DENY);

            dialogOkayString = dialogOkayString != null ? dialogOkayString : P_StringHandler.getString(P_StringHandler.OK);
        }
    }

}
