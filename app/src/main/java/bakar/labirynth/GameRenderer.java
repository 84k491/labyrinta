package bakar.labirynth;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Bakar on 10.03.2018.
 */

public class GameRenderer extends SurfaceView implements SurfaceHolder.Callback{

    // IMPORT /////////////////////////////
    boolean isDebug = false;
    ///////////////////////////////////////

    boolean fogEnabled = false;

    boolean registerBonusTouch = false;

    float cellSize = 10; // gameCoords side
    Camera camera = new Camera();
    float globalScale = cellSize * 3 / 70;
    private PointF globalOffset = new PointF(0, 0); // gameCoords
    public float playerHitbox = 0; // screenCoord // in surfaceCreated();

    public boolean isMovingOffset = false;
    public boolean isMovingPlayer = false;
    int enlightenRadius = Math.round(cellSize * 200 / 70); // gamecoord

    private GameLogic gameLogic;
    private RenderThread renderThread;

    private Context context;

    ArrayList<Button> buttons = new ArrayList<>();
    ArrayList<PickUpAnimation> pickUpAnimations = new ArrayList<>();

    public GameRenderer(Context _context) {
        super(_context);
        context = _context;
        camera.setLocation(1,1,1);
        getHolder().addCallback(this);
    }
    void addPickUpAnimation(PointF pointF, String text){
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
    boolean bonusTouch(PointF screenCoord){
        PointF gc = screen2game(screenCoord);
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

    private RectF pt2rectGm(PointF gameCoord){ // gameCoord 2 gameCoord
        RectF rect = new RectF();
        rect.set(gameCoord.x - cellSize / 2, gameCoord.y - cellSize / 2,
                gameCoord.x + cellSize / 2, gameCoord.y + cellSize / 2);
        return rect;
    }
    private RectF pt2rectSc(PointF screenCoord){ // screen2screen
        RectF rect = new RectF();
        PointF gameCoord = screen2game(screenCoord);

        PointF topLeft = new PointF(gameCoord.x - cellSize / 2, gameCoord.y - cellSize / 2);
        PointF botRight = new PointF(gameCoord.x + cellSize / 2, gameCoord.y + cellSize / 2);
        topLeft = game2screen(topLeft);
        botRight = game2screen(botRight);
        rect.set(topLeft.x, topLeft.y, botRight.x, botRight.y);
        return rect;
    }
    private RectF pt2rectSc(PointF screenCoord1, PointF screenCoord2){ // screen2screen
        RectF rect = new RectF();

        RectF rect1 = pt2rectSc(screenCoord1);
        RectF rect2 = pt2rectSc(screenCoord2);

        rect.set(Math.min(rect1.left, rect2.left), Math.min(rect1.top, rect2.top),
                Math.max(rect1.right, rect2.right), Math.max(rect1.bottom, rect2.bottom));
        return rect;
    }

    boolean isPlayerInSight(){
        PointF pl = game2screen(gameLogic.playerCoords());
        return  (pl.x > 0 && pl.y > 0 && pl.x < getWidth() && pl.y < getHeight());
    }
    void lightFog(PointF gc){
        renderThread.enlightenFogBmp(gc);
    }
    void resetFog(){renderThread.createFogBmp();}

    // Converters /////////////////////////////
    public PointF screen2game(PointF value){
        PointF result = new PointF();
        result.set(value.x * globalScale + globalOffset.x,
                value.y * globalScale + globalOffset.y);
        return result;
    }
    public Point game2field(PointF value){
        Point result = new Point();
        double x, y;
        x = Math.floor(value.x / cellSize);
        y = Math.floor(value.y / cellSize);
        result.set((int)x, (int)y);
        return result;
    }
    public Point screen2field(PointF value){
        return game2field(screen2game(value));
    }
    //
    public PointF field2game(Point value){
        PointF result = new PointF();
        result.set(value.x * cellSize + cellSize / 2, value.y * cellSize + cellSize / 2);
        return result;
    }
    public PointF game2screen(PointF value){
        PointF result = new PointF();
        float x = (value.x - globalOffset.x) / globalScale;
        float y = (value.y - globalOffset.y) / globalScale;
        result.set(x, y);
        return result;
    }
    public PointF field2screen(Point value){
        return game2screen(field2game(value));
    }
    ///////////////////////////////////////////

    // Touch //////////////////////////////////
    void onTouchDown(MotionEvent.PointerCoords pointerCoords){
        PointF pointF = new PointF(pointerCoords.x, pointerCoords.y); // screenCoords

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
        if (gameLogic.pathfinderActive || gameLogic.teleportActive){
            if (screenCoord != null && registerBonusTouch) {
                bonusTouch(screenCoord);
                gameLogic.pathfinderActive = false;
                gameLogic.teleportActive = false;
            }

        }
        registerBonusTouch = false;
        isMovingPlayer = false;
        isMovingOffset = false;
        renderThread.clearLabyrinthBmp();
        if (gameLogic.usesJoystick)
            gameLogic.joystick.touchReleased();
    }
    //
    void movePlayer(PointF pointSc){
        if (gameLogic.usesJoystick)
            gameLogic.joystick.lastTouch = screen2game(pointSc);
        else
            gameLogic.movePlayerTo(screen2game(pointSc));
    }
    void changeOffset(PointF offset){
        registerBonusTouch = false;
        //globalOffset.offset(offset.x * globalScale, offset.y * globalScale);
        camera.save();
        camera.translate(-offset.x, offset.y, 0);
    }
    void changeScale(float value){
        // value is raw
        globalScale += value / (cellSize * 4000 / 70);
        //renderThread.clearLabyrinthBmp();
    }
    ///////////////////////////////////////////

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        float rad1 = getWidth() / 20;
        PointF pos1 = new PointF(rad1 * 2, getHeight() - rad1 * 2);
        buttons.add(new PlayerFinder(pos1, rad1));
        buttons.get(0).onClick();
        ((PlayerFinder)buttons.get(0)).instantAnimation(); //Button.class.getClasses()

        PointF pos2 = new PointF(rad1 * 2, getHeight() - rad1 * 5);
        buttons.add(new MenuButton(pos2, rad1));

        PointF pos3 = new PointF(rad1 * 2, getHeight() - rad1 * 8);
        buttons.add(new Bonuses(pos3, rad1));

        gameLogic.eFactory.init();

        if (gameLogic.usesJoystick){
            float rad = getWidth() / 10;
            PointF pos = new PointF(getWidth() - rad * 2, getHeight() - rad * 2);
            gameLogic.createJoystick(pos, rad);
        }

        // тред создается последним
        renderThread = new RenderThread(this);
        renderThread.setHolder(getHolder());
        renderThread.setRunning(true);
        renderThread.start();

        playerHitbox = getWidth() / 10;
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

        private long prevDrawTime = 0;
        private long redrawPeriod = 30; // milliS // microS
        private long fpsTimer = 0;

        private Bitmap labBitmap;
        private Bitmap fogBitmap;
        private Bitmap pointerBitmap;
        private boolean canClearBmp = true;

        TextureAdapter textureAdapter = new TextureAdapter();

        //Todo: перенести Paint в ресурсы цвета
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
        RenderThread(GameRenderer game_renderer){
            gameRenderer = game_renderer;
            floor.setColor(Color.rgb(184, 203, 191));
            wall.setColor(Color.rgb(150, 50, 50));
            node.setStrokeWidth(10);
            node.setColor(Color.rgb(0, 255, 0));
            node.setStyle(Paint.Style.STROKE);
            player.setColor(Color.rgb(255, 0, 255));
            hitbox.setColor(Color.argb(120, 255, 0, 0));
            trace.setColor(Color.YELLOW);
            text.setColor(Color.CYAN);
            text.setStrokeWidth(8);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTextSize(70);
            joystickMain.setColor(Color.argb(120, 255, 0, 0));
            joystickCurrent.setColor(Color.BLUE);
            exit.setColor(Color.RED);
            button.setColor(Color.argb(120, 255, 255, 255));
            path.setColor(Color.argb(120, 10, 255, 10));
            fog.setColor(Color.argb(255, 20, 20, 30));
            enlighten.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            enlighten.setColor(Color.argb(0, 60, 50, 60));
            puanim.setColor(Color.CYAN);
            puanim.setTextAlign(Paint.Align.CENTER);
            puanim.setTextSize(50);
            bonusRadius.setColor(Color.argb(120, 255, 10, 10));
            createFogBmp();
            enlightenFogBmp(gameLogic.playerCoords());
        }
        private void createFogBmp(){
            if (!fogEnabled) return;
            int xsize = Math.round(gameLogic.field.getxSize() * cellSize/* / globalScale*/);
            int ysize = Math.round(gameLogic.field.getySize() * cellSize/* / globalScale*/);
            fogBitmap = Bitmap.createBitmap(xsize, ysize, Bitmap.Config.ALPHA_8);
            Canvas canvas = new Canvas(fogBitmap);

            canvas.drawRect(0, 0, xsize, ysize, fog);
        }
        private void enlightenFogBmp(PointF gCoord){
            if (fogBitmap == null || !fogEnabled) return;
            //Todo: рисовать круги в отдельном битмапе
            Canvas canvas = new Canvas(fogBitmap);
//            for (int i = 5; i > 0; i--){
//                enlighten.setColor(Color.argb( (i - 1) * 5, 60, 50, 60));
//                canvas.drawCircle(gCoord.x, gCoord.y, enlightenRadius + i * 50, enlighten);
//            }
            int scale = 2;
            int rad = enlightenRadius * scale;
            int steps = 5;
            int alpha = 2;
            while (rad > enlightenRadius){
                enlighten.setColor(Color.argb(alpha, 60, 50, 60));
                canvas.drawCircle(gCoord.x, gCoord.y, rad, enlighten);
                alpha += 5;
                rad -= (enlightenRadius * scale - enlightenRadius) / steps;
            }
            enlighten.setColor(Color.argb(230, 60, 50, 60));
            canvas.drawCircle(gCoord.x, gCoord.y, enlightenRadius, enlighten);
        }
        void clearLabyrinthBmp(){
            long start_time = getTime();
            while (null != labBitmap && (getTime() - start_time) < (redrawPeriod * 2))
                if (canClearBmp)
                    labBitmap = null;
        }
        void updateLabyrinthBmp(){
            // рисуется в игровых координатах, при отрисовке умножается на матрицу
            labBitmap = Bitmap.createBitmap(Math.round(gameLogic.field.getxSize() * cellSize/* / globalScale*/),
                    Math.round(gameLogic.field.getySize() * cellSize/* / globalScale*/), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(labBitmap);

            for (int x = 0; x < gameRenderer.gameLogic.field.getxSize(); ++x){
                for (int y = 0; y < gameRenderer.gameLogic.field.getySize(); ++y){
                    Paint paint;

                    PointF rectScreenPt = field2game(new Point(x, y));
                    if (gameLogic.field.get(x, y))
                        paint = floor;
                    else
                        paint = wall;

                    canvas.drawRect(pt2rectGm(rectScreenPt), paint);
                }
            }
        }

        void drawPickedUpAnimation(Canvas canvas){
            for (int i = 0; i < pickUpAnimations.size(); i++) {
                if (pickUpAnimations.get(i).isOld()){
                    pickUpAnimations.remove(i);
                    i--;
                }
                else{
                    PointF pos = pickUpAnimations.get(i).getPos();
                    pos = game2screen(pos);
                    canvas.drawText(pickUpAnimations.get(i).text,
                            pos.x, pos.y, puanim);
                }
            }
        }
        void drawPointer(Canvas canvas){
            if (!gameLogic.pointerActive) return;
            if (pointerBitmap == null)
                pointerBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.pointer);

            Matrix matrix = new Matrix();
            float size = cellSize * 2.f;
            matrix.postScale((size / pointerBitmap.getHeight()) / globalScale,
                    (size / pointerBitmap.getHeight()) / globalScale);
            PointF offset = gameLogic.playerCoords();
            offset.offset(size, 0); // смещаем текстуру так, чтобы она была сбоку
            offset.offset(-size / 2, -size / 2);

            offset = game2screen(offset);
            matrix.postTranslate(offset.x, offset.y);

            // 0 градусов это напрвление направо
            PointF vec1 = new PointF(gameLogic.exitCoords().x - gameLogic.playerCoords().x,
                    gameLogic.exitCoords().y - gameLogic.playerCoords().y);
            PointF vec2 = new PointF(1.f, 0.f);

            float scalar = vec1.x * vec2.x + vec1.y * vec2.y;
            float mod1 = (float)Math.sqrt((vec1.x * vec1.x + vec1.y * vec1.y));
            float mod2 = (float)Math.sqrt((vec2.x * vec2.x + vec2.y * vec2.y));
            float cos = scalar / (mod1 * mod2);
            float deg = 180 * (float)Math.acos(cos) / 3.1415f;
            if (gameLogic.playerCoords().y > gameLogic.exitCoords().y)
                deg = -deg;
            matrix.postRotate(deg, game2screen(gameLogic.playerCoords()).x, game2screen(gameLogic.playerCoords()).y);

            canvas.drawBitmap(pointerBitmap, matrix, fog);

        }
        void drawDebugText(Canvas canvas){
            if (!isDebug)
                return;
            ArrayList<String> outputs = new ArrayList<>();

            outputs.add("Seed: " + gameLogic.seed);
            outputs.add("Scale: " + globalScale);
            //outputs.add("Offset: " + globalOffset);
            outputs.add("FPS: " + calcFps());
            outputs.add("Free mem: " + availMemory() + " MB");
            outputs.add("Lab bmp size: " + labBitmap.getRowBytes() * labBitmap.getHeight() / 1024 + " kB");
            outputs.add("Lab bmp height: " + labBitmap.getHeight());
            if (fogBitmap != null){
                outputs.add("Fog bmp size: " + fogBitmap.getRowBytes() * fogBitmap.getHeight() / 1024 + " kB");
                outputs.add("Fog bmp height: " + fogBitmap.getHeight());
            }

            for (int i = 0; i < outputs.size(); ++i){
                canvas.drawText(outputs.get(i), getWidth(), (i + 1) * 70, text);
            }

            for (int i = 0; i < profiler.size(); ++i){
                canvas.drawText(profiler.getName(i) + ": " + profiler.getTime(i) + " us",
                        getWidth(), (i + outputs.size() + 1) * 70, text);
            }
        }
        void drawTraces(Canvas canvas){
            for (int i = 0; i < gameLogic.traces.size(); ++i){
                canvas.drawRect(pt2rectSc(field2screen(((LinkedList<Point>)gameLogic.traces).get(i))), trace);
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
            canvas.drawRect(pt2rectSc(game2screen(gameLogic.playerCoords())), player);
        }
        void drawButtons(Canvas canvas){
            for (Button bt : buttons){
                canvas.drawCircle(bt.pos.x, bt.pos.y, bt.rad, button);
            }
        }
        void drawPlayerHitbox(Canvas canvas){
            if (!isDebug)
                return;

            PointF pointF = game2screen(gameLogic.playerCoords());
            canvas.drawCircle(pointF.x, pointF.y, playerHitbox, hitbox);
        }
        void drawRails(Canvas canvas){
            if (!isDebug)
                return;

            PointF first = field2screen(gameLogic.currentNode.pos);
            PointF second;
            for (int i = 0; i < gameLogic.currentNode.links.size(); ++i) {
                second = field2screen(gameLogic.currentNode.links.valueAt(i).pos);
                canvas.drawLine(first.x, first.y, second.x, second.y, node);
            }

            PointF pl = game2screen(gameLogic.playerCoords());
            PointF touch = game2screen(gameLogic.debugTouchGameCoord);
            canvas.drawLine(pl.x, pl.y, touch.x, touch.y, node);

        }
        void drawNodes(Canvas canvas){
            if (!isDebug)
                return;

            canvas.drawRect(pt2rectSc(field2screen(gameLogic.currentNode.pos)), node);
            for (int i = 0; i < gameLogic.currentNode.links.size(); ++i){
                canvas.drawRect(pt2rectSc(field2screen(gameLogic.currentNode.links.valueAt(i).pos)), node);
            }
        }
        void drawLabyrinth(Canvas canvas){
            if (labBitmap == null)
                updateLabyrinthBmp();
            PointF game00 = game2screen(new PointF(0, 0));
            Matrix matrix = new Matrix();
            matrix.postScale(1/globalScale, 1/globalScale);
            matrix.postTranslate(game00.x, game00.y);
            canvas.drawBitmap(labBitmap, matrix, wall);
        }
        void drawEntities(Canvas canvas){
            for (Entity entity : gameLogic.eFactory.entities
                 ) {
                textureAdapter.reInit(entity);
                canvas.drawBitmap(textureAdapter.bitmap, textureAdapter.matrix, fog);
            }

        }
        void drawBonusRadius(Canvas canvas){
            if (gameLogic.teleportActive){
                PointF pointF = game2screen(gameLogic.playerCoords());
                canvas.drawCircle(pointF.x, pointF.y, gameLogic.getTeleportRadius() / globalScale, bonusRadius);
            }
            if (gameLogic.pathfinderActive){
                PointF pointF = game2screen(gameLogic.playerCoords());
                canvas.drawCircle(pointF.x, pointF.y, gameLogic.getPathfinderRadius() / globalScale, bonusRadius);
            }
        }
        void drawPath(Canvas canvas){
            if (gameLogic.finded_path == null) {
                return;
            }
            //Todo: перенести это в отрисовку битмапа
            for (int i = 0; i < gameLogic.finded_path.size() - 1; i++) {
                canvas.drawRect(pt2rectSc(game2screen(gameLogic.finded_path.get(i)),
                        game2screen(gameLogic.finded_path.get(i + 1))), path);
                canvas.drawRect(pt2rectSc(game2screen(gameLogic.finded_path.get(i + 1))), floor);
            }
        }
        void drawFog(Canvas canvas){
            if (fogBitmap == null || !fogEnabled) return;

            PointF game00 = game2screen(new PointF(0, 0));
            Matrix matrix = new Matrix();
            matrix.postScale(1/globalScale, 1/globalScale);
            matrix.postTranslate(game00.x, game00.y);
            canvas.drawBitmap(fogBitmap, matrix, fog);
        }

        void onDraw(Canvas canvas){
            if (!gameLogic.isInited) return;
            canClearBmp = false;
            camera.applyToCanvas(canvas);

            canvas.drawColor(Color.rgb(20, 20, 100));

            profiler.start("Lab draw time");
            drawLabyrinth(canvas);
            profiler.stop("Lab draw time");
            drawPath(canvas);
            drawTraces(canvas);
            drawEntities(canvas);

            drawNodes(canvas);
            drawRails(canvas);
            drawPlayerHitbox(canvas);
            drawBonusRadius(canvas);

            drawPlayer(canvas);
            drawPointer(canvas);
            drawPickedUpAnimation(canvas);
            profiler.start("Fog draw time");
            drawFog(canvas);
            profiler.stop("Fog draw time");
            drawButtons(canvas);
            drawJoystick(canvas);

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

    class TextureAdapter{
        Matrix matrix;
        Bitmap bitmap;
        //Todo: убрать пересоздание объектов

        TextureAdapter(){}
        TextureAdapter(Entity entity){
            reInit(entity);
        }
        void reInit(Entity entity){
            float localCellSize;
            if (entity.isLarge)
                localCellSize = cellSize * 2.0f;
            else
                localCellSize = cellSize;

            bitmap = entity.getDrawTexture();
            matrix = new Matrix();
            matrix.postScale((localCellSize / bitmap.getHeight()) / globalScale,
                    (localCellSize / bitmap.getHeight()) / globalScale);
            PointF offset = field2game(entity.pos);
            offset.offset(-localCellSize / 2, -localCellSize / 2);
            offset = game2screen(offset);
            matrix.postTranslate(offset.x, offset.y);
        }
    }
    class PickUpAnimation{
        private PointF pos;
        String text;
        int animStep = 0;
        float step = -1.f;
        int animDuration = 10;

        PickUpAnimation(PointF startPos, String _text){
            pos = startPos;
            text = _text;
        }
        PointF getPos(){
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
        }

        ArrayList<Element> elements = new ArrayList<>();

        Element find(String name){
            for (int i = 0; i < elements.size(); ++i){
                if (elements.get(i).name == name)
                    return elements.get(i);
            }
                return null;
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
        }

        String getName(int i){
            return elements.get(i).name;
        }
        long getTime(int i){
            return elements.get(i).time_us;
        }
    }

    abstract class Button { // all screenCoords
        PointF pos = new PointF();
        float rad = 0;

        Button(){}
        Button(PointF position, float radius){
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
        private PointF newOffset = new PointF(0, 0);
        private int animationSpeed = 150;

        PlayerFinder(PointF position, float radius){
            pos = position;
            rad = radius;
            init();
        }

        @Override
        void onClick(){
            PointF newGlobalOffset = new PointF(gameLogic.playerCoords().x, gameLogic.playerCoords().y);
            PointF center = screen2game(new PointF(getWidth() / 2, getHeight() / 2));
            PointF corner00 = screen2game(new PointF(0, 0));
            newGlobalOffset.offset(corner00.x - center.x, corner00.y - center.y);

            newOffset = newGlobalOffset;
            animationEnabled = true;
        }
        void remoteAnimation(){
            if (animationEnabled){
                if (gameLogic.distance(newOffset, globalOffset) < animationSpeed){ //gameCoords
                    instantAnimation();
                }
                else {
                    PointF vector = new PointF(newOffset.x - globalOffset.x, newOffset.y - globalOffset.y);
                    float module = (float)Math.sqrt(vector.x * vector.x + vector.y * vector.y);
                    vector = new PointF(vector.x / module, vector.y / module);
                    vector = new PointF(vector.x * animationSpeed, vector.y * animationSpeed);

                    globalOffset.offset(vector.x, vector.y);
                }
            }
        }
        void instantAnimation(){
            globalOffset = newOffset;
            animationEnabled = false;
        }
    }
    class MenuButton extends Button{

        MenuButton(PointF position, float radius){
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
        Bonuses(PointF position, float radius){
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
