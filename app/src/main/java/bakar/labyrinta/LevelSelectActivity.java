package bakar.labyrinta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.AppCompatTextView;
import android.util.Xml;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Random;

public class LevelSelectActivity extends Activity implements View.OnClickListener {

    Random random = new Random(274412536);
    LinearLayout mainLayout;
    TextView gold;
    Animation on_click_anim;
    final ArrayList<NumeratedTextView> textViews = new ArrayList<>();

    private int touched_view_id;

    Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            for (NumeratedTextView tv:textViews){
                if (tv.getId() == touched_view_id){
                    tv.startAnimation(on_click_anim);

                    // при нажатии на "0" происходит покупка следующего размера
                    if (tv.number == 0){
                        String dataKey = StoredProgress.levelUpgKey;
                        int level_value = StoredProgress.getInstance().getValue(dataKey);
                        int level_cost = Economist.getInstance().price_map.get(dataKey).apply(
                                level_value
                        );

                        if (StoredProgress.getInstance().getGoldAmount() >= level_cost){

                            Intent intent = new Intent(LevelSelectActivity.this, LevelBuyActivity.class);
                            intent.putExtra("level_number", level_value + 1);
                            intent.putExtra("level_cost", level_cost);
                            startActivityForResult(intent, 42);
                        }
                    }

                    if (tv.number > 0){
                        Intent intent = new Intent(LevelSelectActivity.this, LevelSelectActivity.class);
                        intent.putExtra("level_size", tv.number);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                    break;
                }
            }
        }

        @Override
        public void onAnimationEnd(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}
    };

    int itemSizePx = 0; // FIXME: 7/30/19 remove?

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        final Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.fontScale = 1.0f;
        applyOverrideConfiguration(configuration);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK){
            if (intent.getStringExtra("result_key").equals("positive")){
                SoundCore.inst().playSound(Sounds.correct);
                String dataKey = StoredProgress.levelUpgKey;
                int level_value = StoredProgress.getInstance().getValue(dataKey);
                int level_cost = Economist.getInstance().price_map.get(dataKey).apply(
                        level_value
                );

                StoredProgress.getInstance().
                        setGold(StoredProgress.getInstance().getGoldAmount() - level_cost);

                StoredProgress.getInstance().setValue(dataKey, level_value + 1);

                mainLayout.removeAllViews();
                fillLayout(level_value + 1);
                for (NumeratedTextView tv_loop : textViews){
                    tv_loop.setOnClickListener(this);
                }
                updateGoldLabel();
            }
        }
    }

    @Override
    public void onClick(View view){
        touched_view_id = view.getId();

        if (view.getId() == R.id.bt_select_back){
            Intent intent = new Intent(this, LevelSelectActivity.class);
            intent.putExtra("level_size", -1);
            setResult(RESULT_OK, intent);
            finish();
        }

        for (NumeratedTextView tv : textViews){
            if (tv.getId() == view.getId()){
                view.startAnimation(on_click_anim);
            }
        }
    }

    @Override
    protected void onStart(){
        SoundCore.inst().playBackgroungMusic();
        super.onStart();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        itemSizePx = size.x / (1080 / 170);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_level_select);

        mainLayout = findViewById(R.id.ll_level_select);

        findViewById(R.id.bt_select_back).setOnClickListener(this);

        gold = findViewById(R.id.tw_gold_amount_ls);
        gold.setTypeface(StoredProgress.getInstance().getTrenchFont(getAssets()));
        gold.setTextColor(Color.WHITE);
        updateGoldLabel();

        ((TextView)findViewById(R.id.tw_select_level_size)).setTextColor(Color.WHITE);
        ((TextView)findViewById(R.id.tw_select_level_size)).
                setTypeface(StoredProgress.getInstance().getTrenchFont(getAssets()));

        fillLayout(getIntent().getIntExtra("max_level_allowed", 0));
        for (NumeratedTextView tv : textViews){
            tv.setOnClickListener(this);
        }

        on_click_anim = AnimationUtils.loadAnimation(this, R.anim.on_menu_button_tap);
        on_click_anim.setAnimationListener(animationListener);
    }

    @Override
    protected void onStop(){
        SoundCore.inst().pauseBackgroundMusic();
        super.onStop();
    }

    void updateGoldLabel(){
        if (gold != null) {
            gold.setText(String.valueOf(StoredProgress.getInstance().getGoldAmount()));
        }
    }

    void fillLayout(int lvl_amount){
        final int row_volume = 4;
        int row_amount = 1 + lvl_amount / row_volume;
        int iter = 0;

        mainLayout.addView(getSpace());
        mainLayout.addView(getSpace());
        mainLayout.addView(getSpace());
        for (int i = 0; i < row_amount; ++i){
            LinearLayout hlo = generateHorLayout();
            mainLayout.addView(hlo);
            hlo.addView(getSpace());
            for (int k = 0; k < row_volume; ++k){
                if (iter < lvl_amount)
                    hlo.addView(constraintLayoutWrap(generateTV(++iter)));
                else
                    if (iter++ == lvl_amount){
//                        NumeratedTextView tw = generateTV(0);
                        ConstraintLayout tw = null;
                        tw = generateBuyingTV();
                        if (tw != null) {
                            hlo.addView(tw);
                        }
                    }
                    else{
                        hlo.addView(constraintLayoutWrap(generateTV(-1)));
                    }

                    hlo.addView(getSpace());
            }

            if (i != row_amount - 1){
                mainLayout.addView(getSpace());
            }
        }
        mainLayout.addView(getSpace());
    }

    class NumeratedTextView extends AppCompatTextView{

        int number;
        NumeratedTextView(Context c, int _number){
            super(c);
            number = _number;
            setTypeface(StoredProgress.getInstance().getTrenchFont(getAssets()));
            setGravity(Gravity.RIGHT);
        }
    }

    Space getSpace(){
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(
                50,
                50, 1));
        return space;
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
    NumeratedTextView generateTV(int num){

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                itemSizePx,
                itemSizePx,
                1
        );

        int buying_level_number = 1 + StoredProgress.getInstance().getValue(
                StoredProgress.levelUpgKey);

        if (buying_level_number > Economist.maxLevel && 0 == num){
            num = -1;
        }

        NumeratedTextView res = new NumeratedTextView(this, num);

        if (num > 0){
            if (num > 9){
                res.setText(String.valueOf(num));
            }
            else{
                res.setText("0" + num);
            }
        }

        if (0 == num){
            if (buying_level_number > 9){
                res.setText(String.valueOf(buying_level_number));
            }
            else{
                res.setText("0" + buying_level_number);
            }
        }

        res.setTextSize(35);
        res.setLayoutParams(params);
        res.setTextColor(Color.WHITE);
        res.setGravity(Gravity.CENTER);

        int resource_id = R.xml.level_select_bg;
        if (num == 0){
            resource_id = R.xml.level_select_bg_cannot_buy;
            res.setTextColor(Color.parseColor("#555555"));
            if (StoredProgress.getInstance().getGoldAmount() >= Economist.getInstance().
                    price_map.get(StoredProgress.levelUpgKey).apply(buying_level_number - 1)){
                resource_id = R.xml.level_select_bg_can_buy;
                res.setTextColor(Color.parseColor("#005500"));
            }
        }

        if (num >= 0){
            try{
                Drawable bg;
                bg = Drawable.createFromXml(getResources(), getResources().getXml(resource_id));
                XmlPullParser parser = getResources().getXml(resource_id);
                bg.inflate(getResources(), parser, Xml.asAttributeSet(parser));

                res.setBackground(bg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        textViews.add(res);
        res.setId(getRandomId());
        return res;
    }

    ConstraintLayout constraintLayoutWrap(NumeratedTextView tv){
        // этот метод помогает сохранить квадратный размер вне зависимости от разрешения

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                itemSizePx,
                itemSizePx,
                1
        );

        ConstraintLayout constraintLayout = new ConstraintLayout(LevelSelectActivity.this);
        constraintLayout.setLayoutParams(params);
        constraintLayout.setId(getRandomId());

        constraintLayout.addView(tv);

        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);

        set.centerHorizontally(tv.getId(), ConstraintSet.PARENT_ID);
        set.centerVertically(tv.getId(), ConstraintSet.PARENT_ID);

        set.constrainWidth(tv.getId(), itemSizePx);
        set.constrainHeight(tv.getId(), itemSizePx);

        set.applyTo(constraintLayout);

        return constraintLayout;
    }

    ConstraintLayout generateBuyingTV(){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                itemSizePx,
                itemSizePx,
                1
        );

        NumeratedTextView bgTextView = generateTV(0);

        if (null == bgTextView){
            return null;
        }

        ConstraintLayout constraintLayout = new ConstraintLayout(LevelSelectActivity.this);
        constraintLayout.setLayoutParams(params);
        constraintLayout.setId(getRandomId());

        LinearLayout costLabel = generateCostLabel();
        costLabel.setId(getRandomId());

        constraintLayout.addView(bgTextView);
        constraintLayout.addView(costLabel);

        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);

        set.centerHorizontally(bgTextView.getId(), ConstraintSet.PARENT_ID);
        set.centerVertically(bgTextView.getId(), ConstraintSet.PARENT_ID);

        set.constrainWidth(bgTextView.getId(), itemSizePx);
        set.constrainHeight(bgTextView.getId(), itemSizePx);

        set.centerHorizontally(costLabel.getId(), ConstraintSet.PARENT_ID);
        set.centerVertically(costLabel.getId(), ConstraintSet.PARENT_ID);

        set.constrainWidth(costLabel.getId(), itemSizePx);
        set.constrainHeight(costLabel.getId(), itemSizePx / 4);

        set.applyTo(constraintLayout);

        return constraintLayout;
    }

    LinearLayout generateHorLayout(){
        LinearLayout lo = new LinearLayout(this);
        lo.setOrientation(LinearLayout.HORIZONTAL);
        return lo;
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

        LinearLayout wraping_lo = generateHorLayout();

        LinearLayout inner_lo = generateHorLayout();

        ImageView currencyIcon = new ImageView(LevelSelectActivity.this);
        TextView costLabel = new TextView(this);
        costLabel.setTextSize(10);

        String dataKey = StoredProgress.levelUpgKey;
        int level_value = StoredProgress.getInstance().getValue(dataKey);
        int level_cost = Economist.getInstance().price_map.get(dataKey).apply(
                level_value
        );

        if (Economist.maxLevel > level_value){
            currencyIcon.setImageResource(R.drawable.coin_anim1);

            costLabel.setText(String.valueOf(level_cost));
            costLabel.setTypeface(StoredProgress.getInstance().getTrenchFont(getAssets()));
            costLabel.setTextColor(Color.WHITE);
            costLabel.setGravity(Gravity.FILL_VERTICAL);
        }


        inner_lo.addView(currencyIcon, iconParams);
        inner_lo.addView(costLabel, labelParams);

        Space[] spaces = {new Space(this), new Space(this)};
        wraping_lo.addView(spaces[0], spaceParams);
        wraping_lo.addView(inner_lo, loParams);
        wraping_lo.addView(spaces[1], spaceParams);

        return wraping_lo;
    }
}
