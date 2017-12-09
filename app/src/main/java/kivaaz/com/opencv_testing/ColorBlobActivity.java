package kivaaz.com.opencv_testing;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ColorBlobActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d("ColorBlob","Opencv Not Loaded");
        }else{
            Log.d("ColorBlob","OpenCV Loaded");
        }
    }

    int iLowH = 45;
    int iHighH = 75;
    int iLowS = 20;
    int iHighS = 255;
    int iLowV = 10;
    int iHighV = 255;
    Mat imgHsv, imgThresholded;
    Scalar sc1,sc2;
    JavaCameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_color_blob);

        sc1 = new Scalar(iLowH,iLowS,iLowV);
        sc2 = new Scalar(iHighH,iHighS,iHighV);
        cameraView  = (JavaCameraView) findViewById(R.id.colorBlobCamera);
        cameraView.setCameraIndex(0);
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        imgHsv = new Mat(width,height, CvType.CV_16UC4);
        imgThresholded = new Mat(width,height, CvType.CV_16UC4);

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Imgproc.cvtColor(inputFrame.rgba(), imgHsv,Imgproc.COLOR_BGR2HSV);
        Core.inRange(imgHsv,sc1,sc2,imgThresholded);

        return imgThresholded;
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.disableView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
    }
}
