package com.cyoda.connector.client.logic.converters.structure;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.function.Function;

public class TemporalTransformer<T extends Temporal> {

    private final Function<Year, T> fromYear;
    private final Function<YearMonth, T> fromYearMonth;
    private final Function<LocalTime, T> fromLocalTime;
    private final Function<LocalDate, T> fromLocalDate;
    private final Function<LocalDateTime, T> fromLocalDateTime;
    private final Function<ZonedDateTime, T> fromZonedDateTime;

    public TemporalTransformer(Function<Year, T> fromYear,
                                  Function<YearMonth, T> fromYearMonth,
                                  Function<LocalTime, T> fromLocalTime,
                                  Function<LocalDate, T> fromLocalDate,
                                  Function<LocalDateTime, T> fromLocalDateTime,
                                  Function<ZonedDateTime, T> fromZonedDateTime) {
        this.fromYear = fromYear;
        this.fromYearMonth = fromYearMonth;
        this.fromLocalTime = fromLocalTime;
        this.fromLocalDate = fromLocalDate;
        this.fromLocalDateTime = fromLocalDateTime;
        this.fromZonedDateTime = fromZonedDateTime;
    }


    /*
    covered types:
  ".year" : "2024" - length always 4
  ".year_month" : "2025-03" - length always 7
  ".local_time" : "00:00","00:00:01","23:59:59.999999999" - length 5 or 8 or '.' at [8]
  ".local_date" : "1970-01-02" - length always 10,
  ".local_date_time" : "1970-01-02T00:00:01", - 16, 19 or '.' at [19]
  ".zoned_date_time" : "1970-01-02T00:00:01+03:00"
    * */
    public T parse(Object value, String columnName, Class<T> clazz) {
        String sValue = (String) value;
        int sLen = sValue.length();
        try {
            if (fromYear != null && sLen == 4) {
                return fromYear.apply(Year.parse(sValue));
            } else if (fromYearMonth != null && sLen == 7) {
                return fromYearMonth.apply(YearMonth.parse(sValue));
            } else if (fromLocalTime != null && (sLen == 5 || sLen == 8 || (sLen > 8 && sValue.charAt(8) == '.'))) {
                return fromLocalTime.apply(LocalTime.parse(sValue));
            } else if (fromLocalDate != null && sLen == 10) {
                return fromLocalDate.apply(LocalDate.parse(sValue));
            } else if (fromLocalDateTime != null && (sLen == 16 || sLen == 19 || (sLen > 19 && sValue.charAt(19) == '.'))) {
                return fromLocalDateTime.apply(LocalDateTime.parse(sValue));
            } else if (fromZonedDateTime != null) {
                return fromZonedDateTime.apply(parseZonedDateTime(sValue));
            } else throw new RuntimeException("No converter to target type");
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse " + sValue + " to " + clazz.getSimpleName() + " at column " + columnName, e);
        }
    }
    private ZonedDateTime parseZonedDateTime(String value) {
        if (value.charAt(19) == '.') {
            int tzPos = value.length() - 6;
            String local = value.substring(0, tzPos);
            String tz = value.substring(tzPos);
            return ZonedDateTime.of(LocalDateTime.parse(local), ZoneOffset.of(tz));
        } else {
            return ZonedDateTime.parse(value);
        }
    }
}
