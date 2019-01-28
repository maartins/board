#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_martins_board_ArucoProcessing_estimatePoseFromMarkers(
        JNIEnv *env, jlong markerCorners, jfloat markerLength, jlong cameraMatrix, jlong distCoeff, jlong rvecs, jlong tvecs) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
