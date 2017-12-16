package kivaaz.com.opencv_testing;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    private MenuItem               mItemType;

    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = NATIVE_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.5f;
    private int                    mAbsoluteFaceSize   = 0;

    Button clorBtn;
    private CameraBridgeViewBase _cameraBridgeViewBase ;
    // Used to load the 'native-lib' library on application startup.

    private BaseLoaderCallback _baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {

            switch (status){
                case LoaderCallbackInterface.SUCCESS:{
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("native-lib");
                    System.loadLibrary("opencv_java3");
                    try {
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir,"lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1){
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if(mJavaDetector.empty()){
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        }else{
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        }

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(),0);
                        cascadeDir.delete();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }



                    _cameraBridgeViewBase.enableView();
                }
                break;
                default:
                    super.onManagerConnected(status);
            }


        }

    };

    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        startActivity(new Intent(getBaseContext(),ImageComparison.class));

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA},
                1);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        clorBtn = (Button) findViewById(R.id.colorBlobbtn);
        clorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext(),ImageComparison.class));
                finish();
            }
        });
        _cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.main_surface);
        _cameraBridgeViewBase.setMaxFrameSize(width,height);
        _cameraBridgeViewBase.setVisibility(View.VISIBLE);
        _cameraBridgeViewBase.setCvCameraViewListener(this);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat matGray = inputFrame.gray();
        Mat matRGBA = inputFrame.rgba();



//        salt(matGray.getNativeObjAddr(),matRGBA.getNativeObjAddr(), 2000);
        mNativeDetector.start();


        if (mAbsoluteFaceSize == 0 ){
            int height = matGray.rows();
            if(Math.round(height * mRelativeFaceSize) > 0){
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR){
            if(mJavaDetector != null){
                mJavaDetector.detectMultiScale(matGray,faces,1.1,2,2,new Size(mAbsoluteFaceSize,mAbsoluteFaceSize),new Size());
            }
        }else if(mDetectorType == NATIVE_DETECTOR){
            if(mNativeDetector != null){
                mNativeDetector.detect(matGray, faces);
            }
        }else{
            Log.e(TAG, "Detection method is not selected!");
        }

//        Log.e(TAG, "Detector " + mDetectorType);

        Rect[] faceArray = faces.toArray();
        for(int i = 0; i< faceArray.length; i++){
            Imgproc.rectangle(matRGBA, faceArray[i].tl(),faceArray[i].br(),FACE_RECT_COLOR, 3);
        }

        return matRGBA;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, _baseLoaderCallback);
        }else{
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            _baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void disableCamera() {
        if(_cameraBridgeViewBase != null){
            _cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length > 0&& grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }else{
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }


    public native String stringFromJNI();

    public native void salt(long nativeObjAddr, long matAddrGray, int nbrElem);
}
