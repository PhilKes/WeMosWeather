package com.philkes.wemosweather.thingspeak;

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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Data grouped by Days
 */
public class DataSet {
    private HashMap<LocalDate, DayData> data=new HashMap<>();

    public DayData addEntry(DataEntry dataEntry) {
        LocalDate dataLocalDate=dataEntry.getTime().toLocalDate();
        DayData dayData=getOrAddDayData(dataLocalDate);
        dayData.addData(dataEntry);
        return dayData;
    }

    public DataEntry getCurrentData() {
        DayData todayData=data.get(DateTime.now().toLocalDate());
        return todayData==null ? null : todayData.getNewestData();
    }

    public DayData getOrAddDayData(LocalDate date) {
        if(data.containsKey(date)) {
            return data.get(date);
        }
        DayData dayData=new DayData(date);
        data.put(date, dayData);
        return dayData;
    }
    public DayData getDayNumberData(int dayNumber){
        return data.entrySet().stream().collect(Collectors.toList()).get(dayNumber).getValue();
    }

    public HashMap<LocalDate, DayData> getData() {
        return data;
    }

    public Set<LocalDate> getDates(){
        return data.keySet();
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

        public void addData(DataEntry dataEntry) {
            data.add(dataEntry);
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
            return (float)data.stream().mapToDouble(entry-> (double)entry.getTemperature()).average().orElse(0.0);
        }

        public float getMaxTemp() {
            return (float)data.stream().mapToDouble(entry-> (double)entry.getTemperature()).max().orElse(0.0);
        }
        public float getMinTemp() {
            return (float)data.stream().mapToDouble(entry-> (double)entry.getTemperature()).min().orElse(0.0);
        }

        public String getLabel() {
            return date.toString("dd.MM");
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
