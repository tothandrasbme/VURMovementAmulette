package com.dairyroadsolutions.accelplot;


import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by andrastoth on 2020. 02. 26..
 */

public class SocketServer {
	private static final int  TRACE_COUNT = 12;

	private ServerSocket serverSocket;
	public Thread serverThread = null;
	public static final int SERVERPORT = 6000;
	protected static final int FILE_WRITE_DONE = 4;
	protected static final int SUCCESS_DISCONNECT = 3;
	protected static final int SUCCESS_CONNECT = 2;
	public boolean serverRunning = false;
	public SamplesBuffer samplesBuffer[] = new SamplesBuffer[TRACE_COUNT];
	public ChartRenderer classChartRenderer;
	public static boolean bStreamData = false;
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

	// Sampling frequency, in hertz. This is set in the Arduino code, see "Firmware.ino" and
	// "ISR Frequency Ranges.xlsx" for details
	private static final float F_OFFSET_COUNT = 4095.0f;
	private static double dSampleFreq = 250.0;

	public static double dGetSampleFrequency() {return dSampleFreq; }
	public static void vSetSampleFreq(double dFreq) {dSampleFreq = dFreq;}

	//-------------------------------------------------------------------------
	// Arduino accelerometer data
	//-------------------------------------------------------------------------
	//
	// This is the number of samples written to each file.
	public static int iFileSampleCount = (int) dSampleFreq * 60;
	public static float[] fX_Accel = new float[iFileSampleCount];
	public static float[] fY_Accel = new float[iFileSampleCount];
	public static float[] fZ_Accel = new float[iFileSampleCount];

	//This channel can be a gyro or the ADC, depending how how the firmware is configured
	//in the Arduino
	public static float[] fX_Gyro = new float[iFileSampleCount];
	public static float[] fY_Gyro = new float[iFileSampleCount];
	public static float[] fZ_Gyro = new float[iFileSampleCount];

	public static float[] fX_Accel2 = new float[iFileSampleCount];
	public static float[] fY_Accel2 = new float[iFileSampleCount];
	public static float[] fZ_Accel2 = new float[iFileSampleCount];

	//This channel can be a gyro or the ADC, depending how how the firmware is configured
	//in the Arduino
	public static float[] fX_Gyro2 = new float[iFileSampleCount];
	public static float[] fY_Gyro2 = new float[iFileSampleCount];
	public static float[] fZ_Gyro2 = new float[iFileSampleCount];

	public static int idxBuff = 0;

	public static final FileHelper fhelper = new FileHelper(iFileSampleCount);
	private static boolean bWriteLocal = true;
	private static boolean bWritePending = true;


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


	public static void vSetWritePending( boolean bIn) {bWritePending = bIn;}

	public static void vSetFileSamples(int iSamples ){
		iFileSampleCount = iSamples;
		fX_Accel = new float[iFileSampleCount];
		fY_Accel = new float[iFileSampleCount];
		fZ_Accel = new float[iFileSampleCount];
		fX_Gyro = new float[iFileSampleCount];
		fY_Gyro = new float[iFileSampleCount];
		fZ_Gyro = new float[iFileSampleCount];
		idxBuff=0;
		fhelper.vSetSamples(iSamples);
	}






	public static void vSetWriteLocal (boolean bNewFlag){
		bWriteLocal = bNewFlag;
	}

	class ServerThread implements Runnable {

		public boolean running = false;
		private CommunicationThread commThread = null;

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

					commThread = new CommunicationThread(socket);
					commThread.running = true;
					new Thread(commThread).start();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void cancel() {
			commThread.cancel();
			commThread = null;
			running = false;
		}
	}

	class CommunicationThread implements Runnable {

		private Socket clientSocket;

		private BufferedReader input;
		public boolean running = false;

		public CommunicationThread(Socket clientSocket) {

			this.clientSocket = clientSocket;

			try {

				this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {

			float fCh1Out;
			float fCh2Out;

			Log.d("Active","Run Communication thread (running = " + running + ")");

			while (!Thread.currentThread().isInterrupted() && running) {

				try {
					//Log.d("Active","waiting for message ...");

					String read = input.readLine();
					if(read != null) {


						try	{

							// Read from the InputStream
							// ToDo: Read from the stream


							String[] readParts = read.split(";");

							//Log.d("Active", "Message received: size - " + readParts.length + " type - " + readParts[0]);

							if(readParts.length == 27) {

								int unixTimeStamp = 0;
								int messageNumber = 0;
								int iX_Accel = 0;
								int iY_Accel = 0;
								int iZ_Accel = 0;
								int iX_Gyro = 0;
								int iY_Gyro = 0;
								int iZ_Gyro = 0;

								double preprocX_Accel = 0.0;
								double preprocY_Accel = 0.0;
								double preprocZ_Accel = 0.0;
								double preprocX_Gyro = 0.0;
								double preprocY_Gyro = 0.0;
								double preprocZ_Gyro = 0.0;

								int iX_Accel2 = 0;
								int iY_Accel2 = 0;
								int iZ_Accel2 = 0;
								int iX_Gyro2 = 0;
								int iY_Gyro2 = 0;
								int iZ_Gyro2 = 0;

								double preprocX_Accel2 = 0.0;
								double preprocY_Accel2 = 0.0;
								double preprocZ_Accel2 = 0.0;
								double preprocX_Gyro2 = 0.0;
								double preprocY_Gyro2 = 0.0;
								double preprocZ_Gyro2 = 0.0;

								unixTimeStamp = Integer.parseInt(readParts[1]);
								messageNumber = Integer.parseInt(readParts[2]);

								iX_Accel = Integer.parseInt(readParts[3]);
								iY_Accel = Integer.parseInt(readParts[4]);
								iZ_Accel = Integer.parseInt(readParts[5]);
								iX_Gyro = Integer.parseInt(readParts[6]);
								iY_Gyro = Integer.parseInt(readParts[7]);
								iZ_Gyro = Integer.parseInt(readParts[8]);

								preprocX_Accel = Double.parseDouble(readParts[9]);
								preprocY_Accel = Double.parseDouble(readParts[10]);
								preprocZ_Accel = Double.parseDouble(readParts[11]);
								preprocX_Gyro = Double.parseDouble(readParts[12]);
								preprocY_Gyro = Double.parseDouble(readParts[13]);
								preprocZ_Gyro = Double.parseDouble(readParts[14]);

								iX_Accel2 = Integer.parseInt(readParts[15]);
								iY_Accel2 = Integer.parseInt(readParts[16]);
								iZ_Accel2 = Integer.parseInt(readParts[17]);
								iX_Gyro2 = Integer.parseInt(readParts[18]);
								iY_Gyro2 = Integer.parseInt(readParts[19]);
								iZ_Gyro2 = Integer.parseInt(readParts[20]);

								preprocX_Accel2 = Double.parseDouble(readParts[21]);
								preprocY_Accel2 = Double.parseDouble(readParts[22]);
								preprocZ_Accel2 = Double.parseDouble(readParts[23]);
								preprocX_Gyro2 = Double.parseDouble(readParts[24]);
								preprocY_Gyro2 = Double.parseDouble(readParts[25]);
								preprocZ_Gyro2 = Double.parseDouble(readParts[26]);

								/*Log.d("Active",
										"Parsed message from Accelerometer 1: --- Ax-" + iX_Accel + " Ay-" + iY_Accel + " Az-" + iZ_Accel + " Gx-" + iX_Gyro + " Gy-" + iY_Gyro + " Gz-"
											+ iZ_Gyro + " Pax-" + preprocX_Accel + " Pay-" + preprocY_Accel
											+ " Paz-" + preprocZ_Accel + " Pgx-" + preprocX_Gyro + " Pgx-" + preprocY_Gyro + " Pgx-"
											+ preprocZ_Gyro + "\r\n" + "From Accelerometer 2: --- Ax-" + iX_Accel2 + " Ay-" + iY_Accel2 + " Az-" + iZ_Accel2 + " Gx-" + iX_Gyro2 + " Gy-" + iY_Gyro2 + " Gz-"
									+ iZ_Gyro2 + " Pax-" + preprocX_Accel2 + " Pay-" + preprocY_Accel2
									+ " Paz-" + preprocZ_Accel2 + " Pgx-" + preprocX_Gyro2 + " Pgx-" + preprocY_Gyro2 + " Pgx-"
									+ preprocZ_Gyro2 + "\r\n");*/

								bStreamData = true;

								// Parse through the bytes and validate
								if (bStreamData) {
									int iErrorCount = 0;

									// ToDo: check the read value
									fX_Accel[idxBuff] = (float)((float) (iX_Accel) / 8.0 + 4000);

									fY_Accel[idxBuff] = (float)((float) (iY_Accel) / 8.0 + 4000);

									fZ_Accel[idxBuff] = (float)((float) (iZ_Accel) / 8.0 + 4000);

									fX_Gyro[idxBuff] = (float)((float) (iX_Gyro) / 8.0 + 4000);

									fY_Gyro[idxBuff] = (float)((float) (iY_Gyro) / 8.0 + 4000);

									fZ_Gyro[idxBuff] = (float)((float) (iZ_Gyro) / 8.0 + 4000);

									fX_Accel2[idxBuff] = (float)((float) (iX_Accel2) / 8.0 + 4000);

									fY_Accel2[idxBuff] = (float)((float) (iY_Accel2) / 8.0 + 4000);

									fZ_Accel2[idxBuff] = (float)((float) (iZ_Accel2) / 8.0 + 4000);

									fX_Gyro2[idxBuff] = (float)((float) (iX_Gyro2) / 8.0 + 4000);

									fY_Gyro2[idxBuff] = (float)((float) (iY_Gyro2) / 8.0 + 4000);

									fZ_Gyro2[idxBuff] = (float)((float) (iZ_Gyro2) / 8.0 + 4000);

									if (iErrorCount == 0) {
										// Throw the data to the renderer subtracting off the offset so we get
										// positive and negative traces
										classChartRenderer.classChart
											.addSample(fX_Accel[idxBuff] - F_OFFSET_COUNT, 0);
										classChartRenderer.classChart
											.addSample(fY_Accel[idxBuff] - F_OFFSET_COUNT, 1);
										classChartRenderer.classChart
											.addSample(fZ_Accel[idxBuff] - F_OFFSET_COUNT, 2);
										classChartRenderer.classChart
											.addSample(fX_Gyro[idxBuff] - F_OFFSET_COUNT, 3);
										classChartRenderer.classChart
											.addSample(fY_Gyro[idxBuff] - F_OFFSET_COUNT, 4);
										classChartRenderer.classChart
											.addSample(fZ_Gyro[idxBuff] - F_OFFSET_COUNT, 5);

										classChartRenderer.classChart
											.addSample(fX_Accel2[idxBuff] - F_OFFSET_COUNT, 6);
										classChartRenderer.classChart
											.addSample(fY_Accel2[idxBuff] - F_OFFSET_COUNT, 7);
										classChartRenderer.classChart
											.addSample(fZ_Accel2[idxBuff] - F_OFFSET_COUNT, 8);
										classChartRenderer.classChart
											.addSample(fX_Gyro2[idxBuff] - F_OFFSET_COUNT, 9);
										classChartRenderer.classChart
											.addSample(fY_Gyro2[idxBuff] - F_OFFSET_COUNT, 10);
										classChartRenderer.classChart
											.addSample(fZ_Gyro2[idxBuff] - F_OFFSET_COUNT, 11);

										// Throw the data to the audio output
										/*fCh1Out =
											((bADC1ToCh1Out ? 1.0f : 0.0f) * fX_Accel[idxBuff]) + (
												(bADC2ToCh1Out ? 1.0f : 0.0f) * fY_Accel[idxBuff])
												+ ((bADC3ToCh1Out ? 1.0f : 0.0f) * fZ_Accel[idxBuff])
												+ ((bADC4ToCh1Out ? 1.0f : 0.0f) * fX_Gyro[idxBuff]);
										fCh2Out =
											((bADC1ToCh2Out ? 1.0f : 0.0f) * fX_Accel[idxBuff]) + (
												(bADC2ToCh2Out ? 1.0f : 0.0f) * fY_Accel[idxBuff])
												+ ((bADC3ToCh2Out ? 1.0f : 0.0f) * fZ_Accel[idxBuff])
												+ ((bADC4ToCh2Out ? 1.0f : 0.0f) * fX_Gyro[idxBuff]);
										classAudioHelper.setFreqOfTone(1000.0f + fCh1Out / 2.0f,
											1000.0f + fCh2Out / 2.0f);/*/

										// Save the data off to the sd card / local directory
										if (bWriteLocal) {
											if (idxBuff == (iFileSampleCount - 1)
												&& bWritePending) {
												Log.d("Active",
													"Write Out data");

												/*fhelper.bFileToSD(fX_Accel, fY_Accel, fZ_Accel,
													fX_Gyro);
												Log.i("Active",
													":HM:                   Write files, idxBuff: "
														+ idxBuff);
												Log.i("Active",
													":HM:                             fX_Gyro[0]: "
														+ fX_Gyro[0]);
//                                    Log.i(_strTag, ":HM:                          X_Accel Error: " + iErrorCount);
												bWritePending = false;
												mHandler.obtainMessage(FILE_WRITE_DONE, mmSocket).sendToTarget();*/

											}
										}

										// Increment the data buffer index
										idxBuff = ++idxBuff % iFileSampleCount;

									} else {

										// Skip a byte to see if we can get back in sync
									}


								}
							}

						}
						catch (Exception e){ // It was IOException
							break;
						}

					} else {
						break;
					}

					// ToDo: Check received text

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void cancel() {

			running = false;
		}

	}

	/**
	 * Init socket connection
	 */
	public void startServer(){
		Log.d("Active","Start server for getting socket messages");
		this.serverThread = new Thread(new ServerThread());
		this.serverThread.start();
		this.serverRunning = true;
	}

	/**
	 * Init socket connection
	 */
	public void initServer(){
		// Todo: Put init steps here
	}

	/**
	 * Terminate the Socket connection
	 */
	public void disconnect()
	{
		if (serverThread != null)
		{
			serverThread.interrupt();
			serverThread = null;
		}
	}
}
