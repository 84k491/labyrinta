package bakar.labirynth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Xml;
import android.view.Gravity;
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
    Button sounds;
    Button music;

    TextView joystick_TextView;
    TextView sounds_TextView;
    TextView music_TextView;
    ConstraintLayout layout;

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.joystick_bt:
                setState(joystick, StoredProgress.getInstance().switchUsesJoystick());
                break;
            case R.id.sounds_bt:
                SoundCore.inst().doPlaySounds = !SoundCore.inst().doPlaySounds;
                SoundCore.inst().doPlayMusic = !SoundCore.inst().doPlayMusic;
                StoredProgress.getInstance().setValue("isSoundsOn",
                        !StoredProgress.getInstance().getValueBoolean("isSoundsOn")
                );
                setState(sounds, SoundCore.inst().doPlaySounds);
                break;
            case R.id.music_bt:
                SoundCore.inst().doPlayMusic = !SoundCore.inst().doPlayMusic;
                StoredProgress.getInstance().setValue("isMusicOn",
                        !StoredProgress.getInstance().getValueBoolean("isMusicOn")
                );
                setState(music, SoundCore.inst().doPlayMusic);

                if (SoundCore.inst().doPlayMusic){
                    SoundCore.inst().playBackgroungMusicForced();
                }
                else{
                    SoundCore.inst().pauseBackgroundMusicForced();
                }
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
        sounds = findViewById(R.id.sounds_bt);
        music = findViewById(R.id.music_bt);

        setState(joystick, StoredProgress.getInstance().getUsesJoystick());
        setState(sounds, SoundCore.inst().doPlaySounds);
        setState(music, SoundCore.inst().doPlayMusic);

        joystick.setOnClickListener(this);
        sounds.setOnClickListener(this);
        music.setOnClickListener(this);

        joystick.setGravity(Gravity.FILL_VERTICAL | Gravity.CENTER_HORIZONTAL);
        sounds.setGravity(Gravity.FILL_VERTICAL | Gravity.CENTER_HORIZONTAL);
        music.setGravity(Gravity.FILL_VERTICAL | Gravity.CENTER_HORIZONTAL);

        joystick.setTextSize(25.f);
        sounds.setTextSize(25.f);
        music.setTextSize(25.f);

        joystick.setTextColor(Color.WHITE);
        sounds.setTextColor(Color.WHITE);
        music.setTextColor(Color.WHITE);

        joystick.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        sounds.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        music.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );

        float text_size = 25.f;

        joystick_TextView = ((TextView)findViewById(R.id.joystick_tw));
        music_TextView = ((TextView)findViewById(R.id.music_tw));
        sounds_TextView = ((TextView)findViewById(R.id.sounds_tw));

        joystick_TextView.setGravity(Gravity.FILL_VERTICAL | Gravity.START);
        sounds_TextView.setGravity(Gravity.FILL_VERTICAL | Gravity.START);
        music_TextView.setGravity(Gravity.FILL_VERTICAL | Gravity.START);

//        joystick_TextView.setBackgroundColor(Color.GREEN);
//        music_TextView.setBackgroundColor(Color.GREEN);
//        sounds_TextView.setBackgroundColor(Color.GREEN);

        joystick_TextView.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        joystick_TextView.setTextSize(text_size);

        music_TextView.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        music_TextView.setTextSize(text_size);

        sounds_TextView.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        sounds_TextView.setTextSize(text_size);


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
