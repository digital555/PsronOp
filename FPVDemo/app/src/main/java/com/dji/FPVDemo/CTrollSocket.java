package com.dji.FPVDemo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

/**
 * Created by atomic on 2017-01-26.
 */

public class CTrollSocket {


    private ServerSocket serverSocket;
    Thread serverThread = null;
    private int SERVERPORT = 1234;
    public String dataString;
    public boolean networkIsWorking = true;
    public Context mContext;

    public String dataToSend = "";






    public void Create(int port) {

        networkIsWorking = true;
        SERVERPORT = port;

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

        dataString = "thread created";
    }


    public void onStop() {
        networkIsWorking = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {

                e.printStackTrace();
                dataString = "exception" + e.getMessage();
            }

            int numerPaczki = 0;

            while (!Thread.currentThread().isInterrupted()  &&   networkIsWorking) {

                boolean sockectIsOpened = false;
                try {

                    dataString = "try to accept";
                    socket = serverSocket.accept();

                    sockectIsOpened = true;
                    socket.setTcpNoDelay(true);
                    //socket.setPerformancePreferences();



                    dataString = "get input stream";
                    inputStream = socket.getInputStream();

                    outputStream = socket.getOutputStream();
                    //DataOutputStream out = new DataOutputStream(socket.getOutputStream());


                } catch (IOException e) {
                    e.printStackTrace();
                    dataString = "exception" + e.getMessage();
                }
                catch (Exception e) {
                    dataString = "exception" + e.getMessage();
                }




                // mamy otwarte polaczenie - przesyalamy dane, az cos sie wykrzaczy, wtedy powracamy do nasluchu
                byte[] buffer = new byte[100 * 1024];





                while(networkIsWorking && sockectIsOpened)
                {
                    // czytamy dane
                    //dataString = "try to read data";

                    int bytesRead = 0;
                    int totalBytesRead = 0;
                    try
                    {
                        // dla uĹ‚atwienia wczytujemy stringa
                        int lastChar = 0;

                        while (lastChar != 10) {
                            bytesRead = inputStream.read(buffer, totalBytesRead, 1);
                            // zwraca wartosc -1 jezeli polaczenie zostalo zerwane
                            if(bytesRead <= 0) {
                                lastChar = 10;
                                sockectIsOpened = false;
                                socket.close();
                                dataString = "socket is closed";
                                break;
                            }
                            else {
                                lastChar = (int) buffer[totalBytesRead] & 0xFF;
                                totalBytesRead += bytesRead;
                                //dataString = "bytes read: " + String.valueOf(totalBytesRead);
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        dataString = "exception" + e.getMessage();
                    }
                    catch (Exception e) {
                        dataString = "exception" + e.getMessage();
                    }
                    // koniec czytania danych




                    // wysylamy dane
                    if(networkIsWorking && sockectIsOpened)
                    {
                        // czytamy dane
                        //dataString = "try to write data";
                        try {

                            //outputStream.write();


                            //dataString = String.valueOf((int)buffer[0]) + " " + String.valueOf((int)buffer[1]) + " " + String.valueOf((int)buffer[2]) + " " + String.valueOf((int)buffer[3]);

                            // dane zapisujemu od 5 bajta, tzn. od buffer[4]
                            int pos = 4;

                            // dane z HID

                            // zapisujemy liczbÄ™ urzÄ…dzeĹ„ BT




                            // numer paczki
                            buffer[pos] = (byte) (numerPaczki >> 24);
                            pos++;
                            buffer[pos] = (byte) ( (numerPaczki << 8) >> 24);
                            pos++;
                            buffer[pos] = (byte) ( (numerPaczki << 16) >> 24);
                            pos++;
                            buffer[pos] = (byte) ( (numerPaczki << 24) >> 24);
                            pos++;
                            numerPaczki++;


                            char[] slowo = (dataToSend).toCharArray();
                            for (int i = 0; i < slowo.length ; i++){
                                buffer[i+4] = (byte) slowo[i];
                                pos++;
                            }

                            //buffer[4] = 'a';

                            // na koĹ„cu zapisuje w pierwszych 4 bajtach rozmiar caĹ‚ej paczki
                            int byteCount = pos - 4;
                            buffer[0] = (byte) (byteCount >> 24);
                            buffer[1] = (byte) ( (byteCount << 8) >> 24);
                            buffer[2] = (byte) ( (byteCount << 16) >> 24);
                            buffer[3] = (byte) ( (byteCount << 24) >> 24);





                            // wszystko wysyĹ‚amy jednÄ… komendÄ…
                            outputStream.write(buffer,0,pos);


                            //outputStream.write(buffer, 0, totalBytesRead);
                            outputStream.flush();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            dataString = "exception" + e.getMessage();
                        }
                        catch (Exception e) {
                            dataString = "exception" + e.getMessage();
                        }
                    } // koniec wysylania danych
                } // end while(networkIsWorking && sockectIsOpened)

                // czytamy dane
                dataString = "socket is closed";
            } // end while (!Thread.currentThread().isInterrupted()  &&   networkIsWorking)

        }
    }



}