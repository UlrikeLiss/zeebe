/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.logstreams.storage.atomix;

import io.atomix.protocols.raft.zeebe.ZeebeEntry;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.atomix.storage.journal.index.SparseJournalIndex;
import io.zeebe.logstreams.impl.Loggers;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.slf4j.Logger;

public final class ZeebeIndexBridge implements JournalIndex, ZeebeIndexMapping {

  public static final Logger LOG = Loggers.LOGSTREAMS_LOGGER;
  private static final int DEFAULT_DENSITY = 50;

  private final ConcurrentNavigableMap<Long, Long> positionIndexMapping =
      new ConcurrentSkipListMap<>();
  private final ConcurrentNavigableMap<Long, Long> indexPositionMapping =
      new ConcurrentSkipListMap<>();
  // atomix positions
  private final int density;
  private final SparseJournalIndex sparseJournalIndex;

  private ZeebeIndexBridge(int density) {
    this.density = density;

    // sparse journal accepts only floating density - we should change that
    // internally it calculates the density
    // this.x = (int) Math.ceil(MIN_DENSITY / (y * MIN_DENSITY));
    // if we want to be x = ourDensity, then y needs to be 1 / ourDensity
    sparseJournalIndex = new SparseJournalIndex(1f / density);
  }

  public static ZeebeIndexBridge ofDensity(int density) {
    return new ZeebeIndexBridge(density);
  }

  public static ZeebeIndexBridge ofDefaultDensity() {
    return new ZeebeIndexBridge(DEFAULT_DENSITY);
  }

  @Override
  public void index(final Indexed indexedEntry, final int position) {
    final var index = indexedEntry.index();
    if (index % density == 0) {
      LOG.error("Add index {}", index);
      if (indexedEntry.type() == ZeebeEntry.class) {
        final ZeebeEntry zeebeEntry = (ZeebeEntry) indexedEntry.entry();
        final var lowestPosition = zeebeEntry.lowestPosition();
        LOG.error("Add index {} for zeebe entry with low pos {}", index, lowestPosition);
        positionIndexMapping.put(lowestPosition, index);
        indexPositionMapping.put(index, lowestPosition);
      }
    } else {
      LOG.debug("INDEX {} does not reach density {}", index, DEFAULT_DENSITY);
    }

    sparseJournalIndex.index(indexedEntry, position);
  }

  @Override
  public long lookupPosition(final long position) {
    final long startTime = System.currentTimeMillis();

    var index = positionIndexMapping.getOrDefault(position, -1L);

    if (index == -1) {
      final var lowerEntry = positionIndexMapping.lowerEntry(position);
      if (lowerEntry != null) {
        index = lowerEntry.getValue();
      }
    }

    final long endTime = System.currentTimeMillis();
    io.zeebe.logstreams.impl.Loggers.LOGSTREAMS_LOGGER.info(
        "Finding position {} in map took: {} ms ", position, endTime - startTime);

    return index;
  }

  @Override
  public Position lookup(final long index) {
    return sparseJournalIndex.lookup(index);
  }

  @Override
  public void truncate(final long index) {
    //    LOG.error(
    //        "Truncate index for given idx {}, map {}", index, indexPositionMapping, new
    // Exception());
    final var lowerEntry = indexPositionMapping.lowerEntry(index);

    if (lowerEntry != null) {

      final var lowerIndex = lowerEntry.getKey();
      final var lowerPosition = lowerEntry.getValue();

      indexPositionMapping.tailMap(lowerIndex).clear();
      positionIndexMapping.tailMap(lowerPosition).clear();
    }

    sparseJournalIndex.truncate(index);
  }

  @Override
  public void compact(long index) {

    //    LOG.error(
    //        "Compact index for given idx {}, map {}", index, indexPositionMapping, new
    // Exception());
    final var lowerEntry = indexPositionMapping.lowerEntry(index);

    if (lowerEntry != null) {

      final var lowerIndex = lowerEntry.getKey();
      final var lowerPosition = lowerEntry.getValue();

      indexPositionMapping.headMap(lowerIndex).clear();
      positionIndexMapping.headMap(lowerPosition).clear();
    }

    sparseJournalIndex.compact(index);
  }
}
