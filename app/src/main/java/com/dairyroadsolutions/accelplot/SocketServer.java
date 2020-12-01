package com.dairyroadsolutions.accelplot;


import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Created by andrastoth on 2020. 02. 26..
 */

public class SocketServer {
	private static final int  TRACE_COUNT = 12;


	public Thread LocalSocketThread = null;
	public ServerThread serverRunnableForThread = null;

	protected static final int FILE_WRITE_DONE = 4;
	protected static final int SUCCESS_DISCONNECT = 3;
	protected static final int SUCCESS_CONNECT = 2;
	public boolean serverRunning = false;
	public SamplesBuffer samplesBuffer[] = new SamplesBuffer[TRACE_COUNT];
	public ChartRenderer classChartRendererFromParent;
	private double actualSampleFreq = 0;

	//-------------------------------------------------------------------------
	public static boolean bAudioOut = false;
	public static AudioHelper classAudioHelper = new AudioHelper();

	private static boolean bADC1ToCh1Out = false;
	private static boolean bADC2ToCh1Out = false;
	private static boolean bADC3ToCh1Out = false;
	private static boolean bADC4ToCh1Out = false;
	private static boolean bADC1ToCh2Out = false;
	private static boolean bADC2ToCh2Out = false;
	private static boolean bADC3ToCh2Out = false;
	private static boolean bADC4ToCh2Out = false;


	public SocketServer() {
		this.serverRunnableForThread = new ServerThread();
	}


	// Getter and setter goodness below
	public static boolean isbADC1ToCh1Out() {
		return bADC1ToCh1Out;
	}

	public static void setbADC1ToCh1Out(boolean bADC1ToCh1Out) {
		bADC1ToCh1Out = bADC1ToCh1Out;
	}

	public static boolean isbADC2ToCh1Out() {
		return bADC2ToCh1Out;
	}

	public static void setbADC2ToCh1Out(boolean bADC2ToCh1Out) {
		bADC2ToCh1Out = bADC2ToCh1Out;
	}

	public static boolean isbADC3ToCh1Out() {
		return bADC3ToCh1Out;
	}

	public static void setbADC3ToCh1Out(boolean bADC3ToCh1Out) {
		bADC3ToCh1Out = bADC3ToCh1Out;
	}

	public static boolean isbADC4ToCh1Out() {
		return bADC4ToCh1Out;
	}

	public static void setbADC4ToCh1Out(boolean bADC4ToCh1Out) {
		bADC4ToCh1Out = bADC4ToCh1Out;
	}

	public static boolean isbADC1ToCh2Out() {
		return bADC1ToCh2Out;
	}

	public static void setbADC1ToCh2Out(boolean bADC1ToCh2Out) {
		bADC1ToCh2Out = bADC1ToCh2Out;
	}

	public static boolean isbADC2ToCh2Out() {
		return bADC2ToCh2Out;
	}

	public static void setbADC2ToCh2Out(boolean bADC2ToCh2Out) {
		bADC2ToCh2Out = bADC2ToCh2Out;
	}

	public static boolean isbADC3ToCh2Out() {
		return bADC3ToCh2Out;
	}

	public static void setbADC3ToCh2Out(boolean bADC3ToCh2Out) {
		bADC3ToCh2Out = bADC3ToCh2Out;
	}

	public static boolean isbADC4ToCh2Out() {
		return bADC4ToCh2Out;
	}

	public static void setbADC4ToCh2Out(boolean bADC4ToCh2Out) {
		bADC4ToCh2Out = bADC4ToCh2Out;
	}






	/**
	 * Init socket connection
	 */
	public void startServer(){
		Log.d("Active","Start server for getting socket messages");
		this.LocalSocketThread = new Thread(this.serverRunnableForThread);
		this.LocalSocketThread.start();
		this.serverRunning = true;
	}



	/**
	 * Init socket connection
	 */
	public void initServer(){
		// Todo: Put init steps here
	}

	public ServerThread getServerRunnableForChannels(){
		return this.serverRunnableForThread;
	}

	/**
	 * Terminate the Socket connection
	 */
	public void disconnect()
	{
		if (LocalSocketThread != null)
		{
			LocalSocketThread.interrupt();
			LocalSocketThread = null;
		}
	}

	public void setWritelocalFlagsAtThreads(boolean bNewFlag){
		this.serverRunnableForThread.setChannelsLocalWriteFlag(bNewFlag);
	}

	public void setWritePendingAtThreads(boolean bWritePending){
		this.serverRunnableForThread.setChannelsLocalPendingFlag(bWritePending);
	}

	public void setSampleFreqsAtThreads(double sampleFreq){
		this.actualSampleFreq = sampleFreq;
		this.serverRunnableForThread.setChannelsSampleFreq(sampleFreq);
	}

	public double getSampleFreqsAtThreads(){
		return this.actualSampleFreq;

	}

	public void sendToDataCollector(String message) {
		try {
			if(getServerRunnableForChannels().findDataCollector() != null) {
				getServerRunnableForChannels().findDataCollector().sendMessageOnSocket(message);
			} else {
				Log.d("Main: ", "We cannot find the Data collector!");
			}
		} catch (Exception ex) {
			Log.e("Main: ", "Error while writing to the socket. ");
			ex.printStackTrace();
		}
			
	}

	public void setChartRendererClass(ChartRenderer currentRenderer){
		this.serverRunnableForThread.setChartRenderer(currentRenderer);
	}
}
