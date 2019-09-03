package bakar.labyrinta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class LevelBuyActivity extends Activity implements View.OnClickListener {

    private int level_number = 0;
    private int level_cost = 0;

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
        // FIXME: 9/3/19 error inflating xml
        setContentView(R.layout.activity_level_buy);

        level_number = getIntent().getIntExtra("level_number", 2);
        level_cost = getIntent().getIntExtra("level_cost", 0);

        findViewById(R.id.bt_level_buy_no).setOnClickListener(this);
        findViewById(R.id.bt_level_buy_yes).setOnClickListener(this);

        ((TextView)findViewById(R.id.tw_level_buy_title)).setText(
                getString(R.string.level_buy_confirmation, level_number)
        );

        ((TextView)findViewById(R.id.tw_level_buy_title)).setTypeface(
                StoredProgress.getInstance().getTrenchFont(getAssets()));

        ((TextView)findViewById(R.id.tw_level_cost)).setText(String.valueOf(level_cost));
        ((TextView)findViewById(R.id.tw_level_cost)).setTypeface(
                StoredProgress.getInstance().getTrenchFont(getAssets()));

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
