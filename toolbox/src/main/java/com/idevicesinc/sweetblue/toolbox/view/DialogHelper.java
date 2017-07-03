package com.idevicesinc.sweetblue.toolbox.view;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;


public class DialogHelper
{

    private DialogHelper() {}


    public static AlertDialog showRadioGroupDialog(Context context, String title, String message, final String[] choices, int selectedIndex, final RadioGroupListener listener)
    {
        AlertDialog.Builder build = new AlertDialog.Builder(context);
        if (!TextUtils.isEmpty(title))
        {
            build.setTitle(title);
        }
        build.setSingleChoiceItems(choices, selectedIndex, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == -1)
                {
                    if (listener != null)
                    {
                        listener.onChoiceSelected("");
                    }
                    return;
                }
                if (listener != null)
                {
                    listener.onChoiceSelected(choices[which]);
                }
                dialog.dismiss();
            }
        });
        build.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                if (listener != null)
                {
                    listener.onCanceled();
                }
            }
        });
        if (!TextUtils.isEmpty(message))
        {
            build.setMessage(message);
        }
        return build.show();
    }


    public interface RadioGroupListener
    {
        void onChoiceSelected(String choice);
        void onCanceled();
    }


}
