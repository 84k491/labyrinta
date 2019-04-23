package bakar.labirynth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

enum EndMenuItems{
    goldTotal,
    levelFinished,
    coinsPicked
}

public class EndActivity extends Activity implements View.OnClickListener{

    Button next;
    Button menu;
    LinearLayout mainLayout;
    SharedPreferences sPref;
    int startGoldAmount;
    int goldEarnedByCoins;
    int goldEarnedByLevel;

    final HashMap<EndMenuItems, Integer> imageMap = new HashMap<>();
    final HashMap<EndMenuItems, TextView> textViewMap = new HashMap<>();

    void setMap(){
        imageMap.put(EndMenuItems.levelFinished, R.drawable.vortex_frame_00);
        imageMap.put(EndMenuItems.coinsPicked, R.drawable.coin_anim1);
        imageMap.put(EndMenuItems.goldTotal, R.drawable.coin_anim1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);

        setMap();

        next = findViewById(R.id.bt_next);
        next.setOnClickListener(this);
        menu = findViewById(R.id.bt_menu);
        menu.setOnClickListener(this);
        //gold = findViewById(R.id.gold);
        sPref = getSharedPreferences("global", MODE_PRIVATE);
        startGoldAmount = sPref.getInt("gold", 0);
        goldEarnedByCoins = getIntent().getIntExtra("goldEarnedByCoins", 0);
        goldEarnedByLevel = getIntent().getIntExtra("goldEarnedByLevel", 0);

        //gold.setText(String.valueOf(startGoldAmount));
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt("gold", startGoldAmount + goldEarnedByCoins + goldEarnedByLevel);
        ed.apply();

        mainLayout = findViewById(R.id.end_layout);

        mainLayout.addView(getSpace(), 1);
        mainLayout.addView(getChangingTextWithIcon(EndMenuItems.goldTotal), 1);
        mainLayout.addView(getSpace(), 1);
        mainLayout.addView(getChangingTextWithIcon(EndMenuItems.levelFinished), 1);
        if (goldEarnedByCoins != 0){
            mainLayout.addView(getSpace(), 1);
            mainLayout.addView(getChangingTextWithIcon(EndMenuItems.coinsPicked), 1);
        }
        mainLayout.addView(getSpace(), 1);
        mainLayout.addView(getSpace(), 1);
    }

    LinearLayout getChangingTextWithIcon(EndMenuItems item){
        float size_coef = 1.f;
        if (EndMenuItems.goldTotal == item){
            size_coef = 1.5f;
        }

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                Math.round(50 * size_coef),
                Math.round(50 * size_coef),
                1
        );

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );

        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
                100,
                50,
                2
        );

        LinearLayout resultLo = new LinearLayout(this);
        resultLo.setOrientation(LinearLayout.HORIZONTAL);

        Space spaces[] = {new Space(this), new Space(this)};

        ImageView imageView = new ImageView(this);
        imageView.setImageResource(imageMap.get(item));
        //imageView.setBackgroundColor(Color.RED);

        TextView someText = new TextView(this);
        someText.setText(String.valueOf(item));

        if (EndMenuItems.goldTotal == item){
            someText.setTextSize(15.f * size_coef);
            someText.setTextColor(Color.rgb(170, 200, 20));
        }
        else{
            someText.setTextColor(Color.rgb(50, 200, 70));
            someText.setTextSize(15.f);
        }
        //someText.setBackgroundColor(Color.BLUE);
        textViewMap.put(item, someText);

        resultLo.addView(spaces[0], spaceParams);
        resultLo.addView(imageView, imageParams);
        resultLo.addView(someText, textParams);
        resultLo.addView(spaces[1], spaceParams);

        return resultLo;
    }
    Space getSpace(){
        Space space = new Space(this);
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
                100,
                10,
                2
        );
        space.setLayoutParams(spaceParams);
        return space;
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
        final int animTimeMs = 1000;

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
                            textViewMap.get(EndMenuItems.levelFinished).
                                    setText('+' + String.valueOf(
                                            goldEarnedByLevel
                                                    * (getTime() - startTime) / animTimeMs));
                            if (goldEarnedByCoins != 0){
                                textViewMap.get(EndMenuItems.coinsPicked).
                                        setText('+' + String.valueOf(
                                                goldEarnedByCoins
                                                        * (getTime() - startTime) / animTimeMs));
                            }
                            textViewMap.get(EndMenuItems.goldTotal).
                                    setText(String.valueOf(
                                            startGoldAmount + (goldEarnedByCoins + goldEarnedByLevel)
                                                    * (getTime() - startTime) / animTimeMs));
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
                    textViewMap.get(EndMenuItems.levelFinished).
                            setText('+' + String.valueOf(goldEarnedByLevel));
                    if (goldEarnedByCoins != 0){
                        textViewMap.get(EndMenuItems.coinsPicked).
                                setText('+' + String.valueOf(goldEarnedByCoins));
                    }
                    textViewMap.get(EndMenuItems.goldTotal).
                            setText(String.valueOf(startGoldAmount +
                                    goldEarnedByCoins +
                                    goldEarnedByLevel));
                }
            });
        }
    }
}
