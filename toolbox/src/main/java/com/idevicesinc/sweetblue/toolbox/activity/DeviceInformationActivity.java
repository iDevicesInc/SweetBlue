package com.idevicesinc.sweetblue.toolbox.activity;


import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.toolbox.R;
import com.jaredrummler.android.device.DeviceName;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class DeviceInformationActivity extends BaseActivity
{

    private TextView kernelVersion;
    private TextView deviceName;
    private TextView bluetoothName;
    private TextView androidVer;
    private TextView apiVersion;
    private TextView brand;
    private TextView manufacturer;
    private TextView model;
    private TextView product;
    private TextView board;
    private ImageView bleSupported;
    private ImageView lollipopScanSupported;
    private ImageView scanBatchSupported;
    private ImageView multiAdvSupported;

    private BleManager bleManager;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        bleManager = BleManager.get(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.device_information_title));

        kernelVersion = find(R.id.kernelVersion);
        kernelVersion.setText(getKernelVersion());

        deviceName = find(R.id.deviceName);
        deviceName.setText(DeviceName.getDeviceName(Build.MODEL, getString(R.string.unknown_device)));

        bluetoothName = find(R.id.bluetoothName);
        bluetoothName.setText(bleManager.getNativeAdapter().getName());

        androidVer = find(R.id.osVersion);
        androidVer.setText(Build.VERSION.RELEASE);

        apiVersion = find(R.id.apiVersion);
        apiVersion.setText(String.valueOf(Build.VERSION.SDK_INT));

        brand = find(R.id.brand);
        brand.setText(Build.BRAND);

        manufacturer = find(R.id.manufacturer);
        manufacturer.setText(Build.MANUFACTURER);

        model = find(R.id.model);
        model.setText(Build.MODEL);

        product = find(R.id.product);
        product.setText(Build.PRODUCT);

        board = find(R.id.board);
        board.setText(Build.BOARD);

        int supported = R.drawable.icon_check;
        int notsupported = R.drawable.icon_x;

        bleSupported = find(R.id.bleSupported);
        bleSupported.setImageResource(bleManager.isBleSupported() ? supported : notsupported);

        lollipopScanSupported = find(R.id.lollipopScanSupported);
        lollipopScanSupported.setImageResource(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? supported : notsupported);

        scanBatchSupported = find(R.id.scanBatchingSupported);
        boolean scanSupported = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            scanSupported = bleManager.getNativeAdapter().isOffloadedScanBatchingSupported();
        }
        scanBatchSupported.setImageResource(scanSupported ? supported : notsupported);

        multiAdvSupported = find(R.id.multipleAdvertisementSupported);

        boolean multiSupported = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            multiSupported = bleManager.getNativeAdapter().isMultipleAdvertisementSupported();
        }
        multiAdvSupported.setImageResource(multiSupported ? supported : notsupported);
    }

    String getKernelVersion()
    {
        try
        {
            Process p = Runtime.getRuntime().exec("uname -r");
            InputStream is = null;
            if (p.waitFor() == 0)
            {
                is = p.getInputStream();
            }
            else
            {
                is = p.getErrorStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            reader.close();
            return line;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return this.getString(R.string.unknown);
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
