package com.dairyroadsolutions.accelplot;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class CommunicationThread implements Runnable {

    private Socket clientSocket;
    public static boolean bStreamData = false;
    public static int bDirectionFlag = 0; // 0 external, 1 internal
    private ServerThread mainSocketServerThreadForChannels = null;

    public ChartRenderer classChartRendererFromParent;

    private BufferedReader input;
    private PrintWriter output;
    public boolean running = false;
    public int connectionType = 0; // 0 - sensor Bananapi, 1 - DataCollector

    // Sampling frequency, in hertz. This is set in the Arduino code, see "Firmware.ino" and
    // "ISR Frequency Ranges.xlsx" for details
    private static final float F_OFFSET_COUNT = 4095.0f;
    private static double dSampleFreq = 250.0;

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


    public CommunicationThread(Socket clientSocket,ChartRenderer chRenderer,ServerThread parent) {

        this.clientSocket = clientSocket;
        this.classChartRendererFromParent = chRenderer;
        this.mainSocketServerThreadForChannels = parent;

        try {

            this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.output = new PrintWriter(new PrintWriter(this.clientSocket.getOutputStream()),true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        float fCh1Out;
        float fCh2Out;

        Log.d("Active","Run Communication thread (running = " + running + ")");
        try {
            while (!Thread.currentThread().isInterrupted() && running) {


                Log.d("Active","waiting for message ...");

                String read = input.readLine();
                if(read != null) {

                    Log.d("Main:", read);
                    try	{

                        // Read from the InputStream
                        // ToDo: Read from the stream


                        String[] readParts = read.split(";");

                        boolean isDataMessage = false;
                        boolean isCommandMessage = false;
                        if(readParts[0].contains("DATA")) {
                            Log.d("Main: ", "SocketMessage - Data Frame arrived");
                            isDataMessage = true;
                        } else if(readParts[0].contains("CMD")) {
                            Log.d("Main: ", "SocketMessage - Command Frame arrived");
                            isCommandMessage = true;
                        }

                        //Log.d("Active", "Message received: size - " + readParts.length + " type - " + readParts[0]);
                        if(isCommandMessage) {

                            if(readParts.length > 1 && readParts[1].contains("DCD")){
                                this.connectionType = 1;
                                Log.d("Main: ", "This is a Data Collector - " + clientSocket.getRemoteSocketAddress());
                                sendMessageOnSocket("Welcome on board ( " + clientSocket.getRemoteSocketAddress() + ")\r\n");
                            }
                        }else if(this.connectionType == 0 && isDataMessage) {
                            if (readParts.length == 27) {
                                //Log.d("Main: ", "Message has been accepted");
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

                                Log.d("Active",
                                        "Parsed message from Accelerometer 1: --- Ax-" + iX_Accel + " Ay-" + iY_Accel + " Az-" + iZ_Accel + " Gx-" + iX_Gyro + " Gy-" + iY_Gyro + " Gz-"
                                                + iZ_Gyro + " Pax-" + preprocX_Accel + " Pay-" + preprocY_Accel
                                                + " Paz-" + preprocZ_Accel + " Pgx-" + preprocX_Gyro + " Pgx-" + preprocY_Gyro + " Pgx-"
                                                + preprocZ_Gyro + "\r\n" + "From Accelerometer 2: --- Ax-" + iX_Accel2 + " Ay-" + iY_Accel2 + " Az-" + iZ_Accel2 + " Gx-" + iX_Gyro2 + " Gy-" + iY_Gyro2 + " Gz-"
                                                + iZ_Gyro2 + " Pax-" + preprocX_Accel2 + " Pay-" + preprocY_Accel2
                                                + " Paz-" + preprocZ_Accel2 + " Pgx-" + preprocX_Gyro2 + " Pgx-" + preprocY_Gyro2 + " Pgx-"
                                                + preprocZ_Gyro2 + "\r\n");

                               // Parse through the bytes and validate

                                    int iErrorCount = 0;

                                    // ToDo: check the read value
                                    fX_Accel[idxBuff] = (float) ((float) (iX_Accel) / 8.0 + 4000);

                                    fY_Accel[idxBuff] = (float) ((float) (iY_Accel) / 8.0 + 4000);

                                    fZ_Accel[idxBuff] = (float) ((float) (iZ_Accel) / 8.0 + 4000);

                                    fX_Gyro[idxBuff] = (float) ((float) (iX_Gyro) / 8.0 + 4000);

                                    fY_Gyro[idxBuff] = (float) ((float) (iY_Gyro) / 8.0 + 4000);

                                    fZ_Gyro[idxBuff] = (float) ((float) (iZ_Gyro) / 8.0 + 4000);

                                    fX_Accel2[idxBuff] = (float) ((float) (iX_Accel2) / 8.0 + 4000);

                                    fY_Accel2[idxBuff] = (float) ((float) (iY_Accel2) / 8.0 + 4000);

                                    fZ_Accel2[idxBuff] = (float) ((float) (iZ_Accel2) / 8.0 + 4000);

                                    fX_Gyro2[idxBuff] = (float) ((float) (iX_Gyro2) / 8.0 + 4000);

                                    fY_Gyro2[idxBuff] = (float) ((float) (iY_Gyro2) / 8.0 + 4000);

                                    fZ_Gyro2[idxBuff] = (float) ((float) (iZ_Gyro2) / 8.0 + 4000);

                                    // Send data to the Collector if it is there
                                    if (bStreamData) {
                                        try {
                                            if(mainSocketServerThreadForChannels.findDataCollector() != null) {
                                                mainSocketServerThreadForChannels.findDataCollector().sendMessageOnSocket("[DATA;External;" + fX_Accel[idxBuff] + ";" + fY_Accel[idxBuff] + ";" + fZ_Accel[idxBuff] + ";" + fX_Accel2[idxBuff] + ";" + fY_Accel2[idxBuff] + ";" + fZ_Accel2[idxBuff] + "]");
                                            } else {
                                                Log.d("Main: ", "We cannot find the Data collector!");
                                            }
                                        } catch (Exception ex) {
                                            Log.e("Main: ", "Error while writing to the socket. ");
                                            ex.printStackTrace();
                                        }
                                    }

                                    if (iErrorCount == 0 && bDirectionFlag == 0) {
                                        // Throw the data to the renderer subtracting off the offset so we get
                                        // positive and negative traces
                                        classChartRendererFromParent.classChart
                                                .addSample(fX_Accel[idxBuff] - F_OFFSET_COUNT, 0);
                                        classChartRendererFromParent.classChart
                                                .addSample(fY_Accel[idxBuff] - F_OFFSET_COUNT, 1);
                                        classChartRendererFromParent.classChart
                                                .addSample(fZ_Accel[idxBuff] - F_OFFSET_COUNT, 2);
                                        classChartRendererFromParent.classChart
                                                .addSample(fX_Gyro[idxBuff] - F_OFFSET_COUNT, 3);
                                        classChartRendererFromParent.classChart
                                                .addSample(fY_Gyro[idxBuff] - F_OFFSET_COUNT, 4);
                                        classChartRendererFromParent.classChart
                                                .addSample(fZ_Gyro[idxBuff] - F_OFFSET_COUNT, 5);

                                        classChartRendererFromParent.classChart
                                                .addSample(fX_Accel2[idxBuff] - F_OFFSET_COUNT, 6);
                                        classChartRendererFromParent.classChart
                                                .addSample(fY_Accel2[idxBuff] - F_OFFSET_COUNT, 7);
                                        classChartRendererFromParent.classChart
                                                .addSample(fZ_Accel2[idxBuff] - F_OFFSET_COUNT, 8);
                                        classChartRendererFromParent.classChart
                                                .addSample(fX_Gyro2[idxBuff] - F_OFFSET_COUNT, 9);
                                        classChartRendererFromParent.classChart
                                                .addSample(fY_Gyro2[idxBuff] - F_OFFSET_COUNT, 10);
                                        classChartRendererFromParent.classChart
                                                .addSample(fZ_Gyro2[idxBuff] - F_OFFSET_COUNT, 11);


                                        // Save the data off to the sd card / local directory
                                        if (bWriteLocal) {
                                            if (idxBuff == (iFileSampleCount - 1)
                                                    && bWritePending) {
                                                Log.d("Active",
                                                        "Write Out data");

												fhelper.logAccData("External1",fX_Accel[idxBuff],fY_Accel[idxBuff],fZ_Accel[idxBuff]);
												fhelper.logAccData("External2",fX_Accel2[idxBuff],fY_Accel2[idxBuff],fZ_Accel2[idxBuff]);


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
                        //Log.e("Main: ", "IOException during reading: " + e.getMessage());
                        e.printStackTrace();
                        break;
                    }

                } else {
                    break;
                }

                // ToDo: Check received text

            }
        } catch (Exception e) {
            Log.d("Main: ", " Error. maybe socket has disconnected");
            e.printStackTrace();
            running = false;
        }
    }

    public boolean isDataConnection(){
        return (connectionType == 0);
    }

    public static double dGetSampleFrequency() {return dSampleFreq; }
    public static void vSetSampleFreq(double dFreq) {dSampleFreq = dFreq;}

    public boolean isCollectorConnection(){
        return (connectionType == 1);
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

    public static void vSetStreaming (boolean streamingFlag){
        bStreamData = streamingFlag;
    }

    public static void vSetDirection (int directionFlag){ // 0 extrenal, 1 internal
        bDirectionFlag = directionFlag;
    }

    public void cancel() {

        running = false;
    }

    public void sendMessageOnSocket(final String message) {
        Thread writingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //Log.d("Main: ", "Send Message to the Client on Socket");
                output.println(message);
            }
        });
        writingThread.start();

    }

}
