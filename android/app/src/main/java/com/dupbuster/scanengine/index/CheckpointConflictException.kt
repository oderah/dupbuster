package com.dupbuster.scanengine.index

/** Thrown when [CheckpointStore.beginRun] would create a conflicting active `scan_run`. */
class CheckpointConflictException(
    val rootId: Long?,
    val generation: Int,
) : IllegalStateException(
    "Active scan_run already exists for rootId=$rootId generation=$generation",
)
