package bakar.labirynth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatTextView;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
    Animation on_click_anim; //todo анимация не успевает показаться
    final ArrayList<NumeratedTextView> textViews = new ArrayList<>();

    @Override
    public void onClick(View view){
        if (view.getId() == R.id.bt_select_back){
            Intent intent = new Intent(this, LevelSelectActivity.class);
            intent.putExtra("level_size", -1);
            setResult(RESULT_CANCELED, intent);
            finish();
        }

        for (NumeratedTextView tv:textViews){
            if (tv.getId() == view.getId()){
                tv.startAnimation(on_click_anim);
                if (tv.number > 0){
                    Intent intent = new Intent(this, LevelSelectActivity.class);
                    intent.putExtra("level_size", tv.number);
                    setResult(RESULT_OK, intent);
                    finish();
                }
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

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_level_select);

        mainLayout = findViewById(R.id.ll_level_select);
        ((ConstraintLayout)findViewById(R.id.lsa_constraint_lo))
                .addView(new Background(this), 0);
        on_click_anim = AnimationUtils.loadAnimation(this, R.anim.on_button_tap);

        findViewById(R.id.bt_select_back).setOnClickListener(this);

        fillLayout(getIntent().getIntExtra("max_level_allowed", 0));
        for (NumeratedTextView tv : textViews){
            tv.setOnClickListener(this);
        }
    }

    void fillLayout(int lvl_amount){
        final int row_volume = 4;
        int row_amount = 1 + lvl_amount / row_volume;
        int iter = 0;

        mainLayout.addView(getSpace());
        mainLayout.addView(getSpace());
        for (int i = 0; i < row_amount; ++i){
            LinearLayout hlo = generateHorLayout();
            mainLayout.addView(hlo);
            hlo.addView(getSpace());
            for (int k = 0; k < row_volume; ++k){
                if (iter < lvl_amount)
                    hlo.addView(generateTV(++iter));
                else
                    if (iter++ == lvl_amount)
                        hlo.addView(generateTV(0));
                    else
                        hlo.addView(generateTV(-1));


                    hlo.addView(getSpace());
            }

            if (i != row_amount - 1){
                mainLayout.addView(getSpace());
            }
        }
    }

    class NumeratedTextView extends AppCompatTextView{

        int number;
        NumeratedTextView(Context c, int _number){
            super(c);
            number = _number;
            setTypeface(Typeface.createFromAsset(getAssets(), "fonts/trench100free.ttf"));
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
        return random.nextInt(); // TODO: 3/9/19 fix this
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

        if (0 == num){
            res.setText("S");
        }

        res.setTextSize(40);
        res.setLayoutParams(params);
        res.setTextColor(Color.WHITE);
        res.setGravity(Gravity.CENTER);

        if (num >= 0){
            try{
                Drawable bg;
                bg = Drawable.createFromXml(getResources(), getResources().getXml(R.xml.level_select_bg));
                XmlPullParser parser = getResources().getXml(R.xml.level_select_bg);
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
