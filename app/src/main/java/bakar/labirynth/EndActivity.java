package bakar.labirynth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class EndActivity extends Activity implements View.OnClickListener{

    Button next;
    Button menu;
    TextView gold;
    SharedPreferences sPref;
    int startGoldAmount;
    int earnedGold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);

        next = findViewById(R.id.bt_next);
        next.setOnClickListener(this);
        menu = findViewById(R.id.bt_menu);
        menu.setOnClickListener(this);
        gold = findViewById(R.id.gold);
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        startGoldAmount = sPref.getInt("gold", 0);
        earnedGold = getIntent().getIntExtra("earned_gold", 0);
        gold.setText(String.valueOf(startGoldAmount));
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt("gold", startGoldAmount + earnedGold);
        ed.apply();

    }

    @Override
    protected void onResume(){
        super.onResume();
        new TextChanger().start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_next:
                setResult("next".hashCode());
                break;
            case R.id.bt_menu:
                setResult("menu".hashCode());
                break;
        }
        finish();
    }

    class TextChanger extends Thread{
        int animTimeMs = 3000;

        private long getTime(){
            return System.nanoTime() / 1_000_000;
        }

        @Override
        public void run(){
            final long startTime = getTime();

            while (getTime() - startTime < animTimeMs) {
                try{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gold.setText(String.valueOf(startGoldAmount + earnedGold * (getTime() - startTime) / animTimeMs));
                        }
                    });
                    Thread.sleep(30);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gold.setText(String.valueOf(startGoldAmount + earnedGold));
                }
            });
        }
    }
}
