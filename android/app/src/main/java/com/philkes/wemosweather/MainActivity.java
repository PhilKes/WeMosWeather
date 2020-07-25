package com.philkes.wemosweather;

import android.os.Bundle;

import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.philkes.wemosweather.components.DataSlider;

import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.PieChartView;

public class MainActivity extends AppCompatActivity {

    public static final String TAG="WeMosWeather";

    private DataSlider tempSlider;
    private DataSlider humSlider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RequestQueue queue=Volley.newRequestQueue(this);

        tempSlider=new DataSlider(this, (PieChartView) findViewById(R.id.tempPie), 50f, ChartUtils.COLOR_RED)
                .setLabel("Temperature").setUnits("C°")
                .setRequestQueue(queue)
                .setUpdateWithHTTP("temperature", 3000);

        humSlider=new DataSlider(this, (PieChartView) findViewById(R.id.humPie), 100f, ChartUtils.COLOR_BLUE)
                .setLabel("Humidity").setUnits("%")
                .setRequestQueue(queue)
                .setUpdateWithHTTP("humidity", 3000);


    }


}
