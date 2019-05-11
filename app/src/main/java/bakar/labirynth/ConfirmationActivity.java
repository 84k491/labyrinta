package bakar.labirynth;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ConfirmationActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        ((Button)findViewById(R.id.bt_confirm_no)).setOnClickListener(this);
        ((Button)findViewById(R.id.bt_confirm_yes)).setOnClickListener(this);

        ((TextView)findViewById(R.id.tw_confirm_title)).setTypeface(
                Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_confirm_no){
            setResult("confirm_no".hashCode());
        }
        if (v.getId() == R.id.bt_confirm_yes){
            setResult("confirm_yes".hashCode());
        }
        finish();
    }
}
