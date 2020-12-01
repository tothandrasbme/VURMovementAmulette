package com.dairyroadsolutions.accelplot;

import static java.lang.Math.pow;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import org.w3c.dom.Node;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;


import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends Activity implements DataClient.OnDataChangedListener,
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener{

    private static final int TRACE_COUNT = 12;
    private static final int CHART_COLUMN_COUNT = 1;
    private static final int SCREEN_BUFFER_COUNT = 3000;
    private static final int LINE_THICKNESS = 3;
    private static final boolean CYCLIC = true;


    private SocketServer sockServer;

    // I truncated the accelerometer outputs from 2^15 bits to 2^12 bits to allow for the
    // address to included in the 2-byte structure. See "Firmware.ino" for the implementation
    // details
    private static float fAccelCountsPerG = 1024.0f;
    private static final float F_ADC_COUNTS_PER_VOLT = 1024.0f/5.0f;

    // The plot area for each trace has to be scaled to +1 to -1
    private static final float F_SCALE_FACTOR_ACC = 0.50f/2048.0f;
    private static final float F_SCALE_FACTOR_GYRO = 0.25f/1024f;

    // Grid controls. It works best if they are even numbers
    private static final int I_DIVISIONS_X = 20;
    private static final int I_DIVISIONS_Y = 4;
    private static float dGPerDiv = 0.0f;
    private static final float V_PER_DIV =(0.5f/F_SCALE_FACTOR_GYRO)/(I_DIVISIONS_Y*F_ADC_COUNTS_PER_VOLT);
    private float fTimePerDiv = 0;
    TextView[] tvTrace = new TextView[TRACE_COUNT+1];
    private int iLabelSize;

    // Chart trace controls
    private GLSurfaceView glChartSurfaceView;
    private float fChScale[];
    private float fChOffset[];
    private Button _bDisconnect;
    private TextView _tvControl;
    private ToggleButton _tbStream;
    private TextView _tvDataStorage;
    private ToggleButton _tbSaveData;
    private ToggleButton _tbDirectionData;
    private TextView _tvAudioOut;
    private ToggleButton _tbAudioOut;
    private TextView _tvCh1;
    private RadioGroup _rgCh1;
    private TextView _tvCh2;
    private RadioGroup _rgCh2;
    private TextView _tvArduino;
    private Button _bInst;
    private Spinner _spFreq;
    private Spinner _spAccRange;
    private TextView _tvFile;
    private EditText _etFileSamples;
    private int sensorDirection; // 0 - external , 1 - internal

    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private float lastX, lastY, lastZ;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;
    private static double dSampleFreq = 250.0;

    public static int iFileSampleCount = (int) dSampleFreq * 60;
    public static float[] fX_Acce_internal = new float[iFileSampleCount];
    public static float[] fY_Acce_internal = new float[iFileSampleCount];
    public static float[] fZ_Acce_internal = new float[iFileSampleCount];

    public static float[] fX_Acce_watch = new float[iFileSampleCount];
    public static float[] fY_Acce_watch = new float[iFileSampleCount];
    public static float[] fZ_Acce_watch = new float[iFileSampleCount];

    public static float x_current_Acce_watch = 0.0f;
    public static float y_current_Acce_watch = 0.0f;
    public static float z_current_Acce_watch = 0.0f;

    //This channel can be a gyro or the ADC, depending how how the firmware is configured
    //in the Arduino
    public static float[] fX_Gyro_internal = new float[iFileSampleCount];
    public static float[] fY_Gyro_internal = new float[iFileSampleCount];
    public static float[] fZ_Gyro_internal = new float[iFileSampleCount];

    public static float[] fX_Gyro = new float[iFileSampleCount];
    public static float[] fY_Gyro = new float[iFileSampleCount];
    public static float[] fZ_Gyro = new float[iFileSampleCount];

    public static float[] fX_Accel2 = new float[iFileSampleCount];
    public static float[] fY_Accel2 = new float[iFileSampleCount];
    public static float[] fZ_Accel2 = new float[iFileSampleCount];

    //This channel can be a gyro or the ADC, depending how how the firmware is configured
    //in the Arduino
    public static float[] fX_Gyro2 = new float[iFileSampleCount];
    public static float[] fY_Gyro2 = new float[iFileSampleCount];
    public static float[] fZ_Gyro2 = new float[iFileSampleCount];

    public static int idxBuff = 0;
    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;
    protected Handler myHandler;
    public static final String SEND_ACCELERATION_DATA_MESSAGE_PATH = "/acceleration_dataexploration";

    public ChartRenderer classChartRenderer;

    //Define a nested class that extends BroadcastReceiver//

    public class Receiver extends BroadcastReceiver {
        @Override

        public void onReceive(Context context, Intent intent) {

            //Upon receiving each message from the wearable, display the following text//

            String message = "I just received a message from the wearable " + receivedMessageNumber++;;

            //myLabel.append(message + "\r\n");

        }
    }

    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);

    }

    private FilterHelper filter = new FilterHelper();

    // Data writing controls
    private boolean bWriteLocal = false;

    // debug
    private static final String strTag = MainActivity.class.getSimpleName();

    // Application preferences
    private static SharedPreferences sharedPref;

    // Arduino values are stored in the Bluetooth.java object
    // BananaPi values are stored in the SocketServer.java object

    // NOTE: All sampling and filtering information is defined
    // in the Bluetooth.java class or Socketserver.

    // Data write is in the Bluetooth class
    // Data write is also in the SocketServer class

    /**
     * Set the channel scale factor.  Each trace could have a different scale
     * factor, but for this instance all traces will be set to the same value.
     * @param fScaleFactor Universal scale factor
     */
    public void setChScale(float fScaleFactor){

        // Populate the array with the scale factors for the accelerometer channels
        for( int idx = 0; idx < (TRACE_COUNT-1); ++idx) {
            fChScale[idx] = fScaleFactor/(TRACE_COUNT +1.0f);
        }

        // The scale factor for the gyro/ADC channel is set separately from the accels
        fChScale[TRACE_COUNT-1]=F_SCALE_FACTOR_GYRO;

        // Update dependent objects
        //Bluetooth.classChartRenderer.setChScale(fChScale);
        classChartRenderer.setChScale(fChScale);
        // ToDo: Check Socketserver action here

    }

    /**
     * Set the channel offsets to avoid overlaying the data
     */
    public void setChOffset(){

        // ToDo: Check Socketserver action here

        //float fIncrement = Bluetooth.classChartRenderer.classChart.classTraceHelper.getTraceIncrement();
        //float fStart = Bluetooth.classChartRenderer.classChart.classTraceHelper.getTraceStart();
        float fIncrement = classChartRenderer.classChart.classTraceHelper.getTraceIncrement();
        float fStart = classChartRenderer.classChart.classTraceHelper.getTraceStart();

        // Populate the array with the scale factors
        for( int idx = 0; idx< TRACE_COUNT; ++idx) {
            fChOffset[idx] = fStart;
            fStart = fStart + fIncrement;
        }

        // Update the dependent objects
        //Bluetooth.classChartRenderer.setChOffset(fChOffset);
        classChartRenderer.setChOffset(fChOffset);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Setup the gl surface
        LinearLayout llControlLayer = (LinearLayout)findViewById(R.id.Chart);
        glChartSurfaceView = new GLSurfaceView(this);
        glChartSurfaceView.setEGLConfigChooser(false);
        llControlLayer.addView(glChartSurfaceView);


        // Scale the location of the grid division labels
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int iHeightDisp = displaymetrics.heightPixels;
//        int width = displaymetrics.widthPixels;

        // We have to scale the labels for smaller displays
        iLabelSize = 15;
        if( iHeightDisp < 750 ){
            iLabelSize = (int)((float)iHeightDisp / 50.0f);
        }


        // Add the vertical axis labels
        FrameLayout flTemp = (FrameLayout)findViewById(R.id.flChartStuff);

        int idxText;
        for( idxText = 0; idxText<=TRACE_COUNT; ++idxText){
            tvTrace[idxText] = new TextView(this);
            tvTrace[idxText].setText("");
            tvTrace[idxText].setTextSize(iLabelSize);
            tvTrace[idxText].setPadding((iLabelSize/2)+1,(iLabelSize/2)+1, 0, 0);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP);
            flTemp.addView(tvTrace[idxText], params);

        }

        // Horizontal axis labels
        --idxText;
        tvTrace[idxText].setPadding((iLabelSize/2)+1,(iLabelSize/2)+1, (iLabelSize/2)+1, (iLabelSize/2)+1);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, (Gravity.BOTTOM | Gravity.RIGHT) );
        tvTrace[idxText].setLayoutParams(params);

        // User prefs
        sharedPref = getPreferences(Context.MODE_PRIVATE);

        // Flags and initialization of the BlueTooth object
        // ToDo: Check Socketserver action here
        //Bluetooth.samplesBuffer=new SamplesBuffer[TRACE_COUNT];
        //Bluetooth.vSetWriteLocal(bWriteLocal);

        sockServer = new SocketServer();
        sockServer.setWritelocalFlagsAtThreads(bWriteLocal);

        classChartRenderer = new ChartRenderer(this,SCREEN_BUFFER_COUNT,sockServer.samplesBuffer, TRACE_COUNT);
        classChartRenderer.setCyclic(CYCLIC);
        classChartRenderer.bSetDivisionsX(I_DIVISIONS_X);
        classChartRenderer.bSetDivisionsY(I_DIVISIONS_Y);

        sockServer.setChartRendererClass(classChartRenderer);


        // Flags for the disconnet button
        _bDisconnect = (Button)findViewById(R.id.bDisconnect);
        _bDisconnect.setEnabled(false);
        _bDisconnect.setVisibility(View.GONE);

        // Flags for the data stream button
        _tbStream = (ToggleButton)findViewById(R.id.tbStream);
        _tbStream.setEnabled(true);
        _tbStream.setVisibility(View.VISIBLE);
        _tvControl = (TextView) findViewById(R.id.tvControl);
        _tvControl.setVisibility(View.VISIBLE);

        // Flags for the data save button
        _tbSaveData = (ToggleButton)findViewById(R.id.tbSaveData);
        _tbSaveData.setEnabled(true);
        _tbSaveData.setVisibility(View.VISIBLE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(mSensorListener,accelSensor,SensorManager.SENSOR_DELAY_NORMAL);

        sensorDirection = 0; // 0 - external, 1 - internal


        // Flags for the data direction button
        _tbDirectionData = (ToggleButton)findViewById(R.id.tbDirectionData);
        _tbDirectionData.setEnabled(true);
        _tbDirectionData.setVisibility(View.VISIBLE);

        _tvDataStorage = (TextView)findViewById(R.id.tvDataStorage);
        _tvDataStorage.setVisibility(View.VISIBLE);

        // Flags and init for the audio out
        _tbAudioOut = (ToggleButton)findViewById(R.id.tbAudioOut);
        _tbAudioOut.setEnabled(false);
        _tbAudioOut.setVisibility(View.GONE);

        _tvAudioOut = (TextView)findViewById(R.id.tvAudioOut);
        _tvAudioOut.setVisibility(View.GONE);

        // Initialze audio mappings
        _tvCh1 = (TextView)findViewById(R.id.tvCh1);
        _tvCh2 = (TextView)findViewById(R.id.tvCh2);
        _rgCh1 = (RadioGroup)findViewById(R.id.radio_ADC_to_Ch1);
        _rgCh2 = (RadioGroup)findViewById(R.id.radio_ADC_to_Ch2);

        for(int i=0;i< TRACE_COUNT;i++)
        {
            //Bluetooth.samplesBuffer[i] = new SamplesBuffer(SCREEN_BUFFER_COUNT, true);
            // ToDo: Check Socketserver action here
            sockServer.samplesBuffer[i] = new SamplesBuffer(SCREEN_BUFFER_COUNT, true);
        }

        // Arduino control mappings
        _tvArduino = (TextView)findViewById(R.id.tvArduino);
        _bInst = (Button)findViewById(R.id.bInst);
        _spFreq = (Spinner)findViewById(R.id.spFreq);
        _spAccRange = (Spinner)findViewById(R.id.spAccRange);
        _tvFile = (TextView)findViewById(R.id.tvFile);
        _etFileSamples = (EditText)findViewById(R.id.etFileSamples);
        _etFileSamples.setFilters(new InputFilter[]{ new InputFilterMinMax("1","7200000")});
        List<String> listFreq = new ArrayList<String>();
        for( int iOCRA = 1; iOCRA<256; ++iOCRA){
            listFreq.add(strListItem(dGetFreq(256, iOCRA), iOCRA));
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.freq_spinner, listFreq);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spFreq.setAdapter(dataAdapter);

        List<String> listAccRange = new ArrayList<String>();
        listAccRange.add("MPU6050_ACCEL_FS_2");
        listAccRange.add("MPU6050_ACCEL_FS_4");
        listAccRange.add("MPU6050_ACCEL_FS_8");
        listAccRange.add("MPU6050_ACCEL_FS_16");
        ArrayAdapter<String> accAdapter = new ArrayAdapter<String>(this,
                R.layout.freq_spinner, listAccRange);
        accAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        _spAccRange.setAdapter(accAdapter);
        vUpdateArduinoControls(false);

        // Set controls values
        vGetUserPrefs();
        vUpdateChMapsEnabled(false);

        // ToDo: check the SocketServer here
        //Bluetooth.classChartRenderer = new ChartRenderer(this,SCREEN_BUFFER_COUNT,Bluetooth.samplesBuffer, TRACE_COUNT);
        //Bluetooth.classChartRenderer.setCyclic(CYCLIC);
        //Bluetooth.classChartRenderer.bSetDivisionsX(I_DIVISIONS_X);
        //Bluetooth.classChartRenderer.bSetDivisionsY(I_DIVISIONS_Y);

        fChScale = new float[TRACE_COUNT];
        setChScale(F_SCALE_FACTOR_ACC);

        fChOffset = new float[TRACE_COUNT];
        setChOffset();

        // Line thickness
        //Bluetooth.classChartRenderer.setThickness(LINE_THICKNESS);
        classChartRenderer.setThickness(LINE_THICKNESS);

        // Number of columns of chart data
        //Bluetooth.classChartRenderer.setChartColumnCount(CHART_COLUMN_COUNT);
        classChartRenderer.setChartColumnCount(CHART_COLUMN_COUNT);

        //glChartSurfaceView.setRenderer(Bluetooth.classChartRenderer);
        glChartSurfaceView.setRenderer(classChartRenderer);

        // Initialize the Bluetooth object
        init();

        // Initialize the buttons
        ButtonInit();

        // Debug code to test display of screen buffers
        for( int idx=0; idx<SCREEN_BUFFER_COUNT; ++idx){
            //Bluetooth.samplesBuffer[1].addSample((float)(idx>>2));
            sockServer.samplesBuffer[1].addSample((float)(idx>>2));
        }

        // Testing calls here. Remove comments to run them.
        filter.TestHarness();
        //Bluetooth.samplesBuffer[0].TestHarness();
        sockServer.samplesBuffer[0].TestHarness();


        myHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                messageText(stuff.getString("messageText"));
                return true;
            }
        });


        //Register to receive local broadcasts, which we'll be creating in the next step//

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

    }

    @Override
    public void onResume() {
        super.onResume();

        // Instantiates clients without member variables, as clients are inexpensive to create and
        // won't lose their listeners. (They are cached and shared between GoogleApi instances.)
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    public void onPause() {
        super.onPause();

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("Main: ", "onDataChanged: " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d("Main: ", "DataItem Changed" + event.getDataItem().toString());
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d("Main: ", "DataItem Changed" + event.getDataItem().toString());
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(
                "Main: ",
                "onMessageReceived() A message from watch was received:"
                        + messageEvent.getRequestId()
                        + " "
                        + messageEvent.getPath());

        String messageContent = new String(messageEvent.getData(), StandardCharsets.UTF_8);

        Log.d("Main: ", "Message from watch" + messageContent);
        // Process acceleration value from the watch

        String[] accelDatas = messageContent.substring(1,messageContent.length()-2).split(",");

        try {
            x_current_Acce_watch = (float)((float) (Float.parseFloat(accelDatas[0])) * 1000 + 4000);
        } catch (NumberFormatException nfe) {
            Log.d("Main: ", "Cannot parse message from wearable (" + nfe.getMessage() + ")");
        }

        try {
            y_current_Acce_watch = (float)((float) (Float.parseFloat(accelDatas[1])) * 1000 + 4000);
        } catch (NumberFormatException nfe) {
            Log.d("Main: ", "Cannot parse message from wearable (" + nfe.getMessage() + ")");
        }

        try {
            z_current_Acce_watch = (float)((float) (Float.parseFloat(accelDatas[2])) * 1000 + 4000);
        } catch (NumberFormatException nfe) {
            Log.d("Main: ", "Cannot parse message from wearable (" + nfe.getMessage() + ")");
        }


        /*dataPayload = "[" + Float.toString(event.values[0]) +
                        "," + Float.toString(event.values[1]) +
                        "," + Float.toString(event.values[2]) + "]";*/

        /*Log.d("Main: ","Acceleration internal raw: " + event.values[0] + "   ,"  + event.values[1] + "   ,"  + event.values[2] + "Acceleration changes: " + deltaX + "   ,"  + deltaY + "   ,"  + deltaZ );

            // get the change of the x,y,z values of the accelerometer
            deltaX = Math.abs(lastX - event.values[0]);
            deltaY = Math.abs(lastY - event.values[1]);
            deltaZ = Math.abs(lastZ - event.values[2]);


            // if the change is below 2, it is just plain noise
            if (deltaX < 2)
                deltaX = 0;
            if (deltaY < 2)
                deltaY = 0;
            if (deltaZ < 2)
                deltaZ = 0;

            // set the last know values of x,y,z
            lastX = event.values[0];
            lastY = event.values[1];
            lastZ = event.values[2];

            fX_Acce_internal[idxBuff] = (float)((float) (event.values[0]) * 500 + 4000);

            fY_Acce_internal[idxBuff] = (float)((float) (event.values[1]) * 500 + 4000);

            fZ_Acce_internal[idxBuff] = (float)((float) (event.values[2]) * 500 + 4000);
*/

    }

    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        Log.d("Main: ", "onCapabilityChanged: " + capabilityInfo);
    }

    @WorkerThread
    private void sendStartActivityMessage(String node) {

        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(this).sendMessage(node, SEND_ACCELERATION_DATA_MESSAGE_PATH, new byte[0]);

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            Log.d("Main: ", "Message sent: " + result);

        } catch (ExecutionException exception) {
            Log.e("Main: ", "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e("Main: ", "Interrupt occurred: " + exception);
        }
    }

    class SendMessage extends Thread {
        String path;
        String message;

//Constructor for sending information to the Data Layer//

        SendMessage() {
        }

        public void run() {

//Retrieve the connected devices//

            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

//Block on a task and get the result synchronously//

                List<Node> nodes = Tasks.await(nodeListTask);
                for (Node node : nodes) {

                    sendStartActivityMessage(node.getId());

                }

            } catch (ExecutionException exception) {

            } catch (InterruptedException exception) {

            }
        }
    }


    private SensorEventListener mSensorListener = new SensorEventListener() {

        private static final float F_OFFSET_COUNT = 4095.0f;

        @Override
        public void onSensorChanged(SensorEvent event) {
            //Log.d("Main: ","Acceleration internal raw: " + event.values[0] + "   ,"  + event.values[1] + "   ,"  + event.values[2] + "Acceleration changes: " + deltaX + "   ,"  + deltaY + "   ,"  + deltaZ );

                // get the change of the x,y,z values of the accelerometer
                deltaX = Math.abs(lastX - event.values[0]);
                deltaY = Math.abs(lastY - event.values[1]);
                deltaZ = Math.abs(lastZ - event.values[2]);


                // if the change is below 2, it is just plain noise
                if (deltaX < 2)
                    deltaX = 0;
                if (deltaY < 2)
                    deltaY = 0;
                if (deltaZ < 2)
                    deltaZ = 0;

                // set the last know values of x,y,z
                lastX = event.values[0];
                lastY = event.values[1];
                lastZ = event.values[2];

                fX_Acce_internal[idxBuff] = (float) ((float) (event.values[0]) * 500 + 4000);

                fY_Acce_internal[idxBuff] = (float) ((float) (event.values[1]) * 500 + 4000);

                fZ_Acce_internal[idxBuff] = (float) ((float) (event.values[2]) * 500 + 4000);

                fX_Acce_watch[idxBuff] = x_current_Acce_watch;

                fY_Acce_watch[idxBuff] = y_current_Acce_watch;

                fZ_Acce_watch[idxBuff] = z_current_Acce_watch;

            if (sensorDirection == 1) { // 0 - external, 1- internal
                classChartRenderer.classChart
                        .addSample(fX_Acce_internal[idxBuff] - F_OFFSET_COUNT, 0);
                classChartRenderer.classChart
                        .addSample(fY_Acce_internal[idxBuff] - F_OFFSET_COUNT, 1);
                classChartRenderer.classChart
                        .addSample(fZ_Acce_internal[idxBuff] - F_OFFSET_COUNT, 2);

                classChartRenderer.classChart
                        .addSample(0.0f, 3);
                classChartRenderer.classChart
                        .addSample(0.0f, 4);
                classChartRenderer.classChart
                        .addSample(0.0f, 5);

                classChartRenderer.classChart
                        .addSample(fX_Acce_watch[idxBuff], 6);
                classChartRenderer.classChart
                        .addSample(fY_Acce_watch[idxBuff], 7);
                classChartRenderer.classChart
                        .addSample(fZ_Acce_watch[idxBuff], 8);
            }

                // Try to get the socket server for the data collector
                if (sockServer != null && _tbStream.isChecked()) {
                    Log.d("Main: ", "Send data to server: main and watch!");
                    sockServer.sendToDataCollector("[DATA;INTERNAL;WATCH;" + fX_Acce_watch[idxBuff] + ";" + fY_Acce_watch[idxBuff] + ";" + fZ_Acce_watch[idxBuff] + "]");
                    sockServer.sendToDataCollector("[DATA;INTERNAL;MAIN;" + fX_Acce_internal[idxBuff] + ";" + fY_Acce_internal[idxBuff] + ";" + fZ_Acce_internal[idxBuff] + "]");
                } else {
                    Log.d("Main: ", "Server is null!");
                }



                // Increment the data buffer index
                idxBuff = ++idxBuff % iFileSampleCount;
            }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d("MY_APP", sensor.toString() + " - " + accuracy);
        }
    };

    /**
     * Pass the message handler to the Bluetooth class
     */
    void init() {

        //Bluetooth.gethandler(mUpdateHandler);
        sockServer.initServer();
    }

    public void messageText(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            //myLabel.append("\n Wearable sent :" + newinfo);

        }
    }


    /**
     * When streaming stops, this need to be halted as well
     */
    private void vStopStreamDep(){

        _tbSaveData.setChecked(false);
        _tbSaveData.setEnabled(false);
        _tbSaveData.setVisibility(View.GONE);
        _tvDataStorage.setVisibility(View.GONE);
        vUpdateSaveData();

        vUpdateChMapsEnabled(false);
        _tbAudioOut.setChecked(false);
        _tbAudioOut.setEnabled(false);
        _tbAudioOut.setVisibility(View.GONE);
        _tvAudioOut.setVisibility(View.GONE);
        vUpdateAudioOut();

    }

    /**
     * Handles process that should be affected by change in the SaveData button status
     */
    private void vUpdateSaveData(){
        bWriteLocal = _tbStream.isChecked();
        //Bluetooth.vSetWriteLocal(bWriteLocal);
        //Bluetooth.vSetWritePending(true);

        sockServer.setWritelocalFlagsAtThreads(bWriteLocal);
        sockServer.setWritePendingAtThreads(true);
    }

    /**
     * Update the radio buttons to reflect the internal state of the Bluetooth or Socketserver buttons.
     */
    private void vUpdateChMaps(){

        // Channel 1
        /*if(Bluetooth.isbADC1ToCh1Out()){
            _rgCh1.check(R.id.radio_ADC1_Ch1);
        }
        if(Bluetooth.isbADC2ToCh1Out()){
            _rgCh1.check(R.id.radio_ADC2_Ch1);
        }
        if(Bluetooth.isbADC3ToCh1Out()){
            _rgCh1.check(R.id.radio_ADC3_Ch1);
        }*/

        // Channel 1
        if(sockServer.isbADC1ToCh1Out()){
            _rgCh1.check(R.id.radio_ADC1_Ch1);
        }
        if(sockServer.isbADC2ToCh1Out()){
            _rgCh1.check(R.id.radio_ADC2_Ch1);
        }
        if(sockServer.isbADC3ToCh1Out()){
            _rgCh1.check(R.id.radio_ADC3_Ch1);
        }

        //Channel 2
        if(sockServer.isbADC1ToCh2Out()){
            _rgCh2.check(R.id.radio_ADC1_Ch2);
        }
        if(sockServer.isbADC2ToCh2Out()){
            _rgCh2.check(R.id.radio_ADC2_Ch2);
        }
        if(sockServer.isbADC3ToCh2Out()){
            _rgCh2.check(R.id.radio_ADC3_Ch2);
        }

    }

    /**
     * Construct the string for the spinner control
     * @param dFreq     Frequency, double
     * @param iOCRA     Timer0 register value
     * @return
     */
    private String strListItem(double dFreq, int iOCRA){
        return String.format("%.1f Hz (%d)", dFreq, iOCRA);
    }

    /***
     * Calculate the frequency from the Timer0 OCRA0 value
     * @param iPre      Prescalar value
     * @param iOCRA     Register value
     * @return
     */
    private double dGetFreq(int iPre, int iOCRA){
        return ( (16000000.0 / iPre)/ (double)(iOCRA+1));

    }

    /**
     * Toggles the controls associated with the Arduino configuration
     * @param bEnabled  New status value
     */
    private void vUpdateArduinoControls(boolean bEnabled){

        if( bEnabled){
            _tvArduino.setVisibility(View.VISIBLE);
            _spFreq.setVisibility(View.VISIBLE);
            _bInst.setVisibility(View.VISIBLE);
            _spAccRange.setVisibility(View.VISIBLE);
        }
        else{
            _tvArduino.setVisibility(View.GONE);
            _spFreq.setVisibility(View.GONE);
            _bInst.setVisibility(View.GONE);
            _spAccRange.setVisibility(View.GONE);
        }

    }

    /**
     * Toggles the enabled status of the audio channel mapping buttons
     * @param bEnabled  New enabled status value
     */
    private void vUpdateChMapsEnabled(boolean bEnabled){

        int iRadBut = _rgCh1.getChildCount();
        RadioButton rbTemp;

        // This is for the buttons
        for (int iBut=0; iBut<iRadBut; iBut++){

            // Channel 1 mappings
            rbTemp = ((RadioButton) _rgCh1.getChildAt(iBut));
            rbTemp.setEnabled( bEnabled );
            if( bEnabled ){
                rbTemp.setVisibility(View.VISIBLE);
            }
            else{
                rbTemp.setVisibility(View.GONE);
            }

            //Channel 2 mappings
            rbTemp = ((RadioButton) _rgCh2.getChildAt(iBut));
            rbTemp.setEnabled( bEnabled );
            if( bEnabled ){
                rbTemp.setVisibility(View.VISIBLE);
            }
            else{
                rbTemp.setVisibility(View.GONE);
            }
        }

        // This is for the channel labels
        if( bEnabled ){
            _tvCh1.setVisibility(View.VISIBLE);
            _tvCh2.setVisibility(View.VISIBLE);
        }else{
            _tvCh1.setVisibility(View.GONE);
            _tvCh2.setVisibility(View.GONE);
        }

    }


    /**
     * Get the user preferences
     */
    private void vGetUserPrefs(){

        /*Bluetooth.setbADC1ToCh1Out(sharedPref.getBoolean(getString(R.string.radio_ADC1_Ch1), true));
        Bluetooth.setbADC2ToCh1Out(sharedPref.getBoolean(getString(R.string.radio_ADC2_Ch1), true));
        Bluetooth.setbADC3ToCh1Out(sharedPref.getBoolean(getString(R.string.radio_ADC3_Ch1), true));

        Bluetooth.setbADC1ToCh2Out(sharedPref.getBoolean(getString(R.string.radio_ADC1_Ch2), true));
        Bluetooth.setbADC2ToCh2Out(sharedPref.getBoolean(getString(R.string.radio_ADC2_Ch2), true));
        Bluetooth.setbADC3ToCh2Out(sharedPref.getBoolean(getString(R.string.radio_ADC3_Ch2), true));*/

        sockServer.setbADC1ToCh1Out(sharedPref.getBoolean(getString(R.string.radio_ADC1_Ch1), true));
        sockServer.setbADC2ToCh1Out(sharedPref.getBoolean(getString(R.string.radio_ADC2_Ch1), true));
        sockServer.setbADC3ToCh1Out(sharedPref.getBoolean(getString(R.string.radio_ADC3_Ch1), true));

        sockServer.setbADC1ToCh2Out(sharedPref.getBoolean(getString(R.string.radio_ADC1_Ch2), true));
        sockServer.setbADC2ToCh2Out(sharedPref.getBoolean(getString(R.string.radio_ADC2_Ch2), true));
        sockServer.setbADC3ToCh2Out(sharedPref.getBoolean(getString(R.string.radio_ADC3_Ch2), true));

        _spFreq.setSelection(sharedPref.getInt("OCR0A", 248));
        _spAccRange.setSelection(sharedPref.getInt("ACCFS", 0));
        _etFileSamples.setText(String.format("%d", sharedPref.getInt("SAMPSAVE",15000)));

        vUpdateChMaps();

    }
    /**
     * Update the user preferences
     */
    private void vUpdateUserPrefs(){

        SharedPreferences.Editor editor = sharedPref.edit();
        /*editor.putBoolean(getString(R.string.radio_ADC1_Ch1), Bluetooth.isbADC1ToCh1Out());
        editor.putBoolean(getString(R.string.radio_ADC2_Ch1), Bluetooth.isbADC2ToCh1Out());
        editor.putBoolean(getString(R.string.radio_ADC3_Ch1), Bluetooth.isbADC3ToCh1Out());
        editor.putBoolean(getString(R.string.radio_ADC1_Ch2), Bluetooth.isbADC1ToCh2Out());
        editor.putBoolean(getString(R.string.radio_ADC2_Ch2), Bluetooth.isbADC2ToCh2Out());
        editor.putBoolean(getString(R.string.radio_ADC3_Ch2), Bluetooth.isbADC3ToCh2Out());*/

        editor.putBoolean(getString(R.string.radio_ADC1_Ch1), sockServer.isbADC1ToCh1Out());
        editor.putBoolean(getString(R.string.radio_ADC2_Ch1), sockServer.isbADC2ToCh1Out());
        editor.putBoolean(getString(R.string.radio_ADC3_Ch1), sockServer.isbADC3ToCh1Out());
        editor.putBoolean(getString(R.string.radio_ADC1_Ch2), sockServer.isbADC1ToCh2Out());
        editor.putBoolean(getString(R.string.radio_ADC2_Ch2), sockServer.isbADC2ToCh2Out());
        editor.putBoolean(getString(R.string.radio_ADC3_Ch2), sockServer.isbADC3ToCh2Out());
        editor.putInt("OCR0A",_spFreq.getSelectedItemPosition());
        editor.putInt("ACCFS",_spAccRange.getSelectedItemPosition());
        editor.putInt("SAMPSAVE", Integer.parseInt(_etFileSamples.getText().toString()));
        editor.commit();

    }


    /**
     * Handles process that change with the AudioOut button status
     */
    private void vUpdateAudioOut(){
        sockServer.bAudioOut = _tbAudioOut.isChecked();
        sockServer.classAudioHelper.vSetAudioOut(sockServer.bAudioOut);

        //Bluetooth.bAudioOut = _tbAudioOut.isChecked();
        //Bluetooth.classAudioHelper.vSetAudioOut(Bluetooth.bAudioOut);
    }

    /**
     * This function locks the orientation. The real problem is that my code doesn't handle
     * orientation changes while streaming data from the Bluetooth device.
     * TODO - understand what needs to be changed to allow orientation changes while streaming.
     */
    private void vLockOrient(){
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    private void vUpdateGridLabels(){

        FrameLayout flTemp = (FrameLayout)findViewById(R.id.flChartStuff);
        int iDiff = (int)((float)flTemp.getHeight()/(float)TRACE_COUNT);

        // Update the scaling values
        int iOCRA = sharedPref.getInt("OCR0A", 249);
        //Bluetooth.vSetSampleFreq(dGetFreq(256, iOCRA));
        sockServer.setSampleFreqsAtThreads(dGetFreq(256, iOCRA));
        fAccelCountsPerG = (float)(2048.0f/pow(2.0f, (float)(1+sharedPref.getInt("ACCFS", 0))));
        dGPerDiv = (1.0f/F_SCALE_FACTOR_ACC)/(I_DIVISIONS_Y* fAccelCountsPerG);

        // Accelerometer labels
        int idxText;
        for( idxText = 0; idxText<TRACE_COUNT; ++idxText){

            tvTrace[idxText].setText("Ch" + String.valueOf(idxText+1) + ", " + String.valueOf(dGPerDiv) + "g's per div");
            if(idxText == (TRACE_COUNT-1)){
                tvTrace[idxText].setText("Ch" + String.valueOf(idxText+1) + ", " + String.valueOf(V_PER_DIV) + " volt per div");
            }
            tvTrace[idxText].setBackgroundColor(Color.BLACK);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP);
            params.setMargins((iLabelSize/2)+1,(iLabelSize/2)+1+(iDiff*idxText),0,0);
            tvTrace[idxText].setLayoutParams(params);

        }

        // Horizontal label goodness
        //fTimePerDiv = SCREEN_BUFFER_COUNT / ((float)Bluetooth.dGetSampleFrequency() * (float)I_DIVISIONS_X );
        fTimePerDiv = SCREEN_BUFFER_COUNT / ((float)sockServer.getSampleFreqsAtThreads() * (float)I_DIVISIONS_X );
        tvTrace[idxText].setText(String.format("%.1f sec. per div", fTimePerDiv));

    }

    /**
     * Open a URL link
     * @param url   String with the URL link
     */
    private void goToUrl (String url) {
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    /**
     * Initialize the button controls and listeners
     */
    private void ButtonInit(){

        final Button btnConnectButton;
        final Button btnDiscconnectButton;

        // Configure the stream data button
        _tbStream.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                // Lock the orientation
                vLockOrient();

                // This section handles the thread
                /*if (Bluetooth.connectedThread != null)
                {
                    Bluetooth.bStreamData = _tbStream.isChecked();
                }*/

                // This section handles the thread
                if (sockServer.serverRunnableForThread != null)
                {
                    sockServer.serverRunnableForThread.setChannelsStreamingFlag(_tbStream.isChecked());
                }


            }

        });

        // Configure the Bluetooth connect button
        btnConnectButton = (Button)findViewById(R.id.bConnect);
        _bDisconnect = (Button)findViewById(R.id.bDisconnect);

        btnConnectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                // Update user prefs for samples to save
                vUpdateUserPrefs();
                //Bluetooth.vSetFileSamples(sharedPref.getInt("SAMPSAVE",15000));
                sockServer.getServerRunnableForChannels().vSetFileSamples(sharedPref.getInt("SAMPSAVE",15000));

                //startActivity(new Intent("android.intent.action.BT1"));

                // ToDo: start the server

                btnConnectButton.setVisibility(View.INVISIBLE);
                btnConnectButton.setEnabled(false);
                _bDisconnect.setVisibility(View.VISIBLE);
                _bDisconnect.setEnabled(true);
                sockServer.startServer();

            }


        });

        // Configure the network Socket disconnect button

        _bDisconnect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                _tbStream.setChecked(false);
                sockServer.serverRunnableForThread.setChannelsStreamingFlag(false);
                btnConnectButton.setVisibility(View.VISIBLE);
                btnConnectButton.setEnabled(true);
                _bDisconnect.setVisibility(View.INVISIBLE);
                _bDisconnect.setEnabled(false);
                sockServer.disconnect();
                vStopStreamDep();
            }


        });

        // Configure the save data button
        _tbSaveData = (ToggleButton)findViewById(R.id.tbSaveData);
        _tbSaveData.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /// TEST // vUpdateSaveData();
                new SendMessage().start();
                Log.d("Main: ", "Send to Wearable - TEST Message ");
            }
        });

        // Configure the direction data button
        _tbDirectionData = (ToggleButton)findViewById(R.id.tbDirectionData);
        _tbDirectionData.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_tbDirectionData.isChecked()) {      // Internal Sensor
                    sensorDirection = 1; // 0 - external, 1 - internal
                    sockServer.getServerRunnableForChannels().setSensorDirectionAtChannels(sensorDirection); // 0 external, 1 internal
                    Toast.makeText(getApplicationContext(), "Internal sensor start reading",Toast.LENGTH_SHORT);

                    //sensorManager.registerListener(mSensorListener,accelSensor,SensorManager.SENSOR_DELAY_NORMAL);


                    Log.d("Main:","Internal sensor will be used now.");
                    List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
                    Log.d("Main:","Sensors: " + deviceSensors.toString());
                } else {
                    sensorDirection = 0; // 0 - external, 1 - internal
                    sockServer.getServerRunnableForChannels().setSensorDirectionAtChannels(sensorDirection); // 0 external, 1 internal

                    //sensorManager.unregisterListener(mSensorListener);
                }
            }
        });

        // Configure the Build It button to link to Instructables
        _bInst.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            goToUrl("https://www.instructables.com/id/Realtime-MPU-6050A0-Data-Logging-With-Arduino-and-");

            }


        });

        // Configure the Arduino frequency selection spinner
        _spFreq.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                vUpdateUserPrefs();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Configure the Accelerometer range selection spinner
        _spAccRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                vUpdateUserPrefs();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Configure the channel 1 radio buttons
        _rgCh1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                // Check which radio button was clicked
                switch(checkedId) {
                    case R.id.radio_ADC1_Ch1:
                        /*Bluetooth.setbADC1ToCh1Out(true);
                        Bluetooth.setbADC2ToCh1Out(false);
                        Bluetooth.setbADC3ToCh1Out(false);*/
                        sockServer.setbADC1ToCh1Out(true);
                        sockServer.setbADC2ToCh1Out(false);
                        sockServer.setbADC3ToCh1Out(false);
//                        Log.d(strTag, ":HM:                     ADC1_Ch1 Active: ");
                        break;
                    case R.id.radio_ADC2_Ch1:
                        /*Bluetooth.setbADC1ToCh1Out(false);
                        Bluetooth.setbADC2ToCh1Out(true);
                        Bluetooth.setbADC3ToCh1Out(false);*/
                        sockServer.setbADC1ToCh1Out(false);
                        sockServer.setbADC2ToCh1Out(true);
                        sockServer.setbADC3ToCh1Out(false);
//                        Log.d(strTag, ":HM:                     ADC2_Ch1 Active: ");
                        break;
                    case R.id.radio_ADC3_Ch1:
                        /*Bluetooth.setbADC1ToCh1Out(false);
                        Bluetooth.setbADC2ToCh1Out(false);
                        Bluetooth.setbADC3ToCh1Out(true);*/
                        sockServer.setbADC1ToCh1Out(false);
                        sockServer.setbADC2ToCh1Out(false);
                        sockServer.setbADC3ToCh1Out(true);
//                        Log.d(strTag, ":HM:                     ADC3_Ch1 Active: ");
                        break;
                }

                vUpdateUserPrefs();

            }
        });

        // Configure the channel 2 radio buttons
        _rgCh2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                // Check which radio button was clicked
                switch(checkedId) {
                    case R.id.radio_ADC1_Ch2:
                        /*Bluetooth.setbADC1ToCh2Out(true);
                        Bluetooth.setbADC2ToCh2Out(false);
                        Bluetooth.setbADC3ToCh2Out(false);*/

                        sockServer.setbADC1ToCh2Out(true);
                        sockServer.setbADC2ToCh2Out(false);
                        sockServer.setbADC3ToCh2Out(false);
//                        Log.d(strTag, ":HM:                     ADC1_Ch2 Active: ");
                        break;
                    case R.id.radio_ADC2_Ch2:
                        /*Bluetooth.setbADC1ToCh2Out(false);
                        Bluetooth.setbADC2ToCh2Out(true);
                        Bluetooth.setbADC3ToCh2Out(false);*/

                        sockServer.setbADC1ToCh2Out(false);
                        sockServer.setbADC2ToCh2Out(true);
                        sockServer.setbADC3ToCh2Out(false);
//                        Log.d(strTag, ":HM:                     ADC2_Ch2 Active: ");
                        break;
                    case R.id.radio_ADC3_Ch2:
                        /*Bluetooth.setbADC1ToCh2Out(false);
                        Bluetooth.setbADC2ToCh2Out(false);
                        Bluetooth.setbADC3ToCh2Out(true);*/

                        sockServer.setbADC1ToCh2Out(false);
                        sockServer.setbADC2ToCh2Out(false);
                        sockServer.setbADC3ToCh2Out(true);
//                        Log.d(strTag, ":HM:                     ADC3_Ch2 Active: ");
                        break;
                }

                vUpdateUserPrefs();

            }
        });

        // Configure the audio out button
        _tbAudioOut = (ToggleButton)findViewById(R.id.tbAudioOut);
        _tbAudioOut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                vUpdateAudioOut();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    Handler mUpdateHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){

                case SocketServer.SUCCESS_DISCONNECT:
                    Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_LONG).show();
                    /*_bDisconnect.setEnabled(false);
                    _bDisconnect.setVisibility(View.GONE);
                    _tvControl.setVisibility(View.GONE);
                    _tbStream.setVisibility(View.GONE);
                    _tbStream.setEnabled(false);
                    vUpdateArduinoControls(true);*/
                    break;

                case SocketServer.SUCCESS_CONNECT:
                    Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                    /*_bDisconnect.setVisibility(View.VISIBLE);
                    _bDisconnect.setEnabled(true);
                    _tvControl.setVisibility(View.VISIBLE);
                    _tbStream.setVisibility(View.VISIBLE);
                    _tbStream.setEnabled(true);
                    vUpdateArduinoControls(false);*/
                    break;

                case SocketServer.FILE_WRITE_DONE:
                    /*_tbSaveData.setChecked(false);
                    _tbSaveData.setEnabled(false);*/
                    break;

                /*case Bluetooth.SUCCESS_DISCONNECT:
                    Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_LONG).show();
                    _bDisconnect.setEnabled(false);
                    _bDisconnect.setVisibility(View.GONE);
                    _tvControl.setVisibility(View.GONE);
                    _tbStream.setVisibility(View.GONE);
                    _tbStream.setEnabled(false);
                    vUpdateArduinoControls(true);
                    break;

                case Bluetooth.SUCCESS_CONNECT:
                    Bluetooth.connectedThread = new Bluetooth.ConnectedThread((BluetoothSocket)msg.obj);
                    Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                    Bluetooth.connectedThread.start();
                    _bDisconnect.setVisibility(View.VISIBLE);
                    _bDisconnect.setEnabled(true);
                    _tvControl.setVisibility(View.VISIBLE);
                    _tbStream.setVisibility(View.VISIBLE);
                    _tbStream.setEnabled(true);
                    vUpdateArduinoControls(false);
                    break;

                case Bluetooth.FILE_WRITE_DONE:
                    _tbSaveData.setChecked(false);
                    _tbSaveData.setEnabled(false);
                    break;*/
            }

        }

    };

}