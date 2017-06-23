package com.gattspecparser;


import android.app.Activity;
import android.os.Bundle;
import com.example.gattspecparser.R;

public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        new BuildDescriptorSource();
    }
}
