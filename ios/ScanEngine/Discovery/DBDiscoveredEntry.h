#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/** Wire values for media_type hint (architecture §4.1 stage 1). */
typedef NSString *DBMediaTypeHint NS_TYPED_ENUM;
FOUNDATION_EXPORT DBMediaTypeHint const DBMediaTypeHintImage;
FOUNDATION_EXPORT DBMediaTypeHint const DBMediaTypeHintVideo;
FOUNDATION_EXPORT DBMediaTypeHint const DBMediaTypeHintAudio;
FOUNDATION_EXPORT DBMediaTypeHint const DBMediaTypeHintDocument;
FOUNDATION_EXPORT DBMediaTypeHint const DBMediaTypeHintText;
FOUNDATION_EXPORT DBMediaTypeHint const DBMediaTypeHintOther;

/** One file discovered under an active scan_root (Modes A and B). */
@interface DBDiscoveredEntry : NSObject

/** Set for Mode A `file://` / security-scoped paths and optional bookmark union. */
@property (nonatomic, copy, nullable) NSURL *contentURL;
/** Set for Mode B PHAsset rows (`uri_or_path` in index). */
@property (nonatomic, copy, nullable) NSString *phAssetLocalIdentifier;
@property (nonatomic, assign) NSInteger scanRootId;
@property (nonatomic, assign) NSInteger generation;
@property (nonatomic, copy) NSString *displayName;
@property (nonatomic, copy) DBMediaTypeHint mediaTypeHint;
@property (nonatomic, assign) int64_t sizeBytes;
@property (nonatomic, assign) int64_t mtimeNs;

- (instancetype)initWithContentURL:(nullable NSURL *)contentURL
            phAssetLocalIdentifier:(nullable NSString *)phAssetLocalIdentifier
                       scanRootId:(NSInteger)scanRootId
                       generation:(NSInteger)generation
                      displayName:(NSString *)displayName
                    mediaTypeHint:(DBMediaTypeHint)mediaTypeHint
                        sizeBytes:(int64_t)sizeBytes
                          mtimeNs:(int64_t)mtimeNs;

@end

NS_ASSUME_NONNULL_END
