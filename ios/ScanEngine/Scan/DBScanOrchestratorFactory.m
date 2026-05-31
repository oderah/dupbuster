#import "DBScanOrchestratorFactory.h"

#import "DBCatalogDatabase.h"
#import "DBCheckpointStore.h"
#import "DBFileContentReader.h"
#import "DBGrouper.h"
#import "DBIndexWriter.h"
#import "DBProductionScanDiscoveryRunner.h"
#import "DBScanProgressBridge.h"
#import "DBSqliteDurationBucketIndex.h"
#import "DBSqliteSizeBucketIndex.h"
#import "DBStatStage.h"
#import "DBUriValidator.h"

@implementation DBScanOrchestratorFactory

+ (DBScanOrchestrator *)createWithEmitProgress:(void (^)(NSDictionary *payload))emitProgress
                                     emitError:(void (^)(NSDictionary *payload))emitError
{
  DBCatalogDatabase *database = [DBCatalogDatabase sharedDatabase];
  DBIndexWriter *indexWriter = [[DBIndexWriter alloc] initWithDatabase:database];
  DBCheckpointStore *checkpointStore = [[DBCheckpointStore alloc] initWithDatabase:database];
  DBScanProgressBridge *progressBridge = [[DBScanProgressBridge alloc] initWithEmitBlock:emitProgress];
  DBUriValidator *uriValidator = [[DBUriValidator alloc] init];
  DBStatStage *statStage = [[DBStatStage alloc] initWithUriValidator:uriValidator];

  return [[DBScanOrchestrator alloc] initWithIndexWriter:indexWriter
                                         checkpointStore:checkpointStore
                                                 grouper:[[DBGrouper alloc] initWithDatabase:database]
                                         discoveryRunner:[[DBProductionScanDiscoveryRunner alloc] init]
                                                statFile:^DBStatStageResult *(DBDiscoveredEntry *entry, DBScanRootGrant *grant) {
                                                  return [statStage statDiscoveredEntry:entry
                                                                        scanRootGrant:grant
                                                                           provenance:DBUriProvenanceGrantRoot];
                                                }
                                     hashPipelineFactory:^DBHashPipeline *(DBIndexWriter *writer) {
                                       DBFileContentReader *contentReader = [[DBFileContentReader alloc] init];
                                       DBSqliteSizeBucketIndex *sizeBucketIndex =
                                           [[DBSqliteSizeBucketIndex alloc] initWithIndexWriter:writer];
                                       DBSqliteDurationBucketIndex *durationBucketIndex =
                                           [[DBSqliteDurationBucketIndex alloc] initWithIndexWriter:writer];
                                       return [[DBHashPipeline alloc] initWithFileContentReader:contentReader
                                                                              sizeBucketIndex:sizeBucketIndex
                                                                         durationBucketIndex:durationBucketIndex
                                                                           videoFingerprinter:nil];
                                     }
                                              progressBridge:progressBridge
                                                   emitError:emitError
                                                   workQueue:nil];
}

@end
