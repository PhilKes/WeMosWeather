package com.philkes.wemosweather.thingspeak;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.util.ChartUtils;

public class Util {

    private static GsonBuilder gsonBuilder=new GsonBuilder();
    public static Gson gson;

    static {
        gsonBuilder.registerTypeAdapter(DataEntry.class, new DataEntry.DataEntryAdapter());
        gsonBuilder.registerTypeAdapter(DataSet.class, new DataSet.DataSetAdapter());
        gson=gsonBuilder.create();
    }

    public static final String READ_KEY="OO615HXB4VWR8TKR";
    public static final String CHANNEL_ID="1093516";
    public static final String THINGSPEAK_URL="https://api.thingspeak.com/";
    public static final String TIME_ZOME_PARAM="&timezone=Europe/Berlin";

    public static String getLastChannelEntryURL() {
        return THINGSPEAK_URL + "/channels/" + CHANNEL_ID + "/feeds/last.json?api_key="
                + READ_KEY + TIME_ZOME_PARAM;
    }

    public static String getChannelFeedsURL() {
        return THINGSPEAK_URL + "/channels/" + CHANNEL_ID + "/feeds.json?api_key=" +
                READ_KEY + "&days=7" + TIME_ZOME_PARAM;
    }

    public static ColumnChartData generateColumnData(DataSet dataSet) {
        List<AxisValue> axisValues=new ArrayList<AxisValue>();
        List<Column> columns=new ArrayList<Column>();
        List<SubcolumnValue> values;

        int axisCounter=0;
        for(Map.Entry<LocalDate, DataSet.DayData> dayData : dataSet.getData().entrySet()) {
            values=new ArrayList<>();
            float dayTempValue=dayData.getValue().getMaxTemp();
            values.add(new SubcolumnValue(dayTempValue, Util.chartColorForTemperature(dayTempValue)));

            axisValues.add(new AxisValue(axisCounter).setLabel(dayData.getValue().getLabel()));
            columns.add(new Column(values).setHasLabelsOnlyForSelected(true));
            axisCounter++;
        }
        ColumnChartData colData=new ColumnChartData(columns);
        colData.setAxisXBottom(new Axis(axisValues)
                .setMaxLabelChars(5)
                .setName("Last 7 Days")
        );
        colData.setAxisYLeft(new Axis().setHasLines(true)
                .setMaxLabelChars(3)
                .setName("Max  C °")
        );
        return colData;
    }
    public static LineChartData generateLineData(DataSet.DayData dayData,int color) {
        List<AxisValue> axisValues=new ArrayList<AxisValue>();
        List<PointValue> values=new ArrayList<PointValue>();
        int dataCounter=0;
        for(DataEntry dataEntry : dayData.getData()) {
            values.add(new PointValue(dataCounter, dataEntry.getTemperature()));
            axisValues.add(new AxisValue(dataCounter).setLabel(dataEntry.getLabel()));
            dataCounter++;
        }

        Line line=new Line(values);
        line.setColor(color).setCubic(true);

        List<Line> lines=new ArrayList<Line>();
        lines.add(line);

        LineChartData lineData=new LineChartData(lines);
        lineData.setAxisXBottom(new Axis(axisValues)
                .setHasLines(true)
                .setMaxLabelChars(6)
        );
        lineData.setAxisYLeft(new Axis()
                .setHasLines(true)
                .setMaxLabelChars(3)
                .setName("C °")
        );
        return lineData;
    }

    private static int chartColorForTemperature(float temperature) {
        if(temperature < 10)
            return ChartUtils.darkenColor(ChartUtils.COLOR_BLUE);
        else if(temperature <17)
            return ChartUtils.COLOR_BLUE;
        else if(temperature <24)
            return ChartUtils.COLOR_ORANGE;
        else if(temperature < 32)
            return ChartUtils.COLOR_RED;
        else
            return ChartUtils.darkenColor(ChartUtils.COLOR_RED);

    }
}
