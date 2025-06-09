package com.cyoda.connector.client.treenode.dto.conditions.expressions;

import com.cyoda.connector.client.treenode.dto.conditions.AbstractTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.cyoda.connector.client.treenode.dto.conditions.SimpleTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.complex.AbstractConditionDto;
import com.cyoda.connector.handles.CyodaColumnHandle;
import io.trino.spi.expression.ConnectorExpression;
import io.trino.spi.expression.Constant;
import io.trino.spi.expression.Variable;

import java.util.List;
import java.util.function.Function;

public class SingleExpressionConverter implements ExpressionConverter {
    private final Operation operation;

    public SingleExpressionConverter(Operation operation) {
        this.operation = operation;
    }

    @Override
    public AbstractConditionDto convert(List<ConnectorExpression> arguments, Function<Variable, CyodaColumnHandle> fieldResolver) {
        if (arguments.get(0) instanceof Variable variable) {
            CyodaColumnHandle columnHandle = fieldResolver.apply(variable);
            if (arguments.get(1) instanceof Constant constant) {
                AbstractTrinoConditionDto trinoCondition = columnHandle.getConverter().toCondition(operation, columnHandle.getColumnType(), constant.getValue());
                return trinoCondition.toComplexCondition(columnHandle.getColumnCategory(), columnHandle.getColumnKey());
            } else throw new IllegalArgumentException("For condition expression, second argument must be a column reference. Actual type: " + arguments.get(1).getClass());
        } else throw new IllegalArgumentException("For condition expression, first argument must be a column reference. Actual type: " + arguments.get(0).getClass());
    }
}
