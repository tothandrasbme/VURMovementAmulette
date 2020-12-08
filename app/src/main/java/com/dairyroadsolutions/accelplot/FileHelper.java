package com.dairyroadsolutions.accelplot;

/**
 * Created by Brian on 1/24/2016.
 *
 * This helper provides methods to write data to the sd card on the device.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.util.Date;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

class FileHelper {

    private ByteBuffer bb;

    FileHelper(int iFloatBuffLength){
        vSetSamples( iFloatBuffLength );
    }

    private File logFile = null;
    private File logDir = null;

    // debug
    private static final String _strTag = MainActivity.class.getSimpleName();

    // File index
    private long lFileIdx = 0;

    public void vSetSamples( int iFloatBuffLength ){
        bb = ByteBuffer.allocate(iFloatBuffLength*4);
    }


    public void clearFileHelper(){
        this.logFile = null;
        this.logDir = null;
    }

    public String getFilesInDirectory(){
        String strDir = "/Amulette_Log";
        logDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + strDir);
        if(!logDir.exists()) {
            Log.d("Main: ", "LOG: Try to get content of the directory, but there is no library");
            return "";
        }
        else {
            File[] filesInTheDirectory = logDir.listFiles();
            Log.d("Main: ","Files list: Size - " + filesInTheDirectory.length);
            if(filesInTheDirectory.length == 0){
                Log.d("Main: ","No files in the directory!!!");
                return "";
            } else {
                String result = "[DATA;RFD";
                for (int i = 0; i < filesInTheDirectory.length; i++) {
                    result += ";" + filesInTheDirectory[i].getName();
                    Log.d("Main: ", "FileName: " + filesInTheDirectory[i].getName());

                }
                return result;
            }
        }
    }

    public String readLogFileContent(String sourceFile){
        String resultFileContent = "[DATA;RFC;";
        String strDir = "/Amulette_Log";
        File readDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + strDir);
        if(readDir.exists()) {
            File readSource = new File(readDir, sourceFile);
            if (readSource != null) {
                try {
                    //String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                    StringBuilder buildResult = new StringBuilder();

                    BufferedReader buf = new BufferedReader(new FileReader(readSource));
                    String line;
                    while((line = buf.readLine()) != null){
                        buildResult.append(line+"#");
                        //buildResult.append("\n");
                    }
                    buf.close();
                    resultFileContent += buildResult.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    resultFileContent += ",OK]";
                }

                return resultFileContent;
            } else {
                Log.d("Main: ", "LOG: Log file is null we are not able to open it");
                return "";
            }
        }
        return "";
    }

    public boolean logAccData(String source, float data1, float data2, float data3){



        if(logFile == null) {


            String strDir = "/Amulette_Log";

            File sdCard;

            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());


            logDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + strDir);

            // If the directory is missing then create it
            if(!logDir.exists()) {
                Log.d("Main: ", "LOG: Create directory");
                if(logDir.mkdir()){
                    Log.d("Main: ", "LOG: Check file");


                }
            }

            String Logfilename = System.currentTimeMillis() + "_log.txt";

            logFile = new File(logDir, Logfilename);

            // If the file is missing then create it
            if(!logFile.exists()) {
                try {
                    Log.d("Main: ", "LOG: Create file");
                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        appenLogToSD(source,data1,data2,data3);

        return true;
    }

    /**
     * This function writes one single data into the logfile
     * directory and file name values;
     */
    public boolean appenLogToSD(String source, float data1, float data2, float data3){

        if(logFile != null) {
            try {
                //String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                String currentDateTimeString = System.currentTimeMillis() + "";

                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(currentDateTimeString + "-" + "[DATA1;"+ source + ";" + data1 + ";" + data2 + ";" + data3 + "]");
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        } else {
            Log.d("Main: ", "LOG: Log file is null we are not able to open it");
            return false;
        }
    }


    /**
     * This function writes a single array to the external storage directory using default
     * directory and file name values;
     * @param strDir        String with directory name. Must include leading "/"
     * @param strFileName   String with the filename
     * @param data          float array with the data
     * @return              true if successful
     */
    public boolean bFileToSD(String strDir, String strFileName, float[] data){

        File sdCard;

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        }else{
            sdCard = Environment.getExternalStorageDirectory();
        }

        File dir = new File (sdCard.getAbsolutePath() + strDir);
        dir.mkdirs();
        File file = new File(dir, strFileName);

        try{
            FileChannel fchannel = new FileOutputStream(file).getChannel();

            bb.clear();

            for( int idx=0; idx<data.length; ++idx){
                bb.putFloat(data[idx]);
            }

            Log.i(_strTag, ":HM:                              Directory: " + dir.getPath());
            Log.i(_strTag, ":HM:                              File Name: " + strFileName);
            Log.i(_strTag, ":HM:                                data[0]: " + data[0]);


            // The position value is at the end of the buffer, so we need this command to move the
            // position marker to zero and reset the mark.
            bb.flip();

            // Write the buffer to the file
            fchannel.write(bb);

            // Close the file
            fchannel.close();

            //Log.d(_strTag, ":HM:                    File write complete: ");

        } catch (Exception e) {
            Log.d(_strTag, ":HM:                   FileHelper Exception: " + e.getMessage());
        }

        // Increment the file index
        ++lFileIdx;

        // Everything must have gone ok
        return true;

    }

    /**
     * This function writes a single array to the external storage directory using default
     * directory and file name values;
     * @param data      float array with the data
     * @return          true if successful
     */
    public boolean bFileToSD(float[] data){

        // The filename and directory strings
        String strFileName = "Trace01_" + String.format("%07d", lFileIdx) + ".dat";
        String strDir = "/AccelPlot";

        // Call the generic function
        bFileToSD(strDir, strFileName, data);

        // Increment the file index
        ++lFileIdx;

        // Everything must have gone ok
        return true;

    }

    /**
     * This function writes four arrays to the external storage directory using default
     * directory and file name values;
     * @param data01    float array with the data
     * @param data02    float array with the data
     * @param data03    float array with the data
     * @param data04    float array with the data
     * @return          true if successful
     */
    boolean bFileToSD(float[] data01, float[] data02, float[] data03, float[] data04){

        String strFileName;
        String strDir;

        // The filename and directory strings
        strFileName = "Trace01_" + String.format("%07d", lFileIdx) + ".dat";
        strDir = "/AccelPlot";
        bFileToSD(strDir, strFileName, data01);

        // The filename and directory strings
        strFileName = "Trace02_" + String.format("%07d", lFileIdx) + ".dat";
        strDir = "/AccelPlot";
        bFileToSD(strDir, strFileName, data02);

        // The filename and directory strings
        strFileName = "Trace03_" + String.format("%07d", lFileIdx) + ".dat";
        strDir = "/AccelPlot";
        bFileToSD(strDir, strFileName, data03);

        // The filename and directory strings
        strFileName = "Trace04_" + String.format("%07d", lFileIdx) + ".dat";
        strDir = "/AccelPlot";
        bFileToSD(strDir, strFileName, data04);

        // Everything must have gone ok
        return true;

    }


}
