package e.p.bleController;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class JoystickView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener
{
    private int valX = 0;
    private int valY = 0;
    private float centerX;
    private float centerY;
    private float baseRadius;
    private float hatRadius;

    public boolean joy_spring = false;
    public int joy_step = 4;
    public int joy_deadzone = 20;


    private JoystickListener joystickCallback;
    private final int ratio = 5; //The smaller, the more shading will occur

    private void setupDimensions()
    {
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        baseRadius = Math.min(getWidth(), getHeight());
        baseRadius /= 2.5;
        hatRadius = Math.min(getWidth(), getHeight());
        hatRadius /= 6;
    }

    public JoystickView(Context context)
    {
        super(context);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }

    public JoystickView(Context context, AttributeSet attributes, int style)
    {
        super(context, attributes, style);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }

    public JoystickView (Context context, AttributeSet attributes)
    {
        super(context, attributes);
        getHolder().addCallback(this);
        setOnTouchListener(this);
        if(context instanceof JoystickListener)
            joystickCallback = (JoystickListener) context;
    }

    private void drawJoystick(float newX, float newY, int val_x, int val_y)
    {
        if(getHolder().getSurface().isValid())
        {
            Canvas myCanvas = this.getHolder().lockCanvas(); //Stuff to draw
            Paint colors = new Paint();
            myCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // Clear the BG

            //First determine the sin and cos of the angle that the touched point is at relative to the center of the joystick
            float hypotenuse = (float) Math.sqrt(Math.pow(newX - centerX, 2) + Math.pow(newY - centerY, 2));
            float sin = (newY - centerY) / hypotenuse; //sin = o/h
            float cos = (newX - centerX) / hypotenuse; //cos = a/h

            //Draw the base first before shading
            colors.setARGB(100, 100, 0, 160);
            myCanvas.drawCircle(centerX, centerY, baseRadius, colors);
            int lim = (int) (baseRadius / ratio);
            for(int i = 1; i <= lim; i++)
            {
                colors.setARGB(lim-i, 128, 0, 0); //Gradually decrease the shade of black drawn to create a nice shading effect
                myCanvas.drawCircle(newX - cos * hypotenuse * (ratio/baseRadius) * i,
                        newY - sin * hypotenuse * (ratio/baseRadius) * i, i * (hatRadius * ratio / baseRadius), colors); //Gradually increase the size of the shading effect
            }

            //Drawing the joystick hat
            for(int i = 0; i <= (int) (hatRadius / ratio); i++)
            {
                colors.setARGB(255, (int) (i * (255 * ratio / hatRadius)), (int) (i * (255 * ratio / hatRadius)), 255); //Change the joystick color for shading purposes
                myCanvas.drawCircle(newX, newY, hatRadius - (float) i * (ratio) / 2 , colors); //Draw the shading for the hat
            }

            float y = getY() + getHeight() - 50;

            colors.setARGB(255, 255, 255, 0);
            colors.setTextSize(52);
            String xs = "" + val_x;
            String ys = "" + val_y;
            myCanvas.drawText(xs, getX() + 12, y, colors);
            myCanvas.drawText(ys, getX() + getWidth() - 120, y, colors);

            getHolder().unlockCanvasAndPost(myCanvas); //Write the new drawing to the SurfaceView
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        setupDimensions();
        drawJoystick(centerX, centerY,0,0);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public boolean onTouch(View v, MotionEvent e)
            // the values
    {
        if (v.equals(this))
        {
            boolean changed = false;
            float draw_x = centerX;
            float draw_y = centerY;

            if(joy_spring && e.getAction() == e.ACTION_UP)
            {
                if (valX != 0 || valY != 0) {
                    changed = true;
                    valX = 0;
                    valY = 0;
                }

                // drawJoystick(centerX, centerY, 0, 0);
                // joystickCallback.onJoystickMoved(0, 0, getId());
            }
            else
            {
                float displacement = (float) Math.sqrt((Math.pow(e.getX() - centerX, 2)) + Math.pow(e.getY() - centerY, 2));
                if (displacement < baseRadius)
                {
                    double fx = 128.0 * (e.getX() - centerX)/baseRadius;
                    double fy = 128.0 * (e.getY() - centerY)/baseRadius;
                    int temp_x = (int) -fx;
                    int temp_y = (int) -fy;

                    draw_x = e.getX();
                    draw_y = e.getY();

                    if (Math.abs(temp_x) <= joy_deadzone)
                        temp_x = 0;
                    if (Math.abs(temp_y) <= joy_deadzone)
                        temp_y = 0;

                    if (Math.abs(valX-temp_x) >= joy_step ||
                        Math.abs(valY-temp_y) >= joy_step)
                    {
                        // note that X is reversed from Tumbler

                        valX = -temp_x;
                        valY = temp_y;
                        changed = true;
                    }

                    // val_x = (val_x / 60) * 20;
                    // val_y = (val_y / 50) * 20;

                    // drawJoystick(e.getX(), e.getY(),val_x,val_y);
                    // joystickCallback.onJoystickMoved(val_x,val_y,getId());
                }
                else
                {
                    float ratio = baseRadius / displacement;
                    float constrainedX = centerX + (e.getX() - centerX) * ratio;
                    float constrainedY = centerY + (e.getY() - centerY) * ratio;
                    double fx = 128.0 * (constrainedX - centerX)/baseRadius;
                    double fy = 128.0 * (constrainedY - centerY)/baseRadius;
                    int temp_x = (int) -fx;
                    int temp_y = (int) -fy;

                    if (Math.abs(temp_x) <= joy_deadzone)
                        temp_x = 0;
                    if (Math.abs(temp_y) <= joy_deadzone)
                        temp_y = 0;

                    draw_x = constrainedX;
                    draw_y = constrainedY;
                    if (Math.abs(valX-temp_x) >= joy_step ||
                        Math.abs(valY-temp_y) >= joy_step)
                    {
                        valX = -temp_x;
                        valY = temp_y;
                        changed = true;
                    }


                    // val_x = (val_x / 60) * 20;
                    // val_y = (val_y / 50) * 20;

                    // drawJoystick(constrainedX, constrainedY,val_x,val_y);
                    // joystickCallback.onJoystickMoved(val_x,val_y,getId());
                }
            }

            drawJoystick(draw_x,draw_y,valX,valY);
            if (changed)
                joystickCallback.onJoystickMoved(valX,valY,getId());
        }
        return true;
    }

    public interface JoystickListener
    {
        void onJoystickMoved(int x, int y, int id);
    }
}