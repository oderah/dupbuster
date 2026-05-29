#import <Foundation/Foundation.h>
#import <sqlite3.h>

NS_ASSUME_NONNULL_BEGIN

@interface DBCatalogDatabase : NSObject

@property (nonatomic, readonly, nullable) sqlite3 *db;

- (instancetype)initWithPath:(NSString *)path NS_DESIGNATED_INITIALIZER;
- (instancetype)init NS_UNAVAILABLE;

+ (instancetype)sharedDatabase;
+ (instancetype)inMemoryDatabase;

- (BOOL)openWithError:(NSError * _Nullable * _Nullable)error;
- (void)close;

@end

NS_ASSUME_NONNULL_END
