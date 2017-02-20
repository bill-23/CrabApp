package com.example.timer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.googlemaps.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.UUID;

import static android.R.attr.fragment;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private ImageButton imagebutton;
    private Button resetButton;
    private Button startButton;
    private Button pauseButton;

    private TextView timerValue;
    private long startTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;

    private GoogleMap googleMap;
    private static final String TAG = "bluetooth2";
    TextView txtposition;
    Handler h;
    final int RECIEVE_MESSAGE = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    int clickcount = 0;
    int clickcount2 = 0;

    private ConnectedThread mConnectedThread;


    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private static String address = "98:76:B6:00:5E:C0";


   // public MainActivity() {
   // }

        @Override
        public void onCreate (Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            MapFragment mapFragment = (MapFragment) getFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);


            btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
            checkBTState();

            {
                final Runnable updateTimerThread = new Runnable() {
                    public void run() {
                        timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
                        updatedTime = timeSwapBuff + timeInMilliseconds;
                        int secs = (int) (updatedTime / 1000);
                        int mins = secs / 60;
                        secs = secs % 60;
                        timerValue.setText("" + mins + ":"
                                + String.format("%02d", secs));
                        customHandler.postDelayed(this, 0);
                    }
                };

                timerValue = (TextView) findViewById(R.id.timerValue);
                Toast.makeText(getApplicationContext(), "Tap To Start!", Toast.LENGTH_LONG).show();
                timerValue.setText("00:00");
                timerValue.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                       clickcount = clickcount + 1;

                        if (clickcount == 1) {
                            Toast.makeText(getApplicationContext(), "Timer Started! Tap Again To Pause", Toast.LENGTH_LONG).show();
                            startTime = SystemClock.uptimeMillis();
                            customHandler.postDelayed(updateTimerThread, 0);

                        } else if (clickcount == 2) {
                            Toast.makeText(getApplicationContext(), "Timer Paused! Tap Again To Start Again", Toast.LENGTH_LONG).show();
                            timeSwapBuff += timeInMilliseconds;
                            customHandler.removeCallbacks(updateTimerThread);

                        }
                        else if (clickcount > 2)
                            clickcount = 0;
                    }
                });

                resetButton = (Button) findViewById(R.id.resetButton);
                resetButton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View view) {
                        clickcount2 = clickcount2 + 1;
                        if (clickcount2 == 1) {
                            Toast.makeText(getApplicationContext(), "Press Again To Reset!", Toast.LENGTH_LONG).show();

                        } else if (clickcount2 == 2) {
                            Toast.makeText(getApplicationContext(), "Timer Reset!", Toast.LENGTH_LONG).show();
                            timeSwapBuff = 0L;
                            timerValue.setText("00:00");
                            customHandler.removeCallbacks(updateTimerThread);
                        }
                    }
                });
                imagebutton = (ImageButton) findViewById(R.id.tideButton);
                imagebutton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View view) {
                        Intent openBrowser = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.tides4fishing.com/us/california/bodega-harbor-entrance"));
                        startActivity(openBrowser);
                    }
                });
                imagebutton = (ImageButton) findViewById(R.id.regulationButton);
                imagebutton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View view) {
                        Intent openBrowser = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wildlife.ca.gov/Fishing/Ocean/Regulations/Fishing-Map/San-Francisco"));
                        startActivity(openBrowser);
                    }
                });

                try {
                    initializeMap();

                    googleMap.setMyLocationEnabled(true);


                } catch (Exception e) {
                    e.printStackTrace();
                }

                h = new Handler() {
                    public void handleMessage(android.os.Message msg) {

                        switch (msg.what) {
                            case RECIEVE_MESSAGE:
                                byte[] readBuf = (byte[]) msg.obj;
                                String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                                sb.append(strIncom);                                                // append string
                                int endOfLineIndex = sb.indexOf("~");                            // determine the end-of-line
                                if (endOfLineIndex > 0) {                                            // if end-of-line,

                                    String Position_One = sb.substring(0, endOfLineIndex);               // extract string
                                    sb.delete(0, sb.length());                                      // and clear
                                    StringTokenizer tokens = new StringTokenizer(Position_One, ":");
                                    if (tokens.hasMoreElements()) {
                                        String Latt = tokens.nextToken();
                                        if (tokens.hasMoreElements()) {
                                            String Lngg = tokens.nextToken();
                                            float f1 = Float.parseFloat(Latt);
                                            float f2 = Float.parseFloat(Lngg);
                                            try {
                                                googleMap.clear();
                                                googleMap.addMarker(new MarkerOptions()
                                                        .position(new LatLng(f1, f2))
                                                        .title("Crab Pot 1"));

                                            } catch (NullPointerException e) {
                                                e.printStackTrace();
                                            }

                                        }


                                    }
                                }
                        }

                    }

                    ;
                };
            }
        }

    private void initializeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();
            // check if map is created successfully or not


            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        initializeMap();
        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();


    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }


    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
}















