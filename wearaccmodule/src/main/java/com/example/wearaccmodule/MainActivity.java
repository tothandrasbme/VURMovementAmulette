package com.example.wearaccmodule;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.drawer.WearableActionDrawerView;

public class MainActivity extends WearableActivity implements SensorEventListener,
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener {

    private TextView mTextView;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Button testButton;
    Vibrator vibrator = null;
    long[] vibrationPattern = {0, 500, 50, 300};
    int indexInPatternToRepeat;
    private int mSensorType;
    int receivedMessageNumber = 1;
    public static final String SEND_ACCELERATION_DATA_MESSAGE_PATH = "/acceleration_dataexploration";
    private static final String SENDBACK_ACCELERATION_DATA_MESSAGE_PATH = "/acceleration_dataexploration_tomobile";
    public WearableActivity currentActivity;
    public int measurementNumber = 0;
    public static final int MeasurementLimit = 20;
    String dataPayload = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text_view);
        testButton = (Button) findViewById(R.id.googlePlusButton);

        currentActivity = this;

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //new SendMessage("/my_path", "TEST message").start();
                Log.d("Watch: ", "Send to Mobile - TEST Message ");

                new SendMessage("Test",currentActivity).start();
            }
        });

        // Set vibrator service
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        //-1 - don't repeat
        indexInPatternToRepeat = -1;


        // Init sensor service
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        IntentFilter newSendFilter = new IntentFilter(Intent.ACTION_SEND);


        Receiver messageReceiver = new Receiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newSendFilter);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        //-1 - don't repeat
        indexInPatternToRepeat = -1;

        Log.d("Watch: ", "Register Sensor");

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        Log.d("Watch: ", "Register Listener");

        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Log.d("Watch: ","Sensors: " + deviceSensors.toString());

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Instantiates clients without member variables, as clients are inexpensive to create and
        // won't lose their listeners. (They are cached and shared between GoogleApi instances.)
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        /*if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            Log.d("Watch: ", "Sensor value is unreliable");
            return;
        }*/
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mTextView.setText(
                    "x = " + Float.toString(event.values[0]) + "\n" +
                            "y = " + Float.toString(event.values[1]) + "\n" +
                            "z = " + Float.toString(event.values[2]) + "\n"
            );

            if(measurementNumber < MeasurementLimit) {
                measurementNumber++;
            } else {
                measurementNumber = 0;
                dataPayload = "[" + Float.toString(event.values[0]) +
                        "," + Float.toString(event.values[1]) +
                        "," + Float.toString(event.values[2]) + "]";

                new SendMessage(dataPayload, currentActivity).start();
            }


        }

        /*Log.d("Watch: ", "onSensorChanged: " +  "x = " + Float.toString(event.values[0]) + "\n" +
                "y = " + Float.toString(event.values[1]) + "\n" +
                "z = " + Float.toString(event.values[2]) + "\n");*/

    }

    class SendMessage extends Thread {
        String message;
        WearableActivity mActivity;

//Constructor for sending information to the Data Layer//

        SendMessage(String toSend, WearableActivity mWA) {
            message = toSend;
            mActivity = mWA;
        }

        public void run() {

//Retrieve the connected devices//

            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

//Block on a task and get the result synchronously//

                List<Node> nodes = Tasks.await(nodeListTask);
                for (Node node : nodes) {



                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(mActivity)
                                    .sendMessage(node.getId(), SENDBACK_ACCELERATION_DATA_MESSAGE_PATH, message.getBytes());

                    sendMessageTask.addOnCompleteListener(
                            new OnCompleteListener<Integer>() {
                                @Override
                                public void onComplete(Task<Integer> task) {
                                    if (task.isSuccessful()) {
                                        Log.d("Watch: ", "Message sent successfully");
                                    } else {
                                        Log.d("Watch: ", "Message failed.");
                                    }
                                }
                            });

                }

            } catch (ExecutionException exception) {

            } catch (InterruptedException exception) {

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d("Watch: ", "onCapabilityChanged: " + capabilityInfo);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("Watch: ", "Message received (" + messageEvent.getPath() + ")");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Display the following when a new message is received//

            String onMessageReceived = "I just received a message from the handheld " + receivedMessageNumber++;
            //textView.setText(onMessageReceived);

            // Vibrate
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

            Log.d("Watch: ", "From mobile to Wearable - Intent: " + intent.getStringExtra("message").toString());
            //textView.append("Data In: "+intent.getStringExtra("message").toString()+"\r\n");

        }
    }
}
