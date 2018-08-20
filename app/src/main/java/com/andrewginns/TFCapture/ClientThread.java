package com.andrewginns.TFCapture;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

class ClientThread implements Runnable {
    private static final int SERVER_PORT = 7000;
    private static final String SERVER_IP = "192.168.0.11";
    private static final String TAG = "ClientThread";
//    private Socket socket;

    public ClientThread() {
        //            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
        Log.d(TAG, "Created new thread");
//            socket = new Socket(serverAddress, SERVER_PORT);
    }

    @Override
    public void run() {
    }

    static void sendData(byte[] data, int msgLength) {

        try {
            Socket socket;
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
            Log.d(TAG, "created");
            socket = new Socket(serverAddress, SERVER_PORT);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            DataOutputStream dos = new DataOutputStream(out);
            Log.d("ADebugTag", "\nArray lenght sent: " + msgLength);

            dos.writeInt(msgLength);
            dos.write(data);
//                            if (msgLength > 0) {
//                                dos.write(dataOut, 0, msgLength);
//                            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Log.d("ADebugTag", "\nReader:" + reader.readLine());
            Log.d("ADebugTag", "\nData Sent");
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e);
        }
    }
}
