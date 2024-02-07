package com.cyoda.plugins;
import com.cyoda.presto.client.types.DataType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ValueMaps {
    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, Character> chars = new HashMap<>();
    private final Map<String, Double> doubles = new HashMap<>();
    private final Map<String, Float> floats = new HashMap<>();
    private final Map<String, Byte> bytes = new HashMap<>();
    private final Map<String, Short> shorts = new HashMap<>();
    private final Map<String, Long> longs = new HashMap<>();
    private final Map<String, Integer> ints = new HashMap<>();
    private final Map<String, LocalDate> localDates = new HashMap<>();
    private final Map<String, LocalDateTime> localDateTimes = new HashMap<>();
    private final Map<String, LocalTime> localTimes = new HashMap<>();
    private final Map<String, Date> dates = new HashMap<>();
    private final Map<String, ZonedDateTime> zonedDateTimes = new HashMap<>();
    private final Map<String, Year> years = new HashMap<>();
    private final Map<String, YearMonth> yearMonths = new HashMap<>();
    private final Map<String, Locale> locales = new HashMap<>();
    private final Map<String, BigDecimal> bigDecimals = new HashMap<>();
    private final Map<String, BigInteger> bigIntegers = new HashMap<>();
    private final Map<String, Boolean> booleans = new HashMap<>();
    private final Map<String, UUID> uuids = new HashMap<>();
    private final Map<String, byte[]> byteArrays = new HashMap<>();

    public Object getValue(DataType dataType, String fieldName) {
        return switch (dataType) {
            case STRING -> strings.get(fieldName);
            case CHARACTER -> chars.get(fieldName);
            case DOUBLE -> doubles.get(fieldName);
            case FLOAT -> floats.get(fieldName);
            case BYTE -> bytes.get(fieldName);
            case SHORT -> shorts.get(fieldName);
            case LONG -> longs.get(fieldName);
            case INTEGER -> ints.get(fieldName);
            case LOCAL_DATE -> localDates.get(fieldName);
            case LOCAL_DATE_TIME -> localDateTimes.get(fieldName);
            case LOCAL_TIME -> localTimes.get(fieldName);
            case DATE -> dates.get(fieldName);
            case ZONED_DATE_TIME -> zonedDateTimes.get(fieldName);
            case YEAR -> years.get(fieldName);
            case YEAR_MONTH -> yearMonths.get(fieldName);
            case LOCALE -> locales.get(fieldName);
            case BIG_DECIMAL -> bigDecimals.get(fieldName);
            case BIG_INTEGER -> bigIntegers.get(fieldName);
            case BOOLEAN -> booleans.get(fieldName);
            case UUID_TYPE -> uuids.get(fieldName);
            case BYTE_ARRAY -> byteArrays.get(fieldName);
            default -> strings.get(fieldName);
        };
    }
    public Map<String, String> serializeContent() {
        Map<String, String> serialized = new HashMap<>();

        // Serialize strings
        strings.forEach(serialized::put);

        // Serialize other types, converting each value to a String
        chars.forEach((key, value) -> serialized.put(key, value.toString()));
        doubles.forEach((key, value) -> serialized.put(key, value.toString()));
        floats.forEach((key, value) -> serialized.put(key, value.toString()));
        bytes.forEach((key, value) -> serialized.put(key, value.toString()));
        shorts.forEach((key, value) -> serialized.put(key, value.toString()));
        longs.forEach((key, value) -> serialized.put(key, value.toString()));
        ints.forEach((key, value) -> serialized.put(key, value.toString()));

        // Example of serializing dates with a specific format
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        localDates.forEach((key, value) -> serialized.put(key, value.format(dateFormatter)));

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        localDateTimes.forEach((key, value) -> serialized.put(key, value.format(dateTimeFormatter)));
        localTimes.forEach((key, value) -> serialized.put(key, value.format(DateTimeFormatter.ISO_LOCAL_TIME)));

        // Assuming a simple toString() for other complex types; customize as needed
        bigDecimals.forEach((key, value) -> serialized.put(key, value.toPlainString()));
        bigIntegers.forEach((key, value) -> serialized.put(key, value.toString()));
        booleans.forEach((key, value) -> serialized.put(key, value.toString()));
        uuids.forEach((key, value) -> serialized.put(key, value.toString()));
        locales.forEach((key, value) -> serialized.put(key, value.toString()));
        // For byteArrays and others, you might need a specific serialization strategy
        // Example for byteArrays: Convert to a Base64 string or similar

        // Add serialization for other types as needed, with appropriate formatting

        return serialized;
    }
    public Map<String, Object> consolidateMaps() {
        Map<String, Object> consolidated = new HashMap<>();

        // Consolidate all maps into a single map without changing the types
        consolidated.putAll(strings); // Strings are already Object
        consolidated.putAll(chars); // Autoboxing will wrap char into Character
        consolidated.putAll(doubles); // Autoboxing will wrap double into Double
        consolidated.putAll(floats); // Autoboxing will wrap float into Float
        consolidated.putAll(bytes); // Autoboxing will wrap byte into Byte
        consolidated.putAll(shorts); // Autoboxing will wrap short into Short
        consolidated.putAll(longs); // Autoboxing will wrap long into Long
        consolidated.putAll(ints); // Autoboxing will wrap int into Integer
        consolidated.putAll(localDates); // LocalDate is already an Object
        consolidated.putAll(localDateTimes); // LocalDateTime is already an Object
        consolidated.putAll(localTimes); // LocalTime is already an Object
        consolidated.putAll(dates); // Date is already an Object
        consolidated.putAll(zonedDateTimes); // ZonedDateTime is already an Object
        consolidated.putAll(years); // Year is already an Object
        consolidated.putAll(yearMonths); // YearMonth is already an Object
        consolidated.putAll(locales); // Locale is already an Object
        consolidated.putAll(bigDecimals); // BigDecimal is already an Object
        consolidated.putAll(bigIntegers); // BigInteger is already an Object
        consolidated.putAll(booleans); // Autoboxing will wrap boolean into Boolean
        consolidated.putAll(uuids); // UUID is already an Object
        consolidated.putAll(byteArrays); // byte[] is already an Object, though it's an array not a single value

        return consolidated;
    }

    public Map<String, String> getStrings() {
        return strings;
    }

    public Map<String, Character> getChars() {
        return chars;
    }

    public Map<String, Double> getDoubles() {
        return doubles;
    }

    public Map<String, Float> getFloats() {
        return floats;
    }

    public Map<String, Byte> getBytes() {
        return bytes;
    }

    public Map<String, Short> getShorts() {
        return shorts;
    }

    public Map<String, Long> getLongs() {
        return longs;
    }

    public Map<String, Integer> getInts() {
        return ints;
    }

    public Map<String, LocalDate> getLocalDates() {
        return localDates;
    }

    public Map<String, LocalDateTime> getLocalDateTimes() {
        return localDateTimes;
    }

    public Map<String, LocalTime> getLocalTimes() {
        return localTimes;
    }

    public Map<String, Date> getDates() {
        return dates;
    }

    public Map<String, ZonedDateTime> getZonedDateTimes() {
        return zonedDateTimes;
    }

    public Map<String, Year> getYears() {
        return years;
    }

    public Map<String, YearMonth> getYearMonths() {
        return yearMonths;
    }

    public Map<String, Locale> getLocales() {
        return locales;
    }

    public Map<String, BigDecimal> getBigDecimals() {
        return bigDecimals;
    }

    public Map<String, BigInteger> getBigIntegers() {
        return bigIntegers;
    }

    public Map<String, Boolean> getBooleans() {
        return booleans;
    }

    public Map<String, UUID> getUuids() {
        return uuids;
    }

    public Map<String, byte[]> getByteArrays() {
        return byteArrays;
    }
}
