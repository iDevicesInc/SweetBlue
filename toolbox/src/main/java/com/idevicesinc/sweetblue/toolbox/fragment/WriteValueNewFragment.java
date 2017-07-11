package com.idevicesinc.sweetblue.toolbox.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.activity.WriteValueActivity;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.List;

import static com.idevicesinc.sweetblue.toolbox.activity.WriteValueActivity.sAllowedFormats;

public class WriteValueNewFragment extends Fragment/* implements BleServicesActivity.Listener*/
{
    private EditText mValueEditText;
    private Spinner mWriteTypeSpinner;
    private EditText mSaveAsEditText;
    private ImageView mLegalityImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.layout_write_value_new, null);

        mValueEditText = (EditText) layout.findViewById(R.id.valueEditText);
        mWriteTypeSpinner = (Spinner) layout.findViewById(R.id.writeTypeSpinner);
        mLegalityImageView = (ImageView) layout.findViewById(R.id.legalityImageView);
        mSaveAsEditText = (EditText) layout.findViewById(R.id.saveNameEditText);

        setValueEditText();

        setWriteTypeSpinner();

        refreshLegalityIndicator();

        mValueEditText.requestFocus();

        /*new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                mValueEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mValueEditText, InputMethodManager.SHOW_FORCED);
            }
        }, 25);*/

        return layout;
    }

    public void onWriteClicked()
    {
        // Do write here
    }

    private void setValueEditText()
    {
        mValueEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                // Refresh legality
                refreshLegalityIndicator();
            }
        });
    }

    private void setWriteTypeSpinner()
    {
        List<String> l = new ArrayList<>();

        String names[] = getResources().getStringArray(R.array.gatt_format_type_names);

        for (Uuids.GATTFormatType gft : sAllowedFormats)
            l.add(names[gft.ordinal()]);

        ArrayAdapter<String> aa = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, l);

        mWriteTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                refreshLegalityIndicator();
                refreshTextInputType();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                refreshLegalityIndicator();
                refreshTextInputType();
            }
        });

        mWriteTypeSpinner.setAdapter(aa);
    }

    private void refreshLegalityIndicator()
    {
        Uuids.GATTFormatType gft = sAllowedFormats[mWriteTypeSpinner.getSelectedItemPosition()];
        String currentInput = mValueEditText.getText().toString();
        try
        {
            if (currentInput != null && currentInput.length() > 0)
                gft.stringToByteArray(currentInput);
            mLegalityImageView.setVisibility(View.GONE);
        }
        catch (Exception e)
        {
            mLegalityImageView.setVisibility(View.VISIBLE);
        }
    }

    private void refreshTextInputType()
    {
        int type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
        switch (WriteValueActivity.sAllowedFormats[mWriteTypeSpinner.getSelectedItemPosition()])
        {
            case GCFT_boolean:
                type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
                break;
            case GCFT_2bit:
            case GCFT_nibble:
            case GCFT_uint8:
            case GCFT_uint12:
            case GCFT_uint16:
            case GCFT_uint24:
            case GCFT_uint32:
            case GCFT_uint48:
            case GCFT_uint64:
            case GCFT_uint128:
                type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL;
                break;
            case GCFT_sint8:
            case GCFT_sint12:
            case GCFT_sint16:
            case GCFT_sint24:
            case GCFT_sint32:
            case GCFT_sint48:
            case GCFT_sint64:
            case GCFT_sint128:
                type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
                break;
            case GCFT_float32:
            case GCFT_float64:
            case GCFT_SFLOAT:
            case GCFT_FLOAT:
                type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
                break;
            case GCFT_duint16:
            case GCFT_utf8s:
            case GCFT_utf16s:
            case GCFT_struct:
                type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
                break;
        }

        mValueEditText.setInputType(type);
    }

    public String getValueString()
    {
        return mValueEditText.getText().toString();
    }

    public Uuids.GATTFormatType getValueGATTFormatType()
    {
        return WriteValueActivity.sAllowedFormats[mWriteTypeSpinner.getSelectedItemPosition()];
    }

    public String getSaveAsName()
    {
        String s = mSaveAsEditText.getText().toString();
        if (s == null || s.length() < 1)
            return null;
        return s;
    }

    public void setFromSavedValue(WriteValueActivity.SavedValue sv)
    {
        mValueEditText.setText(sv.getValueString());
        mSaveAsEditText.setText(sv.getName());
        for (int i = 0; i < sAllowedFormats.length; ++i)
        {
            if (sAllowedFormats[i] == sv.getGATTFormatType())
                mWriteTypeSpinner.setSelection(i);
        }
    }
}