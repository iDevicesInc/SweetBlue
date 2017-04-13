package com.idevicesinc.sweetblue.rx;


import android.content.Context;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.ScanOptions;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;


public class RxBleHelper
{

    public static Observable<BleDevice> scan(final Context context, final ScanOptions options) {
        final BleManager mgr = BleManager.get(context);
        return Observable.create(new ObservableOnSubscribe<BleDevice>()
        {
            @Override public void subscribe(@NonNull final ObservableEmitter<BleDevice> emitter) throws Exception
            {
                mgr.setListener_Discovery(new DiscoveryListener()
                {
                    @Override public void onEvent(DiscoveryEvent e)
                    {
                        if (e.was(LifeCycle.DISCOVERED))
                        {
                            if (!emitter.isDisposed())
                            {
                                emitter.onNext(e.device());
                            }
                        }
                    }
                });
                mgr.startScan(options);

            }
        }).doOnDispose(new Action()
        {
            @Override public void run() throws Exception
            {
                mgr.stopPeriodicScan();
                mgr.stopScan();
            }
        });
    }

}
