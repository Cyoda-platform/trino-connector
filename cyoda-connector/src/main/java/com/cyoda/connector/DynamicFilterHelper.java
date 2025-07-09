package com.cyoda.connector;

import com.cyoda.connector.client.reporting.stats.ConditionPushdownLogMonitor;
import com.cyoda.connector.handles.CyodaTableHandle;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.predicate.TupleDomain;

public class DynamicFilterHelper {
    private static final long DF_WAITING_STEP_MS = 100;
    private static final int DF_JSON_WAITING_STEPS = 100;
    private static final int DF_OTHER_WAITING_STEPS = 40;
    private static final int DF_ALREADY_HAS_CONDITION_STEPS_SUBTRACT = 20;

    static void dynamicFilterPushdown(ConditionPushdownLogMonitor pushdownLogMonitor,
                                      String queryId,
                                      DynamicFilter dynamicFilter,
                                      CyodaTableHandle handle,
                                      CyodaConfig.DynamicFilterWaitStage settingsDFWS,
                                      CyodaConfig.DynamicFilterWaitStage currentDFWS) {
        if (handle.getTableType().isPushdownSupported()){
            String codePoint = currentDFWS.name();
            if ((settingsDFWS == CyodaConfig.DynamicFilterWaitStage.ALL || settingsDFWS == currentDFWS) &&
                    dynamicFilter.isAwaitable()) {
                int cnt = 0;
                int maxWaitingSteps = handle.getTableMetaId().endsWith("json") ? DF_JSON_WAITING_STEPS : DF_OTHER_WAITING_STEPS;
                if ((handle.getCondition() == null || handle.getCondition().isAll()) &&
                        handle.getConstraint().isAll()){
                    maxWaitingSteps -= DF_ALREADY_HAS_CONDITION_STEPS_SUBTRACT;
                }
                while (!dynamicFilter.isComplete() && cnt++ < maxWaitingSteps) try {
                    Thread.sleep(DF_WAITING_STEP_MS);
                } catch (InterruptedException e){
                    throw new RuntimeException(e);
                }
                setConstraintToHandle(pushdownLogMonitor, queryId, dynamicFilter, handle, cnt, codePoint);
            } else {
                setConstraintToHandle(pushdownLogMonitor, queryId, dynamicFilter, handle, -1, codePoint);
            }
        }
    }

    private static void setConstraintToHandle(ConditionPushdownLogMonitor pushdownLogMonitor, String queryId, DynamicFilter dynamicFilter, CyodaTableHandle handle, int stepsWaited, String codePoint) {
        TupleDomain<ColumnHandle> currentPredicate = dynamicFilter.getCurrentPredicate();

        handle.setConstraint(handle.getConstraint().intersect(currentPredicate));
        pushdownLogMonitor.registerPushdown(queryId, handle.toSchemaTableName().toString(), codePoint, currentPredicate.toString(), null, handle.getConstraint().toString(), String.valueOf(stepsWaited));
    }
}
