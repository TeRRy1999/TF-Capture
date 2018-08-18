package com.andrewginns.TFCapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.database.Cursor;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.Tensor;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

public class MainActivity extends AppCompatActivity {
    private int[] intValues;
    private float[] floatValues;
    private float[] hybridValues;
    private float[][][] placeholder;

    private ImageView ivPhoto;

    private FileInputStream is = null;
    private static final int CODE = 1;
    private static final int RESULT_LOAD_IMAGE = 2;

//    static final int SocketServerPORT = 8080;
//    ServerSocket serverSocket;
//
//    ServerSocketThread serverSocketThread;
//
//    serverSocketThread = new ServerSocketThread();
//    serverSocketThread.start();

    File photoFile;

    private TensorFlowInferenceInterface inferenceInterface;

    //  Model parameters
    private static final String MODEL_FILE = "file:///android_asset/optimized-graph.pb";
    private static final String INPUT_NODE = "inputA";
    private static final String OUTPUT_NODE = "a2b_generator/Conv_7/Relu:0";
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
        final String root;
        root = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                "tensorflow";
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

//    private static void storeFC(float[] floats, String path) {
//        FileOutputStream out = null;
//        try {
//            out = new FileOutputStream(path);
//            FileChannel file = out.getChannel();
//            ByteBuffer buf = file.map(FileChannel.MapMode.READ_WRITE, 0, 4 * floats.length);
//            for (float i : floats) {
//                buf.putFloat(i);
//            }
//            file.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } finally {
//            safeClose(out);
//        }
//    }
//
//    private static void safeClose(OutputStream out) {
//        try {
//            if (out != null) {
//                out.close();
//            }
//        } catch (IOException e) {
//            // do nothing
//        }
//    }

//    public class ServerSocketThread extends Thread {
//
//        @Override
//        public void run() {
//            Socket socket = null;
//
//            try {
//                serverSocket = new ServerSocket(SocketServerPORT);
//
//                while (true) {
//                    socket = serverSocket.accept();
//                    FileTxThread fileTxThread = new FileTxThread(socket);
//                    fileTxThread.start();
//                }
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } finally {
//                if (socket != null) {
//                    try {
//                        socket.close();
//                    } catch (IOException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }
//
//    public class FileTxThread extends Thread {
//        Socket socket;
//
//        FileTxThread(Socket socket){
//            this.socket= socket;
//        }
//
//        @Override
//        public void run() {
//            File file = new File(
//                    Environment.getExternalStorageDirectory(),
//                    "test.txt");
//
//            byte[] bytes = new byte[(int) file.length()];
//            BufferedInputStream bis;
//            try {
//                bis = new BufferedInputStream(new FileInputStream(file));
//                bis.read(bytes, 0, bytes.length);
//                OutputStream os = socket.getOutputStream();
//                os.write(bytes, 0, bytes.length);
//                os.flush();
//                socket.close();
//
//                final String sentMsg = "File sent to: " + socket.getInetAddress();
//                MainActivity.this.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        Toast.makeText(MainActivity.this,
//                                sentMsg,
//                                Toast.LENGTH_LONG).show();
//                    }});
//
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } finally {
//                try {
//                    socket.close();
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//
//        }
//    }


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

//    private String getIpAddress() {
//        String ip = "";
//        try {
//            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
//                    .getNetworkInterfaces();
//            while (enumNetworkInterfaces.hasMoreElements()) {
//                NetworkInterface networkInterface = enumNetworkInterfaces
//                        .nextElement();
//                Enumeration<InetAddress> enumInetAddress = networkInterface
//                        .getInetAddresses();
//                while (enumInetAddress.hasMoreElements()) {
//                    InetAddress inetAddress = enumInetAddress.nextElement();
//
//                    if (inetAddress.isSiteLocalAddress()) {
//                        ip += "SiteLocalAddress: "
//                                + inetAddress.getHostAddress() + "\n";
//                    }
//
//                }
//
//            }
//
//        } catch (SocketException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            ip += "Something Wrong! " + e.toString() + "\n";
//        }
//
//        return ip;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ADebugTag", "\nDefault Resolution: " + Integer.toString(res));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TextView textView;
        textView = (TextView) findViewById(R.id.text_view);
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        //noinspection deprecation
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress
                ());
        textView.setText("Your Device IP Address: " + ipAddress);
        Log.d("ADebugTag", "\nNetwork Address: " + ipAddress);

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

//        // Load a set of images from folder
//        onFolder.setOnClickListener(arg0 -> {
//
//
//                initTensorFlowAndLoadModel();
//                Intent i = new Intent(
//                        Intent.ACTION_PICK,
//                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//
//                startActivityForResult(i, RESULT_LOAD_IMAGE);
//        });

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

    private void stylizeImage(Bitmap bitmap) {

        Bitmap scaledBitmap = scaleBitmap(bitmap, res, res); // desiredSize
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0,
                scaledBitmap.getWidth(), scaledBitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / (127.5f) - 1f; //red

            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / (127.5f) - 1f; //green

            floatValues[i * 3 + 2] = (val & 0xFF) / (127.5f) - 1f; //blue
        }

//        placeholder = new float[150][150][256];
//        Tensor t = Tensor.create(placeholder);

        // Copy the input data into TensorFlow.
        inferenceInterface.feed(INPUT_NODE, floatValues, 1, res, res, 3);
        // Run the inference call.
        inferenceInterface.run(new String[]{OUTPUT_NODE});
        // Copy the output Tensor back into the output array.
        inferenceInterface.fetch(OUTPUT_NODE, hybridValues);

        // Send the returned array to the server
        File hybridStore = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        assert hybridStore != null;

        String hybridString = java.util.Arrays.toString(hybridValues);
        try {
            byte[] compressed = gzip(hybridString);
            Files.write(Paths.get(hybridStore + "/" + "compressed.gz"), compressed);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        float[] result = ((JSONArray) JSONArray.parse(hybridString)).toArray(new float[] {});

//        try{
//            RandomAccessFile aFile = new RandomAccessFile(hybridStore + "/" + "hybrid.txt", "rw");
//            FileChannel outChannel = aFile.getChannel();
//
//            //one float 4 bytes
//            ByteBuffer buf = ByteBuffer.allocate(4*150*150*256);
//            buf.clear();
//            buf.asFloatBuffer().put(hybridValues);
//
//            //while(buf.hasRemaining())
//            {
//                outChannel.write(buf);
//            }
//
//            //outChannel.close();
//            buf.rewind();
//
//            float[] out=new float[3];
//            buf.asFloatBuffer().get(out);
//
//            outChannel.close();
//
//        }
//        catch (IOException ex) {
//            System.err.println(ex.getMessage());
//        }

//        FileOutputStream fos = null;
//        DataOutputStream dos = null;
//
//        try {
//            // create file output stream
//            fos = new FileOutputStream(hybridStore + "/" + "hybrid.txt");
//
//            // create data output stream
//            dos = new DataOutputStream(fos);
//
//            // for each byte in the buffer
//            for (float f:hybridValues) {
//                // write float to the dos
//                dos.writeFloat(f);
//            }
//
//            // force bytes to the underlying stream
//            dos.flush();
//
//        } catch(Exception e) {
//            // if any I/O error occurs
//            e.printStackTrace();
//        } finally {
//            // releases all system resources from the streams
//            if(dos!=null) try {
//                is.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            if(fos!=null) try {
//                    fos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//        }

//        Gson gson = new Gson();
//
//        try (FileWriter file = new FileWriter(hybridStore + "/" + "hybrid.json")) {
//            String json = gson.toJson(hybridValues);
//            file.write(json);
//            File f = new File(hybridStore + "/" + "hybrid.json");
//            //Print absolute path
////            System.out.println(f.getAbsolutePath());
//            Log.d("ADebugTag", "\nLocation: " + f.getAbsolutePath());
//            System.out.println("Successfully Copied JSON Object to File...");
//            Log.d("ADebugTag", "\n\"Successfully Copied JSON Object to File...");
//
//            System.out.println("\nJSON generated");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        try (FileWriter file = new FileWriter(hybridStore + "/" + "hybrid.json")) {
//            JSONArray mJSONArray = new JSONArray(Arrays.asList(hybridValues));
//
//            file.write(mJSONArray.toString());
//            try {
//                byte[] compJSON = gzip(mJSONArray.toString());
//                Files.write(Paths.get(hybridStore + "/" + "compressed.gz"), compJSON);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            File f = new File(hybridStore + "/" + "hybrid.json");
//            //Print absolute path
////            System.out.println(f.getAbsolutePath());
//            Log.d("ADebugTag", "\nLocation: " + f.getAbsolutePath());
//            System.out.println("Successfully Copied JSON Object to File...");
//            Log.d("ADebugTag", "\n\"Successfully Copied JSON Object to File...");
//
//            System.out.println("\nJSON generated");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Log.d("ADebugTag", "\nArray Length: " + Integer.toString(hybridValues.length));
        Log.d("ADebugTag", "\nArray: " + hybridString);


//        for (int i = 0; i < intValues.length; ++i) {
//            intValues[i] =
//                    0xFF000000
//                            | (((int) ((floatValues[i * 3] + 1f) * 127.5f)) << 16) //red
//                            | (((int) ((floatValues[i * 3 + 1] + 1f) * 127.5f)) << 8) //green
//                            | ((int) ((floatValues[i * 3 + 2] + 1f) * 127.5f)); //blue
//        }
//        scaledBitmap.setPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0,
//                scaledBitmap.getWidth(), scaledBitmap.getHeight());
//        return scaledBitmap;

        //

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        long currentTime = System.currentTimeMillis();
        if (requestCode == CODE && resultCode == RESULT_OK) {
            try {
                is = new FileInputStream(photoFile);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                stylizeImage(bitmap);

                Toast.makeText(
                        MainActivity.this,
                        "Partial output generated",
                        Toast.LENGTH_LONG)
                        .show();
//                ivPhoto.setImageBitmap(bitmap2);
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
            stylizeImage(bitmap);
//            ivPhoto.setImageBitmap(bitmap2);
//
//            saveBitmap(bitmap2, (currentTime + "Processed" +  ".png"));
//            saveBitmap(BitmapFactory.decodeFile(picturePath), (currentTime + "UnProcessed" + "
// .png"));
            Toast.makeText(
                    MainActivity.this,
                    "Partial output generated",
                    Toast.LENGTH_LONG)
                    .show();

        }
    }

//    public class Bucket {
//
//        private String name;
//        private String firstImageContainedPath;
//
//        public Bucket(String name, String firstImageContainedPath) {
//            this.name = name;
//            this.firstImageContainedPath = firstImageContainedPath;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public String getFirstImageContainedPath() {
//            return firstImageContainedPath;
//        }
//    }
//
//    public static List<Bucket> getImageBuckets(Context mContext){
//        List<Bucket> buckets = new ArrayList<>();
//        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//        String [] projection = {MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images
// .Media.DATA};
//
//        Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
//        if(cursor != null){
//            File file;
//            while (cursor.moveToNext()){
//                String bucketPath = cursor.getString(cursor.getColumnIndex(projection[0]));
//                String firstImage = cursor.getString(cursor.getColumnIndex(projection[1]));
//                file = new File(firstImage);
//                if (file.exists() && !bucketSet.contains(bucketName)) {
//                    buckets.add(new Bucket(bucketName, firstImage));
//                }
//            }
//            cursor.close();
//        }
//        return buckets;
//    }

}