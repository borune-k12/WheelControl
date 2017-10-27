package com.borune.wheelcontrol;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by user on 27.10.17.
 */

public class WheelControl extends View {   

    private final static String TAG = "WheelControl";
    private final static float TOUCH_ERROR = 5.f;
    public static final double ROTATION_STEP = 5.;

    Paint mWheelPaint;
    Paint mDisablerPaint;
    Paint mScrubberPaint;
    Path wheelPath = new Path();
    Path scrubberPath = new Path();
    int wheelWidth = 50;
    int scrubberRadius = 50;
    int wheelRadiusOuter;
    int wheelRadiusInner;
    int gapDelta = 50;
    double wheelRadius;

    boolean showAngle;

    boolean isTouched = false;

    int scrubberCx;
    int scrubberCy;

    double scrubberAngle;
    Drawable scrubberDrawable;
    int scrubberColor;
    Drawable wheelDrawable;
    int wheelColor;

    float minAngle;
    float maxAngle;
    float angleTextSize;

    OnAngleChangeListener mListener;

    public WheelControl(Context context, AttributeSet attributeSet) throws RuntimeException{
        super(context,attributeSet);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attributeSet,
                R.styleable.WheelControl,
                0, 0);

        try {
            wheelRadius = a.getFloat(R.styleable.WheelControl_wheelRadius,0);
            scrubberAngle = a.getFloat(R.styleable.WheelControl_scrubberAngle, 0);
            scrubberDrawable = a.getDrawable(R.styleable.WheelControl_scrubberDrawable);
            scrubberColor = a.getColor(R.styleable.WheelControl_scrubberColor, Color.BLUE);
            wheelDrawable = a.getDrawable(R.styleable.WheelControl_wheelDrawable);
            wheelColor = a.getColor(R.styleable.WheelControl_wheelColor,Color.GREEN);
            showAngle = a.getBoolean(R.styleable.WheelControl_showAngle,false);
            minAngle = a.getFloat(R.styleable.WheelControl_minAngle,0);
            maxAngle = a.getFloat(R.styleable.WheelControl_maxAngle,360);
            angleTextSize = a.getFloat(R.styleable.WheelControl_angleTextSize,18);
        } finally {
            a.recycle();
        }

        if(checkAngles(minAngle,maxAngle))
            init();
    }

    public void setAngle(double angle) {
        setAngle(angle,false);
    }

    public boolean isAngleShown() { return showAngle; }

    public void setMinMaxAngles(float min, float max) {
        if(checkAngles(min,max)) {
            minAngle = min;
            maxAngle = max;
            invalidate();
        }
    }


    private boolean checkAngles(float minAngle, float maxAngle) {
        if(maxAngle<=minAngle)
            throw new RuntimeException(TAG+": max angle should be greater than min. But provided max = " + maxAngle + " is not greater than min = " + minAngle);
        else if(Math.abs(maxAngle) > 180 || Math.abs(minAngle) > 180)
            throw new RuntimeException(TAG+": max and min angles should be from (-180;180). But provided max = " + maxAngle + "; min = " + minAngle);
        return true;
    }

    private void setAngle(double angle, boolean fromUser) {
        while(angle >= 360.)
            angle -= 360.;
        while(angle < 0.)
            angle += 360.;

        scrubberAngle = angle;
        double alpha = angle*Math.PI/180;

        scrubberCx = getWidth()/2 + new Double((wheelRadiusOuter-wheelWidth/2)*Math.sin(alpha)).intValue();
        scrubberCy = getHeight()/2 - new Double((wheelRadiusOuter-wheelWidth/2)*Math.cos(alpha)).intValue();

        if(mListener != null)
            mListener.onAngleChanged(scrubberAngle,fromUser);
        isTouched = false;

        invalidate();
    }


    public void setOnAngleChangeListener(OnAngleChangeListener listener) {
        mListener = listener;
    }

    private void init() {
        mWheelPaint = new Paint();
        mWheelPaint.setAntiAlias(true);
        mWheelPaint.setStyle(Paint.Style.STROKE);
        mWheelPaint.setStrokeWidth(wheelWidth);

        mScrubberPaint = new Paint();
        mScrubberPaint.setAntiAlias(true);
        mScrubberPaint.setColor(scrubberColor);

        mDisablerPaint = new Paint();
        mDisablerPaint.setAntiAlias(true);
        mDisablerPaint.setColor(Color.parseColor("#888888"));
        mDisablerPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        int centerX = width/2;
        int centerY = height/2;

        if(wheelRadius == 0)
            wheelRadius = Math.min(centerX,centerY)-wheelWidth;
        wheelPath.addCircle(centerX,centerY,(float)wheelRadius, Path.Direction.CW);


        wheelRadiusOuter = (int)wheelRadius+wheelWidth/2;
        wheelRadiusInner = wheelRadiusOuter - wheelWidth;

        double alpha = 0;

        if(scrubberAngle >=0 && scrubberAngle <360)
            alpha = scrubberAngle*Math.PI/180;

        scrubberCx = centerX + new Double((wheelRadiusOuter-wheelWidth/2)*Math.sin(alpha)).intValue();
        scrubberCy = centerY - new Double((wheelRadiusOuter-wheelWidth/2)*Math.cos(alpha)).intValue();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(wheelDrawable != null) {
            Bitmap bitmap = drawableToBitmap(wheelDrawable);
            BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            mWheelPaint.setShader(shader);
        } else
            mWheelPaint.setColor(wheelColor);

        // draw wheel
        mWheelPaint.setStrokeWidth(wheelWidth);
        canvas.drawPath(wheelPath, mWheelPaint);

        // draw disabler
        if(maxAngle-minAngle < 360) {
            mDisablerPaint.setStrokeWidth(wheelWidth);
            canvas.drawArc(new RectF(getWidth()/2-(int)wheelRadius, getHeight()/2-(int)wheelRadius, getWidth()/2+(int)wheelRadius, getHeight()/2+(int)wheelRadius), maxAngle-90, 360-maxAngle+minAngle, false, mDisablerPaint);
        }

        // draw wheel borders
        Path wheelBorder = new Path();
        wheelBorder.addCircle(getWidth()/2,getHeight()/2,(float)wheelRadiusInner, Path.Direction.CW);
        wheelBorder.addCircle(getWidth()/2,getHeight()/2,(float)wheelRadiusOuter, Path.Direction.CW);

        Paint borderPainter = new Paint();
        borderPainter.setAntiAlias(true);
        borderPainter.setColor(getResources().getColor(R.color.material_blue_grey_800));
        borderPainter.setStrokeWidth(2);
        borderPainter.setStyle(Paint.Style.STROKE);

        canvas.drawPath(wheelBorder,borderPainter);

        scrubberPath.reset();
        canvas.drawPath(scrubberPath,mScrubberPaint);

        scrubberPath.addCircle(scrubberCx, scrubberCy,scrubberRadius,Path.Direction.CW);
        canvas.drawPath(scrubberPath,mScrubberPaint);

        if(scrubberDrawable != null) {
            if(isTouched) {
                Drawable drawable = getResources().getDrawable(R.drawable.scrubber_active);
                drawable.setBounds(scrubberCx - scrubberRadius, scrubberCy - scrubberRadius, scrubberCx + scrubberRadius, scrubberCy + scrubberRadius);
                drawable.draw(canvas);
            } else {
                scrubberDrawable.setBounds(scrubberCx - scrubberRadius, scrubberCy - scrubberRadius, scrubberCx + scrubberRadius, scrubberCy + scrubberRadius);
                scrubberDrawable.draw(canvas);
            }
        }
        else canvas.drawPath(scrubberPath,mScrubberPaint);

        if(showAngle) {
            Paint fontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fontPaint.setTextSize(angleTextSize);
            fontPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            fontPaint.setStyle(Paint.Style.STROKE);
            fontPaint.setColor(wheelColor);

            String text = String.valueOf(new Double((180-scrubberAngle>0)?scrubberAngle:scrubberAngle-360).intValue());
            Rect bounds = new Rect();
            fontPaint.getTextBounds(text,0,text.length(),bounds);

            canvas.drawText(text, getWidth()/2-bounds.width()/2, getHeight()/2+bounds.height()/2, fontPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        double cx = x - getWidth()/2.;
        double cy = y - getHeight()/2.;
        double alpha = Math.atan2(cx,cy);
        double d = Math.sqrt(cy*cy + cx*cx);
        double beta = alpha*180/Math.PI;
        beta = (beta>0)?180-beta:-beta-180;
        Log.d(TAG,String.valueOf(beta));

        if(beta < minAngle- TOUCH_ERROR || beta > maxAngle+ TOUCH_ERROR) {
            isTouched = false;
            invalidate();
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // distance from scrobber center
                double dist = Math.sqrt((x-scrubberCx)*(x-scrubberCx)+(y-scrubberCy)*(y-scrubberCy));
                // if up inside wheel and outside scrubber
                if(d > wheelRadiusInner && d < wheelRadiusOuter){
                    if(dist > scrubberRadius) {
                        // do step
                        double touchAlpha = 180 - alpha * 180 / Math.PI;
                        double delta = Math.abs(touchAlpha - scrubberAngle);

                        int sign = 1;
                        if (delta > 180 && scrubberAngle < 180) sign = -1;
                        else if (delta > 180 && touchAlpha < 180) sign = 1;
                        else sign = (scrubberAngle > touchAlpha) ? -1 : 1;
                        setAngle(scrubberAngle + sign * ROTATION_STEP, true);
                        isTouched = false;
                    } else isTouched = true;
                }
                invalidate();

                break;

            case MotionEvent.ACTION_MOVE:
                if(isTouched) {
                    int newX = new Double(x + (wheelRadiusOuter-wheelWidth/2-d)*Math.sin(alpha)).intValue();
                    int newY = new Double(y + (wheelRadiusOuter-wheelWidth/2-d)*Math.cos(alpha)).intValue();

                    //if(Math.abs(newX - scrubberCx) < gapDelta && Math.abs(newY - scrubberCy) < gapDelta) {
                    scrubberAngle = 180-alpha*180/Math.PI;
                    if(mListener != null)
                        mListener.onAngleChanged(scrubberAngle,true);
                    scrubberCx = newX;
                    scrubberCy = newY;
                    invalidate();
                    //   }
                }
                //isTouched = true;
                break;

            case MotionEvent.ACTION_UP:
                isTouched = false;
                invalidate();
                break;
        }

        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putFloat("scrubberAngle", (float) scrubberAngle);
        state.putBoolean("showAngle",showAngle);;
        state.putParcelable("super", super.onSaveInstanceState());
        return state;
    }


    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            scrubberAngle = ((Bundle)state).getFloat("scrubberAngle",0);
            showAngle = ((Bundle)state).getBoolean("showAngle",false);
            super.onRestoreInstanceState(bundle.getParcelable("super"));
        } else {
            super.onRestoreInstanceState(state);
        }
    }



    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }



    public interface OnAngleChangeListener {
        void onAngleChanged(double angle, boolean fromUser);
        }
    
}
