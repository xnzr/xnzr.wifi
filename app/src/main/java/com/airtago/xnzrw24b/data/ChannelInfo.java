package com.airtago.xnzrw24b.data;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by alexe on 10.01.2017.
 */

public final class ChannelInfo {

    public ChannelInfo(int channel) {
        Channel = channel;
    }

    public int Channel;

    private final int MAX_HISTORY = 20;

    private ArrayList<Double> mHistory = new ArrayList<>();
    private ArrayList<Double> mHistoryDiff = new ArrayList<>();
    private double mRssi;
    private double mRssiDiff;

    public void AddRssi(double rssi, double diff) {
        mRssi = rssi;
        mRssiDiff = diff;

        mHistory.add(rssi);
        mHistoryDiff.add(diff);
        if (mHistory.size() > MAX_HISTORY)
        {
            mHistory.remove(0);
            mHistoryDiff.remove(0);
        }
        //OnPropertyChanged(nameof(AvgRssi1));
    }

    public double AvgRssi() {
        return mRssi;

//        double r = 0;
//        for (double d: mHistory) {
//            r += d;
//        }
//        return mHistory.size() > 0 ? r / mHistory.size() : -255d;
    }

    public double Diff() {
        return mRssiDiff;

//        double r = 0;
//        for (double d: mHistoryDiff) {
//            r += d;
//        }
//        return mHistoryDiff.size() > 0 ? r / mHistoryDiff.size() : -0d;
    }
}
