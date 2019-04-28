package bakar.labirynth;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ConfirmationActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        ((Button)findViewById(R.id.bt_confirm_no)).setOnClickListener(this);
        ((Button)findViewById(R.id.bt_confirm_yes)).setOnClickListener(this);
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
