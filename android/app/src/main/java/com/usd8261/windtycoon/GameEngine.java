package com.usd8261.windtycoon;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameEngine {
    public static class FloatingText {
        public float x, y;
        public String text;
        public int color;
        public float alpha = 1.0f;
        public float vy = -2.5f;

        public FloatingText(float x, float y, String text, int color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }

        public boolean update() {
            y += vy;
            alpha -= 0.025f;
            return alpha > 0;
        }
    }

    public final TurbinePlot[][] plots = new TurbinePlot[3][3];
    public double balance = 1200.0;
    public float windSpeed = 8.5f;
    public String weather = "Бриз";
    public float timeOfDay = 0.35f; // 0..1 (0=night, 0.25=sunrise, 0.5=noon, 0.75=sunset)

    public float batteryCapacity = 0.0f;
    public float batteryCharge = 0.0f;
    public float marketPrice = 0.32f; // $/kWh
    public float marketDemand = 1.0f;

    public float sciencePoints = 0.0f;
    public int prestigeCount = 0;
    public double prestigeMultiplier = 1.0;

    public boolean hasTechSupport = false;
    public boolean hasInsurance = false;

    public Contract currentContract = null;
    public final List<FloatingText> floatingTexts = new ArrayList<>();
    public final List<String> eventLog = new ArrayList<>();

    private final Context context;
    private final SoundManager sound;
    private long lastTickTime = System.currentTimeMillis();

    public GameEngine(Context context) {
        this.context = context;
        this.sound = SoundManager.getInstance(context);

        // Initialize 3x3 plots
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                plots[r][c] = new TurbinePlot(r, c);
            }
        }
        // Free starting small turbine
        plots[0][0].type = TurbineType.SMALL;
        plots[0][0].level = 1;

        currentContract = Contract.generateRandom();
        addLog("Добро пожаловать в Wind Turbine Tycoon!");
        addLog("Построен стартовый Малый ветряк на участке [0,0]");

        loadState();
    }

    public void addLog(String message) {
        eventLog.add(0, message);
        if (eventLog.size() > 15) {
            eventLog.remove(eventLog.size() - 1);
        }
    }

    public float getTotalOutputKW() {
        float aeroMult = ResearchItem.BLADE_AERO.unlocked ? 1.2f : 1.0f;
        float total = 0.0f;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                total += plots[r][c].getOutputKW(windSpeed, aeroMult);
            }
        }
        return total;
    }

    public double getIncomePerSecond() {
        float kw = getTotalOutputKW();
        float price = marketPrice * (ResearchItem.GENERATOR.unlocked ? 1.25f : 1.0f);
        double income = (kw * price * 0.1) * prestigeMultiplier;
        if (hasTechSupport) {
            income = Math.max(0, income - 1.0);
        }
        return income;
    }

    public void update() {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.2f, (now - lastTickTime) / 1000.0f);
        lastTickTime = now;

        // Day/Night progression (cycle takes 120 seconds)
        timeOfDay = (timeOfDay + dt / 120.0f) % 1.0f;

        // Weather & Wind fluctuation
        if (Math.random() < 0.02) {
            windSpeed += (float) ((Math.random() - 0.5) * 1.5);
            windSpeed = Math.max(1.5f, Math.min(24.0f, windSpeed));

            if (windSpeed < 3.5f) weather = "Штиль";
            else if (windSpeed < 8.0f) weather = "Бриз";
            else if (windSpeed < 14.0f) weather = "Свежий ветер";
            else if (windSpeed < 19.0f) weather = "Сильный ветер";
            else weather = "Шторм";
        }

        // Market Price fluctuation
        if (Math.random() < 0.03) {
            float base = 0.28f + (float) (Math.sin(timeOfDay * Math.PI * 2) * 0.12);
            float randomWalk = (float) ((Math.random() - 0.5) * 0.08);
            marketPrice = Math.max(0.12f, Math.min(0.85f, base + randomWalk));
            if (ResearchItem.AI_FORECAST.unlocked && Math.random() < 0.1) {
                marketPrice *= 1.35f;
            }
        }

        // Update plots
        boolean autoDrones = ResearchItem.AUTO_DRONES.unlocked;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                plots[r][c].update(windSpeed, autoDrones, hasTechSupport);
            }
        }

        // Generation & Income
        double incomeThisTick = getIncomePerSecond() * dt;
        balance += incomeThisTick;

        // Science generation (0.1 SP/s per active turbine)
        int activeCount = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (!plots[r][c].isEmpty() && !plots[r][c].isBroken) activeCount++;
            }
        }
        sciencePoints += activeCount * 0.08f * dt;

        // Contract timer
        if (currentContract != null && !currentContract.completed) {
            currentContract.remainingSeconds -= dt;
            if (getTotalOutputKW() >= currentContract.targetKW) {
                currentContract.completed = true;
                balance += currentContract.reward;
                addLog("🏆 Контракт '" + currentContract.title + "' выполнен! Награда: +$" + (int) currentContract.reward);
                sound.playSound("coin");
            } else if (currentContract.remainingSeconds <= 0) {
                addLog("⏳ Контракт истек. Новый заказ через минуту.");
                currentContract = Contract.generateRandom();
            }
        } else if (currentContract == null || currentContract.completed) {
            if (Math.random() < 0.01) {
                currentContract = Contract.generateRandom();
                addLog("📋 Новый контракт города: " + currentContract.title);
            }
        }

        // Update floating particles
        Iterator<FloatingText> it = floatingTexts.iterator();
        while (it.hasNext()) {
            if (!it.next().update()) {
                it.remove();
            }
        }
    }

    public void tapTurbine(int r, int c, float screenX, float screenY) {
        TurbinePlot plot = plots[r][c];
        if (plot.isEmpty()) return;

        boolean isCrit = Math.random() < 0.18;
        double gain = (plot.type.baseKW * 0.4 * (isCrit ? 4.5 : 1.0)) * prestigeMultiplier;
        balance += gain;

        if (isCrit) {
            sound.playSound("crit");
            floatingTexts.add(new FloatingText(screenX, screenY, "+$" + (int) gain + " КРИТ!", 0xFFFFD700));
        } else {
            sound.playSound("click");
            floatingTexts.add(new FloatingText(screenX, screenY, "+$" + (int) gain, 0xFF10B981));
        }

        // Spin boost on tap
        plot.bladeAngle = (plot.bladeAngle + 25.0f) % 360.0f;
    }

    public boolean buildTurbine(int r, int c, TurbineType type) {
        TurbinePlot plot = plots[r][c];
        if (!plot.isEmpty()) return false;

        double cost = type.baseCost;
        if (balance >= cost) {
            balance -= cost;
            plot.type = type;
            plot.level = 1;
            plot.condition = 100.0f;
            plot.isBroken = false;
            addLog("🔨 Построен " + type.title + " на участке [" + (r + 1) + "," + (c + 1) + "]");
            sound.playSound("build");
            return true;
        }
        return false;
    }

    public boolean upgradeTurbine(int r, int c) {
        TurbinePlot plot = plots[r][c];
        if (plot.isEmpty() || plot.level >= 10) return false;

        double cost = plot.getUpgradeCost();
        if (balance >= cost) {
            balance -= cost;
            plot.upgrade();
            addLog("⬆️ " + plot.type.title + " улучшен до Lv." + plot.level);
            sound.playSound("upgrade");
            return true;
        }
        return false;
    }

    public boolean evolveTurbine(int r, int c) {
        TurbinePlot plot = plots[r][c];
        if (plot.isEmpty() || !plot.canEvolve()) return false;

        double cost = plot.getEvolutionCost();
        if (balance >= cost) {
            balance -= cost;
            TurbineType oldType = plot.type;
            plot.evolve();
            addLog("⚡ ЭВОЛЮЦИЯ! " + oldType.title + " ➔ " + plot.type.title);
            sound.playSound("evolve");
            return true;
        }
        return false;
    }

    public boolean repairTurbine(int r, int c) {
        TurbinePlot plot = plots[r][c];
        if (plot.isEmpty() || plot.condition >= 100.0f) return false;

        double cost = plot.getRepairCost();
        if (balance >= cost) {
            balance -= cost;
            plot.repair();
            sound.playSound("repair");
            return true;
        }
        return false;
    }

    public boolean unlockResearch(ResearchItem item) {
        if (item.unlocked) return false;
        if (sciencePoints >= item.costSP) {
            sciencePoints -= item.costSP;
            item.unlocked = true;
            addLog("🔬 Исследование завершено: " + item.title);
            sound.playSound("coin");
            return true;
        }
        return false;
    }

    public boolean triggerPrestige() {
        if (balance < 100000.0) return false;

        prestigeCount++;
        prestigeMultiplier = 1.0 + (prestigeCount * 0.5);

        // Reset farm
        balance = 1200.0;
        sciencePoints = 0.0f;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                plots[r][c] = new TurbinePlot(r, c);
            }
        }
        plots[0][0].type = TurbineType.SMALL;
        plots[0][0].level = 1;

        addLog("👑 ПРЕСТИЖ R" + prestigeCount + "! Множитель x" + String.format("%.1f", prestigeMultiplier));
        sound.playSound("rebirth");
        return true;
    }

    public void saveState() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("WindTycoonPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putFloat("balance", (float) balance);
            ed.putFloat("sciencePoints", sciencePoints);
            ed.putInt("prestigeCount", prestigeCount);
            ed.putBoolean("hasTech", hasTechSupport);
            ed.putBoolean("hasInsurance", hasInsurance);

            for (ResearchItem item : ResearchItem.values()) {
                ed.putBoolean("res_" + item.name(), item.unlocked);
            }

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    TurbinePlot p = plots[r][c];
                    String prefix = "p_" + r + "_" + c + "_";
                    ed.putString(prefix + "type", p.type != null ? p.type.name() : "");
                    ed.putInt(prefix + "level", p.level);
                    ed.putFloat(prefix + "condition", p.condition);
                }
            }
            ed.apply();
        } catch (Exception ignored) {}
    }

    public void loadState() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("WindTycoonPrefs", Context.MODE_PRIVATE);
            if (!prefs.contains("balance")) return;

            balance = prefs.getFloat("balance", 1200.0f);
            sciencePoints = prefs.getFloat("sciencePoints", 0.0f);
            prestigeCount = prefs.getInt("prestigeCount", 0);
            prestigeMultiplier = 1.0 + (prestigeCount * 0.5);
            hasTechSupport = prefs.getBoolean("hasTech", false);
            hasInsurance = prefs.getBoolean("hasInsurance", false);

            for (ResearchItem item : ResearchItem.values()) {
                item.unlocked = prefs.getBoolean("res_" + item.name(), false);
            }

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    String prefix = "p_" + r + "_" + c + "_";
                    String typeName = prefs.getString(prefix + "type", "");
                    if (!typeName.isEmpty()) {
                        try {
                            plots[r][c].type = TurbineType.valueOf(typeName);
                            plots[r][c].level = prefs.getInt(prefix + "level", 1);
                            plots[r][c].condition = prefs.getFloat(prefix + "condition", 100.0f);
                        } catch (Exception ignored) {}
                    } else {
                        plots[r][c].type = null;
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
