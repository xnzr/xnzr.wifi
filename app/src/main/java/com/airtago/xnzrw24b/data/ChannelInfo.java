package com.airtago.xnzrw24b.data;

import android.os.Bundle;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by alexe on 10.01.2017.
 */

public final class ChannelInfo {

    public ChannelInfo(int channel) {
        Channel = channel;
    }

    public int Channel;
    public int Rssi;

    private final int MAX_HISTORY = 20;

    private ArrayList<Double> mHistory1 = new ArrayList<>();
    private ArrayList<Double> mHistory2 = new ArrayList<>();
    private double mRssi;
    private double mRssiDiff;

    public void AddRssi1(double rssi) {
        mHistory1.add(rssi);
        if (mHistory1.size() > MAX_HISTORY)
            mHistory1.remove(0);
        //OnPropertyChanged(nameof(AvgRssi1));
    }

    public void AddRssi2(double rssi) {
        mHistory2.add(rssi);
        if (mHistory2.size() > MAX_HISTORY)
            mHistory2.remove(0);
        //OnPropertyChanged(nameof(AvgRssi2));
    }

    public double AvgRssi1() {
        double r = 0;
        for (double d: mHistory1) {
            r += d;
        }
        return mHistory1.size() > 0 ? r / mHistory1.size() : -255d;
    }

    public double AvgRssi2() {
        double r = 0;
        for (double d: mHistory2) {
            r += d;
        }
        return mHistory2.size() > 0 ? r / mHistory2.size() : -0d;
    }

    private double[] convertDoubles(ArrayList<Double> doubles) {
        double[] ret = new double[doubles.size()];
        Iterator<Double> iterator = doubles.iterator();
        int i = 0;
        while(iterator.hasNext()) {
            ret[i++] = iterator.next();
        }
        return ret;
    }

    private void convertToDoubles(ArrayList<Double> list, double[] values) {
        list.clear();
        for (int i = 0; i < values.length; ++i) {
            list.add(values[i]);
        }
    }

    public void serialize(Bundle bundle) {
        bundle.putInt("channel", Channel);
        bundle.putDoubleArray("h1", convertDoubles(mHistory1));
        bundle.putDoubleArray("h2", convertDoubles(mHistory2));
    }

    public void deserialize(Bundle bundle) {
        Channel = bundle.getInt("channel");
        convertToDoubles(mHistory1, bundle.getDoubleArray("h1"));
        convertToDoubles(mHistory2, bundle.getDoubleArray("h2"));
    }
}
