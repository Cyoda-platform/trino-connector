package com.cyoda.connector.client.treenode.dto.conditions.expressions;

import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.cyoda.connector.client.treenode.dto.conditions.complex.AbstractConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.complex.GroupConditionDto;
import com.cyoda.connector.handles.CyodaColumnHandle;
import io.trino.spi.expression.ConnectorExpression;
import io.trino.spi.expression.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GroupExpressionConverter implements ExpressionConverter {

    private final GroupConditionDto.Operator operation;

    public GroupExpressionConverter(GroupConditionDto.Operator operation) {
        this.operation = operation;
    }

    @Override
    public AbstractConditionDto convert(List<ConnectorExpression> arguments, Function<Variable, CyodaColumnHandle> fieldResolver) {
        List<AbstractConditionDto> children = new ArrayList<>();

        for (ConnectorExpression child : arguments) {
            AbstractConditionDto converted = SupportedExpressionConditions.convert(child, fieldResolver);
            if (converted == null) {
                return null;
            }
            children.add(converted);
        }

        return new GroupConditionDto(operation, children);
    }
}
