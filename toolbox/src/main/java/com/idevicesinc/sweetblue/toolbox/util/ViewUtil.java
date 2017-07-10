package com.idevicesinc.sweetblue.toolbox.util;

import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class ViewUtil
{
    public static void postFixRunnable(final TextView tv)
    {
        tv.setVisibility(View.INVISIBLE);
        tv.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (tv.getLineCount() > 1)
                {
                    // Shrink the size so the text all fits
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.getTextSize() - 1);
                    tv.requestLayout();
                    postFixRunnable(tv);
                    tv.setVisibility(View.INVISIBLE);
                }
                else
                    tv.setVisibility(View.VISIBLE);
            }
        });
    }
}
