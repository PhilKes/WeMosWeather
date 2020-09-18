package com.philkes.wemosweather.thingspeak;

import androidx.annotation.NonNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;
import java.util.Map;

/** Temperature, Humidity, Pressure Data for a Timestamp*/
public class DataEntry implements Comparable<DataEntry> {
    private static final DateTimeFormatter formatter =DateTimeFormat.forPattern("HH:mm");

    private long id;
    private float temperature;
    private float humidity;
    private float pressure;
    private int brightness;
    private DateTime time;

    public DataEntry() {
    }

    public DataEntry(long id, float temperature, float humidity, float pressure, int brightness, DateTime time) {
        this.id=id;
        this.temperature=temperature;
        this.humidity=humidity;
        this.pressure=pressure;
        this.time=time;
        this.brightness=brightness;
    }


    public long getId() {
        return id;
    }

    public DataEntry setId(long id) {
        this.id=id;
        return this;
    }

    public int getBrightness() {
        return brightness;
    }

    public DataEntry setBrightness(int brightness) {
        this.brightness=brightness;
        return this;
    }

    public float getTemperature() {
        return temperature;
    }

    public DataEntry setTemperature(float temperature) {
        this.temperature=temperature;
        return this;
    }

    public float getHumidity() {
        return humidity;
    }

    public DataEntry setHumidity(float humidity) {
        this.humidity=humidity;
        return this;

    }

    public float getPressure() {
        return pressure;
    }

    public DataEntry setPressure(float pressure) {
        this.pressure=pressure;
        return this;

    }

    public DateTime getTime() {
        return time;
    }

    public String getTimeString(){
        return formatter.print(time)+"h";
    }

    public DataEntry setTime(DateTime time) {
        this.time=time;
        return this;

    }

    @Override
    public int compareTo(DataEntry o) {
        if(this.time== null && o.time==null)
            return 0;
        else if(this.time== null && o.time!=null)
            return -1;
        else if(this.time!=null && o.time== null)
            return 1;
        return this.time.compareTo(o.time);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s{ Temp:%f\tHum:%f\tPress:%f\tBright:%d}",time.toString("dd.MM.YYYY HH:mm:ss"),temperature,humidity,pressure,brightness);
    }

    public String getLabel() {
        return time.toString("HH:mm");
    }

    public static class DataEntryAdapter implements JsonDeserializer<DataEntry> {

        private static final DateTimeFormatter parser    = ISODateTimeFormat.dateTimeParser();
        @Override
        public DataEntry deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {

            Type mapType=new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> data=context.deserialize(json, mapType);
            if(!data.containsKey("created_at")
                    || !data.containsKey("entry_id")) {
                throw new JsonParseException("Invalid fields");
            }
            DataEntry entry=null;
            for(int i=1; i<5; i++) {
                if(data.get("field"+i)==null){
                    return null;
                }
            }
            try {
                entry=new DataEntry()
                        .setTemperature(Float.parseFloat(data.get("field1")))
                        .setPressure(Float.parseFloat(data.get("field2")))
                        .setHumidity(Float.parseFloat(data.get("field3")))
                        .setBrightness((int)Double.parseDouble(data.get("field4")))
                        .setId(Long.parseLong(data.get("entry_id")));

                DateTime dateTime = parser.parseDateTime(data.get("created_at"));
                dateTime = dateTime.withZone(DateTimeZone.forID("Europe/Berlin"));
                entry.setTime(dateTime);
            }
            catch(Exception e) {
                throw new JsonParseException(e);
            }
            return entry;

        }
    }
}