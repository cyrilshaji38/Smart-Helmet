package com.example.SmartHelmet;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private TextView tvMsg;

    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_ENABLE_BT = 1;
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvMsg = this.findViewById(R.id.image_change_explanation);

        if (!btAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }

        if(!isNotificationServiceEnabled()){
            AlertDialog enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }

        // Finally we register a receiver to tell the MainActivity when a notification has been received
        ReceiveBroadcastReceiver imageChangeBroadcastReceiver = new ReceiveBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.ssa_ezra.SmartHelmet");
        registerReceiver(imageChangeBroadcastReceiver,intentFilter);

    }

    /**
     * Is Notification Service Enabled.
     */
    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;

                    }
                }
            }
        }
        return false;
    }

    /**
     * Receive Broadcast Receiver.
     * */
    public class ReceiveBroadcastReceiver extends BroadcastReceiver {

        String temptitle = "";

        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {

            String title = intent.getStringExtra("distance");
            String text = intent.getStringExtra("text");
            String arrow = intent.getStringExtra("arrow");
            String arrowG;
            {
                switch (arrow) {
                    case "^":
                        arrowG = "F";
                        break;
                    case "<":
                        arrowG = "L";
                        break;
                    case ">":
                        arrowG = "R";
                        break;
                    case "$":
                        arrowG = "U";
                        break;
                    case "&":
                        arrowG = "Final Destination";
                        break;
                    default:
                        arrowG = "X";
                        break;
                }
            }

            String helmet1 = title.replace("%", "m");
            String helmet = helmet1.replace("#", "k");

            if (!text.equals(temptitle)) {
                if(btAdapter.isEnabled())
                    Oled(title, arrow);

                if (title.contains("*"))
                    tvMsg.setText("Google Maps: " + text + "\n\n Helmet: " + arrowG + ", " + title);
                else
                    tvMsg.setText("Google Maps: " + text + "\n\n Helmet: " + arrowG + " in " + helmet);
              temptitle = text;
            }
        }
    }

    /**
     * Build Notification Listener Alert Dialog.
     */
    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }

    public void Oled(String title, String arrow){

        BluetoothDevice hc05 = btAdapter.getRemoteDevice("00:19:08:35:BA:1E");
        BluetoothSocket btSocket = null;
        try {
            btSocket = hc05.createRfcommSocketToServiceRecord(mUUID);
            System.out.println(btSocket);
            btSocket.connect();
            System.out.println(btSocket.isConnected());

        } catch (IOException e) {
            e.printStackTrace();
        }

        String clear = "/", in = "!@";
        try {
            assert btSocket != null;
            OutputStream outputStream = btSocket.getOutputStream();
            outputStream.write(clear.getBytes());
            outputStream.write(arrow.getBytes());
            if (!title.contains("*"))
              outputStream.write(in.getBytes());
            outputStream.write(title.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            btSocket.close();
            System.out.println(btSocket.isConnected());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_ENABLE_BT) {  // Match the request code
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth Turned on", Toast.LENGTH_LONG).show();
            } else {   // RESULT_CANCELED
                Toast.makeText(this, "Bluetooth must be turned on to connect to helmet", Toast.LENGTH_LONG).show();
            }
        }
    }

}