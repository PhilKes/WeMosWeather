package com.philkes.wemosweather;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.philkes.wemosweather.components.DataSlider;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

public class MainActivity extends AppCompatActivity {

    public static final String TAG="WeMosWeather";

    private DataSlider tempSlider;
    private DataSlider humSlider;

    public final static String[] months=new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug",
            "Sep", "Oct", "Nov", "Dec",};

    public final static String[] days=new String[]{"Mon", "Tue", "Wen", "Thu", "Fri", "Sat", "Sun",};

    private LineChartView chartTop;
    private ColumnChartView chartBottom;

    private LineChartData lineData;
    private ColumnChartData columnData;

    MqttClient mqttAndroidClient;

    final String serverUri = "tcp://mqtt.thingspeak.com:1883";

    String clientId =MqttClient.generateClientId();
    final String READ_API_KEY="TZDBU82CN85PWA1V";
    final String CHANNEL_ID="1093516";
    private static final String MQTT_API_KEY="9U4L9Q0OIRT3M4SR";

    final String subscriptionTopic = "channels/"+CHANNEL_ID+"/subscribe/json/"+READ_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RequestQueue queue=Volley.newRequestQueue(this);

        tempSlider=new DataSlider(
                this,
                (PieChartView) findViewById(R.id.tempPie),
                "Temperature",
                "C°",
                50f,
                ChartUtils.COLOR_RED)
                .setRequestQueue(queue)
                .setUpdateWithHTTP("temperature", 3000);

        humSlider=new DataSlider(this,
                (PieChartView) findViewById(R.id.humPie),
                "Humidity",
                "%",
                100f,
                ChartUtils.COLOR_BLUE)
                .setRequestQueue(queue)
                .setUpdateWithHTTP("humidity", 3000);

        chartTop=findViewById(R.id.lineChart1);
        generateInitialLineData();
        chartBottom=findViewById(R.id.colChart1);
        generateColumnData();

    /*    try {
            setupThingspeakMQTT();
        }
        catch(MqttException e) {
            e.printStackTrace();
        }*/
    }

    private void setupThingspeakMQTT() throws MqttException {
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(System.getProperty("java.io.tmpdir"));

        mqttAndroidClient = new MqttClient(serverUri, clientId,dataStore);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "connectionLost: The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "messageArrived:Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        //mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
       mqttConnectOptions.setPassword((MQTT_API_KEY).toCharArray());
        mqttConnectOptions.setUserName(clientId);
        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions);


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.d(TAG, "messageArrived: "+topic+"msg: "+message);
                }
            });

            /*// THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    Log.d(TAG, "Message: " + topic + " : " + new String(message.getPayload()));
                }
            });*/

        } catch (MqttException ex){
            Log.e(TAG, "subscribeToTopic: Exception whilst subscribing");
            ex.printStackTrace();
        }
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

