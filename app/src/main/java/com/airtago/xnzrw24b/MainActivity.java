package com.airtago.xnzrw24b;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.airtago.xnzrw24b.data.ChannelInfo;
import com.airtago.xnzrw24b.data.NetworkInfo;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity implements NetworkInfoFragment.OnListFragmentInteractionListener, ChannelInfoFragment.OnListFragmentInteractionListener {

    private NetworkInfo mSelectedNetwork;
    private int mSelectedChannel = 0;
    private LevelCalculator mLevelCalculator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        networksFragment = NetworkInfoFragment.newInstance(this);
        mChannelsFragment = ChannelInfoFragment.newInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCameraFragment = CameraFragment.newInstance();
        } else {
            mCameraFragment = OldCameraFragment.newInstance();
        }
        mCameraFragmentInterface = (CameraFragmentInterface) mCameraFragment;

        getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_NONE).replace(R.id.networks_list_content_frame, networksFragment).commit();
        getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_NONE).replace(R.id.camera_content_frame, mCameraFragment).commit();
        getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_NONE).replace(R.id.channels_list_content_frame, mChannelsFragment).commit();

        handler = new android.os.Handler() {
            public void handleMessage(android.os.Message msg) {
                //mCameraFragmentInterface.setLevel(-84);
                if (msg.what == MessageFields.CODE_DATA) {
                    Log.d(TAG, "ant=" + msg.getData().getInt(MessageFields.FIELD_ANT_INT) + " ch=" + msg.getData().getInt(MessageFields.FIELD_CH_INT) + " ssid=" +msg.getData().getString(MessageFields.FIELD_SSID_STR) + " mac=" + msg.getData().getString(MessageFields.FIELD_MAC_STR) + " rssi=" + msg.getData().getDouble(MessageFields.FIELD_RSSI_DOUBLE));
                    try {
                        WFPacket packet = new WFPacket(msg.getData().getString(MessageFields.FIELD_RAW_STR));
                        networksFragment.addInfo(packet);
                        if (mSelectedNetwork != null && mSelectedNetwork.Ssid.equals(packet.apName) && mSelectedNetwork.Mac.equals(packet.mac)) {
                            mChannelsFragment.addInfo(packet);

                            if (mLevelCalculator != null) {
                                mLevelCalculator.handleInfo(packet);
                                mCameraFragmentInterface.setLevel(mLevelCalculator.getAvg());
                            }
                        }
                    } catch (WFParseException e) {
                        e.printStackTrace();
                    }
                }
            };
        };
        //handler.sendMessageDelayed(handler.obtainMessage(999), 3000);
    }

    private android.os.Handler handler;

    private NetworkInfoFragment networksFragment;
    private ChannelInfoFragment mChannelsFragment;
    private Fragment mCameraFragment;
    private CameraFragmentInterface mCameraFragmentInterface;

    private final String TAG = MainActivity.class.getSimpleName();
    private Thread thread;
    private UsbSerialPortTi port = null;

    private final int INIT_INTERVAL_MS = 3000;
    private final int READ_TIMEOUT_MS  = 500;
    private final int READ_BUF_SIZE    = ItsPacketCreator.PACK_LEN * 16;

    private final int STATE_INIT    = 0;
    private final int STATE_READING = 1;
    private final int STATE_EXIT    = 255;
    private int state = STATE_INIT;

    private int curChan = 1;

    private WFPacketCreator wfCreator = new WFPacketCreator();

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        //filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);

        changeState(STATE_INIT);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                usbLoop();
            }
        });
        thread.start();
    }

    @Override
    protected void onStop() {
        mCameraFragmentInterface.clearRadius();
        thread.interrupt();

        super.onStop();
    }

    private void usbLoop() {
        boolean isRunning = true;

        while (!Thread.currentThread().isInterrupted() && isRunning) {
            if (state == STATE_INIT) {
                stepInit();
            } else if (state == STATE_READING) {
                stepRead();
            } else if (state == STATE_EXIT ) {
                isRunning = false;
            } else {
                Log.e(TAG, "Logic error");
                isRunning = false;
            }
        }

        Log.d(TAG, "I was interrupted");
        closeDevice();
    }

    private Thread changer = null;

    private void stepInit() {
        port = new UsbSerialPortTi(getApplicationContext());

        boolean inited = false;

        while (!inited && !Thread.currentThread().isInterrupted()) {
            try {
                port.init();
                sendInfo("Device init OK");
                inited = true;
                changeState(STATE_READING);

                if ( changer != null ) {
                    changer.interrupt();
                }

                changer = createChangerThread();
                changer.start();

            } catch (DeviceNotFoundException e) {
                sendError("Device was not found");
            } catch (DeviceOpenFailedException e) {
                sendError("Device init error. Check Permissions.");
            }

            if (!inited) {
                try {
                    Thread.sleep(INIT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Log.d(TAG, "I was interrupted");
                    changeState(STATE_EXIT);
                    return;
                }
            }

        }
    }

    private Thread createChangerThread() {
        return new Thread() {
            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        Thread.sleep(INIT_INTERVAL_MS);
                        Log.d(TAG, "Going to change channel");
                        changeChannel();
                        Log.d(TAG, "---------------------");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "  *** CHANGER EXIT ***");
            }
        };
    }

    private void stepRead() {
        byte[] buf = new byte[READ_BUF_SIZE];
        int read = 0;
        try {
            read = port.read(buf, READ_TIMEOUT_MS);
            //Log.d( TAG, "Read " + read );


            wfCreator.putData(buf, read);
            ArrayList<WFPacket> packets = wfCreator.getPackets();
            //Log.d(TAG, "Have " + packets.size() + " wfifi packets" );

            if ( packets.size() > 0 ) {
                //TODO: Send packets
                for (WFPacket p : packets) {
                    sendInfo(p.toString());
                    sendData(p);
                }
            }
        } catch (IOException e) {
            sendError("IOError");
            closeDevice();
            changeState(STATE_INIT);
//        }
//        catch (BadRssiException e) {
//            // nop
//            Log.d( TAG, "Bad Rssi" );
        } catch (Exception e) {
            Log.e(TAG, "Parse error: read " + read + " bytes");
            String sss = "data: ";
            String str = "";
            for ( int i = 0; i < read; i++ ) {
                sss += String.format("0x%02x ", buf[i]);
                if ( buf[i] != 0x0d ) {
                    str += Character.toString((char) buf[i]);
                }
            }
            Log.e(TAG, sss);
            Log.e(TAG, str);

            sendError("ERROR");

            //changeChannel();

        }
    }

    private void changeChannel() {
        changeChannel(0);
    }

    private void changeChannel(int chan) {
        Log.d( TAG, "changeChannel " );
        if ( port != null ) {
            try {
                if (chan > 0)
                    port.write(getChannelChar(chan), READ_TIMEOUT_MS);
                else
                    port.write(getChannelChar(curChan), READ_TIMEOUT_MS);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            if ( ++curChan > 13 ) {
                curChan = 1;
            }

        }
    }

    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.d(TAG, "Device was detached");
                    sendInfo("Device was detached");
                    closeDevice();
                    changeState(STATE_INIT);
                }
            }
        }
    };

    private void changeState( int newState ) {
        state = newState;
    }

    private void closeDevice() {
        if ( port != null ) {
            port.close();
        }
    }

    private void sendInfo( String infoString ) {
        Message message = handler.obtainMessage(MessageFields.CODE_INFO);
        //message.setAction(ServiceConstants.ACTION_NAME);
        message.getData().putInt(MessageFields.FIELD_CODE_INT, MessageFields.CODE_INFO);
        message.getData().putString(MessageFields.FIELD_INFO_STR, infoString);
        //sendBroadcast(intent);
        handler.sendMessage(message);
    }

    private void sendError( String errorString ) {
        Message message = handler.obtainMessage(MessageFields.CODE_ERROR);
        //intent.setAction(ServiceConstants.ACTION_NAME);
        message.getData().putInt(MessageFields.FIELD_CODE_INT, MessageFields.CODE_ERROR);
        message.getData().putString(MessageFields.FIELD_INFO_STR, errorString);
        //sendBroadcast(intent);
        handler.sendMessage(message);
    }

    private void sendData(WFPacket packet) {
        Message message = handler.obtainMessage(MessageFields.CODE_DATA);
        //intent.setAction(ServiceConstants.ACTION_NAME);
        message.getData().putInt(MessageFields.FIELD_CODE_INT, MessageFields.CODE_DATA);
        message.getData().putString(MessageFields.FIELD_SSID_STR, packet.apName);
        message.getData().putString(MessageFields.FIELD_MAC_STR, packet.mac);
        message.getData().putLong(MessageFields.FIELD_TIME_MS_LONG, packet.time);
        message.getData().putInt(MessageFields.FIELD_CH_INT, packet.wifiCh);
        message.getData().putDouble(MessageFields.FIELD_RSSI_DOUBLE, packet.power);
        message.getData().putDouble(MessageFields.FIELD_DIFF_DOUBLE, packet.diff);
        message.getData().putString(MessageFields.FIELD_RAW_STR, packet.raw);
        //sendBroadcast(intent);
        handler.sendMessage(message);
    }

    private byte getChannelChar( int channel ) {
        if (channel < 10)         {
            return (byte)(48 + channel);
        } else {
            return (byte)(65 + channel - 10);
        }
    }

    @Override
    public void onListFragmentInteraction(NetworkInfo item) {
        if (mSelectedNetwork == null || mSelectedNetwork != item) {
            mSelectedChannel = 0;
            mLevelCalculator = null;
            mSelectedNetwork = item;
            mChannelsFragment.setNetwork(item);
            mCameraFragmentInterface.clearRadius();
            if (changer == null) {
                changer = createChangerThread();
                changer.start();
            }
        }
    }

    @Override
    public void onListFragmentInteraction(ChannelInfo item) {
        if (mSelectedChannel != item.Channel) {
            mCameraFragmentInterface.clearRadius();
            mLevelCalculator = new LevelCalculator(mSelectedNetwork.Ssid, mSelectedNetwork.Mac);
            mSelectedChannel = item.Channel;
            if (changer != null) {
                changer.interrupt();
                changer = null;
            }
            changeChannel(mSelectedChannel);
        }
    }
}
