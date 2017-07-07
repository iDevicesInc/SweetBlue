package com.idevicesinc.sweetblue.toolbox.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WriteValueActivity extends BaseActivity
{
    private BleManager mBleManager;
    private BleDevice mDevice;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mCharacteristic;

    private Spinner mLoadValueSpinner;
    private EditText mValueEditText;
    private Spinner mWriteTypeSpinner;
    private EditText mSaveAsEditText;
    private ImageView mLegalityImageView;
    private TextView mWriteValueTextView;

    private final String SHARED_PREFERENCES_FILE_NAME = "SAVED_VALUES";

    static final Uuids.GATTFormatType sAllowedFormats[] =
        {
            Uuids.GATTFormatType.GCFT_boolean,
            Uuids.GATTFormatType.GCFT_2bit,
            Uuids.GATTFormatType.GCFT_nibble,
            Uuids.GATTFormatType.GCFT_uint8,
            Uuids.GATTFormatType.GCFT_uint12,
            Uuids.GATTFormatType.GCFT_uint16,
            Uuids.GATTFormatType.GCFT_uint24,
            Uuids.GATTFormatType.GCFT_uint32,
            Uuids.GATTFormatType.GCFT_uint48,
            Uuids.GATTFormatType.GCFT_uint64,
            Uuids.GATTFormatType.GCFT_uint128,
            Uuids.GATTFormatType.GCFT_sint8,
            Uuids.GATTFormatType.GCFT_sint12,
            Uuids.GATTFormatType.GCFT_sint16,
            Uuids.GATTFormatType.GCFT_sint24,
            Uuids.GATTFormatType.GCFT_sint32,
            Uuids.GATTFormatType.GCFT_sint48,
            Uuids.GATTFormatType.GCFT_sint64,
            Uuids.GATTFormatType.GCFT_sint128,
            Uuids.GATTFormatType.GCFT_float32,
            Uuids.GATTFormatType.GCFT_float64,
            Uuids.GATTFormatType.GCFT_utf8s,
            Uuids.GATTFormatType.GCFT_utf16s,
            Uuids.GATTFormatType.GCFT_struct
        };

    static class SavedValue implements Comparable<SavedValue>
    {
        String mName;
        String mValueString;
        Uuids.GATTFormatType mGattFormatType;

        SavedValue(String name, String value, Uuids.GATTFormatType gft)
        {
            mName = name;
            mValueString = value;
            mGattFormatType = gft;
        }

        public String getName()
        {
            return mName;
        }

        public String getValueString()
        {
            return mValueString;
        }

        public Uuids.GATTFormatType getGATTFormatType()
        {
            return mGattFormatType;
        }

        public static SavedValue parse(String name, String packed)
        {
            String splits[] = packed.split("\\|", 2);
            String lenString = splits[0];
            String leftover = splits[1];

            Integer len = Integer.parseInt(lenString);

            String value = leftover.substring(0, len);
            String typeString = leftover.substring(len);
            Uuids.GATTFormatType gft = Uuids.GATTFormatType.valueOf(typeString);

            SavedValue sv = new SavedValue(name, value, gft);
            return sv;
        }

        public void writePreference(SharedPreferences.Editor editor)
        {
            StringBuilder sb = new StringBuilder();

            sb.append("" + mValueString.length());
            sb.append("|");
            sb.append(mValueString);
            sb.append(mGattFormatType.name());

            editor.putString(mName, sb.toString());
        }

        @Override
        public int compareTo(@NonNull SavedValue o)
        {
            return mName.compareTo(o.mName);
        }
    }

    private List<SavedValue> mSavedValueList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_value);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        mBleManager = BleManager.get(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final String mac = getIntent().getStringExtra("mac");
        final String serviceUUID = getIntent().getStringExtra("serviceUUID");
        final String characteristicUUID = getIntent().getStringExtra("characteristicUUID");

        mDevice = mBleManager.getDevice(mac);
        mService = mDevice.getNativeService(UUID.fromString(serviceUUID));
        List<BluetoothGattCharacteristic> charList = mService.getCharacteristics();
        if (characteristicUUID != null)
        {
            for (BluetoothGattCharacteristic bgc : charList)
            {
                if (characteristicUUID.equals(bgc.getUuid().toString()))
                {
                    mCharacteristic = bgc;
                    break;
                }
            }
        }

        mLoadValueSpinner = find(R.id.loadValueSpinner);
        mValueEditText = find(R.id.valueEditText);
        mWriteTypeSpinner = find(R.id.writeTypeSpinner);
        mLegalityImageView = find(R.id.legalityImageView);
        mSaveAsEditText = find(R.id.saveNameEditText);

        loadSavedValues();

        // Focus the value edit text
        mValueEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mValueEditText, InputMethodManager.SHOW_FORCED);

        setTitle(UuidUtil.getCharacteristicName(mCharacteristic));

        setLoadValueSpinner();

        setValueEditText();

        setWriteTypeSpinner();

        refreshLegalityIndicator();
    }

    private void loadSavedValues()
    {
        mSavedValueList.clear();

        SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        Set<String> keySet = sp.getAll().keySet();

        for (String key : keySet)
        {
            String s = sp.getString(key, null);

            SavedValue sv = SavedValue.parse(key, s);

            mSavedValueList.add(sv);
        }

        Collections.sort(mSavedValueList);
    }

    private void saveSavedValues()
    {
        SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        for (SavedValue sv : mSavedValueList)
            sv.writePreference(editor);

        editor.commit();
    }

    private void setLoadValueSpinner()
    {
        List<String> l = new ArrayList<>();

        l.add("");  // Add empty row first
        for (SavedValue sv : mSavedValueList)
            l.add(sv.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, l);

        mLoadValueSpinner.setAdapter(adapter);
        mLoadValueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                // Populate from saved value
                if (position > 0)
                {
                    SavedValue sv = mSavedValueList.get(position - 1);

                    mValueEditText.setText(sv.getValueString());

                    for (int i = 0 ; i < sAllowedFormats.length; ++i)
                    {
                        Uuids.GATTFormatType gft = sAllowedFormats[i];
                        if (gft == sv.getGATTFormatType())
                            mWriteTypeSpinner.setSelection(i);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
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

        ArrayAdapter<String> aa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, l);

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
        switch (sAllowedFormats[mWriteTypeSpinner.getSelectedItemPosition()])
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

    private void writeValue()
    {
        String value = mValueEditText.getText().toString();
        Uuids.GATTFormatType gft = sAllowedFormats[mWriteTypeSpinner.getSelectedItemPosition()];

        try
        {
            byte rawVal[] = gft.stringToByteArray(value);

            final Dialog d = ProgressDialog.show(WriteValueActivity.this, getString(R.string.write_value_writing_dialog_title), getString(R.string.write_value_writing_dialog_message));

            mDevice.write(mCharacteristic.getUuid(), rawVal, new BleDevice.ReadWriteListener()
            {
                @Override
                public void onEvent(ReadWriteEvent e)
                {
                    if (e.wasSuccess())
                    {
                        Toast.makeText(getApplicationContext(), R.string.write_value_writing_success_toast, Toast.LENGTH_LONG).show();
                        saveAndFinish();
                    }
                    else
                        Toast.makeText(getApplicationContext(), R.string.write_value_writing_fail_toast, Toast.LENGTH_LONG).show();

                    d.dismiss();
                }
            });
        }
        catch (Uuids.GATTCharacteristicFormatTypeConversionException e)
        {
            //FIXME:  Add toast telling user the write failed
            Toast.makeText(getApplicationContext(), "Invalid input: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.write_value, menu);

        return true;
    }

    @Override
    public boolean onNavigateUp()
    {
        finish();
        return true;
    }

    private void saveAndFinish()
    {
        String saveAsName = mSaveAsEditText.getText().toString();
        if (saveAsName != null && saveAsName.length() > 0)
        {
            String value = mValueEditText.getText().toString();
            Uuids.GATTFormatType gft = sAllowedFormats[mWriteTypeSpinner.getSelectedItemPosition()];

            SavedValue sv = new SavedValue(saveAsName, value, gft);

            mSavedValueList.add(sv);
        }

        saveSavedValues();

        finish();
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.write)
        {
            writeValue();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
