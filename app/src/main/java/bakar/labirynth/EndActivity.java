package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Random;

enum EndMenuItems{
    goldTotal,
    levelFinished,
    coinsPicked
}

public class EndActivity extends Activity implements View.OnClickListener{

    Button next;
    Button menu;
    ConstraintLayout next_level_buy;

    LinearLayout mainLayout;
    int startGoldAmount;
    int goldEarnedByCoins;
    int goldEarnedByLevel;

    boolean loadMaxLevelOnResult = false;

    Random random = new Random(24931875);

    int itemSizePx = 0;

    final HashMap<EndMenuItems, Integer> imageMap = new HashMap<>();
    final HashMap<EndMenuItems, TextView> textViewMap = new HashMap<>();

    void setMap(){
        imageMap.put(EndMenuItems.levelFinished, R.drawable.exit_icon_glow);
        imageMap.put(EndMenuItems.coinsPicked, R.drawable.coin_anim1);
        imageMap.put(EndMenuItems.goldTotal, R.drawable.gold_pile);
    }

    void startConfirmationActivity(){
        String dataKey = StoredProgress.levelUpgKey;
        int level_value = StoredProgress.getInstance().getValue(dataKey);
        int level_cost = Economist.getInstance().price_map.get(dataKey).apply(
                level_value
        );

        Intent intent = new Intent(this, LevelBuyActivity.class);
        intent.putExtra("level_number", level_value + 1);
        intent.putExtra("level_cost", level_cost);
        startActivityForResult(intent, 42);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK){
            if (intent.getStringExtra("result_key").equals("positive")){
                loadMaxLevelOnResult = true;
                SoundCore.inst().playSound(Sounds.correct);
                String dataKey = StoredProgress.levelUpgKey;
                int level_value = StoredProgress.getInstance().getValue(dataKey);
                int level_cost = Economist.getInstance().price_map.get(dataKey).apply(
                        level_value
                );

                StoredProgress.getInstance().
                        setGold(StoredProgress.getInstance().getGoldAmount() - level_cost);

                StoredProgress.getInstance().setValue(dataKey, level_value + 1);

                next_level_buy.removeAllViews();
                makeNextLevelSizeButton();

                textViewMap.get(EndMenuItems.goldTotal).setText(
                        String.valueOf(StoredProgress.getInstance().getGoldAmount())
                );
            }
        }
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(EndActivity.this,
                GameActivity.class);
        intent.putExtra("what_from", EndActivity.class.toString());
        setResult(RESULT_OK, intent);
        intent.putExtra("result", "menu");
        finish();
    }

    private int getRandomId(){
        int id = random.nextInt();
        if (findViewById(id) != null){
            return getRandomId();
        }
        else{
            return id;
        }
    }

    LinearLayout generateCostLabel(){
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                3
        );
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                30,
                LinearLayout.LayoutParams.MATCH_PARENT,
                5
        );
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
        );
        LinearLayout.LayoutParams loParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );

        LinearLayout wraping_lo = new LinearLayout(this);
        wraping_lo.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout inner_lo = new LinearLayout(this);
        inner_lo.setOrientation(LinearLayout.HORIZONTAL);

        ImageView currencyIcon = new ImageView(this);
        currencyIcon.setImageResource(R.drawable.coin_anim1);

        TextView costLabel = new TextView(this);

        String dataKey = StoredProgress.levelUpgKey;
        int level_value = StoredProgress.getInstance().getValue(dataKey);
        int level_cost = Economist.getInstance().price_map.get(dataKey).apply(
                level_value
        );
        costLabel.setText(String.valueOf(level_cost));
        costLabel.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));
        costLabel.setTextColor(Color.WHITE);
        costLabel.setGravity(Gravity.FILL_VERTICAL);

        inner_lo.addView(currencyIcon, iconParams);
        inner_lo.addView(costLabel, labelParams);

        Space[] spaces = {new Space(this), new Space(this)};
        wraping_lo.addView(spaces[0], spaceParams);
        wraping_lo.addView(inner_lo, loParams);
        wraping_lo.addView(spaces[1], spaceParams);

        return wraping_lo;
    }

    public int convertDpToPixels(float dp) {
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, this.getResources().getDisplayMetrics());
        return px;
    }

    void makeNextLevelSizeButton(){
        int buying_level_number = 1 + StoredProgress.getInstance().getValue(
                StoredProgress.levelUpgKey);

        if (buying_level_number > Economist.maxLevel){
            return;
        }

        //Это хардкод из лейаута
        // TODO: 6/15/19 remove hardcode
        final int max_size = convertDpToPixels(60);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                max_size,
                max_size,
                1
        );

        TextView bgTextView = new TextView(this);

        bgTextView.setText(String.valueOf(buying_level_number));
        bgTextView.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );
        bgTextView.setTextSize(40);
        bgTextView.setLayoutParams(params);
        bgTextView.setGravity(Gravity.CENTER);

        int resource_id = R.xml.level_select_bg_cannot_buy;
        bgTextView.setTextColor(Color.parseColor("#999999"));

        int cost = Economist.getInstance().
                price_map.get(StoredProgress.levelUpgKey).apply(buying_level_number - 1);
        int stored_gold = StoredProgress.getInstance().getGoldAmount() +
                goldEarnedByCoins + goldEarnedByLevel;

        if (stored_gold >= cost){
            resource_id = R.xml.level_select_bg_can_buy;
            bgTextView.setTextColor(Color.parseColor("#009900"));
        }

        try{
            Drawable bg;
            bg = Drawable.createFromXml(getResources(), getResources().getXml(resource_id));
            XmlPullParser parser = getResources().getXml(resource_id);
            bg.inflate(getResources(), parser, Xml.asAttributeSet(parser));
            bgTextView.setBackground(bg);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ConstraintLayout constraintLayout = findViewById(R.id.cl_end_level_next);
        constraintLayout.setLayoutParams(params);
        //constraintLayout.setId(getRandomId());

        LinearLayout costLabel = generateCostLabel();

        costLabel.setId(getRandomId());

        constraintLayout.addView(bgTextView);
        constraintLayout.addView(costLabel);

        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);

        set.centerHorizontally(bgTextView.getId(), ConstraintSet.PARENT_ID);
        set.centerVertically(bgTextView.getId(), ConstraintSet.PARENT_ID);

        set.constrainWidth(bgTextView.getId(), max_size);
        set.constrainHeight(bgTextView.getId(), max_size);

        set.centerHorizontally(costLabel.getId(), ConstraintSet.PARENT_ID);
        set.centerVertically(costLabel.getId(), ConstraintSet.PARENT_ID);

        set.constrainWidth(costLabel.getId(), max_size);
        set.constrainHeight(costLabel.getId(), max_size / 4);

        set.applyTo(constraintLayout);

        //return constraintLayout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_end);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        itemSizePx = size.x / (1080 / 170);

        setMap();
        startGoldAmount = StoredProgress.getInstance().getGoldAmount();
        goldEarnedByCoins = getIntent().getIntExtra("goldEarnedByCoins", 0);
        goldEarnedByLevel = getIntent().getIntExtra("goldEarnedByLevel", 0);

        next = findViewById(R.id.bt_next);
        next.setOnClickListener(this);
        menu = findViewById(R.id.bt_menu);
        menu.setOnClickListener(this);

        next_level_buy = findViewById(R.id.cl_end_level_next);
        makeNextLevelSizeButton();
        next_level_buy.setOnClickListener(this);

        ((TextView)findViewById(R.id.tw_nice)).setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );

        StoredProgress.getInstance().setGold(startGoldAmount + goldEarnedByCoins + goldEarnedByLevel);

        mainLayout = findViewById(R.id.end_layout);

        mainLayout.addView(getSpace(), 3);

        mainLayout.addView(getSpace(), 2);
        mainLayout.addView(getChangingTextWithIcon(EndMenuItems.goldTotal), 2);
        mainLayout.addView(getSpace(), 2);
        mainLayout.addView(getChangingTextWithIcon(EndMenuItems.levelFinished), 2);
        if (goldEarnedByCoins != 0){
            mainLayout.addView(getSpace(), 2);
            mainLayout.addView(getChangingTextWithIcon(EndMenuItems.coinsPicked), 2);
        }
        mainLayout.addView(getSpace(), 2);
        mainLayout.addView(getSpace(), 2);

//        try{
//            Drawable bg;
//            bg = Drawable.createFromXml(getResources(), getResources().getXml(R.xml.level_select_bg));
//            XmlPullParser parser = getResources().getXml(R.xml.level_select_bg);
//            bg.inflate(getResources(), parser, Xml.asAttributeSet(parser));
//
//            findViewById(R.id.bt_menu).setBackground(bg);
//            findViewById(R.id.bt_shop).setBackground(bg);
//            findViewById(R.id.bt_next).setBackground(bg);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        ((Button)findViewById(R.id.bt_menu)).setBackgroundResource(R.drawable.menu_button);
//        ((Button)findViewById(R.id.bt_shop)).setBackgroundResource(R.drawable.bag);
//        ((Button)findViewById(R.id.bt_next)).setBackgroundResource(R.drawable.play);

        new TextChanger().start();
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
        someText.setTypeface(
                Typeface.createFromAsset(getAssets(),  "fonts/trench100free.ttf")
        );

        if (EndMenuItems.goldTotal == item){
            someText.setTextSize(20.f * size_coef);
            someText.setTextColor(Color.rgb(170, 200, 20));
        }
        else{
            someText.setTextColor(Color.rgb(50, 200, 70));
            someText.setTextSize(20.f);
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
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_next:
                Intent intent = new Intent(EndActivity.this,
                        GameActivity.class);
                intent.putExtra("what_from", EndActivity.class.toString());

                if (loadMaxLevelOnResult){
                    intent.putExtra("result", "load_max_level");
                }
                else{
                    intent.putExtra("result", "next");
                }

                setResult(RESULT_OK, intent);

                finish();
                break;
            case R.id.bt_menu:
                Intent intent1 = new Intent(EndActivity.this,
                        GameActivity.class);
                intent1.putExtra("what_from", EndActivity.class.toString());
                intent1.putExtra("result", "menu");
                setResult(RESULT_OK, intent1);
                finish();
                break;
            case R.id.cl_end_level_next:
                int buying_level_number = 1 + StoredProgress.getInstance().getValue(
                        StoredProgress.levelUpgKey);

                if (buying_level_number <= Economist.maxLevel){
                    startConfirmationActivity();
                }
                break;
        }
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
