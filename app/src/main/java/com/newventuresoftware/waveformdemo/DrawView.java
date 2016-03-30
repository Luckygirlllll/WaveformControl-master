package com.newventuresoftware.waveformdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by Iryna on 3/30/16.
 */
public class DrawView extends View {
    Paint paint = new Paint();

    public DrawView(OnClickListener context) {
        super((Context) context);
        paint.setColor(Color.RED);
    }

    public void onDraw(Canvas canvas) {
        canvas.drawLine(0, 0, 20, 20, paint);
        canvas.drawLine(20, 0, 0, 20, paint);
    }
}
