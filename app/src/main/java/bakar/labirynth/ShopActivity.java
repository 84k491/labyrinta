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
            item.resetValue();
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
            if (item.getLayout().getId() == view.getId())
                item.onTrigger();
        }
    }

    void updateGoldLabel(){
        if (gold != null) {
            gold.setText(String.valueOf(getCreditBalance()));
        }
    }

    private int getRandomId(){
        return random.nextInt();
    }
    int getCreditBalance(){
        sharedPreferences = getSharedPreferences("global", MODE_PRIVATE);
        return sharedPreferences.getInt("gold", 0);
    }

    void setItems(){
        items.add(new ShopItem("Next level", "next_level", 355, ItemType.Level));
        items.add(new ShopItem("Teleport upgrade", "tp_upg", 45, ItemType.Upgrade));
        items.add(new ShopItem("Pathfinder upgrade","pf_upg", 22, ItemType.Upgrade));
        items.add(new ShopItem("Pointer upgrade", "pt_upg", 14, ItemType.Upgrade));
        items.add(new ShopItem("More credits (100)","credits100", 3, ItemType.Credit));
        items.add(new ShopItem("More credits (300)", "credits300",5, ItemType.Credit));
        items.add(new ShopItem("More credits (1000)", "credits1000",10, ItemType.Credit));
    }
    Space getSpace(){
        Space space = new Space(ShopActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50));
        return space;
    }

    class ShopItem{

        String label;
        String dataKey;
        int cost;
        ItemType type;
        boolean isReal;
        LinearLayout assosiatedLayout = null;
        TextView label_tw = null;
        TextView cost_tw;

        void resetValue(){
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putInt(dataKey, 0);
            ed.commit();
        }

        int getValue(){
            sharedPreferences = getSharedPreferences("global", MODE_PRIVATE);
            return sharedPreferences.getInt(dataKey, 0);

        }
        int incrementValue(){
            int value = getValue();
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putInt(dataKey, value + 1);
            ed.commit();
            return cost;
        }

        void removeGold(int g){
            sharedPreferences = getSharedPreferences("global", MODE_PRIVATE);
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putInt("gold", getCreditBalance() - cost);
            ed.commit();
        }

        void updateLabelText(){
            if (type == ItemType.Level || type == ItemType.Upgrade)
                label_tw.setText(label + " " +(getValue() + 1));
            else
                label_tw.setText(label);
        }
        void updateCostText(){
            cost_tw.setText(String.valueOf(cost) + " " + (isReal ? "$" : "Cr"));
        }

        int getStartCost(){
            if (dataKey.equals("next_level")){
                return 120;
            }
            if (dataKey.equals("tp_upg")){
                return 70;
            }
            if (dataKey.equals("pf_upg")){
                return 50;
            }
            if (dataKey.equals("pt_upg")){
                return 30;
            }
            if (dataKey.equals("credits100")){
                return 3;
            }
            if (dataKey.equals("credits300")){
                return 5;
            }
            if (dataKey.equals("credits1000")){
                return 10;
            }
            return 0;
        }
        int getCostByValue(int v){
            if (0 == v)
                return getStartCost();
            else
                return 2 * getCostByValue(v - 1);
        }

        void onTrigger(){
            if (getCreditBalance() > cost){
                removeGold(incrementValue());
                cost = getCostByValue(getValue());
                updateCostText();
                updateLabelText();
                updateGoldLabel();
            }
        }

        ShopItem(String _label, String _dataKey, int _cost, ItemType t){
            dataKey = _dataKey;
            label = _label;
            cost = _cost;
            type = t;
            isReal = ItemType.Credit == type;
        }

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

    enum ItemType{
        None,
        Level,
        Upgrade,
        Credit
    }
}
