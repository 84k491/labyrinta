package bakar.labirynth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.widget.EditText;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    SharedPreferences sPref;
    EditText xsize;
    EditText ysize;
    Switch joystick;
    Switch debug;
    Switch fog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_settings);
        xsize = findViewById(R.id.xsize_et);
        ysize = findViewById(R.id.ysize_et);
        joystick = findViewById(R.id.joystick_sw);
        debug = findViewById(R.id.debug_sw);
        fog = findViewById(R.id.fog_sw);
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
        ed.putInt("xsize", Integer.parseInt(xsize.getText().toString()));
        ed.putInt("ysize", Integer.parseInt(ysize.getText().toString()));
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

        xsize.setText(String.valueOf(sPref.getInt("xsize", 42)));
        ysize.setText(String.valueOf(sPref.getInt("ysize", 42)));
        //gold.setText(String.valueOf(sPref.getInt("gold", 0)));
    }
}
