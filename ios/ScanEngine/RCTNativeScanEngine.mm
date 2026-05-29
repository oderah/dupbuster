#import "RCTNativeScanEngine.h"

#import <React/RCTUtils.h>

#import "DBCatalogDatabase.h"
#import "DBCatalogMeta.h"
#import "DBIndexWriter.h"

static NSString *const kScanEngineNotImplemented = @"SCANENGINE_NOT_IMPLEMENTED";

@implementation RCTNativeScanEngine

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeScanEngineSpecJSI>(params);
}

- (void)startScan:(NSDictionary *)options
          resolve:(RCTPromiseResolveBlock)resolve
           reject:(RCTPromiseRejectBlock)reject
{
  reject(kScanEngineNotImplemented,
         @"startScan is not implemented until scan orchestrator wires Grouper (M1-10+)",
         nil);
}

- (void)pauseScan:(double)scanRunId
          resolve:(RCTPromiseResolveBlock)resolve
           reject:(RCTPromiseRejectBlock)reject
{
  reject(kScanEngineNotImplemented,
         @"pauseScan is not implemented until ScanEngine pipeline tasks (M1-03+)",
         nil);
}

- (void)resumeScan:(double)scanRunId
           resolve:(RCTPromiseResolveBlock)resolve
            reject:(RCTPromiseRejectBlock)reject
{
  reject(kScanEngineNotImplemented,
         @"resumeScan is not implemented until ScanEngine pipeline tasks (M1-03+)",
         nil);
}

- (void)cancelScan:(double)scanRunId
           resolve:(RCTPromiseResolveBlock)resolve
            reject:(RCTPromiseRejectBlock)reject
{
  reject(kScanEngineNotImplemented,
         @"cancelScan is not implemented until ScanEngine pipeline tasks (M1-03+)",
         nil);
}

- (void)deleteDuplicates:(NSDictionary *)command
                 resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject
{
  reject(kScanEngineNotImplemented,
         @"deleteDuplicates is not implemented until M3 DeleteCoordinator",
         nil);
}

- (void)getCatalogMeta:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject
{
  DBIndexWriter *writer = [[DBIndexWriter alloc] initWithDatabase:[DBCatalogDatabase sharedDatabase]];
  DBCatalogMeta *meta = [writer readCatalogMeta];
  resolve(@{
    @"schemaVersion" : @(meta.schemaVersion),
    @"fullRescanRequired" : @(meta.fullRescanRequired),
  });
}

+ (NSString *)moduleName
{
  return @"NativeScanEngine";
}

@end
