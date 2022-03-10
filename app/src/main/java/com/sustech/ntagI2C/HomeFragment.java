package com.sustech.ntagI2C;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sustech.ntagI2C.utils.FragmentPagerItem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeFragment extends Fragment {
    public static int LED = 0;
    private static TextView vsource,vdrv,chn,resis;
    private static Button button_LED;
    private static File file;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int position = FragmentPagerItem.getPosition(getArguments());
        findView(view);
        String path=getContext().getExternalFilesDir(null).getAbsolutePath();
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String dateString = formatter.format(currentTime);
        file = new File(path+"/log-"+dateString+".txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void findView(View view){
        vsource = view.findViewById(R.id.vsource);
        vdrv = view.findViewById(R.id.vdrv);
        chn = view.findViewById(R.id.chn);
        resis = view.findViewById(R.id.resis);
        button_LED = view.findViewById(R.id.led);
        button_LED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(LED==0){LED=1;button_LED.setText("Close");}
                else{LED=0;button_LED.setText("Open");}
            }
        });
    }
    public void setDisplay(float[] data){
        if(data.length<4){return;}
        vsource.setText(String.format("%.2f ℃",data[0]));
        vdrv.setText(String.format("%.2f ℃",data[1]));
        chn.setText(String.format("%.0f ",data[2]));
        resis.setText(String.format("%.3f ℃",data[3]));
        writeLog(data);
    }
    public void writeLog(float[] data){
//        if(file==null||!file.exists()){
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String dateString = formatter.format(currentTime);
        @SuppressLint("DefaultLocale") String ans = String.format("[%s]Sensor1=%.2f Sensor2=%.2f Chanel=%.0f TemperatureDif=%.3f\n",dateString,data[0],data[1],data[2],data[3]);
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true)));
            out.write(ans);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

