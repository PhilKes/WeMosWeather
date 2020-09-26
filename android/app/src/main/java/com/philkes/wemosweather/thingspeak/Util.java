package com.philkes.wemosweather.thingspeak;

import android.content.res.Resources;
import android.provider.ContactsContract;
import android.util.Log;

import com.annimon.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.philkes.wemosweather.R;
import com.philkes.wemosweather.prediction.Weather;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;

public class Util {

    private static GsonBuilder gsonBuilder=new GsonBuilder();
    public static Gson gson;

    static {
        gsonBuilder.registerTypeAdapter(DataEntry.class, new DataEntry.DataEntryAdapter());
        gsonBuilder.registerTypeAdapter(DataSet.class, new DataSet.DataSetAdapter());
        gson=gsonBuilder.create();
    }

    public static String READ_KEY;
    public static String CHANNEL_ID;

    public static void loadValues(Resources resources) {
        CHANNEL_ID=resources.getString(R.string.THINGSPEAK_CHANNEL);
        READ_KEY=resources.getString(R.string.THINGSPEAK_READ_KEY);
    }

    public static final String THINGSPEAK_URL="https://api.thingspeak.com/";
    public static final String TIME_ZOME_PARAM="&timezone=Europe/Berlin";

    public static final int CHART_TEXT_SIZE=14;


    public static final float TEMP_MAX=50;
    public static final float TEMP_MIN=-10f;
    public static final float HUM_MAX=100;

    public static final float TEMP_HUM_SCALE=(TEMP_MAX + Math.abs(TEMP_MIN)) / HUM_MAX;

    public static final float sub=(0 * TEMP_HUM_SCALE) / 2.0f;

    public static String getLastChannelEntryURL() {
        return THINGSPEAK_URL + "/channels/" + CHANNEL_ID + "/feeds/last.json?api_key="
                + READ_KEY + TIME_ZOME_PARAM;
    }

    public static String getChannelFeedsURL() {
        return THINGSPEAK_URL + "/channels/" + CHANNEL_ID + "/feeds.json?api_key=" +
                READ_KEY + "&days=14" + TIME_ZOME_PARAM;
    }

    public static ColumnChartData generateColumnData(DataSet dataSet) {
        List<AxisValue> axisValues=new ArrayList<AxisValue>();
        List<Column> columns=new ArrayList<Column>();
        List<SubcolumnValue> values;

        int axisCounter=0;
        for(Map.Entry<LocalDate, DataSet.DayData> dayData : dataSet.getData().entrySet()) {
            values=new ArrayList<>();
            float dayTempValue=dayData.getValue().getMaxTemp();
            values.add(new SubcolumnValue(dayTempValue, Util.chartColorForTemperature(dayTempValue))
                    .setLabel("" + dayTempValue + "C°")
            );

            axisValues.add(new AxisValue(axisCounter).setLabel(dayData.getValue().getLabel()));
            columns.add(new Column(values).setHasLabelsOnlyForSelected(true));
            axisCounter++;
        }
        //TODO "HUmidity" außerhalb des kreises
        ColumnChartData colData=new ColumnChartData(columns);
        colData.setAxisXBottom(new Axis(axisValues)
                .setMaxLabelChars(5)
                //.setName("Last 7 Days")
                .setTextSize(CHART_TEXT_SIZE - 2)
        );
        colData.setAxisYLeft(new Axis().setHasLines(true)
                .setMaxLabelChars(3)
                .setName("Max  C °")
                .setTextSize(CHART_TEXT_SIZE - 2)
        );
        return colData;
    }

    public static LineChartData generateLineData(DataSet.DayData dayData, int color) {

        List<AxisValue> axisValues=new ArrayList<AxisValue>();
        List<PointValue> temperatureVals=new ArrayList<PointValue>();
        List<PointValue> humidityVals=new ArrayList<PointValue>();
        int dataCounter=0;
        for(DataEntry dataEntry : dayData.getData()) {
            temperatureVals.add(new PointValue(dataCounter, 0f)
                    .setTarget(dataCounter, dataEntry.getTemperature())
                    .setLabel("" + dataEntry.getTemperature())
            );
            humidityVals.add(new PointValue(dataCounter, 0f)
                    .setTarget(dataCounter, dataEntry.getHumidity() * TEMP_HUM_SCALE - Math.abs(TEMP_MIN)));
            axisValues.add(new AxisValue(dataCounter).setLabel(dataEntry.getLabel()));
            dataCounter++;
        }

        Line temperatureLine=new Line(temperatureVals);
        temperatureLine.setColor(ChartUtils.darkenColor(ChartUtils.COLOR_RED))
                .setCubic(true)
                .setHasLabelsOnlyForSelected(true);

        Line humidityLine=new Line(humidityVals);
        humidityLine.setColor(ChartUtils.COLOR_BLUE)
                .setFilled(true)
                .setPointRadius(0)
                .setHasLabelsOnlyForSelected(false)
                .setHasPoints(false);

        List<Line> lines=new ArrayList<Line>();
        lines.add(temperatureLine);
        lines.add(humidityLine);

        LineChartData lineData=new LineChartData(lines);

        lineData.setValueLabelBackgroundEnabled(true);

        lineData.setAxisXBottom(new Axis(axisValues)
                .setHasLines(true)
                .setMaxLabelChars(6)
        );
        lineData.setAxisYLeft(new Axis()
                .setHasLines(true)
                .setMaxLabelChars(3)
                .setName("Temperature C °")
                .setTextSize(CHART_TEXT_SIZE)
                .setTextColor(ChartUtils.COLOR_RED)
        );

        lineData.setAxisYRight(new Axis().setFormatter(new HumidityValueFormatter(TEMP_HUM_SCALE, sub, 0))
                .setHasLines(true)
                .setMaxLabelChars(3)
                .setName("Humidity %")
                .setTextSize(CHART_TEXT_SIZE)
                .setTextColor(ChartUtils.COLOR_BLUE)
        );

        return lineData;
    }

    public static float getFieldFromEntry(String field, DataEntry dataEntry) {
        switch(field) {
            case "TEMP":
                return dataEntry.getTemperature();
            case "HUM":
                return dataEntry.getHumidity();
            case "PRESS":
                return dataEntry.getPressure();
            case "BRIGHT":
                return dataEntry.getBrightness();

        }
        return -1f;
    }

    public static String getFieldUnits(String field) {
        switch(field) {
            case "TEMP":
                return "Temperature C°";
            case "HUM":
                return "Humidity %";
            case "PRESS":
                return "Pressure hPA";
            case "BRIGHT":
                return "Brightness";

        }
        return "Unit";
    }


    public static LineChartData generateHistoryData(DataSet dataSet, String field, int days, int color) {

        List<AxisValue> axisValues=new ArrayList<AxisValue>();
        List<PointValue> values=new ArrayList<PointValue>();
        int dataCounter=0;
        for(DataSet.DayData dayData : dataSet.getData().values()) {
            for(DataEntry dataEntry : dayData.getData()) {
                values.add(new PointValue(dataCounter, 0f)
                        .setTarget(dataCounter, getFieldFromEntry(field, dataEntry))
                        .setLabel("" + getFieldFromEntry(field, dataEntry))
                );
                axisValues.add(new AxisValue(dataCounter).setLabel(dataEntry.getLabel()));
                dataCounter++;
            }
        }

        Line valueLine=new Line(values);
        valueLine.setColor(ChartUtils.darkenColor(color))
                .setCubic(true)
                .setPointRadius(1)
                .setHasLabelsOnlyForSelected(true);

        List<Line> lines=new ArrayList<Line>();
        lines.add(valueLine);

        LineChartData lineData=new LineChartData(lines);

        lineData.setValueLabelBackgroundEnabled(true);

        lineData.setAxisXBottom(new Axis(axisValues)
                .setHasLines(true)
                .setMaxLabelChars(6)
        );
        lineData.setAxisYLeft(new Axis()
                .setHasLines(true)
                .setMaxLabelChars(3)
                .setName(getFieldUnits(field))
                .setTextSize(CHART_TEXT_SIZE)
                .setTextColor(color)
        );

        return lineData;
    }

    public static DecimalFormat decimalFormat=new DecimalFormat("#.00");

    private static int MINUTES_PER_DAY=24 * 60;

    public static DataSet generateTestDataSet(int days, int entriesPerDay, float minTemp, float maxTemp) {
        float tempRange=(maxTemp - minTemp);
        DataSet dataSet=new DataSet();
        LocalDate date=LocalDate.now().minusDays(days - 1);
        int entryInterval=(int) Math.round(Math.ceil(MINUTES_PER_DAY / entriesPerDay));

        int nightFactor=Math.round(tempRange / 3);
        int nightBorder=entriesPerDay - (entriesPerDay / 5);

        for(int day=0; day<days; day++) {
            DataSet.DayData dayData=dataSet.getOrAddDayData(date);
            DateTime entryTime=new DateTime(date.toDate());
            float dayMaxTemp=maxTemp - (maxTemp * (float) Math.random() * 0.5f);
            float dayTempRange=(dayMaxTemp - minTemp);

            for(int entry=0; entry<entriesPerDay; entry++) {
                float temp;
                if(entry<nightBorder) {
                    temp=Float.parseFloat(decimalFormat.format((minTemp + nightFactor) + (float) Math.random() * (dayTempRange - nightFactor)));
                }
                else {
                    temp=Float.parseFloat(decimalFormat.format((minTemp) + (float) Math.random() * (dayTempRange - nightFactor)));
                }
                float hum=Math.abs(Float.parseFloat(decimalFormat.format((temp / dayTempRange) * 100)));
                dayData.addData(new DataEntry()
                        .setId(day * entriesPerDay + entry)
                        .setTemperature(temp)
                        .setHumidity(hum)
                        .setTime(entryTime));
                entryTime=entryTime.plusMinutes(entryInterval);
            }
            date=date.plusDays(1);
        }
        return dataSet;
    }

    public static Weather getPrediction(DateTime currentDateTime, DataSet dataSet) {
        List<DataEntry> hourData=dataSet.getPreviousHours(currentDateTime, 1);
        DataEntry mostRecentEntry=hourData.get(hourData.size() - 1);

        if(mostRecentEntry.getPressure()>968) {
            return Weather.SUNNY;
        }
        else if(mostRecentEntry.getPressure()<963) {
            if(mostRecentEntry.getPressure()<960) {
                return Weather.RAIN_HEAVY;
            }
            else {
                return Weather.RAIN_LIGHT;
            }
        }

        List<Float> pressureValues=Stream.of(hourData).map(entry -> entry.getPressure()).toList();
        float pressureDifSum=0;
        //15min between each entry
        for(int t=1; t<pressureValues.size(); t++) {
            float diff=pressureValues.get(t) - pressureValues.get(t - 1);
            pressureDifSum+=diff;
        }
        System.out.println("Last hour PressDiffSum:" + pressureDifSum);

        // Fast changing Pressure
        if(Math.abs(pressureDifSum)>1.0) {
            //Pressure going up fast
            if(pressureDifSum>0) {
                if(mostRecentEntry.getBrightness()<500) {
                    return Weather.CLEAR_NIGHT;
                }
                return Weather.SUNNY;
            }
            //Pressure going down fast
            else {
                if(Math.abs(pressureDifSum)>1.3) {
                    return Weather.STORM;
                }
                else {
                    return Weather.RAIN_HEAVY;
                }
            }
        }
        // Slower changing pressure
        else {
            // Medium fast negative Pressure
            if(Math.abs(pressureDifSum)>0.5 && pressureDifSum<0.0) {
                return Weather.CLOUDY;
            }
            if(mostRecentEntry.getBrightness()<500) {
                return Weather.CLEAR_NIGHT;
            }
            return Weather.SUNNY;
        }
    }

    /**
     * Recalculated height values to display on axis. For this example I use auto-generated height axis so I
     * override only formatAutoValue method.
     */
    private static class HumidityValueFormatter extends SimpleAxisValueFormatter {

        private float scale;
        private float sub;
        private int decimalDigits;

        private float bias=0f;

        public HumidityValueFormatter(float scale, float sub, int decimalDigits) {
            this.scale=scale;
            this.sub=sub;
            this.decimalDigits=decimalDigits;
            if(TEMP_MIN<0) {
                bias=Math.abs(TEMP_MIN);
            }
        }

        @Override
        public int formatValueForAutoGeneratedAxis(char[] formattedValue, float value, int autoDecimalDigits) {
            float scaledValue=(value + sub + bias) / scale;
            int x=super.formatValueForAutoGeneratedAxis(formattedValue, scaledValue, this.decimalDigits);
            return x;
        }
    }

    private static int chartColorForTemperature(float temperature) {
        if(temperature<10) {
            return ChartUtils.darkenColor(ChartUtils.COLOR_BLUE);
        }
        else if(temperature<17) {
            return ChartUtils.COLOR_GREEN;
        }
        else if(temperature<21) {
            return ChartUtils.COLOR_GREEN;
        }
        else if(temperature<30) {
            return ChartUtils.COLOR_ORANGE;
        }
        else {
            return ChartUtils.darkenColor(ChartUtils.COLOR_RED);
        }
    }
}
