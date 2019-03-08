package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.Random;

public class GameActivity extends Activity{
    //Todo: rename
    //Todo: сделать выбор подожения джойстика и кнопок
    //Todo: заблочить кнопку "назад"
    //Todo: белые кнопки рисуются заново при разворачивании
    // TODO: 15.05.2018 сделать режим на время
    // TODO: 19.05.2018 rate this app
    // TODO: 12/31/18 вылетает если использовать бонус за пределами лабиринта
    // TODO: 1/27/19 mutex на вектор с предметами (отрисовка и удаление в разных потоках)
    // todo OOM при переходе на след. уровень
    GameRenderer gameRenderer;
    GameLogic gameLogic;
    CustomTouchListener touchListener;
    SharedPreferences sPref;
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

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        //getWindow().setFormat(PixelFormat.RGBA_8888);

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
        gameRenderer.setOnTouchListener(touchListener);
        gameRenderer.setGameLogic(gameLogic);
        loadData();
        setContentView(gameRenderer);
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
    }
    @Override
    protected void onPause(){
        saveData();
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
        }
        if (resultCode == "teleport".hashCode()){
            gameLogic.teleportActive = true;
        }
        if (resultCode == "path".hashCode()){
            gameLogic.pathfinderActive = true;
        }
        if (resultCode == "abort".hashCode()){}
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
}
