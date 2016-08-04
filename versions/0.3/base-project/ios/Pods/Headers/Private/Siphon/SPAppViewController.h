@protocol SPAppViewControllerDelegate <NSObject>

@optional
- (void)appDismissed:(NSString *)appId;
@end

@interface SPAppViewController : UIViewController

- (instancetype)initWithAppId:(NSString *)appId andSubmissionId:(NSString *)submissionId;
- (instancetype)initWithAppId:(NSString *)appId andSubmissionId:(NSString *)submissionId andHost:(NSString *)host;

@end