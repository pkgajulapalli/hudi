<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# RFC-83: [Sync tool to integrate with lakeview]

## Proposers

- @pkgajulapalli

## Approvers
 - @yihua
 - @codope

## Status

JIRA: <link to umbrella JIRA>

> Please keep the status updated in `rfc/README.md`.

## Abstract

This document proposes the integration of a new sync tool with [LakeView]((https://www.onehouse.ai/product/lakeview)) for Apache Hudi. LakeView is a free product provided by Onehouse for the Apache Hudi community. It exposes an interactive interface with pre-built metrics, charts, and alerts to help users monitor, optimize, and debug their hudi tables. It offers pre-built dashboards to track the performance of hudi tables to help users manage data partitions and address data skew effectively.


## Background
LakeView identifies the metrics of a hudi table by processing the commit metadata files across the timeline. When a table is synced for the first time with LakeView, a checkpoint would be initialized for it. As the hoodie commit files are uploaded to the LakeView, the checkpoint will be progressed further and metrics will be updated.

## Implementation

After every commit, the sync tool will trigger the [lakeview extractor](https://github.com/onehouseinc/LakeView). This extractor would look into the commit timeline and upload the hoodie commit files since the previous checkpoint. If the table is synced for the first time with LakeView, a new checkpoint will be created and the commit files will be uploaded starting with the archived commits. The sync tool execution time would depend on the number of commit files to be uploaded to LakeView. As there can be many commit files associated with a given table, the sync tool has a configuration option for a timeout per execution.

Once the timeline is caught up with LakeView, running the sync tool will be faster as there will be only one commit related files to be uploaded per iteration.

### Running the sync tool

Following configuration is needed to run the lakeview sync tool.

```properties
# Enable lakeview sync
hoodie.datasource.lakeview_sync.enable=true

# lakeview project & authentication
hoodie.meta.sync.lakeview.version=V1
hoodie.meta.sync.lakeview.projectId=<LakeView-project-id>
hoodie.meta.sync.lakeview.apiKey=<LakeView-api-key>
hoodie.meta.sync.lakeview.apiSecret=<LakeView-api-secret>
hoodie.meta.sync.lakeview.userId=<LakeView-user-id>

# For tables stored in S3
hoodie.meta.sync.lakeview.s3.region=<aws-region>
hoodie.meta.sync.lakeview.s3.accessKey=<optional>
hoodie.meta.sync.lakeview.s3.accessSecret=<optional>

# For tables stored in GCS
hoodie.meta.sync.lakeview.gcs.projectId=<optional-projectId>
hoodie.meta.sync.lakeview.gcs.gcpServiceAccountKeyPath=<optional-path_to_gcp_auth_key>

# Paths to be excluded by lakeview extractor
hoodie.meta.sync.lakeview.metadataExtractor.pathExclusionPatterns=<pattern1>,<pattern2>,...

# Lake & Database mappings for table base paths
hoodie.meta.sync.lakeview.metadataExtractor.lakes.<lake1>.databases.<database1>.basePaths=<basepath11>,<basepath12>
hoodie.meta.sync.lakeview.metadataExtractor.lakes.<lake1>.databases.<database2>.basePaths=<basepath21>,<basepath22>

# Optional timeout per sync
hoodie.datasource.lakeview_sync.timeout.seconds=120
```

#### Run with Hudi Streamer/Standard spark jobs
User can download latest Hudi minor version or add LakeView SyncTool JAR to classpath. After setting all the properties mentioned above, user can run the sync tool by setting `--sync-tool-classes org.apache.hudi.sync.LakeviewSyncTool` property.
#### Run in Command Line
User can download latest Hudi minor version or add LakeView SyncTool JAR to classpath. LakeView sync tool is pre-packages in Hudi. User can navigate to Hudi LakeView directory `cd hudi-sync/hudi-lakeview-sync/` and execute `run_sync_tool.sh` script with required configurations.

```shell
# To sync tables stored in s3
./run_sync_tool.sh --base-path TABLE_BASE_PATH --version V1 --project-id LAKEVIEW_PROJECT_ID --api-key LAKEVIEW_API_KEY --api-secret LAKEVIEW_API_SECRET --userid LAKEVIEW_USER_ID --s3-region S3_REGION --lake-paths LAKE_NAME.databases.DATABASE_NAME.basePaths=TABLE_BASE_PATHS

# To sync tables stored in gcs
./run_sync_tool.sh --base-path TABLE_BASE_PATH --version V1 --project-id LAKEVIEW_PROJECT_ID --api-key LAKEVIEW_API_KEY --api-secret LAKEVIEW_API_SECRET --userid LAKEVIEW_USER_ID --gcp-project-id GCP_PROJECT_ID --lake-paths LAKE_NAME.databases.DATABASE_NAME.basePaths=TABLE_BASE_PATHS
```

### Composite sync tool with Hive/Glue
In standard Spark jobs, Hudi only supports using one SyncTool (Hudi Streamer supports multiple sync tools). A composite sync tool can be created that wraps the HiveSyncTool/GlueSyncTool so users can sync to both LakeView and Hive/Glue when running a Spark job.


## Rollout/Adoption Plan

 - Users can register their project in [lakeview](https://www.onehouse.ai/product/lakeview) and get their API credentials. Then they can run lakeview sync tool based on the options discussed above (via hudi streamer/spark jobs/command line).

## Test Plan

The sync tool can be triggered via a shell script as well. Once the execution is completed, the metrics can be verified in LakeView portal.