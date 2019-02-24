package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Random;

public class MenuActivity extends Activity implements OnClickListener {

    boolean justLoadedState;

    SharedPreferences sPref;
    ConstraintLayout layout;
    Button start;
    Button settings;
    Button shop;
    TextView gold;
    TextView title;

    @Override
    public void onClick(View view) {
        if (justLoadedState){
            justLoadedState = false;
            startOnTouchAnimations();
        }
        else
        {
            switch (view.getId()){
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
    }

    void startLevelSelectActivity(){
        Intent intent = new Intent(this, LevelSelectActivity.class);
        intent.putExtra("max_level_allowed", sPref.getInt("level_upg", 1) + 1);
        startActivityForResult(intent, 1);
    }
    void startGameActivity(int level_size){
        long seed = System.currentTimeMillis();
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("seed", seed);
        intent.putExtra("level_size", level_size);
        startActivity(intent);
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
        if (resultCode == RESULT_OK)
            startGameActivity(data.getIntExtra("level_size", 1));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_menu);

        justLoadedState = true;

        title = findViewById(R.id.main_title);
        start = findViewById(R.id.start);
        start.setOnClickListener(this);
        gold = findViewById(R.id.gold);
        layout = findViewById(R.id.menu_layout);
        layout.setOnClickListener(this);
        settings = findViewById(R.id.settings_bt);
        settings.setOnClickListener(this);
        shop = findViewById(R.id.shop_bt);
        shop.setOnClickListener(this);

        Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/CLiCHE 21.ttf");
        title.setTypeface(custom_font);
    }
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
        loadData();
        if (justLoadedState)
            startWelcomingAnimations();
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

    void saveData() {
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        //ed.putInt("xsize", Integer.parseInt(xsize.getText().toString()));
        //ed.putInt("ysize", Integer.parseInt(xsize.getText().toString()));
        //ed.putBoolean("uses_joystick", joystick.isChecked());
        //ed.putBoolean("is_debug", debug.isChecked());
        //ed.putBoolean("fog_enabled", fog.isChecked());
        ed.commit(); //ed.apply();
    }
    void loadData() {
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        //joystick.setChecked(sPref.getBoolean("uses_joystick", true));
        //debug.setChecked(sPref.getBoolean("is_debug", false));
        //fog.setChecked(sPref.getBoolean("fog_enabled", false));

        //xsize.setText(String.valueOf(sPref.getInt("xsize", 42)));
        //ysize.setText(String.valueOf(sPref.getInt("ysize", 42)));
        gold.setText(String.valueOf(sPref.getInt("gold", 0)));
    }
    void startWelcomingAnimations(){
        Animation move_anim = AnimationUtils.loadAnimation(this, R.anim.keep_title_offset);
        Animation inv_anim = AnimationUtils.loadAnimation(this, R.anim.keep_invisible);
        title.startAnimation(move_anim);

        start.startAnimation(inv_anim);
        shop.startAnimation(inv_anim);
        settings.startAnimation(inv_anim);
        gold.startAnimation(inv_anim);
    }
    void startOnTouchAnimations(){
        Animation move_anim = AnimationUtils.loadAnimation(this, R.anim.title_move);
        Animation inv_anim = new AlphaAnimation(0.0f, 1.0f);
        inv_anim.setDuration(3000);
        inv_anim.setStartOffset(700);
        title.startAnimation(move_anim);

        start.startAnimation(inv_anim);
        shop.startAnimation(inv_anim);
        settings.startAnimation(inv_anim);
        gold.startAnimation(inv_anim);
    }
}
