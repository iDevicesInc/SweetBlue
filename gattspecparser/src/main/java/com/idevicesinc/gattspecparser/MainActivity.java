package com.idevicesinc.gattspecparser;


import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        new BuildDescriptorSource();  // Makes updated enum with GATT Descriptor data from https://www.bluetooth.com/specifications/gatt/descriptors
        new BuildCharactersticsSource();  // Makes updated enum with GATT Charactaristic data from https://www.bluetooth.com/specifications/gatt/characteristics
    }
}
