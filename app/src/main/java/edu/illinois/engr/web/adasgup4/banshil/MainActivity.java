package edu.illinois.engr.web.adasgup4.banshil;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import android.widget.EditText;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    Thread threadplaypause;
    Thread threadnext;
    Thread threadprevious;
    Thread threadvolumedown;
    Thread threadvolumeup;
    Thread threadvolumemute;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openButton = (Button)findViewById(R.id.open);
        Button sendButton = (Button)findViewById(R.id.send);
        Button closeButton = (Button)findViewById(R.id.close);
        Button settingsButton = (Button)findViewById(R.id.settings);
        myLabel = (TextView)findViewById(R.id.label);
        myTextbox = (EditText)findViewById(R.id.entry);

        //play/pause
        threadplaypause = new Thread() {
            public void run () {
            Instrumentation instpp = new Instrumentation();
            instpp.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        };

        //next
        threadnext = new Thread() {
            public void run () {
                Instrumentation instn = new Instrumentation();
                instn.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
        };

        //previous
        threadprevious = new Thread() {
            public void run () {
                Instrumentation instp = new Instrumentation();
                instp.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        };

        //volumeup
        threadvolumeup = new Thread() {
            public void run () {
                Instrumentation instvu = new Instrumentation();
                instvu.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP);
            }
        };

        //volumedown
        threadvolumedown = new Thread() {
            public void run () {
                Instrumentation instvd = new Instrumentation();
                instvd.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_DOWN);
            }
        };

        //volumemute
        threadvolumemute = new Thread() {
            public void run () {
                Instrumentation instvm = new Instrumentation();
                instvm.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_MUTE);
            }
        };

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    findBT();
                    openBT();
                }
                catch (IOException ex) { }
            }
        });

        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    sendData();
                }
                catch (IOException ex) { }
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                }
                catch (IOException ex) { }
            }
        });

        //Settings button
        settingsButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    PackageManager packageManager = getPackageManager();
                    Intent btintent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                    List activities = packageManager.queryIntentActivities(btintent, PackageManager.MATCH_DEFAULT_ONLY);
                    boolean btintentsafe = activities.size() > 0;
                    if (btintentsafe)
                    {
                        startActivityForResult(btintent,0);
                    }
                }
                catch (Exception ex){}
            }
        });
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }

    void musicControl(String data)
    {
        switch(data)
        {
            case " ":
                threadplaypause.start();
                threadplaypause.interrupt();
                myLabel.setText("Input received!");
                break;
            case "n":
                threadnext.start();
                threadnext.interrupt();
                myLabel.setText("Input received!");
                break;
            case "p":
                threadprevious.start();
                threadprevious.interrupt();
                myLabel.setText("Input received!");
                break;
            case "d":
                threadvolumedown.start();
                threadvolumedown.interrupt();
                myLabel.setText("Input received!");
                break;
            case "u":
                threadvolumeup.start();
                threadvolumeup.interrupt();
                myLabel.setText("Input received!");
                break;
            case "m":
                threadvolumeup.start();
                threadvolumeup.interrupt();
                myLabel.setText("Input received!");
                break;
            default:
                myLabel.setText("'If' conditions not met");
        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character


        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    //final String data = new String(encodedBytes, "US-ASCII");
                                    final String data = new String(encodedBytes);
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Toast test_toast = Toast.makeText(getApplicationContext(), "command = '"+data+"'", Toast.LENGTH_LONG);
                                            test_toast.show();
                                            musicControl(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData() throws IOException
    {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }
}
