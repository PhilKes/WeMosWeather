package com.philkes.wemosweather;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.philkes.wemosweather.thingspeak.DataEntry;
import com.philkes.wemosweather.thingspeak.DataSet;
import com.philkes.wemosweather.thingspeak.Util;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.SelectedValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;

import static com.philkes.wemosweather.thingspeak.Util.getChannelFeedsURL;

public class HistoryActivity extends AppCompatActivity {
    public static final String TAG="WeMosWeather_History";

    private LineChartView chartTemp;
    private LineChartView chartHum;
    private LineChartView chartPress;
    private LineChartView chartBright;

    private DataSet dataSet;
    RequestQueue queue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        chartTemp=findViewById(R.id.lineChartTemp);
        chartHum=findViewById(R.id.lineChartHum);
        chartPress=findViewById(R.id.lineChartPress);
        chartBright=findViewById(R.id.lineChartBright);
        queue=Volley.newRequestQueue(this);

        dataSet=MainActivity.dataSet;
        updateDataUI();
       /* String url=getChannelFeedsURL();
        JsonObjectRequest stringRequest=new JsonObjectRequest
                (Request.Method.GET, url, null,
                        *//** Load dataSet from Thingspeak data
                         * Setup Thingspeak updates*//*
                        response -> {
                            Log.d(TAG, "History Thingspeak Data: " + response);
                            dataSet=Util.gson.fromJson(response.toString(), DataSet.class);
                            updateDataUI();
                        },
                        error -> {
                            Log.e(TAG, "initial Thingspeak Data: " + error.toString());

                        });
        queue.add(stringRequest);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.history_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actions_back:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    public LineChartView getFieldChart(String field) {
        switch(field) {
            case "TEMP":
                return chartTemp;
            case "HUM":
                return chartHum;
            case "PRESS":
                return chartPress;
            case "BRIGHT":
                return chartBright;

        }
        return null;
    }

    public int getFieldColor(String field) {
        switch(field) {
            case "TEMP":
                return ChartUtils.COLOR_RED;
            case "HUM":
                return ChartUtils.COLOR_BLUE;
            case "PRESS":
                return ChartUtils.COLOR_ORANGE;
            case "BRIGHT":
                return ChartUtils.COLOR_VIOLET;

        }
        return 255;
    }

    public int getFieldMax(String field) {
        switch(field) {
            case "TEMP":
                return 50;
            case "HUM":
                return ChartUtils.COLOR_BLUE;
            case "PRESS":
                return ChartUtils.COLOR_ORANGE;
            case "BRIGHT":
                return ChartUtils.COLOR_VIOLET;

        }
        return 255;
    }

    private void updateDataUI() {

        initFieldChart("TEMP");
        initFieldChart("HUM");
        initFieldChart("PRESS");
        initFieldChart("BRIGHT");
    }

    private void initFieldChart(String field) {
        LineChartView chartTemp=getFieldChart(field);

        chartTemp.cancelDataAnimation();

        LineChartData fieldChartData=Util.generateHistoryData(dataSet, field, 7, getFieldColor(field));

        chartTemp.setLineChartData(fieldChartData);

        chartTemp.setViewportCalculationEnabled(true);
        int valueSize=fieldChartData.getLines().get(0).getValues().size();
      /*  Viewport v=new Viewport(0, 50, valueSize, 0);
        Viewport vMax=new Viewport(0, 50, valueSize, 0);
        chartTemp.setMaximumViewport(vMax);
        chartTemp.setCurrentViewport(v);*/

        chartTemp.setZoomType(ZoomType.HORIZONTAL);
        chartTemp.setZoomEnabled(false);

        chartTemp.setValueSelectionEnabled(true);

        chartTemp.startDataAnimation(300);
    }
}
