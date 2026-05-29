#import "DBHashConstants.h"

const int64_t DBHashSampleSizeThresholdBytes = 50LL * 1024 * 1024;
const NSUInteger DBHashSampleChunkBytes = 64 * 1024;
const NSUInteger DBHashReadBufferBytes = 1024 * 1024;
const int64_t DBHashLargeFileCapBytes = 2LL * 1024 * 1024 * 1024;
const NSTimeInterval DBHashTimeoutSeconds = 120.0;
