package com.sustech.ntagI2C;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class GraphFragment extends Fragment {
    int count = 0;
    private static LineChart vote,resis;
    List<Entry> vote_data1 =new ArrayList<>();
    List<Entry> vote_data2 =new ArrayList<>();
    List<Entry> resis_data =new ArrayList<>();
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.fragment_graph, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vote = view.findViewById(R.id.vote);
        resis = view.findViewById(R.id.resis);

    }

    public void addData(float[] data){
        if(data.length<4){return;}
        vote_data1.add(new Entry(count,data[0]));
        vote_data2.add(new Entry(count,data[1]));
        resis_data.add(new Entry(count,data[3]));
        if(count>5){
            vote_data1.remove(0);
            vote_data2.remove(0);
            resis_data.remove(0);
        }
        LineDataSet vote_set_1 = new LineDataSet(vote_data1,"Vdrv");
        vote_set_1.setCircleColor(Color.BLUE);
        LineData line_vote = new LineData(vote_set_1);
        LineDataSet vote_set_2 = new LineDataSet(vote_data2,"VSource");
        vote_set_2.setCircleColor(Color.WHITE);
        line_vote.addDataSet(vote_set_2);
        vote.setData(line_vote);
        vote.notifyDataSetChanged();
        vote.invalidate();
        LineData line_resis = new LineData(new LineDataSet(resis_data,"Resis"));
        resis.setData(line_resis);
        resis.notifyDataSetChanged();
        resis.invalidate();
        count++;
    }


}
