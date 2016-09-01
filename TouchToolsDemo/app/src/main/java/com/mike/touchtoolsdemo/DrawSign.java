package com.mike.touchtoolsdemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by johnjoe on 01/08/2016.
 */
public class DrawSign extends Activity {
    private TouchToolView mTouchToolView = null;
    private ImageView imgPen, imgDone, imgErase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.canvas_activity);
        initializeObject();
        eventListeners();
    }
    private void initializeObject() {
        imgPen = (ImageView) findViewById(R.id.imgPen);
        imgErase = (ImageView) findViewById(R.id.imgErase);
        imgDone = (ImageView) findViewById(R.id.imgDoneCanvas);
        mTouchToolView = (TouchToolView) findViewById(R.id.drawing);
    }
    private void eventListeners() {
        imgPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgPen.setImageResource(R.drawable.pen);
                imgErase.setImageResource(R.drawable.pinkeraser);
                mTouchToolView.setErase(false);
//                mTouchToolView.setBrushSize(20);
//                mTouchToolView.setLastBrushSize(20);
            }
        });
        imgDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTouchToolView.setDrawingCacheEnabled(true);
                String imagePath = store_image(mTouchToolView.getDrawingCache(), "/mnt/sdcard/Painter/", Math.random() + "image.jpg");
                Intent intent = new Intent();
                intent.putExtra("drawPath", imagePath);
                setResult(13, intent);
                finish();
            }
        });
        imgErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgPen.setImageResource(R.drawable.pen);
                imgErase.setImageResource(R.drawable.pinkeraser);
                mTouchToolView.setErase(true);
//                mTouchToolView.setBrushSize(15);
            }
        });
    }
    public String store_image(Bitmap _bitmapScaled, String dirPath, String fileName) {
        //you can create a new file name "test.jpg" in sdcard folder.
        File f = new File(dirPath, fileName);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            _bitmapScaled.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Log.d("mypath****************", dirPath + File.separator + fileName);
            return dirPath + fileName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}