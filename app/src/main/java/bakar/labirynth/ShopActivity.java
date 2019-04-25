package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.Xml;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ShopActivity extends Activity implements View.OnClickListener {

    LinearLayout layout;
    ArrayList<ShopItem> items = new ArrayList<>();
    Random random = new Random(6654345);
    final int layoutHeight = 250; // FIXME: 3/27/19
    TextView gold;
    Animation on_click_anim;
    Map<String, Integer> id_map;
    Drawable coinIcon;

    void setIdMap(){
        id_map = new HashMap<>();

        id_map.put(StoredProgress.levelUpgKey, R.drawable.level_up);

        id_map.put(StoredProgress.teleportAmountKey, R.drawable.teleport);
        id_map.put(StoredProgress.teleportUpgKey, R.drawable.teleport);

        id_map.put(StoredProgress.pathfinderAmountKey, R.drawable.pathfinder);
        id_map.put(StoredProgress.pathfinderUpgKey, R.drawable.pathfinder);

        id_map.put(StoredProgress.pointerAmountKey, R.drawable.pointer);
        id_map.put(StoredProgress.pointerUpgKey, R.drawable.pointer);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_shop);

        setIdMap();

        coinIcon = new BitmapDrawable(getResources(),
                BitmapFactory.decodeResource(getResources(), R.drawable.coin_anim1));
        on_click_anim = AnimationUtils.loadAnimation(this, R.anim.on_button_tap);

        layout = (LinearLayout)findViewById(R.id.ll_scroll_layout);
        ((ConstraintLayout)findViewById(R.id.cl_shop_act))
                .addView(new Background(this), 0);

        gold = (TextView)findViewById(R.id.tw_gold_amount);
        gold.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));
        updateGoldLabel();

        setItems();

        layout.addView(getSpace());
        layout.addView(getSpace());
        layout.addView(getSpace());

        for (ShopItem item : items){
            //item.resetValue();
            layout.addView(item.spacesDecorator(item.getMainLayout()));
            layout.addView(getSpace());
            item.getMainLayout().setOnClickListener(this);
        }

        //layout.addView(getSpace());

        findViewById(R.id.bt_shop_back).setOnClickListener(this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        for (ShopItem item : items){
            item.assosiatedLayout = null;
        }
        items.clear();
    }

    @Override
    public void onClick(View view){
        if (view.getId() == R.id.bt_shop_back){
            finish();
        }

        for (ShopItem item:items
             ) {
            if (item.getMainLayout().getId() == view.getId()){
                item.getMainLayout().startAnimation(on_click_anim);
                item.onTrigger();
                updateGoldLabel();
            }
        }
    }

    void updateGoldLabel(){
        if (gold != null) {
            gold.setText(String.valueOf(StoredProgress.getInstance().getGoldAmount()));
        }
    }

    private int getRandomId(){
        return random.nextInt();
    }

    void setItems(){
        items.add(new UpgrageItem(StoredProgress.getInstance().levelUpgKey));
        items.add(new BonusBuyItem(StoredProgress.getInstance().teleportAmountKey));
        items.add(new BonusBuyItem(StoredProgress.getInstance().pathfinderAmountKey));
        items.add(new BonusBuyItem(StoredProgress.getInstance().pointerAmountKey));
        items.add(new UpgrageItem(StoredProgress.getInstance().teleportUpgKey));
        items.add(new UpgrageItem(StoredProgress.getInstance().pathfinderUpgKey));
        items.add(new UpgrageItem(StoredProgress.getInstance().pointerUpgKey));
        items.add(new GoldBuyItem(3, 100));
        items.add(new GoldBuyItem(5,300));
        items.add(new GoldBuyItem(10,1000));
    }
    Space getSpace(){
        Space space = new Space(ShopActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50));
        return space;
    }

    abstract class ShopItem{
        String label;
        LinearLayout assosiatedLayout = null;
        int costIconResource = R.drawable.coin_anim1;
        ImageView costIcon = null;
        int mainIconResource = -1;
        ImageView mainIcon = null;
        float iconSizeCoef = .7f;
        TextView label_tw = null; // TODO: 3/27/19 rename
        TextView cost_tw = null;

        ShopItem(){}

        void removeGold(int g){
            StoredProgress.getInstance().setGold(StoredProgress.getInstance().getGoldAmount() - g);
        }

        void updateLabelText(){label_tw.setText(label);}
        void updateCostText(){cost_tw.setText(" " + String.valueOf(getCost()));}
        abstract int getCost();
        abstract void onTrigger();

        LinearLayout spacesDecorator(LinearLayout centerLO){
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    250,
                    1
            );

            LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
                    70,
                    50,
                    1
            );

            LinearLayout result = new LinearLayout(ShopActivity.this);

            Space space1 = new Space(ShopActivity.this);
            Space space2 = new Space(ShopActivity.this);
            space1.setLayoutParams(spaceParams);
            space2.setLayoutParams(spaceParams);

            result.setLayoutParams(layoutParams);
            result.addView(space1);
            result.addView(getMainLayout());
            result.addView(space2);

            return result;
        }
        LinearLayout getMainLayout(){
            if (assosiatedLayout != null) return assosiatedLayout;

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    layoutHeight,
                    2
            );

            assosiatedLayout = new LinearLayout(ShopActivity.this);
            assosiatedLayout.setLayoutParams(layoutParams);
            assosiatedLayout.addView(getFinalIconLayout());
            Space space = new Space(ShopActivity.this);
            space.setLayoutParams(new LinearLayout.LayoutParams(
                    50,
                    50, 1));
            assosiatedLayout.addView(space);
            assosiatedLayout.addView(getCostLayout());
            updateCostText();

            try{
                Drawable bg;
                bg = Drawable.createFromXml(getResources(), getResources().getXml(R.xml.shop_item_bg));
                XmlPullParser parser = getResources().getXml(R.xml.shop_item_bg);
                bg.inflate(getResources(), parser, Xml.asAttributeSet(parser));

                assosiatedLayout.setBackground(bg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            assosiatedLayout.setId(getRandomId()); // fix

            return assosiatedLayout;
        }

        LinearLayout getCostLayout(){
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );
            LinearLayout.LayoutParams costParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    50,  // TODO: 4/2/19 remove hardcode in whole method
                    50,
                    1
            );

            LinearLayout costLayout = new LinearLayout(ShopActivity.this);
            costLayout.setLayoutParams(layoutParams);
            costLayout.setOrientation(LinearLayout.HORIZONTAL);
            cost_tw = new TextView(ShopActivity.this);
            cost_tw.setLayoutParams(costParams);
            cost_tw.setTextSize(25);
            cost_tw.setTextColor(Color.WHITE);
            cost_tw.setGravity(Gravity.CENTER);
            cost_tw.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));
            costLayout.addView(cost_tw);


            costIcon = new ImageView(ShopActivity.this);
//            if (costIconResource != -1)
//                costIcon.setBackgroundResource(costIconResource);
            costIcon.setBackground(coinIcon);
            //costIcon.setBackgroundColor(Color.RED);
            costIcon.setId(getRandomId());
            costIcon.setLayoutParams(iconParams);
            costLayout.addView(costIcon);

            Space space1 = new Space(ShopActivity.this);
            space1.setLayoutParams(new LinearLayout.LayoutParams(
                    50,
                    100,
                    1));
            Space space2 = new Space(ShopActivity.this);
            space2.setLayoutParams(new LinearLayout.LayoutParams(
                    50,
                    100,
                    1));

            LinearLayout wrapingLo = new LinearLayout(ShopActivity.this);
            wrapingLo.setLayoutParams(layoutParams);
            wrapingLo.setOrientation(LinearLayout.VERTICAL);

            wrapingLo.addView(space1);
            wrapingLo.addView(costLayout);
            wrapingLo.addView(space2);

            return wrapingLo;
        }
        ConstraintLayout getMainIconLayout(){
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            );

            ConstraintLayout constraintLayout = new ConstraintLayout(ShopActivity.this);
            //constraintLayout.setBackgroundColor(Color.GRAY);
            constraintLayout.setLayoutParams(params);
            constraintLayout.setId(getRandomId());
            constraintLayout.setMaxHeight(layoutHeight);
            constraintLayout.setMaxWidth(layoutHeight);

            mainIcon = new ImageView(ShopActivity.this);
            if (mainIconResource != -1)
                mainIcon.setBackgroundResource(mainIconResource);
            mainIcon.setId(getRandomId());
            constraintLayout.addView(mainIcon);

            ConstraintSet set = new ConstraintSet();
            set.clone(constraintLayout);

            set.centerHorizontally(mainIcon.getId(), ConstraintSet.PARENT_ID);
            set.centerVertically(mainIcon.getId(), ConstraintSet.PARENT_ID);

            set.constrainWidth(mainIcon.getId(), Math.round(layoutHeight * iconSizeCoef));
            set.constrainHeight(mainIcon.getId(), Math.round(layoutHeight * iconSizeCoef));

            set.applyTo(constraintLayout);
            return constraintLayout;
        }
        ConstraintLayout getFinalIconLayout(){
            return getMainIconLayout();
        }
    }

    abstract class NotRealBuyItem extends ShopItem{
        String dataKey;

        void incrementValue(){
            StoredProgress.getInstance().setValue(dataKey, getValue() + 1);
        }
        int getValue(){
            return StoredProgress.getInstance().getValue(dataKey);
        }
        @Override
        void onTrigger(){
            if (StoredProgress.getInstance().getGoldAmount() >= getCost()){
                removeGold(getCost());
                incrementValue();
                updateCostText();
                updateLabelText();
                updateGoldLabel();
            }
        }
        @Override
        int getCost(){
            return Economist.getInstance().price_map.get(dataKey).apply(getValue());
        }

        ConstraintLayout addAmountToLayout(ConstraintLayout constraintLayout){
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );

            label_tw = new TextView(ShopActivity.this);
            label_tw.setLayoutParams(labelParams);
            label_tw.setTextSize(20);
            label_tw.setTextColor(Color.WHITE);
            label_tw.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));
            //amount.setBackgroundColor(Color.RED);
            label_tw.setGravity(Gravity.RIGHT);
            constraintLayout.addView(label_tw);
            label_tw.setId(getRandomId());
            updateLabelText();

            ConstraintSet set = new ConstraintSet();
            set.clone(constraintLayout);

            set.connect(label_tw.getId(), ConstraintSet.BOTTOM, mainIcon.getId(), ConstraintSet.BOTTOM,1);
            set.connect(label_tw.getId(), ConstraintSet.RIGHT, mainIcon.getId(), ConstraintSet.RIGHT,1);

            set.applyTo(constraintLayout);
            return constraintLayout;
        }
    }

    class UpgrageItem extends NotRealBuyItem{
        UpgrageItem(String _dataKey){
            dataKey = _dataKey;
            if (dataKey != null){
                mainIconResource = id_map.get(dataKey);
            }
        }
        @Override
        void updateLabelText(){
            label_tw.setText(String.valueOf(getValue() + 1));
        }

        ConstraintLayout addUpdArrowToLayout(ConstraintLayout constraintLayout){
            ImageView green_arrow = new ImageView(ShopActivity.this);
            green_arrow.setBackgroundResource(R.drawable.green_arrow);
            green_arrow.setId(getRandomId());
            constraintLayout.addView(green_arrow);

            ConstraintSet set = new ConstraintSet();
            set.clone(constraintLayout);

            set.constrainWidth(green_arrow.getId(), Math.round(layoutHeight * iconSizeCoef / 3.f));
            set.constrainHeight(green_arrow.getId(), Math.round(layoutHeight * iconSizeCoef / 3.f));

            set.connect(green_arrow.getId(), ConstraintSet.TOP, mainIcon.getId(), ConstraintSet.TOP,1);
            set.connect(green_arrow.getId(), ConstraintSet.RIGHT, mainIcon.getId(), ConstraintSet.RIGHT,1);

            set.applyTo(constraintLayout);
            return constraintLayout;
        }

        @Override
        ConstraintLayout getFinalIconLayout(){
            return addUpdArrowToLayout(addAmountToLayout(getMainIconLayout()));
        }
    }
    class GoldBuyItem extends ShopItem{
        int gold;
        int startCost;

        GoldBuyItem(int _cost, int _gold){
            mainIconResource = R.drawable.coin_anim1;

            startCost = _cost;
            gold = _gold;
        }

        @Override
        int getCost() {
            return startCost;
        }
        @Override
        void updateCostText(){
            cost_tw.setText(String.valueOf(getCost()));
        }
        @Override
        void onTrigger() {
            StoredProgress.getInstance().setGold(StoredProgress.getInstance().getGoldAmount() + gold);
        }
    }
    class BonusBuyItem extends NotRealBuyItem{
        BonusBuyItem(String _dataKey){
            dataKey = _dataKey;
            if (dataKey != null)
                mainIconResource = id_map.get(dataKey);
        }

        @Override
        void updateLabelText(){
            label_tw.setText(String.valueOf(getValue()));
        }

        @Override
        ConstraintLayout getFinalIconLayout(){
            return addAmountToLayout(getMainIconLayout());
        }
    }
}
