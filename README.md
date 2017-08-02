<b>|</b>&nbsp;<a href='#why'>Why?</a>
<b>|</b>&nbsp;<a href='#features'>Features</a>
<b>|</b>&nbsp;<a href='#getting-started'>Getting Started</a>
<b>|</b>&nbsp;<a href='#licensing'>Licensing</a>
<b>|</b>&nbsp;<a href="https://github.com/iDevicesInc/SweetBlue/wiki">Wiki</a>
<b>|</b>&nbsp;<a href="https://play.google.com/store/apps/details?id=com.idevicesinc.sweetblue.toolbox">Toolbox</a>
<a href="https://travis-ci.org/iDevicesInc/SweetBlue">
  <img align="right" src="https://img.shields.io/badge/version-2.52.10-blue.svg" />
  <img align="right" src="https://travis-ci.org/iDevicesInc/SweetBlue.svg?branch=master"/>
</a>
<p align="center">
  <br>
  <a href="https://idevicesinc.com/sweetblue">
    <img src="https://github.com/iDevicesInc/SweetBlue/blob/master/scripts/assets/sweetblue_logo.png" />
  </a>
</p>
Why?
====

Android's BLE stack has some...issues...

* https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues
* https://code.google.com/p/android/issues/detail?id=58381
* http://androidcommunity.com/nike-blames-ble-for-their-shunning-of-android-20131202/
* http://stackoverflow.com/questions/17870189/android-4-3-bluetooth-low-energy-unstable

SweetBlue is a blanket abstraction that shoves all that troublesome behavior behind a clean interface and gracefully degrades when the underlying stack becomes too unstable for even it to handle.

It’s built on the hard-earned experience of several commercial BLE projects and provides so many transparent workarounds to issues both annoying and fatal that it’s frankly impossible to imagine writing an app without it. It also supports many higher-level constructs, things like atomic transactions for coordinating authentication handshakes and firmware updates, flexible scanning configurations, read polling, transparent retries for transient failure conditions, and, well, the list goes on. The API is dead simple, with usage dependence on a few plain old Java objects and link dependence on standard Android classes. It offers conveniences for debugging and analytics and error handling that will save you months of work - last mile stuff you didn't even know you had to worry about.

Features
========

*	Full-coverage API documentation: http://idevicesinc.com/sweetblue/docs/api
*	Sample applications.
*	Battle-tested in commercial apps.
*	Plain old Java with zero API-level dependencies.
*	Rich, queryable state tracking that makes UI integration a breeze.
*	Automatic service discovery.
*	Full support for server role including advertising.
*	Easy RSSI tracking with built-in polling and caching, including distance and friendly signal strength calculations.
*	Highly configurable scanning with min/max time limits, periodic bursts, advanced filtering, and more.
*	Continuous scanning mode that saves battery and defers to more important operations by stopping and starting as needed under the hood.
*	Atomic transactions for easily coordinating authentication handshakes, initialization, and firmware updates.
* 	Automatic striping of characteristic writes greater than [MTU](http://en.wikipedia.org/wiki/Maximum_transmission_unit) size of 20 bytes.
*	Undiscovery based on last time seen.
*	Clean leakage of underlying native stack objects in case of emergency.
*	Wraps Android API level checks that gate certain methods.
*	Verbose [logging](https://github.com/iDevicesInc/SweetBlue/wiki/Logging) that outputs human-readable thread IDs, UUIDs, status codes and states instead of alphabet soup.
*	Wrangles a big bowl of thread spaghetti behind a nice asynchronous API - make a call on main thread, get a callback on main thread a short time later.
*	Internal priority job queue that ensures serialization of all operations so native stack doesn’t get overloaded and important stuff gets done first.
*	Optimal coordination of the BLE stack when connected to multiple devices.
*	Detection and correction of dozens of BLE failure conditions.
*	Numerous manufacturer-specific workarounds and hacks all hidden from you.
*	Built-in polling for read characteristics with optional change-tracking to simulate notifications.
*	Transparent retries for transient failure conditions related to connecting, getting services, and scanning.
*	Comprehensive callback system with clear enumerated reasons when something goes wrong like connection or read/write failures.
*	Distills dozens of lines of boilerplate, booby-trapped, native API usages into single method calls.
*	Transparently falls back to Bluetooth Classic for certain BLE failure conditions.
*	On-the-fly-configurable reconnection loops started automatically when random disconnects occur, e.g. from going out of range.
*	Retention and automatic reconnection of devices after BLE off->on cycle or even complete app reboot.
*	One convenient method to completely unwind and reset the Bluetooth stack.
*	Detection and reporting of BLE failure conditions that user should take action on, such as restarting the Bluetooth stack or even the entire phone.
*	Runtime analytics for tracking average operation times, total elapsed times, and time estimates for long-running operations like firmware updates.


Getting Started
===============
1. If using **Android Studio** or **Gradle**...
  1. [Download](http://idevicesinc.com/sweetblue/#tryit) the latest release to a subfolder of your project such as `MyApp/src/main/lib/`. This ZIP contains several samples, precompiled JARS, and API docs and is preferable to downloading from GitHub, which only contains the raw source.
  2. Open the app module's `build.gradle` file.
  3. If building with source, your gradle file should look something like this:

    ```
    
    android {
        compileSdkVersion 25
        buildToolsVersion '25.0.3'
        
        defaultConfig {
            minSdkVersion 18
            targetSdkVersion 25
            ...
        }
    
        sourceSets {
            main.java.srcDirs += 'src/main/lib/sweetblue/src'
            main.res.srcDirs += 'src/main/lib/sweetblue/res'
            ...
        }
        ...
    }
    
    ```
    
  4. If you're building with source from github, the sourceSet path is a bit different:
  
    ```
      
      android {
          compileSdkVersion 25
          buildToolsVersion '25.0.3'
          
          defaultConfig {
              minSdkVersion 18
              targetSdkVersion 25
              ...
          }
      
          sourceSets {
              main.java.srcDirs += 'src/main/lib/sweetblue/library/src/main/java'              
              main.res.srcDirs += 'src/main/lib/sweetblue/library/src/main/res'
              ...
          }
          ...
      }
      
      ```
    
  5. Else if building with JAR, it should look something like this:

    ```
    
    android {
        compileSdkVersion 25
        buildToolsVersion '25.0.3'
        
        defaultConfig {
            minSdkVersion 18
            targetSdkVersion 25
            ...
        }
    
        dependencies {
            compile fileTree(dir: 'libs', include: '*.jar')
            ...
        }
        ...
    }
    
    ```
    
2. Now add these to the root of `MyApp/AndroidManifest.xml`:
 
    ```
    <uses-sdk android:minSdkVersion="18" android:targetSdkVersion="25" />
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <!-- NOTE: Location is a new requirement for scanning in Android M.  -->
    <!--       You may use ACCESS_FINE_LOCATION also or instead.         -->
    ```
        
3. From your `Activity` or `Service` or `Application` instance, this is all it takes to discover a device, connect to it, and read a characteristic:
    ```
    // A ScanFilter decides whether a BleDevice instance will be created from a
    // BLE advertisement and passed to the DiscoveryListener implementation below.
    final ScanFilter scanFilter = new ScanFilter()
    {
    	@Override public Please onEvent(ScanEvent e)
    	{
    		return Please.acknowledgeIf(e.name_normalized().contains("my_device"))
    		             .thenStopScan();
    	}
    };
    
    // New BleDevice instances are provided through this listener.
    // Nested listeners then listen for connection and read results.
    // Obviously you will want to structure your actual code a little better.
    // The deep nesting simply demonstrates the async-callback-based nature of the API.
    final DiscoveryListener discoveryListener = new DiscoveryListener()
    {
    	@Override public void onEvent(DiscoveryEvent e)
    	{
    		if( e.was(LifeCycle.DISCOVERED) )
    		{
	    		e.device().connect(new StateListener()
	    		{
	    			@Override public void onEvent(StateEvent e)
	    			{
	    				if( e.didEnter(BleDeviceState.INITIALIZED) )
	    				{
	    					e.device().read(Uuids.BATTERY_LEVEL, new ReadWriteListener()
	    					{
	    						@Override public void onEvent(ReadWriteEvent e)
	    						{
	    							if( e.wasSuccess() )
	    							{
	    								Log.i("", "Battery level is " + e.data_byte() + "%");
    								}
    							}
    						});
    					}
    				}
    			});
    		}
    	}
};
    
    // This class helps you navigate the treacherous waters of Android M Location requirements for scanning.
    // First it enables bluetooth itself, then location permissions, then location services. The latter two
    // are only needed in Android M. This must be called from an Activity instance.
    BluetoothEnabler.start(this, new DefaultBluetoothEnablerFilter()
    {
    	@Override public Please onEvent(BluetoothEnablerEvent e)
    	{
        	if( e.isDone() )
        	{
        		e.bleManager().startScan(scanFilter, discoveryListener);
        	}
        	
        	return super.onEvent(e);
        }
    });
    ```


Licensing
=========

SweetBlue is released here under the [GPLv3](http://www.gnu.org/copyleft/gpl.html). Please visit http://idevicesinc.com/sweetblue for proprietary licensing options. In a nutshell, if you're developing a for-profit commercial app you may use this library for free for evaluation purposes, but most likely your use case will require purchasing a proprietary license before you can release your app to the public. See the [FAQ](https://github.com/iDevicesInc/SweetBlue/wiki/FAQ) for more details and https://tldrlegal.com/license/gnu-general-public-license-v3-%28gpl-3%29 for a general overview of the GPL.
<p align="center"><a href="https://idevicesinc.com/sweetblue"><img src="https://github.com/iDevicesInc/SweetBlue/blob/master/scripts/assets/sweetblue_logo.png" /></a></p>
