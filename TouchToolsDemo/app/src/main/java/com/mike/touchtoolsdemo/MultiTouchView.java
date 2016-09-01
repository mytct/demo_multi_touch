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
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class MultiTouchView extends View {
	private Paint paint;
	final int MAX_NUMBER_OF_POINT = 10;
	float[] x = new float[MAX_NUMBER_OF_POINT];
	float[] y = new float[MAX_NUMBER_OF_POINT];
	boolean[] touching = new boolean[MAX_NUMBER_OF_POINT];
	private Path m_Path;
	private ArrayList<Pair<Path, Paint>> paths = new ArrayList<Pair<Path, Paint>>();
	private Canvas m_Canvas;
	private float mX, mY;  private static final float TOUCH_TOLERANCE = 4;
	private Bitmap bitmap;
	private MainActivity.TOOL_KIND tool_kind = MainActivity.TOOL_KIND.TOOL_KIND_PEN;
	//	private Bitmap bg;
	private MultiTouchListener listener;
	private float offset = 2f;
	private float biasPercent = 0.5f;
	private int biasHieght = 200;
	private PointF lastPoint = new PointF(0,0);
	private Rect rectTouchCapture;
	private Rect rectLeftMouse;
	private Rect rectRightMouse;
	private Rect rectWheelMouse;
	private Rect rectZoomAreaCamera;
	private boolean isTouchLeftMouse = false;
	private boolean isTouchRightMouse = false;
	private Bitmap bitmapMouseLeft;
	private Bitmap bitmapMouseRight;
	private boolean isZoomCamera;
	private Bitmap paintCaptured;
	private boolean isCaptureCamera;
	private float lastYWheel = 0;
	private double distance2Point =0 ;
	private BitmapShader shader;
	private Matrix matrix;
	private PointF zoomPos;
	private boolean zooming = false;
	private final int sizeMaglass = 170;
	private final int offsetDrag = 100;
	private Bitmap bg;
	private Paint paintMagnify;
	private long timeDown = 0;
	private long maxDown = 200;
	private boolean isMulti = false;

	public Bitmap getPaintCaptured() {
		return paintCaptured;
	}

	public void setPaintCaptured(Bitmap paintCaptured) {
		this.paintCaptured = paintCaptured;
	}

	public interface MultiTouchListener{
		void onMagnifyImageListener(boolean magnifyMode, float x, float y, boolean isZoomCamera);
		void onFinishDrawing(Bitmap bitmap) throws Exception;
		void onZoomImageWhenWheeling(boolean isZoomOut);
		void onResetRealSizeImage();
		void onDragImage(float x, float y);
	}

	public MainActivity.TOOL_KIND getTool_kind() {
		return tool_kind;
	}

	public void setTool_kind(MainActivity.TOOL_KIND tool_kind) {
		this.tool_kind = tool_kind;
	}

	public MultiTouchView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public MultiTouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MultiTouchView(Context context) {
		super(context);
		init();
	}

	void init() {
		setDrawingCacheEnabled(true);
		zoomPos = new PointF(0, 0);
		matrix = new Matrix();
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(30);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
		m_Path = new Path();
		m_Canvas = new Canvas();
		Paint newPaint = new Paint(paint);
		paths.add(new Pair<Path, Paint>(m_Path, newPaint));
		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pen);
		listener = (MultiTouchListener)getContext();
		bitmapMouseLeft = BitmapFactory.decodeResource(getResources(), R.drawable.mouse_left);
		bitmapMouseRight = BitmapFactory.decodeResource(getResources(), R.drawable.mouse_right);
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	private void createCapture(float x, float y) {
		Matrix matrix = new Matrix();
		matrix.postScale(2f, 2f, x, y);
		Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,
				(int)(bitmap.getWidth()/getScaleX()),
				(int)(bitmap.getHeight()/getScaleX()),
				false);
		paintCaptured = Bitmap.createBitmap(bg,
				(int)x - newBitmap.getWidth()/2,
				(int)y - newBitmap.getHeight()/2,
				newBitmap.getWidth()/3,
				newBitmap.getHeight()/3,
				matrix, false);
	}

	public void drawBitmaptAt(Canvas canvas, float x, float y){
		/**
		 * drawing path pencil/eraser
		 */
		for (Pair<Path, Paint> p : paths) {
			canvas.drawPath(p.first, p.second);
		}

		/**
		 * drawing magnify
		 */
		if (!zooming) {
			Log.v("TEST","do not draw magnify canvas");
//			buildDrawingCache();
		} else {
			Log.v("TEST","start draw magnify canvas");
			magnifyImage(zoomPos.x,
					zoomPos.y);
			Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,
					(int)(bitmap.getWidth()/getScaleX()),
					(int)(bitmap.getHeight()/getScaleX()),
					false);
			if(!isZoomCamera){
				Log.v("TEST","draw magnify glass in magnify glass");
				canvas.drawCircle(zoomPos.x + newBitmap.getWidth()/2,
						zoomPos.y + newBitmap.getHeight()/4,
						sizeMaglass/getScaleX(), paintMagnify);
			}else{
				Log.v("TEST","draw magnify glass in camera");
				canvas.drawRect(zoomPos.x,
						zoomPos.y + newBitmap.getHeight(),
						zoomPos.x + (int)(newBitmap.getWidth()/(offset -0.2)),
						zoomPos.y + newBitmap.getHeight()/5, paintMagnify);
			}
		}
		bg = getDrawingCache();

		PointF centerPoint = calculateCenterPointOfMultiplePoints();
		if(centerPoint != null && centerPoint.x != 0 && centerPoint.y != 0){
			if(numberTouchPoint() >= 3){
				lastPoint.x = centerPoint.x-bitmap.getWidth()/ offset;
				lastPoint.y = centerPoint.y -bitmap.getHeight()/ offset;

				if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN ||
						tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER ||
						tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER ||
						tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY){
					Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,
							(int)(bitmap.getWidth()/getScaleX()),
							(int)(bitmap.getHeight()/getScaleX()),
							false);
					canvas.drawBitmap(newBitmap,
							centerPoint.x -newBitmap.getWidth()/ offset,
							centerPoint.y -newBitmap.getHeight()/ offset, null);
				}
				else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
					if(numberTouchPoint() >= 4){
						if(isCaptureCamera){
							if(paintCaptured != null){
								Log.v("TEST","draw paint");
								canvas.drawBitmap(paintCaptured,
										centerPoint.x -bitmap.getWidth()/ offset,
										centerPoint.y -bitmap.getHeight()/ offset + biasHieght/4, null);
							}
						}
						if(isZoomCamera){
							Log.v("TEST","zoomcamera on");
							listener.onMagnifyImageListener(true,
									centerPoint.x - bitmap.getWidth()/2,
									centerPoint.y - bitmap.getHeight()/2, true);
						}

						Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,
								(int)(bitmap.getWidth()/getScaleX()),
								(int)(bitmap.getHeight()/getScaleX()),
								false);
						canvas.drawBitmap(newBitmap,
								centerPoint.x - newBitmap.getWidth()/ offset,
								centerPoint.y - newBitmap.getHeight()/ offset, null);
					}
				}
				else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
					if(numberTouchPoint() >= 4){
						if(isTouchLeftMouse){
							Bitmap newBitmap = Bitmap.createScaledBitmap(bitmapMouseLeft,
									(int)(bitmapMouseLeft.getWidth()/getScaleX()),
									(int)(bitmapMouseLeft.getHeight()/getScaleX()),
									false);
							canvas.drawBitmap(newBitmap,
									centerPoint.x - newBitmap.getWidth()/ offset,
									centerPoint.y - newBitmap.getHeight()/(offset - biasPercent), null);
						}else if(isTouchRightMouse){
							Bitmap newBitmap = Bitmap.createScaledBitmap(bitmapMouseRight,
									(int)(bitmapMouseRight.getWidth()/getScaleX()),
									(int)(bitmapMouseRight.getHeight()/getScaleX()),
									false);
							canvas.drawBitmap(newBitmap,
									centerPoint.x - newBitmap.getWidth()/ offset,
									centerPoint.y - newBitmap.getHeight()/(offset - biasPercent), null);
						}else{
							Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,
									(int)(bitmap.getWidth()/getScaleX()),
									(int)(bitmap.getHeight()/getScaleX()),
									false);
							canvas.drawBitmap(newBitmap,
									centerPoint.x - newBitmap.getWidth()/ offset,
									centerPoint.y - newBitmap.getHeight()/(offset - biasPercent), null);
						}
					}
				}
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
//		canvas.drawBitmap(bg, 0, 0, paintBg);
//		canvas.drawBitmap(bg, 0, 0, null);
		drawBitmaptAt(canvas, 0,0);

		//drawing touching points
		/*for(int i = 0; i < MAX_NUMBER_OF_POINT; i++) {
			switch (i) {
				case 0:
					paint.setColor(Color.BLACK);
					break;
				case 1:
					paint.setColor(Color.RED);
					break;
				case 2:
					paint.setColor(Color.GREEN);
					break;
				case 3:
					paint.setColor(Color.YELLOW);
					break;
				case 4:
					paint.setColor(Color.GRAY);
					break;
				case 5:
					paint.setColor(Color.BLUE);
					break;
			}
			if (touching[i]) {
				paint.setStrokeWidth(10);
				canvas.drawCircle(x[i], y[i], 50f, paint); //draw each point
			}
		}

		PointF center = calculateCenterPointOfMultiplePoints();
		paint.setColor(Color.MAGENTA);
		canvas.drawCircle(center.x , center.y, 50f, paint); //draw each point*/

		/*rectTouchCapture = new Rect((int)lastPoint.x + bitmap.getWidth()/2,
				(int)lastPoint.y - bitmap.getHeight()/4,
				(int)(lastPoint.x + bitmap.getWidth()),
				(int)(lastPoint.y + bitmap.getHeight()/ 3));

		rectZoomAreaCamera = new Rect((int)lastPoint.x + bitmap.getWidth()/2,
				(int)lastPoint.y + bitmap.getWidth()/4,
				(int)(lastPoint.x + bitmap.getWidth()),
				(int)(lastPoint.y + bitmap.getHeight()));

		paint.setColor(Color.MAGENTA);
		canvas.drawRect(rectTouchCapture, paint);
		canvas.drawRect(rectZoomAreaCamera, paint);*/
		/*Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,
				(int)(bitmap.getWidth()/getScaleX()),
				(int)(bitmap.getHeight()/getScaleX()),
				false);
		rectTouchCapture = new Rect((int)lastPoint.x + newBitmap.getWidth()/2,
				(int)lastPoint.y - newBitmap.getHeight()/4,
				(int)(lastPoint.x + newBitmap.getWidth()),
				(int)(lastPoint.y + newBitmap.getHeight()/ 3));

		rectZoomAreaCamera = new Rect((int)lastPoint.x + newBitmap.getWidth()/2,
				(int)lastPoint.y + newBitmap.getWidth()/4,
				(int)(lastPoint.x + newBitmap.getWidth()),
				(int)(lastPoint.y + newBitmap.getHeight()));
		paint.setColor(Color.MAGENTA);
		canvas.drawRect(rectTouchCapture, paint);
		canvas.drawRect(rectZoomAreaCamera, paint);*/
	}

	public void setZoomPos(float x, float y, boolean zooming, boolean isZoomCamera) {
		Log.v("TEST","setZoomPos: " + "zooming: " + zooming + " isZoomCamera: " + isZoomCamera);
		this.zoomPos.x = x;
		this.zoomPos.y = y;
		this.zooming = zooming;
		this.isZoomCamera = isZoomCamera;
	}


	private void magnifyImage(float x, float y) {
		Bitmap bitmap = getDrawingCache();
		shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

		paintMagnify = new Paint();
		paintMagnify.setShader(shader);
		matrix.reset();
		Log.v("TEST","getScale: " + getScaleX());
		matrix.postScale(2f, 2f, x, y);
		paintMagnify.getShader().setLocalMatrix(matrix);
	}

	public void refreshDrawable(MainActivity.TOOL_KIND tool_kind){
		if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER){
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pinkeraser);
		}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER){
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dryerase);
		}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN){
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pen);
		}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY){
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.magglass);
		}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.camera);
		}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mouse);
		}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_TAPEMEASURE){
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tapemeasure1);
		}
		isZoomCamera = false;
		isCaptureCamera = false;
		paintCaptured = null;
		invalidate();
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

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
				MeasureSpec.getSize(heightMeasureSpec));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = (event.getAction() & MotionEvent.ACTION_MASK);
		int pointCount = event.getPointerCount();

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

		if(event.getAction() == MotionEvent.ACTION_UP){
			timeDown = 0;
			touch_up();
			resetGestureMode();
			isMulti = false;
		}

		detectModeGesture(event, action);
		invalidate();
		return true;

	}

	private void detectModeGesture(MotionEvent event, int action) {
		Log.v("TEST","detectModeGesture: " + numberTouchPoint());
		if(numberTouchPoint() >= 3){
			isMulti = true;
//			int nearest = findNearestPoint();

			Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,
					(int)(bitmap.getWidth()/getScaleX()),
					(int)(bitmap.getHeight()/getScaleX()),
					false);
			PointF centerPoint = calculateCenterPointOfMultiplePoints();
			if(centerPoint != null && centerPoint.x != 0 && centerPoint.y != 0){
				if (tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN ||
						tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER ||
						tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER) {
					Log.v("TEST","event.getAction(): " + event.getAction());
					if(event.getAction() == MotionEvent.ACTION_POINTER_3_DOWN){
						touch_start(centerPoint.x-newBitmap.getWidth()/ offset, centerPoint.y+newBitmap.getHeight()/ offset);
					}
					if(action == MotionEvent.ACTION_MOVE){
						if (tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER) {
							touch_move(centerPoint.x, centerPoint.y);
						}if (tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER) {
							touch_move(centerPoint.x, centerPoint.y);
						}if (tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN) {
							touch_move(centerPoint.x-newBitmap.getWidth()/ offset, centerPoint.y+newBitmap.getHeight()/ offset);
						}

					}
					if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
						touch_up();
					}
				}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MAGNIFY){
					listener.onMagnifyImageListener(true, centerPoint.x-newBitmap.getWidth()/ offset, centerPoint.y -newBitmap.getHeight()/ offset, false);
				}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
					if(numberTouchPoint() > 4){
						rectTouchCapture = new Rect((int)lastPoint.x + newBitmap.getWidth()/2,
								(int)lastPoint.y - newBitmap.getHeight()/4,
								(int)(lastPoint.x + newBitmap.getWidth()),
								(int)(lastPoint.y + newBitmap.getHeight()/ 3));

						rectZoomAreaCamera = new Rect((int)lastPoint.x + newBitmap.getWidth()/2,
								(int)lastPoint.y + newBitmap.getWidth()/4,
								(int)(lastPoint.x + newBitmap.getWidth()),
								(int)(lastPoint.y + newBitmap.getHeight()));

						Log.v("TEST","touch rectTouchCapture " + detectTouchCaptureCamera(x[4], y[4], rectTouchCapture));
						Log.v("TEST","touch rectZoomAreaCamera" + detectTouchCaptureCamera(x[4], y[4], rectZoomAreaCamera));

						if(detectTouchCaptureCamera(x[4], y[4], rectZoomAreaCamera)) {
							isZoomCamera = true;
							isCaptureCamera = false;
						}

						if(detectTouchCaptureCamera(x[4], y[4], rectTouchCapture)){
							isZoomCamera = false;
							isCaptureCamera = true;

							createCapture(centerPoint.x,
									centerPoint.y);
						}
					}
				}else if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_MOUSE){
					if(numberTouchPoint() > 4){
						rectWheelMouse = new Rect((int)lastPoint.x + newBitmap.getWidth()/3,
								(int)lastPoint.y - biasHieght,
								(int)(lastPoint.x + (2*newBitmap.getWidth())/3),
								(int)(lastPoint.y + newBitmap.getHeight()/ 4));


						rectRightMouse = new Rect((int)lastPoint.x + (2*newBitmap.getWidth())/3,
								(int)lastPoint.y - biasHieght,
								(int)(lastPoint.x + newBitmap.getWidth()),
								(int)(lastPoint.y + newBitmap.getHeight()/ 4));

						rectLeftMouse = new Rect((int)lastPoint.x,
								(int)lastPoint.y - biasHieght,
								(int)(lastPoint.x + newBitmap.getWidth() / 3),
								(int)(lastPoint.y + newBitmap.getHeight()/ 4));

						if(detectTouchCaptureCamera(x[4], y[4], rectLeftMouse)){
							isTouchLeftMouse = detectTouchCaptureCamera(x[4], y[4], rectLeftMouse);
						}else if(detectTouchCaptureCamera(x[3], y[3], rectLeftMouse)){
							isTouchLeftMouse = detectTouchCaptureCamera(x[3], y[3], rectLeftMouse);
						}else if(detectTouchCaptureCamera(x[2], y[2], rectLeftMouse)){
							isTouchLeftMouse = detectTouchCaptureCamera(x[2], y[2], rectLeftMouse);
						}

						if(detectTouchCaptureCamera(x[4], y[4], rectRightMouse)){
							isTouchRightMouse = detectTouchCaptureCamera(x[4], y[4], rectRightMouse);
						}else if(detectTouchCaptureCamera(x[3], y[3], rectRightMouse)){
							isTouchRightMouse = detectTouchCaptureCamera(x[3], y[3], rectRightMouse);
						}else if(detectTouchCaptureCamera(x[2], y[2], rectRightMouse)){
							isTouchRightMouse = detectTouchCaptureCamera(x[2], y[2], rectRightMouse);
						}

//					Log.v("TEST","touch rectLeftMouse: " + detectTouchCaptureCamera(x[4], y[4], rectLeftMouse));
//					Log.v("TEST","touch rectRightMouse " + detectTouchCaptureCamera(x[4], y[4], rectRightMouse));
//					Log.v("TEST","touch rectWheelMouse " + detectTouchCaptureCamera(x[4], y[4], rectWheelMouse));

						if(detectTouchCaptureCamera(x[4], y[4], rectWheelMouse)){
							if(lastYWheel != 0){
								if(Math.abs(lastYWheel-y[4]) > 20){
									if(lastYWheel > y[4]){
										Log.v("TEST","rectWheelMouse wheel up");
										listener.onZoomImageWhenWheeling(false);
									}else{
										Log.v("TEST","rectWheelMouse wheel down");
										listener.onZoomImageWhenWheeling(true);
									}
									lastYWheel = y[4];
								}
							}else{
								lastYWheel = y[4];
							}
						}else if(detectTouchCaptureCamera(x[3], y[3], rectWheelMouse)){
							if(lastYWheel != 0){
								if(Math.abs(lastYWheel-y[3]) > 20){
									if(lastYWheel > y[3]){
										Log.v("TEST","rectWheelMouse wheel up");
										listener.onZoomImageWhenWheeling(false);
									}else{
										Log.v("TEST","rectWheelMouse wheel down");
										listener.onZoomImageWhenWheeling(true);
									}
									lastYWheel = y[3];
								}
							}else{
								lastYWheel = y[3];
							}
						}else if(detectTouchCaptureCamera(x[2], y[2], rectWheelMouse)){
							if(lastYWheel != 0){
								if(Math.abs(lastYWheel-y[2]) > 20){
									if(lastYWheel > y[2]){
										Log.v("TEST","rectWheelMouse wheel up");
										listener.onZoomImageWhenWheeling(false);
									}else{
										Log.v("TEST","rectWheelMouse wheel down");
										listener.onZoomImageWhenWheeling(true);
									}
									lastYWheel = y[2];
								}
							}else{
								lastYWheel = y[2];
							}
						}

					}else{
						isTouchLeftMouse = false;
						isTouchRightMouse = false;
					}
				}
			}

			if(numberTouchPoint() == 3){
				isZoomCamera = false;
			}

			if(tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_CAMERA){
				if(isZoomCamera){
					Log.v("TEST","zoomcamera on");
					listener.onMagnifyImageListener(true,
							centerPoint.x - newBitmap.getWidth()/2,
							centerPoint.y - newBitmap.getHeight()/2, true);
				}else{
					listener.onMagnifyImageListener(false, 0, 0, false);
				}
			}
		}else{
			if(numberTouchPoint() == 2){
				if(!isMulti){
					resetGestureMode();
					double dynamicDistance = calculateDistanceBetween2Points(new PointF(x[0], y[0]), new PointF(x[1], y[1]));
					timeDown = event.getEventTime() - event.getDownTime();
					if(timeDown > maxDown){
						if(distance2Point != 0 && distance2Point != dynamicDistance){
							if(distance2Point < dynamicDistance){
								Log.v("TEST","zoom out");
								listener.onZoomImageWhenWheeling(true);
							}else{
								Log.v("TEST","zoom in");
								listener.onZoomImageWhenWheeling(false);
							}
							distance2Point = dynamicDistance;
						}else{
							distance2Point = dynamicDistance;
						}
					}
				}
			}else if(numberTouchPoint() == 1){
				resetGestureMode();
				double dynamicDistance = calculateDistanceBetween2Points(new PointF(x[0], y[0]), lastPoint);
				if(dynamicDistance >= offsetDrag){
//					listener.onDragImage(x[0], y[0]);
					/*TranslateAnimation animation =
							new TranslateAnimation(lastPoint.x, x[0], lastPoint.y, y[0]);
					animation.setFillAfter(true);
					animation.setDuration(300);
					animation.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {

						}

						@Override
						public void onAnimationEnd(Animation animation) {
							setTranslationX(x[0]);
							setTranslationY(y[0]);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {

						}
					});
					startAnimation(animation);*/
				}
			}
		}
	}

	private void resetGestureMode() {
		Log.v("TEST","resetGestureMode");
		touch_up();
		listener.onMagnifyImageListener(false, 0, 0, false);
		isTouchLeftMouse = false;
		isTouchRightMouse = false;
//		isZoomCamera = false;
	}

	public void touch_start(float x, float y) {
		Log.v("TEST","touch_start x: " + x + " y: " + y);
		Log.v("TEST","touch_start mx: " + mX + " my: " + mY);
		if (tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_SMALLERASER) {
			paint.setStrokeWidth(100);
			paint.setColor(Color.TRANSPARENT);
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			Paint newPaint = new Paint(paint); // Clones the mPaint object
			paths.add(new Pair<Path, Paint>(m_Path, newPaint));
		}if (tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_WHITEBOARDERASER) {
			paint.setStrokeWidth(300);
			paint.setColor(Color.TRANSPARENT);
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			Paint newPaint = new Paint(paint); // Clones the mPaint object
			paths.add(new Pair<Path, Paint>(m_Path, newPaint));
		}if (tool_kind == MainActivity.TOOL_KIND.TOOL_KIND_PEN) {
			paint.setColor(Color.RED);
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
			paint.setStrokeWidth(30);
			Paint newPaint = new Paint(paint); // Clones the mPaint object
			paths.add(new Pair<Path, Paint>(m_Path, newPaint));
		}

		if(x != 0 && y !=0 ){
			m_Path.reset();
			m_Path.moveTo(x, y);
			mX = x;
			mY = y;
		}
	}

	public void touch_move(float x, float y) {
		Log.v("TEST","touch_move x: " + x + " y: " + y);
		Log.v("TEST","touch_move mx: " + mX + " my: " + mY);
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			m_Path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
		}
	}

	public void touch_up() {
		Log.v("TEST","touch_up mx: " + mX + " my: " + mY);
//        m_Path.lineTo(mX, mY);
		// commit the path to our offscreen
		if(mX != 0 && mY != 0){
			m_Canvas.drawPath(m_Path, paint);
			// kill this so we don't double draw
			m_Path = new Path();
			Paint newPaint = new Paint(paint); // Clones the mPaint object
			paths.add(new Pair<Path, Paint>(m_Path, newPaint));
		}else{
			reset();
		}

		/*try {
			listener.onFinishDrawing(getDrawingCache());
//			destroyDrawingCache();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	public Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
		Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
		Canvas canvas = new Canvas(bmOverlay);
		canvas.drawBitmap(bmp1, 0, 0, null);
		canvas.drawBitmap(bmp2, 0, 0, null);
		return bmOverlay;
	}

	public void reset()
	{
		paths.clear();
		invalidate();
	}

	private double calculateDistanceBetween2Points(PointF start, PointF end){
		return Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
	}

	public PointF calculateCenterPointOfMultiplePoints() {
		float minY = y[0];
		float minX = x[0];
		float maxY = y[0];
		float maxX = x[0];

		for(int i = 0; i < numberTouchPoint(); i++){
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

	private boolean detectTouchCaptureCamera(float x, float y, Rect rect){
		Log.v("TEST","recLeft: " + rect.left + " rectRight: " + rect.right + " rectBottom: " + rect.bottom + " rectTop: " + rect.top);
		if(x > rect.left && x < rect.right && y < rect.bottom && y > rect.top){
			return true;
		}
		return false;
	}
}
