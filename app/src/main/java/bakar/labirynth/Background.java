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

    class RenderThread extends Thread{

        boolean running = false;
        private SurfaceHolder surfaceHolder;
        Random random;

        RenderThread(){
            random = new Random(System.currentTimeMillis());

            BgResources.inst().dotPaint.setColor(Color.argb(255,255,255,255));
            if (BgResources.inst().backgroundBitmap == null)
                updateBackgroundBmp();

            if (BgResources.inst().dotPool.size() == 0){
                while (BgResources.inst().isDotsArrayLocked){}
                BgResources.inst().isDotsArrayLocked = true;

                Random r = new Random(System.currentTimeMillis());
                for (int i = 0; i < 100; ++i){
                    BgResources.inst().dotPool.
                            add(new TheDot(getWidth(), getHeight(), r));
                }

                BgResources.inst().isDotsArrayLocked = false;
            }
        }

        void updateBackgroundBmp(){
            final float bmp_scale = 0.25f;

            int h = Math.round(getHeight() * bmp_scale);
            int w = Math.round(getWidth() * bmp_scale);

            BgResources.inst().bgScaleMatrix.postScale(Math.round(1.f / bmp_scale),
                                                       Math.round(1.f / bmp_scale));

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
            BgResources.inst().backgroundBitmap = blured;
        }
        void drawBackground(Canvas canvas){
//            int h = backgroundBitmap.getHeight();
//            int w = backgroundBitmap.getWidth();
            canvas.drawBitmap(BgResources.inst().backgroundBitmap, BgResources.inst().bgScaleMatrix, BgResources.inst().common);
        }
        void drawDotBitmap(Canvas canvas, Bitmap bmp, CPoint.Screen pos){
            BgResources.inst().translate_matrix.reset();

            BgResources.inst().translate_matrix.preTranslate(pos.x - (float)bmp.getWidth() / 2,
                    pos.y  - (float)bmp.getHeight() / 2);

            canvas.drawBitmap(bmp,
                    BgResources.inst().translate_matrix, BgResources.inst().dotPaint);
        }
        void drawDots(Canvas canvas){
            while (BgResources.inst().isDotsArrayLocked){}
            BgResources.inst().isDotsArrayLocked = true;

            for(TheDot dot : BgResources.inst().dotPool){
                drawDotBitmap(canvas, BgResources.inst().getDotBmp(dot.radius, dot.depth, rs), dot.pos);
                dot.onDraw(); // FIXME: 4/16/19
            }

            BgResources.inst().isDotsArrayLocked = false;
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
        depth = random.nextInt(BgResources.inst().dotDepthLevelAmount);

//        pos.x = 500;
//        pos.y = 500;
//
//        moveVector.x = 0;
//        moveVector.y = 0;
//        radius = 3;
//        depth = 4;
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

class BgResources{
    private static BgResources instance = new BgResources();

    // TODO: 4/16/19 make mutex methods
    boolean isDotsArrayLocked = false;

    final Matrix bgScaleMatrix = new Matrix();
    final Matrix translate_matrix = new Matrix();
    ArrayList<TheDot> dotPool = new ArrayList<>();
    Map<Long, Bitmap> dotBitmaps = new HashMap<>();

    int dotDepthLevelAmount = 10;
    float maxDotBlurRadius = 24.f;

    Paint dotPaint = new Paint();
    Paint common = new Paint();

    Bitmap backgroundBitmap;

    static BgResources inst(){
        return instance;
    }

    private BgResources(){

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

    Bitmap makeDotBmp(float dot_radius, float blur_radius, RenderScript rs){
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

        if (!BgResources.inst().dotBitmaps.containsKey(key)){
            float blur_radius = depthLevel * BgResources.inst().maxDotBlurRadius /
                    BgResources.inst().dotDepthLevelAmount;
            BgResources.inst().dotBitmaps.put(key, makeDotBmp(dot_radius, blur_radius, rs));
        }
        return BgResources.inst().dotBitmaps.get(key);
    }

    private static final float BITMAP_SCALE = 1.f;

    public Bitmap blurRs(float radius, Bitmap image, RenderScript rs) {
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