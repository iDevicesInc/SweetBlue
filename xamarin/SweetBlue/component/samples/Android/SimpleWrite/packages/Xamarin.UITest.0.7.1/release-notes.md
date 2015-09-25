# Xamarin.UITest release notes

## 0.7.1

* Readded --async option to anonymous submit command in uploader (test-cloud.exe)
* Fixed typo in default locale value in uploader (test-cloud.exe)
* Reduced memory usage in loggers
* Improved error reporting in IOS host run loop
* Fixed error in naming of inherited [Test]
* Added support for parameterized test fixtures in uploader (test-cloud.exe)
* Fixed an error when querying with All().Css()

## 0.7.0

* Added methods for Pinch gestures on Android
* Added `PreferIdeSettings` to configuration for upcoming IDE integration
* Added an experimental `AppDataMode` parameter in `StartApp` for controlling whether data is cleared or not in Android test runs
* Fixed upload issues with test chunking when uploading to Xamarin Test Cloud when encountering assemblies that couldn't be resolved
* Removed the legacy positional device parameter in `test-cloud.exe submit` - you should now use `--devices` instead
* Added support for trace sources
* Added `test-cloud.exe repl` command for launching a REPL without a test suite
* Added support for `InstalledApp` for Android as an alternative to `ApkFile`
* Added support for `LogPath` to control the log output directory for both Android and iOS
* Added label printing to `tree` in the REPL
* Added `DragAndDrop` support for Android
* Added `DoubleTap` support for web views on Android
* Fixed an error in `WaitForElement` if the element was visible briefly and then disappeared
* Added support for specifying an activity to launch using `LaunchableActivity` on Android
* Improved discovery of the Android SDK if using Xamarin Studio on OSX
* Increased the length of `SwipeRight` and `SwipeLeft` on Android to make them more useful

## 0.6.9

* Improved test detection in uploader
* `tree` command speedup on IOS
* Updated communication with Test Cloud Agent to support 12.0.x
* Android test server updated to 0.5.6.pre2 to fix issue with invoking methods on main Activity

## 0.6.8

* Removed assembly loading from the REPL when looking for extension methods to avoid crash on Mono
* Fixed an issue that would sometimes cause waits longer than expected after gestures

## 0.6.7

* Added `app.PressUserAction` for Android
* Added `app.SendAppToBackground` for iOS
* Improved stability of detection of connected iOS devices
* Improved error message when the REPL fails to load assemblies
* Improved error message when UIAutomation is not enabled on iOS 8 devices
* Improved detection of Android emulators
* Fixed an issue with Android `app.Invoke`
* Fixed wait for keyboard on iOS in Test Cloud
* Added support for launching Android apps using activity aliases
* Fixed `tree` on Android where some visible elements would not show up

## 0.6.6

* Adds methods for clearing text fields with `app.ClearText`
* Improved iOS physical device detection
* Fixes an issue on iOS 8 with coordinates being off in some cases
* Fixed an issue where the REPL would not work with ReSharper shadow copy
* Improved error reporting when the REPL fails to launch properly
* Added XPath selectors as an alternative to Css for web views
* Added double tap gesture for Android
* Fixed a hang issue in Android with multiple UI threads and modal dialogs

## 0.6.5

* Re-enabled shared runtime check
* Fixes an issue with iOS version parsing

## 0.6.4

* Disable shared runtime check 

## 0.6.2

* Improved test execution speed for locally connected iOS 8 devices
* Improved tree command output 
* Repl extented to support extension methods, including custom ones

## 0.6.1

* Fixes an issue where api key wasn't being properly passed to REPL

## 0.6.0

* Support added for XCode 6 and iOS 8
* Added activation. An api key must be provided in order to run on physical devices or test runs for 15+ minutes
* Added `Back` on iOS apps
* Added `TextField` and `Switch` to the query api
* Added automatic clean-up of test artifacts in the temp folder so they don't grow forever
* Added proper exit codes to `test-cloud.exe` for various error scenarios
* Fixed a bug in `Scroll`
* Changed REPL intellisense to use NRefactory (same as XS) - much better now
* Added the `copy` command in the REPL to copy your command history to clipboard for easy paste into test
* Added the first tab completion shortcuts to REPL. `tap` + (tab) expands to `app.Tap(x => x.`, `query` + (tab) expands to `app.Query(x => x.`
* Improved stability of tests with smarter waiting for both Android and iOS. Should be less need for waits before gestures and screenshots now
* iOS `PanCoordinates` was renamed to `DragCoordinates` and added to the `IApp` interface

## 0.5.0

* `xut-console.exe` has been renamed to `test-cloud.exe`
* The `Xamarin.UITest.Console` NuGet package has been removed. Everything is (and has been for a while) contained in the `Xamarin.UITest` NuGet package
* `.DeviceIdentifier(...)` can now accept iOS simulator strings for running on a specific simulator. Entering an invalid text produces and error with all the valid options
* Support for `[TestFixtureSetUp]` has been added for Test Cloud
* Android test start up time has been improved
* Direct HTTP access to the test server is available through `.TestServer`
* `.Parent()`, `.Sibling()`, `.Descendant()` and `.Child()` can all accept an integer index for the element - so `.Child(1)` is equivalent to the current `.Child().Index(1)`
* The query language contains `.WebView()` for cross platform web view selection
* The query language has `.InvokeJS(...)` for evaluating JavaScript in matched web views
* Dynamic wait times has been added - currently used for longer default waits in Test Cloud and shorter default waits in the REPL
* `iOSApp` and `IApp` now also have support for `.Back()`
* Android `.EnterText()` has been converted from old `setText` approach to touching the input field and entering text

## 0.4.10

* Adds `EnterText(text)` to `IApp` and `AndroidApp` for typing into the currently focused view element

## 0.4.9

* Allows direct access to iOS UIA using JavaScript via `InvokeUia(script)` for advanced scenarios and workarounds
* Fixes an issue with proxying HTTP over USB for iOS on physical devices on some systems
* Improves the error message when trying to use `SetLocation` on Android without having the `android.permission.ALLOW_MOCK_LOCATIONS` permission

## 0.4.8

* Both Android and iOS now have support for `.Flash(...)` to highlight view elements matched by the query
* Both Android and iOS now have support for `.SetLocation(...)` - it can be found under `app.Device`
* Invoking methods on Android where the result cannot be serialized by calabash are handled more gracefully
* Running tests on both iOS simulators and physical devices should no longer produce port conflicts
* Deserializing json results have better support for `object` and nullables
* Configuration properties on `.Config` have found a new home in `.Device` for clearer naming
* Android scrolling works again, after adapting to the new method used by calabash-android 
* Uploading iOS tests no longer fail if user does not have the Android SDK installed
* Improved a bunch of error messages in different scenarios
