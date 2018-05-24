package com.example.nicolas.glaskbt;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.io.*;


public class MainActivity extends AppCompatActivity {
    List<Byte> buffer_z = new ArrayList<Byte>();

    //int buffer_z_len = -1;
    private static final int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;

    TextView textInfo, textStatus, textDisplay;
    ListView listViewPairedDevice;
    LinearLayout inputPane;
    EditText inputField;
    Button btnSend;
    String bytereceived = "";
    private GameManager game;

    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private UUID myUUID;
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "00001101-0000-1000-8000-00805F9B34FB";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textInfo = (TextView)findViewById(R.id.info);
        textStatus = (TextView)findViewById(R.id.status);
        textDisplay = (TextView)findViewById(R.id.display);
        listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);

        inputPane = (LinearLayout)findViewById(R.id.inputpane);
        inputField = (EditText)findViewById(R.id.input);
        btnSend = (Button)findViewById(R.id.send);
        btnSend.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

            }});

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NOT support",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //using the well-known SPP UUID
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this hardware platform",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String stInfo = bluetoothAdapter.getName() + "\n" +
                bluetoothAdapter.getAddress();
        textInfo.setText(stInfo);
        game = new GameManager();
    }

    private class GameManager extends GlaskPlayers {
        public void pushIsFilled(List<Integer> id) {
            Toast.makeText(getApplicationContext(), "Qqun est rempli !", Toast.LENGTH_SHORT).show();
        }

        public void pushIsAffoning(List<Integer> id) {
            Toast.makeText(getApplicationContext(), "Qqun affone !", Toast.LENGTH_SHORT).show();
        }

        public void pushIsShaked(List<Integer> id) {
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        //Turn ON BlueTooth if it is OFF
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    private void setup() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            pairedDeviceArrayList = new ArrayList<BluetoothDevice>();

            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device);
            }

            pairedDeviceAdapter = new ArrayAdapter<BluetoothDevice>(this,
                    android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);

            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    BluetoothDevice device =
                            (BluetoothDevice) parent.getItemAtPosition(position);
                    Toast.makeText(MainActivity.this,
                            "Name: " + device.getName() + "\n"
                                    + "Address: " + device.getAddress() + "\n"
                                    + "BondState: " + device.getBondState() + "\n"
                                    + "BluetoothClass: " + device.getBluetoothClass() + "\n"
                                    + "Class: " + device.getClass(),
                            Toast.LENGTH_LONG).show();

                    textStatus.setText("start ThreadConnectBTdevice");
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                    myThreadConnectBTdevice.start();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                setup();
            }else{
                Toast.makeText(this,
                        "BlueTooth NOT enabled",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
                //textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        textStatus.setText("something wrong bluetoothSocket.connect(): \n" + eMessage);
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {
                        //textStatus.setText(msgconnected);

                        listViewPairedDevice.setVisibility(View.GONE);
                        inputPane.setVisibility(View.VISIBLE);
                    }});

                startThreadConnected(bluetoothSocket);
            }else{
                //fail
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[100];
            int bytes;

            while (true) {
                try {
                    //byte[] bytes = extract(connectedInputStream);

                    if(myThreadConnected!=null)
                    {
                        if(game.isOnline())
                        {
                            try {
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

                                byte key_start[] = {0, 0, -1}; //lancement de l'écriture
                                outputStream.write(key_start);
                                while(game.toPush())
                                {
                                    int id = game.push();
                                    byte toSend[] = game.getById(id).formatOut();
                                    outputStream.write(toSend);
                                }
                                byte key_stop[] = {-2, -2, -2}; //lancement de la lecture
                                outputStream.write(key_stop);
                                myThreadConnected.write(outputStream.toByteArray());
                            }
                            catch (IOException ioe) {};
                            game.sync();

                        }
                        else
                        {
                            byte[] bytesToSend = {-2, -2, -2};
                            myThreadConnected.write(bytesToSend);
                        }
                    }


                    bytes = connectedInputStream.read(buffer);

                    String message = "";

                    //byte[] buffer_z = new byte[100];
                    int j = 0; // 0
                    for(int i=0; i < buffer.length && j < 100;i++)
                    {
                        if((int) buffer[i] != 0)
                        {
                            buffer_z.add(buffer[i]);
                            //message += String.valueOf(buffer_z.size()) + ", ";
                            j++;
                        }
                    }

                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {

                            String  msgReceived = "";
                            if(buffer_z.size() > 27)
                            {
                                boolean stop = false;
                                for(int i=0; i < buffer_z.size() - 2 && !stop;i++)
                                {
                                    // On vérifie que la séquence commence par -34 et -1, sinon inutile
                                    if(buffer_z.get(i) == (byte) -34 && buffer_z.get(i + 1) == (byte) -1 && i < 20) //
                                    {
                                        // i + 2 = id
                                        // i + 3 = isFilled
                                        // i + 4 = isDrinking
                                        // i + 5 = lastTime
                                        // i + 6 = lastTime
                                        // i + 7 = shaked
                                        // On vérifie qu'il y n'a pas de -1 dans la séquence
                                        if(buffer_z.get(i + 2) != -1 && buffer_z.get(i + 3) != -1 && buffer_z.get(i + 4) != -1 && buffer_z.get(i + 5) != -1 && buffer_z.get(i + 6) != -1 && buffer_z.get(i + 7) != -1)
                                        {
                                            // On vérifie que les boolens sont bien des booleens et que le shaked est positif
                                            if((buffer_z.get(i + 3) == -10 || buffer_z.get(i + 3) == 10) && (buffer_z.get(i + 4) == -10 || buffer_z.get(i + 4) == 10) && buffer_z.get(i + 7) > 0)
                                            {
                                                msgReceived += ", " + String.valueOf((int) buffer_z.get(i)) + ", " + String.valueOf((int) buffer_z.get(i+1)) + ", " + String.valueOf((int) buffer_z.get(i+2)) + ", " + String.valueOf((int) buffer_z.get(i+3)) + ", " + String.valueOf((int) buffer_z.get(i+4)) + ", " + String.valueOf((int) buffer_z.get(i+5)) + ", " + String.valueOf((int) buffer_z.get(i+6)) + ", " + String.valueOf((int) buffer_z.get(i+7));
                                                int id = buffer_z.get(i + 2);
                                                int isFilled = buffer_z.get(i + 3);
                                                int isDrinking = buffer_z.get(i + 4);
                                                int lastTime = ((buffer_z.get(i + 6) & 0xff) << 8) | ( buffer_z.get(i + 5) & 0xff);
                                                int shaked = buffer_z.get(i + 7);
//                                                i = i + 6;
                                                game.set(id, isFilled > 0, isDrinking > 0, (double) lastTime, shaked);
                                            }

                                        }

                                    }
                                    if(buffer_z.get(i) == (byte) -2 && buffer_z.get(i + 1) == (byte) -3)
                                    {
                                        stop = true;
                                    }
                                }

                                buffer_z.clear();
                            }

                            String status = "";
                            if(game.isOnline())
                            {

                                for(int i=0; i < game.numOfPlayers(); i++)
                                {
                                    int id = game.players.get(i).id;
                                    int R = game.players.get(i).R;
                                    int G = game.players.get(i).G;
                                    int B = game.players.get(i).B;
                                    int vibrate = game.players.get(i).vibrate;
                                    boolean isFilled = game.players.get(i).isFilled;
                                    boolean isDrinking = game.players.get(i).isDrinking;
                                    double lastTime = game.players.get(i).lastTime;
                                    int shaked = game.players.get(i).shaked;

                                    status += "\n id=" + String.valueOf(id) + " R=" + String.valueOf(R) + " G=" + String.valueOf(G) + " B=" + String.valueOf(B) + " vibrate=" + String.valueOf(vibrate) + " isFilled=" + String.valueOf(isFilled) + " isDrinking=" + String.valueOf(isDrinking) + " lastTime=" + String.valueOf(lastTime) + " shaked=" + String.valueOf(shaked);
                                }
                            }
                            if(msgReceived.length() > 3) {
                                textStatus.setText(msgReceived);
                                textDisplay.setText(status);
                            }
                        }});

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            textStatus.setText(msgConnectionLost);
                        }});
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}