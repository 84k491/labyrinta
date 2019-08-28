package bakar.labyrinta;

import android.arch.core.util.Function;
import android.graphics.Point;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Economist {
    private static final Economist ourInstance = new Economist();

    private final float coinCostAvg;

    static final int maxLevel = 30;
    static final int maxUpgPathfinder = 10;
    static final int maxUpgTeleport = 5;

    private final Random random;

    private final float nextLevelHypotIncrementation = (float)(5.f * Math.sqrt(2));
    private final float startLevelHypot = (float)(15.f * Math.sqrt(2));

    private final float coinIncomeToRewardCoef = 1.f;

    private final LinearFunction avgLengthOfHypot = new LinearFunction(1.6862f, 5.8403f);
    private final LinearFunction levelAmountOfLength = new LinearFunction(0.f, 5.f);

    private final LinearFunction pathfinderCostOfUpgLevel;
    private final LinearFunction teleportCostOfUpgLevel;
    private final LinearFunction pointerCostOfUpgLevel;

    private final LinearFunction pathfinderUpgCostOfUpgLevel;
    private final LinearFunction teleportUpgCostOfUpgLevel;
    private final LinearFunction pointerUpgCostOfUpgLevel;

    private final LinearFunction levelRewardOfLength;

    private final LinearFunction coinsAmountOfSquare;
    private final LinearFunction pointerPropabilityOfSquare;
    private final LinearFunction teleportPropabilityOfSquare;
    private final LinearFunction pathfinderPropabilityOfSquare;

    Map<String, Function<Integer, Integer> > price_map = new HashMap<>();

    public static Economist getInstance() {
        return ourInstance;
    }
    void priceMapSetUp(){
        price_map.put(StoredProgress.teleportUpgKey,
                (Integer upgLevel) -> {return Math.round(teleportUpgCostOfUpgLevel.calc(upgLevel));});

        price_map.put(StoredProgress.pointerUpgKey,
                (Integer upgLevel) -> {return Math.round(pointerUpgCostOfUpgLevel.calc(upgLevel));});

        price_map.put(StoredProgress.pathfinderUpgKey,
                (Integer upgLevel) -> {return Math.round(pathfinderUpgCostOfUpgLevel.calc(upgLevel));});

        price_map.put(StoredProgress.pathfinderAmountKey,
                (Integer upgLevel) -> {return Math.round(
                        pathfinderCostOfUpgLevel.calc(StoredProgress.getInstance().getValue(
                                StoredProgress.pathfinderUpgKey)));});

        price_map.put(StoredProgress.teleportAmountKey,
                (Integer upgLevel) -> {return Math.round(
                        teleportCostOfUpgLevel.calc(StoredProgress.getInstance().getValue(
                                StoredProgress.teleportUpgKey)));});

        price_map.put(StoredProgress.pointerAmountKey,
                (Integer upgLevel) -> {return Math.round(
                        pointerCostOfUpgLevel.calc(StoredProgress.getInstance().getValue(
                                StoredProgress.pointerUpgKey)));});

        price_map.put(StoredProgress.levelUpgKey,
                (Integer upgLevel) -> {return getNextLevelCost(getLevelHypotByUpg(upgLevel));});
    }

    private Economist(){
        priceMapSetUp();
        random = new Random(System.currentTimeMillis());

        levelRewardOfLength = new LinearFunction(.5f, .0f);

        float square = 20 * 20;

        coinsAmountOfSquare = new LinearFunction(2.f * (1 / square), .0f);
        pointerPropabilityOfSquare = new LinearFunction(.03f * (1 / square), 0.f);
        teleportPropabilityOfSquare = new LinearFunction(.01f * (1 / square), 0.f);
        pathfinderPropabilityOfSquare = new LinearFunction(.02f * (1 / square), 0.f);

        float exampleHypot = 25.f;
        coinCostAvg = (getLevelReward(exampleHypot) * coinIncomeToRewardCoef) / getCoinsAmountAvg(exampleHypot);

        float baseCost = getLevelReward(getLevelHypotByUpg(1));

        pathfinderCostOfUpgLevel = new LinearFunction(baseCost * 2.f, baseCost * 7);
        teleportCostOfUpgLevel = new LinearFunction(baseCost * 3.f, baseCost * 9);
        pointerCostOfUpgLevel = new LinearFunction(baseCost * 1.2f, baseCost * 5);

        pathfinderUpgCostOfUpgLevel = new LinearFunction(baseCost * 3.7f, baseCost * 24);
        teleportUpgCostOfUpgLevel = new LinearFunction(baseCost * 5.3f, baseCost * 30);
        pointerUpgCostOfUpgLevel = new LinearFunction(baseCost * 3.2f, baseCost * 15);
    }

    float getLevelHypotByUpg(Integer upgLevel){
        return startLevelHypot + (upgLevel - 1) * nextLevelHypotIncrementation;
    }

    float getSquareSide(float hypot){
        return (float)(hypot / Math.sqrt(2));
    }
    float getSquare(float hypot){
        return getSquareSide(hypot) * getSquareSide(hypot);
    }

    float getAverageLength(float hypot){
        return avgLengthOfHypot.calc(hypot);
    }

    static float hypot(Point levelSize){
        return (float)Math.sqrt(levelSize.x * levelSize.x + levelSize.y * levelSize.y);
    }

    int getNextLevelCost(float currentHypot){
        return Math.round(getNumOfLevelsToUnlockNext(currentHypot) *
                getLevelTotalIncome(currentHypot, 50.f));
    }

    float getNumOfLevelsToUnlockNext(float hypot){
        return levelAmountOfLength.calc(getAverageLength(hypot));
    }

    int getLevelReward(float hypot){
        return Math.round(levelRewardOfLength.calc(getAverageLength(hypot)));
    }

    int getLevelTotalIncome(float hypot, float coinsPercent){
        float result = 0;
        result += getCoinsAmountAvg(hypot) * coinCostAvg * (coinsPercent / 100.f);
        result += getLevelReward(hypot);
        return Math.round(result);
    }

    float getCoinsAmountAvg(float hypot){
        return coinsAmountOfSquare.calc(getSquare(hypot));
    }

    int getCoinsAmountRand(float hypot){
        float avg = getCoinsAmountAvg(hypot);
        float rand = random.nextFloat() * 2.f - 1.f;
        rand *= avg * 0.15f;
        return Math.round(avg + rand);
    }

    int getCoinCostRand(){
        float rand = random.nextFloat() * 2.f - 1.f;
        rand *= coinCostAvg * 0.2;
        return Math.round(coinCostAvg + rand);
    }

    float getTeleportPropability(float hypot){
        return teleportPropabilityOfSquare.calc(getSquare(hypot));
    }
    float getPathfinderPropability(float hypot){
        return pathfinderPropabilityOfSquare.calc(getSquare(hypot));
    }
    float getPointerPropability(float hypot){
        return pointerPropabilityOfSquare.calc(getSquare(hypot));
    }

    private class LinearFunction{
        final private float _coeficient;
        final private float _constant;

        LinearFunction(float coeficient, float constant){
            _coeficient = coeficient;
            _constant = constant;
        }

        float calc(float arg){
            return arg * _coeficient + _constant;
        }
    }
}
