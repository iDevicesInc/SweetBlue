package com.idevicesinc.sweetblue.toolbox.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;
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

import java.util.List;
import java.util.UUID;

public class WriteValueActivity extends BaseActivity
{
    private BleManager mBleManager;
    private BleDevice mDevice;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mCharacteristic;

    private EditText mValueEditText;
    private Spinner mWriteTypeSpinner;
    private ImageView mLegalityImageView;
    private TextView mWriteValueTextView;

    class SavedValue
    {
        String mValueString;
        Uuids.GATTFormatType mGattFormatType;

        SavedValue(String value, Uuids.GATTFormatType gft)
        {
            mValueString = value;
            mGattFormatType = gft;
        }
    }

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

        mValueEditText = find(R.id.valueEditText);
        mWriteTypeSpinner = find(R.id.writeTypeSpinner);
        mLegalityImageView = find(R.id.legalityImageView);
        //mWriteValueTextView = find(R.id.writeValueTextView);

        // Focus the value edit text
        mValueEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mValueEditText, InputMethodManager.SHOW_FORCED);

        setTitle(UuidUtil.getCharacteristicName(mCharacteristic));

        setValueEditText();

        setWriteTypeSpinner();

        refreshLegalityIndicator();
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
    }

    /*private void setWriteValueTextView()
    {
        mWriteValueTextView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                writeValue();
            }
        });
    }*/

    private void refreshLegalityIndicator()
    {
        Uuids.GATTFormatType gft = Uuids.GATTFormatType.values()[mWriteTypeSpinner.getSelectedItemPosition() + 1];
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
        switch (Uuids.GATTFormatType.values()[mWriteTypeSpinner.getSelectedItemPosition() + 1])
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
        Uuids.GATTFormatType gft = Uuids.GATTFormatType.values()[mWriteTypeSpinner.getSelectedItemPosition()+1];

        try
        {
            byte rawVal[] = gft.stringToByteArray(value);

            final Dialog d = ProgressDialog.show(WriteValueActivity.this, "title", "message");

            mDevice.write(mCharacteristic.getUuid(), rawVal, new BleDevice.ReadWriteListener()
            {
                @Override
                public void onEvent(ReadWriteEvent e)
                {
                    if (e.wasSuccess())
                    {
                        Toast.makeText(getApplicationContext(), "Value written!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                    else
                        Toast.makeText(getApplicationContext(), "Write failed", Toast.LENGTH_LONG).show();

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
