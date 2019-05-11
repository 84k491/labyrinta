package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LevelBuyActivity extends Activity implements View.OnClickListener {

    int level_number = 0;
    int level_cost = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_buy);

        level_number = getIntent().getIntExtra("level_number", 2);
        level_cost = getIntent().getIntExtra("level_cost", 0);

        ((Button)findViewById(R.id.bt_level_buy_no)).setOnClickListener(this);
        ((Button)findViewById(R.id.bt_level_buy_yes)).setOnClickListener(this);

        ((TextView)findViewById(R.id.tw_level_buy_title)).setText(
                "Are you sure you want to buy level size " + level_number + "?"
        );

        ((TextView)findViewById(R.id.tw_level_buy_title)).setTypeface(
                Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));

        ((TextView)findViewById(R.id.tw_level_cost)).setText(String.valueOf(level_cost));
        ((TextView)findViewById(R.id.tw_level_cost)).setTypeface(
                Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));

    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, LevelBuyActivity.class);
        String result_key = "result_key";
        if (v.getId() == R.id.bt_level_buy_no){
            intent.putExtra(result_key, "negative");
        }
        if (v.getId() == R.id.bt_level_buy_yes){
            intent.putExtra(result_key, "positive");
        }
        setResult(RESULT_OK, intent);
        finish();
    }
}
