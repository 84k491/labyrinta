package bakar.labyrinta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import java.util.logging.Logger;

import static bakar.labyrinta.StoredProgress.getInstance;
import static bakar.labyrinta.TutorialKey.BeginTutorial_1;
import static bakar.labyrinta.TutorialKey.BeginTutorial_2;
import static bakar.labyrinta.TutorialKey.BeginTutorial_3;
import static bakar.labyrinta.TutorialKey.NextLevelBuyTutorial;

public class GameActivity extends Activity{

    // HIGH PRIORITY
    // TODO: 9/4/19 npe @ shop activity
    // TODO: 8/29/19 skins
    // TODO: 8/14/19 в туториале дописать, что можно найти бонусы
    // TODO: 5/5/19 max level gz tutorial
    // TODO: 6/25/19 change scaling speed
    // TODO: 3/18/19 coin icons
    // TODO: 8/24/19 подписи к бонусам в магазине
    // TODO: 6/25/19 кнопки должны нажиматься при нажатии и отпускаться при отпускании

    // TODO: 8/29/19 Сделать комбо с монетами
    //  (если следующую монету поднять в течении какого-то времени, то она даст больше денех)

    // LOW PRIORITY
    // TODO: 8/26/19 сделать центровку между шариком и выходом
    // TODO: 7/30/19 поменять иконку меню ?
    // TODO: 7/30/19 добавить переворот экрана
    // TODO: 7/30/19 проверить шрифты на низком разрешении
    // TODO: 5/5/19 currency on a same line with cost
    // TODO: 5/18/19 Название съезжает если выйти домой в самом начале
    // TODO: 6/30/19 проверить расположение точек бекграунда. они могут быть поверх всего (после разворачивания)
    // TODO: 6/29/19 make less coins
    // TODO: 5/5/19 doubleclick zoom
    // TODO: 5/1/19 затемнять итемы, которые нельзя купить
    // TODO: 4/20/19 плавное движение от акселерометра
    // TODO: 3/18/19 in-app purchases
    // TODO: 5/1/19 randomizer bonus
    // TODO: 4/20/19 pointer upgrade
    // TODO: 8/5/19 можно открыть настройки и завершение уровня
    // TODO: 19.05.2018 rate this app

    private GameRenderer gameRenderer;
    private GameLogic gameLogic;
    private CustomTouchListener touchListener;
    private SharedPreferences sPref;
    private TiltController tiltController;
    private ConstraintLayout gameLayout;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        final Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.fontScale = 1.0f;
        applyOverrideConfiguration(configuration);
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

    private void init(){
        Logger.getAnonymousLogger().info("GameActivity.init() begin");

        Intent intent = getIntent();
        sPref = getSharedPreferences("global", MODE_PRIVATE);

        int difficulty = intent.getIntExtra("level_size", 1);
        gameLogic = new GameLogic(null, intent.getLongExtra("seed", 123456789), difficulty);
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

        mInterstitialAd = newInterstitialAd();
        loadInterstitial();

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
        gameLayout = findViewById(R.id.cl_game);
        StoredProgress.getInstance().applyActiveSkinToLayout(gameLayout);
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
    private void reInit(){
        gameLogic.reInit();

        runOnUiThread(()->{
            gameRenderer = new GameRenderer(this);
            gameRenderer.setGameLogic(gameLogic);
            gameRenderer.fogEnabled = sPref.getBoolean("fog_enabled", false);

            touchListener = new CustomTouchListener();

            gameRenderer.setOnTouchListener(touchListener);

            gameLogic.gameRenderer = gameRenderer;
            loadData();
            touchListener.setRenderer(gameRenderer);

            //mInterstitialAd = newInterstitialAd();
            loadInterstitial();

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
            gameLayout = findViewById(R.id.cl_game);
            StoredProgress.getInstance().applyActiveSkinToLayout(gameLayout);
            gameLayout.addView(gameRenderer);
//        setContentView(gameRenderer);
            Logger.getAnonymousLogger().info("GameActivity.init() end");
        });

//        gameRenderer.onTouchUp(null);
//        gameRenderer.resetLab();
//        gameRenderer.resetFog();
//        gameRenderer.lightFog(gameLogic.playerCoords());
//
//        gameRenderer.prepareNewLevel();
//
//        gameRenderer.centerCameraTo(new CPoint.Game(0,0));
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
        super.onStart();
        SoundCore.inst().playGameBackgroundMusic();
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

        //StoredProgress.getInstance().setValue(StoredProgress.goldKey, 260000);
        //StoredProgress.getInstance().setValue(StoredProgress.levelUpgKey, 29);
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
        SoundCore.inst().pauseGameBackgroundMusic();
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    private void startTutorialActivity(TutorialKey key){
        Intent intent = new Intent(this, TutorialActivity.class);
        intent.putExtra(TutorialKey.class.toString(), String.valueOf(key));
        this.startActivityForResult(intent, 42);
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
                    if (StoredProgress.getInstance().getValue(StoredProgress.levelUpgKey) >= 3){
                        showInterstitial();
                    }
                    else{
                        new Handler().postDelayed(()-> goToNextLevel(), 200);
                    }
//                    new Handler().postDelayed(()->{
//                        goToNextLevel();
//                    }, 200);
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

    private void saveData() {
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
    private void loadData() {
        // TODO: 6/13/19 use singleton
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        //gameRenderer.globalScale = sPref.getFloat("global_scale", 3);
        gameLogic.pointerAmount = sPref.getInt("pointerAmount", 0);
        gameLogic.pathfinderAmount = sPref.getInt("pathfinderAmount", 0);
        gameLogic.teleportAmount = sPref.getInt("teleportAmount", 0);
    }

    private void updateSettings(){
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

    private void goToNextLevel(){
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

//        Logger.getAnonymousLogger().info("GameActivity setContentView(R.layout.game_layout);");
//        setContentView(R.layout.game_layout);
//        gameLayout = ((ConstraintLayout)findViewById(R.id.cl_game));
//        gameLayout.addView(gameRenderer);
        //setContentView(gameRenderer);

//        mInterstitialAd = newInterstitialAd();
//        loadInterstitial();

        Logger.getAnonymousLogger().info("GameActivity.goToNextLevel() end");
    }

    private InterstitialAd newInterstitialAd() {
        InterstitialAd interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Logger.getAnonymousLogger().info("Ad onAdLoaded()");
            }

//            @Override
//            public void onAdFailedToLoad(int errorCode) {
//                mNextLevelButton.setEnabled(true);
//            }

            @Override
            public void onAdClosed() {
                Logger.getAnonymousLogger().info("Ad onAdClosed()");
                goToNextLevel();
            }
        });
        return interstitialAd;
    }
    private void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        mInterstitialAd.loadAd(adRequest);
    }
    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and reload the ad.
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            Logger.getAnonymousLogger().info("Ad show()");
            mInterstitialAd.show();
        }
        else {
            Logger.getAnonymousLogger().info("Ad notLoaded!!!");
            goToNextLevel();
        }
    }

    class Loader extends Thread{
        final boolean is_first_launch;
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
                runOnUiThread(() -> {
                    Logger.getAnonymousLogger().info("Loader.run() init();");
                    init();
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

        final SensorManager sensorManager;
        Sensor sensorAccel;
        Sensor sensorLinAccel;
        Sensor sensorGravity;
        private boolean registered = false;
        boolean isRegistered(){return registered;}

        final float[] valuesGravity = new float[3];

        final SensorEventListener listener = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                    case Sensor.TYPE_GRAVITY:
                        System.arraycopy(event.values, 0, valuesGravity, 0, 3);
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
            registered = sensorManager.registerListener(listener, sensorGravity,
                    period);
        }
        void unregisterSensors(){
            sensorManager.unregisterListener(listener);
            registered = false;
            valuesGravity[0] = 0.f;
            valuesGravity[1] = 0.f;
            valuesGravity[2] = 0.f;
        }

        final CPoint.Game speed_vector = new CPoint.Game(0.f, 0.f);
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
// #1e508c #da2b90 blue & pink | deep \\\ default
