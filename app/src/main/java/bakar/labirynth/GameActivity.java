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
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import static bakar.labirynth.StoredProgress.getInstance;
import static bakar.labirynth.TutorialKey.BeginTutorial_1;
import static bakar.labirynth.TutorialKey.BeginTutorial_2;
import static bakar.labirynth.TutorialKey.BeginTutorial_3;
import static bakar.labirynth.TutorialKey.NextLevelBuyTutorial;

public class GameActivity extends Activity{

    // STEPS-TO-BETA
    // TODO: 7/1/19 check copyrights

    // STEPS-TO-RELEASE
    // TODO: 7/3/19 ignore system font size
    // TODO: 7/3/19 joystick level end lag
    // TODO: 6/30/19 проверить расположение точек бекграунда. они могут быть поверх всего
    // TODO: 3/18/19 coin icons
    // TODO: 6/24/19 fix passing an exit on high speed
    // TODO: 6/25/19 кнопки должны нажиматься при нажатии и отпускаться при отпускании
    // TODO: 6/25/19 change scaling speed

    //after release
    // TODO: 5/5/19 currency on a same line with cost
    // TODO: 5/18/19 Название съезжает если выйти домой в самом начале
    // TODO: 6/29/19 make less coins
    // TODO: 5/5/19 max level gz tutorial
    // TODO: 5/5/19 doubleclick zoom
    // TODO: 5/1/19 затемнять итемы, которые нельзя купить
    // TODO: 19.05.2018 rate this app
    // TODO: 4/20/19 плавное движение от акселерометра
    // TODO: 3/18/19 in-app purchases
    // TODO: 3/18/19 gold for video
    // TODO: 5/1/19 randomizer bonus
    // TODO: 4/20/19 pointer upgrade

    GameRenderer gameRenderer;
    GameLogic gameLogic;
    CustomTouchListener touchListener;
    SharedPreferences sPref;
    TiltController tiltController;
    ConstraintLayout gameLayout;
    //private InterstitialAd mInterstitialAd;

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

        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        Logger.getAnonymousLogger().info("GameActivity.onCreate()");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().setFormat(PixelFormat.RGBA_8888);
        Logger.getAnonymousLogger().info("GameActivity.onCreate() setContentView(R.layout.loading_screen)");
        setContentView(R.layout.loading_screen);

        if (!StoredProgress.getInstance().getValueBoolean(StoredProgress.usesJoystickKey)){
            tiltController = new TiltController();
        }

    }

    void init(){
        Logger.getAnonymousLogger().info("GameActivity.init() begin");

        Intent intent = getIntent();
        sPref = getSharedPreferences("global", MODE_PRIVATE);

        int difficulty = intent.getIntExtra("level_size", 1);
        Point lvl_size = difficultyToActualSize(difficulty);
        gameLogic = new GameLogic(null, intent.getLongExtra("seed", 123456789),
                Math.min(lvl_size.x, lvl_size.y), Math.max(lvl_size.x, lvl_size.y));
        gameLogic.level_difficulty = difficulty;
        gameLogic.usesJoystick = sPref.getBoolean("uses_joystick", false);
        if (!gameLogic.usesJoystick){
            //tiltController = new TiltController();
            gameLogic.tiltControler = tiltController;
        }

        gameRenderer = new GameRenderer(this);
        gameRenderer.setGameLogic(gameLogic);
        gameRenderer.fogEnabled = sPref.getBoolean("fog_enabled", false);

        touchListener = new CustomTouchListener();

        gameRenderer.setOnTouchListener(touchListener);

        gameLogic.gameRenderer = gameRenderer;
        loadData();
        touchListener.setRenderer(gameRenderer);

//        mInterstitialAd = newInterstitialAd();
//        loadInterstitial();

        while (!gameRenderer.threadIsStarted() || gameRenderer.threadIsDoingPreparations()){
            try{
                Thread.sleep(100);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        Logger.getAnonymousLogger().info("GameActivity.init() changing surface to game_layout");
        setContentView(R.layout.game_layout);
        gameLayout = ((ConstraintLayout)findViewById(R.id.cl_game));
        gameLayout.addView(gameRenderer);
//        setContentView(gameRenderer);
        Logger.getAnonymousLogger().info("GameActivity.init() end");

        if (getInstance().getValueBoolean(StoredProgress.isNeedToShowTutorialFirst)){
            getInstance().
                    switchValueBoolean(StoredProgress.isNeedToShowTutorialFirst);
            Intent tutorialIntent = new Intent(this, TutorialActivity.class);
            tutorialIntent.putExtra(TutorialKey.class.toString(), String.valueOf(BeginTutorial_1));
            startActivityForResult(tutorialIntent, 42);
        }
    }
    void reInit(){
        gameLogic.isInited = false;
        Point size = difficultyToActualSize(gameLogic.level_difficulty);
        gameLogic.init(Math.min(size.x, size.y), Math.max(size.x, size.y));
        gameRenderer.onTouchUp(null);
        gameRenderer.resetLab();
        gameRenderer.resetFog();
        gameRenderer.lightFog(gameLogic.playerCoords());

        gameRenderer.prepareNewLevel();

        gameRenderer.centerCameraTo(new CPoint.Game(0,0));
    }
    @Override
    public void onBackPressed(){
        Intent intent = new Intent(this, ConfirmationActivity.class);
        startActivityForResult(intent,
                1);
    }
    @Override
    protected void onRestart(){
        super.onRestart();
    }
    @Override
    protected void onStart(){
        SoundCore.inst().playBackgroungMusic();
        super.onStart();
    }
    @Override
    protected void onResume(){
        super.onResume();
        Logger.getAnonymousLogger().info("GameActivity.onResume()");

        if (gameRenderer == null){
            new Handler().postDelayed(()->{
                Logger.getAnonymousLogger().info("GameActivity new Loader()");
                Loader loader = new Loader(true);
                loader.start();
            }, 100);
        }

        if (tiltController != null) {
            Logger.getAnonymousLogger().info("GameActivity tiltController.registerSensors()");
            tiltController.registerSensors();
        }

        //startTutorialActivity(BeginTutorial_1); // 4 test
    }
    @Override
    protected void onPause(){
        saveData();
        if (tiltController != null) {
            Logger.getAnonymousLogger().info("GameActivity tiltController.unregisterSensors()");
            tiltController.unregisterSensors();
        }
        super.onPause();
    }
    @Override
    protected void onStop(){
        SoundCore.inst().pauseBackgroundMusic();
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    void startTutorialActivity(TutorialKey key){
        Intent intent = new Intent(this, TutorialActivity.class);
        intent.putExtra(TutorialKey.class.toString(), String.valueOf(key));
        this.startActivityForResult(intent, EndActivity.class.toString().hashCode());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Logger.getAnonymousLogger().info("GameActivity.onActivityResult()");

        if (-1 == resultCode){
            if (TutorialKey.class.toString().equals(intent.getStringExtra("what_from"))){
                TutorialKey finishedTutorial = TutorialKey.valueOf
                        (intent.getStringExtra(TutorialKey.class.toString()));

                Map<TutorialKey, TutorialKey> finishedToNext = new HashMap<>();
                finishedToNext.put(BeginTutorial_1, BeginTutorial_2);
                finishedToNext.put(BeginTutorial_2, BeginTutorial_3);

                if (NextLevelBuyTutorial == finishedTutorial){
                    // после этого туториала должно появиться EndMenu
                    gameLogic.startEndActivity();
                }

                if (finishedToNext.get(finishedTutorial) != null){
                    Intent tutorialIntent = new Intent(this, TutorialActivity.class);
                    tutorialIntent.putExtra
                            (TutorialKey.class.toString(),
                                    String.valueOf(finishedToNext.get(finishedTutorial)));

                    startActivityForResult(tutorialIntent, 42);
                }
            }

            if (EndActivity.class.toString().equals(intent.getStringExtra("what_from"))) {
                Runnable fn =  () -> {
                    Logger.getAnonymousLogger().info("GameActivity setContentView(R.layout.loading_screen);");
                    gameLayout.removeView(gameRenderer);
                    setContentView(R.layout.loading_screen);
                    gameLayout = null;
//                    if (StoredProgress.getInstance().getValue(StoredProgress.levelUpgKey) >= 5){
//                        showInterstitial();
//                    }
//                    else{
//                        new Handler().postDelayed(()->{
//                            goToNextLevel();
//                        }, 200);
//                    }
                    new Handler().postDelayed(()->{
                        goToNextLevel();
                    }, 200);
                };
                if (intent.getStringExtra("result").equals("next")){
                    fn.run();
                }
                if (intent.getStringExtra("result").equals("menu")){
                    saveData();
                    finish();
                }
                if (intent.getStringExtra("result").equals("load_max_level")){
                    Logger.getAnonymousLogger().info("GameActivity loading max_level;");
                    gameLogic.level_difficulty = StoredProgress.getInstance().getValue(StoredProgress.levelUpgKey);
                    fn.run();
                }
            }

            if (BonusActivity.class.toString().equals(intent.getStringExtra("what_from"))){
                Runnable startBonusRangeTutorial = () -> {
                    if (StoredProgress.getInstance().getValueBoolean(
                            StoredProgress.isNeedToShowTutorialBonusRange)){
                        startTutorialActivity(TutorialKey.BonusRangeTutorial);
                        StoredProgress.getInstance().
                                switchValueBoolean(StoredProgress.isNeedToShowTutorialBonusRange);
                    }
                };
                if (intent.getStringExtra("result").equals("teleport")){
                    gameLogic.teleportActive = true;
                    gameLogic.remote_move_flag = true;
                    startBonusRangeTutorial.run();
                }
                if (intent.getStringExtra("result").equals("pointer")){
                    gameLogic.activatePointer();
                    gameLogic.remote_move_flag = true;
                }
                if (intent.getStringExtra("result").equals("path")){
                    gameLogic.pathfinderActive = true;
                    gameLogic.remote_move_flag = true;
                    startBonusRangeTutorial.run();
                }
                if (intent.getStringExtra("result").equals("abort_bonus")){
                    gameLogic.remote_move_flag = true;
                }
            }

            if (SettingsActivity.class.toString().equals(intent.getStringExtra("what_from"))){
                if (intent.getStringExtra("result").equals("settings_finished")){
                    updateSettings();
                }
            }

            if (ConfirmationActivity.class.toString().equals(intent.getStringExtra("what_from"))){
                if (intent.getStringExtra("result").equals("confirm_yes")){
                    saveData();
                    finish();
                }
            }
        }
    }

    void saveData() {
        // TODO: 6/13/19 use singleton
        if (null == gameLogic) return;
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        //ed.putFloat("global_scale", gameRenderer.globalScale);
        ed.putInt("pointerAmount", gameLogic.pointerAmount);
        ed.putInt("teleportAmount", gameLogic.teleportAmount);
        ed.putInt("pathfinderAmount", gameLogic.pathfinderAmount);
        ed.commit(); //ed.apply();
    }
    void loadData() {
        // TODO: 6/13/19 use singleton
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        //gameRenderer.globalScale = sPref.getFloat("global_scale", 3);
        gameLogic.pointerAmount = sPref.getInt("pointerAmount", 0);
        gameLogic.pathfinderAmount = sPref.getInt("pathfinderAmount", 0);
        gameLogic.teleportAmount = sPref.getInt("teleportAmount", 0);
    }

    void updateSettings(){
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        gameLogic.usesJoystick = StoredProgress.getInstance().getUsesJoystick();
        if (!gameLogic.usesJoystick){
            if (null == tiltController){
                tiltController = new TiltController();
            }
            gameLogic.tiltControler = tiltController;
            if (!tiltController.isRegistered()){
                tiltController.registerSensors();
            }
        }
        else{
            gameRenderer.createJoystick();
            if (tiltController != null) {
                tiltController.unregisterSensors();
            }
        }
    }

    void goToNextLevel(){
        Logger.getAnonymousLogger().info("GameActivity.goToNextLevel() start");

        Loader loader = new Loader(false);
        loader.start();
        loader.is_running = true;

        while (loader.is_running ||
                gameRenderer.threadIsPrepairingNewLevel() ||
                gameRenderer.threadIsDoingPreparations()){
            try{
                Thread.sleep(100);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        Logger.getAnonymousLogger().info("GameActivity setContentView(R.layout.game_layout);");
        setContentView(R.layout.game_layout);
        gameLayout = ((ConstraintLayout)findViewById(R.id.cl_game));
        gameLayout.addView(gameRenderer);
        //setContentView(gameRenderer);

//        mInterstitialAd = newInterstitialAd();
//        loadInterstitial();

        Logger.getAnonymousLogger().info("GameActivity.goToNextLevel() end");
    }
//    private InterstitialAd newInterstitialAd() {
//        InterstitialAd interstitialAd = new InterstitialAd(this);
//        interstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
//        interstitialAd.setAdListener(new AdListener() {
////            @Override
////            public void onAdLoaded() {
////            }
//
////            @Override
////            public void onAdFailedToLoad(int errorCode) {
////                mNextLevelButton.setEnabled(true);
////            }
//
//            @Override
//            public void onAdClosed() {
//                goToNextLevel();
//            }
//        });
//        return interstitialAd;
//    }
//    void loadInterstitial() {
//        AdRequest adRequest = new AdRequest.Builder()
//                .setRequestAgent("android_studio:ad_template").build();
//        mInterstitialAd.loadAd(adRequest);
//    }
//    private void showInterstitial() {
//        // Show the ad if it's ready. Otherwise toast and reload the ad.
//        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
//            mInterstitialAd.show();
//        }
//        else {
//            goToNextLevel();
//        }
//    }

    class Loader extends Thread{
        boolean is_first_launch;
        boolean is_running = false;

        Loader(boolean _is_first_launch){
            is_first_launch = _is_first_launch;
        }
        @Override
        public void run(){
            is_running = true;
            Logger.getAnonymousLogger().info("Loader.run() begin");
            try{
                sleep(100);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }

            if (is_first_launch){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Logger.getAnonymousLogger().info("Loader.run() init();");
                        init();
                    }
                });
            }
            else{
                Logger.getAnonymousLogger().info("Loader.run() reInit();");
                reInit();
            }

            Logger.getAnonymousLogger().info("Loader.run() end");
            is_running = false;
        }
    }

    class TiltController implements VelosityControllerInterface{

        SensorManager sensorManager;
        Sensor sensorAccel;
        Sensor sensorLinAccel;
        Sensor sensorGravity;
        private boolean registered = false;
        boolean isRegistered(){return registered;}

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
//                        for (int i = 0; i < 3; i++) {
//                            valuesAccel[i] = event.values[i];
//                            valuesAccelGravity[i] = (float) (0.1 * event.values[i] + 0.9 * valuesAccelGravity[i]);
//                            valuesAccelMotion[i] = event.values[i]
//                                    - valuesAccelGravity[i];
//                        }
                        for (int i = 0; i < 3; i++) {
                            valuesGravity[i] = event.values[i];
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

            sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            if (null == sensorGravity){
                Logger.getAnonymousLogger().info("No Sensor.TYPE_GRAVITY!! Using Sensor.TYPE_ACCELEROMETER");
                sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
        }

        void registerSensors(){
            int period = SensorManager.SENSOR_DELAY_GAME;
            boolean result = false;
//            result = sensorManager.registerListener(listener, sensorAccel,
//                    period);
//            result = sensorManager.registerListener(listener, sensorLinAccel,
//                    period);
            result = sensorManager.registerListener(listener, sensorGravity,
                    period);

            //registered = true;
            registered = result;
        }
        void unregisterSensors(){
            sensorManager.unregisterListener(listener);
            registered = false;
            valuesGravity[0] = 0.f;
            valuesGravity[1] = 0.f;
            valuesGravity[2] = 0.f;
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
            if (gameRenderer != null){
                if (gameLogic.teleportActive || gameLogic.pathfinderActive){
                    return new CPoint.Game(0.f,0.f);
                }
            }
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
