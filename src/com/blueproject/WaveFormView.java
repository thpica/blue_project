package com.blueproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.blueproject.DrawingThread;
import com.blueproject.AcquisitionThread;

public class WaveFormView extends SurfaceView implements SurfaceHolder.Callback {
	static float voltAdjFactor = (float)0.195;
	public float voltDivFactor=5;
	int height;
    int width;
    int buffer[]=new int[480];
    float position=0;
    public int xZero=0;
	private Paint plot_paint = new Paint();
	private Paint grid_paint = new Paint();
	private Paint center_paint = new Paint();
	public DrawingThread _dthread;
	public AcquisitionThread _athread;
	
    public WaveFormView(Context context,AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        _dthread = new DrawingThread(getHolder(), this);
        _athread = new AcquisitionThread();
        setFocusable(true);
        plot_paint.setStrokeWidth(1);
        plot_paint.setColor(Color.rgb(0, 0, 255));
        grid_paint.setStrokeWidth(1);
        grid_paint.setColor(Color.rgb(100, 100, 100));
        center_paint.setStrokeWidth(1);
        center_paint.setColor(Color.rgb(200, 200, 200));
        
    }



	@Override
    public void onDraw(Canvas canvas) {
    	//position=height/2+selPosition*2;
    	canvas.drawColor(Color.BLACK); //erase screen
    	// draw grids
	    for(int x = width/2; x<width; x+=40){
	    	canvas.drawLine(
	    			width/2-(x-width/2), 0,
	    			width/2-(x-width/2), height,
	    			grid_paint);
	    	canvas.drawLine(
	    			x, 0,
	    			x, height,
	    			grid_paint);
	    }
	    
	    for(int y = height/2; y<height; y+=40){
	    	canvas.drawLine(
	    			0, height/2-(y-height/2),
	    			width, height/2-(y-height/2),
	    			grid_paint);
	    	canvas.drawLine(
	    			0, y,
	    			width, y,
	    			grid_paint);
	    }
	    
	    // draw center cross
		canvas.drawLine(0, (height/2), width, (height/2), center_paint);
		canvas.drawLine((width/2), 0, (width/2), height, center_paint);
        //plot points
		/*for (int i=0, k=xZero-_athread.buffer.length/2;i<_athread.buffer.length-1;i++,k++){
	     	canvas.drawLine(k, 
	     			position+height/2-_athread.buffer[i]*voltAdjFactor*voltDivFactor,
	     			k+1,
	     			position+height/2-_athread.buffer[i+1]*voltAdjFactor*voltDivFactor, 
	     			plot_paint);
	    }*/
		for (int i=0, k=xZero-_athread.buffer.length/2;i<_athread.buffer.length-1;i++,k++){
	     	canvas.drawLine(k, 
	     			position+height/2-buffer[i]*voltAdjFactor*voltDivFactor,
	     			k+1,
	     			position+height/2-buffer[i+1]*voltAdjFactor*voltDivFactor, 
	     			plot_paint);
	    }
     // draw outline
		canvas.drawLine(0, 0, width-1, 0, center_paint);	// top
		canvas.drawLine((width-1), 0, (width-1), (height-1), center_paint); //right
		canvas.drawLine(0, (height-1), (width-1), (height-1), center_paint); // bottom
		canvas.drawLine(0, 0, 0, (height-1), center_paint); //left
		//draw FPS counter
		//canvas.drawText("height/width: " + String.valueOf((float)(height/width)), 1, 11, center_paint);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int _format, int _width, int _height) {
    	height=_height;
    	width=_width;
    	xZero=_width/2;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        _dthread.setRunning(true);
        _dthread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // simply copied from sample application LunarLander:
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        _dthread.setRunning(false);
        while (retry) {
            try {
                _dthread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }
    //On surfaceview touch event processing
    float firstTouch,firstPos;
    @Override
    public boolean onTouchEvent(MotionEvent event){
    	if(event.getAction()==MotionEvent.ACTION_DOWN) {
    		firstTouch=event.getY();
    		firstPos=position;
    	}else{
	    	position=firstPos-(firstTouch-event.getY())/(5);
    	}
		return true;	
    }
}
