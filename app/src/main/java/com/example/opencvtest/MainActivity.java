package com.example.opencvtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements  CameraBridgeViewBase.CvCameraViewListener2 {
        CameraBridgeViewBase cameraBridgeViewBase;
        JavaCameraView javaCameraView;
        Mat mRGBA, mRGBAT;
        Mat buffImg;
        private Mat mRgba, mGray,tmp;
        File cascFile;
        CascadeClassifier faceDetector;
        LBPHFaceRecognizer lbphFaceRecognizer;
        //private opencv_face.FaceRecognizer mLBPHFaceRecognizer = opencv_face.LBPHFaceRecognizer.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("OPENCV","MSG LOADING STATUS: ${OpenCVLoader.initDebug()}");

        javaCameraView = (JavaCameraView) findViewById(R.id.my_camera_view);
        javaCameraView.setCameraIndex(1);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCameraPermissionGranted();
        //javaCameraView.setRotation(360);
        javaCameraView.setCvCameraViewListener(MainActivity.this);

    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height,width, CvType.CV_8UC4);
        mRgba = new Mat(height,width, CvType.CV_8UC4);
        tmp = new Mat();

        mGray = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }
    //
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //input frame je frame kamere
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        //tmp = inputFrame.gray();
        //mRGBAT = mRGBA.t();

        //detect face
        Rect rect_crop = null;

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(mRgba,faceDetections);
        for(Rect rect: faceDetections.toArray()){
            Imgproc.rectangle(mRgba,new Point(rect.x,rect.y),
                    new Point(rect.x + rect.width,rect.y+rect.height),
                    new Scalar(255,0,0));


            //buff img je grayscale version trenutnega frama
            buffImg = new Mat(rect.width,rect.height,CvType.CV_8UC1);
            Imgproc.cvtColor(mRgba,buffImg,Imgproc.COLOR_RGBA2GRAY);
            //rect_crop je nov rectangle z dimenzijami kvadrata na kordinatah kjer je zaznam obraz
            rect_crop = new Rect(rect.x,rect.y,rect.width,rect.height);
            // nova matrika ki im iz kvadrata
            //Mat croppedImg = new Mat(buffImg,rect_crop);
            Mat croppedImg = new MatOfRect(rect_crop);
            double[] rgb = croppedImg.get(rect.x,rect.y);
            //Log.d("PIXELTEST2", "RGB:"+rgb);

            //slika sebe z virov
            InputStream imgStream = this.getResources().openRawResource(R.raw.slika);
            Bitmap imgBitmap = BitmapFactory.decodeStream(imgStream);
            //naredimo novo matriko z dimenzijami kvadrata in bitmap slike pretvorimo v matriko
            Mat imgFromPhone = new Mat(rect.width,rect.height,CvType.CV_8UC1);
            Utils.bitmapToMat(imgBitmap,imgFromPhone);
            //pretvorimo v sivo
            Imgproc.cvtColor(imgFromPhone,imgFromPhone,Imgproc.COLOR_RGBA2GRAY);
            //int croppedSize = rect.height * rect.width;
            //novi Mat list za slike s katerih bomo ucili
            List<Mat> trainData = new ArrayList<>();
            trainData.add(imgFromPhone);
            trainData.add(imgFromPhone);
            Log.d("TRAIN",String.valueOf(trainData.size()));
            //Definiramo labele
            Mat labels = new Mat(1,trainData.size(),CvType.CV_32S);
            int [] l ={1,2,3};
            labels.put(0,0,l);
            Log.d("TRAIN",String.valueOf(labels.size()));
            // z train preucimo slike
           // lbphFaceRecognizer.train(trainData,labels);
             // z recognize dobimo ugotovimo katerimu je najblizje detectan face
            //int [] label = new int[1];
           // double [] conf = new double[1];
           // lbphFaceRecognizer.predict(croppedImg,label,conf);
           // Log.d("TESTPREDICT", "label: "+label[0]+ "conf: "+conf[0]);


        }

        return mRgba;
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.d("MainActivity","OpenCv pravilno deluje");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }else{
            Log.d("MainActivity","OpenCv nepravilno deluje");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,baseLoaderCallback);
        }
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS: {
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    cascFile = new File(cascadeDir,"haarcascade_frontalface_alt2.xml");

                    try {
                        FileOutputStream fos  = new FileOutputStream(cascFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while((bytesRead= is.read(buffer))!=-1){
                            fos.write(buffer,0,bytesRead);
                        }
                        is.close();
                        fos.close();
                        faceDetector = new CascadeClassifier(cascFile.getAbsolutePath());

                        if(faceDetector.empty()){
                            faceDetector = null;
                        }else {
                        cascadeDir.delete();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    javaCameraView.enableView();
                    break;
                }
                default:
                {
                    super.onManagerConnected(status);
                }
            }
        }
    };

    public void onButtonPress(View view) {

    }
}