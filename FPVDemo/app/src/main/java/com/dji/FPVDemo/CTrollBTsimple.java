package com.dji.FPVDemo;

/**
 * Created by atomic on 2017-01-30.
 */

        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothSocket;
        import android.support.annotation.NonNull;


        import java.io.IOException;
        import java.io.InputStream;
        import java.util.Set;
        import java.util.UUID;
        import java.util.concurrent.TimeUnit;
        import java.util.concurrent.locks.Condition;
        import java.util.concurrent.locks.Lock;


public class CTrollBTsimple {

    /// zarzadzanie bluetotth
    private final int mMaxBuffer = 1024;
    byte[] mBuffer = new byte[mMaxBuffer];  // buffer store for the stream
    public byte[] mAllDataBuffer = new byte[mMaxBuffer];  // buffer store for the stream
    public int mBufferSize = 0;



    private boolean mHasBtSocket = false;
    private boolean mHasDevice = false;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mBluetoothStarted = false;
    private int mBluetoothInitiated = -1;
    private int mBluetoothConnected = -1;
    private BluetoothDevice mBluetoothDevice = null;
    private ConnectedThread mDeviceThread = null;
    private BluetoothSocket mSocketDevice = null;

    public String mDeviceName = "unknown";
    public String mMessageString = "BT";

    private Lock mDataLock = new Lock() {
        @Override
        public void lock() {

        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void unlock() {

        }

        @NonNull
        @Override
        public Condition newCondition() {
            return null;
        }
    };

    public void DataLock()
    {
        mDataLock.lock();
    }

    public void DataUnLock()
    {
        mDataLock.unlock();
    }


    public boolean /*BluetoothSocket*/ CreateBluetothSocket(BluetoothDevice device) {
        UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        mHasBtSocket = false;
        BluetoothSocket tempSocket;
        boolean hasSocket = false;

        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tempSocket = device.createRfcommSocketToServiceRecord(mUUID);
            hasSocket = true;

        } catch (IOException e) {
            mMessageString = "exception: createRfcommSocketToServiceRecord" + e.getMessage();
            return false;// tempSocket;
        }


        if (hasSocket) {

            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                tempSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                //    txtBox.setText("Unable to connect");
                mMessageString = "unable to connect" + connectException.getMessage();
                try {
                    tempSocket.close();
                } catch (IOException closeException) {
                }
                return false;//tempSocket;
            }

            mHasBtSocket = true;
        }

        mSocketDevice = tempSocket;
        return true;
    }



    public void StartBluetooth(String name)
    {
        if(mBluetoothStarted) return;

        mBluetoothStarted = true;

        mBluetoothInitiated = 0;
        mBluetoothConnected = 0;

        mHasDevice = false;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mMessageString = device.getName() + " " + device.getAddress();


                //mUUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                if(device.getName().contains(name))
                //if(device.getName().equals(name))
                {
                    mDeviceName = device.getName();
                    mHasBtSocket = CreateBluetothSocket(device);


                    if(mHasBtSocket)
                    {
                        mHasDevice = true;
                        mBluetoothInitiated = 1;
                        mBluetoothConnected = 1;
                        mBluetoothDevice = device;
                        DataUnLock();
                        mDeviceThread = new ConnectedThread(mSocketDevice,0);
                        DataUnLock();
                        mDeviceThread.start();

                        DataUnLock();
                        break;
                    }

                }
            }
        }
    }


    public void BluetoothReConnect()
    {
        if(mBluetoothInitiated > 0 && mBluetoothConnected <= 0)
        {
            boolean hasSocket = CreateBluetothSocket(mBluetoothDevice);

            if(mHasBtSocket)
            {
                mBluetoothConnected = 1;
                mDeviceThread = new ConnectedThread(mSocketDevice,0);
                mDeviceThread.start();
            }
        }
    }

    private String composeString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b: bytes) {
            int i = b;
            builder.append(b);
            builder.append(" ");
        }

        return builder.toString();
    }

    private String composeString2(byte[] bytes, int size) {
        String builder = new String();
        int count = 0;
        for (byte b: bytes) {
            int i = b;
            builder += String.valueOf(i);

            count++;
            if(count >= size)
                return builder;
        }

        return builder;
    }




    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        //private final OutputStream mmOutStream;
        public float mPacket;
        private int mSocketID;

        public ConnectedThread(BluetoothSocket socket,int id) {
            mmSocket = socket;
            InputStream tmpIn = null;
            mSocketID = id;
            //OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                // tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                mMessageString = "exception " + e.getMessage();
            }

            mmInStream = tmpIn;
            //mmOutStream = tmpOut;

            mPacket = 0;

        }

        public void run() {
            int bytes; // bytes returned from read()


            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(mBuffer);

                    if(bytes > 0) {
                        DataLock();
                        for (int i = 0; i < bytes; i++) {
                            //str += (char)mBuffer[i];

                            mAllDataBuffer[mBufferSize] = mBuffer[i];
                            mBufferSize++;
                            if (mBufferSize >= mMaxBuffer)
                                mBufferSize = 0;
                        }
                        DataUnLock();
                    }


                } catch (IOException e) {
                    mMessageString = "exception " + e.getMessage();
                    mBluetoothConnected = -1;
                    break;
                }


            }
        }

    /* Call this from the main activity to send data to the remote device
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }*/

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                mMessageString = "exception " + e.getMessage();
            }
        }
    }
    /// koniec BlueTooth
}

