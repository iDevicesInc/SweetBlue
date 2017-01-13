package com.idevicesinc.sweetblue;


import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

public class UIUtil
{

    private UIUtil() {}


    public static final String TEXT_ALLOW = "Allow";
    public static final String TEXT_DENY = "Deny";



    public static boolean turnOnPermissionDialogShowing(UiDevice device)
    {
        UiObject allowButton = device.findObject(new UiSelector().text(TEXT_ALLOW));
        return allowButton.exists();
    }

    public static void allowPermission(UiDevice device) throws UiObjectNotFoundException
    {
        UiObject allow = device.findObject(new UiSelector().text(TEXT_ALLOW));
        allow.click();
    }

    public static boolean viewExistsExact(UiDevice device, String messageText) throws UiObjectNotFoundException
    {
        UiObject view = device.findObject(new UiSelector().text(messageText));
        return view.exists();
    }

    public static boolean viewExistsContains(UiDevice device, String textToMatch) throws UiObjectNotFoundException
    {
        UiObject view = device.findObject(new UiSelector().textContains(textToMatch));
        return view.exists();
    }

    public static boolean viewExistsContains(UiDevice device, String... textToMatch) throws UiObjectNotFoundException
    {
        if (textToMatch == null || textToMatch.length < 1)
        {
            return false;
        }
        UiObject view;
        boolean contains = false;
        for (int i = 0; i < textToMatch.length; i++)
        {
            view = device.findObject(new UiSelector().textContains(textToMatch[i]));
            contains = view.exists();
            if (contains) return true;
        }
        return false;
    }

    public static void acceptClickPermission(UiDevice device, String acceptText) throws UiObjectNotFoundException
    {
        UiObject accept = device.findObject(new UiSelector().text(acceptText));
        if (accept.exists())
        {
            accept.click();
        }
        accept = device.findObject(new UiSelector().text(acceptText.toUpperCase()));
        if (accept.exists())
        {
            accept.click();
        }
    }

    public static void denyPermission(UiDevice device) throws UiObjectNotFoundException
    {
        UiObject deny = device.findObject(new UiSelector().text(TEXT_DENY));
        deny.click();
    }

}
