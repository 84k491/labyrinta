package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

enum TutorialKey{
    BeginTutorial_1,
    BeginTutorial_2,
    BeginTutorial_3,
    PathfinderTutorial,
    TeleportTutorial,
    PointerTutorial
}

public class TutorialActivity extends Activity {

    LinearLayout tutorial_lo;

    Map<TutorialKey, String> text_map = new HashMap<>();
    Map<TutorialKey, Integer> image_map = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                "TutorialKey.BeginTutorial_1"
                );
        text_map.put(TutorialKey.BeginTutorial_2,
                "TutorialKey.BeginTutorial_2"
        );
        text_map.put(TutorialKey.BeginTutorial_3,
                "TutorialKey.BeginTutorial_3"
        );
        text_map.put(TutorialKey.PathfinderTutorial,
                "TutorialKey.PathfinderTutorial"
        );
        text_map.put(TutorialKey.PointerTutorial,
                "TutorialKey.PointerTutorial"
        );
        text_map.put(TutorialKey.TeleportTutorial,
                "TutorialKey.TeleportTutorial"
        );

        image_map.put(TutorialKey.BeginTutorial_1,
                R.drawable.floor
        );
        image_map.put(TutorialKey.BeginTutorial_2,
                R.drawable.floor
        );
        image_map.put(TutorialKey.BeginTutorial_3,
                R.drawable.floor
        );
        image_map.put(TutorialKey.PathfinderTutorial,
                R.drawable.pathfinder
        );
        image_map.put(TutorialKey.PointerTutorial,
                R.drawable.pointer
        );
        image_map.put(TutorialKey.TeleportTutorial,
                R.drawable.teleport
        );

    }

    void setTutorial(TutorialKey key){
        TextView textView = new TextView(this);
        textView.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        textView.setTextSize(20);

        ImageView imageView = new ImageView(this);

        textView.setText(text_map.get(key));
        if (image_map.get(key) != null) {
            imageView.setImageResource(image_map.get(key));
        }

        tutorial_lo.addView(imageView, 0);
        tutorial_lo.addView(textView, 0);
    }
}