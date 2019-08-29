package bakar.labyrinta;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ResearchActivity extends Activity implements View.OnClickListener {

    Button start;
    TextView settings;
    TextView results;
    SharedPreferences prefs;
    EditText iter;
    float previous_dist = 0;

    int xSize, ySize;

    GameLogic gameLogic;

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.res_start_bt:
                startResearch();
                break;
            default: break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_research);

        start = findViewById(R.id.res_start_bt);
        start.setOnClickListener(this);
        settings = findViewById(R.id.settings_tv);
        results = findViewById(R.id.results_tv);
        iter = findViewById(R.id.iter_et);

        loadData();
    }

    void loadData() {
        prefs = getSharedPreferences("global", MODE_PRIVATE);

        xSize = prefs.getInt("xsize", 42);
        ySize = prefs.getInt("ysize", 42);
    }

    void startResearch(){
        float dist = 0;
        int iterations_amount = Integer.parseInt(iter.getText().toString());
        long start_time = System.currentTimeMillis();
        gameLogic = new GameLogic(null, start_time, xSize, ySize, 5);

        for (int i = 0; i < iterations_amount; ++i){
            dist += gameLogic.getPathLength(
                    gameLogic.getPath(gameLogic.playerCoords(), gameLogic.exitCoords())
            );
            gameLogic.seed = start_time + 1 + i;
            gameLogic.finded_path = null;
            gameLogic.isInited = false;
            gameLogic.init(gameLogic.field.getxSize(), gameLogic.field.getySize());
        }
        long calc_time = System.currentTimeMillis() - start_time;
        float calc_time_s = (float)calc_time / 1000.f;

        dist = dist / iterations_amount;

        dist = dist / gameLogic.cellSize;

        results.setText("Average distance: " + dist + "\n" +
                        "Previous value = " + previous_dist);
        settings.setText("Xsize = " + xSize + "\n" +
                         "Ysize = " + ySize + "\n" +
                         "Iterations = " + iterations_amount + "\n" +
                         "Calc time = " + calc_time_s + "s");
        previous_dist = dist;
    }
}
