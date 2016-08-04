//
//  AppDelegate.m
//  SiphonBase
//
//  Created by James Potter on 22/09/2015.
//  Copyright © 2015 Siphon. All rights reserved.
//

#import "AppDelegate.h"
#import "SPConfig.h"
#import <React/RCTRootView.h>
#import <Siphon/SPAppViewController.h>
#import <Siphon/SPDevelopmentAppViewController.h>
#import <FBSDKCoreKit/FBSDKCoreKit.h>

@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    
    [[UIApplication sharedApplication] setIdleTimerDisabled:YES];
    
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    [self.window makeKeyAndVisible];
    
    if (SP_SUBMISSION_ID) {
        // We want to run a production app
        SPAppViewController *appViewController;
        
        if (SP_CUSTOM_HOST) {
            appViewController = [[SPAppViewController alloc] initWithAppId:SP_APP_ID andSubmissionId:SP_SUBMISSION_ID andHost:SP_CUSTOM_HOST];;
        } else {
            appViewController = [[SPAppViewController alloc] initWithAppId:SP_APP_ID andSubmissionId:SP_SUBMISSION_ID];
        }
        self.window.rootViewController = appViewController;
    } else if (SP_PACKAGER_HOST && SP_PACKAGER_PORT && SP_PACKAGER_ENDPOINT) {
        // We want to run a regular RN app using the RN packager
        NSString *urlString = [NSString stringWithFormat:@"http://%@:%@/%@", SP_PACKAGER_HOST, SP_PACKAGER_PORT, SP_PACKAGER_ENDPOINT];
        RCTRootView *rootView = [[RCTRootView alloc] initWithBundleURL:[NSURL URLWithString:urlString] moduleName:@"App" initialProperties:nil launchOptions:nil];
        UIViewController *rootViewController = [[UIViewController alloc] init];
        rootViewController.view = rootView;
        self.window.rootViewController = rootViewController;
    } else {
        // We want to run a development app using our packager
        SPDevelopmentAppViewController *appViewController;
        BOOL devMode = [SP_DEV_MODE isEqualToString:@"ON"] ? YES : NO;
        if (SP_CUSTOM_HOST) {
            appViewController = [[SPDevelopmentAppViewController alloc] initWithAppId:SP_APP_ID andAuthToken:SP_AUTH_TOKEN andDelegate:nil andHost:SP_CUSTOM_HOST sandboxMode:NO devMode:devMode];
        } else {
            appViewController = [[SPDevelopmentAppViewController alloc] initWithAppId:SP_APP_ID andAuthToken:SP_AUTH_TOKEN andDelegate:nil sandboxMode:NO devMode:devMode];
        }
        self.window.rootViewController = appViewController;
    }
    
    // If the FacebookAppID key in the info.plist file has been modified (i.e. it doesn't equal
    // {{facebook-app-id}}) then we perform the required set up.
    NSString *facebookAppId = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"FacebookAppID"];
    if (![facebookAppId isEqualToString:@"{{facebook-app-id}}"]) {
        [[FBSDKApplicationDelegate sharedInstance] application:application didFinishLaunchingWithOptions:launchOptions];
    }

    return YES;
}


- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
    // If the FacebookAppID key in the info.plist file has been modified (i.e. it doesn't equal
    // {{facebook-app-id}}) then we perform the required set up.
    NSString *facebookAppId = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"FacebookAppID"];
    BOOL handled = NO;
    if (![facebookAppId isEqualToString:@"{{facebook-app-id}}"]) {
        handled = [[FBSDKApplicationDelegate sharedInstance] application:application openURL:url sourceApplication:sourceApplication annotation:annotation];
    }
    // Add any custom logic here.
    return handled;
}

- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

@end
