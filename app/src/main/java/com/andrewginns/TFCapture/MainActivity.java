package com.andrewginns.TFCapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.database.Cursor;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

public class MainActivity extends AppCompatActivity {
    private Socket socket;

    private static final int SERVERPORT = 7000;
    private static final String SERVER_IP = "192.168.0.11";

    private int[] intValues;
    private float[] floatValues;
    private float[] hybridValues;
    private float[][][] placeholder;

    private ImageView ivPhoto;

    private FileInputStream is = null;
    private static final int CODE = 1;
    private static final int RESULT_LOAD_IMAGE = 2;

    File photoFile;

    private TensorFlowInferenceInterface inferenceInterface;

    //  Model parameters
    private static final String MODEL_FILE = "file:///android_asset/optimized-graph.pb";
    private static final String INPUT_NODE = "inputA";
    private static final String OUTPUT_NODE = "a2b_generator/Conv_7/Relu";
    private static int res = 200;

    //  Permissions control
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_W_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_R_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String INTERNET = Manifest.permission.INTERNET;
    private static final String ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;

    private boolean hasPermission() {
        return (checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                (checkSelfPermission(PERMISSION_W_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && (checkSelfPermission(PERMISSION_R_STORAGE) == PackageManager
                .PERMISSION_GRANTED) && (checkSelfPermission(INTERNET) == PackageManager
                .PERMISSION_GRANTED) && (checkSelfPermission(ACCESS_NETWORK_STATE) ==
                PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                shouldShowRequestPermissionRationale(PERMISSION_W_STORAGE) ||
                shouldShowRequestPermissionRationale(PERMISSION_R_STORAGE)) {
            Toast.makeText(MainActivity.this, "Camera AND storage permission are required for " +
                    "this app", Toast.LENGTH_LONG).show();
        }
        requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_W_STORAGE,
                PERMISSION_R_STORAGE}, PERMISSIONS_REQUEST);
    }

    //  Image saving
    public static void saveBitmap(final Bitmap bitmap, final String filename) {
//        String unique = Long.toString(System.currentTimeMillis());
        final String root =
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tensorflow";
        final File myDir = new File(root);
//        final String myDir = root;
        final File file = new File(myDir, filename);

        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (final Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ADebugTag", "\nDefault Resolution: " + Integer.toString(res));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ivPhoto = findViewById(R.id.ivPhoto);

//      Buttons
        Button checkPerm = findViewById(R.id.checkPerm);
        Button onCamera = findViewById(R.id.onCamera);
        Button onGallery = findViewById(R.id.onGallery);
//        Button onFolder = findViewById(R.id.onFolder);
        Button changeRes = findViewById(R.id.changeRes);

        if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                shouldShowRequestPermissionRationale(PERMISSION_W_STORAGE) ||
                shouldShowRequestPermissionRationale(PERMISSION_R_STORAGE)) {
            Toast.makeText(MainActivity.this, "Camera AND storage permission are required for " +
                    "this app", Toast.LENGTH_LONG).show();
        }
        requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_W_STORAGE,
                PERMISSION_R_STORAGE}, PERMISSIONS_REQUEST);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                    socket = new Socket(serverAddr, SERVERPORT);
                    Log.d("ADebugTag", "\nConnected to: " + SERVER_IP + ":" + SERVERPORT);

                    try {
                        String str = "Hello, World!";
                        PrintWriter out = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream())),
                                true);
                        out.println(str);
                        Log.d("ADebugTag", "\nMessage Sent: " + str);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        thread.start();

//      Click events
        // Check Permissions
//        checkPerm.setOnClickListener(new View.OnClickListener() {
        checkPerm.setOnClickListener(v -> {
            if (hasPermission()) {
                Toast.makeText(
                        MainActivity.this,
                        "Permissions already granted",
                        Toast.LENGTH_LONG)
                        .show();
            } else {
                requestPermission();
            }
        });

        // Take new picture
        onCamera.setOnClickListener(v -> {
            initTensorFlowAndLoadModel();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                try {
                    photoFile = createImageFile();  //Create temporary image
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (photoFile != null) {
                    //Enable secure sharing of images to other apps through FileProvider
                    Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                            BuildConfig.APPLICATION_ID + ".fileprovider",
                            photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(intent, 1);
                }
            }
        });

        // Load single image from gallery
        onGallery.setOnClickListener(arg0 -> {
            initTensorFlowAndLoadModel();
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);
        });

        // Set the entered resolution
        changeRes.setOnClickListener(v -> {
            Log.d("ADebugTag", "\nPrevious Resolution: " + Integer.toString(res));
            EditText value = findViewById(R.id.resEdit);
            String resolution = value.getText().toString();
            res = Integer.valueOf(resolution);
            initTensorFlowAndLoadModel();
            Log.d("ADebugTag", "\nNew Resolution: " + Integer.toString(res));
        });


    }

    private File createImageFile() throws IOException {
        // Name the file
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // getExternalFilesDir()stores long term data in SDCard/Android/data/your_app/files/
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        assert storageDir != null;
        Log.d("TAH", storageDir.toString());
        //Create a temporary file with prefix >=3 characters. Default suffix is ".tmp"
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    private void initTensorFlowAndLoadModel() {
        intValues = new int[res * res];
        floatValues = new float[res * res * 3];
        hybridValues = new float[150 * 150 * 256];

        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
    }

    private Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }

        int height = origin.getHeight();
        int width = origin.getWidth();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled()) {
            origin.recycle();
        }
        return newBM;
    }

    private byte[] stylizeImage(Bitmap bitmap) {
        Bitmap scaledBitmap = scaleBitmap(bitmap, res, res); // desiredSize
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0,
                scaledBitmap.getWidth(), scaledBitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / (127.5f) - 1f; //red

            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / (127.5f) - 1f; //green

            floatValues[i * 3 + 2] = (val & 0xFF) / (127.5f) - 1f; //blue
        }

        // Copy the input data into TensorFlow.
        inferenceInterface.feed(INPUT_NODE, floatValues, 1, res, res, 3);
        // Run the inference call.
        inferenceInterface.run(new String[]{OUTPUT_NODE});
        // Copy the output Tensor back into the output array.
        inferenceInterface.fetch(OUTPUT_NODE, hybridValues);

        // Send the returned array to the server
        File hybridStore = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        assert hybridStore != null;

        byte[] dataOut = new byte[0];

        String hybridString = java.util.Arrays.toString(hybridValues);
        Log.d("ADebugTag", "\nArray Length: " + Integer.toString(hybridValues.length));
        Log.d("ADebugTag", "\nArray: " + hybridString);

        try {
            byte[] compressed = gzip(hybridString);
            Files.write(Paths.get(hybridStore + "/" + "compressed.gz"), compressed);
            dataOut = compressed;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataOut;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        long currentTime = System.currentTimeMillis();
        if (requestCode == CODE && resultCode == RESULT_OK) {
            try {
                is = new FileInputStream(photoFile);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                byte[] bitmap2 = stylizeImage(bitmap);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (requestCode == RESULT_LOAD_IMAGE) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            assert selectedImage != null;
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            assert cursor != null;
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            byte[] dataOut = stylizeImage(bitmap);
            int msgLength = dataOut.length;
            Log.d("ADebugTag", "\nArray length: " + msgLength);


            // SENDING DATA VIA TCP //
            Thread send = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Socket socket;
                        InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
                        Log.d("ADebugTag", "created");
                        socket = new Socket(serverAddress, SERVERPORT);

                        OutputStream out = socket.getOutputStream();
                        InputStream in = socket.getInputStream();

                        DataOutputStream dos = new DataOutputStream(out);
//                        dos.writeInt(msgLength);
//                        dos.write(dataOut);
                        if (msgLength > 0) {
                            dos.write(dataOut, 0, msgLength);
                        }
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        Log.d("ADebugTag", "\nReader:" + reader.readLine());
                        Log.d("ADebugTag", "\nData Sent");
                        Log.d("ADebugTag", "\nArray length sent: " + msgLength);

                    } catch (Exception e) {
                        Log.d("ADebugTag", "\nERROR");
                    }
                }
            });

            send.start();

//            ClientThread t = new ClientThread();
//            new Thread(t).start();
//            t.sendData(dataOut, msgLength);

//            ClientThread.sendData(dataOut, msgLength);
//            send.sendData(dataOut, msgLength);
//            ClientThread t = new ClientThread(dataOut, msgLength);
//            new Thread(t).start();
//            Thread t = new Thread(send);
//            t.start();
//            t.sendData(dataOut, msgLength);
//            Thread t = new Thread(new ClientThread(dataOut, msgLength));
//            t.start();

        }
        Toast.makeText(
                MainActivity.this,
                "Partial result sent",
                Toast.LENGTH_LONG)
                .show();
    }

    public static byte[] gzip(String string) throws IOException {
//        https://stackoverflow.com/questions/3752359/gzip-in-android
        ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(string.getBytes());
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }

//        ClientThread(byte[] data, int msgLength) {
//            try {
//                InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
//                Log.d(TAG, "created");
//                socket = new Socket(serverAddress, SERVER_PORT);
//                try {
//                    OutputStream out = socket.getOutputStream();
//                    InputStream in = socket.getInputStream();
//
//                    DataOutputStream dos = new DataOutputStream(out);
//                    Log.d("ADebugTag", "\nArray lenght sent: " + msgLength);
//
//                    dos.writeInt(msgLength);
//                    dos.write(data);
////                            if (msgLength > 0) {
////                                dos.write(dataOut, 0, msgLength);
////                            }
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//                    Log.d("ADebugTag", "\nReader:" + reader.readLine());
//                    Log.d("ADebugTag", "\nData Sent");
//                } catch (Exception e) {
//                    Log.e(TAG, "Error: " + e);
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "Error: " + e);
//            }
//        }

}


//    private class SendData implements Runnable {
//        public SendData() {
//            try {
//                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
//
//                Socket sock = new Socket(serverAddr, SERVERPORT);
//                Log.d("ADebugTag", "\nConnected to: " + SERVER_IP + ":" + SERVERPORT);
//
//                try {
//                    OutputStream out = socket.getOutputStream();
//                    InputStream in = socket.getInputStream();
//
//                    DataOutputStream dos = new DataOutputStream(out);
//                    Log.d("ADebugTag", "\nArray lenght sent: " + msgLength);
//
//                    dos.writeInt(msgLength);
//                    dos.write(dataOut);
////                            if (msgLength > 0) {
////                                dos.write(dataOut, 0, msgLength);
////                            }
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//                    Log.d("ADebugTag", "\nReader:" + reader.readLine());
//                    Log.d("ADebugTag", "\nData Sent");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//        }
//    }