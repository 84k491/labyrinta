package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.Random;

public class GameActivity extends Activity{

    //Todo: rename
    //Todo: сделать выбор положения джойстика и кнопок
    //Todo: заблочить кнопку "назад"
    //Todo: белые кнопки рисуются заново при разворачивании
    // TODO: 15.05.2018 сделать режим на время
    // TODO: 19.05.2018 rate this app
    // TODO: 4/20/19 плавное движение от акселерометра
    // TODO: 4/20/19 заблочить поворот

    // STEPS-TO-BETA
    // TODO: 3/18/19 put icons in shop activity
    // TODO: 3/18/19 OOM fixes
    // TODO: 3/18/19 background fix
    // TODO: 3/18/19 accelerometer movement
    // TODO: 3/18/19 bigger app icon
    // TODO: 4/19/19 nicer bonus ranges
    // TODO: 4/19/19 settings activity

    // TODO: 4/23/19 stored gold icon
    // TODO: 4/20/19 endlevel counter
    // TODO: 4/22/19 switching controls @ runtime
    // TODO: 4/22/19 go-to-menu confirmation
    // TODO: 3/18/19 loading screen
    // TODO: 12/31/18 вылетает если использовать бонус за пределами лабиринта
    // TODO: 1/27/19 mutex на вектор с предметами (отрисовка и удаление в разных потоках)
    // TODO: 4/16/19 sounds
    // TODO: 3/18/19 move up back buttons in menus
    // TODO: 3/18/19 in-app purchases
    // TODO: 3/18/19 credits for video
    // TODO: 3/18/19 player, exit, coin sprites
    // TODO: 4/20/19 pointer upgrade

    GameRenderer gameRenderer;
    GameLogic gameLogic;
    CustomTouchListener touchListener;
    SharedPreferences sPref;
    TiltController tiltController;
    ConstraintLayout mainLayout;
    private InterstitialAd mInterstitialAd;

    Point difficultyToActualSize(int lvl_difficulty){
        // от сложности должна зависеть диагональ прямоугольника

        float hypot = Economist.getInstance().getLevelHypotByUpg(lvl_difficulty);

        float square_side = Economist.getInstance().getSquareSide(hypot);

        Random random = new Random(System.currentTimeMillis());
        float rand = random.nextFloat() * 2.f - 1.f;
        rand *= square_side / 4.f;

        PointF resultF = new PointF(0,0);
        resultF.x = square_side + rand;
        resultF.y = (float)Math.sqrt(hypot * hypot - resultF.x * resultF.x);

        Point result = new Point();
        result.x = Math.round(resultF.x);
        result.y = Math.round(resultF.y);

//        float hypot_check = (float)Math.sqrt(result.x * result.x + result.y * result.y);
//        float diff = hypot - hypot_check;

        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().setFormat(PixelFormat.RGBA_8888);

        mainLayout = new ConstraintLayout(this);

        Intent intent = getIntent();
        sPref = getSharedPreferences("global", MODE_PRIVATE);

        gameRenderer = new GameRenderer(this);
        gameRenderer.isDebug = sPref.getBoolean("is_debug",false);
        gameRenderer.fogEnabled = sPref.getBoolean("fog_enabled", false);

        touchListener = new CustomTouchListener();

        int difficulty = intent.getIntExtra("level_size", 1);

        Point lvl_size = difficultyToActualSize(difficulty);
        gameLogic = new GameLogic(gameRenderer, intent.getLongExtra("seed", 123456789),
                lvl_size.x, lvl_size.y);
        gameLogic.level_difficulty = difficulty;

        gameLogic.usesJoystick = sPref.getBoolean("uses_joystick", true);

        if (!gameLogic.usesJoystick){
            tiltController = new TiltController();
            gameLogic.tiltControler = tiltController;
        }

        gameRenderer.setOnTouchListener(touchListener);
        gameRenderer.setGameLogic(gameLogic);
        loadData();
        setContentView(gameRenderer);
//        mainLayout.addView();
//        mainLayout.addView(new Background(this));
        touchListener.setRenderer(gameRenderer);

        mInterstitialAd = newInterstitialAd();
        loadInterstitial();
    }
    @Override
    protected void onRestart(){
        super.onRestart();
    }
    @Override
    protected void onStart(){
        super.onStart();
    }
    @Override
    protected void onResume(){
        super.onResume();
        //Logger.getAnonymousLogger().info("GameActivity::onResume()");

        if (tiltController != null) {
            tiltController.registerSensors();
        }
    }
    @Override
    protected void onPause(){
        saveData();
        if (tiltController != null) {
            tiltController.unregisterSensors();
        }
        super.onPause();
    }
    @Override
    protected void onStop(){
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //TODO переделать. в resultCode должно быть -1. остальное в интент
        if (resultCode == "next".hashCode()){
            showInterstitial();
        }
        if (resultCode == "menu".hashCode()){
            saveData();
            finish();
        }
        if (resultCode == "pointer".hashCode()){
            gameLogic.activatePointer();
            gameLogic.remote_move_flag = true;
        }
        if (resultCode == "teleport".hashCode()){
            gameLogic.teleportActive = true;
            gameLogic.remote_move_flag = true;
        }
        if (resultCode == "path".hashCode()){
            gameLogic.pathfinderActive = true;
            gameLogic.remote_move_flag = true;
        }
        if (resultCode == "abort".hashCode()){
            gameLogic.remote_move_flag = true;
        }
    }

    void saveData() {
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        //ed.putFloat("global_scale", gameRenderer.globalScale);
        ed.putInt("pointerAmount", gameLogic.pointerAmount);
        ed.putInt("teleportAmount", gameLogic.teleportAmount);
        ed.putInt("pathfinderAmount", gameLogic.pathfinderAmount);
        ed.commit(); //ed.apply();
    }
    void loadData() {
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        //gameRenderer.globalScale = sPref.getFloat("global_scale", 3);
        gameLogic.pointerAmount = sPref.getInt("pointerAmount", 0);
        gameLogic.pathfinderAmount = sPref.getInt("pathfinderAmount", 0);
        gameLogic.teleportAmount = sPref.getInt("teleportAmount", 0);
    }

    void goToNextLevel(){
        gameLogic.isInited = false;
        Point size = difficultyToActualSize(gameLogic.level_difficulty);
        gameLogic.init(size.x, size.y);
        gameRenderer.buttons.get(0).onClick(); // center to player
        ((GameRenderer.PlayerFinder)gameRenderer.buttons.get(0)).instantAnimation();
        gameRenderer.onTouchUp(null);
        gameRenderer.resetLab();
        gameRenderer.resetFog();
        gameRenderer.lightFog(gameLogic.playerCoords());

        mInterstitialAd = newInterstitialAd();
        loadInterstitial();
    }
    private InterstitialAd newInterstitialAd() {
        InterstitialAd interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        interstitialAd.setAdListener(new AdListener() {
//            @Override
//            public void onAdLoaded() {
//            }

//            @Override
//            public void onAdFailedToLoad(int errorCode) {
//                mNextLevelButton.setEnabled(true);
//            }

            @Override
            public void onAdClosed() {
                goToNextLevel();
            }
        });
        return interstitialAd;
    }
    void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        mInterstitialAd.loadAd(adRequest);
    }
    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and reload the ad.
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
        else {
            goToNextLevel();
            loadInterstitial();
        }
    }

    class TiltController implements VelosityControllerInterface{

        SensorManager sensorManager;
        Sensor sensorAccel;
        Sensor sensorLinAccel;
        Sensor sensorGravity;

        float[] valuesAccel = new float[3];
        float[] valuesAccelMotion = new float[3];
        float[] valuesAccelGravity = new float[3];
        float[] valuesGravity = new float[3];

        SensorEventListener listener = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        for (int i = 0; i < 3; i++) {
                            valuesAccel[i] = event.values[i];
                            valuesAccelGravity[i] = (float) (0.1 * event.values[i] + 0.9 * valuesAccelGravity[i]);
                            valuesAccelMotion[i] = event.values[i]
                                    - valuesAccelGravity[i];
                        }
                        break;
                    case Sensor.TYPE_GRAVITY:
                        for (int i = 0; i < 3; i++) {
                            valuesGravity[i] = event.values[i];
                        }
                        break;
                }
            }
        };

        TiltController(){
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorLinAccel = sensorManager
                    .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        }

        void registerSensors(){
            int period = SensorManager.SENSOR_DELAY_GAME;
            sensorManager.registerListener(listener, sensorAccel,
                    period);
            sensorManager.registerListener(listener, sensorLinAccel,
                    period);
            sensorManager.registerListener(listener, sensorGravity,
                    period);
        }
        void unregisterSensors(){
            sensorManager.unregisterListener(listener);
        }

        CPoint.Game speed_vector = new CPoint.Game(0.f, 0.f);
        final float coef = 1.7f;
        private long lastCallTime_ms = 0;

        private long getTimeSinceLastCall(){
            long result = System.currentTimeMillis() - lastCallTime_ms;
            lastCallTime_ms = System.currentTimeMillis();
            return result;
        }

        @Override
        public CPoint.Game getCurrentVelocity() {
            speed_vector.x = -valuesGravity[0] * coef;
            speed_vector.y = valuesGravity[1] * coef;
            //changeSpeedVector();
            return speed_vector;
        }
    }
}

// #b80f0a #57a0d2 dark
// #d25e78 #6fc5c1 red & cyan | light
// #008eb3 #f4656d red & cyan | mid
// #5a6794 #a9794b gray & yellow | neutral
// #1e508c #148f67 analog blue
// #00a8ff #fec601 blue & yellow | bright
// #7b297d #2b0549 deep purple
// #d40e37 #e6ee9f red & yellow/green | light
// #ff7caa #------ very light version
// #dcd549 #ff4274 red & yellow | mid
// #ffc501 #da2b90 yellow & pink | bright
// #1e508c #da2b90 blue & pink | deep