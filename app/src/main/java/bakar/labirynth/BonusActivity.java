package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class BonusActivity extends Activity implements View.OnClickListener{

    Button pointer;
    Button teleport;
    Button path;
    int pointerAmount;
    int teleportAmount;
    int pathfinderAmount;
    ImageView imageView;
    TextView t_pointerAmount;
    TextView t_pathfinderAmount;
    TextView t_teleportAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_bonus);

        Intent intent = getIntent();
        pointerAmount = intent.getIntExtra("pointerAmount", 0);
        pathfinderAmount = intent.getIntExtra("pathfinderAmount", 0);
        teleportAmount = intent.getIntExtra("teleportAmount", 0);

        t_pathfinderAmount = findViewById(R.id.text_path_amount);
        t_pointerAmount = findViewById(R.id.text_pointer_amount);
        t_teleportAmount = findViewById(R.id.text_teleport_amount);
        t_pathfinderAmount.setText(String.valueOf(pathfinderAmount));
        t_teleportAmount.setText(String.valueOf(teleportAmount));
        t_pointerAmount.setText(String.valueOf(pointerAmount));

        pointer = findViewById(R.id.bt_pointer);
        pointer.setOnClickListener(this);
        teleport = findViewById(R.id.bt_teleport);
        teleport.setOnClickListener(this);
        path = findViewById(R.id.bt_path);
        path.setOnClickListener(this);
        imageView = findViewById(R.id.background_image);
        imageView.setOnClickListener(this);

        if (0 == pathfinderAmount)
            path.getBackground().setColorFilter(0xee222222, PorterDuff.Mode.SRC_ATOP);
        else
            path.getBackground().setColorFilter(null);
        if (0 == teleportAmount)
            teleport.getBackground().setColorFilter(0xee222222, PorterDuff.Mode.SRC_ATOP);
        else
            teleport.getBackground().setColorFilter(null);
        if (0 == pointerAmount)
            pointer.getBackground().setColorFilter(0xee222222, PorterDuff.Mode.SRC_ATOP);
        else
            pointer.getBackground().setColorFilter(null);
        path.invalidate();
        path.invalidate();
        pointer.invalidate();
    }

    @Override
    public void onClick(View view) {
        boolean finishFlag = false;
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("what_from", BonusActivity.class.toString());
        switch (view.getId()){
            case R.id.bt_pointer:
                if (pointerAmount > 0){
                    intent.putExtra("result", "pointer");
                    finishFlag = true;
                }
                break;
            case R.id.bt_teleport:
                if (teleportAmount > 0){
                    intent.putExtra("result", "teleport");
                    finishFlag = true;
                }
                break;
            case R.id.bt_path:
                if (pathfinderAmount > 0){
                    intent.putExtra("result", "path");
                    finishFlag = true;
                }
                break;
            case R.id.background_image:
                intent.putExtra("result", "abort_bonus");
                finishFlag = true;
                break;
        }
        if (finishFlag){
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
