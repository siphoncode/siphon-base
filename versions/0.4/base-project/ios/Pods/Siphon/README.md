
siphon-sdk-ios
==============

Siphon's SDK for iOS apps. The Siphon SDK provides an SPAppViewController
component that is responsible for loading a given app and presenting it on
screen.

There are two Xcode project directories in this repo: `Example` and `SiphonSDK`.
The former is an application for local development, where the SDK is installed
as a 'development pod' meaning that modifying SDK files in the 'Example'
project will actually modify the SDK files themselves. 'SiphonSDK', on the other
hand, contains static library project.

Requirements
------------

Make sure Cocoapods is installed:

    $ sudo gem install cocoapods

Version considerations
----------------------

Different versions of the SDK are kept on separate branches that correspond
to their compatible base version. Make sure that development is being done
on the correct branch!

TODO: Git tagging (minor versions?)

Local development
-----------------

Local development is done using the 'Example' application project. To start
modifying the SDK, open `Example/Siphon.xcworkspace` using Xcode, and navigate
to the 'Development Pods' directory in the Navigator, where the SDK files
can be found.

If you require a new SDK file, you must add it to the `SiphonSDK/SiphonSDK`
directory (or a subdirectory therein). Once the file has been created, close
Xcode, navigate to the `Example` directory, and run

    $ pod install

The file(s) should now be visible in the `Development Pods` directory of the
`Pods` target.

If you wish to modify the path/name of any of the SDK files, the same procedure
must be followed.

Building a universal binary
----------------------------

Once the SDK is fit for release, open `SiphonSDK/SiphonSDK.xcworkspace`
using Xcode and:

1. Select 'SiphonSDK' from the list of targets on the primary toolbar.
2. Select a simulator device from the list of devices on the primary toolbar and
click the 'play' button.
3. Select 'Generic iOS Device' from the same dropdown list and click the 'play'
button.
4. Select 'UniversalLib' from the list of targets on the primary toolbar and
click the 'play' button.

Commit/tag and push the changes and the SDK is ready for use by another
in another Xcode project (see below).

Incorporating the SDK in another project
----------------------------------------

The SDK can be included in a project by specifying the following in the
project's Podfile:

`pod 'Siphon', :git => '<sdk_repo>' commit:'<hash>' (or tag:'<tag>')`

The 'SPAppViewController' component is then accessed via
`#import <Siphon/SPAppViewController.h>`.
