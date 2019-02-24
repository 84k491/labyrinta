package bakar.labirynth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    SharedPreferences sPref;
    Switch joystick;
    Switch debug;
    Switch fog;
    Button reset_bt;

    void resetSavedData(){
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt("level_upg", 0);
        ed.putInt("teleportAmount", 0);
        ed.putInt("pathfinderAmount", 0);
        ed.putInt("pointerAmount", 0);
        ed.putInt("tp_upg", 0);
        ed.putInt("pf_upg", 0);
        ed.putInt("pt_upg", 0);
        ed.putInt("gold", 0);
        ed.putInt("level_upg", 0);
        ed.putInt("level_upg", 0);
        ed.putInt("level_upg", 0);
        ed.putInt("level_upg", 0);

        ed.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_settings);
        joystick = findViewById(R.id.joystick_sw);
        debug = findViewById(R.id.debug_sw);
        fog = findViewById(R.id.fog_sw);
        reset_bt = findViewById(R.id.bt_reset);

        reset_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetSavedData();
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
        ed.putBoolean("fog_enabled", fog.isChecked());
        ed.commit(); //ed.apply();
    }
    void loadData() {
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        joystick.setChecked(sPref.getBoolean("uses_joystick", true));
        debug.setChecked(sPref.getBoolean("is_debug", false));
        fog.setChecked(sPref.getBoolean("fog_enabled", false));
    }
}
