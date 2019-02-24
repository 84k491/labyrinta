package bakar.labirynth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

public class ShopActivity extends Activity implements View.OnClickListener {

    LinearLayout layout;
    ArrayList<ShopItem> items = new ArrayList<>();
    Random random = new Random(6654345);
    SharedPreferences sharedPreferences;
    TextView gold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_shop);

        //setGoldAmount(10);

        layout = findViewById(R.id.ll_scroll_layout);
        gold = findViewById(R.id.tw_gold_amount);
        updateGoldLabel();

        setItems();

        for (ShopItem item : items){
            //item.resetValue();
            layout.addView(item.getLayout());
            layout.addView(getSpace());
            item.getLayout().setOnClickListener(this);
        }
    }
    void setGoldAmount(int ga){
        sharedPreferences = getSharedPreferences("global", MODE_PRIVATE);
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putInt("gold", ga);
        ed.commit();
    }

    @Override
    public void onClick(View view){
        for (ShopItem item:items
             ) {
            if (item.getLayout().getId() == view.getId()){
                item.onTrigger();
                updateGoldLabel();
            }

        }
    }

    void updateGoldLabel(){
        if (gold != null) {
            gold.setText(String.valueOf(getGoldBalance()));
        }
    }

    private int getRandomId(){
        return random.nextInt();
    }
    int getGoldBalance(){
        sharedPreferences = getSharedPreferences("global", MODE_PRIVATE);
        return sharedPreferences.getInt("gold", 0);
    }

    void setItems(){
        items.add(new UpgrageItem("Level size", "next_level", 355));
        items.add(new BonusBuyItem("Teleport", "teleportAmount", 50));
        items.add(new BonusBuyItem("Pathfinder", "pathfinderAmount", 30));
        items.add(new BonusBuyItem("Pointer", "pointerAmount", 20));
        items.add(new UpgrageItem("Teleport upgrade", "tp_upg", 450));
        items.add(new UpgrageItem("Pathfinder upgrade","pf_upg", 220));
        items.add(new UpgrageItem("Pointer upgrade", "pt_upg", 320));
        items.add(new GoldBuyItem("More credits (100)",3, 100));
        items.add(new GoldBuyItem("More credits (300)", 5,300));
        items.add(new GoldBuyItem("More credits (1000)", 10,1000));
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
        int startCost;
        LinearLayout assosiatedLayout = null;
        TextView label_tw = null;
        TextView cost_tw = null;

        ShopItem(){}

        void removeGold(int g){
            sharedPreferences = getSharedPreferences("global", MODE_PRIVATE);
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putInt("gold", getGoldBalance() - getCost());
            ed.commit();
        }
        void resetValue(){}

        void updateLabelText(){label_tw.setText(label);}
        void updateCostText(){cost_tw.setText(String.valueOf(getCost()) + " Cr");}
        int getCost(){return  startCost;}
        abstract void onTrigger();

        LinearLayout getLayout(){
            if (assosiatedLayout != null) return assosiatedLayout;

            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
            );
            LinearLayout.LayoutParams costParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            );
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    250,
                    1
            );

            label_tw = new TextView(ShopActivity.this);
            label_tw.setLayoutParams(labelParams);
            label_tw.setTextSize(20);
            label_tw.setTextColor(Color.WHITE);
            label_tw.setGravity(Gravity.CENTER);
            updateLabelText();

            cost_tw = new TextView(ShopActivity.this);
            cost_tw.setLayoutParams(costParams);
            cost_tw.setTextSize(25);
            cost_tw.setTextColor(Color.WHITE);
            cost_tw.setGravity(Gravity.CENTER);
            updateCostText();

            assosiatedLayout = new LinearLayout(ShopActivity.this);
            assosiatedLayout.setLayoutParams(layoutParams);
            assosiatedLayout.addView(label_tw);
            assosiatedLayout.addView(cost_tw);

            try{
                Drawable bg;
                bg = Drawable.createFromXml(getResources(), getResources().getXml(R.xml.shop_item_bg));
                XmlPullParser parser = getResources().getXml(R.xml.shop_item_bg);
                bg.inflate(getResources(), parser, Xml.asAttributeSet(parser));

                assosiatedLayout.setBackground(bg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            assosiatedLayout.setId(getRandomId());

            return assosiatedLayout;
        }
    }

    abstract class NotRealBuyItem extends ShopItem{
        String dataKey;

        void incrementValue(){
            int value = getValue();
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putInt(dataKey, value + 1);
            ed.commit();
        }
        int getValue(){
            sharedPreferences = getSharedPreferences("global", MODE_PRIVATE);
            return sharedPreferences.getInt(dataKey, 0);

        }
        @Override
        void resetValue(){
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putInt(dataKey, 0);
            ed.commit();
        }
        @Override
        void onTrigger(){
            if (getGoldBalance() > getCost()){
                removeGold(getValue());
                incrementValue();
                updateCostText();
                updateLabelText();
                updateGoldLabel();
            }
        }
    }
    class UpgrageItem extends NotRealBuyItem{
        UpgrageItem(String _label, String _dataKey, int _cost){
            dataKey = _dataKey;
            label = _label;
            startCost = _cost;
        }
        @Override
        void updateLabelText(){
            label_tw.setText(label + " " +(getValue() + 1));
        }
        @Override
        int getCost(){
            int result = startCost;
            for (int i = 0; i < getValue(); ++i)
                result = result * 2;
            return result;
        }
    }
    class GoldBuyItem extends ShopItem{
        int gold;

        GoldBuyItem(String _label, int _cost, int _gold){
            label = _label;
            startCost = _cost;
            gold = _gold;
        }

        @Override
        void updateCostText(){
            cost_tw.setText(String.valueOf(getCost()) + " $");
        }
        @Override
        void onTrigger() {
            setGoldAmount(getGoldBalance() + gold);
        }
    }
    class BonusBuyItem extends NotRealBuyItem{
        BonusBuyItem(String _label, String _dataKey, int _cost){
            label = _label;
            dataKey = _dataKey;
            startCost = _cost;
        }

        @Override
        void updateLabelText(){
            label_tw.setText(label + " (" +(getValue()) + ")");
        }
    }
}
