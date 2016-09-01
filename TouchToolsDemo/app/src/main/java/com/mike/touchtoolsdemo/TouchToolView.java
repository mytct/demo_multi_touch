package com.mike.touchtoolsdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ClipDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Created by johnjoe on 01/08/2016.
 */
public class TouchToolView extends RelativeLayout {
    //drawing path
    private Path drawPath, circlePath, mPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint, circlePaint, mBitmapPaint;
    //initial color
    private int paintColor = 0xFF000000, paintAlpha = 255;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;
    //erase flag
    private boolean erase = false;
    float mX, mY;
    private static final float TOUCH_TOLERANCE = 1;
    private Bitmap bitmap;
    private MainActivity.TOOL_KIND tool_kind = MainActivity.TOOL_KIND.TOOL_KIND_PEN;
    final int MAX_NUMBER_OF_POINT = 10;
    float[] x = new float[MAX_NUMBER_OF_POINT];
    float[] y = new float[MAX_NUMBER_OF_POINT];
    boolean[] touching = new boolean[MAX_NUMBER_OF_POINT];
    private RectF RectFTouchCapture;
    private RectF RectFLeftMouse;
    private RectF RectFRightMouse;
    private RectF RectFWheelMouse;
    private RectF RectFZoomAreaCamera;
    private RectF RectFZoomAreaCameraOut;
    private boolean isTouchLeftMouse = false;
    private boolean isTouchRightMouse = false;
    private Bitmap bitmapMouseLeft;
    private Bitmap bitmapMouseRight;
    private float offset = 2f;
    private boolean isZoomCamera;
    private boolean isZoomCameraOut;
    private Bitmap paintCaptured;
    private boolean isCaptureCamera;
    private int biasHieght = 200;
    private float biasPercent = 0.5f;
    private Bitmap newBitmap;
    private boolean zooming = false;
    private PointF zoomPos;
    private BitmapShader shader;
    private Matrix matrix;
    private Paint paintMagnify;
    private final int sizeMaglass = 170;
    private float lastYWheel = 0;
    private TouchToolViewInterface listener;
    private PointF centerPoint;
    private Bitmap bg;
    private boolean isDragging = false;
    private boolean isMagnify;
    private float currZoom = 1f;
    private ImageView img;
    private Bitmap bitmapTape;
    private ImageView imgTape;
    private PointF lastPoint;
    private double lastDistance;
    private ImageView imgCursor;
    private RectF RectFdrawRectangle;
    private Paint mouseRectFangle;
    private Bitmap imgContextMenu;
    private PointF lastMousePos = new PointF(0,0);
    private ImageView imgCaptured;

    public interface TouchToolViewInterface{
        void onZoomImageWhenWheeling(boolean isZoomOut);
        void onMagnifyView(float x, float y, boolean magnifyMode, boolean isZoomCamera, boolean isZoomCamerOut, boolean isMagnify);
        void onCapture(float x, float y);
    }

    public TouchToolView(Context context, AttributeSet attrs){
        super(context, attrs);
        setupDrawing(context);
    }
    //setup drawing
    private void setupDrawing(Context context){
        zoomPos = new PointF(0, 0);
        matrix = new Matrix();

        //prepare for drawing and setup paint stroke properties
        drawPath = new Path();
        mPath = new Path();
        drawPaint = new Paint();
        mouseRectFangle = new Paint();
        paintMagnify = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        circlePaint = new Paint();
        circlePath = new Path();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.TRANSPARENT);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeJoin(Paint.Join.MITER);
        circlePaint.setStrokeWidth(0f);
        setDrawingCacheEnabled(true);
        listener = (TouchToolViewInterface)context;

        mouseRectFangle.setColor(Color.BLUE);
        mouseRectFangle.setAlpha(100);
        mouseRectFangle.setAntiAlias(true);
        mouseRectFangle.setStyle(Paint.Style.FILL);
        mouseRectFangle.setStrokeJoin(Paint.Join.ROUND);
        mouseRectFangle.setStrokeCap(Paint.Cap.ROUND);

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pen);
        bitmapMouseLeft = BitmapFactory.decodeResource(getResources(), R.drawable.mouse_left);
        bitmapMouseRight = BitmapFactory.decodeResource(getResources(), R.drawable.mouse_right);
        bitmapTape = BitmapFactory.decodeResource(getResources(), R.drawable.tapemeasure2);

        imgCursor = new ImageView(context);
        imgCursor.setBackgroundResource(R.drawable.cursor);
        imgCursor.setVisibility(GONE);

        imgContextMenu = BitmapFactory.decodeResource(getResources(), R.drawable.context_menu);

        img = new ImageView(context);
        img.setBackgroundResource(R.drawable.pen);
        img.setVisibility(GONE);

        imgCaptured = new ImageView(context);
        imgCaptured.setVisibility(GONE);

        imgTape = new ImageView(context);
//        imgTape.setImageBitmap(bitmapTape);
        imgTape.setBackgroundResource(R.drawable.clip_source_horizontal);
        imgTape.setVisibility(GONE);
        imgTape.setLayoutParams(new LayoutParams(bitmapTape.getWidth(), bitmapTape.getHeight()));
//        imgTape.setScrollY(200);
        addView(imgTape);
        addView(imgCaptured);
        addView(img);
        addView(imgCursor);
    }
    //size assigned to view
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvasBitmap.eraseColor(Color.TRANSPARENT);
        drawCanvas = new Canvas(canvasBitmap);
    }
    //draw the view - will be called after touch event
    @Override
    protected void onDraw(Canvas canvas) {

        if (!zooming) {
            Log.v("TEST","do not draw magnify canvas");
//            buildDrawingCache();
        } else {
            Log.v("TEST","start draw magnify canvas");
            if(isMagnify){
                magnifyImage(bitmap, zoomPos.x,
                        zoomPos.y);
                Log.v("TEST","draw magnify glass in magnify glass");
                if(numberTouchPoint() == 3){
                    canvas.drawCircle(zoomPos.x + newBitmap.getWidth()/2,
                            zoomPos.y + newBitmap.getHeight()/4 - 10,
                            sizeMaglass/getScaleX(), paintMagnify);
                }
            }else{
                if(isZoomCamera || isZoomCameraOut){
                    magnifyImage(bitmap, zoomPos.x,
                            zoomPos.y);
                    Log.v("TEST","draw magnify glass in camera");
                    canvas.drawRect(
                            img.getX() + img.getWidth()/17,
                            img.getY() + 2*img.getHeight()/20,
                            img.getX() + 2*img.getWidth()/3,
                            img.getY() + 18*img.getHeight()/20,
                            paintMagnify
                    );
                }

            }
        }

        if(erase) {
            canvas.drawBitmap(canvasBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(circlePath, circlePaint);
        } else {
            canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
            canvas.drawPath(drawPath, drawPaint);
        }

        if(numberTouchPoint() >= 1){
            if(centerPoint != null && centerPoint.x != 0 && centerPoint.y != 0){
                lastPoint.x = centerPoint.x-bitmap.getWidth()/ offset;
                lastPoint.y = centerPoint.y -bitmap.getHeight()/ offset;

                if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN ||
                        tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER ||
                        tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER){
                    /*canvas.drawBitmap(newBitmap,
                            centerPoint.x -newBitmap.getWidth()/ offset,
                            centerPoint.y -newBitmap.getHeight()/ offset, null);*/
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY){
                    if(numberTouchPoint() == 3){
                        /*canvas.drawBitmap(newBitmap,
                                centerPoint.x -newBitmap.getWidth()/ offset,
                                centerPoint.y -newBitmap.getHeight()/ offset, null);*/
                    }
                }
                else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
                    if(numberTouchPoint() >= 4){

                        /*img.setAlpha(0.5f);
                        canvasPaint.setColor(Color.RED);
                        RectFTouchCapture = new RectF(
                                img.getX() + img.getWidth()/2,
                                img.getY() - img.getHeight()/5,
                                img.getX() + img.getWidth(),
                                img.getY() + img.getHeight()/5
                        );
                        canvas.drawRect(RectFTouchCapture, canvasPaint);

                        canvasPaint.setColor(Color.YELLOW);
                        RectFZoomAreaCamera = new RectF(
                                img.getX() + img.getWidth()/2,
                                img.getY() + img.getHeight()/5,
                                img.getX() + 3*img.getWidth()/4,
                                img.getY() + img.getHeight()
                        );
                        canvas.drawRect(RectFZoomAreaCamera, canvasPaint);

                        canvasPaint.setColor(Color.BLUE);
                        RectFZoomAreaCameraOut = new RectF(
                                img.getX() + 3*img.getWidth()/4,
                                img.getY() + img.getHeight()/5,
                                img.getX() + img.getWidth(),
                                img.getY() + img.getHeight()
                        );
                        canvas.drawRect(RectFZoomAreaCameraOut, canvasPaint);*/

                        /*if(paintCaptured != null){
                            Log.v("TEST","draw paint");
                            canvas.drawBitmap(paintCaptured,
                                    centerPoint.x -bitmap.getWidth()/ offset,
                                    centerPoint.y -bitmap.getHeight()/ offset + biasHieght/4, null);
                        }*/

                        Log.v("TEST","onDraw isZoomCamera: " + isZoomCamera);
                        Log.v("TEST","onDraw isZoomCameraOut: " + isZoomCameraOut);
                        Log.v("TEST","onDraw isZoomCameraCapture: " + isCaptureCamera);
                    }
                }
                else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
                    if(numberTouchPoint() >= 4){
                        /*if(isTouchLeftMouse){
                            canvas.drawBitmap(newBitmap,
                                    centerPoint.x - newBitmap.getWidth()/ offset,
                                    centerPoint.y - newBitmap.getHeight()/(offset - biasPercent), null);
                        }else if(isTouchRightMouse){
                            canvas.drawBitmap(newBitmap,
                                    centerPoint.x - newBitmap.getWidth()/ offset,
                                    centerPoint.y - newBitmap.getHeight()/(offset - biasPercent), null);
                        }else{
                            canvas.drawBitmap(newBitmap,
                                    centerPoint.x - newBitmap.getWidth()/ offset,
                                    centerPoint.y - newBitmap.getHeight()/(offset - biasPercent), null);
                        }*/

                        if(isTouchLeftMouse){
                            canvas.drawRect(RectFdrawRectangle, mouseRectFangle);
                        }
                        if(isTouchRightMouse){
                            canvas.drawBitmap(imgContextMenu,
                                    imgCursor.getX(),
                                    imgCursor.getY(),
                                    null);
                        }

                        /*img.setVisibility(GONE);
                        canvasPaint.setColor(Color.RED);
                        RectFWheelMouse = new RectF(
                                img.getX() + img.getWidth()/3,
                                img.getY() - img.getHeight()/5,
                                img.getX() + 2*img.getWidth()/3,
                                img.getY() + 1*img.getHeight()/5
                         );
                        canvas.drawRect(RectFWheelMouse, canvasPaint);

                        canvasPaint.setColor(Color.BLUE);
                        RectFRightMouse = new RectF(
                                img.getX() + 2*img.getWidth()/3,
                                img.getY() - img.getHeight()/5,
                                img.getX() + img.getWidth(),
                                img.getY() + img.getHeight()/5
                        );
                        canvas.drawRect(RectFRightMouse, canvasPaint);

                        canvasPaint.setColor(Color.YELLOW);
                        RectFLeftMouse = new RectF(
                                img.getX(),
                                img.getY() - img.getHeight()/5,
                                img.getX() + img.getWidth()/3,
                                img.getY() + img.getHeight()/5
                        );
                        canvas.drawRect(RectFLeftMouse, canvasPaint);*/
                    }
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_TAPEMEASURE){

                }
            }
        }
    }

    public void setZoomPos(Bitmap bitmap, float x, float y,
                           boolean zooming, boolean isZoomCamera, boolean isZoomCameraOut, boolean isMagnify) {
        Log.v("TEST","setZoomPos: " + "zooming: " + zooming +
                " isZoomCamera: " + isZoomCamera + " isZoomCameraOut: " +
                isZoomCameraOut + " isMagnify: " + isMagnify);
        this.zoomPos.x = x;
        this.zoomPos.y = y;
        this.zooming = zooming;
        this.isZoomCamera = isZoomCamera;
        this.isZoomCameraOut = isZoomCameraOut;
        this.isMagnify = isMagnify;
    }

    private void magnifyImage(Bitmap bitmap, float x, float y) {
        Bitmap bm = getDrawingCache();
        shader = new BitmapShader(bm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        paintMagnify = new Paint();
        paintMagnify.setShader(shader);
        matrix.reset();
        Log.v("TEST","getScale: " + getScaleX());
        if(isMagnify){
            matrix.postScale(2f, 2f, x, y);
        }else{
            matrix.postScale(currZoom, currZoom, x, y);
        }
        paintMagnify.getShader().setLocalMatrix(matrix);
    }

    //register user touches as drawing action
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.v("TEST","event.getAction: " + event.getAction());
        int pointCount = event.getPointerCount();
        int action = (event.getAction() & MotionEvent.ACTION_MASK);
        //respond to down, move and up events

        for (int i = 0; i < pointCount; i++) {
            int id = event.getPointerId(i);

            //Ignore pointer higher than our max.
            if (id < MAX_NUMBER_OF_POINT) {
                x[id] = (int) event.getX(i);
                y[id] = (int) event.getY(i);

                if ((action == MotionEvent.ACTION_DOWN)
                        || (action == MotionEvent.ACTION_POINTER_DOWN)
                        || (action == MotionEvent.ACTION_MOVE)) {

                    touching[id] = true;
                } else {
                    touching[id] = false;
                }
            }
        }
        Log.v("TEST","number touch: " + numberTouchPoint());

        centerPoint = calculateCenterPointOfMultiplePoints();
        if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN ||
                tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER ||
                tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER ||
                tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY ||
                tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA)
        {
            newBitmap = Bitmap.createScaledBitmap(bitmap,
                    (int)(bitmap.getWidth()/getScaleX()),
                    (int)(bitmap.getHeight()/getScaleX()),
                    false);
        }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
            if(isTouchLeftMouse){
                newBitmap = Bitmap.createScaledBitmap(bitmapMouseLeft,
                        (int)(bitmapMouseLeft.getWidth()/getScaleX()),
                        (int)(bitmapMouseLeft.getHeight()/getScaleX()),
                        false);
            }else if(isTouchRightMouse){
                newBitmap = Bitmap.createScaledBitmap(bitmapMouseRight,
                        (int)(bitmapMouseRight.getWidth()/getScaleX()),
                        (int)(bitmapMouseRight.getHeight()/getScaleX()),
                        false);
            }else{
                newBitmap = Bitmap.createScaledBitmap(bitmap,
                        (int)(bitmap.getWidth()/getScaleX()),
                        (int)(bitmap.getHeight()/getScaleX()),
                        false);
            }
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.v("TEST","MotionEvent.ACTION_DOWN");
                PointF center = calculateCenterPointOfMultiplePoints();
                lastPoint = new PointF(event.getX(), event.getY());
                buildDrawingCache();
                if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN){
                    setErase(false);
                    drawPaint.setStrokeWidth(15f);
                    img.setVisibility(VISIBLE);
                    img.setX(centerPoint.x - img.getWidth()/2);
                    img.setY(centerPoint.y - img.getHeight());
                    startDrawing(img.getX(), img.getY() + img.getHeight());
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER){
                    setErase(true);
                    circlePaint.setStrokeWidth(50f);
                    startDrawing(centerPoint.x, centerPoint.y);
                    img.setVisibility(VISIBLE);
                    img.setX(centerPoint.x - img.getWidth()/offset);
                    img.setY(centerPoint.y - img.getHeight()/offset);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER){
                    setErase(true);
                    circlePaint.setStrokeWidth(130f);
                    startDrawing(centerPoint.x, centerPoint.y);
                    img.setVisibility(VISIBLE);
                    img.setX(centerPoint.x - img.getWidth()/offset);
                    img.setY(centerPoint.y - img.getHeight()/offset);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY){
                    img.setVisibility(VISIBLE);
                    img.setX(centerPoint.x - img.getWidth()/offset);
                    img.setY(centerPoint.y - img.getHeight()/offset);
//                    setZoomPos(centerPoint.x-newBitmap.getWidth()/ offset, centerPoint.y -newBitmap.getHeight()/ offset, true, false);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
                    img.setVisibility(VISIBLE);
                    img.setX(centerPoint.x - img.getWidth()/offset);
                    img.setY(centerPoint.y - img.getHeight()/offset);
//                    actionOnCamera();
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
                    img.setVisibility(VISIBLE);
                    img.setX(centerPoint.x - img.getWidth()/offset);
                    img.setY(centerPoint.y - img.getHeight()/offset);
                    actionOnMouse(event);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_TAPEMEASURE){
                    if(numberTouchPoint() >= 2){
                        img.setVisibility(VISIBLE);
                        img.setX((x[0]+x[1])/2 - img.getWidth()/offset);
                        img.setY((y[0]+y[1])/2 - img.getHeight()/offset);

                        if(numberTouchPoint() == 4){
                            int distance = (int)calculateDistanceBetween2Points(
                                    new PointF((x[0]+x[1])/2, (y[0]+y[1])/2),
                                    new PointF((x[2]+x[3])/2, (y[3]+y[2])/2));
                            imgTape.setVisibility(VISIBLE);
//                            imgTape.setLayoutParams(new LayoutParams(distance-100, bitmapTape.getHeight()));
                            imgTape.setX((x[0]+x[1])/2 + img.getWidth()/offset  - 100);
                            imgTape.setY((y[0]+y[1])/2 - bitmapTape.getHeight()/offset);
                            imgTape.invalidate();
                        }else{
                            imgTape.setVisibility(GONE);
                        }
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                Log.v("TEST","MotionEvent.ACTION_MOVE");
                if(numberTouchPoint() <= 2){
                    double distance = calculateDistanceBetween2Points(lastPoint, new PointF(event.getX(), event.getY()));
                    Log.v("TEST","MotionEvent.ACTION_MOVE : " + distance);
                    /*if(lastDistance < distance ){
                        listener.onZoomImageWhenWheeling(false);
                    }else if(lastDistance > distance){
                        listener.onZoomImageWhenWheeling(true);
                    }
                    lastDistance = distance;
                    return super.onTouchEvent(event);*/
                }else{
                    if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN){
                        img.setVisibility(VISIBLE);
                        img.setX(centerPoint.x - img.getWidth()/2);
                        img.setY(centerPoint.y - img.getHeight());
                        setErase(false);
                        drawPaint.setStrokeWidth(15f);
                        doDrawing(img.getX(), img.getY() + img.getHeight());
                    }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER){
                        img.setVisibility(VISIBLE);
                        img.setX(centerPoint.x - img.getWidth()/offset);
                        img.setY(centerPoint.y - img.getHeight()/offset);
                        setErase(true);
                        circlePaint.setStrokeWidth(50f);
                        doDrawing(centerPoint.x, centerPoint.y);
                    }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER){
                        img.setVisibility(VISIBLE);
                        img.setX(centerPoint.x - img.getWidth()/offset);
                        img.setY(centerPoint.y - img.getHeight()/offset);
                        setErase(true);
                        circlePaint.setStrokeWidth(130f);
                        doDrawing(centerPoint.x, centerPoint.y);
                    }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY){
                        img.setVisibility(VISIBLE);
                        img.setX(centerPoint.x - img.getWidth()/offset);
                        img.setY(centerPoint.y - img.getHeight()/offset);
                        listener.onMagnifyView(
                                centerPoint.x-newBitmap.getWidth()/ offset,
                                centerPoint.y -newBitmap.getHeight()/ offset,
                                true,
                                false,
                                false,
                                true);
                    }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
                        img.setVisibility(VISIBLE);
                        img.setX(centerPoint.x - img.getWidth()/offset);
                        img.setY(centerPoint.y - img.getHeight()/offset);

                        imgCaptured.setX(img.getX() + img.getWidth()/17);
                        imgCaptured.setY(img.getY() + 2*img.getHeight()/20);
                        actionOnCamera();
                    }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
                        img.setVisibility(VISIBLE);
                        img.setX(centerPoint.x - img.getWidth()/offset);
                        img.setY(centerPoint.y - img.getHeight()/offset);

                        imgCursor.setVisibility(VISIBLE);
                        imgCursor.setX(centerPoint.x - img.getWidth());
                        imgCursor.setY(centerPoint.y - img.getHeight() + 200);
                        actionOnMouse(event);
                    }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_TAPEMEASURE){
                        img.setVisibility(VISIBLE);
                        if(numberTouchPoint() >= 2){
                            img.setVisibility(VISIBLE);
                            img.setX((x[0]+x[1])/2 - img.getWidth()/offset);
                            img.setY((y[0]+y[1])/2 - img.getHeight()/offset);

                            if(numberTouchPoint() == 4){
                                imgTape.setVisibility(VISIBLE);
                                int distance = (int)calculateDistanceBetween2Points(
                                        new PointF((x[0]+x[1])/2, (y[0]+y[1])/2),
                                        new PointF((x[2]+x[3])/2, (y[3]+y[2])/2));

                                Log.v("TEST","tape distance: " + distance);
                                Log.v("TEST","tape bitmapTape.getWidth(): " + bitmapTape.getWidth());
                                Log.v("TEST","tape measure y 1: " + (y[0]+y[1])/2);
                                Log.v("TEST","tape measure y 2: " + (y[2]+y[3])/2);

                                if((y[0]+y[1])/2 - (y[2]+y[3])/2 > 0 && Math.abs((y[0]+y[1])/2 - (y[2]+y[3])/2) > 200){
                                    Log.v("TEST","tape measure up");
                                    imgTape.setRotation(-90);
                                    img.setRotation(-90);

                                    if(Math.abs((x[0]+x[1])/2 - (x[2]+x[3])/2) < 50){
                                        ClipDrawable mImageDrawable = (ClipDrawable) imgTape.getBackground();
                                        if(distance < 300){
                                            mImageDrawable.setLevel(2000);
                                        }else{
                                            mImageDrawable.setLevel(distance*10);
                                        }


                                        if(distance < bitmapTape.getWidth() - bitmapTape.getWidth()/3){
                                            imgTape.setX((x[0]+x[1])/2 - bitmapTape.getWidth()/offset + 20);
                                            imgTape.setY((y[2]+y[3])/2 - distance/2 + bitmapTape.getWidth()/2);
                                            imgTape.invalidate();
                                        }else{
                                            imgTape.setVisibility(GONE);
                                        }
                                    }else{
                                        imgTape.setVisibility(GONE);
                                    }
                                }else if(Math.abs((y[0]+y[1])/2 - (y[2]+y[3])/2) < 200){

                                    Log.v("TEST","tape measure normal");
                                    img.setRotation(0);
                                    imgTape.setRotation(0);
                                    if(Math.abs((y[0]+y[1])/2 - (y[2]+y[3])/2) < 50){
                                        ClipDrawable mImageDrawable = (ClipDrawable) imgTape.getBackground();
                                        if(distance < 300){
                                            mImageDrawable.setLevel(2000);

                                        }else{
                                            mImageDrawable.setLevel(distance*10);
                                        }

                                        if(distance < bitmapTape.getWidth() - bitmapTape.getWidth()/3){
                                            imgTape.setX((x[3]+x[2])/2 - bitmapTape.getWidth() + distance/2);
                                            imgTape.setY((y[0]+y[1])/2 - bitmapTape.getHeight()/offset);
                                            imgTape.invalidate();
                                        }else{
                                            imgTape.setVisibility(GONE);
                                        }

                                    }else{
                                        imgTape.setVisibility(GONE);
                                    }
                                }else{
                                    Log.v("TEST","tape measure down");
                                    imgTape.setRotation(90);
                                    img.setRotation(90);

                                    if(Math.abs((x[0]+x[1])/2 - (x[2]+x[3])/2) < 50){
                                        ClipDrawable mImageDrawable = (ClipDrawable) imgTape.getBackground();
                                        if(distance < 300){
                                            mImageDrawable.setLevel(2000);
                                        }else{
                                            mImageDrawable.setLevel(distance*10);
                                        }

                                        if(distance < bitmapTape.getWidth() - bitmapTape.getWidth()/3){
                                            imgTape.setX((x[0]+x[1])/2 - bitmapTape.getWidth()/offset + 20);
                                            imgTape.setY((y[2]+y[3])/2 + distance/2 - bitmapTape.getWidth()/2);
                                            imgTape.invalidate();
                                        }else{
                                            imgTape.setVisibility(GONE);
                                        }
                                    }else{
                                        imgTape.setVisibility(GONE);
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN){
                    img.setVisibility(GONE);
                    setErase(false);
                    drawPaint.setStrokeWidth(15f);
                    stopDrawing(img.getX(), img.getY() + img.getHeight());
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER){
                    img.setVisibility(GONE);
                    setErase(true);
                    circlePaint.setStrokeWidth(50f);
                    stopDrawing(centerPoint.x, centerPoint.y);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER){
                    img.setVisibility(GONE);
                    setErase(true);
                    circlePaint.setStrokeWidth(130f);
                    stopDrawing(centerPoint.x, centerPoint.y);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY){
                    img.setVisibility(GONE);
                    listener.onMagnifyView(0, 0, false, false,false, false);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
                    img.setVisibility(GONE);
                    isZoomCamera = false;
                    isZoomCameraOut = false;
                    isCaptureCamera = false;
                    paintCaptured = null;
                    imgCaptured.setVisibility(GONE);
                    listener.onMagnifyView(0, 0, false, false,false, false);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
                    img.setVisibility(GONE);
                    imgCursor.setVisibility(GONE);
                    isTouchRightMouse = false;
                    isTouchLeftMouse = false;
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_TAPEMEASURE){
                    img.setVisibility(GONE);
                    imgTape.setVisibility(GONE);
                }
                Log.v("TEST","MotionEvent.ACTION_UP");

                break;
            default:
                return false;
        }
        //redraw
        invalidate();
        return true;
    }

    private void actionOnCamera() {
        RectFTouchCapture = new RectF(
                img.getX() + img.getWidth()/2,
                img.getY() - img.getHeight()/5,
                img.getX() + img.getWidth(),
                img.getY() + img.getHeight()/5
        );

        RectFZoomAreaCamera = new RectF(
                img.getX() + img.getWidth()/2,
                img.getY() + img.getHeight()/5,
                img.getX() + 3*img.getWidth()/4,
                img.getY() + img.getHeight()
        );

        RectFZoomAreaCameraOut = new RectF(
                img.getX() + 3*img.getWidth()/4,
                img.getY() + img.getHeight()/5,
                img.getX() + img.getWidth(),
                img.getY() + img.getHeight()
        );

//        canvasPaint.setColor(Color.BLUE);
        Log.v("TEST","touch RectFTouchCapture " + detectTouchOnRectFangleView(x[4], y[4], RectFTouchCapture));
        Log.v("TEST","touch RectFZoomAreaCamera" + detectTouchOnRectFangleView(x[4], y[4], RectFZoomAreaCamera));
        Log.v("TEST","touch RectFZoomAreaCameraOut" + detectTouchOnRectFangleView(x[4], y[4], RectFZoomAreaCameraOut));

        if(detectTouchOnRectFangleView(x[4], y[4], RectFZoomAreaCamera)) {
            isZoomCamera = true;
            isZoomCameraOut = false;
            paintCaptured = null;
        }

        if(detectTouchOnRectFangleView(x[4], y[4], RectFZoomAreaCameraOut)) {
            isZoomCamera = false;
            isZoomCameraOut = true;
            paintCaptured = null;
        }

        if(touching[4] && detectTouchOnRectFangleView(x[4], y[4], RectFTouchCapture)){
            if(!isCaptureCamera){
                isCaptureCamera = true;
                listener.onCapture(img.getX(), img.getY());
            }
        }

        if(isZoomCamera){
            Log.v("TEST","zoomcamera on in currZoom: " + currZoom);
            if(numberTouchPoint() == 5){
                if(currZoom < 2f){
                    currZoom = currZoom + 0.003f;
                }
            }
        }else if(isZoomCameraOut){
            Log.v("TEST","zoomcamera on out currZoom: " + currZoom);
            if(numberTouchPoint() == 5){
                if(currZoom > 0.3f){
                    currZoom = currZoom - 0.003f;
                }
            }
        }

        /*setZoomPos(getDrawingCache(), centerPoint.x,
                centerPoint.y,
                true,
                true,
                false,
                false);*/
        listener.onMagnifyView(centerPoint.x,
                centerPoint.y,
                true,
                isZoomCamera,
                isZoomCameraOut,
                false);
    }

    public void createCapture(float x, float y) {
        /*Matrix matrix = new Matrix();
        if(isMagnify){
            matrix.postScale(2f, 2f, x, y);
        }else{
            matrix.postScale(currZoom, currZoom, x, y);
        }*/
        paintCaptured = Bitmap.createBitmap(getDrawingCache(),
                (int)(x + img.getWidth()/17),
                (int)(y + 2*img.getHeight()/20),
                2*img.getWidth()/3,
                18*img.getHeight()/20);
        imgCaptured.setVisibility(VISIBLE);
        imgCaptured.setImageBitmap(paintCaptured);
    }

    private void actionOnMouse(MotionEvent event) {
        if(numberTouchPoint() >= 4){
            RectFWheelMouse = new RectF(
                    img.getX() + img.getWidth()/3,
                    img.getY() - img.getHeight()/5,
                    img.getX() + 2*img.getWidth()/3,
                    img.getY() + 1*img.getHeight()/5
            );

            RectFRightMouse = new RectF(
                    img.getX() + 2*img.getWidth()/3,
                    img.getY() - img.getHeight()/5,
                    img.getX() + img.getWidth(),
                    img.getY() + img.getHeight()/5
            );

            RectFLeftMouse = new RectF(
                    img.getX(),
                    img.getY() - img.getHeight()/5,
                    img.getX() + img.getWidth()/3,
                    img.getY() + img.getHeight()/5
            );

            for(int i =0; i < numberTouchPoint(); i++){
                isTouchRightMouse = detectTouchOnRectFangleView(x[i], y[i], RectFRightMouse);
                if(isTouchRightMouse){
                    break;
                }
            }
            for(int i =0; i < numberTouchPoint(); i++){
                isTouchLeftMouse = detectTouchOnRectFangleView(x[i], y[i], RectFLeftMouse);
                if(isTouchLeftMouse){
                    if(lastMousePos.x == 0 && lastMousePos.y ==0){
                        lastMousePos = new PointF(imgCursor.getX(), imgCursor.getY());
                    }
                    break;
                }else{
//                    lastMousePos = new PointF(0,0);
                }
            }

            if(isTouchLeftMouse){
                img.setBackgroundResource(R.drawable.mouse_left);
                Log.v("TEST","drawing in mouse: " + lastMousePos.x + " y: " + lastMousePos.y);
                RectFdrawRectangle = new RectF(
                        lastMousePos.x,
                        lastMousePos.y,
                        imgCursor.getX() + 100,
                        imgCursor.getY() + 100
                );
            }else if(isTouchRightMouse){
                img.setBackgroundResource(R.drawable.mouse_right);
            }else{
                lastMousePos = new PointF(0,0);
                img.setBackgroundResource(R.drawable.mouse);
            }

            for(int i =0; i < numberTouchPoint(); i++){
                if(detectTouchOnRectFangleView(x[i], y[i], RectFWheelMouse)){
                    if(lastYWheel != 0){
                        if(Math.abs(lastYWheel-y[i]) > biasHieght/10){
                            if(lastYWheel > y[i]){
                                Log.v("TEST","RectFWheelMouse wheel up");
                                listener.onZoomImageWhenWheeling(false);
                            }else{
                                Log.v("TEST","RectFWheelMouse wheel down");
                                listener.onZoomImageWhenWheeling(true);
                            }
                            lastYWheel = y[i];
                        }
                    }else{
                        lastYWheel = y[i];
                    }
                }
            }

        }else{
            lastMousePos = new PointF(0,0);
            img.setBackgroundResource(R.drawable.mouse);
            isTouchLeftMouse = false;
            isTouchRightMouse = false;
        }
    }

    private void stopDrawing(float touchX, float touchY) {
        drawPath.lineTo(touchX, touchY);
        drawCanvas.drawPath(drawPath, drawPaint);
        circlePath.reset();
        drawPath.reset();
    }

    private void doDrawing(float touchX, float touchY) {
        if(erase) {
            float dx = Math.abs(touchX - mX);
            float dy = Math.abs(touchY - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (touchX + mX)/2, (touchY + mY)/2);
                mX = touchX;
                mY = touchY;
                circlePath.reset();
                if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER){
                    circlePath.addCircle(mX, mY, 50, Path.Direction.CW);
                }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER){
                    circlePath.addCircle(mX, mY, 130, Path.Direction.CW);
                }
            }
            drawPaint.setStyle(Paint.Style.FILL);
            drawPaint.setColor(Color.RED);
            if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER){
                drawCanvas.drawCircle(touchX, touchY, 50, drawPaint);
            }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER){
                drawCanvas.drawCircle(touchX, touchY, 130, drawPaint);
            }
        } else {
            drawPath.lineTo(touchX, touchY);
        }
    }

    private void startDrawing(float touchX, float touchY) {
        if(erase) {
            mPath.reset();
            mPath.moveTo(touchX, touchY);
            mX = touchX;
            mY = touchY;
            drawPaint.setStyle(Paint.Style.FILL);
            drawPaint.setColor(Color.RED);
            drawCanvas.drawCircle(touchX, touchY, 15, drawPaint);
        } else {
            drawPath.moveTo(touchX, touchY);
        }
    }

    //set erase true or false
    public void setErase(boolean isErase){
        erase=isErase;
        if(erase) {
            //      drawPaint.setStyle(Paint.Style.FILL);
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setColor(Color.RED);
            drawPaint.setXfermode(null);
        }
    }

    public void refreshDrawable(MainActivity.TOOL_KIND tool_kind){
        if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER){
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pinkeraser);
            img.setBackgroundResource(R.drawable.pinkeraser);
        }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER){
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dryerase);
            img.setBackgroundResource(R.drawable.dryerase);
        }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN){
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pen);
            img.setBackgroundResource(R.drawable.pen);
        }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY){
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.magglass);
            img.setBackgroundResource(R.drawable.magglass);
        }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.camera);
            img.setBackgroundResource(R.drawable.camera);
        }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mouse);
            img.setBackgroundResource(R.drawable.mouse);
        }else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_TAPEMEASURE){
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tapemeasure1);
            img.setBackgroundResource(R.drawable.tapemeasure1);
        }
        setScaleX(1f);
        setScaleY(1f);
        img.setRotation(0);
    }

    public void setTool_kind(MainActivity.TOOL_KIND tool_kind) {
        this.tool_kind = tool_kind;
    }

    private boolean detectTouchOnRectFangleView(float x, float y, RectF rectF){
        if(rectF.contains(x,y)){
            return true;
        }

        return false;
    }

    private double calculateDistanceBetween2Points(PointF start, PointF end){
        return Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
    }

    public PointF calculateCenterPointOfMultiplePoints() {
        float minY = y[0];
        float minX = x[0];
        float maxY = y[0];
        float maxX = x[0];

        for(int i = 0; i < MAX_NUMBER_OF_POINT; i++){
            if(touching[i]){
                if (minY > y[i]) {
                    minY = y[i];
                }
                if (maxY < y[i]) {
                    maxY = y[i];
                }
                if (minX > x[i]) {
                    minX = x[i];
                }
                if (maxX < x[i]) {
                    maxX = x[i];
                }
            }
        }

        return new PointF((minX+maxX)/2, (minY+maxY)/2);
    }

    public int numberTouchPoint(){
        int sum =0;
        for(int i = 0; i < MAX_NUMBER_OF_POINT; i++){
            if(touching[i]){
                sum++;
            }
        }
        return sum;
    }
}
