package com.ynr.keypsd.mobileprogramminghw3;

import static java.lang.Math.abs;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothClass;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final float POCKET_LUX_THRESHOLD = (float) 0.3;
    private static final float DEVICE_MOVING_THRESHOLD = (float) 0.3;
    private static final float WAIT_TO_CHANGE_STATE = 100;
    private static final String ACTION_NAME = "MotionAndLocation";

    class DeviceAcceleration {
        public DeviceAcceleration(float x, float y, float z) {
            X = x;
            Y = y;
            Z = z;
        }

        float X;
        float Y;
        float Z;
    }

    enum DeviceState {
        ACTIVE,
        MOTIONLESS
    }

    enum DeviceLocation {
        POCKET,
        TABLE,
    }

    SensorManager sensorManager;
    Sensor lightSensor, accSensor;
    DeviceState deviceState;
    DeviceLocation deviceLocation;
    DeviceAcceleration acceleration;
    // Added so that minor changes in very short time don't take effect
    int motionlessConsecutive;
    int activeConsecutive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        deviceState = DeviceState.MOTIONLESS;
        deviceLocation = DeviceLocation.POCKET;
        acceleration = new DeviceAcceleration(0, 0, 0);

        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT){
            Log.i("sensorLog", "Light: lux " + sensorEvent.values[0]);

            if(sensorEvent.values[0] < POCKET_LUX_THRESHOLD){ // In pocket

                if(deviceLocation == DeviceLocation.TABLE){ // Device location changes
                    deviceLocation = DeviceLocation.POCKET;
                    // Toast.makeText(this, "POCKET", Toast.LENGTH_LONG).show();
                    Log.i("sensorLog", "POCKET");
                    sendBroadcastOnStatusChange();
                }

            }
            else{  // On table


                if(deviceLocation == DeviceLocation.POCKET){ // Device location changes
                    deviceLocation = DeviceLocation.TABLE;
                    Log.i("sensorLog", "TABLE");
                    // Toast.makeText(this, "TABLE", Toast.LENGTH_LONG).show();
                    sendBroadcastOnStatusChange();
                }
            }
        }
        else if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            Log.i("sensorLog", "Accelerometer: m/s^2 x " + sensorEvent.values[0]);
            Log.i("sensorLog", "Accelerometer: m/s^2 y " + sensorEvent.values[1]);
            Log.i("sensorLog", "Accelerometer: m/s^2 z " + sensorEvent.values[2]);
            DeviceAcceleration newAcceleration =
                    new DeviceAcceleration(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);

            // Motion detected
            if(    (newAcceleration.X != acceleration.X && abs(newAcceleration.X - acceleration.X) > DEVICE_MOVING_THRESHOLD)
                || (newAcceleration.Y != acceleration.Y && abs(newAcceleration.Y - acceleration.Y) > DEVICE_MOVING_THRESHOLD)
                || (newAcceleration.Z != acceleration.Z && abs(newAcceleration.Z - acceleration.Z) > DEVICE_MOVING_THRESHOLD)){

                if(deviceState == DeviceState.MOTIONLESS && activeConsecutive >= WAIT_TO_CHANGE_STATE){ // State changed
                    deviceState = DeviceState.ACTIVE;
                    activeConsecutive = 0;
                    // Toast.makeText(this, "ACTIVE", Toast.LENGTH_SHORT).show();
                    Log.i("sensorLog", "ACTIVE");
                    sendBroadcastOnStatusChange();
                } else{ // Already moving
                    activeConsecutive++;
                }

            }
            else{ // Motionless

                if(deviceState == DeviceState.ACTIVE && motionlessConsecutive >= WAIT_TO_CHANGE_STATE){
                    deviceState = DeviceState.MOTIONLESS;
                    motionlessConsecutive = 0;
                    // Toast.makeText(this, "MOTIONLESS", Toast.LENGTH_SHORT).show();
                    Log.i("sensorLog", "MOTIONLESS");
                    sendBroadcastOnStatusChange();
                } else{
                    motionlessConsecutive++;
                }

            }

            acceleration.X = newAcceleration.X;
            acceleration.Y = newAcceleration.Y;
            acceleration.Z = newAcceleration.Z;
        }
    }

    private void sendBroadcastOnStatusChange(){
        Intent intent = new Intent();
        intent.setAction(ACTION_NAME);
        intent.putExtra("data", getDeviceStatus());
        sendBroadcast(intent);
    }

    private String getDeviceStatus(){
        String state;
        if(deviceState == DeviceState.MOTIONLESS)
            state = "Motionless";
        else
            state = "Active";

        String location;
        if(deviceLocation == DeviceLocation.TABLE)
            location = "Table";
        else
            location = "Pocket";

        return state + "," + location;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}