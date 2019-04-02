package bakar.labirynth;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Shader;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Background extends SurfaceView implements SurfaceHolder.Callback{

    RenderThread renderThread;
    private RenderScript rs = RenderScript.create(getContext());

    public Background(Context _context){
        super(_context);
        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderThread = new RenderThread();
        renderThread.setHolder(getHolder());
        renderThread.setRunning(true);
        renderThread.start();
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        renderThread.setRunning(false);
        while (retry) {
            try {
                renderThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    private class RenderThread extends Thread{

        boolean running = false;
        private SurfaceHolder surfaceHolder;
        Random random;
        final Matrix bgScaleMatrix = new Matrix();
        final Matrix translate_matrix = new Matrix();
        ArrayList<TheDot> dotPool = new ArrayList<>();
        Map<Long, Bitmap> dotBitmaps = new HashMap<>();

        private int dotDepthLevelAmount = 10;
        private float maxDotBlurRadius = 24.f;

        private Paint dotPaint = new Paint();
        private Paint common = new Paint();

        Bitmap backgroundBitmap;

        RenderThread(){
            random = new Random(System.currentTimeMillis());

            dotPaint.setColor(Color.argb(255,255,255,255));
            updateBackgroundBmp();
            for (int i = 0; i < 100; ++i){
                dotPool.add(new TheDot());
            }
        }

        Bitmap makeTransparentBmp(int size){
            int width =  size;
            int height = size;
            Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            int [] allpixels = new int [ myBitmap.getHeight()*myBitmap.getWidth()];
            myBitmap.setPixels(allpixels, 0, width, 0, 0, width, height);

            for(int i =0; i<myBitmap.getHeight() * myBitmap.getWidth(); i++) {
                allpixels[i] = Color.alpha(Color.TRANSPARENT);
            }

            myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
            return myBitmap;
        }
        Bitmap makeDotBmp(float dot_radius, float blur_radius){
            Bitmap dot = makeTransparentBmp(Math.round(dot_radius * 2 + dot_radius * blur_radius / 2));

            Canvas dot_canvas = new Canvas(dot);
            Paint dot_paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            dot_paint.setColor(Color.argb(255, 255, 255, 255));
            dot_canvas.drawCircle(dot.getWidth() / 2, dot.getHeight() / 2, dot_radius, dot_paint);
            if (0 != blur_radius)
                dot = blurRs(blur_radius, dot);

            return dot;
        }
        Bitmap getDotBmp(int dot_radius, int depthLevel){
            long key = Math.round(Math.pow(dot_radius, depthLevel));

            if (!dotBitmaps.containsKey(key)){
                float blur_radius = depthLevel * maxDotBlurRadius / dotDepthLevelAmount;
                dotBitmaps.put(key, makeDotBmp(dot_radius, blur_radius));
            }
            return dotBitmaps.get(key);
        }

        void updateBackgroundBmp(){
            int h = getHeight();
            int w = getWidth();

            Bitmap blured = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

            int color1 = Color.parseColor("#1e508c");
            int color2 = Color.parseColor("#da2b90");
            LinearGradient shader = new LinearGradient(0, 0, w, h - 70,
                    color1, color2, Shader.TileMode.MIRROR);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setDither(false);
            p.setShader(shader);

            Canvas c = new Canvas(blured);
            c.drawRect(0,0, w, h, p);

            //c.drawBitmap(makeDotBmp(5, 25), new Matrix(), common);
            //backgroundBitmap = blur(getContext(), blured);
            //backgroundBitmap = blurFastGauss(40, blured);
            //backgroundBitmap = blurStack(100, blured);
            //backgroundBitmap = blurBox(500, blured);
            //backgroundBitmap = blurSuperFast(100, blured);
            backgroundBitmap = blured;
        }
        void drawBackground(Canvas canvas){
            int h = backgroundBitmap.getHeight();
            int w = backgroundBitmap.getWidth();
            canvas.drawBitmap(backgroundBitmap, bgScaleMatrix, common);
        }
        void drawDotBitmap(Canvas canvas, Bitmap bmp, CPoint.Screen pos){
            translate_matrix.reset();

            translate_matrix.preTranslate(pos.x - (float)bmp.getWidth() / 2,
                    pos.y  - (float)bmp.getHeight() / 2);

            canvas.drawBitmap(bmp,
                    translate_matrix, dotPaint);
        }
        void drawDots(Canvas canvas){
            for(TheDot dot: dotPool){
                dot.draw(canvas);
            }
        }

        void onDraw(Canvas canvas){
            drawBackground(canvas);
            drawDots(canvas);
        }

        void setRunning(boolean running_){
            running = running_;
        }
        void setHolder(SurfaceHolder holder){
            surfaceHolder = holder;
        }
        @Override
        public void run() {
            Canvas canvas = new Canvas();
            while (running) {
                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    if (canvas == null)
                        continue;
                    onDraw(canvas);
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

        class TheDot{
            CPoint.Screen pos = new CPoint.Screen();
            int radius;
            int depth;
            PointF moveVector = new PointF();
            int alpha;
            int alphaStep = 1;

            TheDot(){
                randomValues();
                alpha = random.nextInt(200) - 100;
            }
            void randomValues(){
                pos.x = random.nextFloat() * getWidth();
                pos.y = random.nextFloat() * getHeight();

                moveVector.x = random.nextFloat() * .4f - .2f;
                moveVector.y = random.nextFloat() * .4f - .2f;

                radius = random.nextInt(3) + 1;
                depth = random.nextInt(dotDepthLevelAmount);
            }
            void moveByVector(){
                pos.offset(moveVector.x, moveVector.y);
            }
            void draw(Canvas canvas){
                //dotPaint.setAlpha(100 - Math.abs(alpha));
                drawDotBitmap(canvas, getDotBmp(radius, depth), pos);

                moveByVector();

                alpha += alphaStep;
                if (alpha > 40){
                    alpha = -40;
                    randomValues();
                }
            }
        }

        private static final float BITMAP_SCALE = 1.f;

        public Bitmap blurRs(float radius, Bitmap image) {
            int width = Math.round(image.getWidth() * BITMAP_SCALE);
            int height = Math.round(image.getHeight() * BITMAP_SCALE);

            Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
            Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

            ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
            Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

            intrinsicBlur.setRadius(radius);
            intrinsicBlur.setInput(tmpIn);
            intrinsicBlur.forEach(tmpOut);
            tmpOut.copyTo(outputBitmap);

            return outputBitmap;
        }
    }
}