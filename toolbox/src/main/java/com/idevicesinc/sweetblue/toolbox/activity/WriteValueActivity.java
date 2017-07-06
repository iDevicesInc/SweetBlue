package com.idevicesinc.sweetblue.toolbox.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.toolbox.R;
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

        //FIXME:  Real title
        setTitle(getString(R.string.device_information_title));

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
                if (bgc.getUuid().equals(characteristicUUID))
                {
                    mCharacteristic = bgc;
                    break;
                }
            }
        }

        mValueEditText = find(R.id.valueEditText);
        mWriteTypeSpinner = find(R.id.writeTypeSpinner);
        mLegalityImageView = find(R.id.legalityImageView);
        mWriteValueTextView = find(R.id.writeValueTextView);

        // Focus the value edit text
        mValueEditText.requestFocus();

        setValueEditText();

        setWriteTypeSpinner();

        setWriteValueTextView();

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

        /*mValueEditText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView tv, int actionId, KeyEvent event)
            {
                if (!(actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
                    return false;

                String value = tv.getText().toString();
                Uuids.GATTFormatType gft = Uuids.GATTFormatType.values()[mWriteTypeSpinner.getSelectedItemPosition()];

                try
                {
                    byte rawVal[] = gft.stringToByteArray(value);

                    final Dialog d = ProgressDialog.show(getApplicationContext(), "title", "message");

                    mDevice.write(mCharacteristic.getUuid(), rawVal, new BleDevice.ReadWriteListener()
                    {
                        @Override
                        public void onEvent(ReadWriteEvent e)
                        {
                            if (e.wasSuccess())
                                Toast.makeText(getApplicationContext(), "Value written!", Toast.LENGTH_LONG).show();
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

                return true; // consume.
            }
        });*/
    }


    private void setWriteTypeSpinner()
    {
        mWriteTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                refreshLegalityIndicator();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                refreshLegalityIndicator();
            }
        });
    }

    private void setWriteValueTextView()
    {
        mWriteValueTextView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                writeValue();
            }
        });
    }

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
                        Toast.makeText(getApplicationContext(), "Value written!", Toast.LENGTH_LONG).show();
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
        return true;
    }

    @Override
    public boolean onNavigateUp()
    {
        finish();
        return true;
    }

}
