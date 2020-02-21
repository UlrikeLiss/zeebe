/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.processor.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.zeebe.engine.processor.TypedStreamWriter;
import io.zeebe.util.buffer.BufferUtil;
import java.util.ArrayList;
import java.util.List;
import org.agrona.DirectBuffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;

public class UpdateVariableStreamWriterTest {

  private static final Long VARIABLE_KEY = 23L;
  private static final Long VARIABLE_SCOPE_KEY = 45L;
  private static final Long VARIABLE_ROOT_SCOPE_KEY = 67L;
  private static final String VARIABLE_NAME = "myVariableName";
  private static final DirectBuffer VARIABLE_NAME_BUFFER = BufferUtil.wrapString(VARIABLE_NAME);
  private static final String VARIABLE_VALUE = "smallValue";
  private static final DirectBuffer VARIABLE_VALUE_BUFFER = BufferUtil.wrapString(VARIABLE_VALUE);
  private static final String VARIABLE_VALUE_LARGE =
      "x".repeat(UpdateVariableStreamWriter.VALUE_CAPACITY_WARNING_LIMIT + 1);
  private static final DirectBuffer VARIABLE_VALUE_LARGE_BUFFER =
      BufferUtil.wrapString(VARIABLE_VALUE_LARGE);

  private Logger logMock;
  private UpdateVariableStreamWriter updateVariableStreamWriter;

  @Before
  public void setUp() {
    logMock = mock(Logger.class);
    updateVariableStreamWriter =
        new UpdateVariableStreamWriter(mock(TypedStreamWriter.class), logMock);
  }

  @Test
  public void shouldNotLogWarningForSmallVariableValueOnCreate() {
    // when
    updateVariableStreamWriter.onCreate(
        VARIABLE_KEY,
        1,
        VARIABLE_NAME_BUFFER,
        VARIABLE_VALUE_BUFFER,
        VARIABLE_SCOPE_KEY,
        VARIABLE_ROOT_SCOPE_KEY);

    // then
    verify(logMock, never()).warn(anyString(), ArgumentMatchers.<Object[]>any());
  }

  @Test
  public void shouldLogWarningForLargeVariableValueOnCreate() {
    // when
    updateVariableStreamWriter.onCreate(
        VARIABLE_KEY,
        1,
        VARIABLE_NAME_BUFFER,
        VARIABLE_VALUE_LARGE_BUFFER,
        VARIABLE_SCOPE_KEY,
        VARIABLE_ROOT_SCOPE_KEY);

    // then
    assertLogArgs();
  }

  @Test
  public void shouldNotLogWarningForSmallVariableValueOnUpdate() {
    // when
    updateVariableStreamWriter.onUpdate(
        VARIABLE_KEY,
        1,
        VARIABLE_NAME_BUFFER,
        VARIABLE_VALUE_BUFFER,
        VARIABLE_SCOPE_KEY,
        VARIABLE_ROOT_SCOPE_KEY);

    // then
    verify(logMock, never()).warn(anyString(), ArgumentMatchers.<Object[]>any());
  }

  @Test
  public void shouldLogWarningForLargeVariableValueOnUpdate() {
    // when
    updateVariableStreamWriter.onUpdate(
        VARIABLE_KEY,
        1,
        VARIABLE_NAME_BUFFER,
        VARIABLE_VALUE_LARGE_BUFFER,
        VARIABLE_SCOPE_KEY,
        VARIABLE_ROOT_SCOPE_KEY);

    // then
    assertLogArgs();
  }

  private void assertLogArgs() {
    final ArgumentCaptor<Object[]> argumentCaptor = ArgumentCaptor.forClass(Object[].class);

    verify(logMock).warn(anyString(), argumentCaptor.capture());

    // required to explicitly cast List<Objects[]> to List<Object>
    final List<Object> args = new ArrayList<>(argumentCaptor.getAllValues());
    assertThat(args)
        .contains(
            VARIABLE_KEY,
            VARIABLE_NAME,
            VARIABLE_VALUE_LARGE_BUFFER.capacity(),
            VARIABLE_ROOT_SCOPE_KEY,
            VARIABLE_SCOPE_KEY);
  }
}
