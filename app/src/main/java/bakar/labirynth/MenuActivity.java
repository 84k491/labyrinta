package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Random;

public class MenuActivity extends Activity implements OnClickListener {

    Button start;
    Switch joystick;
    Switch debug;
    Switch fog;
    EditText xsize;
    EditText ysize;
    SharedPreferences sPref;
    TextView gold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_menu);

        TextView tx = findViewById(R.id.main_title);
        Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/CLiCHE 21.ttf");
        tx.setTypeface(custom_font);

        ysize = findViewById(R.id.ysize);
        xsize = findViewById(R.id.xsize);
        start = findViewById(R.id.start);
        start.setOnClickListener(this);
        joystick = findViewById(R.id.joystick);
        joystick.setOnClickListener(this);
        debug = findViewById(R.id.debug);
        debug.setOnClickListener(this);
        fog = findViewById(R.id.fog);
        fog.setOnClickListener(this);
        gold = findViewById(R.id.gold);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.start:
                startGameActivity();
                break;
            default: break;
        }
    }

    void startGameActivity(){
        long seed = System.currentTimeMillis();
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("seed", seed);
        intent.putExtra("xsize", Integer.parseInt(xsize.getText().toString()));
        intent.putExtra("ysize", Integer.parseInt(ysize.getText().toString()));
        intent.putExtra("uses_joystick", joystick.isChecked());
        intent.putExtra("is_debug", debug.isChecked());
        intent.putExtra("fog_enabled", fog.isChecked());
        startActivity(intent);
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
        ed.putInt("xsize", Integer.parseInt(xsize.getText().toString()));
        ed.putInt("ysize", Integer.parseInt(xsize.getText().toString()));
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
        gold.setText(String.valueOf(sPref.getInt("gold", 0)));
    }
}
