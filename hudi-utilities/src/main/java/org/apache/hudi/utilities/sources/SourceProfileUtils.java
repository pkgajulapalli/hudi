/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.utilities.sources;

import org.apache.hudi.common.config.TypedProperties;
import org.apache.hudi.common.util.Option;
import org.apache.hudi.utilities.config.KafkaSourceConfig;
import org.apache.hudi.utilities.ingestion.HoodieIngestionMetrics;
import org.apache.hudi.utilities.sources.helpers.KafkaOffsetGen;
import org.apache.hudi.utilities.streamer.SourceProfile;
import org.apache.hudi.utilities.streamer.SourceProfileSupplier;
import org.apache.spark.streaming.kafka010.OffsetRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.apache.hudi.common.util.ConfigUtils.getLongWithAltKeys;

public class SourceProfileUtils {

  private static final Logger LOG = LoggerFactory.getLogger(SourceProfileUtils.class);

  private SourceProfileUtils() {
  }

  @SuppressWarnings("unchecked")
  public static OffsetRange[] getOffsetRanges(TypedProperties props,
                                              Option<SourceProfileSupplier> sourceProfileSupplier,
                                              KafkaOffsetGen offsetGen,
                                              HoodieIngestionMetrics metrics,
                                              Option<String> lastCheckpointStr,
                                              long sourceLimit) {
    OffsetRange[] offsetRanges;
    if (sourceProfileSupplier.isPresent() && sourceProfileSupplier.get().getSourceProfile() != null) {
      SourceProfile<Long> kafkaSourceProfile = sourceProfileSupplier.get().getSourceProfile();
      offsetRanges = offsetGen.getNextOffsetRanges(lastCheckpointStr, kafkaSourceProfile.getSourceSpecificContext(),
          kafkaSourceProfile.getSourcePartitions(), metrics);
      metrics.updateStreamerSourceParallelism(kafkaSourceProfile.getSourcePartitions());
      metrics.updateStreamerSourceBytesToBeIngestedInSyncRound(kafkaSourceProfile.getMaxSourceBytes());
      LOG.info("About to read numEvents {} of size {} bytes in {} partitions from Kafka for topic {} with offsetRanges {}",
          kafkaSourceProfile.getSourceSpecificContext(), kafkaSourceProfile.getMaxSourceBytes(),
          kafkaSourceProfile.getSourcePartitions(), offsetGen.getTopicName(), offsetRanges);
    } else {
      int minPartitions = (int) getLongWithAltKeys(props, KafkaSourceConfig.KAFKA_SOURCE_MIN_PARTITIONS);
      metrics.updateStreamerSourceParallelism(minPartitions);
      offsetRanges = offsetGen.getNextOffsetRanges(lastCheckpointStr, sourceLimit, metrics);
      LOG.info("About to read numEvents {} in {} spark partitions from kafka for topic {} with offset ranges {}",
          sourceLimit, minPartitions, offsetGen.getTopicName(),
          Arrays.toString(offsetRanges));
    }
    return offsetRanges;
  }
}
