package com.idevicesinc.sweetblue.toolbox.util;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class ViewUtil
{
    private static int getTextHeight(CharSequence source, TextPaint paint, int width, float textSize)
    {
        // Use the paint to calculate how big the text will be at a given size
        float oldSize = paint.getTextSize();
        paint.setTextSize(textSize);
        StaticLayout layout = new StaticLayout(source, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        paint.setTextSize(oldSize);
        return layout.getHeight();
    }

    public static void fixOversizedText(final TextView tv)
    {
        tv.setMaxLines(1);
        tv.addOnLayoutChangeListener(new View.OnLayoutChangeListener()
        {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom)
            {
                if (tv.getLineCount() > 1)
                {
                    int widthLimit = right - left - tv.getCompoundPaddingLeft() - tv.getCompoundPaddingRight();
                    int heightLimit = bottom - top - tv.getCompoundPaddingBottom() - tv.getCompoundPaddingTop();

                    // Grab text and paint from the text view
                    String text = tv.getText().toString();
                    TextPaint tp = tv.getPaint();

                    // Get current text size and height of text
                    float textSize = tv.getTextSize();
                    int textHeight = getTextHeight(text, tp, widthLimit, textSize);
                    float minPointSize = 2.0f;  // Don't allow the text to get so small it goes negative or we loop forever

                    // Until we either fit within our text view or we had reached our min text size, incrementally try smaller sizes
                    while(textHeight > heightLimit && textSize > minPointSize)
                    {
                        textSize = Math.max(textSize - 1.0f, minPointSize);
                        textHeight = getTextHeight(text, tp, widthLimit, textSize);
                    }
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                }
                tv.removeOnLayoutChangeListener(this);
            }
        });

        /*tv.setVisibility(View.INVISIBLE);
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
                    fixOversizedText(tv);
                    tv.setVisibility(View.INVISIBLE);
                }
                else
                    tv.setVisibility(View.VISIBLE);
            }
        });*/
    }
}
