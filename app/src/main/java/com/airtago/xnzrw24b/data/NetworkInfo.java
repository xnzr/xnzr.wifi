package com.airtago.xnzrw24b.data;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.airtago.xnzrw24b.BeaconInfoData;
import com.airtago.xnzrw24b.WFPacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static android.R.attr.x;

/**
 * Created by alexe on 10.01.2017.
 */

public final class NetworkInfo {
    public NetworkInfo() { }

    public NetworkInfo(BeaconInfoData info) {
        Ssid = info.ssid;
        Mac = info.mac;
    }

    public NetworkInfo(WFPacket info) {
        Ssid = info.apName;
        Mac = info.mac;
    }

    public String Ssid;
    public String Mac;

    public ArrayList<ChannelInfo> Channels = new ArrayList<ChannelInfo>();

    public boolean addChannel(WFPacket packet) {
        boolean result = false;
        ChannelInfo chan = null;

        for (ChannelInfo ch: Channels) {
            if (ch.Channel == packet.wifiCh) {
                chan = ch;
                break;
            }
        }
        if (chan == null) {
            chan = new ChannelInfo(packet.wifiCh);
            Channels.add(chan);
            result = true;
        }
        if (packet.antIdx == 0)
            chan.AddRssi1(packet.power);
        else if (packet.antIdx == 1)
            chan.AddRssi2(packet.power);
        return result;
    }

    public void serialize(Bundle bundle) {
        bundle.putString("mac", Mac);
        bundle.putString("ssid", Ssid);
        bundle.putInt("count", Channels.size());
        int index = 0;
        for (ChannelInfo ch: Channels) {
            Bundle bch = new Bundle();
            ch.serialize(bch);
            bundle.putBundle("ch" + index++, bch);
        }
    }

    public void deserialize(Bundle bundle) {
        Mac = bundle.getString("mac");
        Ssid = bundle.getString("ssid");
        int count = bundle.getInt("count");
        for (int i = 0; i < count; ++i) {
            Bundle bch = bundle.getBundle("ch" + i);
            ChannelInfo ch = new ChannelInfo(0);
            ch.deserialize(bch);
            Channels.add(ch);
        }
    }
}
