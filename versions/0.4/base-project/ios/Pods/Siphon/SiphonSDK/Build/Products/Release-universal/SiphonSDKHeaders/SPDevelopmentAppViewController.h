@protocol SPDevelopmentAppViewControllerDelegate <NSObject>

@optional
- (void)appDismissed:(NSString *)appId;
@end

@interface SPDevelopmentAppViewController : UIViewController

// Initialize a new SPAppViewController for a given app. Use devMode to set streaming vs polling updates,
// and sandboxMode to return sandbox-safe apps with a 'back' button
- (instancetype)initWithAppId:(NSString *)appId andAuthToken:(NSString *)authToken andDelegate:(id)delegate andHost:(NSString *)host sandboxMode:(BOOL)sandboxMode devMode:(BOOL)devMode;
// Use the default Siphon host
- (instancetype)initWithAppId:(NSString *)appId andAuthToken:(NSString *)authToken andDelegate:(id)delegate sandboxMode:(BOOL)sandboxMode devMode:(BOOL)devMode;

// Default Siphon host, sandboxMode & devMode disabled
- (instancetype)initWithAppId:(NSString *)appId andAuthToken:(NSString *)authToken;

@end