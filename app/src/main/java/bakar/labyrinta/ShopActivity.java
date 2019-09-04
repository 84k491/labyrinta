package bakar.labyrinta;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

public class ShopActivity extends Activity implements View.OnClickListener {

    // static //
    private int s_level_upg = 0;
    private int s_teleport_upg = 0;
    private int s_pathfinder_upg = 0;
    ////////////

    // VIDEO ADS //
    private RewardedVideoAd mRewardedVideoAd;
    private final RewardedVideoAdListener mRewardedVideoAdListener = new RewardedVideoAdListener() {
        @Override
        public void onRewardedVideoAdLoaded() {
            Logger.getAnonymousLogger().info("Video ad loaded!");
            try{
                Objects.requireNonNull(getVideoShopItem()).setVideoIcon();
            }
            catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onRewardedVideoAdOpened() {
            Logger.getAnonymousLogger().info("Video ad opened!");
        }

        @Override
        public void onRewardedVideoStarted() {
            Logger.getAnonymousLogger().info("Video ad started!");
        }

        @Override
        public void onRewardedVideoAdClosed() {
            Logger.getAnonymousLogger().info("Video ad closed!");
            loadRewardedVideoAd();
        }

        @Override
        public void onRewarded(RewardItem rewardItem) {
            Logger.getAnonymousLogger().info("Video ad onRewarded() type: " + rewardItem.getType() +
                    " amount: " + rewardItem.getAmount());

            StoredProgress.getInstance().setGold(StoredProgress.getInstance().getGoldAmount()
                    + rewardItem.getAmount()
            );
        }

        @Override
        public void onRewardedVideoAdLeftApplication() {
            Logger.getAnonymousLogger().info("Video ad LeftApplication!");

        }

        @Override
        public void onRewardedVideoAdFailedToLoad(int i) {
            Logger.getAnonymousLogger().info("Video ad failed! Reason: " + i);

        }

        @Override
        public void onRewardedVideoCompleted() {
            Logger.getAnonymousLogger().info("Video ad completed!");
        }
    };

    private LinearLayout layout;
    private final ArrayList<ShopItem> items = new ArrayList<>();
    private final Random random = new Random(6654345);
    private final int layoutHeight = 250; // FIXME: 3/27/19
    private TextView gold;
    private Animation on_click_anim;
    private Map<String, Integer> id_map;
    private Drawable coinIcon;

    private void setIdMap(){
        id_map = new HashMap<>();

        id_map.put(StoredProgress.levelUpgKey, R.drawable.green_arrow);

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

        Logger.getAnonymousLogger().info("ShopActivity onCreate()");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_shop);

        setIdMap();

        coinIcon = new BitmapDrawable(getResources(),
                BitmapFactory.decodeResource(getResources(), R.drawable.coin_anim1));
        on_click_anim = AnimationUtils.loadAnimation(this, R.anim.on_button_tap);

        gold = findViewById(R.id.tw_gold_amount_shop);
        gold.setTypeface(StoredProgress.getInstance().getTrenchFont(getAssets()));
        gold.setTextColor(Color.WHITE);

        layout = findViewById(R.id.ll_scroll_layout);

        rebuildLayout();

        findViewById(R.id.bt_shop_back).setOnClickListener(this);

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(mRewardedVideoAdListener);
        loadRewardedVideoAd();
    }

    private void loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(getString(R.string.interstitial_video_id),
                new AdRequest.Builder().build());
    }

    private void showVideoAd(){
        if (mRewardedVideoAd.isLoaded()) {
            mRewardedVideoAd.show();
        }
        else{
            Logger.getAnonymousLogger().info("Video ad not loaded!");
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        final Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.fontScale = 1.0f;
        applyOverrideConfiguration(configuration);
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onPause(){
        SoundCore.inst().pauseMenuBackgroundMusic();
        super.onPause();
    }

    @Override
    protected void onStop(){
        Logger.getAnonymousLogger().info("ShopActivity onStop()");
        super.onStop();
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
    protected void onResume(){
        super.onResume();
        Logger.getAnonymousLogger().info("ShopActivity onResume()");

        SoundCore.inst().playMenuBackgroundMusic();

        if (mRewardedVideoAd != null)
        {
            if (mRewardedVideoAd.isLoaded())
            {
                try{
                    Objects.requireNonNull(getVideoShopItem()).setVideoIcon();
                }
                catch (NullPointerException e){
                    e.printStackTrace();
                }
            }
            else
            {
                try{
                    Objects.requireNonNull(getVideoShopItem()).setLoadingIcon();
                }
                catch (NullPointerException e){
                    e.printStackTrace();
                }
            }
        }

        updateGoldLabel();
    }

    private boolean checkIfNeedToRebuild(){
        int loc_level_upg = StoredProgress.getInstance().getValue(
                StoredProgress.levelUpgKey);
        int loc_teleport_upg = StoredProgress.getInstance().getValue(
                StoredProgress.teleportUpgKey);
        int loc_pathfinder_upg = StoredProgress.getInstance().getValue(
                StoredProgress.pathfinderUpgKey);

        if (loc_level_upg != s_level_upg){
            s_level_upg = loc_level_upg;
            if (loc_level_upg >= Economist.maxLevel){
                return true;
            }
        }

        if (loc_pathfinder_upg != s_pathfinder_upg){
            s_pathfinder_upg = loc_pathfinder_upg;
            if (loc_pathfinder_upg >= Economist.maxUpgPathfinder){
                return true;
            }
        }

        if (loc_teleport_upg != s_teleport_upg){
            s_teleport_upg = loc_teleport_upg;
            return loc_teleport_upg >= Economist.maxUpgTeleport;
        }

        return false;
    }
    private void rebuildLayout(){
        if (null == layout){
            return;
        }

        layout.removeAllViews();
        items.clear();

        setItems();

        layout.addView(getSpace());
        layout.addView(getSpace());
        layout.addView(getSpace());

        for (ShopItem item : items){
            layout.addView(item.spacesDecorator(item.getMainLayout()));
            layout.addView(getSpace());
            item.getMainLayout().setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view){
        if (view.getId() == R.id.bt_shop_back){
            setResult(RESULT_CANCELED);
            finish();
        }

        for (ShopItem item:items
             ) {
            if (item.getMainLayout().getId() == view.getId()){
                item.getMainLayout().startAnimation(on_click_anim);
                item.onTrigger();
                updateGoldLabel();
                break;
            }
        }

        if (checkIfNeedToRebuild()){
            rebuildLayout();
        }
    }

    private void updateGoldLabel(){
        if (gold != null) {
            gold.setText(String.valueOf(StoredProgress.getInstance().getGoldAmount()));
        }
    }

    private int getRandomId(){
        // fixme
        return random.nextInt();
    }

    private void setItems(){
        items.add(new RewardedVideoShopItem());

        if (StoredProgress.getInstance().getValue(
                StoredProgress.levelUpgKey
        ) < Economist.maxLevel){
            items.add(new LevelUpItem(StoredProgress.levelUpgKey));
        }

        if (!StoredProgress.getInstance().getValueBoolean(
                StoredProgress.isNeedToShowTutorialTeleport
        ) ){
            if (StoredProgress.getInstance().getValue(
                    StoredProgress.teleportUpgKey) < Economist.maxUpgTeleport){
                items.add(new UpgrageItem(StoredProgress.teleportUpgKey));
            }
            items.add(new BonusBuyItem(StoredProgress.teleportAmountKey));
        }

        if (!StoredProgress.getInstance().getValueBoolean(
                StoredProgress.isNeedToShowTutorialPathfinder
        ) ){
            if (StoredProgress.getInstance().getValue(
                    StoredProgress.pathfinderUpgKey)  < Economist.maxUpgPathfinder){
                items.add(new UpgrageItem(StoredProgress.pathfinderUpgKey));
            }
            items.add(new BonusBuyItem(StoredProgress.pathfinderAmountKey));
        }
        if (!StoredProgress.getInstance().getValueBoolean(
                StoredProgress.isNeedToShowTutorialPointer
        ) ){
            items.add(new BonusBuyItem(StoredProgress.pointerAmountKey));
            //items.add(new UpgrageItem(StoredProgress.getInstance().pointerUpgKey));
        }
//        items.add(new GoldBuyItem(5,300));
        //items.add(new GoldBuyItem(10,1000));
    }
    private Space getSpace(){
        Space space = new Space(ShopActivity.this);
        space.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50));
        return space;
    }

    @Nullable
    private RewardedVideoShopItem getVideoShopItem(){
        for (ShopItem item : items){
            if (item instanceof RewardedVideoShopItem){
                return (RewardedVideoShopItem)item;
            }
        }
        return null;
    }

    abstract class ShopItem{
        String label;
        LinearLayout assosiatedLayout = null;
        ImageView costIcon = null;
        int mainIconResource = -1;
        View mainIcon = null;
        final float iconSizeCoef = .7f;
        TextView label_tw = null; // TODO: 3/27/19 rename
        TextView cost_tw = null;

        ShopItem(){}

        void removeGold(int g){
            StoredProgress.getInstance().setGold(StoredProgress.getInstance().getGoldAmount() - g);
        }

        void updateLabelText(){label_tw.setText(label);}
        void updateCostText(){cost_tw.setText(" " + getCost());}
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
            result.addView(centerLO);
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

            assosiatedLayout.setId(getRandomId());

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
            cost_tw.setTextSize(17);
            cost_tw.setTextColor(Color.WHITE);
            cost_tw.setGravity(Gravity.CENTER);
            cost_tw.setTypeface(StoredProgress.getInstance().getTrenchFont(getAssets()));
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
                SoundCore.inst().playSound(Sounds.correct);
                removeGold(getCost());
                incrementValue();
                updateCostText();
                updateLabelText();
                updateGoldLabel();
            }
            else
            {
                SoundCore.inst().playSound(Sounds.incorrect);
            }
        }
        @Override
        int getCost(){
            return Objects.requireNonNull(Economist.getInstance().price_map.get(dataKey)).apply(getValue());
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
            label_tw.setTypeface(StoredProgress.getInstance().getTrenchFont(getAssets()));
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

    class RewardedVideoShopItem extends ShopItem{

        final int loadingIconResource;
        final int videoIconResource;

        RewardedVideoShopItem(){
            videoIconResource = R.drawable.video_ad;
            loadingIconResource = R.drawable.loading_ad;
            mainIconResource = loadingIconResource;
        }

        @Override
        ConstraintLayout getFinalIconLayout(){
            ConstraintLayout layout = getMainIconLayout();
            setLoadingIcon();
            return layout;
        }

        @Override
        int getCost(){
            return 400;
        }

        @Override
        void updateCostText(){
            cost_tw.setText(" +" + getCost());
            cost_tw.setTextColor(Color.rgb(122, 251, 122));
        }

        @Override
        void onTrigger() {
            showVideoAd();
        }

        void setLoadingIcon(){
            mainIcon.setBackgroundResource(loadingIconResource);
            mainIcon.startAnimation(
                    AnimationUtils.loadAnimation(ShopActivity.this, R.anim.rotate_around_center)
            );
        }

        void setVideoIcon(){
            mainIcon.setBackgroundResource(videoIconResource);
            mainIcon.clearAnimation();
        }
    }

    class UpgrageItem extends NotRealBuyItem{
        UpgrageItem(String _dataKey){
            dataKey = _dataKey;
            if (dataKey != null){
                mainIconResource = Objects.requireNonNull(id_map.get(dataKey));
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
    class LevelUpItem extends UpgrageItem{
        LevelUpItem(String dataKey){
            super(dataKey);
        }

        @Override
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

            mainIcon = new TextView(ShopActivity.this);
            mainIcon.setId(getRandomId());
            ((TextView)mainIcon).setTextColor(Color.WHITE);
            updateLabelText();
            ((TextView)mainIcon).setTextSize(15.f);
            ((TextView)mainIcon).setTypeface(
                    StoredProgress.getInstance().getTrenchFont(getAssets()));
            ((TextView)mainIcon).setGravity(Gravity.CENTER);
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
        @Override
        void updateLabelText(){
            ((TextView)mainIcon).setText(getString(R.string.shop_item_level, (getValue() + 1)));
        }
        @Override
        ConstraintLayout getFinalIconLayout(){
            return getMainIconLayout();
        }
    }
    class GoldBuyItem extends ShopItem{
        final int gold;
        final int startCost;

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
            SoundCore.inst().playSound(Sounds.correct);
            StoredProgress.getInstance().setGold(StoredProgress.getInstance().getGoldAmount() + gold);
        }
    }
    class BonusBuyItem extends NotRealBuyItem{
        BonusBuyItem(String _dataKey){
            dataKey = _dataKey;
            if (dataKey != null)
                mainIconResource = Objects.requireNonNull(id_map.get(dataKey));
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
