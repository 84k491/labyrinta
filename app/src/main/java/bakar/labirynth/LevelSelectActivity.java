package bakar.labirynth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Random;

public class LevelSelectActivity extends Activity implements View.OnClickListener {

    Random random = new Random(274412536);
    LinearLayout mainLayout;
    final ArrayList<NumeratedTextView> textViews = new ArrayList<>();

    @Override
    public void onClick(View view){
        for (NumeratedTextView tv:textViews){
            if (tv.getId() == view.getId()){
                Intent intent = new Intent(this, LevelSelectActivity.class);
                intent.putExtra("level_size", tv.number);
                setResult(RESULT_OK, intent);
                finish();
                break;
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_level_select);

        mainLayout = findViewById(R.id.ll_level_select);

        fillLayout(getIntent().getIntExtra("max_level_allowed", 0));
        for (NumeratedTextView tv:textViews){
            tv.setOnClickListener(this);
        }
    }

    void fillLayout(int lvl_amount){
        final int row_volume = 4;
        int row_amount = 1 + lvl_amount / row_volume;
        int iter = 0;

        for (int i = 0; i < row_amount; ++i){
            LinearLayout hlo = generateHorLayout();
            mainLayout.addView(hlo);
            for (int k = 0; k < row_volume; ++k){
                if (iter < lvl_amount)
                    hlo.addView(generateTV(++iter));
                else
                    hlo.addView(generateTV(-1));

                if (k != row_volume - 1)
                    hlo.addView(getSpace());
            }

            if (i != row_amount - 1)
                mainLayout.addView(getSpace());
        }
    }

    class NumeratedTextView extends AppCompatTextView{

        int number;
        NumeratedTextView(Context c, int _number){
            super(c);
            number = _number;
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
        return random.nextInt();
    }
    NumeratedTextView generateTV(int num){

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                200,
                200,
                1
        );
        NumeratedTextView res = new NumeratedTextView(this, num);

        if (num > 0){
            if (num > 9){
                res.setText(String.valueOf(num));
            }
            else{
                res.setText("0" + String.valueOf(num));
            }
        }
        else{
            res.setText("  ");
        }

        res.setLayoutParams(params);
        res.setTextSize(40);
        res.setTextColor(Color.WHITE);
        res.setGravity(Gravity.CENTER);

        try{
            Drawable bg;
            bg = Drawable.createFromXml(getResources(), getResources().getXml(R.xml.level_select_bg));
            XmlPullParser parser = getResources().getXml(R.xml.level_select_bg);
            bg.inflate(getResources(), parser, Xml.asAttributeSet(parser));

            res.setBackground(bg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        textViews.add(res);
        res.setId(getRandomId());
        return res;
    }
    LinearLayout generateHorLayout(){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        LinearLayout lo = new LinearLayout(this);
        lo.setOrientation(LinearLayout.HORIZONTAL);
        return lo;
    }

}
