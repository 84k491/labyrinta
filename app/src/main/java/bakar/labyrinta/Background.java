package bakar.labyrinta;

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
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

public class Background extends SurfaceView{

    BgRenderThread renderThread;
    SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Logger.getAnonymousLogger().info("Background surfaceChanged()");
        }
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Logger.getAnonymousLogger().info("Background surfaceCreated()");
            renderThread = new BgRenderThread();
            renderThread.setHolder(getHolder());
            renderThread.setRunning(true);
            renderThread.start();
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Logger.getAnonymousLogger().info("Background surfaceDestroyed()");
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
    };

    public static final int dotDepthLevelAmount = 10;
    public static final float maxDotBlurRadius = 24.f;

    private static final boolean doNothing = false;

    private RenderScript rs = RenderScript.create(getContext());


    public Background(Context _context){
        super(_context);
        setZOrderOnTop(true);
        getHolder().addCallback(callback);
        getHolder().setFormat(PixelFormat.RGBA_8888);
    }

    public Background(Context _context, AttributeSet set, int defStyle){
        super(_context, set, defStyle);
        setZOrderOnTop(true);
        getHolder().addCallback(callback);
        getHolder().setFormat(PixelFormat.RGBA_8888);
    }

    public Background(Context _context, AttributeSet set){
        super(_context, set);
        setZOrderOnTop(true);
        getHolder().addCallback(callback);
        getHolder().setFormat(PixelFormat.RGBA_8888);
    }
    class BgRenderThread extends Thread{

        boolean running = false;
        private SurfaceHolder surfaceHolder;
        Random random;

        private long prevDrawTime = 0;
        private long redrawPeriod = 30; // milliS // microS

        Paint dotPaint = new Paint();
        final Matrix translate_matrix = new Matrix();
        ArrayList<TheDot> dotPool = new ArrayList<>();
        private final Map<Long, Bitmap> dotBitmaps = new HashMap<>();

        private long getTime(){
            return System.nanoTime() / 1_000_000;
        }

        BgRenderThread(){
            Logger.getAnonymousLogger().info("BGRenderThread ctor");
            random = new Random(System.currentTimeMillis());

            dotPaint.setColor(Color.argb(255,255,255,255));

            if (dotPool.size() == 0){
                Random r = new Random(System.currentTimeMillis());
                for (int i = 0; i < 100; ++i){
                    dotPool.add(new TheDot(getWidth(), getHeight(), r));
                }
            }
        }

        void drawBackground(Canvas canvas){
            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
            //canvas.drawBitmap(BgResources.inst().backgroundBitmap, BgResources.inst().bgScaleMatrix, BgResources.inst().common);
        }
        void drawDotBitmap(Canvas canvas, Bitmap bmp, CPoint.Screen pos){
            translate_matrix.reset();

            translate_matrix.preTranslate(pos.x - (float)bmp.getWidth() / 2,
                    pos.y  - (float)bmp.getHeight() / 2);

            canvas.drawBitmap(bmp, translate_matrix, dotPaint);
        }
        void drawDots(Canvas canvas){
            for(TheDot dot : dotPool){
                drawDotBitmap(canvas, getDotBmp(dot.radius, dot.depth, rs), dot.pos);
                dot.onDraw(); // FIXME: 4/16/19
            }
        }

        void onDraw(Canvas canvas){
            if (doNothing){
                return;
            }
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
            boolean isCanvasLocked = false;

            while (running) {
                if (getTime() - prevDrawTime > redrawPeriod) {
                    prevDrawTime = getTime();
                    try {
                        if (!isCanvasLocked){
                            //Logger.getAnonymousLogger().info("Locking canvas @ " + this);
                            canvas = surfaceHolder.lockCanvas();
                            isCanvasLocked = true;
                            if (canvas == null){
                                isCanvasLocked = false;
                                continue;
                            }
                            synchronized (surfaceHolder){
                                onDraw(canvas);
                            }
                        }
                    } finally {
                        if (canvas != null) {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                            //Logger.getAnonymousLogger().info("Unlocking canvas" + this);
                            isCanvasLocked = false;
                        }
                    }
                }
                else{
                    try {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }

        private Bitmap makeTransparentBmp(int size){
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

        private Bitmap makeDotBmp(float dot_radius, float blur_radius, RenderScript rs){
            Bitmap dot = makeTransparentBmp(Math.round(dot_radius * 2 + dot_radius * blur_radius / 2));

            Canvas dot_canvas = new Canvas(dot);
            Paint dot_paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            dot_paint.setColor(Color.argb(255, 255, 255, 255));
            dot_canvas.drawCircle(dot.getWidth() / 2, dot.getHeight() / 2, dot_radius, dot_paint);
            if (0 != blur_radius)
                dot = blurRs(blur_radius, dot, rs);

            return dot;
        }

        Bitmap getDotBmp(int dot_radius, int depthLevel, RenderScript rs){
            long key = Math.round(Math.pow(dot_radius + 1, depthLevel + 1));

            if (!dotBitmaps.containsKey(key)){
                float blur_radius = depthLevel * maxDotBlurRadius /
                        dotDepthLevelAmount;
                dotBitmaps.put(key, makeDotBmp(dot_radius, blur_radius, rs));
            }
            return dotBitmaps.get(key);
        }

        private static final float BITMAP_SCALE = 1.f;

        private Bitmap blurRs(float radius, Bitmap image, RenderScript rs) {
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

class TheDot{
    Random random;
    CPoint.Screen pos = new CPoint.Screen();
    int radius;
    int depth;
    PointF moveVector = new PointF();
    int alpha;
    int alphaStep = 1;
    int max_x = 500;
    int max_y = 500;

    TheDot(int _max_x, int _max_y, Random _random){
        random = _random;
        max_x = _max_x;
        max_y = _max_y;
        randomValues();
        alpha = random.nextInt(200) - 100;
    }
    void randomValues(){
        pos.x = random.nextFloat() * max_x;
        pos.y = random.nextFloat() * max_y;

        moveVector.x = random.nextFloat() * .4f - .2f;
        moveVector.y = random.nextFloat() * .4f - .2f;

        radius = random.nextInt(3) + 1;
        depth = random.nextInt(Background.dotDepthLevelAmount);
    }
    void moveByVector(){
        pos.offset(moveVector.x, moveVector.y);
    }

    void onDraw(){
        moveByVector();

        alpha += alphaStep;
        if (alpha > 40){
            alpha = -40;
            randomValues();
        }
    }
}