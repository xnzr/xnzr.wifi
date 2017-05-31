package com.airtago.xnzrw24b;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by alexe on 12.01.2017.
 */

public final class LevelCalculator {
    private String ssid;
    private String mac;
    private int avgCount = 15;
    private double avgDiff = 0.0;
    private boolean needRecalc = true;
    private LinkedList<Double> mLevels = new LinkedList<Double>();
    private LinkedList<Double> mDiffs = new LinkedList<Double>();

    private final String TAG = LevelCalculator.class.getSimpleName();

    public LevelCalculator(String ssid, String mac) {
        this.ssid = ssid;
        this.mac = mac;
    }

    public boolean handleInfo(WFPacket data)
    {
        //data.print();
        if (data.apName.equals(ssid) && data.mac.equals(mac)) {
            mLevels.add(data.power);
            mDiffs.add(data.diff);
            while (mLevels.size() > avgCount) {
                mLevels.removeFirst();
                mDiffs.removeFirst();
            }
            needRecalc = true;
            print();
        } else {
            //Console.WriteLine("LevelCalculator.HandleInfo() skip ssid " + data.ssid);
        }
        return needRecalc;
    }

    public void print() {
        double diff = getAvg();
        Log.d(TAG, String.format("\r                                                          \rLEVEL: %5.2f       rssi: %6.2f", diff, GetCurrent()));
    }

    public double getAvg() {
        if (needRecalc) {
            double sum = 0d;
            for (Double x: mDiffs) {
                sum += x;
            }
            int count = mDiffs.size();
            if (count != 0) {
                sum /= count;
            }
            avgDiff = sum;
            needRecalc = false;
        }
        return avgDiff;
    }

    public double GetCurrent() {
        return !mLevels.isEmpty() ? mLevels.peekLast() : -100;
    }
}
