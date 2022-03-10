package com.sustech.ntagI2C.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;

import com.sustech.ntagI2C.Ntag_I2C_Demo;
import com.sustech.ntagI2C.R;

public class MainActivity extends Activity {
    public static Ntag_I2C_Demo demo;
    private PendingIntent mPendingIntent; //delay intent
    private NfcAdapter mAdapter;          //NFC adapter
    private static TextView tv_display;   //display i2c data
    private static TextView tv_dir;       //display direction
    private static CheckBox Sram0to31_checkbox;
    private static CheckBox Sram32to61_checkbox;


    public final static int AUTH_REQUEST = 0;
    // Current authentication state
    private static int mAuthStatus;

    // Current used password
    private static byte[] mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_display = (TextView) findViewById(R.id.tv_nfc_result);
        Sram0to31_checkbox = (CheckBox) findViewById(R.id.Sram0to31_checkbox);
        Sram32to61_checkbox = (CheckBox) findViewById(R.id.Sram32to61_checkbox);
        tv_dir = (TextView) findViewById(R.id.dir);


        //Initialize the demo
        //demo = new Ntag_I2C_Demo(null, this,null, 0);
        Log.v("MainActivity","zhongrf onCreate");
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        setNfcForeground();
        checkNFC();

    }

    public void setNfcForeground() {
        // Create a generic PendingIntent that will be delivered to this
        // activity. The NFC stack will fill
        // in the intent with the details of the discovered tag before
        // delivering it to this activity.
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(
                getApplicationContext(), getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @SuppressLint("InlinedApi")
    private void checkNFC() {
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                new AlertDialog.Builder(this)
                        .setTitle("NFC not enabled")
                        .setMessage("Go to Settings?")
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        if (android.os.Build.VERSION.SDK_INT >= 16) {
                                            startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                                        } else {
                                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                                        }
                                    }
                                })
                        .setNegativeButton("No",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        System.exit(0);
                                    }
                                }).show();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("No NFC available. App is going to be closed.")
                    .setNeutralButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    System.exit(0);
                                }
                            }).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
            Log.v("MainActivity","zhongrf onPause mAdapter is not null");
        }
        if(demo == null) {
            Log.v("MainActivity","zhongrf demo is null");
        }
        if(demo != null) {
            if (demo.isReady()) {

                demo.LEDFinish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    public void onNewIntent(Intent nfc_intent) {
        super.onNewIntent(nfc_intent);
        // set the pattern for vibration
        long pattern[] = {0, 100};

        // vibrate on new intent
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, -1);
        doProcess(nfc_intent);
    }

    public void doProcess(Intent nfc_intent) {
        Tag tag = nfc_intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        demo = new Ntag_I2C_Demo(tag, this,mPassword, mAuthStatus);
        demo.LED();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    // ===========================================================================
    // NTAG I2C Plus getters and setters
    // ===========================================================================
    public static int getAuthStatus() {
        return mAuthStatus;
    }

    public static byte[] getPassword() {
        return mPassword;
    }

    public static Intent getNfcIntent() {
        return null;
    }

    public static void setAuthStatus(int status) {    }

    public static void setPassword(byte[] pwd) { }


    public static void setNfcIntent(Intent intent) {

    }

    public static void setTransferDir(String answer) {
        tv_dir.setText(answer);
    }

    public static void setSensorDisplay(String answer) {
        tv_display.setText(answer);
    }

    public static boolean isSram0to31Enabled() {
        return true;
    }

    public static boolean isSram32to61Enabled() {
        return true;
    }
}

