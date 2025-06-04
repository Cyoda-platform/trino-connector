package com.cyoda.connector.client.treenode.dto.conditions.expressions;

import com.cyoda.connector.client.treenode.dto.conditions.complex.AbstractConditionDto;
import com.cyoda.connector.handles.CyodaColumnHandle;
import io.trino.spi.expression.ConnectorExpression;
import io.trino.spi.expression.Variable;

import java.util.List;
import java.util.function.Function;

public interface ExpressionConverter {
    AbstractConditionDto convert(List<ConnectorExpression> arguments, Function<Variable, CyodaColumnHandle> fieldResolver);
}
