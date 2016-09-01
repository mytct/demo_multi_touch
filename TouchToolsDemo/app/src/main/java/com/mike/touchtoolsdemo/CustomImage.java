package com.mike.touchtoolsdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mtoanmy on 7/29/16.
 */
public class CustomImage extends ImageView {

    private PointF zoomPos;
    private Matrix matrix;
    private Paint paint;
    private boolean zooming = false;
    private boolean isZoomCamera;
    private BitmapShader shader;
    private Bitmap bitmap;
    private final int sizeMaglass = 170;
    private float offset = 2f;
    private Bitmap bitmapCamera;

    public CustomImage(Context context) {
        super(context);
        init();
    }

    public CustomImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        zoomPos = new PointF(0, 0);
        matrix = new Matrix();
        paint = new Paint();
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.magglass);
        bitmapCamera = BitmapFactory.decodeResource(getResources(), R.drawable.camera);
    }

    private void createCapture(float x, float y) {
        Matrix matrix = new Matrix();
        matrix.postScale(2f, 2f, x, y);
       /* paintCaptured = Bitmap.createBitmap(getDrawingCache(),
                (int)x - (int)(bitmap.getWidth()/(offset +1)),
                (int)y - (int)(bitmap.getHeight()/(offset)),
                (int)(bitmap.getWidth()/(offset +1)),
                (int)(bitmap.getHeight()/(offset)) - biasHieght/8,
                matrix, false);*/
    }

    public void setZoomPos(float x, float y, boolean zooming, boolean isZoomCamera, Bitmap magglas) {
        Log.v("TEST","setZoomPos: " + "zooming: " + zooming + " isZoomCamera: " + isZoomCamera);
        this.zoomPos.x = x;
        this.zoomPos.y = y;
        this.zooming = zooming;
        this.isZoomCamera = isZoomCamera;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /**
         * drawing magnify
         */
		if (!zooming) {
			Log.v("TEST","do not draw magnify canvas");
			buildDrawingCache();
		} else {
			Log.v("TEST","start draw magnify canvas");
			magnifyImage(zoomPos.x,
					zoomPos.y);

			if(!isZoomCamera){
//                float delta = Math.abs(bg.getWidth() - bg.getWidth()*getScaleX());
//				Log.v("TEST","draw magnify glass in magnify glass delta = " + delta);
                Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,
                        (int)(bitmap.getWidth()/getScaleX()),
                        (int)(bitmap.getHeight()/getScaleX()),
                        false);
                canvas.drawCircle(zoomPos.x - getX(),
                        zoomPos.y - (int)(bitmap.getHeight()/getScaleX())/4 - getY(),
                        sizeMaglass/getScaleX(),
                        paint);
                canvas.drawBitmap(newBitmap,
                        zoomPos.x - (int)(bitmap.getWidth()/getScaleX())/2 - getX(),
                        zoomPos.y - (int)(bitmap.getHeight()/getScaleX())/2 - getY(),
                        paint);
			}else{
				Log.v("TEST","draw magnify glass in camera");
                Bitmap newBitmap = Bitmap.createScaledBitmap(bitmapCamera,
                        (int)(bitmapCamera.getWidth()/getScaleX()),
                        (int)(bitmapCamera.getHeight()/getScaleX()),
                        false);
                canvas.drawRect(
                        zoomPos.x - newBitmap.getWidth()/2 - getX(),
                        zoomPos.y - newBitmap.getHeight()/2 + 30 - getY(),
                        zoomPos.x + (sizeMaglass*1.2f)/getScaleX() - getX(),
                        zoomPos.y + (sizeMaglass*1.2f)/getScaleX() - getY(),
                        paint);
                canvas.drawBitmap(newBitmap,
                        zoomPos.x - newBitmap.getWidth()/2 - getX(),
                        zoomPos.y - newBitmap.getHeight()/2 - getY(),
                        paint);
			}
		}
    }

    private void magnifyImage(float x, float y) {
        Bitmap bitmap = getDrawingCache();
        shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        paint = new Paint();
        paint.setShader(shader);
        matrix.reset();
        Log.v("TEST","getScale: " + getScaleX());
        matrix.postScale(2f, 2f, x, y);
        paint.getShader().setLocalMatrix(matrix);
    }
}
