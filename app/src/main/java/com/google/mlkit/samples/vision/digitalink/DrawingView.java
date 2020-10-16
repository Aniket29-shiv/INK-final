package com.google.mlkit.samples.vision.digitalink;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.google.mlkit.samples.vision.digitalink.StrokeManager.ContentChangedListener;
import com.google.mlkit.vision.digitalink.Ink;

import java.util.ArrayList;
import java.util.List;

/**
 * Main view for rendering content.
 *
 * <p>The view accepts touch inputs, renders them on screen, and passes the content to the
 * StrokeManager. The view is also able to draw content from the StrokeManager.
 */
public class DrawingView extends View implements ContentChangedListener, View.OnTouchListener {
    private static final String TAG = "MLKD.DrawingView";
    private static final int STROKE_WIDTH_DP = 3;
    private static final int MIN_BB_WIDTH = 10;
    private static final int MIN_BB_HEIGHT = 10;
    private static final int MAX_BB_WIDTH = 999;
    private static final int MAX_BB_HEIGHT = 500;

    private final Paint recognizedStrokePaint;
    private final TextPaint textPaint;
    private Paint currentStrokePaint;
    private Path currentStroke;
    private ArrayList<Paint> paints = new ArrayList<Paint>();
    private ArrayList<Paint> undonepaints = new ArrayList<Paint>();

    private Path drawPath;
    private  Paint canvasPaint;
    private Paint drawPaint;

//    private Paint currentStrokePaint;
//    private Path currentStroke;


    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    private StrokeManager strokeManager;
//    private Paint paint;

    private boolean erase=false;
    private int paintColor = 0xFF660000;
    private float brushSize, lastBrushSize;



    public DrawingView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        setupDrawing();
        currentStrokePaint = new Paint();
        currentStrokePaint.setColor(paintColor);
        currentStrokePaint.setAntiAlias(true);
        // Set stroke width based on display density.
        currentStrokePaint.setStrokeWidth(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, STROKE_WIDTH_DP, getResources().getDisplayMetrics()));
        currentStrokePaint.setStyle(Paint.Style.STROKE);
        currentStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        currentStrokePaint.setStrokeCap(Paint.Cap.ROUND);

//        currentStrokePaint = new Paint();
//        currentStrokePaint.setColor(0xFF1B1B1B); // black.
//        currentStrokePaint.setAntiAlias(true);
//        // Set stroke width based on display density.
//        currentStrokePaint.setStrokeWidth(
//                TypedValue.applyDimension(
//                        TypedValue.COMPLEX_UNIT_DIP, STROKE_WIDTH_DP, getResources().getDisplayMetrics()));
//        currentStrokePaint.setStyle(Paint.Style.STROKE);
//        currentStrokePaint.setStrokeJoin(Paint.Join.ROUND);
//        currentStrokePaint.setStrokeCap(Paint.Cap.ROUND);


        recognizedStrokePaint = new Paint(currentStrokePaint);
        recognizedStrokePaint.setColor(0xFFFFFF); // white.

        textPaint = new TextPaint();
        textPaint.setColor(0xFF1B1B1B);// black.

        currentStroke = new Path();
//        currentStroke = new Path();
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    private static Rect computeBoundingBox(Ink ink) {
        float top = Float.MAX_VALUE;
        float left = Float.MAX_VALUE;
        float bottom = Float.MIN_VALUE;
        float right = Float.MIN_VALUE;
        for (Ink.Stroke s : ink.getStrokes()) {
            for (Ink.Point p : s.getPoints()) {
                top = Math.min(top, p.getY());
                left = Math.min(left, p.getX());
                bottom = Math.max(bottom, p.getY());
                right = Math.max(right, p.getX());
            }
        }
        float centerX = (left + right) / 2;
        float centerY = (top + bottom) / 2;
        Rect bb = new Rect((int) left, (int) top, (int) right, (int) bottom);
        // Enforce a minimum size of the bounding box such that recognitions for small inks are readable
        bb.union(
                (int) (centerX - MIN_BB_WIDTH / 2),
                (int) (centerY - MIN_BB_HEIGHT / 2),
                (int) (centerX + MIN_BB_WIDTH / 2),
                (int) (centerY + MIN_BB_HEIGHT / 2));
        // Enforce a maximum size of the bounding box, to ensure Emoji characters get displayed
        // correctly
        if (bb.width() > MAX_BB_WIDTH) {
            bb.set(bb.centerX() - MAX_BB_WIDTH / 2, bb.top, bb.centerX() + MAX_BB_WIDTH / 2, bb.bottom);
        }
        if (bb.height() > MAX_BB_HEIGHT) {
            bb.set(bb.left, bb.centerY() - MAX_BB_HEIGHT / 2, bb.right, bb.centerY() + MAX_BB_HEIGHT / 2);
        }
        return bb;
    }

    void setStrokeManager(StrokeManager strokeManager) {
        this.strokeManager = strokeManager;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
        invalidate();
    }

    public void redrawContent() {
        clear();
        Ink currentInk = strokeManager.getCurrentInk();
        drawInk(currentInk, currentStrokePaint);

        List<RecognitionTask.RecognizedInk> content = strokeManager.getContent();
        for (RecognitionTask.RecognizedInk ri : content) {
            drawInk(ri.ink, recognizedStrokePaint);
            final Rect bb = computeBoundingBox(ri.ink);
            drawTextIntoBoundingBox(ri.text, bb, textPaint);
        }
        invalidate();
    }

    private void drawTextIntoBoundingBox(String text, Rect bb, TextPaint textPaint) {
        final float arbitraryFixedSize = 20.f;
        // Set an arbitrary text size to learn how high the text will be.
        textPaint.setTextSize(arbitraryFixedSize);
        textPaint.setTextScaleX(1.f);

        // Now determine the size of the rendered text with these settings.
        Rect r = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), r);

        // Adjust height such that target height is met.
        float textSize = arbitraryFixedSize * (float) bb.height() / (float) r.height();
        textPaint.setTextSize(textSize);

        // Redetermine the size of the rendered text with the new settings.
        textPaint.getTextBounds(text, 0, text.length(), r);

        // Adjust scaleX to squeeze the text.
        textPaint.setTextScaleX((float) bb.width() / (float) r.width());

        textPaint.setColor(paintColor);
        textPaint.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));

        // And finally draw the text.
        drawCanvas.drawText(text, bb.left, bb.bottom, textPaint);
    }

    private void drawInk(Ink ink, Paint paint) {
        for (Ink.Stroke s : ink.getStrokes()) {
            drawStroke(s, paint);
        }
    }

    private void drawStroke(Ink.Stroke s, Paint paint) {
        Log.i(TAG, "drawstroke");
        Path path = null;
        for (Ink.Point p : s.getPoints()) {
            if (path == null) {
                path = new Path();
                path.moveTo(p.getX(), p.getY());
            } else {
                path.lineTo(p.getX(), p.getY());
            }
        }
        drawCanvas.drawPath(path, paint);
    }

    public void clear() {
        currentStroke.reset();
        onSizeChanged(
                canvasBitmap.getWidth(),
                canvasBitmap.getHeight(),
                canvasBitmap.getWidth(),
                canvasBitmap.getHeight());
    }

    public void onClickUndo () {
        if (paints.size()>0)
        {
            undonepaints.add(paints.remove(paints.size()-1));
            invalidate();
        }
        else
        {

        }
        //toast the user
    }

    public void onClickRedo (){
        if (undonepaints.size()>0)
        {
            paints.add(undonepaints.remove(undonepaints.size()-1));
            invalidate();
        }
        else
        {

        }
        //toast the user
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(currentStroke, currentStrokePaint);
    }

    @Override
    public boolean onTouchEventPaint(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                undonepaints.clear();
                currentStroke.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                currentStroke.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(currentStroke, currentStrokePaint);
                paints.add(textPaint);
                currentStroke = new Path();
                currentStroke.reset();

                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }
    public void setColor(String newColor){
        invalidate();
        paintColor = Color.parseColor(newColor);
        currentStrokePaint.setColor(paintColor);
//        textPaint.setColor(paintColor);
    }
    public void setupDrawing(){
        currentStroke = new Path();
        currentStrokePaint = new Paint();
        currentStrokePaint.setColor(paintColor);
        currentStrokePaint.setAntiAlias(true);
        currentStrokePaint.setStrokeWidth(5);
        currentStrokePaint.setStyle(Paint.Style.STROKE);
        currentStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        currentStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
        brushSize = getResources().getInteger(R.integer.small_size);
        lastBrushSize = brushSize;
        currentStrokePaint.setStrokeWidth(brushSize);

        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
        brushSize = getResources().getInteger(R.integer.small_size);
        lastBrushSize = brushSize;
        drawPaint.setStrokeWidth(brushSize);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                undonepaints.clear();
                currentStroke.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                currentStroke.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                currentStroke.lineTo(x, y);
                drawCanvas.drawPath(currentStroke, currentStrokePaint);
                paints.add(textPaint);
                currentStroke = new Path();
                currentStroke.reset();
                break;
            default:
                break;
        }
        strokeManager.addNewTouchEvent(event);
        invalidate();
        return true;
    }

    @Override
    public void onContentChanged() {
        redrawContent();
    }

    public void startNew(){
//        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
//        invalidate();
        currentStroke.reset();
        onSizeChanged(
                canvasBitmap.getWidth(),
                canvasBitmap.getHeight(),
                canvasBitmap.getWidth(),
                canvasBitmap.getHeight());
    }
    public void setErase(boolean isErase){
        erase=isErase;
        if(erase){
//            currentStrokePaint.setColor(paintColor);
//            currentStrokePaint.setStrokeWidth(50f);
//            recognizedStrokePaint.setColor(0xFFFFFF);
//            textPaint.setColor(0xFFFFFF);
            currentStrokePaint.setStrokeWidth(50f);
            currentStrokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            recognizedStrokePaint.setStrokeWidth(50f);
            recognizedStrokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            textPaint.clearShadowLayer();

            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        }

        else currentStrokePaint.setXfermode(null);
    }
    public void setBrushSize(float newSize){
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, getResources().getDisplayMetrics());
        brushSize=pixelAmount;
        currentStrokePaint.setStrokeWidth(brushSize);
    }
    public void setLastBrushSize(float lastSize){
        lastBrushSize=lastSize;
    }
    public float getLastBrushSize(){
        return lastBrushSize;
    }
    @Override
    public void onSizeChangedPaint(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    public void onDrawPaint(Canvas canvas) {
        
    }

    @Override
    public void oncurrentStrokePaint(Canvas canvas) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

//    @Override
//    public void oncurrentStrokePaint(Canvas canvas) {
//        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
//        canvas.currentStroke(currentStroke, currentStrokePaint);
//    }
}