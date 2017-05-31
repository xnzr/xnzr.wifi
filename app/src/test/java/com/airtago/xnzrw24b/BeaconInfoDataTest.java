package com.airtago.xnzrw24b;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by alexe on 10.01.2017.
 */
public class BeaconInfoDataTest {
    @Test
    public void fromString() throws Exception {
        BeaconInfoData info = BeaconInfoData.FromString("2 06 9027E45EA88D 12E69160 -079 WiFi Alexander");
        assertEquals(1, info.rcvIdx);
        assertEquals(6, info.wifiChan);
        assertEquals("9027E45EA88D", info.mac);
        assertEquals(317100384, info.time);
        assertEquals(-79, (int)info.level);
        assertEquals("WiFi Alexander", info.ssid);
    }

}