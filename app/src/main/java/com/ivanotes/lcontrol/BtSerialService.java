package com.ivanotes.lcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BtSerialService {
	// Debugging
    private static final String TAG = "BTCar";
    private static final String DSTR = "BtSerialService"; //Stands for debug string
    private static final boolean D = true;
    //It is used like this: if(D) Log.d(TAG, DSTR + "My debug message");
    
    // Unique UUID for this application -taken from BluetoothChat example
    // Not safe for production
    private static final UUID MY_UUID_SECURE =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    // UUID for devices like HC-05
    private static final UUID MY_UUID_OTHER =
    	UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    
    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    
 // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    // Handlers message
    protected static final int SUCCESS_CONNECT = 0 ;
    protected static final int MESSAGE_READ = 1 ;
    
    public static boolean connectSucess=false;
    
    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BtSerialService(Context context, Handler handler) {
    	if (D) Log.d(TAG, DSTR + "SerialService created");
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }
    
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        //if (D) Log.d(TAG, DSTR + "Connect to: " + device.getName());
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        if (D) Log.d(TAG, " Connected, Socket ");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }
    
    public void callStop(){
    	stop();
        //Toast.makeText(cntxt.getApplicationContext(), "Booyah", Toast.LENGTH_SHORT).show();
    }
    
    public void writeToDevice(byte[] buffer){
    	mConnectedThread.write(buffer);
    }
    
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device, boolean secure) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	        
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	        	if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                	// MY_UUID is the app's UUID string, also used by the server code
    	            tmp = device.createRfcommSocketToServiceRecord(
    	            		MY_UUID_OTHER);
                    //tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
	            
	        } catch (IOException e) {
	        	Log.e(TAG, "Socket Type: Other , create() failed", e);
	        }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	    	// Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();		        
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	                //Context c= this
	                Log.i(TAG,DSTR+"Socket sucessfully closed");
	                callStop();
	                connectSucess=false;
	            } catch (IOException closeException) {
	            	Log.e(TAG,DSTR+ "unable to close() " +
                            " socket during connection failure", closeException);
	            }
	            return;
	        }
	        
	        //Handler to communicate with the UI
	        //Log.i(TAG,DSTR+"Before handler, socket to string: "+mmSocket.toString());
            //mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
	        // Do work to manage the connection (in a separate thread)
	        //manageConnectedSocket(mmSocket);
	        
	        // Reset the ConnectThread because we're done
            synchronized (BtSerialService.this) {
                mConnectThread = null;
            }
            connectSucess=true;
            // Start the connected thread
            connected(mmSocket, mmDevice);
	        
	        
	    }
	 
		/** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	/**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "Create ConnectedThread ");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            //byte[] buffer = new byte[1024];
            int bytes;
            int availableBytes=0;
            int desiredBytes = 13; //has to wait until it gets all the characters

            // Keep listening to the InputStream while connected
            while (true) {
            	try {
                    availableBytes = mmInStream.available();
                    if(availableBytes > desiredBytes){
                        byte[] buffer = new byte[availableBytes];  // buffer store for the stream
                        // Read from the InputStream


                        bytes = mmInStream.read(buffer);
                        //Log.d("mmInStream.read(buffer);", new String(buffer));
                        if( bytes > 0 ){                        
                            // Send the obtained bytes to the UI activity
                        	mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            	.sendToTarget();                     
                        }                                   
                    }
                } catch (IOException e) {
                    Log.d("Error reading", e.getMessage());
                    e.printStackTrace();
                    break;
                }

            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                if(D) Log.d(TAG, DSTR + " Enviando comando bt");
                if(D) Log.d(TAG, DSTR + new String(buffer));
                mmOutStream.write(buffer);
                if(D) Log.d(TAG, DSTR + "Comando enviado");
                // Share the sent message back to the UI Activity
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}
