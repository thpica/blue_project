package com.blueproject;

import android.graphics.Canvas;
import android.view.SurfaceHolder;
import com.blueproject.WaveFormView;

class DrawingThread extends Thread {
	private SurfaceHolder _surfaceHolder;
    private WaveFormView _waveformview;
    private boolean _run = false;

    public DrawingThread(SurfaceHolder surfaceHolder, WaveFormView waveformview) {
        _surfaceHolder = surfaceHolder;
        _waveformview = waveformview;
    }

    public void setRunning(boolean run) {
        _run = run;
    }

    public SurfaceHolder getSurfaceHolder() {
        return _surfaceHolder;
    }

    @Override
    public void run() {
        Canvas c;
        while (_run) {
            c = null;
            
            try {
                c = _surfaceHolder.lockCanvas(null);
                synchronized (_surfaceHolder) {
                    _waveformview.onDraw(c);
                }
            } finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (c != null) {
                    _surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }
}
