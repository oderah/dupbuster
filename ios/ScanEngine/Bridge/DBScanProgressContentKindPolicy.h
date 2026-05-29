#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface DBScanProgressContentKindPolicy : NSObject

+ (NSString *)contentKindForHashingPhaseWithVideoFingerprintPass:(BOOL)isVideoFingerprintPass;

+ (NSString *_Nullable)bridgeContentKindForPhase:(NSString *)phase
                                    snapshotKind:(NSString *_Nullable)snapshotKind;

+ (BOOL)isAllowedBridgeValue:(NSString *)value;

@end

NS_ASSUME_NONNULL_END
