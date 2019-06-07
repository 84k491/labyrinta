package bakar.labirynth;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ConfirmationActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_confirmation);

        ((Button)findViewById(R.id.bt_confirm_no)).setOnClickListener(this);
        ((Button)findViewById(R.id.bt_confirm_yes)).setOnClickListener(this);
        findViewById(R.id.confirmation_bg).setOnClickListener(this);

        ((TextView)findViewById(R.id.tw_confirm_title)).setTypeface(
                Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_confirm_no ||
            v.getId() == R.id.confirmation_bg){
            setResult("confirm_no".hashCode());
        }
        if (v.getId() == R.id.bt_confirm_yes){
            setResult("confirm_yes".hashCode());
        }
        finish();
    }
}
