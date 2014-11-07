Why?
====

Android's BLE stack has some...issues...

* https://code.google.com/p/android/issues/detail?id=58381
* http://androidcommunity.com/nike-blames-ble-for-their-shunning-of-android-20131202/
* http://stackoverflow.com/questions/17870189/android-4-3-bluetooth-low-energy-unstable

SweetBlue is a blanket abstraction that shoves all that bad behavior behind a clean interface and gracefully degrades when the underlying stack becomes too unstable for even it to handle.

It’s built on the hard-earned experience of several commercial BLE projects and provides so many transparent workarounds to issues both annoying and fatal that it’s frankly impossible to imagine writing an app without it. It also supports higher-level constructs, things like pseudo-atomic transactions for coordinating authentication and firmware updates, advanced scan filtering, read polling, transparent retries for connection failures, and the list goes on. The API is dead simple, with usage dependence on plain old Java objects and link dependence on standard Android classes. It offers conveniences for debugging and analytics and error handling that will save you months of work - last mile stuff you probably wouldn’t even have known you had to worry about.

Features
========

*	Full-coverage API documentation.
*	Sample applications.
*	Battle-tested in commercial apps.
*	Plain old Java with zero API-level dependencies.
*	Rich, queryable state tracking that makes UI integration a breeze. 
*	Highly configurable scan filtering.
*	Atomic and non-atomic transactions for easily coordinating authentication handshakes, initialization, and firmware updates.
*	Undiscovery based on last time seen.
*	Verbose logging that outputs human-readable thread IDs, UUIDs, status codes and states instead of alphabet soup.
*	Wrangles a whole nest of thread spaghetti so you don’t have to - make a call on main thread, get a callback on main thread.
*	Internal priority job queue that ensures serialization of all operations so native stack doesn’t get overloaded and important stuff gets done first.
*	Optimal coordination of the BLE stack when connected to multiple devices.
*	Detection and correction of dozens of BLE failure conditions.
*	Numerous manufacturer-specific workarounds and hacks all hidden from you.
*	Built-in polling for read characteristics with optional change-tracking to simulate notifications.
*	Continuous scanning that saves battery and defers to more important operations by stopping and starting as needed under the hood.
*	Transparent retries for transient failure conditions related to connecting, getting services, and scanning.
*	Rich callback mechanisms with clear enumerated reasons when something goes wrong like connection or read/write failures.
*	Distills dozen-line, boilerplate, booby-trapped, native API usage into single method calls.
*	Transparently falls back to Bluetooth Classic for certain BLE failure conditions.
*	On-the-fly-configurable reconnection loops started automatically when random disconnects occur, e.g. from going out of range.
*	One convenient method to completely unwind and reset the Bluetooth Stack.
*	Detection and reporting of BLE failure conditions that user should take action on, such as restarting the BLE stack or even the entire phone.
*	Runtime analytics for tracking average operation times, total elapsed times, and time estimates for long-running operations like firmware updates.


Getting Started
===============

Add these to the root of your AndroidManifiest.xml:
```xml
<uses-sdk android:minSdkVersion="18" android:targetSdkVersion="19" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```
Make an Activity that looks something like this:
```java
public class MyActivity extends Activity
{
  private BleManager m_bleManager;
	
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		m_bleManager = new BleManager(this);
		
		m_bleManager.startScan(new BleManager.I_DiscoveryListener()
		{
			@Override public void onDeviceDiscovered(BleDevice device)
			{
				m_bleManager.stopScan();
				
				device.connect(new BleDevice.I_StateListener()
				{
					@Override public void onStateChange(BleDevice device, int oldStateMask, int newStateMask)
					{
						if( E_BleDeviceState.INITIALIZED.wasEntered(oldStateMask, newStateMask) )
						{
							String toastText = device.getDebugName() + " just initialized!";
							Toast.makeText(MyActivity.this, toastText, Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		});
	}
	
	@Override protected void onResume()
	{
		super.onResume();
		
		m_bleManager.onResume();
	}
	
	@Override protected void onPause()
	{
		super.onPause();
		
		m_bleManager.onPause();
	}

	@Override protected void onDestroy()
	{
		super.onDestroy();
		
		m_bleManager.onDestroy();
	}
}
```

