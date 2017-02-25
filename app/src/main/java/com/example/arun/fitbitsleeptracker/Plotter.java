package com.example.arun.fitbitsleeptracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;

/**
 * Created by Arun on 2/4/2017.
 */
public class Plotter extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder holder;
    private float heightDrop = 600;
    private int[] data;

    public Plotter(Context context) {
        super(context);

        setY(heightDrop);

        holder = getHolder();
        holder.addCallback(this);

        holder = null;
        data = null;
    }

    public void setData(int[] data) {
        this.data = data;
    }

    public void update(int lowerLimit) {
        Log.i("Output", lowerLimit + " this");
        if(holder == null || data == null) return;
        Canvas canvas = holder.lockCanvas();

        if(canvas == null) return;

        int max = 0, min = 10000;
        for(int i = 0; i < data.length; i++) {
            if(data[i] > max) max = data[i];
            if(data[i] < min) min = data[i];
        }

        Log.i("Output", min + " " + max);

        int bin = 20;
        double segmentWidth = (canvas.getWidth() + 1f) / (data.length / bin);

        canvas.drawColor(Color.BLUE);

        Paint brush = new Paint();
        brush.setStrokeWidth(7f);
        brush.setColor(Color.RED);
        brush.setStrokeCap(Paint.Cap.ROUND);

        float height = (canvas.getHeight() - heightDrop);
        for(int i = bin; i < data.length - bin; i += bin) {
            float prevHeight = 0, nextHeight = 0;
            for(int j = 0; j < bin; j++) {
                prevHeight += data[j + i - bin];
                nextHeight += data[j + i];
            }

            if(nextHeight/bin <= lowerLimit) brush.setColor(Color.BLACK);
            else brush.setColor(Color.RED);

            prevHeight = height - process(prevHeight/bin, min, max, height);
            nextHeight = height - process(nextHeight/bin, min, max, height);

            canvas.drawLine((float) ((i - 1) / bin * segmentWidth), prevHeight, (float)(i / bin * segmentWidth), nextHeight, brush);
        }

        holder.unlockCanvasAndPost(canvas);
    }

    public float process(float data, int min, int max, float height) {
        return data * 5;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.holder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
