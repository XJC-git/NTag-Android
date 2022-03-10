package com.sustech.ntagI2C;

import android.annotation.SuppressLint;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.sustech.ntagI2C.utils.FragmentPagerItem;
import com.sustech.ntagI2C.utils.FragmentPagerItemAdapter;
import com.sustech.ntagI2C.utils.FragmentPagerItems;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    public static Ntag_I2C_Demo demo;
    private PendingIntent mPendingIntent; //delay intent
    private NfcAdapter mAdapter;
    public final static int AUTH_REQUEST = 0;
    // Current authentication state
    private static int mAuthStatus;
    // Current used password
    private static byte[] mPassword;
    private static HomeFragment homepage;
    private static GraphFragment graphFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lunch);
        setStatusBar();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("NTAG");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        ViewGroup tab = (ViewGroup) findViewById(R.id.tab);
        tab.addView(LayoutInflater.from(this).inflate(R.layout.controller, tab, false));
        FragmentPagerItems pages = new FragmentPagerItems(this);
        pages.add(FragmentPagerItem.of("All Data",HomeFragment.class));
        pages.add(FragmentPagerItem.of("LineChart",GraphFragment.class));
        FragmentPagerItemAdapter adapter = new FragmentPagerItemAdapter(
                getSupportFragmentManager(), pages);
        homepage = (HomeFragment) adapter.getItem(0);
        graphFragment = (GraphFragment) adapter.getItem(1);
        ViewPager viewPager = (ViewPager) findViewById(R.id.main_frag);
        viewPager.setAdapter(adapter);

        SmartTabLayout viewPagerTab = (SmartTabLayout) findViewById(R.id.viewpagertab);
        viewPagerTab.setViewPager(viewPager);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        setNfcForeground();
        checkNFC();



    }
    private void setStatusBar(){
        Window window = this.getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        window.setStatusBarColor(this.getResources().getColor(R.color.white));//设置状态栏颜色
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//状态栏为白色 图标显示深色
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
            Log.v("MainActivity"," onPause mAdapter is not null");
        }
        if(demo == null) {
            Log.v("MainActivity"," demo is null");
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
    public static void setDisplay(float[] data){
        homepage.setDisplay(data);
        graphFragment.addData(data);
    }
    public static void setTransferDir(String answer){
    }

    public static void setSensorDisplay(String answer){
    }

    public static boolean isSram0to31Enabled() {
        return true;
    }

    public static boolean isSram32to61Enabled() {
        return true;
    }
    public static void setNfcIntent(Intent intent) {

    }
}