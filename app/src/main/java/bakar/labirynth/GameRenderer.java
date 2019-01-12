package bakar.labirynth;

import android.app.Activity;import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;
import android.util.Range;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

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


    final float cellSize = 10; // gameCoords side
    final Camera camera = new Camera();
    public float playerHitbox = 0; // screenCoord // in surfaceCreated();

    float max_scale;
    float min_scale;

    public boolean isMovingOffset = false;
    public boolean isMovingPlayer = false;
    float enlightenRadius = cellSize * 200 / 70; // gamecoord

    private GameLogic gameLogic;
    private RenderThread renderThread;

    private Context context;

    ArrayList<Button> buttons = new ArrayList<>();
    ArrayList<PickUpAnimation> pickUpAnimations = new ArrayList<>();

    public GameRenderer(Context _context) {
        super(_context);
        context = _context;
        camera.setLocation(0,0,10);
        camera.save();
        getHolder().addCallback(this);
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
        ((Activity)getContext()).startActivityForResult(intent, EndActivity.class.toString().hashCode());
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
        RectF rect = new RectF();

        rect.set(-cellSize / 2,-cellSize / 2, cellSize / 2, cellSize / 2);
        rect.offset(pt.x, pt.y);

        return rect;
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
        camera.translate(0,0, -value * (cellSize / 5));
        if (getGlobalScale() > max_scale || getGlobalScale() < min_scale)
            camera.restore();
        if (renderThread.fogBmpScale != getGlobalScale())
            renderThread.resizeFogBmp();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        min_scale = .9f * getWidth() / (gameLogic.field.getxSize() * cellSize);
        max_scale = 6;
        while (getGlobalScale() < min_scale)
            camera.translate(0,0,10);

        float rad1 = getWidth() / 20;
        CPoint.Screen pos1 = new CPoint.Screen(rad1 * 2, getHeight() - rad1 * 2);
        buttons.add(new PlayerFinder(pos1, rad1));
        buttons.get(0).onClick();
        ((PlayerFinder)buttons.get(0)).instantAnimation(); //Button.class.getClasses()

        CPoint.Screen pos2 = new CPoint.Screen(rad1 * 2, getHeight() - rad1 * 5);
        buttons.add(new MenuButton(pos2, rad1));

        CPoint.Screen pos3 = new CPoint.Screen(rad1 * 2, getHeight() - rad1 * 8);
        buttons.add(new Bonuses(pos3, rad1));

        gameLogic.eFactory.init();

        if (gameLogic.usesJoystick){
            float rad = getWidth() / 10;
            CPoint.Screen pos = new CPoint.Screen(getWidth() - rad * 2, getHeight() - rad * 2);
            gameLogic.createJoystick(pos, rad);
        }

        // тред создается последним
        renderThread = new RenderThread(this);
        renderThread.setHolder(getHolder());
        renderThread.setRunning(true);
        renderThread.start();
        renderThread.bitmaps.rescaleAll();

        playerHitbox = getWidth() / 10;

        renderThread.resizeFogBmp();
        renderThread.enlightenFogBmp(gameLogic.playerCoords());
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
        GameRenderer gameRenderer;
        private boolean running = false;
        private SurfaceHolder surfaceHolder;
        private Profiler profiler = new Profiler();
        final Matrix Ematrix = new Matrix();
        final Matrix bgScaleMatrix = new Matrix();
        final Matrix fogMatrix = new Matrix();
        final Matrix translate_matrix = new Matrix();

        private long prevDrawTime = 0;
        private long redrawPeriod = 30; // milliS // microS
        private long fpsTimer = 0;

        private Bitmap labBitmap;
        private Bitmap fogBitmap;
        private Bitmap backgroundBitmap;
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
        private Paint bonusRadius = new Paint();

        RenderThread(GameRenderer game_renderer){
            gameRenderer = game_renderer;
            floor.setColor(Color.argb(120,30, 30, 30));
            wall.setColor(Color.argb(240,230, 230, 230));
            node.setStrokeWidth(1);
            node.setColor(Color.rgb(0, 255, 0));
            node.setStyle(Paint.Style.STROKE);
            player.setColor(Color.rgb(255, 0, 255));
            hitbox.setColor(Color.argb(120, 255, 0, 0));
            trace.setColor(Color.YELLOW);
            text.setColor(Color.GRAY);
            text.setStrokeWidth(8);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(70);
            joystickMain.setColor(Color.argb(120, 255, 0, 0));
            joystickCurrent.setColor(Color.BLUE);
            exit.setColor(Color.RED);
            button.setColor(Color.argb(120, 255, 255, 255));
            path.setColor(Color.argb(120, 10, 255, 10));
            fog.setColor(Color.rgb(20, 20, 30));
            enlighten.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            enlighten.setColor(Color.argb(0, 60, 50, 60));
            puanim.setColor(Color.CYAN);
            puanim.setTextAlign(Paint.Align.CENTER);
            puanim.setTextSize(20);
            bonusRadius.setColor(Color.argb(120, 255, 10, 10));

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;

            backgroundBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.background, options);
            bgScaleMatrix.postScale((float)getWidth() / (float)backgroundBitmap.getWidth(),
                    (float)getHeight() / (float)backgroundBitmap.getHeight());
            createFogBmp();
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
            labBitmap = Bitmap.createBitmap(Math.round(gameLogic.field.getxSize() * cellSize),
                    Math.round(gameLogic.field.getySize() * cellSize), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(labBitmap);

            for (int x = 0; x < gameRenderer.gameLogic.field.getxSize(); ++x){
                for (int y = 0; y < gameRenderer.gameLogic.field.getySize(); ++y){
//                    Bitmap bmp;
//                    bmp = bitmaps.getByName("Floor");

                    CPoint.Game rectScreenPt = field2game(new CPoint.Field(x, y));
                    if (gameLogic.field.get(x, y)){
                        //bmp = bitmaps.getByName("Floor");
                        canvas.drawRect(pt2rect(rectScreenPt), floor);
                    }
                    else{
//                        drawTile(canvas,
//                                bitmaps.getByName("Wall"),
//                                rectScreenPt,
//                                false);
                        canvas.drawRect(pt2rect(rectScreenPt), wall);
                    }


                }
            }
        }

        void drawTile(Canvas canvas, Bitmap bmp, CPoint.Game pos, boolean isLarge){
            translate_matrix.reset();
            if (isLarge)
            {
                translate_matrix.preTranslate(pos.x - cellSize, pos.y  - cellSize);
                // из преобразования в AnimationBitmaps
                translate_matrix.preScale(2 / max_scale, 2 / max_scale);
            }
            else
            {
                translate_matrix.preTranslate(pos.x - cellSize / 2, pos.y  - cellSize / 2);
                translate_matrix.preScale(1 / max_scale, 1 / max_scale);
            }

            canvas.drawBitmap(bmp,
                    translate_matrix, common);
        }
        void drawDebugText(Canvas canvas){
            if (!isDebug)
                return;
            ArrayList<String> outputs = new ArrayList<>();

            //outputs.add("Scale: " + getGlobalScale());
            //outputs.add("Offset: " + getGlobalOffset());

            outputs.add("FPS: " + calcFps());
            //outputs.add("Free mem: " + availMemory() + " MB");
            //outputs.add("Lab bmp size: " + labBitmap.getRowBytes() * labBitmap.getHeight() / 1024 + " kB");
            //outputs.add("Lab bmp height: " + labBitmap.getHeight());
//            if (fogBitmap != null){
//                outputs.add("Fog bmp size: " + fogBitmap.getRowBytes() * fogBitmap.getHeight() / 1024 + " kB");
//                outputs.add("Fog bmp height: " + fogBitmap.getHeight());
//            }

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

            CPoint.Screen offset_s = game2screen(offset_g);
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
            if (gameLogic.joystick == null)
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
            for (Button bt : buttons){
                canvas.drawCircle(bt.pos.x, bt.pos.y, bt.rad, button);
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
//            PointF game00 = game2screen(new PointF(0, 0));
//            Matrix matrix = new Matrix();
//            matrix.postScale(1/globalScale, 1/globalScale);
//            matrix.postTranslate(game00.x, game00.y);
            canvas.drawBitmap(labBitmap, Ematrix, common);
        }
        void drawEntities(Canvas canvas){
            for (Entity entity : gameLogic.eFactory.entities){
                drawTile(canvas,
                        bitmaps.getByEntity(entity),
                        field2game(entity.pos),
                        entity.isLarge);
            }
        }
        void drawBonusRadius(Canvas canvas){
            if (gameLogic.teleportActive){
                PointF pointF = gameLogic.playerCoords();
                canvas.drawCircle(pointF.x, pointF.y, gameLogic.getTeleportRadius(), bonusRadius);
            }
            if (gameLogic.pathfinderActive){
                PointF pointF = gameLogic.playerCoords();
                canvas.drawCircle(pointF.x, pointF.y, gameLogic.getPathfinderRadius(), bonusRadius);
            }
        }
        void drawPath(Canvas canvas){
            if (gameLogic.finded_path == null) {
                return;
            }
            Path poly = new Path();
            for (int i = 0; i < gameLogic.finded_path.size() - 1; i++) {
                float left = Math.min(gameLogic.finded_path.get(i).x, gameLogic.finded_path.get(i + 1).x) - cellSize / 2;
                float right = Math.max(gameLogic.finded_path.get(i).x, gameLogic.finded_path.get(i + 1).x) + cellSize / 2;
                float top = Math.min(gameLogic.finded_path.get(i).y, gameLogic.finded_path.get(i + 1).y) - cellSize / 2;
                float bot = Math.max(gameLogic.finded_path.get(i).y, gameLogic.finded_path.get(i + 1).y) + cellSize / 2;

                poly.addRect(left, top, right, bot, CW);
            }
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

            /////////////////////////////////////////////////////////
            // если указать пеинт с прозрачным цветом, то будет блюр эффект
            //////////////////////////////////////////////////////////////////////

            int h = backgroundBitmap.getHeight();
            int w = backgroundBitmap.getWidth();

            canvas.drawBitmap(backgroundBitmap, bgScaleMatrix, common);
        }

        void onDraw(Canvas canvas){
            if (!gameLogic.isInited) return;
            canClearBmp = false;
            //canvas.setMatrix(Ematrix);

            profiler.start("Frame");

            profiler.start("BG");
            drawBackground(canvas);
            profiler.stop("BG");

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

            drawDebug(canvas);

            profiler.start("Fog");
            fogMatrix.reset();
            fogMatrix.preTranslate(getGlobalOffset().x, getGlobalOffset().y);
            canvas.setMatrix(fogMatrix);
            drawFog(canvas);
            profiler.stop("Fog");

            // дальше отрисовка HUD. В экранных координатах
            canvas.setMatrix(Ematrix);

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
            Canvas canvas;
            while (running) {
                canvas = null;
                if (getTime() - prevDrawTime > redrawPeriod) {
                    prevDrawTime = getTime();
                    if (gameLogic.usesJoystick)
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
                }
            }
        }
    }

    class BitmapList extends ArrayList<Bitmap>{
        String whoami;

        BitmapList(String name){
            whoami = name;
        }

        void rescale(){
            // TODO: 1/3/19 Сделать адаптивные размеры
            for (int i = 0; i < size(); ++i){
                this.set(i, Bitmap.createScaledBitmap(get(i),
                        Math.round(cellSize * max_scale),
                        Math.round(cellSize * max_scale),
                        true));
            }
        }
    }

    class AnimationBitmaps extends ArrayList<BitmapList>{
        AnimationBitmaps(){
            setCoin();
            setExit();
            setPathfinder();
            setTeleport();
            setPointer();
            setFloor();
            setWall();
            setPlayer();
        }
        void setCoin(){
            if (null != getListByName("Coin"))
                return;

            BitmapList list = new BitmapList("Coin");

            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim1));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim2));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim3));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim4));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim5));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim6));

            add(list);
        }
        void setExit(){
            if (null != getListByName("Exit"))
                return;

            BitmapList list = new BitmapList("Exit");

            //exitTextures.clear();
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_00));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_01));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_02));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_03));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_04));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_05));
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_06));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_07));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_08));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_09));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_10));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_11));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_12));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_13));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_14));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_15));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_16));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_17));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_18));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_19));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_20));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_21));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_22));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_23));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_24));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_25));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_26));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_27));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_28));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_29));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_30));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_31));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_32));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_33));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_34));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_35));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_36));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_37));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_38));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_39));
            add(list);
        }
        void setTeleport(){
            if (null != getListByName("Teleport"))
                return;
            BitmapList list = new BitmapList("Teleport");

            //teleportTextures.clear();
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.teleport));

            add(list);
        }
        void setPointer(){
            if (null != getListByName("Pointer"))
                return;
            BitmapList list = new BitmapList("Pointer");

            //pointerTextures.clear();
            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.pointer));
            add(list);
        }
        void setPathfinder(){
            if (null != getListByName("Pathfinder"))
                return;
            BitmapList list = new BitmapList("Pathfinder");

            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.pathfinder));

            add(list);
        }
        void setFloor(){
            if (null != getListByName("Floor"))
                return;
            BitmapList list = new BitmapList("Floor");

            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.floor));

            add(list);
        }
        void setWall(){
            if (null != getListByName("Wall"))
                return;
            BitmapList list = new BitmapList("Wall");

            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.wall));

            add(list);
        }
        void setPlayer(){
            if (null != getListByName("Player"))
                return;
            BitmapList list = new BitmapList("Player");

            list.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.player));

            add(list);
        }

        BitmapList getListByName(String name){
            for (BitmapList list : this
                    ) {
                if (list.whoami.equals(name))
                        return list;
            }
            return null;
        }
        Bitmap getByEntity(Entity entity){
            BitmapList list = getListByName(entity.whoami);
            return list.get(entity.incrAnimFrame(list.size()));
        }
        Bitmap getByName(String name){
            return getListByName(name).get(0);
        }
        void rescaleAll(){
            for (BitmapList list : this
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

    abstract class Button { // all screenCoords
        CPoint.Screen pos = new CPoint.Screen();
        float rad = 0;

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
            pos = position;
            rad = radius;
            init();
        }

        @Override
        void onClick(){
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
            pos = position;
            rad = radius;
            init();
        }

        @Override
        void onClick() {
            ((Activity)getContext()).finish();
//            Intent intent = new Intent(context, MenuActivity.class);
//            context.startActivity(intent);
        }
    }
    class Bonuses extends Button{
        Bonuses(CPoint.Screen position, float radius){
            pos = position;
            rad = radius;
            init();
        }

        @Override
        void onClick() {
            //gameLogic.getPath(gameLogic.playerCoords(), field2game(gameLogic.field.exitPos));
            startBonusActivity();
        }
    }
}
