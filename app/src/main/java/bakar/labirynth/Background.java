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
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

public class Background extends SurfaceView implements SurfaceHolder.Callback{

    BgRenderThread renderThread;
    private RenderScript rs = RenderScript.create(getContext());

    public Background(Context _context){
        super(_context);
        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.RGBA_8888);
    }

    public Background(Context _context, AttributeSet set, int defStyle){
        super(_context, set, defStyle);
        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.RGBA_8888);
    }

    public Background(Context _context, AttributeSet set){
        super(_context, set);
        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.RGBA_8888);
    }

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

    class BgRenderThread extends Thread{

        boolean running = false;
        private SurfaceHolder surfaceHolder;
        Random random;

        private long prevDrawTime = 0;
        private long redrawPeriod = 30; // milliS // microS

        private long getTime(){
            return System.nanoTime() / 1_000_000;
        }

        BgRenderThread(){
            Logger.getAnonymousLogger().info("BGRenderThread ctor");
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
//            int color1 = Color.parseColor("#0000FF");
//            int color2 = Color.parseColor("#0000FF");
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
            boolean isCanvasLocked = false;

            while (running) {
                if (getTime() - prevDrawTime > redrawPeriod) {
                    prevDrawTime = getTime();
                    try {
                        if (!isCanvasLocked){
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
    private final Map<Long, Bitmap> dotBitmaps = new HashMap<>();

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

        if (!BgResources.inst().dotBitmaps.containsKey(key)){
            float blur_radius = depthLevel * BgResources.inst().maxDotBlurRadius /
                    BgResources.inst().dotDepthLevelAmount;
            BgResources.inst().dotBitmaps.put(key, makeDotBmp(dot_radius, blur_radius, rs));
        }
        return BgResources.inst().dotBitmaps.get(key);
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

    public Bitmap blurFastGauss(int radius, Bitmap original) {
        int w = original.getWidth();
        int h = original.getHeight();
        int[] pix = new int[w * h];
        original.getPixels(pix, 0, w, 0, 0, w, h);

        for (int r = radius; r >= 1; r /= 2) {
            for (int i = r; i < h - r; i++) {
                for (int j = r; j < w - r; j++) {
                    int tl = pix[(i - r) * w + j - r];
                    int tr = pix[(i - r) * w + j + r];
                    int tc = pix[(i - r) * w + j];
                    int bl = pix[(i + r) * w + j - r];
                    int br = pix[(i + r) * w + j + r];
                    int bc = pix[(i + r) * w + j];
                    int cl = pix[i * w + j - r];
                    int cr = pix[i * w + j + r];

                    pix[(i * w) + j] = 0xFF000000 |
                            (((tl & 0xFF) + (tr & 0xFF) + (tc & 0xFF) + (bl & 0xFF) + (br & 0xFF) + (bc & 0xFF) + (cl & 0xFF) + (cr & 0xFF)) >> 3) & 0xFF |
                            (((tl & 0xFF00) + (tr & 0xFF00) + (tc & 0xFF00) + (bl & 0xFF00) + (br & 0xFF00) + (bc & 0xFF00) + (cl & 0xFF00) + (cr & 0xFF00)) >> 3) & 0xFF00 |
                            (((tl & 0xFF0000) + (tr & 0xFF0000) + (tc & 0xFF0000) + (bl & 0xFF0000) + (br & 0xFF0000) + (bc & 0xFF0000) + (cl & 0xFF0000) + (cr & 0xFF0000)) >> 3) & 0xFF0000;
                }
            }
        }
        original.setPixels(pix, 0, w, 0, 0, w, h);
        return original;
    }

    public Bitmap blurSuperFast(int radius, Bitmap original) {
        int w = original.getWidth();
        int h = original.getHeight();
        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;
        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, p1, p2, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];
        int vmax[] = new int[Math.max(w, h)];
        int[] pix = new int[w * h];

        original.getPixels(pix, 0, w, 0, 0, w, h);

        int dv[] = new int[256 * div];
        for (i = 0; i < 256 * div; i++) {
            dv[i] = (i / div);
        }

        yw = yi = 0;

        for (y = 0; y < h; y++) {
            rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                rsum += (p & 0xff0000) >> 16;
                gsum += (p & 0x00ff00) >> 8;
                bsum += p & 0x0000ff;
            }
            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                    vmax[x] = Math.max(x - radius, 0);
                }
                p1 = pix[yw + vmin[x]];
                p2 = pix[yw + vmax[x]];

                rsum += ((p1 & 0xff0000) - (p2 & 0xff0000)) >> 16;
                gsum += ((p1 & 0x00ff00) - (p2 & 0x00ff00)) >> 8;
                bsum += (p1 & 0x0000ff) - (p2 & 0x0000ff);
                yi++;
            }
            yw += w;
        }

        for (x = 0; x < w; x++) {
            rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                rsum += r[yi];
                gsum += g[yi];
                bsum += b[yi];
                yp += w;
            }
            yi = x;
            for (y = 0; y < h; y++) {
                pix[yi] = 0xff000000 | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                if (x == 0) {
                    vmin[y] = Math.min(y + radius + 1, hm) * w;
                    vmax[y] = Math.max(y - radius, 0) * w;
                }
                p1 = x + vmin[y];
                p2 = x + vmax[y];

                rsum += r[p1] - r[p2];
                gsum += g[p1] - g[p2];
                bsum += b[p1] - b[p2];

                yi += w;
            }
        }

        original.setPixels(pix, 0, w, 0, 0, w, h);

        return original;
    }

    public Bitmap blurStack(int radius, Bitmap original) {
        if (radius < 1) {
            return (null);
        }

        int w = original.getWidth();
        int h = original.getHeight();

        int[] pix = new int[w * h];
        original.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }
        original.setPixels(pix, 0, w, 0, 0, w, h);
        return (original);
    }

    public Bitmap blurBox(int radius, Bitmap bmp) {
        assert (radius & 1) == 0 : "Range must be odd.";

        Bitmap blurred = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(blurred);

        int w = bmp.getWidth();
        int h = bmp.getHeight();

        int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);

        boxBlurHorizontal(pixels, w, h, radius / 2);
        boxBlurVertical(pixels, w, h, radius / 2);

        c.drawBitmap(pixels, 0, w, 0.0F, 0.0F, w, h, true, null);

        return blurred;
    }

    private void boxBlurHorizontal(int[] pixels, int w, int h,
                                   int halfRange) {
        int index = 0;
        int[] newColors = new int[w];

        for (int y = 0; y < h; y++) {
            int hits = 0;
            long r = 0;
            long g = 0;
            long b = 0;
            for (int x = -halfRange; x < w; x++) {
                int oldPixel = x - halfRange - 1;
                if (oldPixel >= 0) {
                    int color = pixels[index + oldPixel];
                    if (color != 0) {
                        r -= Color.red(color);
                        g -= Color.green(color);
                        b -= Color.blue(color);
                    }
                    hits--;
                }

                int newPixel = x + halfRange;
                if (newPixel < w) {
                    int color = pixels[index + newPixel];
                    if (color != 0) {
                        r += Color.red(color);
                        g += Color.green(color);
                        b += Color.blue(color);
                    }
                    hits++;
                }

                if (x >= 0) {
                    newColors[x] = Color.argb(0xFF, (int) (r / hits), (int) (g / hits), (int) (b / hits));
                }
            }

            System.arraycopy(newColors, 0, pixels, index + 0, w);

            index += w;
        }
    }

    private void boxBlurVertical(int[] pixels, int w, int h,
                                 int halfRange) {

        int[] newColors = new int[h];
        int oldPixelOffset = -(halfRange + 1) * w;
        int newPixelOffset = (halfRange) * w;

        for (int x = 0; x < w; x++) {
            int hits = 0;
            long r = 0;
            long g = 0;
            long b = 0;
            int index = -halfRange * w + x;
            for (int y = -halfRange; y < h; y++) {
                int oldPixel = y - halfRange - 1;
                if (oldPixel >= 0) {
                    int color = pixels[index + oldPixelOffset];
                    if (color != 0) {
                        r -= Color.red(color);
                        g -= Color.green(color);
                        b -= Color.blue(color);
                    }
                    hits--;
                }

                int newPixel = y + halfRange;
                if (newPixel < h) {
                    int color = pixels[index + newPixelOffset];
                    if (color != 0) {
                        r += Color.red(color);
                        g += Color.green(color);
                        b += Color.blue(color);
                    }
                    hits++;
                }

                if (y >= 0) {
                    newColors[y] = Color.argb(0xFF, (int) (r / hits), (int) (g / hits), (int) (b / hits));
                }

                index += w;
            }

            for (int y = 0; y < h; y++) {
                pixels[y * w + x] = newColors[y];
            }
        }
    }
}