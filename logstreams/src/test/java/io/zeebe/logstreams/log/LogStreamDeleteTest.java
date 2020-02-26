/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.logstreams.log;

import static io.zeebe.logstreams.log.LogStreamTest.writeEvent;
import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.logstreams.util.LogStreamRule;
import io.zeebe.logstreams.util.SynchronousLogStream;
import io.zeebe.util.buffer.BufferUtil;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

public final class LogStreamDeleteTest {

  private static final int EVENT_COUNT = 100;

  private final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final LogStreamRule logStreamRule =
      LogStreamRule.createRuleWithoutStarting(temporaryFolder);

  @Rule public RuleChain ruleChain = RuleChain.outerRule(temporaryFolder).around(logStreamRule);
  private ArrayList<Long> positions;

  @Before
  public void setUp() {
    final int segmentSize = 1024;
    final int entrySize = Math.floorDiv(segmentSize, 2) + 1;

    final SynchronousLogStream logStream =
        logStreamRule.startLogStreamWithStorageConfiguration(
            b -> b.withMaxSegmentSize(segmentSize).withMaxEntrySize(entrySize));

    // remove some bytes for padding per entry
    final byte[] largeEvent = new byte[entrySize - 90];

    positions = new ArrayList<>();
    for (int i = 0; i < EVENT_COUNT; i++) {
      positions.add(writeEvent(logStream, BufferUtil.wrapArray(largeEvent)));
    }
  }

  @Test
  public void shouldDeleteFromLogStream() {
    // given
    final SynchronousLogStream logStream = logStreamRule.getLogStream();

    // when
    logStream.delete(positions.get(EVENT_COUNT / 2));

    // then
    assertThat(events().count()).isLessThan(EVENT_COUNT);
    assertThat(events().anyMatch(e -> e.getPosition() == EVENT_COUNT / 2)).isTrue();
  }

  @Test
  public void shouldNotDeleteOnNegativePosition() {
    // given
    final SynchronousLogStream logStream = logStreamRule.getLogStream();

    // when
    logStream.delete(-1);

    // then
    assertThat(events().count()).isEqualTo(EVENT_COUNT);
  }

  private Stream<LoggedEvent> events() {
    final LogStreamReader reader = logStreamRule.getLogStreamReader();
    reader.seekToFirstEvent();
    final Iterable<LoggedEvent> iterable = () -> reader;
    return StreamSupport.stream(iterable.spliterator(), false);
  }
}
