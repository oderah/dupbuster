#import "RCTNativeScanEngine.h"

#import <React/RCTUtils.h>

#import "DBCatalogDatabase.h"
#import "DBCatalogMeta.h"
#import "DBCatalogReader.h"
#import "DBCatalogSnapshotBridgeMapper.h"
#import "DBIndexWriter.h"
#import "DBScanOrchestrator.h"
#import "DBScanOrchestratorFactory.h"
#import "DBScanStartRequest.h"
#import "DBScanStartRequestParser.h"

static NSString *const kScanEngineNotImplemented = @"SCANENGINE_NOT_IMPLEMENTED";
static NSString *const kScanStartFailed = @"SCAN_START_FAILED";
static NSString *const kScanControlFailed = @"SCAN_CONTROL_FAILED";

@implementation RCTNativeScanEngine {
  DBScanOrchestrator *_orchestrator;
}

- (DBScanOrchestrator *)orchestrator
{
  if (_orchestrator == nil) {
    __weak RCTNativeScanEngine *weakSelf = self;
    _orchestrator = [DBScanOrchestratorFactory createWithEmitProgress:^(NSDictionary *payload) {
      RCTNativeScanEngine *strongSelf = weakSelf;
      if (strongSelf == nil) {
        return;
      }
      dispatch_async(dispatch_get_main_queue(), ^{
        [strongSelf emitOnScanProgress:payload];
      });
    }
                                                              emitError:^(NSDictionary *payload) {
      RCTNativeScanEngine *strongSelf = weakSelf;
      if (strongSelf == nil) {
        return;
      }
      dispatch_async(dispatch_get_main_queue(), ^{
        [strongSelf emitOnScanError:payload];
      });
    }];
  }
  return _orchestrator;
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeScanEngineSpecJSI>(params);
}

- (void)startScan:(JS::NativeScanEngine::ScanStartOptions &)options
          resolve:(RCTPromiseResolveBlock)resolve
           reject:(RCTPromiseRejectBlock)reject
{
  @try {
    DBScanStartRequest *request = [self scanStartRequestFromCodegenOptions:options];
    NSError *error = nil;
    NSInteger scanRunId = [self.orchestrator startScanWithRequest:request error:&error];
    if (error != nil) {
      if (error.code == DBScanOrchestratorErrorResumeUnsupported) {
        reject(kScanEngineNotImplemented, error.localizedDescription, error);
        return;
      }
      reject(kScanStartFailed, error.localizedDescription, error);
      return;
    }
    resolve(@{@"scanRunId" : @(scanRunId)});
  } @catch (NSException *exception) {
    reject(kScanStartFailed, exception.reason, nil);
  }
}

- (void)pauseScan:(double)scanRunId
          resolve:(RCTPromiseResolveBlock)resolve
           reject:(RCTPromiseRejectBlock)reject
{
  NSError *error = nil;
  if (![self.orchestrator pauseScanWithId:(NSInteger)scanRunId error:&error]) {
    reject(kScanControlFailed, error.localizedDescription ?: @"pauseScan failed", error);
    return;
  }
  resolve(nil);
}

- (void)resumeScan:(double)scanRunId
           resolve:(RCTPromiseResolveBlock)resolve
            reject:(RCTPromiseRejectBlock)reject
{
  NSError *error = nil;
  if (![self.orchestrator resumeScanWithId:(NSInteger)scanRunId error:&error]) {
    reject(kScanControlFailed, error.localizedDescription ?: @"resumeScan failed", error);
    return;
  }
  resolve(nil);
}

- (void)cancelScan:(double)scanRunId
           resolve:(RCTPromiseResolveBlock)resolve
            reject:(RCTPromiseRejectBlock)reject
{
  NSError *error = nil;
  if (![self.orchestrator cancelScanWithId:(NSInteger)scanRunId error:&error]) {
    reject(kScanControlFailed, error.localizedDescription ?: @"cancelScan failed", error);
    return;
  }
  resolve(nil);
}

- (void)deleteDuplicates:(JS::NativeScanEngine::DeleteDuplicatesCommand &)command
                 resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject
{
  (void)command;
  reject(kScanEngineNotImplemented,
         @"deleteDuplicates is not implemented until M3 DeleteCoordinator",
         nil);
}

- (void)getCatalogMeta:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject
{
  (void)reject;
  DBIndexWriter *writer = [[DBIndexWriter alloc] initWithDatabase:[DBCatalogDatabase sharedDatabase]];
  DBCatalogMeta *meta = [writer readCatalogMeta];
  resolve(@{
    @"schemaVersion" : @(meta.schemaVersion),
    @"fullRescanRequired" : @(meta.fullRescanRequired),
  });
}

- (void)getCatalogSnapshot:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject
{
  @try {
    DBCatalogReader *reader = [[DBCatalogReader alloc] initWithDatabase:[DBCatalogDatabase sharedDatabase]];
    DBCatalogSnapshot *snapshot = [reader readSnapshot];
    resolve([DBCatalogSnapshotBridgeMapper bridgePayloadForSnapshot:snapshot]);
  } @catch (NSException *exception) {
    reject(@"CATALOG_READ_FAILED", exception.reason, nil);
  }
}

- (DBScanStartRequest *)scanStartRequestFromCodegenOptions:
    (const JS::NativeScanEngine::ScanStartOptions &)options
{
  NSMutableDictionary *payload = [NSMutableDictionary dictionary];
  payload[@"mode"] = options.mode();
  NSMutableArray *roots = [NSMutableArray array];
  auto codegenRoots = options.roots();
  for (size_t index = 0; index < codegenRoots.size(); index++) {
    auto root = codegenRoots[index];
    NSMutableDictionary *rootMap = [@{@"uriGrant" : root.uriGrant()} mutableCopy];
    if (root.scanRootId().has_value()) {
      rootMap[@"scanRootId"] = @((NSInteger)root.scanRootId().value());
    }
    [roots addObject:rootMap];
  }
  payload[@"roots"] = roots;
  if (options.resumeScanRunId().has_value()) {
    payload[@"resumeScanRunId"] = @((NSInteger)options.resumeScanRunId().value());
  }
  return [DBScanStartRequestParser parseDictionary:payload];
}

+ (NSString *)moduleName
{
  return @"NativeScanEngine";
}

@end
