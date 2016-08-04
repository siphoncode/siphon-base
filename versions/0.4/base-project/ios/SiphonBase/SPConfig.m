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

#ifdef SIPHON_HOST
NSString *const SP_CUSTOM_HOST = @MACRO_VALUE(SIPHON_HOST);
#else
NSString *const SP_CUSTOM_HOST = nil;
#endif

#ifdef SIPHON_SUBMISSION_ID
NSString *const SP_SUBMISSION_ID = @MACRO_VALUE(SIPHON_SUBMISSION_ID);
#else
NSString *const SP_SUBMISSION_ID = nil;
#endif

#ifdef SIPHON_PACKAGER_HOST
NSString *const SP_PACKAGER_HOST = @MACRO_VALUE(SIPHON_PACKAGER_HOST);
#else
NSString *const SP_PACKAGER_HOST = nil;
#endif

#ifdef SIPHON_PACKAGER_PORT
NSString *const SP_PACKAGER_PORT = @MACRO_VALUE(SIPHON_PACKAGER_PORT);
#else
NSString *const SP_PACKAGER_PORT = nil;
#endif

#ifdef SIPHON_PACKAGER_ENDPOINT
NSString *const SP_PACKAGER_ENDPOINT = @MACRO_VALUE(SIPHON_PACKAGER_ENDPOINT);
#else
NSString *const SP_PACKAGER_ENDPOINT = nil;
#endif

#ifdef SIPHON_DEV_MODE
NSString *const SP_DEV_MODE = @MACRO_VALUE(SIPHON_DEV_MODE);
#else
NSString *const SP_DEV_MODE = @"OFF";
#endif
