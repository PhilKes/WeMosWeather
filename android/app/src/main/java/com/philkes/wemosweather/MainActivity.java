package com.philkes.wemosweather;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.philkes.wemosweather.components.DataSlider;
import com.philkes.wemosweather.thingspeak.DataEntry;
import com.philkes.wemosweather.thingspeak.DataSet;
import com.philkes.wemosweather.thingspeak.Util;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.ColumnChartData;
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
    public static final boolean DEBUG_LAYOUT=false;

    public static final String TAG="WeMosWeather";
    public static final int UPDATE_DELAY=20000;

    private DataSlider tempSlider;
    private DataSlider humSlider;

    private LineChartView chartTop;
    private ColumnChartView chartBottom;

    public static DataSet dataSet;

    private DataSet.DayData selectedDayData=null;
    private DataEntry selectedEntry=null;

    // 0: Thingspeak Channel
    // 1: HTTP WebServer
    public static final int UPDATE_SOURCE=0;
    private FrameLayout layoutProgressBar;

    RequestQueue queue;

    public TextView textTime;
    public TextView textDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Util.loadValues(getResources());
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

        textTime=findViewById(R.id.textTime);
        textDate=findViewById(R.id.textDate);

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
        if(DEBUG_LAYOUT) {
            dataSet=Util.generateTestDataSet(100, 100, -10, 35);
            updateDataUI();
            hideProgressBar();
        }
        else {
            String url=getChannelFeedsURL();
            JsonObjectRequest stringRequest=new JsonObjectRequest
                    (Request.Method.GET, url, null,
                            /** Load dataSet from Thingspeak data
                             * Setup Thingspeak updates*/
                            response -> {
                                Log.d(TAG, "initial Thingspeak Data: " + response);
                                dataSet=Util.gson.fromJson(response.toString(), DataSet.class);
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
    }

    private void updateDataUI() {
        DataEntry currentData=dataSet.getLatestData();
        tempSlider.updateValue(currentData.getTemperature());
        humSlider.updateValue(currentData.getHumidity());
        generateDaysValues();
        int firstIdx=chartBottom.getSelectedValue().getFirstIndex();
        int secIdx=chartBottom.getSelectedValue().getSecondIndex();
        SubcolumnValue subcolumnValue=chartBottom.getChartData().getColumns().get(firstIdx)
                .getValues().get(secIdx);
        generateTimeValues(firstIdx, subcolumnValue);
        chartBottom.selectValue(new SelectedValue(chartBottom.getChartData().getColumns().size() - 1, 0, SelectedValue.SelectedValueType.COLUMN));
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
                            if(dataSet.addEntry(entry)) {
                                updateDataUI();
                            }
                        }, error -> Log.d(TAG, "update: " + error.toString()));
                queue.add(stringRequest);
                h.postDelayed(this, interval);
            }
        }, interval);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.main_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actions_history:
                Intent intent=new Intent(this, HistoryActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void generateDaysValues() {
        ColumnChartData columnData=Util.generateColumnData(dataSet);

        chartBottom.setColumnChartData(columnData);

        // Set value touch listener that will trigger changes for chartTop.
        chartBottom.setValueSelectionEnabled(true);
        chartBottom.setOnValueTouchListener(new DayColumnValueListener());

        int valueSize=columnData.getColumns().size();
        //Viewport v2=new Viewport(-1, Util.TEMP_MAX + 4, 7, 0);
        Viewport v=new Viewport(valueSize - 7, 50, valueSize, Util.TEMP_MIN);
        Viewport vMax=new Viewport(-1, 50, valueSize, Util.TEMP_MIN);

        chartBottom.setMaximumViewport(vMax);
        chartBottom.setCurrentViewport(v);
        chartBottom.setZoomEnabled(false);
        chartBottom.selectValue(new SelectedValue(columnData.getColumns().size() - 1, 0, SelectedValue.SelectedValueType.COLUMN));
    }

    private void generateTimeValues(int dayIndex, SubcolumnValue dayColumn) {
        // Cancel last animation if not finished.
        chartTop.cancelDataAnimation();

        DataSet.DayData dayData=dataSet.getDayNumberData(dayIndex);
        LineChartData lineChartData=Util.generateLineData(dayData, dayColumn.getColor());

        chartTop.setLineChartData(lineChartData);

        // For build-up animation you have to disable viewport recalculation.
        chartTop.setViewportCalculationEnabled(false);
        int valueSize=lineChartData.getLines().get(0).getValues().size();
        // And set initial max viewport and current viewport- remember to set viewports after data.
        Viewport v=new Viewport(valueSize - 10, 50, valueSize, Util.TEMP_MIN);
        Viewport vMax=new Viewport(0, 50, valueSize, Util.TEMP_MIN);
        chartTop.setMaximumViewport(vMax);
        chartTop.setCurrentViewport(v);

        chartTop.setZoomType(ZoomType.HORIZONTAL);
        chartTop.setZoomEnabled(false);

        chartTop.setValueSelectionEnabled(true);
        chartTop.setOnValueTouchListener(new EntryPointValueListener());

        chartTop.startDataAnimation(300);
    }

    public void displaySelectedEntry() {
        tempSlider.updateValue(selectedEntry.getTemperature());
        humSlider.updateValue(selectedEntry.getHumidity());
        textTime.setText(selectedEntry.getTimeString());
        textDate.setText(selectedDayData.getLabel());
    }

    private class DayColumnValueListener implements ColumnChartOnValueSelectListener {

        @Override
        public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
            generateTimeValues(columnIndex, value);
            selectedDayData=dataSet.getDayNumberData(columnIndex);
            chartTop.selectValue(new SelectedValue(0, chartTop.getLineChartData().getLines().get(0).getValues().size() - 1, SelectedValue.SelectedValueType.LINE));
        }

        @Override
        public void onValueDeselected() {
            //generateLineData();
        }
    }

    private class EntryPointValueListener implements LineChartOnValueSelectListener {

        @Override
        public void onValueSelected(int lineIdx, int pointIdx, PointValue pointValue) {
            System.out.println(String.format("Selected %d , %d of" + pointValue, lineIdx, pointIdx));
            selectedEntry=selectedDayData.getEntryNumberData(pointIdx);
            displaySelectedEntry();

        }

        @Override
        public void onValueDeselected() {

        }
    }
}

