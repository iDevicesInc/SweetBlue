package com.idevicesinc.sweetblue;


public abstract class BleTransaction
{


    public abstract void start();


    public abstract class Auth extends BleTransaction {}

    public abstract class Init extends BleTransaction {}

    public abstract class Ota extends BleTransaction {}

}
