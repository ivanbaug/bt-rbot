package com.ivanotes.lcontrol;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;

public class Controlling extends Activity {

    // Debugging
    private static final String TAG = "BTCar";
    private static final String DSTR = "Controlling"; //Stands for debug string
    private static final boolean D = true;
    //It is used like this: if(D) Log.d(TAG, DSTR + "My debug message");

    //private BluetoothSocket mBTSocket; // maybe this one will be deleted, replaced by:

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the bluetooth services
    private BtSerialService mBtService = null;

    //private ReadInput mReadThread = null;

    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;


    private Button mBtnDisconnect;
    private BluetoothDevice mDevice;

    final static String on="92#";//on
    final static String off="79#";//off
    private char START_CMD_CHAR = '*';
    private char END_CMD_CHAR = '#';
    private char DIV_CMD_CHAR = '|';

    private int CMD_MOVE_CAR = 14;
    private int CMD_MOVE_SERVO_ARMS = 16;
    private int CMD_FAKE = 1;

    private int CMD_FW = 0x0A;
    private int CMD_BW = 0x0B;
    private int CMD_L = 0x0C;
    private int CMD_R = 0x0D;
    private int CMD_STOP = 0x0E;

    private int CMD_ARM_R = 0x0A;
    private int CMD_ARM_L = 0x0B;

    private int minSpeed = 140;



    private ProgressDialog progressDialog;

    private TextView tvSpeed;
    private SeekBar sbSpeed,sbArmRight,sbArmLeft;
    //private Button btnFW;
    private ImageButton btnFW, btnBW, btnRight, btnLeft;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlling);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //ActivityHelper.initialize(this);
        // mBtnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        tvSpeed=(TextView)findViewById(R.id.tv_speed_info);
        sbSpeed=(SeekBar)findViewById(R.id.sb_speed);
        sbArmRight=(SeekBar)findViewById(R.id.sb_arm_right);
        sbArmLeft=(SeekBar)findViewById(R.id.sb_arm_left);
        btnFW = (ImageButton) findViewById(R.id.btn_fwd);
        btnBW =(ImageButton)findViewById(R.id.btn_bwd);
        btnRight=(ImageButton)findViewById(R.id.btn_right);
        btnLeft=(ImageButton)findViewById(R.id.btn_left);

        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);

                // Initialize the BtSerialService to perform bluetooth connections
        mBtService = new BtSerialService(this, mHandler);

        //Request connect device
        connectDevice(false);

        Log.d(TAG, "Ready");

        sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress=0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue+ minSpeed; //Previous android versions didnt have min value so we have to add it manually in this case 140 to make it compatible with older devices
                tvSpeed.setText("Velocidad: "+ progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbArmRight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress=0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress=i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int myProgress = progress+90; //Previous android versions didnt have min value so we have to add it manually in this case 90 to make it compatible with older devices
                sendCommandAD(CMD_MOVE_SERVO_ARMS, CMD_ARM_R, myProgress);
            }
        });

        sbArmLeft.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress=0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress=i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int myProgress = 174-progress; //Previous android versions didnt have min value so we have to add it manually in this case 180-x to make it compatible with older devices
                sendCommandAD(CMD_MOVE_SERVO_ARMS, CMD_ARM_L, myProgress);
            }
        });

        btnFW.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    sendCommandAD(CMD_MOVE_CAR, CMD_FW, sbSpeed.getProgress()+ minSpeed);
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    sendCommandAD(CMD_MOVE_CAR, CMD_STOP, CMD_FAKE);
                }
                return true;
            }
        });

        btnBW.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    sendCommandAD(CMD_MOVE_CAR, CMD_BW, sbSpeed.getProgress()+ minSpeed);
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    sendCommandAD(CMD_MOVE_CAR, CMD_STOP, CMD_FAKE);
                }
                return true;
            }
        });

        btnRight.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    sendCommandAD(CMD_MOVE_CAR, CMD_R, sbSpeed.getProgress()+ minSpeed);
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    sendCommandAD(CMD_MOVE_CAR, CMD_STOP, CMD_FAKE);
                }
                return true;
            }
        });

        btnLeft.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    sendCommandAD(CMD_MOVE_CAR, CMD_L, sbSpeed.getProgress()+ minSpeed);
                }
                if(event.getAction() == MotionEvent.ACTION_UP){
                    sendCommandAD(CMD_MOVE_CAR, CMD_STOP, CMD_FAKE);
                }
                return true;
            }
        });




    }




    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Resumed");
        super.onResume();

        if (mBtService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBtService.getState() == BtSerialService.STATE_NONE) {
                // Start the Bluetooth chat services
                //mBtService.start();
            }
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
    }


    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BtSerialService.STATE_CONNECTED:
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mConversationArrayAdapter.clear();
                            break;
                        case BtSerialService.STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            break;
                        case BtSerialService.STATE_LISTEN:
                            break;
                        case BtSerialService.STATE_NONE:
                            //setStatus(R.string.title_not_connected);
                            if (!mBtService.connectSucess){
                                Toast.makeText(getApplicationContext(), R.string.toast_bt_cant_pair, Toast.LENGTH_LONG).show();
                            }
                            break;

                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    String s = (String) msg.obj;
                    byte[] writeBuf = (byte[]) s.getBytes(Charset.forName("UTF-8"));
                    mBtService.writeToDevice(writeBuf);
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
                    //tReceive.manageString(readMessage); //this is when i had incoming data
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    break;
                /*
                 case MESSAGE_READ :
    				String s = new String((byte[])msg.obj);
    				Log.i(TAGG,"Handler Message read: "+s);
    				Toast.makeText(getApplicationContext(),
    						"Incomming message is: "+s
    						, Toast.LENGTH_SHORT).show();
    				//cT.cancel();
        			break;
                 */
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    };

    private void sendCommandAD(int command, int value1, int value2){
        /*
         * Sends Command ARDU DRIOD Style...
         * Format:
         * START_CMD_CHAR '*' => 0x2A
         * C_FW 10
         * #
         */
        String s = START_CMD_CHAR +
                Integer.toString(command) +
                ',' +
                Integer.toString(value1) +
                ',' +
                Integer.toString(value2) +
                END_CMD_CHAR;

        byte[] writeBuf = (byte[]) s.getBytes(Charset.forName("UTF-8"));
        mBtService.writeToDevice(writeBuf);

    }

    private void connectDevice(boolean secure) {
        // Attempt to connect to the device
        if (mDevice != null){
            if(D) Log.d(TAG, DSTR + " There is no error well... "+mDevice.getName());
            mBtService.connect(mDevice, secure);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBtService != null){
            mBtService.callStop();
        }
        super.onDestroy();
    }
}