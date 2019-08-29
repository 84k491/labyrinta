package bakar.labyrinta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.google.android.gms.ads.MobileAds;

import java.util.Timer;
import java.util.TimerTask;

public class MenuActivity extends Activity implements OnClickListener {

    boolean justLoadedState;

    private TranslateAnimation keep_offset_anim;
    private AlphaAnimation keep_inv_anim;
    private TranslateAnimation title_move_anim;
    private AlphaAnimation appear_anim;

    private int touched_view_id;

    SharedPreferences sPref;
    ConstraintLayout layout;
    Button start;
    Button settings;
    Button shop;
    TextView title;

    Animation on_click_anim;
    Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            switch (touched_view_id){
                case R.id.start:
                    //startGameActivity();
                    startLevelSelectActivity();
                    break;
                case R.id.settings_bt:
                    startSettingsActivity();
                    break;
                case R.id.shop_bt:
                    startShopActivity();
                    break;
                default: break;
            }
        }

        @Override
        public void onAnimationEnd(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        final Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.fontScale = 1.0f;
        applyOverrideConfiguration(configuration);
    }

    @Override
    public void onClick(View view) {
        touched_view_id = view.getId();

//        if (justLoadedState){
//            justLoadedState = false;
//            startOnTouchAnimations();
//        }
//        else
//        {
//        }
        if (touched_view_id == R.id.start ||
                touched_view_id == R.id.settings_bt ||
                touched_view_id == R.id.shop_bt){
            view.startAnimation(on_click_anim);
        }
    }

    void startLevelSelectActivity(){
        Intent intent = new Intent(this, LevelSelectActivity.class);
        intent.putExtra("max_level_allowed", sPref.getInt("level_upg", 1)); //fixme
        startActivityForResult(intent, 42);
    }
    void startGameActivity(int level_size){
        long seed = System.currentTimeMillis();
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("seed", seed);
        intent.putExtra("level_size", level_size);
        startActivityForResult(intent, 42);
    }
    void startSettingsActivity(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    void startResearchActivity(){
        Intent intent = new Intent(this, ResearchActivity.class);
        startActivity(intent);
    }
    void startShopActivity(){
        Intent intent = new Intent(this, ShopActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if (data.getBooleanExtra("startShop", false)){
                startShopActivity();
            }
            else{
                if (data.getIntExtra("level_size", -1) > 0){
                    startGameActivity(data.getIntExtra("level_size", 1));
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_menu);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        StoredProgress.getInstance().
                setSharedPreferences(getSharedPreferences("global", MODE_PRIVATE));

        MobileAds.initialize(this, getString(R.string.app_id_ad));

        justLoadedState = true;

        title = findViewById(R.id.main_title);
        start = findViewById(R.id.start);
        start.setOnClickListener(this);
        layout = findViewById(R.id.menu_layout);
        layout.setOnClickListener(this);
        settings = findViewById(R.id.settings_bt);
        settings.setOnClickListener(this);
        shop = findViewById(R.id.shop_bt);
        shop.setOnClickListener(this);

        title.setTypeface(StoredProgress.getInstance().getTitleFont(getAssets()));

        on_click_anim = AnimationUtils.loadAnimation(this, R.anim.on_menu_button_tap);
        on_click_anim.setAnimationListener(animationListener);

        SoundCore.inst().loadSounds(this);
    }
    protected void onRestart(){
        super.onRestart();
    }
    @Override
    protected void onStart(){
        SoundCore.inst().playMenuBackgroundMusic();
        super.onStart();
    }
    @Override
    protected void onResume(){
        super.onResume();
        loadData();
        if (justLoadedState){
            justLoadedState = false;
            setAnimations();
            startWelcomingAnimations();

            Timer timer = new Timer();
            timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MenuActivity.this.startOnTimerAnimations();
                                }
                            });
                        }
                    }, 500
            );
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
    }
    @Override
    protected void onStop(){
        SoundCore.inst().pauseMenuBackgroundMusic();
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        SoundCore.inst().releaseMP();
    }

    void loadData() {
        sPref = getSharedPreferences("global", MODE_PRIVATE);
    }

    void setAnimations(){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height_center = size.y / 4; // TODO: 5/6/19 узнать почему 4 а не 2

        keep_offset_anim = new TranslateAnimation(
                title.getX(),
                title.getX(),
                height_center,
                height_center);
        keep_offset_anim.setDuration(10000);
        keep_offset_anim.setRepeatCount(1000);

        keep_inv_anim = new AlphaAnimation(0.f,0.f);
        keep_inv_anim.setDuration(10000);
        keep_inv_anim.setRepeatCount(1000);

        title_move_anim = new TranslateAnimation(
                title.getX(),
                title.getX(),
                height_center,
                title.getY());
        title_move_anim.setDuration(2000);

        appear_anim = new AlphaAnimation(0.0f, 1.0f);
        appear_anim.setDuration(3000);
        appear_anim.setStartOffset(700);
    }

    void startWelcomingAnimations(){
        title.startAnimation(keep_offset_anim);

        start.startAnimation(keep_inv_anim);
        shop.startAnimation(keep_inv_anim);
        settings.startAnimation(keep_inv_anim);
    }
    void startOnTimerAnimations(){
        title.startAnimation(title_move_anim);

        start.startAnimation(appear_anim);
        shop.startAnimation(appear_anim);
        settings.startAnimation(appear_anim);
    }
}
