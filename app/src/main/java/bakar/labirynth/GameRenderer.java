package bakar.labirynth;

import android.app.Activity;import android.app.ActivityManager;
import android.arch.core.util.Function;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.Range;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static android.graphics.Path.Direction.CW;

/**
 * Created by Bakar on 10.03.2018.
 */

public class GameRenderer extends SurfaceView implements SurfaceHolder.Callback{

    // IMPORT /////////////////////////////
    boolean isDebug = false;
    ///////////////////////////////////////

    CPoint.Game lastToushGc = new CPoint.Game();
    CPoint.Screen lastToushSc = new CPoint.Screen();

    boolean fogEnabled = false;

    boolean registerBonusTouch = false;

    float smallSquareScale = 0.5f;
    float bigSquareScale = 2.f - smallSquareScale;

    static final float cellSize = 10; // gameCoords side

    ///// это костыль. позволяет сделать уменьшенную бмп уровня, не затрагивая остальное ///
    final float labBitmapcellSize = 4;
    ////////////////////////////////////////////////////////////////////////////////////////

    final Camera camera = new Camera();
    public float playerHitbox = 0; // screenCoord // in surfaceCreated();

    float max_scale = 6;
    float min_scale;

    public boolean isMovingOffset = false;
    public boolean isMovingPlayer = false;
    float enlightenRadius = cellSize * 200 / 70; // gamecoord

    private GameLogic gameLogic;
    private RenderThread renderThread;

    private Context context;

    ArrayList<Button> buttons = new ArrayList<>();
    ArrayList<PickUpAnimation> pickUpAnimations = new ArrayList<>();

    void constructor(Context _context){
        Logger.getAnonymousLogger().info("GameRenderer.ctor p begin");
        setZOrderOnTop(true);
        context = _context;
        camera.setLocation(0,0,10);
        camera.save();
        getHolder().setKeepScreenOn(true);
        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.RGBA_8888);

        renderThread = new RenderThread();
        renderThread.start();
        Logger.getAnonymousLogger().info("GameRenderer.ctor p end");
    }

    public GameRenderer(Context _context) {
        super(_context);
        constructor(_context);
    }

    public GameRenderer(Context _context, AttributeSet set, int defStyle){
        super(_context, set, defStyle);
        constructor(_context);
    }
    public GameRenderer(Context _context, AttributeSet set){
        super(_context, set);
        constructor(_context);
    }
    void addPickUpAnimation(CPoint.Game pointF, String text){
        pickUpAnimations.add(new PickUpAnimation(pointF, text));
    }
    public void setGameLogic(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }
    public float getCellSize() {
        return cellSize;
    }
    void startBonusActivity(){
        Intent intent = new Intent(getContext(), BonusActivity.class);
        intent.putExtra("pathfinderAmount", gameLogic.pathfinderAmount);
        intent.putExtra("teleportAmount", gameLogic.teleportAmount);
        intent.putExtra("pointerAmount", gameLogic.pointerAmount);
        ((Activity)getContext()).startActivityForResult(intent, BonusActivity.class.toString().hashCode());
    }
    void startSettingsActivity(){
        Intent intent = new Intent(getContext(), SettingsActivity.class);
        ((Activity)getContext()).startActivityForResult(intent,
                SettingsActivity.class.toString().hashCode());
    }

    boolean isFieldAtScreen(){
        float margin = playerHitbox / 2;
        CPoint.Screen left_top_of_scr = new CPoint.Screen(margin, margin);
        CPoint.Game left_top_of_scr_g = screen2game(left_top_of_scr);
        CPoint.Screen right_bot_of_scr = new CPoint.Screen(getWidth() - margin, getHeight() - margin);
        CPoint.Game right_bot_of_scr_g = screen2game(right_bot_of_scr);

        RectF screen = new RectF(left_top_of_scr_g.x,
                left_top_of_scr_g.y,
                right_bot_of_scr_g.x,
                right_bot_of_scr_g.y
        );

        CPoint.Field right_bot_of_lab = new CPoint.Field(gameLogic.field.getxSize(), gameLogic.field.getySize());
        CPoint.Game right_bot_of_lab_g = field2game(right_bot_of_lab);

        RectF field = new RectF(0,
                0,
                right_bot_of_lab_g.x,
                right_bot_of_lab_g.y);

        return screen.intersect(field);
    }
    boolean bonusTouch(CPoint.Screen screenCoord){
        CPoint.Game gc = screen2game(screenCoord);
        if (!gameLogic.isCoordIsWithinField(gc)){
            return false;
        }

        if (gameLogic.pathfinderActive){
            if (GameLogic.distance(gameLogic.playerCoords(), gc) < gameLogic.getPathfinderRadius()){
                gameLogic.activatePathfinder(gc);
                return true;
            }
            else
                return false;

        }

        if (gameLogic.teleportActive){
            if (GameLogic.distance(gameLogic.playerCoords(), gc) < gameLogic.getTeleportRadius()) {
                gameLogic.activateTeleport(screen2game(screenCoord));
                return true;
            }
            else
                return false;
        }

        return false;
    }

    private RectF pt2rect(CPoint.Game pt){
        return pt2RectScaled(pt, 1.0f, cellSize);
    }
    private RectF pt2rect(CPoint.Screen pt){
        RectF rect = new RectF();

        CPoint.Game n_pt = screen2game(pt);

        rect.set((-cellSize / 2) / getGlobalScale(),
                (-cellSize / 2) / getGlobalScale(),
                (cellSize / 2) / getGlobalScale(),
                (cellSize / 2)/ getGlobalScale());
        rect.offset(n_pt.x, n_pt.y);

        return rect;
    }
    private RectF pt2RectScaled(CPoint.Game pt, float scale, float _cellSize){
        RectF rect = new RectF();

        rect.set(-scale * _cellSize / 2,
                -scale * _cellSize / 2,
                scale * _cellSize / 2,
                scale * _cellSize / 2);
        rect.offset(pt.x, pt.y);

        return rect;
    }
    private RectF pt2rect(CPoint.Game pt1, CPoint.Game pt2, float scale, float _cellSize){
        float left = Math.min(pt1.x, pt2.x) - (scale * _cellSize) / 2;
        float right = Math.max(pt1.x, pt2.x) + (scale * _cellSize) / 2;
        float top = Math.min(pt1.y, pt2.y) - (scale * _cellSize) / 2;
        float bot = Math.max(pt1.y, pt2.y) + (scale * _cellSize) / 2;

        RectF res = new RectF(left, top, right, bot);
        return res;
    }

    boolean threadIsStarted(){
        return renderThread.is_started;
    }
    boolean threadIsWaiting4Surface(){
        return renderThread.is_waiting_4_surface;
    }
    boolean threadIsDoingPreparations(){
        return renderThread.is_doing_preparations;
    }
    void prepareNewLevel(){
        Logger.getAnonymousLogger().info("RenderThread prepareNewLevel()");
        renderThread.need_2_prepare_new_level = true;
    }
    boolean threadIsPrepairingNewLevel(){
        return renderThread.need_2_prepare_new_level;
    }

    boolean isPlayerInSight(){
        PointF pl = game2screen(gameLogic.playerCoords());
        return  (pl.x > 0 && pl.y > 0 && pl.x < getWidth() && pl.y < getHeight());
    }
    void lightFog(CPoint.Game gc){
        renderThread.enlightenFogBmp(gc);
    }
    void resetFog(){
        renderThread.createFogBmp();
        renderThread.resizeFogBmp();
    }
    void resetLab(){renderThread.clearLabyrinthBmp();}
    void createJoystick(){
        float rad = getWidth() / 10.f;
        CPoint.Screen pos = new CPoint.Screen(getWidth() - rad * 2, getHeight() - rad * 2);
        gameLogic.createJoystick(pos, rad);
    }

    float getGlobalScale(){
        Matrix matrix = new Matrix();
        camera.getMatrix(matrix);
        float[] values = new float[9];
        matrix.getValues(values);
        return values[0];
    }
    PointF getGlobalOffset(){
        Matrix matrix = new Matrix(); //TODO убрать генерацию. очень часто вызывается
        camera.getMatrix(matrix);
        float[] values = new float[9];
        matrix.getValues(values);
        return new PointF(values[2], values[5]);
    }

    void centerCameraTo(CPoint.Game pt){
        CPoint.Game old_center = screen2game(new CPoint.Screen(getWidth() / 2.f,
                getHeight() / 2.f));

        PointF offset = new PointF((pt.x - old_center.x) * getGlobalScale(),
                (pt.y - old_center.y) * getGlobalScale());

        changeOffset(offset);
    }
    void centerCameraBetweenPlayerExit(){
        PointF ofset = new PointF();
        ofset.x = gameLogic.exitCoords().x;
        ofset.y = gameLogic.exitCoords().y;
        ofset.x -= gameLogic.playerCoords().x;
        ofset.y -= gameLogic.playerCoords().y;

        CPoint.Game result = new CPoint.Game();
        result.x = gameLogic.playerCoords().x + ofset.x / 2.f;
        result.y = gameLogic.playerCoords().y + ofset.y / 2.f;

        centerCameraTo(result);
    }

    boolean isPlayerAndExitAtScreen(){
        PointF ofseted_exit = new PointF();
        ofseted_exit.x = gameLogic.exitCoords().x;
        ofseted_exit.y = gameLogic.exitCoords().y;
        ofseted_exit.x -= gameLogic.playerCoords().x;
        ofseted_exit.y -= gameLogic.playerCoords().y;
        ofseted_exit.x = Math.abs(ofseted_exit.x);
        ofseted_exit.y = Math.abs(ofseted_exit.y);

        return ofseted_exit.x < getWidth() && ofseted_exit.y < getHeight();
    }

    // Converters /////////////////////////////////////////////////////////////////////////////////
    public CPoint.Game screen2game(CPoint.Screen value){
        CPoint.Game result = new CPoint.Game();
        result.set((value.x - getGlobalOffset().x) / getGlobalScale(),
                (value.y - getGlobalOffset().y) / getGlobalScale());
        return result;
    }
    public CPoint.Field game2field(CPoint.Game value){
        CPoint.Field result = new CPoint.Field();
        double x, y;
        x = Math.floor(value.x / cellSize);
        y = Math.floor(value.y / cellSize);
        result.set((int)x, (int)y);
        return result;
    }
    public CPoint.Field screen2field(CPoint.Screen value){
        return game2field(screen2game(value));
    }
    //
    public CPoint.Game field2game(CPoint.Field value){
        CPoint.Game result = new CPoint.Game();
        result.set(value.x * cellSize + cellSize / 2, value.y * cellSize + cellSize / 2);
        return result;
    }
    public CPoint.Screen game2screen(CPoint.Game value){

        CPoint.Screen result = new CPoint.Screen();
        result.set(value.x * getGlobalScale() + getGlobalOffset().x ,
                value.y * getGlobalScale() + getGlobalOffset().y);
        return result;
    }
    public CPoint.Screen field2screen(CPoint.Field value){
        return game2screen(field2game(value));
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

    // Touch //////////////////////////////////////////////////////////////////////////////////////
    void onTouchDown(MotionEvent.PointerCoords pointerCoords){
        CPoint.Screen pointF = new CPoint.Screen(pointerCoords.x, pointerCoords.y); // screenCoords
        lastToushSc.x = pointF.x;
        lastToushSc.y = pointF.y;
        lastToushGc = screen2game(lastToushSc);
        if (gameLogic.pathfinderActive || gameLogic.teleportActive){
            registerBonusTouch = true;
            isMovingOffset = true;
        }
        else{
            if (gameLogic.canMovePlayer(pointF)){
                isMovingPlayer = true;
                if (gameLogic.usesJoystick)
                    gameLogic.joystick.lastTouch = screen2game(pointF);
            }
            else
            {
                boolean buttonClicked = false;
                for (Button bt:buttons){
                    buttonClicked = bt.tryClick(pointF);
                    if (buttonClicked) break;
                }
                isMovingOffset = !buttonClicked; // исп в touchListener
            }
        }
    }
    void onTouchUp(PointF screenCoord){
        CPoint.Screen pt = new CPoint.Screen();
        if (screenCoord != null) {
            pt.x = screenCoord.x;
            pt.y = screenCoord.y;
        }

        if (gameLogic.pathfinderActive || gameLogic.teleportActive){
            if (screenCoord != null && registerBonusTouch) {
                bonusTouch(pt);
                gameLogic.pathfinderActive = false;
                gameLogic.teleportActive = false;
            }

        }
        registerBonusTouch = false;
        isMovingPlayer = false;
        isMovingOffset = false;
        //renderThread.clearLabyrinthBmp();
        if (gameLogic.usesJoystick)
            gameLogic.joystick.touchReleased();
    }
    //
    void movePlayer(PointF pointSc){
        CPoint.Screen pt = new CPoint.Screen();
        pt.x = pointSc.x;
        pt.y = pointSc.y;
        if (gameLogic.usesJoystick)
            gameLogic.joystick.lastTouch = screen2game(pt);
        else
            gameLogic.movePlayerTo(screen2game(pt));
    }
    void changeOffset(PointF offset){
        camera.save();
        registerBonusTouch = false;
        camera.translate(-offset.x / getGlobalScale(), offset.y / getGlobalScale(), 0);
        if (!isFieldAtScreen())
            camera.restore();
    }
    void changeScale(float value){
        camera.save();

        CPoint.Game old_center = screen2game(new CPoint.Screen(getWidth() / 2.f,
                getHeight() / 2.f));

        camera.translate(0,0, -value * (cellSize / 40));
        if (getGlobalScale() > max_scale || getGlobalScale() < min_scale)
            camera.restore();
        if (renderThread.fogBmpScale != getGlobalScale())
            renderThread.resizeFogBmp();

        CPoint.Game new_center = screen2game(new CPoint.Screen(getWidth() / 2.f,
                getHeight() / 2.f));

        PointF offset = new PointF((old_center.x - new_center.x) * getGlobalScale(),
                (old_center.y - new_center.y) * getGlobalScale());

        changeOffset(offset);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logger.getAnonymousLogger().info("GameRenderer.surfaceChanged()");
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Logger.getAnonymousLogger().info("GameRenderer.surfaceCreated() begin");
        min_scale = .6f * getWidth() / (gameLogic.field.getxSize() * cellSize);
        max_scale = 6;
        while (getGlobalScale() < min_scale)
            camera.translate(0,0,10);

        float rad1 = (float)getWidth() / 20;
        if (buttons.size() < 1){
            CPoint.Screen pos1 = new CPoint.Screen(rad1 * 2, getHeight() - rad1 * 2);
            buttons.add(new PlayerFinder(pos1, rad1));
        }


        if (StoredProgress.getInstance().getValue(StoredProgress.levelUpgKey) < 4){
            centerCameraBetweenPlayerExit();
        }
        else{
            centerCameraTo(gameLogic.playerCoords());
        }

        if (buttons.size() < 2) {
            CPoint.Screen pos2 = new CPoint.Screen(rad1 * 2, getHeight() - rad1 * 5);
            buttons.add(new Bonuses(pos2, rad1));
        }

        if (buttons.size() < 3) {
            CPoint.Screen pos3 = new CPoint.Screen(getWidth() - rad1 * 2, rad1 * 2);
            buttons.add(new MenuButton(pos3, rad1));
        }

        if (buttons.size() < 4) {
            CPoint.Screen pos4 = new CPoint.Screen(getWidth() - rad1 * 5, rad1 * 2);
            buttons.add(new Settings(pos4, rad1));
        }

        gameLogic.eFactory.init();
        if (gameLogic.usesJoystick){
            createJoystick();
        }

         // тред создается последним
        renderThread.setHolder(getHolder());
        renderThread.setRunning(true);
        renderThread.bitmaps.rescaleAll();

        playerHitbox = (float)getWidth() / 10.f;

        renderThread.resizeFogBmp();
        renderThread.enlightenFogBmp(gameLogic.playerCoords());

        renderThread.is_waiting_4_surface = false;

        Logger.getAnonymousLogger().info("GameRenderer.surfaceCreated() end");
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        renderThread.is_waiting_4_surface = true;
        Logger.getAnonymousLogger().info("GameRenderer.surfaceDestroyed() begin");
//        boolean retry = true;
//        renderThread.setRunning(false);
//        while (retry) {
//            try {
//                renderThread.join();
//                retry = false;
//            } catch (InterruptedException e) {
//            }
//        }
        Logger.getAnonymousLogger().info("GameRenderer.surfaceDestroyed() end");
    }

    private class RenderThread extends Thread{
        boolean is_started = false;
        boolean is_waiting_4_surface = true;
        boolean is_doing_preparations = false;
        boolean need_2_prepare_new_level = false;

        private boolean running = false;
        private SurfaceHolder surfaceHolder;
        private Profiler profiler = new Profiler();
        final Matrix Ematrix = new Matrix();
        final Matrix fogMatrix = new Matrix();
        final Matrix translate_matrix = new Matrix();

        private long prevDrawTime = 0;
        private long redrawPeriod = 30; // milliS // microS
        private long fpsTimer = 0;

        private Bitmap labBitmap;
        private Bitmap fogBitmap;
        private float fogBmpScale = -1;
        private Bitmap pointerBitmap;
        private boolean canClearBmp = true;
        void resizeFogBmp(){
            if (fogBitmap == null) {
                return;
            }
            CPoint.Screen bmp_size =
                    new CPoint.Screen((cellSize * gameLogic.field.getxSize()) * getGlobalScale(),
                            (cellSize * gameLogic.field.getySize() - 1) * getGlobalScale());

            fogBitmap = Bitmap.createScaledBitmap(fogBitmap,
                    Math.round(bmp_size.x),
                    Math.round(bmp_size.y),
                    true);
            fogBmpScale = getGlobalScale();
        }

        AnimationBitmaps bitmaps = new AnimationBitmaps();

        //Todo: перенести Paint в ресурсы цвета
        private Paint common = new Paint();
        private Paint floor = new Paint();
        private Paint wall = new Paint();
        private Paint node = new Paint();
        private Paint player = new Paint();
        private Paint hitbox = new Paint();
        private Paint trace = new Paint();
        private Paint text = new Paint();
        private Paint joystickMain = new Paint();
        private Paint joystickCurrent = new Paint();
        private Paint exit = new Paint();
        private Paint button = new Paint();
        private Paint path = new Paint();
        private Paint fog = new Paint();
        private Paint enlighten = new Paint();
        private Paint puanim = new Paint();
        private Paint levelNumber = new Paint();
        private Paint bonusRadius = new Paint();
        private ExitDrawer exitDrawer = new ExitDrawer();

        RenderThread(){
            Logger.getAnonymousLogger().info("RenderThread.ctor begin");
            floor.setColor(Color.argb(120,30, 30, 30));
            wall.setColor(Color.argb(240,230, 230, 230));
            node.setColor(Color.rgb(56, 255, 24));
            node.setStrokeWidth(.5f);
            node.setStyle(Paint.Style.STROKE);
            player.setColor(Color.rgb(255, 0, 255));
            hitbox.setColor(Color.RED);
            hitbox.setStyle(Paint.Style.FILL);
            trace.setColor(Color.YELLOW);
            text.setColor(Color.GRAY);
            text.setStrokeWidth(8);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(70);
            joystickMain.setColor(Color.argb(120, 240, 240, 240));
            joystickCurrent.setColor(Color.rgb(230, 255, 255));
            exit.setColor(Color.rgb(40, 170, 40));
            exit.setStrokeWidth(1.f);
            exit.setStyle(Paint.Style.STROKE);
            button.setColor(Color.argb(120, 255, 255, 255));
            path.setColor(Color.argb(120, 10, 255, 10));
            fog.setColor(Color.rgb(20, 20, 30));
            enlighten.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            enlighten.setColor(Color.argb(0, 60, 50, 60));
            puanim.setColor(Color.WHITE);
            puanim.setTextAlign(Paint.Align.CENTER);
            puanim.setTextSize(20);
            puanim.setTypeface(Typeface.createFromAsset(getContext().getAssets(),  "fonts/trench100free.ttf"));

            levelNumber.setColor(Color.WHITE);
            levelNumber.setTextAlign(Paint.Align.RIGHT);
            levelNumber.setTextSize(95);
            levelNumber.setTypeface(Typeface.createFromAsset(getContext().getAssets(),  "fonts/trench100free.ttf"));

            bonusRadius.setColor(Color.argb(220, 40, 255, 40));

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;

            random = new Random(System.currentTimeMillis());

            createFogBmp();
            //updateLabyrinthBmp();
            //updateBackgroundBmp();
            Logger.getAnonymousLogger().info("RenderThread.ctor end");
        }
        void doB4Surface(){
            is_doing_preparations = true;
            Logger.getAnonymousLogger().info("RenderThread.doB4Surface() begin");
            updateLabyrinthBmp();
            Logger.getAnonymousLogger().info("RenderThread.doB4Surface() end");
            is_doing_preparations = false;
        }

        private long getTime(){
            return System.nanoTime() / 1_000_000;
        }
        float calcFps(){
            float result = 1000 / (getTime() - fpsTimer);
            fpsTimer = getTime();
            return result;
        }
        float availMemory(){
            final float BYTES_IN_MB = 1024.0f * 1024.0f;

            final Runtime rt = Runtime.getRuntime();
            float bytesUsed = rt.totalMemory();
            float bytesTotal = rt.maxMemory();

            float freeMemoryInMB = (bytesTotal - bytesUsed) / BYTES_IN_MB;
            return freeMemoryInMB;
        }
        private void createFogBmp(){
            if (!fogEnabled) return;
            int xsize = Math.round(gameLogic.field.getxSize() * cellSize);
            int ysize = Math.round(gameLogic.field.getySize() * cellSize);
            fogBitmap = Bitmap.createBitmap(xsize, ysize, Bitmap.Config.ALPHA_8);
            Canvas canvas = new Canvas(fogBitmap);

            canvas.drawRect(0, 0, xsize, ysize, fog);
        }
        private void enlightenFogBmp(CPoint.Game old_pt){
            if (fogBitmap == null || !fogEnabled) return;

            PointF pt = new PointF(old_pt.x * getGlobalScale(), old_pt.y * getGlobalScale());

            Canvas canvas = new Canvas(fogBitmap);
            float scale = 2 * getGlobalScale();
            float rad = enlightenRadius * scale;
            int steps = 5;
            int alpha = 2;
            while (rad > enlightenRadius){
                enlighten.setColor(Color.argb(alpha, 60, 50, 60));
                canvas.drawCircle(pt.x, pt.y, rad, enlighten);
                alpha += 5;
                rad -= (enlightenRadius * scale - enlightenRadius) / steps;
            }
            enlighten.setColor(Color.argb(230, 60, 50, 60));
            canvas.drawCircle(pt.x, pt.y, enlightenRadius, enlighten);
        }
        void clearLabyrinthBmp(){
            long start_time = getTime(); //сразу может не удалится, поэтому пробуем некоторое время
            while (null != labBitmap && (getTime() - start_time) < (redrawPeriod * 2))
                if (canClearBmp)
                    labBitmap = null;
        }
        void updateLabyrinthBmp(){
            // рисуется в игровых координатах, при отрисовке умножается на матрицу
            while(gameLogic == null ||gameLogic.field == null){
                try {
                    sleep(1);
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            labBitmap = Bitmap.createBitmap(Math.round(gameLogic.field.getxSize() * labBitmapcellSize),
                    Math.round(gameLogic.field.getySize() * labBitmapcellSize), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(labBitmap);

            Function<CPoint.Field, CPoint.Game> f2g_f =
                    (CPoint.Field fp) -> {
                        CPoint.Game result = new CPoint.Game();
                        result.set(fp.x * labBitmapcellSize + labBitmapcellSize / 2,
                                fp.y * labBitmapcellSize + labBitmapcellSize / 2);
                        return result;
                    };

            canvas.drawRect(0,
                    0,
                    labBitmap.getWidth(),
                    labBitmap.getHeight(),
                    floor);

            Path poly = new Path();
            for (int x = 0; x < gameLogic.field.getxSize(); ++x){
                for (int y = 0; y < gameLogic.field.getySize(); ++y){
                    CPoint.Game rectScreenPt = f2g_f.apply(new CPoint.Field(x, y));
                    if (!gameLogic.field.get(x, y)){
                        poly.addRect(pt2RectScaled(rectScreenPt, smallSquareScale, labBitmapcellSize), CW);

                        for (Direction dir:Direction.values()){
                            CPoint.Field another = gameLogic.field.ptAt(new CPoint.Field(x,y), dir);
                            if (!gameLogic.field.get(another)){
                                poly.addRect(pt2rect(
                                        rectScreenPt,
                                        f2g_f.apply(another),
                                        smallSquareScale,
                                        labBitmapcellSize),
                                        CW);
                            }
                        }

                    }
                }
            }
            CPoint.Game topLeft = new CPoint.Game(0,0);
            CPoint.Game topRight = new CPoint.Game(labBitmap.getWidth(),0);
            CPoint.Game botRight = new CPoint.Game(labBitmap.getWidth(),labBitmap.getHeight());
            CPoint.Game botLeft = new CPoint.Game(0,labBitmap.getHeight());

            poly.addRect(pt2rect(topLeft, topRight, smallSquareScale, labBitmapcellSize), CW);
            poly.addRect(pt2rect(topLeft, botLeft, smallSquareScale, labBitmapcellSize), CW);
            poly.addRect(pt2rect(botRight, botLeft, smallSquareScale, labBitmapcellSize), CW);
            poly.addRect(pt2rect(botRight, topRight, smallSquareScale, labBitmapcellSize), CW);
            canvas.drawPath(poly, wall);
        }

        Random random;

        void drawTile(Canvas canvas, Bitmap bmp, CPoint.Game pos, boolean isLarge){
            translate_matrix.reset();
            if (isLarge)
            {
                translate_matrix.preTranslate(pos.x - cellSize * bigSquareScale,
                        pos.y  - cellSize * bigSquareScale);
                // из преобразования в AnimationBitmaps
                translate_matrix.preScale(2 * bigSquareScale / max_scale,
                        2 * bigSquareScale / max_scale);
            }
            else
            {
                translate_matrix.preTranslate(pos.x - cellSize * bigSquareScale / 2,
                        pos.y  - cellSize * bigSquareScale / 2);
                translate_matrix.preScale(1 * bigSquareScale / max_scale,
                        1 * bigSquareScale / max_scale);
            }

            canvas.drawBitmap(bmp,
                    translate_matrix, common);
        }
        void drawDebugText(Canvas canvas){
            if (!isDebug)
                return;
            final ArrayList<String> outputs = new ArrayList<>();

            outputs.add("xSize: " + gameLogic.field.getxSize());
            outputs.add("ySize: " + gameLogic.field.getySize());

            if (gameLogic.tiltControler != null){
                outputs.add("Gravity: " + gameLogic.tiltControler.getCurrentVelocity());
            }

            outputs.add("FPS: " + calcFps());

            for (int i = 0; i < outputs.size(); ++i){
                canvas.drawText(outputs.get(i), getWidth(), (i + 1) * 70, text);
            }

            if (profiler.size() != 0)
            canvas.drawText(profiler.getName(0) + ": " + profiler.getTime(0) + " us",
                    getWidth(), (outputs.size() + 1) * 70, text);

            for (int i = 1; i < profiler.size(); ++i){
                canvas.drawText(profiler.getName(i) + ": " + profiler.getPercents(i) + "%",
                        getWidth(), (i + outputs.size() + 1) * 70, text);
            }
        }
        void drawDebug(Canvas canvas){
//            if (!isDebug)
//                return;
//            canvas.drawRect(pt2rect(lastToushGc), player);
//            canvas.drawRect(pt2rect(lastToushSc), trace);
        }
        void drawPickedUpAnimation(Canvas canvas){
            // FIXME: 12/31/18 размер шрифта
            // что с размером шрифта?
            for (int i = 0; i < pickUpAnimations.size(); i++) {
                if (pickUpAnimations.get(i).isOld()){
                    pickUpAnimations.remove(i);
                    i--;
                }
                else{
                    CPoint.Game pos = pickUpAnimations.get(i).getPos();
                    canvas.drawText(pickUpAnimations.get(i).text,
                            pos.x, pos.y, puanim);
                }
            }
        }
        void drawPointer(Canvas canvas){
            if (!gameLogic.pointerActive) return;
            if (pointerBitmap == null)
                pointerBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.pointer);

            if (GameLogic.distance(gameLogic.playerCoords(), gameLogic.exitCoords()) < cellSize * 3){
                return;
            }

            Matrix matrix = new Matrix();
            float size = cellSize * 2.f;
            matrix.postScale((size / pointerBitmap.getHeight()),
                    (size / pointerBitmap.getHeight()));
            CPoint.Game offset_g = gameLogic.playerCoords();
            // смещаем текстуру так, чтобы она была сбоку
            offset_g.offset(size, 0);
            offset_g.offset(-size / 2, -size / 2);

            matrix.postTranslate(offset_g.x, offset_g.y);

            // 0 градусов это напрвление направо
            PointF vec1 = new PointF(gameLogic.exitCoords().x - gameLogic.playerCoords().x,
                    gameLogic.exitCoords().y - gameLogic.playerCoords().y);
            PointF vec2 = new PointF(1.f, 0.f);

            // ищем на какой угол надо повернуть битмап
            // угол между векторами, которые направлены направо и на выход
            float scalar = vec1.x * vec2.x + vec1.y * vec2.y;
            float mod1 = (float)Math.sqrt((vec1.x * vec1.x + vec1.y * vec1.y));
            float mod2 = (float)Math.sqrt((vec2.x * vec2.x + vec2.y * vec2.y));
            float cos = scalar / (mod1 * mod2);
            float deg = 180 * (float)Math.acos(cos) / 3.1415f;
            if (gameLogic.playerCoords().y > gameLogic.exitCoords().y)
                deg = -deg;

            matrix.postRotate(deg, gameLogic.playerCoords().x, gameLogic.playerCoords().y);

            canvas.drawBitmap(pointerBitmap, matrix, common);

        }
        void drawTraces(Canvas canvas){
            for (int i = 0; i < gameLogic.traces.size(); ++i){
                canvas.drawRect(pt2rect(field2game(((LinkedList<CPoint.Field>)gameLogic.traces).get(i))), trace);
            }
        }
        void drawJoystick(Canvas canvas){
            if (gameLogic.joystick == null || !gameLogic.usesJoystick)
                return;
            canvas.drawCircle(gameLogic.joystick.mainPos.x, gameLogic.joystick.mainPos.y,
                    gameLogic.joystick.mainRadius, joystickMain);

            canvas.drawCircle(gameLogic.joystick.curPos.x, gameLogic.joystick.curPos.y,
                    gameLogic.joystick.stickRadius, joystickCurrent);
        }
        void drawPlayer(Canvas canvas){
            drawTile(canvas,
                    bitmaps.getByName("Player"),
                    gameLogic.playerCoords(),
                    false);
            //canvas.drawRect(pt2rect(gameLogic.playerCoords()), player);
        }
        void drawButtons(Canvas canvas){
            final Matrix matrix = new Matrix();
            for (Button bt : buttons){
                matrix.reset();
                float bmpSize = bitmaps.getByName(bt.whoami).getWidth();

                float color_brightness = 1.f - Math.abs(.5f - bt.getAnimtionPercent()) * 2.f;
                int anim_depth = 255;

                button.setColor(Color.argb(120 + Math.round(70 * color_brightness),
                        255 - Math.round(anim_depth * color_brightness),
                        255,
                        255 - Math.round(anim_depth * color_brightness)));

                matrix.preTranslate(bt.pos.x - bmpSize / 2,
                        bt.pos.y - bmpSize / 2);

                canvas.drawCircle(bt.pos.x, bt.pos.y, bt.rad, button);
                canvas.drawBitmap(bitmaps.getByName(bt.whoami), matrix, common);
            }
        }
        void drawPlayerHitbox(Canvas canvas){
            if (!isDebug)
                return;

            PointF pointF = gameLogic.playerCoords();
            canvas.drawCircle(pointF.x, pointF.y, playerHitbox / getGlobalScale(), hitbox);
        }
        void drawRails(Canvas canvas){
            if (!isDebug)
                return;
            PointF first = field2game(gameLogic.currentNode.pos);
            PointF second;
            for (int i = 0; i < gameLogic.currentNode.links.size(); ++i) {
                second = field2game(gameLogic.currentNode.links.valueAt(i).pos);
                canvas.drawLine(first.x, first.y, second.x, second.y, node);
            }

            CPoint.Game pl = gameLogic.playerCoords();
            CPoint.Game touch = gameLogic.debugTouchGameCoord;
            canvas.drawLine(pl.x, pl.y, touch.x, touch.y, node);

        }
        void drawNodes(Canvas canvas){
            if (!isDebug)
                return;

            canvas.drawRect(pt2rect(field2game(gameLogic.currentNode.pos)), node);
            for (int i = 0; i < gameLogic.currentNode.links.size(); ++i){
                canvas.drawRect(pt2rect(field2game(gameLogic.currentNode.links.valueAt(i).pos)), node);
            }
        }
        void drawLabyrinth(Canvas canvas){
            if (labBitmap == null)
                updateLabyrinthBmp();
            Matrix matrix = new Matrix();
            matrix.postScale(cellSize / labBitmapcellSize, cellSize / labBitmapcellSize);
            canvas.drawBitmap(labBitmap, matrix, common);
        }
        void drawEntities(Canvas canvas){
            gameLogic.eFactory.lock();
            for (Entity entity : gameLogic.eFactory.entities){
                if (entity.whoami.equals("Exit")){
                    continue;
                }
                drawTile(canvas,
                        bitmaps.getByEntity(entity),
                        field2game(entity.pos),
                        entity.isLarge);
            }
            gameLogic.eFactory.unlock();
        }
        void drawBonusRadius(Canvas canvas){
            PointF pointF = gameLogic.playerCoords();

            float radius = 0;
            if (gameLogic.teleportActive){
                radius = gameLogic.getTeleportRadius();
            }
            if (gameLogic.pathfinderActive){
                radius = gameLogic.getPathfinderRadius();
            }

            if (radius == 0) return;

            // TODO: 4/20/19 don't create every time
            final RadialGradient gradient = new RadialGradient(pointF.x,
                    pointF.y,
                    radius,
                    Color.argb(10, 10, 255, 20),
                    Color.argb(90, 10, 255, 20),
                    Shader.TileMode.MIRROR);

            bonusRadius.setShader(null);
            bonusRadius.setStyle(Paint.Style.STROKE);
            bonusRadius.setStrokeWidth(1);
            canvas.drawCircle(pointF.x, pointF.y, radius, bonusRadius);

            bonusRadius.setShader(gradient);
            bonusRadius.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawCircle(pointF.x, pointF.y, radius, bonusRadius);
        }
        void drawPath(Canvas canvas){
            if (gameLogic.finded_path == null)
                return;

            Path poly = new Path();
            for (int i = 0; i < gameLogic.finded_path.size() - 1; i++)
                poly.addRect(pt2rect(gameLogic.finded_path.get(i), gameLogic.finded_path.get(i + 1), 1.f, cellSize), CW);

            canvas.drawPath(poly, path);
        }
        void drawFog(Canvas canvas){
            if (fogBitmap == null || !fogEnabled) return;

//            PointF game00 = game2screen(new PointF(0, 0));
//            Matrix matrix = new Matrix();
//            matrix.postScale(1/globalScale, 1/globalScale);
//            matrix.postTranslate(game00.x, game00.y);
            canvas.drawBitmap(fogBitmap, Ematrix, fog);
        }
        void drawBackground(Canvas canvas){
//            canvas.drawBitmap(BgResources.inst().backgroundBitmap,
//                    BgResources.inst().bgScaleMatrix,
//                    common);
            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
//            canvas.drawRect(0, 0, getWidth(), getHeight(), common);
        }
        void drawExit(Canvas canvas){
            exitDrawer.draw(canvas);
        }
        void drawLevelNumber(Canvas canvas){
            //todo don't create every time
            String text = String.valueOf(gameLogic.level_difficulty);
            if (gameLogic.level_difficulty < 10){
                text = "0" + text;
            }
            Rect bounds = new Rect();
            levelNumber.getTextBounds(text, 0, text.length(), bounds);
            bounds.offset(-bounds.left, -bounds.top);
            int offset = 15;
            bounds.offset(offset + 3, offset); // по Х надо больше из-за кривого шрифта

            //canvas.drawRect(bounds, hitbox);
            canvas.drawText(text,
                    bounds.right, bounds.bottom, levelNumber);

        }

        void onDraw(Canvas canvas){
            if (!gameLogic.isInited) return;
            canClearBmp = false;
            //canvas.setMatrix(Ematrix);
            profiler.start("Frame");

            profiler.start("BG");
            drawBackground(canvas);
            profiler.stop("BG");

//            profiler.start("Dots");
//            drawDots(canvas);
//            profiler.stop("Dots");

            // отрисовка игровых объектов в игровых координатах
            camera.applyToCanvas(canvas);

            profiler.start("Lab");
            drawLabyrinth(canvas);
            profiler.stop("Lab");
            drawPath(canvas);
            //drawTraces(canvas);
            profiler.start("Entities");
            drawEntities(canvas);
            profiler.stop("Entities");

            drawNodes(canvas);
            drawRails(canvas);
            drawPlayerHitbox(canvas);
            drawBonusRadius(canvas);

            drawPlayer(canvas);
            drawPointer(canvas);
            drawPickedUpAnimation(canvas);
            drawExit(canvas);

            drawDebug(canvas);

            profiler.start("Fog");
            fogMatrix.reset();
            fogMatrix.preTranslate(getGlobalOffset().x, getGlobalOffset().y);
            canvas.setMatrix(fogMatrix);
            drawFog(canvas);
            profiler.stop("Fog");

            // дальше отрисовка HUD. В экранных координатах
            canvas.setMatrix(Ematrix);

            drawLevelNumber(canvas);
            drawButtons(canvas);
            drawJoystick(canvas);

            profiler.stop("Frame");

            drawDebugText(canvas);

            canClearBmp = true;
        }
        void setRunning(boolean running_){
            running = running_;
        }
        void setHolder(SurfaceHolder holder){
            surfaceHolder = holder;
        }
        @Override
        public void run() {
            Logger.getAnonymousLogger().info("RenderThread.run() begin");
            Canvas canvas;
            is_started = true;

            Logger.getAnonymousLogger().info("RenderThread (is_waiting_4_surface)");
            boolean flag = false;
            while (is_waiting_4_surface){
                if (!flag){
                    doB4Surface();
                    flag = true;
                }
                try {
                    sleep(10);
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            Logger.getAnonymousLogger().info("RenderThread.run() drawing");
            while (running) {
                if (need_2_prepare_new_level){
                    doB4Surface();
                    need_2_prepare_new_level = false;
                }

                canvas = null;
                if (getTime() - prevDrawTime > redrawPeriod) {
                    prevDrawTime = getTime();
                    gameLogic.remoteMove();
                    ((PlayerFinder)buttons.get(0)).remoteAnimation();
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
//                    try {
//                        Thread.sleep(1);
//                    }
//                    catch (InterruptedException e){
//                        e.printStackTrace();
//                    }
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

    abstract class BitmapContainer{
        String whoami;

        BitmapContainer(String name){
            whoami = name;
        }

        int calcInSampleSize(BitmapFactory.Options options, int w, int h){

            if (
                    options.outWidth <= w ||
                            options.outHeight <= h
            ){
                return 1;
            }

            int result = 1;

            while (
                    options.outWidth / result >= w ||
                    options.outHeight / result >= h
            ){
                result = result * 2;
            }

            result = result / 2;

            return result;
        }

        Bitmap getOptimisedBitmapFromResource(int resource_id, int w, int h){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resource_id, options);
            options.inJustDecodeBounds = false;
            options.inSampleSize = calcInSampleSize(options, w, h);
            Bitmap result = BitmapFactory.decodeResource(context.getResources(), resource_id, options);

            result = Bitmap.createScaledBitmap(result,
                    w,
                    h,
                    true);

            return result;
        }

        abstract void addBitmap(int resource_id);

        abstract int size();

        abstract Bitmap get(int index);

        abstract void rescale();
    }
    class BitmapList extends BitmapContainer{
        ArrayList<Bitmap> list = new ArrayList<>();

        BitmapList(String name){
            super(name);
        }

        @Override
        void addBitmap(int resource_id){
            final int const_size = Math.round(cellSize * max_scale);
            // битмапы должны быть квадратными, поэтому один размер
            list.add(getOptimisedBitmapFromResource(resource_id, const_size, const_size));
        }

        @Override
        int size(){
            return  list.size();
        }

        @Override
        Bitmap get(int ind){
            return list.get(ind);
        }

        @Override
        void rescale(){
            // TODO: 1/3/19 Сделать адаптивные размеры
            for (int i = 0; i < list.size(); ++i){
                list.set(i, Bitmap.createScaledBitmap(list.get(i),
                        Math.round(cellSize * max_scale),
                        Math.round(cellSize * max_scale),
                        true));
            }
        }
    }

    class ButtonBitmap extends BitmapContainer{
        Bitmap bitmap;

        ButtonBitmap(String name){
            super(name);
        }

        @Override
        void addBitmap(int resource_id){
            final int const_size = Math.round(cellSize * max_scale);
            // битмапы должны быть квадратными, поэтому один размер
            bitmap = getOptimisedBitmapFromResource(resource_id, const_size, const_size);
        }

        @Override
        int size(){
            return (bitmap != null)
                    ?
                    1
                    :
                    2;
        }

        @Override
        Bitmap get(int ind){
            return bitmap;
        }

        @Override
        void rescale(){
            float rad = (float)getWidth() / 20.f;

            bitmap = Bitmap.createScaledBitmap(bitmap,
                    Math.round(rad),
                    Math.round(rad),
                    true);
        }
    }

    class AnimationBitmaps extends ArrayList<BitmapContainer>{
        AnimationBitmaps(){
            setCoin();
            setExit();

            setPathfinder();
            setTeleport();
            setPointer();
            setPlayer();

            setMenuButton();
            setBonusesButton();
            setSettingsButton();
            setCenterButton();
        }
        void setCoin(){
            if (null != getListByName("Coin"))
                return;

            BitmapList list = new BitmapList("Coin");

            list.addBitmap(R.drawable.coin_anim1);
//            list.addBitmap(R.drawable.coin_anim2);
//            list.addBitmap(R.drawable.coin_anim3);
//            list.addBitmap(R.drawable.coin_anim4);
//            list.addBitmap(R.drawable.coin_anim5);
//            list.addBitmap(R.drawable.coin_anim6);

            add(list);
        }
        void setExit(){
            if (null != getListByName("Exit"))
                return;

//            BitmapList list = new BitmapList("Exit");
//
//            list.addBitmap(R.drawable.vortex_frame_00);
//            list.addBitmap(R.drawable.vortex_frame_01);
//            list.addBitmap(R.drawable.vortex_frame_02);
//            list.addBitmap(R.drawable.vortex_frame_03);
//            list.addBitmap(R.drawable.vortex_frame_04);
//            list.addBitmap(R.drawable.vortex_frame_05);
//            list.addBitmap(R.drawable.vortex_frame_06);
//            add(list);
        }
        void setTeleport(){
            if (null != getListByName("Teleport"))
                return;
            BitmapList list = new BitmapList("Teleport");

            list.addBitmap(R.drawable.teleport);

            add(list);
        }
        void setPointer(){
            if (null != getListByName("Pointer"))
                return;
            BitmapList list = new BitmapList("Pointer");

            list.addBitmap(R.drawable.pointer);
            add(list);
        }
        void setPathfinder(){
            if (null != getListByName("Pathfinder"))
                return;
            BitmapList list = new BitmapList("Pathfinder");

            list.addBitmap(R.drawable.pathfinder);

            add(list);
        }
        void setFloor(){
            if (null != getListByName("Floor"))
                return;
//            BitmapList list = new BitmapList("Floor");
//
//            list.list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.floor));
//
//            add(list);
        }
        void setWall(){
            if (null != getListByName("Wall"))
                return;
//            BitmapList list = new BitmapList("Wall");
//
//            list.list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.wall));
//
//            add(list);
        }
        void setPlayer(){
            if (null != getListByName("Player"))
                return;
            BitmapList list = new BitmapList("Player");

            // TODO: 5/3/19 OOM here
            list.addBitmap(R.drawable.player);

            add(list);
        }

        void setMenuButton(){
            if (null != getListByName("MenuButton"))
                return;
            ButtonBitmap bmp = new ButtonBitmap("MenuButton");

            bmp.addBitmap(R.drawable.menu_button);

            add(bmp);
        }
        void setCenterButton(){
            if (null != getListByName("CenterButton"))
                return;
            ButtonBitmap bmp = new ButtonBitmap("CenterButton");

            bmp.addBitmap(R.drawable.center_button);

            add(bmp);
        }
        void setBonusesButton(){
            if (null != getListByName("BonusesButton"))
                return;
            ButtonBitmap bmp = new ButtonBitmap("BonusesButton");

            bmp.addBitmap(R.drawable.bonuses_button);

            add(bmp);
        }
        void setSettingsButton(){
            if (null != getListByName("SettingsButton"))
                return;
            ButtonBitmap bmp = new ButtonBitmap("SettingsButton");

            bmp.addBitmap(R.drawable.gear_black);

            add(bmp);
        }

        BitmapContainer getListByName(String name){
            for (BitmapContainer list : this
                    ) {
                if (list.whoami.equals(name))
                        return list;
            }
            return null;
        }
        Bitmap getByEntity(Entity entity){
            BitmapContainer list = getListByName(entity.whoami);
            return list.get(entity.incrAnimFrame(list.size()));
        }
        Bitmap getByName(String name){
            return getListByName(name).get(0);
        }
        void rescaleAll(){
            for (BitmapContainer list : this
                 ) {
                list.rescale();
            }
        }
    }

    class PickUpAnimation{
        private CPoint.Game pos;
        String text;
        int animStep = 0;
        float step = -1.f;
        int animDuration = 10;

        PickUpAnimation(CPoint.Game startPos, String _text){
            pos = startPos;
            text = _text;
        }
        CPoint.Game getPos(){
            pos.offset(0, step);
            animStep++;
            return pos;
        }
        boolean isOld(){
            return animStep > animDuration;
        }
    }

    class Profiler{
        class Element{
            String name;
            long time_us;
            boolean isStarted;
            float percent;
        }

        ArrayList<Element> elements = new ArrayList<>();

        Element find(String name){
            for (int i = 0; i < elements.size(); ++i){
                if (elements.get(i).name.equals(name))
                    return elements.get(i);
            }
                return null;
        }
        boolean isAllStopped(){
            for (Element el:elements)
                if (el.isStarted)
                    return false;
            return true;
        }
        void calcPrecents(){
            if (!isAllStopped())
                return;

            elements.get(0).percent = 100;
            for (int i = 1; i < elements.size(); ++i){
                elements.get(i).percent = 100 *
                        elements.get(i).time_us /
                        elements.get(0).time_us;

                //elements.get(i).percent = (float)Math.round(elements.get(i).percent * 100) / 100.f;
            }

        }
        int size(){
            return elements.size();
        }

        void start(String name){
            Element el = find(name);

            if (el == null){
                el = new Element();
                el.name = name;
                elements.add(el);
            }

            el.time_us = System.nanoTime() / 1000;
            el.isStarted = true;
        }
        void stop(String name){
            Element el = find(name);

            if (el == null)
                return;

            el.isStarted = false;
            el.time_us = System.nanoTime() / 1000 - el.time_us;

            if (isAllStopped())
                calcPrecents();
        }

        String getName(int i){
            return elements.get(i).name;
        }
        long getTime(int i){
            return elements.get(i).time_us;
        }
        float getPercents(int i){
            return elements.get(i).percent;
        }
    }

    class ExitDrawer{
        Paint exit_paint = new Paint();
        Paint line_paint = new Paint();
        final Random ed_random = new Random(System.currentTimeMillis());
        final ArrayList<Sector> sectors = new ArrayList<>();
        final ArrayList<Line> lines = new ArrayList<>();

        int colors[] = {
                Color.parseColor("#a101a6"),
                Color.parseColor("#ff5900"),
                Color.parseColor("#a3f400"),
                Color.parseColor("#01b6be"),
                Color.parseColor("#df007e"),
        };

        ExitDrawer(){
            exit_paint.setColor(Color.RED);
            exit_paint.setStyle(Paint.Style.FILL);
            line_paint.setColor(Color.argb(180, 120, 150, 180));

            setRandomObjects();
        }

        void setRandomObjects(){
            sectors.clear();
            for (int color : colors){
                // todo remove radius from ctor
                sectors.add(new Sector(1.f, 80.f, color));
            }
            for (int i = 0; i < 20; ++i){
                lines.add(new Line());
            }
        }

        void drawGradientDot(Canvas canvas, CPoint.Game pt){
            final float radius = cellSize * 1.2f;
            final RadialGradient gradient = new RadialGradient(pt.x,
                    pt.y,
                    radius,
                    Color.argb(40, 255, 255, 240),
                    Color.argb(0, 255, 255, 100),
                    Shader.TileMode.MIRROR);

            exit_paint.setShader(gradient);
            canvas.drawCircle(pt.x, pt.y, radius, exit_paint);
        }

        private PointF rotatePoint(PointF pt, double deg, PointF center){
            PointF result = new PointF();
            result.x = (float)(center.x + (pt.x - center.x) * Math.cos(Math.PI * deg / 180) -
                    (pt.y - center.y) * Math.sin(Math.PI * deg / 180));
            result.y = (float)(center.y + (pt.y - center.y) * Math.cos(Math.PI * deg / 180) +
                    (pt.x - center.x) * Math.sin(Math.PI * deg / 180));

            return result;
        }

        void drawSector(Canvas canvas,
                        CPoint.Game center,
                        double width_deg,
                        double direction_deg,
                        double rad_bot_gc,
                        Paint _paint_light,
                        Paint _paint_dark){
            final int steps_amount = 40;
            final float angle_delta_deg = (float)width_deg / steps_amount;
            final float height = cellSize / 3;

            final float start_angle_deg = (float)(direction_deg - width_deg / 2);

            PointF lt = new PointF();
            lt.x = center.x;
            lt.y = center.y + (float)rad_bot_gc + height;

            PointF lb = new PointF();
            lb.x = center.x;
            lb.y = center.y + (float)rad_bot_gc;

            PointF rt = rotatePoint(lt, width_deg, center);
            PointF rb = rotatePoint(lb, width_deg, center);

            Path sector = new Path();
            sector.reset();
            sector.moveTo(lb.x, lb.y);
            sector.lineTo(lt.x, lt.y);

            for (int i = 0; i < steps_amount; ++i){
                PointF cpt = rotatePoint(lt, angle_delta_deg, center);
                sector.lineTo(cpt.x, cpt.y);
                lt.x = cpt.x;
                lt.y = cpt.y;
            }

            sector.lineTo(rb.x, rb.y);

            for (int i = 0; i < steps_amount; ++i){
                PointF cpt = rotatePoint(rb, -angle_delta_deg, center);
                sector.lineTo(cpt.x, cpt.y);
                rb.x = cpt.x;
                rb.y = cpt.y;
            }

            sector.close();

            canvas.rotate(start_angle_deg, center.x, center.y);
            canvas.drawPath(sector, _paint_light);
            canvas.drawPath(sector, _paint_dark);
            canvas.rotate(-start_angle_deg, center.x, center.y);
        }

        void draw(Canvas canvas){
            drawGradientDot(canvas, gameLogic.exitCoords());

            for (Line l : lines){
                l.draw(canvas);
            }
            for (Sector s : sectors){
                s.draw(canvas);
            }
        }

        class Sector{
            private static final float max_radial_speed = 2.8f;
            private static final float min_radial_speed = 1.4f;

            private static final float max_radius = cellSize * 1.2f;
            private static final float min_radius = cellSize / 3;

            float radial_speed_deg;
            float radius_gc;
            float width_deg;
            float direction_deg;
            final Paint sector_paint_fill = new Paint();
            final Paint sector_paint_stroke = new Paint();

            Sector(float _radius, float _width_deg, int _color){

                radius_gc = getRandonRadius();
                direction_deg = ed_random.nextFloat() * 360;
                width_deg = _width_deg;

                sector_paint_fill.setStyle(Paint.Style.FILL);
                sector_paint_stroke.setStyle(Paint.Style.STROKE);
                sector_paint_stroke.setStrokeWidth(.5f);
                sector_paint_fill.setColor(_color);
                float[] hsv = {0,0,0};
                Color.colorToHSV(_color, hsv);
                hsv[2] -= .3f;
                sector_paint_stroke.setColor(Color.HSVToColor(hsv));

                radial_speed_deg = getRandomRadialSpeed();
            }

            void draw(Canvas canvas){
                drawSector(canvas,
                        gameLogic.exitCoords(),
                        width_deg,
                        direction_deg,
                        radius_gc,
                        sector_paint_fill,
                        sector_paint_stroke);

                direction_deg += radial_speed_deg;
            }
            float getRandonRadius(){
                return ed_random.nextFloat() *
                        (max_radius - min_radius) + min_radius;
            }
            float getRandomRadialSpeed(){
                float result = ed_random.nextFloat() *
                        (max_radial_speed - min_radial_speed) + min_radial_speed;
                if (ed_random.nextBoolean()){
                    result = -result;
                }
                return result;
            }
        }

        class Line{
            private static final float max_length_px = cellSize;
            private static final float min_length_px = cellSize / 4;

            private static final float max_radius_px = cellSize * 3.f;
            private static final float min_radius_px = cellSize * 1.2f;

            float angle_deg;
            float length;
            float speed = 0.4f;
            float radius;
            //float iteration;

            float upper_lim;
            float lower_lim;

            Line(){
                reInit();
            }
            void reInit(){
                angle_deg = ed_random.nextFloat() * 360.f;
                length = ed_random.nextFloat() * (max_length_px - min_length_px) + min_length_px;
                radius = ed_random.nextFloat() * (max_radius_px - min_radius_px) + min_radius_px;

                upper_lim = radius;
                lower_lim = upper_lim;
            }

            private CPoint.Game getMovedExitPoint(float radius, float _angle_deg){
                PointF result = gameLogic.exitCoords();
                result.y += radius;
                result = rotatePoint(result, _angle_deg, gameLogic.exitCoords());

                return new CPoint.Game(result.x, result.y);
            }

            CPoint.Game getUpperPoint(){
                return  getMovedExitPoint(upper_lim, angle_deg);
            }
            CPoint.Game getLowerPoint(){
                return  getMovedExitPoint(lower_lim, angle_deg);
            }

            void draw(Canvas canvas){
                CPoint.Game upper_pt = getUpperPoint();
                CPoint.Game lower_pt = getLowerPoint();

                canvas.drawLine(upper_pt.x, upper_pt.y, lower_pt.x, lower_pt.y, line_paint);

                moveLine();
            }
            void moveLine(){
                CPoint.Game exit = gameLogic.exitCoords();
                CPoint.Game upper_pt = getUpperPoint();
                CPoint.Game lower_pt = getLowerPoint();

                if (GameLogic.distance(upper_pt, lower_pt) > length){
                    upper_lim -= speed;
                }
                if (GameLogic.distance(lower_pt, exit) > speed * 4) {
                    lower_lim -= speed;
                }
                else{
                    if (GameLogic.distance(upper_pt, lower_pt) < length){
                        upper_lim -= speed;
                    }
                }
                if (GameLogic.distance(upper_pt, exit) <= speed * 4) {
                    reInit();
                }
            }
        }
    }

    abstract class Button { // all screenCoords
        CPoint.Screen pos = new CPoint.Screen();
        float rad = 0;
        String whoami;

        boolean lightAnimationEnabled = false;
        private float maxAnimationState = 100;
        private float animationState = 0;
        private float animationStep = 2.f;

        float getAnimtionPercent(){
            if (!lightAnimationEnabled) return 0.f;
            animationState += animationStep;
            if (animationState > maxAnimationState){
                animationState = 0;
            }

            return animationState / maxAnimationState;
        }

        Button(){}
        Button(CPoint.Screen position, float radius){
            pos = position;
            rad = radius;
            init();
        }
        void init(){}

        boolean tryClick (PointF screenCoord){
            if (GameLogic.distance(screenCoord, pos) < rad)
            {
                onClick();
                return true;
            }
            else
                return false;
        }
        abstract void onClick();
    }
    class PlayerFinder extends Button{
        private boolean animationEnabled = false;
        private CPoint.Screen newOffset = new CPoint.Screen(0, 0);
        private int animationSpeed = 150;

        PlayerFinder(CPoint.Screen position, float radius){
            whoami = "CenterButton";
            pos = position;
            rad = radius;
            init();
        }

        @Override
        void onClick(){
            gameLogic.playerPt = gameLogic.exitCoords();

            CPoint.Screen player_coord = game2screen(gameLogic.playerCoords());

            CPoint.Screen center = new CPoint.Screen(getWidth() / 2, getHeight() / 2);
            CPoint.Screen corner00 = game2screen(new CPoint.Game(0, 0));
            center.offset(corner00.x - player_coord.x, corner00.y - player_coord.y);

            newOffset = center;
            animationEnabled = true;
        }

        void set00CornerTo(CPoint.Screen pt){
            // метод переносит 0,0 угол лабиринта в указанное место
            PointF current_offset = getGlobalOffset();
            PointF step = new PointF(pt.x - current_offset.x, pt.y - current_offset.y);
            // translate() зависит от скейла
            // хз почему "У" координата развернута, но так работает
            camera.translate(step.x / getGlobalScale(), -step.y / getGlobalScale(), 0);
        }

        void moveCameraBy(CPoint.Screen offset){
            CPoint.Screen current_offset = new CPoint.Screen(getGlobalOffset()) ;
            set00CornerTo(new CPoint.Screen(current_offset.x + offset.x,
                    current_offset.y + offset.y));
        }

        void remoteAnimation(){
            if (animationEnabled){

                if (gameLogic.distance(newOffset, getGlobalOffset()) < animationSpeed){ //gameCoords
                    instantAnimation();
                }
                else {
                    CPoint.Screen vector = new CPoint.Screen(newOffset.x - getGlobalOffset().x,
                            newOffset.y - getGlobalOffset().y);
                    float module = (float)Math.sqrt(vector.x * vector.x + vector.y * vector.y);
                    vector = new CPoint.Screen(vector.x / module, vector.y / module);
                    vector = new CPoint.Screen(vector.x * animationSpeed, vector.y * animationSpeed);
                    moveCameraBy(vector);
                }
            }
        }
        void instantAnimation(){
            set00CornerTo(newOffset);
            animationEnabled = false;
        }
    }
    class MenuButton extends Button{

        MenuButton(CPoint.Screen position, float radius){
            whoami = "MenuButton";
            pos = position;
            rad = radius;
            init();
        }

        @Override
        void onClick() {
            Intent intent = new Intent(getContext(), ConfirmationActivity.class);
            ((Activity)getContext()).startActivityForResult(intent,
                    1);
        }
    }
    class Bonuses extends Button{
        Bonuses(CPoint.Screen position, float radius){
            whoami = "BonusesButton";
            pos = position;
            rad = radius;
            init();
        }

        @Override
        void onClick() {
            //gameLogic.getPath(gameLogic.playerCoords(), field2game(gameLogic.field.exitPos));
            gameLogic.remote_move_flag = false;
            lightAnimationEnabled = false;
            startBonusActivity();
            //gameLogic.remote_move_flag = true;
        }
    }
    class Settings extends Button{
        Settings(CPoint.Screen position, float radius){
            whoami = "SettingsButton";
            pos = position;
            rad = radius;
            init();
        }

        @Override
        void onClick() {
            startSettingsActivity();
        }
    }
}
