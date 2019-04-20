package bakar.labirynth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    SharedPreferences sPref;
    Switch joystick;
    Switch debug;
    ConstraintLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_settings);
        joystick = findViewById(R.id.joystick_sw);
        debug = findViewById(R.id.debug_sw);

        findViewById(R.id.settings_imageview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.settings_imageview){
                    return;
                }
            }
        });

        findViewById(R.id.settings_bg_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.settings_bg_image){
                    finish();
                }
            }
        });
    }
    @Override
    protected void onResume(){
        super.onResume();
        loadData();
    }
    protected void onPause(){
        saveData();
        super.onPause();
    }

    void saveData() {
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putBoolean("uses_joystick", joystick.isChecked());
        ed.putBoolean("is_debug", debug.isChecked());
        ed.apply();
    }
    void loadData() {
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        joystick.setChecked(sPref.getBoolean("uses_joystick", true));
        debug.setChecked(sPref.getBoolean("is_debug", false));
    }
}
