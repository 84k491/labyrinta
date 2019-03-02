package bakar.labirynth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Random;

public class ShopActivity extends Activity implements View.OnClickListener {

    LinearLayout layout;
    ArrayList<ShopItem> items = new ArrayList<>();
    Random random = new Random(6654345);
    TextView gold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_shop);

        //setGoldAmount(10);

        layout = (LinearLayout)findViewById(R.id.ll_scroll_layout);
        gold = (TextView)findViewById(R.id.tw_gold_amount);
        updateGoldLabel();

        setItems();

        for (ShopItem item : items){
            //item.resetValue();
            layout.addView(item.getLayout());
            layout.addView(getSpace());
            item.getLayout().setOnClickListener(this);
        }
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
            gold.setText(String.valueOf(StoredProgress.getInstance().getGoldAmount()));
        }
    }

    private int getRandomId(){
        return random.nextInt();
    }

    void setItems(){
        items.add(new UpgrageItem("Level size", StoredProgress.getInstance().levelUpgKey));
        items.add(new BonusBuyItem("Teleport", StoredProgress.getInstance().teleportAmountKey));
        items.add(new BonusBuyItem("Pathfinder", StoredProgress.getInstance().pathfinderAmountKey));
        items.add(new BonusBuyItem("Pointer", StoredProgress.getInstance().pointerAmountKey));
        items.add(new UpgrageItem("Teleport upgrade", StoredProgress.getInstance().teleportUpgKey));
        items.add(new UpgrageItem("Pathfinder upgrade", StoredProgress.getInstance().pathfinderUpgKey));
        items.add(new UpgrageItem("Pointer upgrade", StoredProgress.getInstance().pointerUpgKey));
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
        LinearLayout assosiatedLayout = null;
        TextView label_tw = null;
        TextView cost_tw = null;

        ShopItem(){}

        void removeGold(int g){
            StoredProgress.getInstance().setGold(StoredProgress.getInstance().getGoldAmount() - g);
        }
        void resetValue(){}

        void updateLabelText(){label_tw.setText(label);}
        void updateCostText(){cost_tw.setText(String.valueOf(getCost()) + " Cr");}
        abstract int getCost();
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
            StoredProgress.getInstance().setValue(dataKey, getValue() + 1);
        }
        int getValue(){
            return StoredProgress.getInstance().getValue(dataKey);
        }
        @Override
        void resetValue(){
            StoredProgress.getInstance().setValue(dataKey, 0);
        }
        @Override
        void onTrigger(){
            if (StoredProgress.getInstance().getGoldAmount() > getCost()){
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
    }

    class UpgrageItem extends NotRealBuyItem{
        UpgrageItem(String _label, String _dataKey){
            dataKey = _dataKey;
            label = _label;
        }
        @Override
        void updateLabelText(){
            label_tw.setText(label + " " +(getValue() + 1));
        }
    }
    class GoldBuyItem extends ShopItem{
        int gold;
        int startCost;

        GoldBuyItem(String _label, int _cost, int _gold){
            label = _label;
            startCost = _cost;
            gold = _gold;
        }

        @Override
        int getCost() {
            return startCost;
        }
        @Override
        void updateCostText(){
            cost_tw.setText(String.valueOf(getCost()) + " $");
        }
        @Override
        void onTrigger() {
            StoredProgress.getInstance().setGold(StoredProgress.getInstance().getGoldAmount() + gold);
        }
    }
    class BonusBuyItem extends NotRealBuyItem{
        BonusBuyItem(String _label, String _dataKey){
            label = _label;
            dataKey = _dataKey;
        }

        @Override
        void updateLabelText(){
            label_tw.setText(label + " (" +(getValue()) + ")");
        }
    }
}
