package com.airtago.xnzrw24b;

import android.util.Log;

public class WFPacket {

    public int wifiCh = 0;
    public double power = -100.0;
    public double diff = 0.0;
    public String apName = "";
    public String mac = "";
    public long time = 0;
    public String raw;

    private static final String TAG = WFPacket.class.getSimpleName();

    private final String delimeters = "[ ]+";


    public WFPacket( String str ) throws WFParseException {
        String[] tokens = str.split(delimeters);

        if ( tokens.length < 5 ) {
            throw new WFParseException( "Have " + tokens.length + " tokens in string '" + str + "'" );
        }

        wifiCh = Integer.parseInt(tokens[1], 10);
        mac = tokens[0];
        power  = Double.parseDouble(tokens[3]);
        diff  = Double.parseDouble(tokens[2]);
        //diff = Math.pow(2, diff);

        for ( int i = 4; i < tokens.length; i++ ) {
            apName += tokens[i];
            if ( i != tokens.length - 1 ) {
                apName += " ";
            }
        }

        raw = str;
    }

    public String toString() {
        return String.format( "%16s %2d   %5.1f",
                apName, wifiCh, power);
    }

}
