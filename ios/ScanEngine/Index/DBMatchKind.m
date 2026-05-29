#import "DBMatchKind.h"

NSString *const DBMatchKindExactBytes = @"EXACT_BYTES";
NSString *const DBMatchKindSameContentVideo = @"SAME_CONTENT_VIDEO";

const double DBMatchKindConfidenceExact = 1.0;
const double DBMatchKindConfidenceVideoContent = 0.95;
