package com.philkes.wemosweather.thingspeak;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Data grouped by Days
 */
public class DataSet {
    private TreeMap<LocalDate, DayData> data=new TreeMap<>();

    public boolean addEntry(DataEntry dataEntry) {
        LocalDate dataLocalDate=dataEntry.getTime().toLocalDate();
        DayData dayData=getOrAddDayData(dataLocalDate);
        return dayData.addData(dataEntry);
    }

    public DataEntry getLatestData() {
        Map.Entry<LocalDate, DayData> latestDayData=
                data.lastEntry();
        return latestDayData==null ? null : latestDayData.getValue().getNewestData();
    }


    public DayData getOrAddDayData(LocalDate date) {
        if(data.containsKey(date)) {
            return data.get(date);
        }
        DayData dayData=new DayData(date);
        data.put(date, dayData);
        return dayData;
    }

    public DayData getDayNumberData(int dayNumber) {
        return new ArrayList<>(data.entrySet()).get(dayNumber).getValue();
    }

    public List<DayData> asList() {
        return new ArrayList<>(data.values());
    }

    public TreeMap<LocalDate, DayData> getData() {
        return data;
    }

    public Set<LocalDate> getDates() {
        return data.keySet();
    }

    public List<DayData> getPreviousDays(DateTime dateTime, int pastDays) {
        DateTime minTime=dateTime.minusDays(pastDays);
        List<DayData> dayData=new ArrayList<>();
        List<DayData> allDays=Stream.of(data)
                .filter(entry ->
                        entry.getKey().isBefore(dateTime.toLocalDate()) || entry.getKey().isEqual(dateTime.toLocalDate())
                                && entry.getKey().isAfter(minTime.toLocalDate())
                )
                .map(Map.Entry::getValue).toList();
        for(int i=0; i<pastDays; i++) {
            if(allDays.size() - 1 - i >= 0) {
                dayData.add(allDays.get(allDays.size() - 1 - i));
            }else{
                break;
            }
        }
        return dayData;
    }

    public List<DataEntry> getPreviousHours(DateTime dateTime, int pastHours) {
        //TODO Wenn über Tagesgrenze?
        final DateTime minTime=dateTime
                .minusHours(pastHours)
                .minusSeconds(1);
        return Stream.of(getPreviousDays(dateTime, 2)).map(dayData -> dayData.getData())
                .flatMap(Stream::of)
                .filter(entry -> entry.getTime().isAfter(minTime) && entry.getTime().isBefore(dateTime)).toList();
    }

    /**
     * DataEntries for 1 Day
     */
    public static class DayData {
        private LocalDate date;
        private TreeSet<DataEntry> data=new TreeSet<>();

        public DayData(LocalDate date) {
            this.date=date;
        }

        //Returns true if data is new
        public boolean addData(DataEntry dataEntry) {
            return data.add(dataEntry);
        }

        public LocalDate getDate() {
            return date;
        }

        public TreeSet<DataEntry> getData() {
            return data;
        }

        public DataEntry getNewestData() {
            return data.last();
        }

        public float getAverageTemp() {
            return (float) Stream.of(data).mapToDouble(entry -> (double) entry.getTemperature()).average().orElse(0.0);
        }

        public float getMaxTemp() {
            return (float) Stream.of(data).mapToDouble(entry -> (double) entry.getTemperature()).max().orElse(0.0);
        }

        public float getMinTemp() {
            return (float) Stream.of(data).mapToDouble(entry -> (double) entry.getTemperature()).min().orElse(0.0);
        }

        public String getLabel() {
            return date.toString("dd.MM");
        }

        public DataEntry getEntryNumberData(int entryNumber) {
            return Stream.of(data).collect(Collectors.toList()).get(entryNumber);
        }
    }

    public static class DataSetAdapter implements JsonDeserializer<DataSet> {

        private static final DateTimeFormatter parser=ISODateTimeFormat.dateTimeParser();

        @Override
        public DataSet deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            if(!json.isJsonObject()) {
                throw new JsonParseException("Invalid object");
            }
            JsonObject jsonObject=(JsonObject) json;
            if(!jsonObject.has("feeds")) {
                throw new JsonParseException("Invalid fields");
            }
            JsonArray feedsArr=(JsonArray) jsonObject.get("feeds");
            DataSet dataSet=new DataSet();
            for(JsonElement feedEntry : feedsArr) {
                DataEntry dataEntry=new DataEntry.DataEntryAdapter().deserialize(feedEntry, type, context);
                if(dataEntry!=null) {
                    dataSet
                            .getOrAddDayData(dataEntry.getTime().toLocalDate())
                            .addData(dataEntry);
                }
            }
            return dataSet;
        }
    }
}
