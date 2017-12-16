package kivaaz.com.opencv_testing;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageComparison extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = ".ImageComparison";

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d("ColorBlob","Opencv Not Loaded");
        }else{
            Log.d("ColorBlob","OpenCV Loaded");
        }
    }

    ImageView srcImg, scnImg;
    Button faceDetBtn, captureBtn, clearCacheBtn;
    LinearLayout imgViewBox;
    JavaCameraView imgCompCamera;

    ORBDetector ORBDetector;

    private Boolean btnClicked = false;
    private Mat matRgba = new Mat();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image_comparison);

        srcImg = (ImageView) findViewById(R.id.srcImgview);
        scnImg = (ImageView) findViewById(R.id.scnImgview);

        faceDetBtn = (Button) findViewById(R.id.faceDetectBtn);
        captureBtn = (Button) findViewById(R.id.imgCaptBtn);
        clearCacheBtn = (Button) findViewById(R.id.clearCacheBtn);

        imgViewBox = (LinearLayout) findViewById(R.id.imgViewBox);

        imgCompCamera = (JavaCameraView) findViewById(R.id.imgCompCamera);
        imgCompCamera.setCameraIndex(0);
        imgCompCamera.setCvCameraViewListener(this);
        imgCompCamera.enableView();

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnClicked = true;
                Toast.makeText(getBaseContext(),"Finding Matches",Toast.LENGTH_SHORT).show();

            }
        });
        clearCacheBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearDir();
            }
        });
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat matGray = inputFrame.gray();
        final Mat matRGBA = inputFrame.rgba();
        final int[] matchTotal = new int[1];
        final Mat matchedMat = new Mat();
        final MatOfKeyPoint[] matchFeature = {new MatOfKeyPoint()};
        final Mat[] matchDescriptor = {new MatOfKeyPoint()};
        final MatOfDMatch[] matchedList = {new MatOfDMatch()};
        final Bitmap[] matchedBitmap = new Bitmap[1];

        ORBDetector = new ORBDetector();
        if(btnClicked) {
            matRGBA.assignTo(matRgba);
            final Bitmap img = captureBitmap(matRgba);

            btnClicked = false;

            final MatOfKeyPoint sceneKeypoints = ORBDetector.featureDetector(matRgba);
            final Mat sceneDescriptors = ORBDetector.descriptionExtractor(matRgba, sceneKeypoints);

            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    scnImg.setImageBitmap(img);
                    imgViewBox.setVisibility(View.VISIBLE);
                    imgCompCamera.setVisibility(View.GONE);

                    File directory = new File(Environment.getExternalStorageDirectory()
                            + "/Android/data/"
                            + getApplicationContext().getPackageName()
                            + "/Files");
                    if(directory.exists()){
                        Log.d("Storage Details",directory.getAbsolutePath().toString() + " exists");
                        File[] contents = directory.listFiles();

                        if(contents == null || contents.length == 0){
                            Log.d("Storage Details",contents.length + " files exists");
                            Toast.makeText(getBaseContext(),"No Images to Compare with",Toast.LENGTH_SHORT).show();

                            storeImage(img);
                            imgViewBox.setVisibility(View.GONE);
                            imgCompCamera.setVisibility(View.VISIBLE);
                        }else {
                            Log.d("Storage Details",contents.length + " files exists");
//                            contents[0].delete();
                            Bitmap srcBitmap = null;
                            for (File content : contents) {
                                srcBitmap = BitmapFactory.decodeFile(content.getParent() + "/" + content.getName().replace(".jpg",""));
                                Mat srcMat = new Mat();
                                Utils.bitmapToMat(srcBitmap,srcMat);
                                MatOfKeyPoint sourceKeypoints = ORBDetector.featureDetector(srcMat);
                                Mat sourceDescriptors = ORBDetector.descriptionExtractor(srcMat, sourceKeypoints);
                                MatOfDMatch goodMatchesList = ORBDetector.matchDetector(sourceDescriptors,sceneDescriptors);

                                Log.d("SURF Detector", "Found Object " + goodMatchesList.toList().size());
                                if(goodMatchesList.toList().size() > 2 && matchTotal[0] <= goodMatchesList.toList().size()){
                                    matchTotal[0] = goodMatchesList.toList().size();
                                    Utils.bitmapToMat(srcBitmap,matchedMat);
                                    matchFeature[0] = sourceKeypoints;
                                    matchDescriptor[0] = sourceDescriptors;
                                    matchedList[0] = goodMatchesList;
                                    matchedBitmap[0] = srcBitmap;
                                }

                            }

                            if(matchTotal[0] > 5){
                                Toast.makeText(getBaseContext(),"Found Match",Toast.LENGTH_SHORT).show();
                                srcImg.setImageBitmap(matchedBitmap[0]);
                                Mat output = new Mat();
                                output = matRGBA.clone();
                                Imgproc.cvtColor(matRgba, matRgba, Imgproc.COLOR_RGBA2RGB, 1);
                                Imgproc.cvtColor(matchedMat, matchedMat, Imgproc.COLOR_RGBA2RGB, 1);
                                Features2d.drawMatches(matRgba, sceneKeypoints, matchedMat, matchFeature[0], matchedList[0], output);
                                Imgproc.resize(output, output, matRgba.size());
                                scnImg.setImageBitmap(captureBitmap(output));
                            }else{
                                storeImage(img);
                                Toast.makeText(getBaseContext(), "No Matches Found", Toast.LENGTH_SHORT).show();
                                imgViewBox.setVisibility(View.GONE);
                                imgCompCamera.setVisibility(View.VISIBLE);
                            }


                        }
                    }else{
                        if(directory.mkdirs()){
                            Log.d("Storage Details", directory.getAbsolutePath().toString() + " created");
                        }else{
                            Log.d("Storage Details", "Directory Unable to Create");
                        }


                    }
                }
            });



        }

        return matRGBA;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        imgCompCamera.disableView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        imgCompCamera.disableView();
    }

    private Bitmap captureBitmap(Mat mRgba){
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgba, bitmap);
            return bitmap;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ System.currentTimeMillis() +".png";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);

        return mediaFile;
    }

    public void showCamera(){
        imgViewBox.setVisibility(View.GONE);
        imgCompCamera.setVisibility(View.VISIBLE);
    }

    private void clearDir(){
        File directory = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");
        if(directory.exists()){
            Log.d("Storage Details",directory.getAbsolutePath().toString() + " exists");
            File[] contents = directory.listFiles();
            if(contents == null || contents.length == 0){
                Log.d("Storage Details","Directory is Already Empty");
            }else {
                for (File content : contents) {
                    String filename = content.getName();
                    if(content.delete()){
                        Log.d("Storage Detaila", filename.toString() + " deleted");
                    }
                }
                Toast.makeText(getBaseContext(),"Cache Cleared",Toast.LENGTH_SHORT).show();
            }
        }
    }
}
