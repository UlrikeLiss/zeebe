package org.camunda.tngp.broker.wf.runtime;

import org.camunda.tngp.bpmn.graph.ProcessGraph;
import org.camunda.tngp.log.Log;

public interface BpmnFlowElementEventHandler
{

    void handle(BpmnFlowElementEventReader flowElementEventReader, ProcessGraph process, Log log);

}
