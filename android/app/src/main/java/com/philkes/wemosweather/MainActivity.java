package com.philkes.wemosweather;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.philkes.wemosweather.components.DataSlider;
import com.philkes.wemosweather.thingspeak.DataEntry;
import com.philkes.wemosweather.thingspeak.DataSet;
import com.philkes.wemosweather.thingspeak.Util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import java.lang.reflect.Type;
import org.joda.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PieChartView;

import static com.philkes.wemosweather.thingspeak.Util.getChannelFeedsURL;
import static com.philkes.wemosweather.thingspeak.Util.getLastChannelEntryURL;

public class MainActivity extends AppCompatActivity {

    public static final String TAG="WeMosWeather";

    private DataSlider tempSlider;
    private DataSlider humSlider;

    private LineChartView lineChart1;
    private ColumnChartView colChart1;

    public final static String[] months=new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug",
            "Sep", "Oct", "Nov", "Dec",};

    public final static String[] days=new String[]{"Mon", "Tue", "Wen", "Thu", "Fri", "Sat", "Sun",};

    private LineChartView chartTop;
    private ColumnChartView chartBottom;

    private LineChartData lineData;
    private ColumnChartData columnData;

    private DataSet dataSet;

    // 0: Thingspeak Channel
    // 1: HTTP WebServer
    public static final int UPDATE_SOURCE=0;

    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tempSlider=new DataSlider(
                this,
                (PieChartView) findViewById(R.id.tempPie),
                "Temperature",
                "C°",
                50f,
                ChartUtils.COLOR_RED);
        //.setRequestQueue(queue)
        //.setUpdateWithHTTP("temperature", 3000);

        humSlider=new DataSlider(this,
                (PieChartView) findViewById(R.id.humPie),
                "Humidity",
                "%",
                100f,
                ChartUtils.COLOR_BLUE);
        //.setRequestQueue(queue)
        //.setUpdateWithHTTP("humidity", 3000);

        chartTop=findViewById(R.id.lineChart1);
        generateInitialLineData();
        chartBottom=findViewById(R.id.colChart1);
        generateColumnData();
        queue=Volley.newRequestQueue(this);
        requestInitialThingspeakData();
        setupThingspeakUpdates(3000);
    }
    /** Requests and loads DataSet from Thingspeak channel data*/
    private void requestInitialThingspeakData() {
        String url=getChannelFeedsURL();
        JsonObjectRequest stringRequest=new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "initial Thingspeak Data: " + response);
                        dataSet = Util.gson.fromJson(response.toString(), DataSet.class);
                        updateDataUI();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "update: " + error.toString());
                    }
                });
        queue.add(stringRequest);
    }

    private void updateDataUI() {
        DataEntry currentData=dataSet.getCurrentData();
        tempSlider.updateValue(currentData.getTemperature());
        humSlider.updateValue(currentData.getHumidity());
        Log.d(TAG, "updateDataUI: "+currentData);
    }

    /** Updates Data from Thingspeak channel in an given interval */
    public void setupThingspeakUpdates(final int interval) {
        final Handler h=new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                String url=getLastChannelEntryURL();
                JsonObjectRequest stringRequest=new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d(TAG, "update: " + response);
                                DataEntry entry = Util.gson.fromJson(response.toString(), DataEntry.class);
                                dataSet.addEntry(entry);
                                updateDataUI();
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d(TAG, "update: " + error.toString());
                            }
                        });
                queue.add(stringRequest);
                h.postDelayed(this, interval);
            }
        }, interval);
    }


    /**
     * Generates initial data for line chart. At the begining all Y values are equals 0. That will change when user
     * will select value on column chart.
     */
    private void generateInitialLineData() {
        int numValues=7;

        List<AxisValue> axisValues=new ArrayList<AxisValue>();
        List<PointValue> values=new ArrayList<PointValue>();
        for(int i=0; i<numValues; ++i) {
            values.add(new PointValue(i, 0));
            axisValues.add(new AxisValue(i).setLabel(days[i]));
        }

        Line line=new Line(values);
        line.setColor(ChartUtils.COLOR_GREEN).setCubic(true);

        List<Line> lines=new ArrayList<Line>();
        lines.add(line);

        lineData=new LineChartData(lines);
        lineData.setAxisXBottom(new Axis(axisValues).setHasLines(true));
        lineData.setAxisYLeft(new Axis().setHasLines(true).setMaxLabelChars(3));

        chartTop.setLineChartData(lineData);

        // For build-up animation you have to disable viewport recalculation.
        chartTop.setViewportCalculationEnabled(false);

        // And set initial max viewport and current viewport- remember to set viewports after data.
        Viewport v=new Viewport(0, 110, 6, 0);
        chartTop.setMaximumViewport(v);
        chartTop.setCurrentViewport(v);

        chartTop.setZoomType(ZoomType.HORIZONTAL);
    }

    private void generateColumnData() {

        int numSubcolumns=1;
        int numColumns=months.length;

        List<AxisValue> axisValues=new ArrayList<AxisValue>();
        List<Column> columns=new ArrayList<Column>();
        List<SubcolumnValue> values;
        for(int i=0; i<numColumns; ++i) {

            values=new ArrayList<SubcolumnValue>();
            for(int j=0; j<numSubcolumns; ++j) {
                values.add(new SubcolumnValue((float) Math.random() * 50f + 5, ChartUtils.pickColor()));
            }

            axisValues.add(new AxisValue(i).setLabel(months[i]));

            columns.add(new Column(values).setHasLabelsOnlyForSelected(true));
        }

        columnData=new ColumnChartData(columns);

        columnData.setAxisXBottom(new Axis(axisValues).setHasLines(true));
        columnData.setAxisYLeft(new Axis().setHasLines(true).setMaxLabelChars(2));

        chartBottom.setColumnChartData(columnData);

        // Set value touch listener that will trigger changes for chartTop.
        chartBottom.setOnValueTouchListener(new ValueTouchListener());

        // Set selection mode to keep selected month column highlighted.
        chartBottom.setValueSelectionEnabled(true);

        chartBottom.setZoomType(ZoomType.HORIZONTAL);

        // chartBottom.setOnClickListener(new View.OnClickListener() {
        //
        // @Override
        // public void onClick(View v) {
        // SelectedValue sv = chartBottom.getSelectedValue();
        // if (!sv.isSet()) {
        // generateInitialLineData();
        // }
        //
        // }
        // });

    }

    private void generateLineData(int color, float range) {
        // Cancel last animation if not finished.
        chartTop.cancelDataAnimation();

        // Modify data targets
        Line line=lineData.getLines().get(0);// For this example there is always only one line.
        line.setColor(color);
        for(PointValue value : line.getValues()) {
            // Change target only for Y value.
            value.setTarget(value.getX(), (float) Math.random() * range);
        }

        // Start new data animation with 300ms duration;
        chartTop.startDataAnimation(300);
    }

    private class ValueTouchListener implements ColumnChartOnValueSelectListener {

        @Override
        public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            generateLineData(value.getColor(), 100);
        }

        @Override
        public void onValueDeselected() {

            generateLineData(ChartUtils.COLOR_GREEN, 0);

        }
    }
}

