/*
 * Copyright (c) 2014 by Cyoda Pty Ltd.
 *   ABN: 13161204771
 *
 *   This software is the confidential and proprietary information
 *   of Cyoda Pty Ltd. ("Confidential Information").  You are
 *   expressly forbidden to publish share or in anyway disseminate
 *   this information.
 */

package com.cyoda.core.conditions;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum Operation {
    EQUALS("equals"),
    OBJECT_EQUALS("objectEquals"),
    NOT_EQUAL("notEqual"),
    IEQUALS("iEquals"),
    INOT_EQUAL("iNotEqual"),
    GREATER_THAN("greaterThan"),
    LESS_THAN("lessThan"),
    GREATER_OR_EQUAL("greaterOrEqual"),
    LESS_OR_EQUAL("lessOrEqual"),
    CONTAINS("contains"),
    STARTS_WITH("startsWith"),
    ENDS_WITH("endsWith"),
    ICONTAINS("iContains"),
    ISTARTS_WITH("iStartsWith"),
    IENDS_WITH("iEndsWith"),
    NOT_CONTAINS("notContains"),
    NOT_STARTS_WITH("notStartsWith"),
    NOT_ENDS_WITH("notEndsWith"),
    INOT_CONTAINS("iNotContains"),
    INOT_STARTS_WITH("iNotStartsWith"),
    INOT_ENDS_WITH("iNotEndsWith"),
    IBETWEEN_INCLUSIVE("iBetweenInclusive"),
    MATCHES_PATTERN("matchesPattern"),
    IMATCHES_PATTERN("iMatchesPattern"),
    CONTAINS_PATTERN("containsPattern"),
    STARTS_WITH_PATTERN("startsWithPattern"),
    ENDS_WITH_PATTERN("endsWithPattern"),
    ICONTAINS_PATTERN("iContainsPattern"),
    ISTARTS_WITH_PATTERN("iStartsWithPattern"),
    IENDS_WITH_PATTERN("iEndsWithPattern"),
    REGEXP("regexp"),
    IREGEXP("iregexp"),
    IS_NULL("isNull"),
    NOT_NULL("notNull"),
    IN_SET("inSet"),
    NOT_IN_SET("notInSet"),
    EQUALS_FIELD("equalsField"),
    NOT_EQUAL_FIELD("notEqualField"),
    IEQUALS_FIELD("iEqualsField"),
    INOT_EQUAL_FIELD("iNotEqualField"),
    GREATER_THAN_FIELD("greaterThanField"),
    LESS_THAN_FIELD("lessThanField"),
    GREATER_OR_EQUAL_FIELD("greaterOrEqualField"),
    LESS_OR_EQUAL_FIELD("lessOrEqualField"),
    CONTAINS_FIELD("containsField"),
    STARTS_WITH_FIELD("startsWithField"),
    ENDS_WITH_FIELD("endsWithField"),
    ICONTAINS_FIELD("iContainsField"),
    ISTARTS_WITH_FIELD("iStartsWithField"),
    IENDS_WITH_FIELD("iEndsWithField"),
    NOT_CONTAINS_FIELD("notContainsField"),
    NOT_STARTS_WITH_FIELD("notStartsWithField"),
    NOT_ENDS_WITH_FIELD("notEndsWithField"),
    INOT_CONTAINS_FIELD("iNotContainsField"),
    INOT_STARTS_WITH_FIELD("iNotStartsWithField"),
    INOT_ENDS_WITH_FIELD("iNotEndsWithField"),
    AND("and"),
    NOT("not"),
    OR("or"),
    BETWEEN("between"),
    BETWEEN_INCLUSIVE("betweenInclusive"),
    EQUAL_BY_ATTRIBUTES("EqualByAttributes"),
    INSTANCE_OF("InstanceOf"),
    LIKE("like"),
    IS_UNCHANGED("isUnchanged"),
    IS_CHANGED("isChanged");

    private String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static final Map<String, Operation> operations = ImmutableMap.copyOf(
            Arrays.stream(Operation.values()).collect(Collectors.toMap(Operation::getValue, e->e))
    );
}


