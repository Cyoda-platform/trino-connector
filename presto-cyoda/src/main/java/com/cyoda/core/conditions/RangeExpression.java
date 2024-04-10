package com.cyoda.core.conditions;

/**
 * @author Alexey Shurygin
 */
public class RangeExpression {
    public enum Operator {
        LESS,
        LESS_THAN_EQUALS,
        EQUAL,
        GREATER_THAN_EQUALS,
        GREATER;

        public boolean isLess() {
            return this == LESS || this == LESS_THAN_EQUALS;
        }

        public boolean isGreater() {
            return this == GREATER || this == GREATER_THAN_EQUALS;
        }

        public boolean isEquals() {
            return this == EQUAL || this == LESS_THAN_EQUALS || this == GREATER_THAN_EQUALS;
        }
    }

    private String fieldName;
    private Comparable value;
    private Operator operator;

    //TODO Implement builder.
    public RangeExpression(final Comparable value, final Operator operator) {
        //TODO Refactor RangeExpression to exclude field name.
        this("fakeField", value, operator);
    }

    //TODO Implement builder.
    public RangeExpression(final String fieldName, final Comparable value, final Operator operator) {
        //TODO Refactor RangeExpression to exclude field name.
        this.fieldName = fieldName;
        this.value = value instanceof Integer ? ((Integer) value).longValue() : value;
        this.operator = operator;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Comparable getValue() {
        return value;
    }

    public void setValue(Comparable value) {
        this.value = value;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return "RangeExpression{" +
                "fieldName='" + fieldName + '\'' +
                ", operator=" + operator +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RangeExpression that = (RangeExpression) o;

        if (!fieldName.equals(that.fieldName)) return false;
        if (operator != that.operator) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fieldName.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + operator.hashCode();
        return result;
    }
}
