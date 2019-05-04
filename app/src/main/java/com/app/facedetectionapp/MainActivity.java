package com.app.facedetectionapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends BaseActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private int height, width;
    private static final String TAG = "Main Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;
    CameraBridgeViewBase.CvCameraViewFrame mInputFrame;
    private CascadeClassifier cascadeClassifier; //Cascade classifier class for object detection.

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (checkPermission()) {

            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        System.out.println("View Started ");
        this.height = height;
        this.width = width;
        System.out.println(height + " ---- " + width);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        System.out.println("Stop");
        mRgba.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) { System.out.println(" onCameraFrame ------ ");

        mRgba = inputFrame.rgba();
        mInputFrame = inputFrame;
        // OpenCV orients the camera to left by 90 degrees
        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );
        //return recognize(inputFrame.rgba());

        MatOfRect faces = new MatOfRect();
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(mRgba,faces,
                    2,2,2,new Size(10,10),
                    new Size(width,height));
            //cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i <facesArray.length; i++) {
            int intRadius = facesArray[i].height/2;
            Imgproc.circle(mRgba, new Point(facesArray[i].x + (facesArray[i].width/2),
                            facesArray[i].y+(facesArray[i].height/2)),
                    (facesArray[i].height/2)+10,
                    new Scalar(255, 0, 0), 2);
        }
        //    Core.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
        return mRgba;
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    try {
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // Load the cascade classifier
                        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    mOpenCvCameraView.enableView();
                    try {
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

/*
    private void showCircleOnImage() {
        try {
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            System.out.println(mCascadeFile.exists());
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            System.out.println(buffer+" PATH: "+mCascadeFile.getAbsolutePath());
            // Load the cascade classifier
            CascadeClassifier cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (cascadeClassifier.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                cascadeClassifier = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

            // Use the classifier to detect faces
            MatOfRect faces = new MatOfRect();
            if (cascadeClassifier != null) {
                cascadeClassifier.detectMultiScale(mRgba,faces,
                        1.1,2,2,new Size(2,2),
                        new Size(400,400));

                //cascadeClassifier.detectMultiScale(mRgba, faces, 1.1, 2, 2,
                //      new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }

            //Toast.makeText(this, faces.toArray().length+"", Toast.LENGTH_SHORT).show();

            System.out.println("********************** "+faces.toArray().length+"");
            if (faces.toArray().length > 0) {
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.setVisibility(View.GONE);
            }
            // If there are any faces found, draw a rectangle around it
            Rect[] facesArray = faces.toArray();
            for (int i = 0; i <facesArray.length; i++) {
                Imgproc.circle(mRgba, new Point(facesArray[i].x + (facesArray[i].width/2),
                                facesArray[i].y+(facesArray[i].height/2)),
                        facesArray[i].width/2,
                        new Scalar(255, 0, 0), 2);
            }
            //

            Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
            // prepare bitmap
            Utils.matToBitmap(mRgba, bmp);

            Drawable dr = new BitmapDrawable(this.getResources(), bmp );
            ((ImageView)(findViewById(R.id.ivCaptureImage))).setBackground(dr);
            ((ImageView)(findViewById(R.id.ivCaptureImage))).setVisibility(View.VISIBLE);

            if (mOpenCvCameraView != null) {
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.setVisibility(View.GONE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void captureImage(View view) {
        showCircleOnImage();

    }*/
}
