package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

enum TutorialKey{
    BeginTutorial_1,
    BeginTutorial_2,
    BeginTutorial_3,
    PathfinderTutorial,
    TeleportTutorial,
    PointerTutorial,
    NextLevelBuyTutorial
}

public class TutorialActivity extends Activity {

    LinearLayout tutorial_lo;

    Map<TutorialKey, String> text_map = new HashMap<>();
    Map<TutorialKey, Integer> image_map = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_tutorial);
        setMaps();

        tutorial_lo = findViewById(R.id.lo_tutorial);

        TutorialKey passedKey = TutorialKey.valueOf(
                getIntent().getStringExtra(TutorialKey.class.toString()));

        setTutorial(passedKey);

        findViewById(R.id.bt_tutorial_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TutorialActivity.this,
                        TutorialActivity.class);

                intent.putExtra(TutorialKey.class.toString(), String.valueOf(passedKey));

                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    void setMaps(){
        text_map.put(TutorialKey.BeginTutorial_1,
                "Hello. The goal for each level is to reach an exit"
                );
        text_map.put(TutorialKey.BeginTutorial_2,
                "Tilt your device to move around. \n" +
                "You can change controls at settings menu at any time."
        );
        text_map.put(TutorialKey.BeginTutorial_3,
                "You will earn gold for finishing levels and picking up coins. " +
                "Gold can be spent in shop, buying bonuses and unlocking new level sizes"
        );
        text_map.put(TutorialKey.PathfinderTutorial,
                "You picked up a Pathfinder bonus!\n" +
                "Use it to highlight a path to any point within it's range.\n" +
                "You can upgrade it, or buy more in the shop."
        );
        text_map.put(TutorialKey.PointerTutorial,
                "You picked up a Pointer bonus!\n" +
                "It points to exit when used.\n" +
                "You can buy more in the shop."
        );
        text_map.put(TutorialKey.TeleportTutorial,
                "You picked up a Teleport bonus!\n" +
                "Use it to teleport yourself to any point within it's range.\n" +
                "You can upgrade it, or buy more in the shop."
        );
        text_map.put(TutorialKey.NextLevelBuyTutorial,
                "It seems you have enough gold to unlock next level size!\n" +
                "You can unlock it at shop, or at level menu."
        );

        image_map.put(TutorialKey.BeginTutorial_1,
                R.drawable.tutorial_exit
        );
        image_map.put(TutorialKey.BeginTutorial_2,
                R.drawable.phone_tilt
        );
        image_map.put(TutorialKey.BeginTutorial_3,
                R.drawable.gold_pile
        );
        image_map.put(TutorialKey.PathfinderTutorial,
                R.drawable.tutorial_pathfinder
        );
        image_map.put(TutorialKey.PointerTutorial,
                R.drawable.tutorial_pointer
        );
        image_map.put(TutorialKey.TeleportTutorial,
                R.drawable.tutorial_teleport
        );
        image_map.put(TutorialKey.NextLevelBuyTutorial,
                R.drawable.expand
        );

    }

    void setTutorial(TutorialKey key){
        LinearLayout.LayoutParams lo_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
        );

        LinearLayout.LayoutParams space_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                10
        );

        LinearLayout.LayoutParams image_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                200,
                1
        );

        TextView textView = new TextView(this);
        textView.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/JosefinSans_Regular.ttf")
        );
        textView.setTextSize(20);

        ImageView imageView = new ImageView(this);

        textView.setText(text_map.get(key));
        if (image_map.get(key) != null) {
            imageView.setImageResource(image_map.get(key));
        }

        Space[] spaces = {new Space(this), new Space(this)};
        LinearLayout image_lo = new LinearLayout(this);
        image_lo.setOrientation(LinearLayout.HORIZONTAL);
        image_lo.addView(spaces[0], space_params);
        image_lo.addView(imageView, image_params);
        image_lo.addView(spaces[1], space_params);

        tutorial_lo.addView(image_lo, 0);
        tutorial_lo.addView(textView, 0);
    }
}