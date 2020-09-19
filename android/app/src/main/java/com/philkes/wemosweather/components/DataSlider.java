package com.philkes.wemosweather.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.philkes.wemosweather.R;

import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.PieChartView;

import java.util.ArrayList;
import java.util.List;

/**
 * Show Slider Value for Data using PieChartView
 */
public class DataSlider {
    public static final String BASE_URL="http://192.168.178.62/";
    private Context context;
    public static final int LABEL_SIZE=80;

    private PieChartView chart;
    private float maxValue;
    private int color;
    private String units;

    private RequestQueue queue;

    private String TAG="WeMosWeather";

    private float dividerSize;

    private TextView valueTxt;
    private TextView unitsTxt;

    public DataSlider(Context context,
                      PieChartView chart,
                      String label,
                      String units,
                      float maxValue,
                      int color,
                      TextView valueTxt,
                      TextView unitsTxt
    ) {
        this.chart=chart;
        this.maxValue=maxValue;
        this.context=context;
        this.color=color;
        this.dividerSize=this.maxValue/120;
        this.valueTxt=valueTxt;
        this.unitsTxt=unitsTxt;
        setUnits(units);
        initialize();
        setLabel(label);
    }

    private void initialize() {
        List<SliceValue> values=new ArrayList<SliceValue>();

        float tmp=0.0f;

        SliceValue sliceValue=new SliceValue(dividerSize, Color.parseColor("#000000"));
        sliceValue.setLabel("");
        values.add(sliceValue);

        sliceValue=new SliceValue(tmp, color);
        sliceValue.setLabel(tmp + units);
        values.add(sliceValue);

        sliceValue=new SliceValue(maxValue - tmp-dividerSize, Color.parseColor("#FFFFFF"));
        sliceValue.setLabel("");
        values.add(sliceValue);

        PieChartData data=new PieChartData(values);
        data.setHasLabels(false);
        data.setHasLabelsOutside(false);
        data.setHasCenterCircle(true);

        // Get roboto-italic font.
        //Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Italic.ttf");
        //data.setCenterText1Typeface(tf);

        // Get font size from dimens.xml and convert it to sp(library uses sp values).
        data.setCenterText1FontSize(ChartUtils.px2sp(context.getResources().getDisplayMetrics().scaledDensity,
                (int) LABEL_SIZE));
        data.setCenterText1Color(context.getResources().getColor(R.color.actionBarText));
        data.setCenterText2Color(context.getResources().getColor(R.color.actionBarText));
        //Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Italic.ttf");
        //data.setCenterText2Typeface(tf);
    /*    data.setCenterText2FontSize(ChartUtils.px2sp(context.getResources().getDisplayMetrics().scaledDensity,
                (int) LABEL_SIZE));*/
        data.setSlicesSpacing(0);
        chart.setValueTouchEnabled(false);
        chart.setChartRotationEnabled(false);
        chart.setChartRotation(Math.round(90-2*dividerSize), true);
        chart.setPieChartData(data);
    }

    public PieChartView getChart() {
        return chart;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public SliceValue getPieDataValue() {
        if(chart.getPieChartData()!=null && chart.getPieChartData().getValues().size()>1) {
            return chart.getPieChartData().getValues().get(1);
        }
        return null;
    }

    public DataSlider updateValue(float value) {
        SliceValue val=getPieDataValue();
        if(val==null) {
            return this;
        }
        //val.setValue(value);
        val.setTarget(value);
        val.setLabel(value + "");
        SliceValue fillValue=chart.getPieChartData().getValues().get(2);
        //fillValue.setValue(maxValue - value);
        fillValue.setTarget(maxValue - value-dividerSize);
        //chart.getPieChartData().setCenterText1(Math.round(value)+units);
        chart.getPieChartData().setCenterText1("");
        valueTxt.setText(Math.round(value)+"");
        unitsTxt.setText(units);

        //chart.getPieChartData().finish();
        /*chart.setPieChartData(chart.getPieChartData());*/
        chart.startDataAnimation();
        return this;
    }

    public float getCurrentValue() {
        SliceValue val=getPieDataValue();
        if(val==null) {
            return 0;
        }
        return val.getValue();
    }

    public DataSlider setColor(int color) {
        getPieDataValue().setColor(color);
        return this;
    }

    public DataSlider setLabel(String label1) {
        //chart.getPieChartData().setCenterText1(label1);
        TAG=label1;
        return this;
    }

    public DataSlider setUnits(String label2) {
        this.units=label2;
        return this;
    }

    public DataSlider setRequestQueue(RequestQueue queue) {
        this.queue=queue;
        return this;
    }

    /**
     * Updates Slider value using HTTP GET in given interval
     */
    public DataSlider setUpdateWithHTTP(final String dataUrl,final int interval) {
        final Handler h=new Handler();
        h.postDelayed(new Runnable() {

            @Override
            public void run() {
                String url=BASE_URL + dataUrl;
                StringRequest stringRequest=new StringRequest
                        (Request.Method.GET, url, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.d(TAG, "update: " + response+" "+units);
                                updateValue(Float.parseFloat(response));
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
        return this;
    }
}
