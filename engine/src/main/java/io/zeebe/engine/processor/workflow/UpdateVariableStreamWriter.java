/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.processor.workflow;

import io.zeebe.engine.processor.TypedStreamWriter;
import io.zeebe.engine.state.instance.VariablesState.VariableListener;
import io.zeebe.protocol.impl.record.value.variable.VariableRecord;
import io.zeebe.protocol.record.intent.VariableIntent;
import io.zeebe.util.buffer.BufferUtil;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;

public final class UpdateVariableStreamWriter implements VariableListener {

  public static final int VALUE_CAPACITY_WARNING_LIMIT = 32766;
  private final VariableRecord record = new VariableRecord();

  private final TypedStreamWriter streamWriter;
  private final Logger log;

  public UpdateVariableStreamWriter(final TypedStreamWriter streamWriter, Logger log) {
    this.streamWriter = streamWriter;
    this.log = log;
  }

  @Override
  public void onCreate(
      final long key,
      final long workflowKey,
      final DirectBuffer name,
      final DirectBuffer value,
      final long variableScopeKey,
      final long rootScopeKey) {
    logLargeValueSize(key, name, value, variableScopeKey, rootScopeKey);

    record
        .setName(name)
        .setValue(value)
        .setScopeKey(variableScopeKey)
        .setWorkflowInstanceKey(rootScopeKey)
        .setWorkflowKey(workflowKey);

    streamWriter.appendFollowUpEvent(key, VariableIntent.CREATED, record);
  }

  @Override
  public void onUpdate(
      final long key,
      final long workflowKey,
      final DirectBuffer name,
      final DirectBuffer value,
      final long variableScopeKey,
      final long rootScopeKey) {
    logLargeValueSize(key, name, value, variableScopeKey, rootScopeKey);
    record
        .setName(name)
        .setValue(value)
        .setScopeKey(variableScopeKey)
        .setWorkflowInstanceKey(rootScopeKey)
        .setWorkflowKey(workflowKey);

    streamWriter.appendFollowUpEvent(key, VariableIntent.UPDATED, record);
  }

  private void logLargeValueSize(
      long key, DirectBuffer name, DirectBuffer value, long variableScopeKey, long rootScopeKey) {
    final int valueSize = value.capacity();
    if (valueSize > VALUE_CAPACITY_WARNING_LIMIT) {
      log.warn(
          "Variable {key: {}, name: {}, variableScope: {}, rootScope: {}} exceeded recommend max size of {} bytes with a size of {} bytes. As a consequence this variable might be ignored by exporters.",
          key,
          BufferUtil.bufferAsString(name),
          variableScopeKey,
          rootScopeKey,
          VALUE_CAPACITY_WARNING_LIMIT,
          valueSize);
    }
  }
}
