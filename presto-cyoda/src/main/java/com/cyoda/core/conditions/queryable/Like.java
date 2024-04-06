package com.cyoda.core.conditions.queryable;

import com.cyoda.core.conditions.Operation;
import com.cyoda.core.conditions.RangeCondition;
import com.cyoda.core.conditions.RangeExpression;
import com.cyoda.core.conditions.SimpleCondition;
import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Condition can be used as range, only for equals and start with check.
 * To check that field starts with specified value the following pattern must be used:
 * 'value%'. All fields that starts with 'value' will be returned;
 * For value 'value' only equal fields will be returned.
 * Other usages of '%' and '_' in range condition are illegal, IllegalArgumentException can be thrown.
 * For non range fields full 'like' search is possible: '%value%', '%va_ue%'.
 * For such cases like will always used as non range condition.
 * No other special symbols will be checked.
 * For both cases for this pattern '.*blabla[f|g]' only fields with exactly same value ('.*blabla[f|g]') will be returned.
 * To search field values that contain '%', '\' or '_' this symbols have to be escaped - '\%', '\\' and '\_'
 */
@BeanDefinition(hierarchy = "immutable")
public class Like extends RangeCondition<Object, String> {
    private static final String REGEXP_SPECIAL_CHARS = "[](){}.*+?$^|#<>-=";
    private static final String LIKE_SPECIAL_CHARS = "%_\\";

    @PropertyDefinition
    private final String regexp;
    @PropertyDefinition
    private final boolean rangePossible;
    @PropertyDefinition
    private final String value;
    @PropertyDefinition
    private final RangeCondition rangeCondition;
    @PropertyDefinition
    private final Operation rangeOperation;

    public Like(String fieldName, String value, boolean rangeField) {
        super(fieldName, Operation.LIKE, rangeField);
        if (value == null) {
            this.rangePossible = true;
            this.rangeOperation = Operation.EQUALS;
            this.value = null;
            this.regexp = null;
            this.rangeCondition = new Equals(fieldName, null, rangeField);
        } else {
            this.rangePossible = calcRangePossible(value);
            if (rangeField && !this.rangePossible) {
                throw new IllegalArgumentException("Range condition Like is not possible for String " + value);
            }
            this.regexp = prepareSpecialCharacters(value);
            this.rangeOperation = calcOperation(value);
            this.value = value;
            String noEscapeString = removeEscapeCharacters(value);
            if (Operation.STARTS_WITH == rangeOperation) {
                rangeCondition = new StartsWith(fieldName, noEscapeString.substring(0, noEscapeString.length() - 1), rangeField);
            } else {
                rangeCondition = new Equals(fieldName, noEscapeString, rangeField);
            }
        }
    }

    @NotNull
    private static Operation calcOperation(@NotNull String value) {
        return hasLikeSuffix(value) ? Operation.STARTS_WITH : Operation.EQUALS;
    }

    @ImmutableDefaults
    private static void applyDefaults(Builder builder) {
        builder.operation(Operation.LIKE);
    }

    @ImmutablePreBuild
    private static void preBuild(Builder builder) {
        String value = builder.value;
        AtomicBoolean rangeField = (AtomicBoolean) builder.get("rangeField");
        String noEscapeString;
        Operation rangeOperation;
        if (value != null) {
            rangeOperation = calcOperation(value);
            builder.rangePossible(calcRangePossible(value));
            if (rangeField.get() && !builder.rangePossible) {
                throw new IllegalArgumentException("Range condition Like is not possible for String " + builder.value);
            }
            noEscapeString = removeEscapeCharacters(value);
            builder.regexp(prepareSpecialCharacters(value));
        } else {
            builder.rangePossible(true);
            noEscapeString = null;
            rangeOperation = Operation.EQUALS;
        }
        builder.rangeOperation(rangeOperation);
        if (Operation.EQUALS == rangeOperation) {
            builder.rangeCondition(Equals.builder()
                    .set("fieldName", builder.get("fieldName"))
                    .set("value", noEscapeString)
                    .set("rangeField", rangeField)
                    .build());
        } else {
            builder.rangeCondition(StartsWith.builder()
                    .set("fieldName", builder.get("fieldName"))
                    .set("value", noEscapeString.substring(0, builder.value.length() - 1))
                    .set("rangeField", rangeField)
                    .build());
        }

    }

    @Override
    public SimpleCondition setRangeField(boolean rangeField) {
        if (rangeField && !this.rangePossible) {
            throw new IllegalArgumentException("Range condition Like is not possible for String " + value);
        }
        return super.setRangeField(rangeField);
    }

    /**
     * Returns true if a specified string can be used in a range index search.
     * Only the following pattern can be used:
     * 'value%'. All fields that starts with 'value' will be returned;
     * or 'value' - equals search will be used.
     *
     * @param value string to check
     * @return true if value can be used in a range search
     */
    private static boolean calcRangePossible(@NotNull String value) {
        int len = value.length();
        if (len == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '_':
                    if (!hasEscapeCharacter(value, i)) {
                        return false;
                    }
                    break;
                case '%':
                    if (!hasEscapeCharacter(value, i) && i < len - 1) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    @Override
    public SimpleCondition copyWithNewFieldName(String fieldName) {
        return new Like(fieldName, this.value, this.isRangeField());

    }

    /**
     * Prepare String for compiling as a Pattern.
     * Escape all characters that are special in regexp pattern,
     * Remove escaping for _ and %.
     *
     * @param s string to modify
     * @return modified string
     */
    private static String prepareSpecialCharacters(@NotNull String s) {
        if (s.isEmpty()) {
            return s;
        }
        int len = s.length();
        StringBuilder sb = new StringBuilder(len * 3);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (REGEXP_SPECIAL_CHARS.indexOf(c) != -1) {
                sb.append("\\");
                sb.append(c);
            } else if (c == '_') {
                if (!hasEscapeCharacter(s, i)) {
                    sb.append('.');
                } else {
                    sb.deleteCharAt(sb.length() - 1);
                    sb.append(c);
                }
            } else if (c == '%') {
                if (!hasEscapeCharacter(s, i)) {
                    sb.append(".*?");
                } else {
                    sb.deleteCharAt(sb.length() - 1);
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Remove \ before special characters: %, _ or \
     * so that this string can work correct in Equals or StartWith range conditions
     * if searching string contains characters  %, _ or \ .
     *
     * @param s string to modify
     * @return string without escaping
     */
    private static String removeEscapeCharacters(@NotNull String s) {
        if (s.isEmpty()) {
            return s;
        }
        int len = s.length();
        StringBuilder sb = new StringBuilder(len * 3);
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c == '\\') {
                if (i >= len - 1 || LIKE_SPECIAL_CHARS.indexOf(s.charAt(i + 1)) == -1) {
                    throw new IllegalArgumentException("Illegal usage of \\ - it must be escaped or escape other special character.");
                }
                char escapedCharacter = s.charAt(i + 1);
                sb.append(escapedCharacter);
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static boolean hasLikeSuffix(String value) {
        return !value.isEmpty() && value.endsWith("%") && !value.endsWith("\\%");
    }

    private static boolean hasEscapeCharacter(String str, int specCharIndex) {
        if (specCharIndex == 0) {
            return false;
        }
        int counter = 0;
        for (int i = specCharIndex - 1; i >= 0; i--) {
            if ('\\' == str.charAt(i)) {
                counter++;
            } else {
                break;
            }
        }
        return counter % 2 == 1;
    }


    @Override
    public Collection<RangeExpression> generateRangeExpressions() {
        return rangeCondition.generateRangeExpressions();
    }

    @Override
    protected void toString(StringBuilder buf) {
        super.toString(buf);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected String getOperationSign() {
        return ">=";
    }

    //------------------------- AUTOGENERATED START -------------------------
    /**
     * The meta-bean for {@code Like}.
     * @return the meta-bean, not null
     */
    public static Like.Meta meta() {
        return Like.Meta.INSTANCE;
    }

    static {
        MetaBean.register(Like.Meta.INSTANCE);
    }

    /**
     * Returns a builder used to create an instance of the bean.
     * @return the builder, not null
     */
    public static Like.Builder builder() {
        return new Like.Builder();
    }

    /**
     * Restricted constructor.
     * @param builder  the builder to copy from, not null
     */
    protected Like(Like.Builder builder) {
        super(builder);
        this.regexp = builder.regexp;
        this.rangePossible = builder.rangePossible;
        this.value = builder.value;
        this.rangeCondition = builder.rangeCondition;
        this.rangeOperation = builder.rangeOperation;
    }

    @Override
    public Like.Meta metaBean() {
        return Like.Meta.INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the regexp.
     * @return the value of the property
     */
    public String getRegexp() {
        return regexp;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the rangePossible.
     * @return the value of the property
     */
    public boolean isRangePossible() {
        return rangePossible;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the value.
     * @return the value of the property
     */
    public String getValue() {
        return value;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the rangeCondition.
     * @return the value of the property
     */
    public RangeCondition getRangeCondition() {
        return rangeCondition;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the rangeOperation.
     * @return the value of the property
     */
    public Operation getRangeOperation() {
        return rangeOperation;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a builder that allows this bean to be mutated.
     * @return the mutable builder, not null
     */
    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == this.getClass()) {
            Like other = (Like) obj;
            return JodaBeanUtils.equal(regexp, other.regexp) &&
                    (rangePossible == other.rangePossible) &&
                    JodaBeanUtils.equal(value, other.value) &&
                    JodaBeanUtils.equal(rangeCondition, other.rangeCondition) &&
                    JodaBeanUtils.equal(rangeOperation, other.rangeOperation) &&
                    super.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + JodaBeanUtils.hashCode(regexp);
        hash = hash * 31 + JodaBeanUtils.hashCode(rangePossible);
        hash = hash * 31 + JodaBeanUtils.hashCode(value);
        hash = hash * 31 + JodaBeanUtils.hashCode(rangeCondition);
        hash = hash * 31 + JodaBeanUtils.hashCode(rangeOperation);
        return hash ^ super.hashCode();
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-bean for {@code Like}.
     */
    public static class Meta extends RangeCondition.Meta<Object, String> {
        /**
         * The singleton instance of the meta-bean.
         */
        static final Meta INSTANCE = new Meta();

        /**
         * The meta-property for the {@code regexp} property.
         */
        private final MetaProperty<String> regexp = DirectMetaProperty.ofImmutable(
                this, "regexp", Like.class, String.class);
        /**
         * The meta-property for the {@code rangePossible} property.
         */
        private final MetaProperty<Boolean> rangePossible = DirectMetaProperty.ofImmutable(
                this, "rangePossible", Like.class, Boolean.TYPE);
        /**
         * The meta-property for the {@code value} property.
         */
        private final MetaProperty<String> value = DirectMetaProperty.ofImmutable(
                this, "value", Like.class, String.class);
        /**
         * The meta-property for the {@code rangeCondition} property.
         */
        private final MetaProperty<RangeCondition> rangeCondition = DirectMetaProperty.ofImmutable(
                this, "rangeCondition", Like.class, RangeCondition.class);
        /**
         * The meta-property for the {@code rangeOperation} property.
         */
        private final MetaProperty<Operation> rangeOperation = DirectMetaProperty.ofImmutable(
                this, "rangeOperation", Like.class, Operation.class);
        /**
         * The meta-properties.
         */
        private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
                this, (DirectMetaPropertyMap) super.metaPropertyMap(),
                "regexp",
                "rangePossible",
                "value",
                "rangeCondition",
                "rangeOperation");

        /**
         * Restricted constructor.
         */
        protected Meta() {
        }

        @Override
        protected MetaProperty<?> metaPropertyGet(String propertyName) {
            switch (propertyName.hashCode()) {
                case -934799095:  // regexp
                    return regexp;
                case 342784462:  // rangePossible
                    return rangePossible;
                case 111972721:  // value
                    return value;
                case -807966626:  // rangeCondition
                    return rangeCondition;
                case 1716048042:  // rangeOperation
                    return rangeOperation;
            }
            return super.metaPropertyGet(propertyName);
        }

        @Override
        public Like.Builder builder() {
            return new Like.Builder();
        }

        @Override
        public Class<? extends Like> beanType() {
            return Like.class;
        }

        @Override
        public Map<String, MetaProperty<?>> metaPropertyMap() {
            return metaPropertyMap$;
        }

        //-----------------------------------------------------------------------
        /**
         * The meta-property for the {@code regexp} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<String> regexp() {
            return regexp;
        }

        /**
         * The meta-property for the {@code rangePossible} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Boolean> rangePossible() {
            return rangePossible;
        }

        /**
         * The meta-property for the {@code value} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<String> value() {
            return value;
        }

        /**
         * The meta-property for the {@code rangeCondition} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<RangeCondition> rangeCondition() {
            return rangeCondition;
        }

        /**
         * The meta-property for the {@code rangeOperation} property.
         * @return the meta-property, not null
         */
        public final MetaProperty<Operation> rangeOperation() {
            return rangeOperation;
        }

        //-----------------------------------------------------------------------
        @Override
        protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
            switch (propertyName.hashCode()) {
                case -934799095:  // regexp
                    return ((Like) bean).getRegexp();
                case 342784462:  // rangePossible
                    return ((Like) bean).isRangePossible();
                case 111972721:  // value
                    return ((Like) bean).getValue();
                case -807966626:  // rangeCondition
                    return ((Like) bean).getRangeCondition();
                case 1716048042:  // rangeOperation
                    return ((Like) bean).getRangeOperation();
            }
            return super.propertyGet(bean, propertyName, quiet);
        }

        @Override
        protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
            metaProperty(propertyName);
            if (quiet) {
                return;
            }
            throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
        }

    }

    //-----------------------------------------------------------------------
    /**
     * The bean-builder for {@code Like}.
     */
    public static class Builder extends RangeCondition.Builder<Object, String> {

        private String regexp;
        private boolean rangePossible;
        private String value;
        private RangeCondition rangeCondition;
        private Operation rangeOperation;

        /**
         * Restricted constructor.
         */
        protected Builder() {
            applyDefaults(this);
        }

        /**
         * Restricted copy constructor.
         * @param beanToCopy  the bean to copy from, not null
         */
        protected Builder(Like beanToCopy) {
            super(beanToCopy);
            this.regexp = beanToCopy.getRegexp();
            this.rangePossible = beanToCopy.isRangePossible();
            this.value = beanToCopy.getValue();
            this.rangeCondition = beanToCopy.getRangeCondition();
            this.rangeOperation = beanToCopy.getRangeOperation();
        }

        //-----------------------------------------------------------------------
        @Override
        public Object get(String propertyName) {
            switch (propertyName.hashCode()) {
                case -934799095:  // regexp
                    return regexp;
                case 342784462:  // rangePossible
                    return rangePossible;
                case 111972721:  // value
                    return value;
                case -807966626:  // rangeCondition
                    return rangeCondition;
                case 1716048042:  // rangeOperation
                    return rangeOperation;
                default:
                    return super.get(propertyName);
            }
        }

        @Override
        public Builder set(String propertyName, Object newValue) {
            switch (propertyName.hashCode()) {
                case -934799095:  // regexp
                    this.regexp = (String) newValue;
                    break;
                case 342784462:  // rangePossible
                    this.rangePossible = (Boolean) newValue;
                    break;
                case 111972721:  // value
                    this.value = (String) newValue;
                    break;
                case -807966626:  // rangeCondition
                    this.rangeCondition = (RangeCondition) newValue;
                    break;
                case 1716048042:  // rangeOperation
                    this.rangeOperation = (Operation) newValue;
                    break;
                default:
                    super.set(propertyName, newValue);
                    break;
            }
            return this;
        }

        @Override
        public Builder set(MetaProperty<?> property, Object value) {
            super.set(property, value);
            return this;
        }

        @Override
        public Like build() {
            preBuild(this);
            return new Like(this);
        }

        //-----------------------------------------------------------------------
        /**
         * Sets the regexp.
         * @param regexp  the new value
         * @return this, for chaining, not null
         */
        public Builder regexp(String regexp) {
            this.regexp = regexp;
            return this;
        }

        /**
         * Sets the rangePossible.
         * @param rangePossible  the new value
         * @return this, for chaining, not null
         */
        public Builder rangePossible(boolean rangePossible) {
            this.rangePossible = rangePossible;
            return this;
        }

        /**
         * Sets the value.
         * @param value  the new value
         * @return this, for chaining, not null
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Sets the rangeCondition.
         * @param rangeCondition  the new value
         * @return this, for chaining, not null
         */
        public Builder rangeCondition(RangeCondition rangeCondition) {
            this.rangeCondition = rangeCondition;
            return this;
        }

        /**
         * Sets the rangeOperation.
         * @param rangeOperation  the new value
         * @return this, for chaining, not null
         */
        public Builder rangeOperation(Operation rangeOperation) {
            this.rangeOperation = rangeOperation;
            return this;
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(192);
            buf.append("Like.Builder{");
            int len = buf.length();
            toString(buf);
            if (buf.length() > len) {
                buf.setLength(buf.length() - 2);
            }
            buf.append('}');
            return buf.toString();
        }

        @Override
        protected void toString(StringBuilder buf) {
            super.toString(buf);
            buf.append("regexp").append('=').append(JodaBeanUtils.toString(regexp)).append(',').append(' ');
            buf.append("rangePossible").append('=').append(JodaBeanUtils.toString(rangePossible)).append(',').append(' ');
            buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ');
            buf.append("rangeCondition").append('=').append(JodaBeanUtils.toString(rangeCondition)).append(',').append(' ');
            buf.append("rangeOperation").append('=').append(JodaBeanUtils.toString(rangeOperation)).append(',').append(' ');
        }

    }

    //-------------------------- AUTOGENERATED END --------------------------
}
