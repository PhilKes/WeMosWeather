package com.philkes.wemosweather;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.philkes.wemosweather.components.DataSlider;
import com.philkes.wemosweather.thingspeak.DataEntry;
import com.philkes.wemosweather.thingspeak.DataSet;
import com.philkes.wemosweather.thingspeak.Util;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SelectedValue;
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
    public static final int UPDATE_DELAY=60000;

    private DataSlider tempSlider;
    private DataSlider humSlider;

    private LineChartView chartTop;
    private ColumnChartView chartBottom;

    private DataSet dataSet;

    // 0: Thingspeak Channel
    // 1: HTTP WebServer
    public static final int UPDATE_SOURCE=0;
    private FrameLayout layoutProgressBar;

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
        chartBottom=findViewById(R.id.colChart1);
        layoutProgressBar=findViewById(R.id.layoutProgressBar);
        queue=Volley.newRequestQueue(this);
        showProgressBar();

        requestInitialThingspeakData();
    }

    private void showProgressBar() {
        layoutProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        layoutProgressBar.setVisibility(View.GONE);
    }

    /**
     * Requests and loads DataSet from Thingspeak channel data
     */
    private void requestInitialThingspeakData() {
        String url=getChannelFeedsURL();
        JsonObjectRequest stringRequest=new JsonObjectRequest
                (Request.Method.GET, url, null,
                        /** Load dataSet from Thingspeak data
                         * Setup Thingspeak updates*/
                        response -> {
                            Log.d(TAG, "initial Thingspeak Data: " + response);
                            dataSet=Util.gson.fromJson(response.toString(), DataSet.class);
                            generateColumnData();
                            setupThingspeakUpdates(UPDATE_DELAY);
                            updateDataUI();
                            hideProgressBar();
                        },
                        error -> {
                            Log.e(TAG, "initial Thingspeak Data: " + error.toString());
                            requestInitialThingspeakData();
                        });
        queue.add(stringRequest);
    }

    private void updateDataUI() {
        DataEntry currentData=dataSet.getCurrentData();
        tempSlider.updateValue(currentData.getTemperature());
        humSlider.updateValue(currentData.getHumidity());
        int firstIdx=chartBottom.getSelectedValue().getFirstIndex();
        int secIdx=chartBottom.getSelectedValue().getSecondIndex();
        SubcolumnValue subcolumnValue=chartBottom.getChartData().getColumns().get(firstIdx)
                .getValues().get(secIdx);
        generateLineData(firstIdx,subcolumnValue);

                ;
        Log.d(TAG, "updateDataUI: " + currentData);
    }

    /**
     * Updates Data from Thingspeak channel in an given interval
     */
    public void setupThingspeakUpdates(final int interval) {
        final Handler h=new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                String url=getLastChannelEntryURL();
                JsonObjectRequest stringRequest=new JsonObjectRequest
                        (Request.Method.GET, url, null, response -> {
                            Log.d(TAG, "update: " + response);
                            DataEntry entry=Util.gson.fromJson(response.toString(), DataEntry.class);
                            dataSet.addEntry(entry);
                            updateDataUI();
                        }, error -> Log.d(TAG, "update: " + error.toString()));
                queue.add(stringRequest);
                h.postDelayed(this, interval);
            }
        }, interval);
    }


    private void generateColumnData() {
        ColumnChartData columnData=Util.generateColumnData(dataSet);

        chartBottom.setColumnChartData(columnData);

        // Set value touch listener that will trigger changes for chartTop.
        chartBottom.setOnValueTouchListener(new ValueTouchListener());

        // Set selection mode to keep selected month column highlighted.
        chartBottom.setValueSelectionEnabled(true);
        Viewport v=new Viewport(-1, 50, 7, 0);
        chartBottom.setMaximumViewport(v);
        chartBottom.setCurrentViewport(v);

        chartBottom.setZoomEnabled(false);
        chartBottom.selectValue(new SelectedValue(0, 0, SelectedValue.SelectedValueType.COLUMN));
    }

    private void generateLineData(int dayIndex, SubcolumnValue dayColumn) {
        // Cancel last animation if not finished.
        chartTop.cancelDataAnimation();

        DataSet.DayData dayData=dataSet.getDayNumberData(dayIndex);
        LineChartData lineChartData=Util.generateLineData(dayData,dayColumn.getColor());
        chartTop.setLineChartData(lineChartData);

        // For build-up animation you have to disable viewport recalculation.
        chartTop.setViewportCalculationEnabled(false);
        int valueSize=lineChartData.getLines().get(0).getValues().size();
        // And set initial max viewport and current viewport- remember to set viewports after data.
        Viewport v=new Viewport(valueSize - 10, 50, valueSize, 0);
        Viewport vMax=new Viewport(0, 50, valueSize, 0);
        chartTop.setMaximumViewport(vMax);
        chartTop.setCurrentViewport(v);

        chartTop.setZoomType(ZoomType.HORIZONTAL);

       /*
        for(PointValue value : line.getValues()) {
            // Change target only for Y value.
            value.setTarget(value.getX(), (float) Math.random() * range);
        }*/

        // Start new data animation with 300ms duration;
        chartTop.startDataAnimation(300);
    }

    private class ValueTouchListener implements ColumnChartOnValueSelectListener {

        @Override
        public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            generateLineData(columnIndex, value);
        }

        @Override
        public void onValueDeselected() {

            //generateLineData();

        }
    }
}

