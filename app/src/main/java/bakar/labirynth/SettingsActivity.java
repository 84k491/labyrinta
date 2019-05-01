package bakar.labirynth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Xml;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SettingsActivity extends Activity implements View.OnClickListener {

    Button joystick;
    Button debug;
    Button music;
    ConstraintLayout layout;

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.joystick_bt:
                setState(joystick, StoredProgress.getInstance().switchUsesJoystick());
                break;
            case R.id.debug_bt:
                setState(debug, StoredProgress.getInstance().switchIsBebug());
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_settings);
        joystick = findViewById(R.id.joystick_bt);
        debug = findViewById(R.id.debug_bt);
        music = findViewById(R.id.music_bt);

        setState(joystick, StoredProgress.getInstance().getUsesJoystick());
        setState(debug, StoredProgress.getInstance().getIsBebug());
        setState(music, true);

        joystick.setOnClickListener(this);
        debug.setOnClickListener(this);
        music.setOnClickListener(this);

        joystick.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        debug.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        music.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );

        float text_size = 25.f;
        ((TextView)findViewById(R.id.joystick_tw)).setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        ((TextView)findViewById(R.id.joystick_tw)).setTextSize(text_size);
        ((TextView)findViewById(R.id.music_tw)).setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        ((TextView)findViewById(R.id.music_tw)).setTextSize(text_size);
        ((TextView)findViewById(R.id.debug_tw)).setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        ((TextView)findViewById(R.id.debug_tw)).setTextSize(text_size);

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
                    setResult("settings_finished".hashCode());
                    finish();
                }
            }
        });
    }

    void setState(Button bt, boolean is_on){

        int id = is_on
                ?
                R.xml.switch_on
                :
                R.xml.switch_off;

        String text = is_on
                ?
                "On"
                :
                "Off";
        bt.setText(text);

        try{
            Drawable bg;
            bg = Drawable.createFromXml(getResources(), getResources().getXml(id));
            XmlPullParser parser = getResources().getXml(id);
            bg.inflate(getResources(), parser, Xml.asAttributeSet(parser));

            bt.setBackground(bg);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
