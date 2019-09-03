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

public class ConfirmationActivity extends Activity implements View.OnClickListener{

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
        setContentView(R.layout.activity_confirmation);

        findViewById(R.id.bt_confirm_no).setOnClickListener(this);
        findViewById(R.id.bt_confirm_yes).setOnClickListener(this);
        findViewById(R.id.confirmation_bg).setOnClickListener(this);

        ((TextView)findViewById(R.id.tw_confirm_title)).setTypeface(
                StoredProgress.getInstance().getTrenchFont(getAssets()));
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("what_from", ConfirmationActivity.class.toString());
        if (v.getId() == R.id.bt_confirm_no ||
            v.getId() == R.id.confirmation_bg){
            intent.putExtra("result", "confirm_no");
        }
        if (v.getId() == R.id.bt_confirm_yes){
            intent.putExtra("result", "confirm_yes");
        }
        setResult(RESULT_OK, intent);
        finish();
    }
}
