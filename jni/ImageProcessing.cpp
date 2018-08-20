#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#define  LOG_TAG  "Test Project"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

jobject BitmapLock( JNIEnv* env, jobject thiz, jobject pBitmap, void** bmpBuffer )
{
	jobject pBitmapRef = env->NewGlobalRef(pBitmap); //lock the bitmap preventing the garbage collector from destructing it

	if (pBitmapRef == NULL)
	{
		*bmpBuffer = NULL;
		return NULL;
	}

	int result = AndroidBitmap_lockPixels(env, pBitmapRef, bmpBuffer);

	if (result != 0)
	{
		*bmpBuffer = NULL;
		return NULL;
	}

	return pBitmapRef;
}

void BitmapUnlock( JNIEnv* env, jobject thiz, jobject pBitmapRef, void* bmpBuffer )
{
	if (pBitmapRef)
	{
		if (bmpBuffer)
		{
			AndroidBitmap_unlockPixels(env, pBitmapRef);
			bmpBuffer = NULL;
		}
		env->DeleteGlobalRef(pBitmapRef);
		pBitmapRef = NULL;
	}
}

extern "C"
{
	CascadeClassifier face_cascade;

	void Java_acne_recoz_MainActivity_rotateBitmap(JNIEnv* env, jobject thiz,
			jint width, jint height, jobject bmpImg, jboolean bLeft, jintArray outPixels)
	{
		void* bmpImgBuffer;
		jobject bmpImgRef = BitmapLock(env, thiz, bmpImg, &bmpImgBuffer);
		jint * pOutPixels = env->GetIntArrayElements(outPixels, 0);

		int cola_cnt = 0;
		Mat orgImg = Mat(height, width, CV_8UC4, bmpImgBuffer);
		Mat resultImg = Mat(width, height, CV_8UC4, (unsigned char *)pOutPixels);

		Mat rotated;
		if(bLeft){
			transpose(orgImg, rotated);
			flip(rotated, rotated, 1);
			transpose(rotated, rotated);
			flip(rotated, rotated, 1);
			transpose(rotated, rotated);
			flip(rotated, rotated, 1);
		}
		else
		{
			transpose(orgImg, rotated);
			flip(rotated, rotated, 1);
		}

		cvtColor(rotated, rotated, CV_BGRA2RGBA);
		IplImage srcImg = rotated;
		IplImage ResultImg = resultImg;
		cvCopy(&srcImg, &ResultImg);

		BitmapUnlock(env, thiz, bmpImgRef, bmpImgBuffer);
		env->ReleaseIntArrayElements(outPixels, pOutPixels, 0);
	}

	jint Java_acne_recoz_MainActivity_acneRecoz(JNIEnv* env, jobject thiz,
			jint width, jint height, jobject bmpImg, jstring data_path, jint face_size, jintArray outPixels)
	{
		void* bmpImgBuffer;
		jobject bmpImgRef = BitmapLock(env, thiz, bmpImg, &bmpImgBuffer);
		jint * pOutPixels = env->GetIntArrayElements(outPixels, 0);
		const char* datapath = env->GetStringUTFChars(data_path, 0);

		int acneCnt = 0;
		Mat orgImg = Mat(height, width, CV_8UC4, bmpImgBuffer);
		Mat resultImg = Mat(height, width, CV_8UC4, (unsigned char *)pOutPixels);

		int w = 720;
		int h = height * w / width;
		resize(orgImg, orgImg, Size(w, h));

		Mat procImg;
		cvtColor( orgImg, procImg, CV_BGR2GRAY );

		adaptiveThreshold(procImg, procImg, 255, 0, THRESH_BINARY, 15, 5);
		dilate(procImg, procImg, Mat(), Point(-1, -1), 1);

		vector<vector<Point> > contours;
		findContours( procImg, contours, RETR_LIST, CHAIN_APPROX_SIMPLE );

		for( size_t i = 0; i < contours.size(); i++ )
		{
			if( contourArea(contours[i]) > 20 & contourArea(contours[i]) < 150 )
			{
				Rect minRect = boundingRect( Mat(contours[i]) );
				Mat imgroi(orgImg, minRect);

				cvtColor(imgroi, imgroi, CV_BGR2HSV);
				Scalar color = mean(imgroi);
				cvtColor(imgroi, imgroi, CV_HSV2BGR);

				if(color[0] < 200 & color[1] > 70 & color[2] > 30)
				{
					Point2f center;
					float radius = 0;
					minEnclosingCircle(Mat(contours[i]), center, radius);

					if(radius < 20)
					{
						rectangle(orgImg, minRect, Scalar(0,255,0), 1);
						acneCnt++;
					}
				}
			}
		}

		resize(orgImg, orgImg, Size(width, height));
		cvtColor(orgImg, orgImg, CV_BGR2RGBA);
		IplImage srcImg = orgImg;
		IplImage ResultImg = resultImg;
		cvCopy(&srcImg, &ResultImg);

		BitmapUnlock(env, thiz, bmpImgRef, bmpImgBuffer);
		env->ReleaseIntArrayElements(outPixels, pOutPixels, 0);
		env->ReleaseStringUTFChars(data_path, datapath);

		return acneCnt;
	}
}
