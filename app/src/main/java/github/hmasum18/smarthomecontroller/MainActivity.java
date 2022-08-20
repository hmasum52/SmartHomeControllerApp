package github.hmasum18.smarthomecontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import github.hmasum18.smarthomecontroller.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity->";

    public static Handler handler;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    private ActivityMainBinding mVB;

    private BluetoothConnector bluetoothConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set up view binding
        mVB = ActivityMainBinding.inflate(getLayoutInflater());
        super.setContentView(mVB.getRoot());

        // UI Initialization
        final Button buttonConnect = mVB.buttonConnect;
        final Toolbar toolbar = mVB.toolbar;
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        final TextView textViewInfo = mVB.textViewInfo;
        final ToggleButton buttonToggle = mVB.buttonToggle;
        buttonToggle.setEnabled(false);

        // If a bluetooth device has been selected from SelectDeviceActivity
        String deviceName = getIntent().getStringExtra("deviceName");

         /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                buttonToggle.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        switch (arduinoMsg.toLowerCase()){
                            case "led is turned on":
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                            case "led is turned off":
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                        }
                        break;
                }
            }
        };


        if (deviceName != null){
            // Get the device address to make BT Connection
            String deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            bluetoothConnector = new BluetoothConnector(handler, deviceName, deviceAddress);
        }

        if(bluetoothConnector == null){
            showBluetoothNotConnected();
        }



        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        // Button to ON/OFF LED on Arduino Board
        buttonToggle.setOnClickListener(v -> {
            String cmdText = null;
            String btnState = buttonToggle.getText().toString().toLowerCase();
            Log.d(TAG, btnState);
            switch (btnState){
                case "on":
                    //buttonToggle.setText("Turn Off");
                    // Command to turn on LED on Arduino. Must match with the command in Arduino code
                    cmdText = "1";//"<turn on>";
                    break;
                case "off":
                    //buttonToggle.setText("Turn On");
                    // Command to turn off LED on Arduino. Must match with the command in Arduino code
                    cmdText = "0";//"<turn off>";
                    break;
            }
            // Send command to Arduino board
            bluetoothConnector.sendMessage(cmdText);
        });
    }

    void showBluetoothNotConnected(){
        Snackbar snackbar = Snackbar.make(mVB.getRoot(), "Activate Bluetooth or pair a Bluetooth device", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View view) { }
        });
        snackbar.show();
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if(bluetoothConnector!=null)
            bluetoothConnector.disConnect();

        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}