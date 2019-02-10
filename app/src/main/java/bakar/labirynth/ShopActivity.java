package bakar.labirynth;

import android.app.Activity;
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
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.ArrayList;

public class ShopActivity extends Activity {

    LinearLayout layout;
    ArrayList<ShopItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_shop);

        layout = findViewById(R.id.ll_scroll_layout);

        setItems();

        for (ShopItem item : items){
            layout.addView(item.getLayout());
            Space space = new Space(ShopActivity.this);
            space.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    50));
            layout.addView(space);
        }
    }

    void setItems(){
        for (int i = 0; i < 20; ++i)
            items.add(new ShopItem("Test label " + (i + 1), 26 + i * 2));
    }

    class ShopItem{
        String label;
        int cost = 0;
        LinearLayout assosiatedLayout = null;

        ShopItem(String _label, int _cost){
            label = _label;
            cost = _cost;
        }

        LinearLayout getLayout(){
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

            TextView label_tw = new TextView(ShopActivity.this);
            label_tw.setLayoutParams(labelParams);
            label_tw.setText(label);
            label_tw.setTextSize(20);
            label_tw.setTextColor(Color.WHITE);
            label_tw.setGravity(Gravity.CENTER);

            TextView cost_tw = new TextView(ShopActivity.this);
            cost_tw.setLayoutParams(costParams);
            cost_tw.setText(String.valueOf(cost));
            cost_tw.setTextSize(25);
            cost_tw.setTextColor(Color.WHITE);
            cost_tw.setGravity(Gravity.CENTER);

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

            return assosiatedLayout;
        }
    }
}
