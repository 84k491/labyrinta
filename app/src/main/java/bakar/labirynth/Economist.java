package bakar.labirynth;

import android.arch.core.util.Function;
import android.graphics.Point;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Economist {
    private static final Economist ourInstance = new Economist();

    private final float coinCostAvg;

    private final Random random;

    private final float nextLevelHypotIncrementation = (float)(5.f * Math.sqrt(2));
    private final float startLevelHypot = (float)(15.f * Math.sqrt(2));

    private final float coinIncomeToRewardCoef = 1.f;

    private final LinearFunction avgLengthOfHypot = new LinearFunction(1.7f, 3.f);
    private final LinearFunction levelAmountOfLength = new LinearFunction(0.f, 5.f);

    private final LinearFunction pathfinderCostOfUpgLevel = new LinearFunction(200.f, 400.f);
    private final LinearFunction teleportCostOfUpgLevel = new LinearFunction(230.f, 500.f);
    private final LinearFunction pointerCostOfUpgLevel = new LinearFunction(170f, 300.f);

    private final LinearFunction pathfinderUpgCostOfUpgLevel = new LinearFunction(3000.f, 8000.f);
    private final LinearFunction teleportUpgCostOfUpgLevel = new LinearFunction(4000.f, 12000.f);
    private final LinearFunction pointerUpgCostOfUpgLevel = new LinearFunction(2000.f, 5000.f);

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

        coinsAmountOfSquare = new LinearFunction(4.f * (1 / square), .0f);
        pointerPropabilityOfSquare = new LinearFunction(.03f * (1 / square), 0.f);
        teleportPropabilityOfSquare = new LinearFunction(.01f * (1 / square), 0.f);
        pathfinderPropabilityOfSquare = new LinearFunction(.02f * (1 / square), 0.f);

        float exampleHypot = 25.f;
        coinCostAvg = (getLevelReward(exampleHypot) * coinIncomeToRewardCoef) / getCoinsAmountAvg(exampleHypot);
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
