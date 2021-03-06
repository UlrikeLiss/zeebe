/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.util.sched.functional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.zeebe.util.sched.Actor;
import io.zeebe.util.sched.ScheduledTimer;
import io.zeebe.util.sched.testing.ControlledActorSchedulerRule;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;

public final class TimedActionsTest {
  @Rule
  public final ControlledActorSchedulerRule schedulerRule = new ControlledActorSchedulerRule();

  @Test
  public void shouldNotRunActionIfDeadlineNotReached() throws InterruptedException {
    // given
    final Runnable action = mock(Runnable.class);
    final Actor actor =
        new Actor() {
          @Override
          protected void onActorStarted() {
            actor.runDelayed(Duration.ofMillis(10), action);
          }
        };

    // when
    schedulerRule.getClock().setCurrentTime(100);
    schedulerRule.submitActor(actor);
    schedulerRule.workUntilDone();

    // then
    verify(action, never()).run();
  }

  @Test
  public void shouldRunActionWhenDeadlineReached() throws InterruptedException {
    // given
    final Runnable action = mock(Runnable.class);
    final Actor actor =
        new Actor() {
          @Override
          protected void onActorStarted() {
            actor.runDelayed(Duration.ofMillis(10), action);
          }
        };

    // when
    schedulerRule.getClock().setCurrentTime(100);
    schedulerRule.submitActor(actor);
    schedulerRule.workUntilDone();
    schedulerRule.getClock().addTime(Duration.ofMillis(10));
    schedulerRule.workUntilDone();

    // then
    verify(action, times(1)).run();
  }

  @Test
  public void shouldRunAtFixedRate() throws InterruptedException {
    // given
    final Runnable action = mock(Runnable.class);
    final Actor actor =
        new Actor() {
          @Override
          protected void onActorStarted() {
            actor.runAtFixedRate(Duration.ofMillis(10), action);
          }
        };

    // when then
    schedulerRule.getClock().setCurrentTime(100);
    schedulerRule.submitActor(actor);
    schedulerRule.workUntilDone();

    schedulerRule.getClock().addTime(Duration.ofMillis(10));
    schedulerRule.workUntilDone();
    verify(action, times(1)).run();

    schedulerRule.getClock().addTime(Duration.ofMillis(10));
    schedulerRule.workUntilDone();
    verify(action, times(2)).run();

    schedulerRule.getClock().addTime(Duration.ofMillis(10));
    schedulerRule.workUntilDone();
    verify(action, times(3)).run();
  }

  @Test
  public void shouldCancelRunDelayed() {
    // given
    final Runnable action = mock(Runnable.class);
    final AtomicReference<ScheduledTimer> timer = new AtomicReference<>();
    final Actor actor =
        new Actor() {
          @Override
          protected void onActorStarted() {
            timer.set(actor.runDelayed(Duration.ofMillis(10), action));
          }
        };

    // when
    schedulerRule.getClock().setCurrentTime(100);
    schedulerRule.submitActor(actor);
    schedulerRule.workUntilDone();
    timer.get().cancel();
    schedulerRule.getClock().addTime(Duration.ofMillis(10));
    schedulerRule.workUntilDone();

    // then
    verify(action, times(0)).run();
  }

  @Test
  public void shouldCancelRunDelayedAfterExecution() {
    // given
    final Runnable action = mock(Runnable.class);
    final AtomicReference<ScheduledTimer> timer = new AtomicReference<>();
    final Actor actor =
        new Actor() {
          @Override
          protected void onActorStarted() {
            timer.set(actor.runDelayed(Duration.ofMillis(10), action));
          }
        };

    // when
    schedulerRule.getClock().setCurrentTime(100);
    schedulerRule.submitActor(actor);
    schedulerRule.workUntilDone();

    // make timer run
    schedulerRule.getClock().addTime(Duration.ofMillis(10));
    schedulerRule.workUntilDone();

    // when
    timer.get().cancel();

    // then
    // no exception has been thrown
  }

  @Test
  public void shouldCancelRunAtFixedRate() {
    // given
    final Runnable action = mock(Runnable.class);
    final AtomicReference<ScheduledTimer> timer = new AtomicReference<>();
    final Actor actor =
        new Actor() {
          @Override
          protected void onActorStarted() {
            timer.set(actor.runAtFixedRate(Duration.ofMillis(10), action));
          }
        };

    // when
    schedulerRule.getClock().setCurrentTime(100);
    schedulerRule.submitActor(actor);
    schedulerRule.workUntilDone();
    timer.get().cancel();
    schedulerRule.getClock().addTime(Duration.ofMillis(10));
    schedulerRule.workUntilDone();

    // then
    verify(action, times(0)).run();
  }
}
