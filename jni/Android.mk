LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_PATH := E:\CurrentWorks\acne_spot\jni\opencv_sdk

LOCAL_MODULE := libopencv_java
LOCAL_SRC_FILES := $(OPENCV_PATH)/native/libs/armeabi-v7a/libopencv_java.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
include $(OPENCV_PATH)/native/jni/OpenCV.mk

LOCAL_MODULE    := ImageProcessing
LOCAL_SRC_FILES := ImageProcessing.cpp
LOCAL_LDLIBS    += -lm -llog -landroid -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
