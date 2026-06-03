#import "DBMatchKind.h"

NSString *const DBMatchKindExactBytes = @"EXACT_BYTES";
NSString *const DBMatchKindSameContentVideo = @"SAME_CONTENT_VIDEO";
NSString *const DBMatchKindSameContentImage = @"SAME_CONTENT_IMAGE";

const double DBMatchKindConfidenceExact = 1.0;
const double DBMatchKindConfidenceVideoContent = 0.95;
const double DBMatchKindConfidenceImageContent = 0.95;
