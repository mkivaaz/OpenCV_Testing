package kivaaz.com.opencv_testing;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Muguntan on 12/13/2017.
 */

public class ORBDetector {

    FeatureDetector featureDetector;
    DescriptorExtractor descriptorExtractor;
    MatOfKeyPoint sourceKeypoints;

    public ORBDetector() {

    }

    public MatOfKeyPoint featureDetector(Mat imgsource){
        sourceKeypoints = new MatOfKeyPoint();
        featureDetector = FeatureDetector.create(FeatureDetector.ORB);
        featureDetector.detect(imgsource,sourceKeypoints);

        return sourceKeypoints;
    }

    public Mat descriptionExtractor(Mat imgsource, MatOfKeyPoint sourceKeypoints){
        Mat sourceDescriptor = new MatOfKeyPoint();
        descriptorExtractor = descriptorExtractor.create(DescriptorExtractor.ORB);
        descriptorExtractor.compute(imgsource,sourceKeypoints,sourceDescriptor);

        return sourceDescriptor;
    }

    public MatOfDMatch matchDetector(Mat sourceDescriptor, Mat sceneDescriptor){
        Scalar matchestColor = new Scalar(0, 255, 0);
        MatOfDMatch matches = new MatOfDMatch();
        LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();


        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
        descriptorMatcher.match(sourceDescriptor, sceneDescriptor, matches);

//        Sort them in the order of their distance.
        List<DMatch> matchesList = matches.toList();

        List<DMatch> matches_final = new ArrayList<DMatch>();
        for (int i = 0; i < matchesList.size(); i++) {
            float MIN_DIST = 200;
            if (matchesList.get(i).distance < MIN_DIST) {
                matches_final.add(matches.toList().get(i));
            }
        }

        MatOfDMatch matches_final_mat = new MatOfDMatch();
        matches_final_mat.fromList(matches_final);


//        if(goodMatchesList.size() >= 7){
//            return true;
//        }
        return matches_final_mat;
    }

    public Mat drawMatchLines(Mat srcImg, Mat scnImg, LinkedList<DMatch> goodMatchesList, MatOfKeyPoint sceneKeypoints){

            List<KeyPoint> srcKeypointlist = sourceKeypoints.toList();
            List<KeyPoint> scnKeypointlist = sceneKeypoints.toList();

            LinkedList<Point> sourcePoints = new LinkedList<>();
            LinkedList<Point> scenePoints = new LinkedList<>();

            for (int i = 0; i < goodMatchesList.size(); i++) {
                sourcePoints.addLast(srcKeypointlist.get(goodMatchesList.get(i).queryIdx).pt);
                scenePoints.addLast(scnKeypointlist.get(goodMatchesList.get(i).trainIdx).pt);
            }

            MatOfPoint2f srcMatOfPoint2f = new MatOfPoint2f();
            srcMatOfPoint2f.fromList(sourcePoints);
            MatOfPoint2f scnMatOfPoint2f = new MatOfPoint2f();
            scnMatOfPoint2f.fromList(scenePoints);

            Mat homography = Calib3d.findHomography(srcMatOfPoint2f,scnMatOfPoint2f,Calib3d.RANSAC, 3);
            Mat src_corners = new Mat(4, 1, CvType.CV_32FC2);
            Mat scn_corners = new Mat(4, 1, CvType.CV_32FC2);

            src_corners.put(0,0,new double[]{0,0});
            src_corners.put(1,0,new double[]{srcImg.cols(),0});
            src_corners.put(2,0,new double[]{srcImg.cols(),srcImg.rows()});
            src_corners.put(3,0,new double[]{0,srcImg.rows()});

            Core.perspectiveTransform(src_corners, scn_corners, homography);

            Mat img = scnImg;

            Imgproc.line(img, new Point(scn_corners.get(0,0)), new Point(scn_corners.get(1,0)), new Scalar(255,0,0),4);
            Imgproc.line(img, new Point(scn_corners.get(1,0)), new Point(scn_corners.get(2,0)), new Scalar(255,0,0),4);
            Imgproc.line(img, new Point(scn_corners.get(2,0)), new Point(scn_corners.get(3,0)), new Scalar(255,0,0),4);
            Imgproc.line(img, new Point(scn_corners.get(3,0)), new Point(scn_corners.get(0,0)), new Scalar(255,0,0),4);

            return img;
    }


}
