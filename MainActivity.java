package com.example.nicolas.glaskagain;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;

import com.macroyau.blue2serial.BluetoothDeviceListDialog;
import com.macroyau.blue2serial.BluetoothSerial;
import com.macroyau.blue2serial.BluetoothSerialRawListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity
        implements BluetoothSerialRawListener, BluetoothDeviceListDialog.OnDeviceSelectedListener, View.OnClickListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothSerial bluetoothSerial;
    private TextView result, kernel;
    private EditText input;
    private MenuItem actionConnect, actionDisconnect;
    private boolean crlf = false;
    String message = "";
    private GlaskPlayers game;
    private String s = "";
    private long startTime = 0;

    final Handler handler = new Handler();
    private Handler sending = new Handler();
    List<Integer> buffer_z = new ArrayList<Integer>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = (TextView) findViewById(R.id.result);
        input = (EditText) findViewById(R.id.input);
        kernel = (TextView) findViewById(R.id.kernel);

        Button b1 = (Button) findViewById(R.id.button1);
        b1.setOnClickListener(this);
        Button b2 = (Button) findViewById(R.id.button2);
        b2.setOnClickListener(this);
        Button b3 = (Button) findViewById(R.id.button3);
        b3.setOnClickListener(this);

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String send = input.getText().toString().trim();
                    if (send.length() > 0) {
                        bluetoothSerial.write(send, crlf);
                        input.setText("");
                    }
                }
                return false;
            }
        });


        bluetoothSerial = new BluetoothSerial(this, this);
        game = new GameManager();


    }


    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button1:
                if(game.isOnline())
                {
                    game.getById(game.playersID.get(0)).RGB(0,255, 0);
                }
                break;
            case R.id.button2:
                //kernel.setText("sent");
                byte[] bytesToSend = {-2, -2, -2};
                bluetoothSerial.write(bytesToSend);
                break;
            case R.id.button3:
                byte[] bytesToSend2 = {-34,-34, -1, 2, 127, 127, 127, 0, -2, -2};
                bluetoothSerial.write(bytesToSend2);
                break;

        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        bluetoothSerial.setup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothSerial.checkBluetooth() && bluetoothSerial.isBluetoothEnabled()) {
            if (!bluetoothSerial.isConnected()) {
                bluetoothSerial.start();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothSerial.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_terminal, menu);
        actionConnect = menu.findItem(R.id.action_connect);
        actionDisconnect = menu.findItem(R.id.action_disconnect);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_connect) {
            showDeviceListDialog();
            return true;
        } else if (id == R.id.action_disconnect) {
            bluetoothSerial.stop();
            return true;
        } else if (id == R.id.action_crlf) {
            crlf = !item.isChecked();
            item.setChecked(crlf);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void invalidateOptionsMenu() {
        if (bluetoothSerial == null)
            return;
        if (bluetoothSerial.isConnected()) {
            if (actionConnect != null)
                actionConnect.setVisible(false);
            if (actionDisconnect != null)
                actionDisconnect.setVisible(true);
        } else {
            if (actionConnect != null)
                actionConnect.setVisible(true);
            if (actionDisconnect != null)
                actionDisconnect.setVisible(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    bluetoothSerial.setup();
                }
                break;
        }
    }

    private void updateBluetoothState() {
        final int state;
        if (bluetoothSerial != null)
            state = bluetoothSerial.getState();
        else
            state = BluetoothSerial.STATE_DISCONNECTED;

        String subtitle;
        switch (state) {
            case BluetoothSerial.STATE_CONNECTING:
                subtitle = getString(R.string.status_connecting);
                break;
            case BluetoothSerial.STATE_CONNECTED:
                subtitle = getString(R.string.status_connected, bluetoothSerial.getConnectedDeviceName());
                break;
            default:
                subtitle = getString(R.string.status_disconnected);
                break;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    private void showDeviceListDialog() {
        BluetoothDeviceListDialog dialog = new BluetoothDeviceListDialog(this);
        dialog.setOnDeviceSelectedListener(this);
        dialog.setTitle(R.string.paired_devices);
        dialog.setDevices(bluetoothSerial.getPairedDevices());
        dialog.showAddress(true);
        dialog.show();
    }

    @Override
    public void onBluetoothNotSupported() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.no_bluetooth)
                .setPositiveButton(R.string.action_quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBluetoothDisabled() {
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
    }

    @Override
    public void onBluetoothDeviceDisconnected() {
        invalidateOptionsMenu();
        updateBluetoothState();
    }

    @Override
    public void onConnectingBluetoothDevice() {
        updateBluetoothState();
    }

    @Override
    public void onBluetoothDeviceConnected(String name, String address) {
        invalidateOptionsMenu();
        updateBluetoothState();
    }

    @Override
    public void onBluetoothSerialReadRaw(byte[] buffer) {
        for(int i=0; i < buffer.length;i++)
        {
            buffer_z.add((int) buffer[i]);
            //message += String.valueOf(((byte)buffer[i])) + ", ";

        }
        boolean stop = false;
        for(int i=0; i < buffer_z.size() - 1 && !stop;i++)
        {
            //kernel.append(String.valueOf(i) + " = " + String.valueOf(buffer_z.get(i)) + "\n");

            // On vérifie que la séquence commence par -34 et -1, sinon inutile
            if(buffer_z.get(i) == -34 && buffer_z.get(i + 1) == -1 && i < buffer_z.size() - 7)
            {
                // On vérifie qu'il y n'a pas de -1 dans la séquence
                if(buffer_z.get(i + 2) != -1 && buffer_z.get(i + 3) != -1 && buffer_z.get(i + 4) != -1 && buffer_z.get(i + 5) != -1 && buffer_z.get(i + 6) != -1 && buffer_z.get(i + 7) != -1)
                {
                    if((buffer_z.get(i + 3) == -10 || buffer_z.get(i + 3) == 10) && (buffer_z.get(i + 4) == -10 || buffer_z.get(i + 4) == 10))
                    {
                        int id = buffer_z.get(i + 2);
                        int isFilled = buffer_z.get(i + 3);
                        int isDrinking = buffer_z.get(i + 4);
                        int lastTime = ((buffer_z.get(i + 6) & 0xff) << 8) | ( buffer_z.get(i + 5) & 0xff);
                        int shaked = buffer_z.get(i + 7);
                        //kernel.append(String.valueOf((int) buffer_z.get(i)) + ", " + String.valueOf((int) buffer_z.get(i+1)) + ", " + String.valueOf((int) buffer_z.get(i+2)) + ", " + String.valueOf((int) buffer_z.get(i+3)) + ", " + String.valueOf((int) buffer_z.get(i+4)) + ", " + String.valueOf((int) buffer_z.get(i+5)) + ", " + String.valueOf((int) buffer_z.get(i+6)) + ", " + String.valueOf((int) buffer_z.get(i+7)));
                        //i = i + 5;
                        game.set(id, isFilled > 0, isDrinking > 0, (double) lastTime, shaked);
                    }

                }
            }
            else if(buffer_z.get(i) == -2 && buffer_z.get(i + 1) == -3)
            {
                buffer_z.clear();
                stop = true;
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
                        bluetoothSerial.write(outputStream.toByteArray());
                    }
                    catch (IOException ioe) {};
                    game.sync();

                }
                else
                {
                    byte[] bytesToSend = {-2, -2, -2};
                    bluetoothSerial.write(bytesToSend);
                }

            }
        }
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
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

                kernel.setText(status);
            }});
    }
    @Override
    public void onBluetoothSerialRead(String message) {
    }

    @Override
    public void onBluetoothSerialWrite(String message) {
    }

    @Override
    public void onBluetoothSerialWriteRaw(byte[] m) {
    }

    @Override
    public void onBluetoothDeviceSelected(BluetoothDevice device) {
        bluetoothSerial.connect(device);
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

}
