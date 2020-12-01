package com.dairyroadsolutions.accelplot;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class ServerThread implements Runnable {

    public static final int SERVERPORT = 6000;

    public boolean running = false;
    private CommunicationThread commThread = null;
    private ArrayList<CommunicationThread> commThreadList = new ArrayList<>();
    private ServerSocket serverSocket;
    private ChartRenderer classChartRendererFromParent = null;

    public void run() {
        Log.d("Active","Start Thread");
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(SERVERPORT);
            Log.d("Active","Socket definition is ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = true;
        Log.d("Active","Start commserver");
        while (!Thread.currentThread().isInterrupted() && running) {

            try {

                socket = serverSocket.accept();

                commThread = new CommunicationThread(socket,classChartRendererFromParent,this);
                commThread.running = true;
                new Thread(commThread).start();
                commThreadList.add(commThread); // Save the current connection

                Log.d("Main: ", "Communication socket has been saved ");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public CommunicationThread findDataCollector() {
        for (CommunicationThread commTh : commThreadList) {
            if(commTh.isCollectorConnection()) {
                return commTh;
            }
        }
        return null;
    }

    public void cancel() {
        commThread.cancel();
        commThread = null;
        running = false;
    }

    public void setChannelsLocalWriteFlag(boolean bNewFlag) {
        for (CommunicationThread currentChannel : commThreadList) {
            currentChannel.vSetWriteLocal(bNewFlag);
        }
    }

    public void setChannelsLocalPendingFlag(boolean bWritePending) {
        for (CommunicationThread currentChannel : commThreadList) {
            currentChannel.vSetWritePending(bWritePending);
        }
    }

    public void setChannelsSampleFreq(double sampleFreq) {
        for (CommunicationThread currentChannel : commThreadList) {
            currentChannel.vSetSampleFreq(sampleFreq);
        }
    }

    public void setChannelsStreamingFlag(boolean streamingFlag) {
        for (CommunicationThread currentChannel : commThreadList) {
            currentChannel.vSetStreaming(streamingFlag);
        }
    }

    public void setSensorDirectionAtChannels(int directionFlag) { // 0 external, 1 internal
        for (CommunicationThread currentChannel : commThreadList) {
            currentChannel.vSetDirection(directionFlag);
        }
    }

    public void vSetFileSamples(int iSamples ) {
        for (CommunicationThread currentChannel : commThreadList) {
            currentChannel.vSetFileSamples(iSamples);
        }
    }

    public void setChartRenderer(ChartRenderer chRenderer) {
        this.classChartRendererFromParent = chRenderer;
    }


}
