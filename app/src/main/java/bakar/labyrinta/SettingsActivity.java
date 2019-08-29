package bakar.labyrinta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
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

public class SettingsActivity extends Activity implements View.OnClickListener {

    Button joystick;
    Button sounds;
    Button music;

    TextView joystick_TextView;
    TextView sounds_TextView;
    TextView music_TextView;
    ConstraintLayout layout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        final Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.fontScale = 1.0f;
        applyOverrideConfiguration(configuration);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.joystick_bt:
                setState(joystick, StoredProgress.getInstance().switchUsesJoystick());
                break;
            case R.id.sounds_bt:
                SoundCore.inst().doPlaySounds = !SoundCore.inst().doPlaySounds;
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
                    if (SoundCore.inst().currenPlayerIsGame){
                        SoundCore.inst().playGameBackgroundMusic();
                    }
                    else {
                        SoundCore.inst().playMenuBackgroundMusicForced();
                    }
                }
                else{
                    if (SoundCore.inst().currenPlayerIsGame){
                        SoundCore.inst().pauseGameBackgroundMusic();
                    }
                    else{
                        SoundCore.inst().pauseMenuBackgroundMusicForced();
                    }
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
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

        float button_size = 20.f;

        joystick.setTextSize(button_size);
        sounds.setTextSize(button_size);
        music.setTextSize(button_size);

        joystick.setTextColor(Color.WHITE);
        sounds.setTextColor(Color.WHITE);
        music.setTextColor(Color.WHITE);

        joystick.setTypeface(
                StoredProgress.getInstance().getTrenchFont(getAssets())
        );
        sounds.setTypeface(
                StoredProgress.getInstance().getTrenchFont(getAssets())
        );
        music.setTypeface(
                StoredProgress.getInstance().getTrenchFont(getAssets())
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
                StoredProgress.getInstance().getTrenchFont(getAssets())
        );
        joystick_TextView.setTextSize(text_size);

        music_TextView.setTypeface(
                StoredProgress.getInstance().getTrenchFont(getAssets())
        );
        music_TextView.setTextSize(text_size);

        sounds_TextView.setTypeface(
                StoredProgress.getInstance().getTrenchFont(getAssets())
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
                    Intent intent = new Intent(SettingsActivity.this, GameActivity.class);
                    intent.putExtra("what_from", SettingsActivity.class.toString());
                    intent.putExtra("result", "settings_finished");
                    setResult(RESULT_OK, intent);
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
                getString(R.string.on)
                :
                getString(R.string.on);
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
