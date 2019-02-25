package com.martins.board;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class PersonalUtils {
    private static final String TAG = "PersonalUtils";
    private static final File STORAGE_DIR = new File(Environment.getExternalStorageDirectory()
                                                        + "/Android/data/"
                                                        + "com.martins.board"
                                                        + "/Files");

    private static File getOutputMediaFile(String fileName){
        if (!STORAGE_DIR.exists())
            if (!STORAGE_DIR.mkdirs())
                return null;

        File mediaFile = new File(STORAGE_DIR.getPath() + "/" + fileName + ".png");
        return mediaFile;
    }

    public static void drawMarker(Dictionary dict){
        for (int i = 0; i < 200; i++) {
            Mat marker = new Mat();
            Aruco.drawMarker(dict, i, 200, marker, 1);
            Bitmap bmp = Bitmap.createBitmap((int) marker.size().width, (int) marker.size().height, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(marker, bmp);
            try {
                PersonalUtils.storeImage(bmp, i + "_img");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void storeImage(Bitmap source, String fileName) throws IOException {
        File pictureFile = getOutputMediaFile(fileName);

        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return;
        }

        FileOutputStream fos = new FileOutputStream(pictureFile);
        source.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
    }

    public static Mat createMatFromBitmap(Bitmap source) {
        Mat frame = new Mat();
        Bitmap bmp = source.copy(source.getConfig(), true);
        Utils.bitmapToMat(bmp, frame);

        return frame;
    }

    public static boolean fileExists(String fileName) {
        File testFile = new File(STORAGE_DIR.getPath() + "/" + fileName);
        return (testFile.exists() && !testFile.isDirectory());
    }

    public static String loadRawString(int rawId, Context context) throws Exception{
        InputStream is = context.getResources().openRawResource(rawId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while((len = is.read(buf))!= -1){
            baos.write(buf, 0, len);
        }
        return baos.toString();
    }

    public static void writeJSON(String fileName, JSONObject source) throws IOException {
        if (!STORAGE_DIR.exists())
            STORAGE_DIR.mkdirs();

        Log.d(TAG, "Writing JSON to: " + STORAGE_DIR.getPath() + "/" + fileName + ".json");

        FileWriter fw = new FileWriter(STORAGE_DIR.getPath() + "/" + fileName + ".json");
        fw.write(source.toString());
        fw.close();
    }

    public static void writeJSON(String fileName, String source) throws IOException {
        if (!STORAGE_DIR.exists())
            STORAGE_DIR.mkdirs();

        Log.d(TAG, "Writing JSON to: " + STORAGE_DIR.getPath() + "/" + fileName + ".json");

        FileWriter fw = new FileWriter(STORAGE_DIR.getPath() + "/" + fileName + ".json");
        fw.write(source);
        fw.close();
    }

    public static String readJSON(String fileName) throws IOException {
        Log.d(TAG, "Reading JSON : " + STORAGE_DIR.getPath() + "/" + fileName + ".json");
        BufferedReader br = new BufferedReader(new FileReader(STORAGE_DIR.getPath() + "/"
                                                                        + fileName + ".json"));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }

    public static void displaySquareMatrix(float[] matrix){
        int len = matrix.length;
        int size = (int) Math.sqrt(len);
        String s = "";

        int counter = 0;
        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                s += matrix[counter] + ", ";
                counter++;
            }
            s += "\n";
        }
        Log.d(TAG, "START");
        Log.d(TAG, s);
        Log.d(TAG, "END");
    }

    public static void displaySquareMatrix(float[] matrix, String id){
        int len = matrix.length;
        int size = (int) Math.sqrt(len);
        String s = "";

        int counter = 0;
        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                s += matrix[counter] + ", ";
                counter++;
            }
            s += "\n";
        }
        Log.d(TAG, "START " + id);
        Log.d(TAG, s);
        Log.d(TAG, "END");
    }

    public static void displaySquareMatrix(int[] matrix){
        int len = matrix.length;
        int size = (int) Math.sqrt(len);
        String s = "";

        int counter = 0;
        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                s += matrix[counter] + ", ";
                counter++;
            }
            s += "\n";
        }
        Log.d(TAG, "START");
        Log.d(TAG, s);
        Log.d(TAG, "END");
    }

    public static void displaySquareMatrix(int[] matrix, String id){
        int len = matrix.length;
        int size = (int) Math.sqrt(len);
        String s = "";

        int counter = 0;
        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                s += matrix[counter] + ", ";
                counter++;
            }
            s += "\n";
        }
        Log.d(TAG, "START " + id);
        Log.d(TAG, s);
        Log.d(TAG, "END");
    }

    public static void displaySquareMatrix(Mat matrix){
        int size = (int)matrix.size().height;
        String s = "";

        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                s += matrix.get(i, j)[0] + ", ";
            }
            s += "\n";
        }
        Log.d(TAG, "START");
        Log.d(TAG, s);
        Log.d(TAG, "END");
    }

    public static void displaySquareMatrix(Mat matrix, String id){
        int size = (int)matrix.size().height;
        String s = "";

        for (int i = 0; i < size; i++){
            for (int j = 0; j < size; j++){
                s += matrix.get(i, j)[0] + ", ";
            }
            s += "\n";
        }
        Log.d(TAG, "START " + id);
        Log.d(TAG, s);
        Log.d(TAG, "END");
    }
}
