package com.mike.touchtoolsdemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener,
        MultiTouchView.MultiTouchListener, TouchToolView.TouchToolViewInterface{
    private ImageView imageView;
    private boolean isDragging = false;
    private RelativeLayout mainLayout;
    private TOOL_KIND lastType = TOOL_KIND.TOOL_KIND_FINGER;
    private TextView txtStatus;
    private int refDrawing = 100;
    private ImageButton btn1;
    private ImageButton btn2;
    private ImageButton btn3;
    private ImageButton btn4;
    private ImageButton btn5;
    private ImageButton btn6;
    private ImageButton btn7;
    private ImageButton btn8;
//    private MultiTouchView multiTouchView;
    private final float minZoom = 0.9f;
    private final float maxZoom = 1.8f;
    private TouchToolView drawing;

    public MainActivity() {
    }

    @Override
    public void onMagnifyImageListener(boolean magnifyMode, float x, float y, boolean isZoomCamera) {
        Log.v("TEST","onMagnifyView magnifyMode: " + magnifyMode + " isZoomCamera: " + isZoomCamera);
        if(magnifyMode){
            enableMagnifier(x , y, isZoomCamera);
        }else{
            if(isZoomCamera){
                enableMagnifier(x , y, isZoomCamera);
            }else{
                disableMagnifier();
            }
        }
    }

    @Override
    public void onFinishDrawing(Bitmap bitmap) throws Exception {
        Log.v("TEST","onFinishDrawing");
//        magnifierImage.setImageBitmap(bitmap);
//        magnifierImage.invalidate();
    }

    @Override
    public void onZoomImageWhenWheeling(boolean isZoomOut) {
        Log.v("TEST","onZoomImageWhenWheeling");
        float x = drawing.getScaleX();
        float delta = 0;
        if(isZoomOut){
            // set increased value of scale x and y to perform zoom in functionality
            delta = (float) (x + 0.03);
            drawing.setScaleX((delta > maxZoom) ? maxZoom : delta);
            drawing.setScaleY((delta > maxZoom) ? maxZoom : delta);
        }else{
            delta = (float) (x - 0.03);
            drawing.setScaleX((delta < minZoom) ? minZoom : delta);
            drawing.setScaleY((delta < minZoom) ? minZoom : delta);
        }
    }

    @Override
    public void onMagnifyView(float x, float y, boolean magnifyMode, boolean isZoomCamera, boolean isZoomCamerOut, boolean isMagnify) {
        drawing.setZoomPos(null,
                x,
                y,
                magnifyMode,
                isZoomCamera,
                isZoomCamerOut,
                isMagnify);
    }

    @Override
    public void onCapture(float x, float y) {
        Log.v("TEST","onCapture in activity");
        drawing.createCapture(x, y);
    }

    @Override
    public void onResetRealSizeImage() {

    }

    @Override
    public void onDragImage(float x, float y) {
        /*multiTouchView.setTranslationX(x);
        multiTouchView.setTranslationY(y);
        multiTouchView.invalidate();*/
    }

    public static Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        view.layout(0, 0, view.getLayoutParams().width, view.getLayoutParams().height);
        view.draw(canvas);

        return returnedBitmap;
    }

    public enum TOOL_KIND {
        TOOL_KIND_FINGER,
        TOOL_KIND_MOUSE,
        TOOL_KIND_PEN,
        TOOL_KIND_MAGNIFY,
        TOOL_KIND_CAMERA,
        TOOL_KIND_TAPEMEASURE,
        TOOL_KIND_WHITEBOARDERASER,
        TOOL_KIND_SMALLERASER,
        TOOL_KIND_INVALID,

        NUM_TOOL_KIND,
    };

    static native void registerNatives();
    static native int getTouchTool(float[] arr, int size);

    static {
        System.loadLibrary("touchtools");
        registerNatives();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main_test);
//        initLayouts();
        initTest();
    }

    private void initTest() {
        btn1 = (ImageButton)findViewById(R.id.btn1);
        btn2 = (ImageButton)findViewById(R.id.btn2);
        btn3 = (ImageButton)findViewById(R.id.btn3);
        btn4 = (ImageButton)findViewById(R.id.btn4);
        btn5 = (ImageButton)findViewById(R.id.btn5);
        btn6 = (ImageButton)findViewById(R.id.btn6);
        btn7 = (ImageButton)findViewById(R.id.btn7);
        btn8 = (ImageButton)findViewById(R.id.btn8);
//        multiTouchView = (MultiTouchView)findViewById(R.id.multiTouchView);
        drawing = (TouchToolView)findViewById(R.id.drawing);

        mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPenMode();
            }
        });


        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSmallEraserMode();
            }
        });


        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBigEraserMode();
            }
        });


        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMagnifyGlassMode();
            }
        });


        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCameraMode();
            }
        });


        btn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMouseMode();
            }
        });

        btn7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTapeMeasureMode();
            }
        });

        btn8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void onTapeMeasureMode() {
        drawing.setTool_kind(TOOL_KIND.TOOL_KIND_TAPEMEASURE);
        drawing.refreshDrawable(TOOL_KIND.TOOL_KIND_TAPEMEASURE);

    }

    private void onMouseMode() {
        drawing.setTool_kind(TOOL_KIND.TOOL_KIND_MOUSE);
        drawing.refreshDrawable(TOOL_KIND.TOOL_KIND_MOUSE);

    }

    private void onCameraMode() {
        drawing.setTool_kind(TOOL_KIND.TOOL_KIND_CAMERA);
        drawing.refreshDrawable(TOOL_KIND.TOOL_KIND_CAMERA);
    }

    private void onMagnifyGlassMode() {
        drawing.setTool_kind(TOOL_KIND.TOOL_KIND_MAGNIFY);
        drawing.refreshDrawable(TOOL_KIND.TOOL_KIND_MAGNIFY);
    }

    private void onBigEraserMode() {
        drawing.setTool_kind(TOOL_KIND.TOOL_KIND_WHITEBOARDERASER);
        drawing.refreshDrawable(TOOL_KIND.TOOL_KIND_WHITEBOARDERASER);
    }

    private void onSmallEraserMode() {
        drawing.setTool_kind(TOOL_KIND.TOOL_KIND_SMALLERASER);
        drawing.refreshDrawable(TOOL_KIND.TOOL_KIND_SMALLERASER);
    }

    private void onPenMode() {
        drawing.setTool_kind(TOOL_KIND.TOOL_KIND_PEN);
        drawing.refreshDrawable(TOOL_KIND.TOOL_KIND_PEN);
    }

    private void resetGesture() {
//        disableMagnifier();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
//        Utils.printLog("main getActionMasked: " + event.getActionMasked());
        Utils.printLog("main getAction: " + event.getAction());
        if(event.getAction() == MotionEvent.ACTION_UP){
            Utils.printLog("main ACTION_UP");
            resetGesture();
        }else if(event.getAction() == MotionEvent.ACTION_POINTER_3_DOWN){
            Utils.printLog("main ACTION_DOWN");
            detectGestureTouchTools(event);
        }else{
//            Utils.printLog("main other");
//            return super.onTouchEvent(event);
        }
        return true;
    }

    private void disableMagnifier() {
//        drawing.setZoomPos(0, 0,false, false);
    }

    private void enableMagnifier(float x, float y, boolean isZoomCamera) {
//        multiTouchView.setZoomPos(x, y, true, isZoomCamera);
    }

    /**
     * detect each gestures touch on screen
     * @param event
     */
    private void detectGestureTouchTools(MotionEvent event) {
//        Utils.printLog("getAction: " + event.getAction());

        ArrayList<Float> pointers = new ArrayList<>();
        for (int i = 0; i < event.getPointerCount(); i++) {
            pointers.add(event.getX(i));
            pointers.add(event.getY(i));
            pointers.add(event.getTouchMajor(i));
        }

        TOOL_KIND ret;
        float[] arr = new float[pointers.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = pointers.get(i);

        if (event.getPointerCount() < 3) {
        } else if (event.getPointerCount() > 5) {
        } else {
            int r = getTouchTool(arr, arr.length);
            ret = TOOL_KIND.values()[r];
            Utils.printLog("ret: " + ret);

            /*if(lastType != ret){
                lastType = ret;

            }
            if(!isDragging){
                isDragging = true;
                mainLayout.setMotionEventSplittingEnabled(false);
                executeActions(lastType);
            }*/
        }
    }

    /**
     * display background for imageview
     * @param lastType
     */
    public void displayBackgroundImage(TOOL_KIND lastType){
        if(lastType == TOOL_KIND.TOOL_KIND_PEN){
            //call pen actions modules
            Utils.printLog("TOOL_KIND_PEN");
            imageView.setBackgroundResource(R.drawable.pen);
        }else if(lastType == TOOL_KIND.TOOL_KIND_SMALLERASER){
            //call small eraser actions modules
            Utils.printLog("TOOL_KIND_SMALLERASER");
            imageView.setBackgroundResource(R.drawable.pinkeraser);
        }else if(lastType == TOOL_KIND.TOOL_KIND_WHITEBOARDERASER){
            //call big eraser actions modules
            Utils.printLog("TOOL_KIND_WHITEBOARDERASER");
            imageView.setBackgroundResource(R.drawable.dryerase);
        }else if(lastType == TOOL_KIND.TOOL_KIND_CAMERA){
            Utils.printLog("TOOL_KIND_CAMERA");
            imageView.setBackgroundResource(R.drawable.camera);

        }else if(lastType == TOOL_KIND.TOOL_KIND_MAGNIFY){
            Utils.printLog("TOOL_KIND_MAGNIFY");
            imageView.setBackgroundResource(R.drawable.magglass);

        }else if(lastType == TOOL_KIND.TOOL_KIND_MOUSE){
            Utils.printLog("TOOL_KIND_MOUSE");
            imageView.setBackgroundResource(R.drawable.mouse);
        }else if(lastType == TOOL_KIND.TOOL_KIND_TAPEMEASURE){
            Utils.printLog("TOOL_KIND_TAPEMEASURE");
            imageView.setBackgroundResource(R.drawable.tapemeasure1);

        }else{
            //testing modules
        }

        imageView.invalidate();
    }

    /**
     * @param item  the view that received the drag event
     * @param event the event from {@link android.view.View.OnDragListener#onDrag(View, DragEvent)}
     * @return the coordinates of the touch on x and y axis relative to the screen
     */
    public static Point getTouchPositionFromDragEvent(View item, DragEvent event) {
        Rect rItem = new Rect();
        item.getGlobalVisibleRect(rItem);
        return new Point(rItem.left + Math.round(event.getX()), rItem.top + Math.round(event.getY()));
    }

    public static boolean isTouchInsideOfView(View view, Point touchPosition) {
        Rect rScroll = new Rect();
        view.getGlobalVisibleRect(rScroll);
        return isTouchInsideOfRect(touchPosition, rScroll);
    }

    public static boolean isTouchInsideOfRect(Point touchPosition, Rect rScroll) {
        return touchPosition.x > rScroll.left && touchPosition.x < rScroll.right //within x axis / width
                && touchPosition.y > rScroll.top && touchPosition.y < rScroll.bottom; //withing y axis / height
    }
}
