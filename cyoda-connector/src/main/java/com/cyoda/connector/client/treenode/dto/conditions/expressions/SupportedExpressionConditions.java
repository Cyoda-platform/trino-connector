package com.cyoda.connector.client.treenode.dto.conditions.expressions;

import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.cyoda.connector.client.treenode.dto.conditions.complex.AbstractConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.complex.GroupConditionDto;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.expression.Call;
import io.trino.spi.expression.ConnectorExpression;
import io.trino.spi.expression.FunctionName;
import io.trino.spi.expression.StandardFunctions;
import io.trino.spi.expression.Variable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SupportedExpressionConditions {
    //supporting only queryable conditions
    EQUALS(StandardFunctions.EQUAL_OPERATOR_FUNCTION_NAME, new SingleExpressionConverter(Operation.EQUALS)),
    LESS_THAN(StandardFunctions.LESS_THAN_OPERATOR_FUNCTION_NAME, new SingleExpressionConverter(Operation.LESS_THAN)),
    LESS_OR_EQUALS(StandardFunctions.LESS_THAN_OR_EQUAL_OPERATOR_FUNCTION_NAME, new SingleExpressionConverter(Operation.LESS_OR_EQUAL)),
    GREATER_THAN(StandardFunctions.GREATER_THAN_OPERATOR_FUNCTION_NAME, new SingleExpressionConverter(Operation.GREATER_THAN)),
    GREATER_OR_EQUALS(StandardFunctions.GREATER_THAN_OR_EQUAL_OPERATOR_FUNCTION_NAME, new SingleExpressionConverter(Operation.GREATER_OR_EQUAL)),

    AND(StandardFunctions.AND_FUNCTION_NAME, new GroupExpressionConverter(GroupConditionDto.Operator.AND)),
    OR(StandardFunctions.OR_FUNCTION_NAME, new GroupExpressionConverter(GroupConditionDto.Operator.OR)),
    NOT(StandardFunctions.NOT_FUNCTION_NAME, new GroupExpressionConverter(GroupConditionDto.Operator.NOT));

    private static final SupplierLogger LOG = SupplierLogger.get(SupportedExpressionConditions.class);

    private final FunctionName trinoOperation;
    private final ExpressionConverter expressionConverter;

    private static final Map<FunctionName, ExpressionConverter> expressionConverterMap =
            Stream.of(SupportedExpressionConditions.values())
                    .map(condition -> Map.entry(condition.trinoOperation, condition.expressionConverter))
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    SupportedExpressionConditions(FunctionName trinoOperation, ExpressionConverter expressionConverter){
        this.trinoOperation = trinoOperation;
        this.expressionConverter = expressionConverter;
    }

    public static AbstractConditionDto convert(ConnectorExpression expression, Function<Variable, CyodaColumnHandle> fieldResolver){
        if (expression instanceof Call call) {
            ExpressionConverter converter = expressionConverterMap.get(call.getFunctionName());
            if (converter != null) {
                return converter.convert(call.getArguments(), fieldResolver);
            } else {
                LOG.info("Unsupported expression type " + call.getFunctionName());
                return null;
            }
        }
        throw new IllegalArgumentException("Unsupported expression type for top/group level resolver: " + expression);
    }

    public static AbstractConditionDto convertFailsafe(ConnectorExpression expression, Map<String, ColumnHandle> assignments){
        try {
            return convert(expression, new FieldResolver(assignments).getResolverFunction());
        } catch (Exception e) {
            LOG.warn("FAILED to resolve expression " + expression + "\nGIVING UP ON PUSHDOWN.\nReason: " + e.getMessage(), e);
            return null;
        }
    }

    private static class FieldResolver {
        private final Map<String, ColumnHandle> assignments;

        private FieldResolver(Map<String, ColumnHandle> assignments) {
            this.assignments = assignments;
        }

        public CyodaColumnHandle resolve(String fieldKey){
            ColumnHandle columnHandle = assignments.get(fieldKey);
            if (columnHandle instanceof CyodaColumnHandle cyodaColumnHandle) {
                return cyodaColumnHandle;
            } else {
                throw new IllegalArgumentException("Unsupported column type for field key: " + fieldKey + " :: " + columnHandle.getClass());
            }
        }
        public Function<Variable, CyodaColumnHandle> getResolverFunction(){
            return variable -> {
                String fieldKey = variable.getName();
                return resolve(fieldKey);
            };
        }
    }


}
