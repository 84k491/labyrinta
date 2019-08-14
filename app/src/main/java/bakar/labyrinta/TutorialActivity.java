package bakar.labyrinta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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
    NextLevelBuyTutorial,
    BonusRangeTutorial
}

public class TutorialActivity extends Activity {

    LinearLayout tutorial_lo;

    Map<TutorialKey, String> text_map = new HashMap<>();
    Map<TutorialKey, Integer> image_map = new HashMap<>();

    boolean isDetailedTutorial(TutorialKey key){
        return (key == TutorialKey.PathfinderTutorial ||
                key == TutorialKey.TeleportTutorial ||
                key == TutorialKey.PointerTutorial ||
                key == TutorialKey.BonusRangeTutorial);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        final Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.fontScale = 1.0f;
        applyOverrideConfiguration(configuration);
    }

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
                intent.putExtra("what_from", TutorialKey.class.toString());

                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    void setMaps(){
        text_map.put(TutorialKey.BeginTutorial_1,
                getString(R.string.begin_tutorial_1)
                );
        text_map.put(TutorialKey.BeginTutorial_2,
                getString(R.string.begin_tutorial_2)
        );
        text_map.put(TutorialKey.BeginTutorial_3,
                getString(R.string.begin_tutorial_3)
        );
        text_map.put(TutorialKey.PathfinderTutorial,
                getString(R.string.pathfinder_tutorial)
        );
        text_map.put(TutorialKey.PointerTutorial,
                getString(R.string.pointer_tutorial)
        );
        text_map.put(TutorialKey.TeleportTutorial,
                getString(R.string.teleport_tutorial)
        );
        text_map.put(TutorialKey.NextLevelBuyTutorial,
                getString(R.string.next_level_buy_tutorial)
        );
        text_map.put(TutorialKey.BonusRangeTutorial,
                getString(R.string.bonus_range_tutorial)
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
                R.drawable.tutorial_expand
        );image_map.put(TutorialKey.BonusRangeTutorial,
                R.drawable.tutorial_range
        );

    }

    void setTutorial(TutorialKey key){

        LinearLayout.LayoutParams space_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                40
        );

        LinearLayout.LayoutParams image_params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                isDetailedTutorial(key) ? 400 : 200,
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

        Space[] spaces_vert = {new Space(this), new Space(this)};
        LinearLayout.LayoutParams space_params_vert = new LinearLayout.LayoutParams(
                30,
                30,
                4
        );

        tutorial_lo.addView(spaces_vert[0], 0, space_params_vert);
        tutorial_lo.addView(image_lo, 0);
        tutorial_lo.addView(spaces_vert[1], 0, space_params_vert);
        tutorial_lo.addView(textView, 0);
    }
}