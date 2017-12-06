#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

using namespace std;
using namespace cv;

extern "C"{
    jstring Java_kivaaz_com_opencv_1testing_MainActivity_stringFromJNI(
            JNIEnv *env,
            jobject /* this */) {
        string hello = "Hello from C++";

        return env->NewStringUTF(hello.c_str());
    }

    JNIEXPORT void JNICALL
    Java_kivaaz_com_opencv_1testing_MainActivity_salt(JNIEnv *env, jobject instance, jlong matAddrGray,
                                                      jint nbrElem) {

        // TODO

        Mat &mGr = *(Mat *) matAddrGray;
        for (int k = 0; k < nbrElem; k++) {
            int i = rand() % mGr.cols;
            int j = rand() % mGr.rows;
            mGr.at<uchar>(j, i) = 255;
        }

    }
}



