package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class GameActivity extends Activity{
    //Todo: rename
    //Todo: сделать выбор подожения джойстика и кнопок
    //Todo: заблочить кнопку "назад"
    //Todo: белые кнопки рисуются заново при разворачивании
    // TODO: 02.05.2018 фикс вылетов от переполнения памяти
    // TODO: 05.05.2018 пофиксить низкий фпс с туманом
    // TODO: 15.05.2018 сделать режим на время
    // TODO: 19.05.2018 rate this app
    // TODO: 12/31/18 вылетает если использовать бонус за пределами лабиринта
    GameRenderer gameRenderer;
    GameLogic gameLogic;
    CustomTouchListener touchListener;
    SharedPreferences sPref;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        gameRenderer = new GameRenderer(this);
        gameRenderer.isDebug = intent.getBooleanExtra("is_debug", false);
        gameRenderer.fogEnabled = intent.getBooleanExtra("fog_enabled", false);

        touchListener = new CustomTouchListener();

        gameLogic = new GameLogic(gameRenderer, intent.getLongExtra("seed", 123456789),
                intent.getIntExtra("xsize", 10), intent.getIntExtra("ysize", 10));
        gameLogic.usesJoystick = intent.getBooleanExtra("uses_joystick", true);
        gameRenderer.setOnTouchListener(touchListener);
        gameRenderer.setGameLogic(gameLogic);
        loadData();
        gameLogic.pathfinderAmount = 10;
        gameLogic.pointerAmount = 10;
        gameLogic.teleportAmount = 10;
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
        gameLogic.init(gameLogic.field.getxSize(), gameLogic.field.getySize());
        gameRenderer.buttons.get(0).onClick(); // center to player
        ((GameRenderer.PlayerFinder)gameRenderer.buttons.get(0)).instantAnimation();
        gameRenderer.onTouchUp(null);
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
