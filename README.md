siphon-base
===========

Overview
--------

A siphon-base project is the fundamental project on which Siphon apps run. It
consists of config files, the SiphonSDK installed from our
siphon-sdk-<platform> repo (using Cocoapods for iOS), and the files necessary
to wire the SDK up. Different versions of our base project exist to support
particular versions of React Native and the corresponding version of our SDK.

A base project may be configured with the following parameters (these are passed
in as preprocessor macros for Xcode projects):

* SIPHON_APP_ID - The id of the app that the base project will run
* SIPHON_AUTH_TOKEN - The authentication token of the user (needed if the app
is being run in development mode)
* SIPHON_DEV_MODE - If this is provided, the app will be loaded from Siphon's
cloud services, and will use websockets to listen for updates, which will load
automatically. If it is not provided, the app will be loaded from an assets.zip
file included in the app bundle that contains the bundle-footer, any assets
that are required by the app and an asset_listing file recording which assets
are supplied.

Building
--------

Basic prerequisites:

    $ brew install python3
    $ brew install xctool

Install fastlane for managing builds, and make sure the latest version of
xcode is installed:

    $ sudo gem install fastlane --verbose
    $ xcode-select install

For convenience, install `virtualenvwrapper` on your machine:

    $ pip install virtualenvwrapper
    $ export WORKON_HOME=~/.virtualenv # put this in your .bash_profile too
    $ mkdir -p $WORKON_HOME

Make sure that the latest version of node is installed (4.1.1 as of this
writing)

    $ nvm install node && nvm alias default node

Create a virtual environment and activate it:

    $ mkvirtualenv --python=`which python3` siphon-base
    $ workon siphon-base
    $ python --version # should be Python 3

Make siphon-base-install.py executable:

    $ chmod +x siphon-base-install.py

Make generate-cli-packages.py executable:

    $ chmod +x generate-cli-packages.py

Install any dependencies that come in the form of node modules, and clean up
superfluous directories that they may leave behind:

    $ ./siphon-base-install.py

For Android you will need to install the Android SDK and Android NDK and
configure your `local.properties` file (Android Studio can probably do
that for you) to point to the right place. See here:

https://facebook.github.io/react-native/docs/android-building-from-source.html

Example:

```
ndk.dir=/Users/james/Library/Android/android-ndk-r10e
```


Updating node dependencies
------------------------------

When our siphon-dependencies repo is updated with the latest dependencies
for a given version (modules for sandbox and production environments),
base files need to be updated.

To update relevant base files, install the developer python dependencies:

    $ pip install -r dev_requirements.txt

Make the update script executable:

    $ chmod +x ./update-dependencies.py

Finally, run the script:

    $ ./update-dependencies.py

Generating Client Packages
--------------------------

In order to generate the client-side tar.gz packages of our base versions,
run

    $ ./generate-cli-packages.py --dest /path/to/destination/directory

The destination directory will be created, if it does not exist, and will
be populated with the tar archives.

Modifying the base Xcode project
================================

Base Xcode projects for Siphon live in the
`versions/<version_number>base-project` directory in the root of this
repository. Since they are essentially configurable shells that house our
SDK, they should not need to be modified extensively. If it is necessary to
open the project in Xcode, be sure to open the .xcworkspace file.

The most likely reason for modification is to update any libraries/frameworks
that they include. For Xcode projects, this can be done by modifying the
corresponding Podfile, if the module is Cocoapods compliant, then navigating
into the base-project directory and running

    $ pod install

Some third-party libraries, however, must be added to the project manually.
If this is the case, modify the corresponding
`versions/<version_number>base-project/lib/package.json` file and run

    $ ./siphon-base-install.py

from the root of the repo. Then follow the installation instructions in the
Github repo of each of the third-party modules.

How to create an Xcode `base-project` from scratch
==================================================

This is loosely based on the [Integrating with Existing Apps](https://facebook.github.io/react-native/docs/embedded-app-ios.html)
guide.

(1) Create a fresh project in Xcode and choose the 'Single View Application'
template

(2) Fill out the wizard's fields as follows:

    Product Name: SiphonBase
    Organization Name: Siphon
    Organization Identifier: siphon
    Language: Objective-C
    Devices: Universal
    (Deselect all the checkboxes at the bottom.)

(3) Copy the `SiphonBase` directory (the one which contains `AppDelegate.m`)
and `SiphonBase.xcodeproj` into `versions/<version>/base-project/ios`.

(4) Open the Xcode project and build it to make sure everything is fine. Also
navigate to `Preferences -> Source Control` and disable source control within
Xcode.

(5) Install dependencies:

Place a Podfile at `versions/<version>/base-project/ios/Podfile` and put the
React pods in there, but make sure they're versioned like this, for example:

    pod 'React', '0.11.0'
    pod 'React/RCTText', '0.11.0'
    ... all required sub-specs ...
    pod 'Siphon', :git => ...

Create a `versions/<version_number>base-project/lib/package.json` file
any dependencies that need to be installed via npm. run

    $ ./siphon-base-install.py

from the root of the repo and follow the installation instructions in the
Github repo of each of the third-party modules.

(6) Make sure the Xcode project isn't open, then install the pods:

    $ sudo gem install cocoapods
    $ pod setup
    $ cd base-project/ios
    $ pod install

(7) Open `SiphonBase.xcworkspace` in Xcode and delete the following files:

    ViewController.h
    ViewController.m
    Main.storyboard

(8) Also with Xcode, open `Info.plist` and

* remove the key `Launch screen interface file base name`
* remove the key `Main storyboard file base name`.
* set `View controller-based status status bar appearance` to `NO`

(9) Add an `SPConfig.h` to the project:

```
extern NSString *const SP_APP_ID;
extern NSString *const SP_AUTH_TOKEN;
extern BOOL const SP_DEV_MODE;
```
And the corresponding `SPConfig.m`file:

```
#import "SPConfig.h"

#define MACRO_NAME(x) #x
#define MACRO_VALUE(x) MACRO_NAME(x)

#ifdef SIPHON_APP_ID
NSString *const SP_APP_ID = @MACRO_VALUE(SIPHON_APP_ID);
#else
NSString *const SP_APP_ID = @"";
#endif

#ifdef SIPHON_AUTH_TOKEN
NSString *const SP_AUTH_TOKEN = @MACRO_VALUE(SIPHON_AUTH_TOKEN);
#else
NSString *const SP_AUTH_TOKEN = @"";
#endif

#ifdef SIPHON_DEV_MODE
BOOL const SP_DEV_MODE = TRUE;
#else
BOOL const SP_DEV_MODE = FALSE;
#endif
```

(10) Modify `AppDelegate.m`:  

```
#import "AppDelegate.h"
#import "SPConfig.h"
#import <Siphon/SPAppViewController.h>

@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {

    [[UIApplication sharedApplication] setIdleTimerDisabled:YES];

    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    [self.window makeKeyAndVisible];

    SPAppViewController *appViewController = [[SPAppViewController alloc] initWithAppId:SP_APP_ID andAuthToken:SP_AUTH_TOKEN devMode:SP_DEV_MODE];

    self.window.rootViewController = appViewController;


    return YES;
}

... (leave default methods below)
```

How to create an Android `base-project` from scratch
====================================================

This is loosely based on: https://facebook.github.io/react-native/docs/embedded-app-android.html

(0) Install the Kotlin plugin in the Android Studio settings.

(1) Create a fresh project in Android Studio and choose the 'Single View Application' template

(2) Initial wizard fields:

  Application name: SiphonBase
  Company domain: getsiphon.com

(3) "Target Android Devices" step:

  * Tick only 'Phone and Tablet'
  * Pick: API 16 (Android 4.1)

(4) Pick 'Empty Activity'

(5) Call it the default `MainActivity` and disable 'Generate Layout File'

(6) Within `MainActivity` we trigger the desired Siphon activity like this:

```
startActivity(new Intent(this, DevelopmentAppActivity.class));
```

Android: Building our React Native libs from from source
============================================================

If the target React Native version is not yet available on Maven Central,
we need to bundle the source code ourselves and point Gradle in the right
direction.

This guide is loosely based on this: https://facebook.github.io/react-native/docs/android-building-from-source.html

You will need the Android SDK + NDK installed, use this version of the NDK:

https://dl.google.com/android/repository/android-ndk-r10e-darwin-x86_64.zip

The below example is for base_version 0.4 / React Native 0.22.2 -- Navigate
to the right place first:

$ cd /path/to/siphon-base/versions/0.4/base-project/android

Clone our fork there:

$ wget https://github.com/getsiphon/react-native/archive/4d0e29a.tar.gz

Extract only the Android sources:

$ tar xvfz <file>.tar.gz react-native-*/ReactAndroid
$ mv react-native-<sha> react-native
$ rm <file>.tar.gz

Now tell Android Studio to do a gradle sync (it will show a banner). Now wait
two millennia for it to compile :)
