package com.usd8261.windtycoon;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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
    public String weatherForecast = "Свежий ветер";
    public int forecastSeconds = 45;
    public float timeOfDay = 0.35f; // 0..1 (0=night, 0.25=sunrise, 0.5=noon, 0.75=sunset)

    public float batteryCapacity = 0.0f;
    public float batteryCharge = 0.0f;
    public float marketPrice = 0.32f; // $/kWh
    public float marketDemand = 1.0f;

    // Green Hydrogen & Mega-Projects
    public float hydrogenStored = 0.0f;
    public float hydrogenCapacity = 500.0f;
    public boolean techHydrogenUnlocked = false;
    public boolean techOrbitalLink = false;

    // ESG Carbon Credits & City Policies
    public int carbonCredits = 0;
    public float carbonAccumulator = 0.0f;
    public boolean policyGreenTariff = false;
    public boolean policyDurability = false;
    public boolean policyScience = false;
    public boolean policyBattery = false;

    public float sciencePoints = 0.0f;
    public int prestigeCount = 0;
    public double prestigeMultiplier = 1.0;

    public boolean hasTechSupport = false;
    public boolean hasInsurance = false;

    // Biomes & Seasons
    public Biome currentBiome = Biome.VALLEY;
    public final boolean[] unlockedBiomes = new boolean[Biome.values().length];
    public Season currentSeason = Season.SPRING;
    public float seasonTimer = 0.0f;

    public Contract currentContract = null;
    public final List<FloatingText> floatingTexts = new ArrayList<>();
    public final List<String> eventLog = new ArrayList<>();

    private final Context context;
    private final SoundManager sound;
    private long lastTickTime = System.currentTimeMillis();

    public GameEngine(Context context) {
        this.context = context;
        this.sound = SoundManager.getInstance(context);

        // Valley is unlocked by default
        unlockedBiomes[Biome.VALLEY.ordinal()] = true;

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
        addLog("Построен стартовый Малый ветряк на участке [1,1]");

        loadState();
        checkOfflineProgress();
    }

    public void addLog(String message) {
        eventLog.add(0, message);
        if (eventLog.size() > 15) {
            eventLog.remove(eventLog.size() - 1);
        }
    }

    private void checkOfflineProgress() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("WindTycoonPrefs", Context.MODE_PRIVATE);
            long lastSave = prefs.getLong("lastSaveTimestamp", 0);
            long now = System.currentTimeMillis();
            if (lastSave > 0 && now > lastSave) {
                long offlineSec = Math.min(12 * 3600, (now - lastSave) / 1000);
                if (offlineSec >= 20) {
                    double rate = getIncomePerSecond() * 0.75;
                    double gained = rate * offlineSec;
                    if (gained > 5.0) {
                        balance += gained;
                        int mins = (int) (offlineSec / 60);
                        addLog("💤 С возвращением! Офлайн доход: +$" + (int) gained + " (" + mins + " мин)");
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public float getTotalOutputKW() {
        float aeroMult = ResearchItem.BLADE_AERO.unlocked ? 1.2f : 1.0f;
        float total = 0.0f;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                TurbinePlot p = plots[r][c];
                float kw = p.getOutputKW(windSpeed, aeroMult);
                // Summer Solar Hybrid buff x1.50
                if (currentSeason == Season.SUMMER && p.type == TurbineType.HYBRID) {
                    kw *= 1.50f;
                }
                total += kw;
            }
        }
        if (techOrbitalLink) {
            total += 400.0f;
        }
        return total;
    }

    public double getIncomePerSecond() {
        float kw = getTotalOutputKW();
        float price = marketPrice * (ResearchItem.GENERATOR.unlocked ? 1.25f : 1.0f);
        if (policyGreenTariff) {
            price *= 1.25f;
        }

        // Winter Heating Demand Bonus x1.35
        if (currentSeason == Season.WINTER) {
            price *= 1.35f;
        }

        double income = (kw * price * 0.1) * prestigeMultiplier * currentBiome.incomeModifier;

        // Offshore in Ocean biome bonus +50%
        if (currentBiome == Biome.OCEAN) {
            int offshoreCount = 0;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (plots[r][c].type == TurbineType.OFFSHORE && !plots[r][c].isBroken && !plots[r][c].isBurning) {
                        offshoreCount++;
                    }
                }
            }
            if (offshoreCount > 0) {
                income *= (1.0 + offshoreCount * 0.15);
            }
        }

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

        // Seasons cycle (every 60 seconds)
        seasonTimer += dt;
        if (seasonTimer >= 60.0f) {
            seasonTimer = 0.0f;
            currentSeason = currentSeason.next();
            addLog("📅 Смена сезона: Наступила " + currentSeason.title + "! " + currentSeason.bonusDesc);
            sound.playSound("upgrade");
        }

        // Weather & Wind fluctuation
        if (Math.random() < 0.02) {
            float targetWind = windSpeed + (float) ((Math.random() - 0.5) * 2.0);
            targetWind *= currentBiome.windModifier * currentSeason.windMult;
            windSpeed = Math.max(1.5f, Math.min(26.0f, targetWind));

            if (windSpeed < 3.5f) weather = "Штиль";
            else if (windSpeed < 8.0f) weather = "Бриз";
            else if (windSpeed < 14.0f) weather = "Свежий ветер";
            else if (windSpeed < 19.0f) weather = "Сильный ветер";
            else weather = "Шторм";
        }

        // Forecast timer (Meteorologist)
        forecastSeconds--;
        if (forecastSeconds <= 0) {
            forecastSeconds = 60;
            String[] weathers = {"Штиль", "Бриз", "Свежий ветер", "Сильный ветер", "Шторм"};
            weatherForecast = weathers[(int) (Math.random() * weathers.length)];
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

        // AI-Broker auto trade during peak prices (>= $0.40)
        if (ResearchItem.AI_BROKER.unlocked && marketPrice >= 0.40f && Math.random() < 0.04) {
            double brokerGain = 45.0 * marketPrice * (1.0 + prestigeMultiplier * 0.2);
            balance += brokerGain;
            addLog("📈 AI-Брокер сбросил энергию на пике: +$" + (int) brokerGain);
            sound.playSound("coin");
        }

        // Update plots
        boolean autoDrones = ResearchItem.AUTO_DRONES.unlocked;
        float wearMultiplier = currentSeason.wearMult;
        if (ResearchItem.CHIEF_MECHANIC.unlocked) {
            wearMultiplier *= 0.70f; // -30% wear
        }
        if (policyDurability) {
            wearMultiplier *= 0.70f; // ESG durability policy
        }

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                TurbinePlot p = plots[r][c];
                boolean wasBroken = p.isBroken;
                p.update(windSpeed, autoDrones, hasTechSupport, wearMultiplier);

                // Chief Mechanic breakdown rescue
                if (!wasBroken && p.isBroken && ResearchItem.CHIEF_MECHANIC.unlocked) {
                    if (Math.random() < 0.35) {
                        p.condition = 45.0f;
                        p.isBroken = false;
                        p.isBurning = false;
                        addLog("🔧 Главный механик спас ветряк [" + (r + 1) + "," + (c + 1) + "] от аварии!");
                        sound.playSound("repair");
                    }
                }
            }
        }

        // Generation & Income
        double incomeThisTick = getIncomePerSecond() * dt;
        balance += incomeThisTick;

        // Science generation (0.1 SP/s per active turbine)
        int activeCount = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (!plots[r][c].isEmpty() && !plots[r][c].isBroken && !plots[r][c].isBurning) activeCount++;
            }
        }
        float spGain = activeCount * 0.08f * dt;
        if (policyScience) spGain *= 2.0f;
        sciencePoints += spGain;

        // Carbon Credits & Hydrogen
        float kwTotal = getTotalOutputKW();
        carbonAccumulator += (kwTotal / 3600.0f) * dt;
        if (carbonAccumulator >= 1000.0f) {
            int newCC = (int)(carbonAccumulator / 1000.0f);
            carbonCredits += newCC;
            carbonAccumulator -= newCC * 1000.0f;
            addLog("🌿 ESG: +" + newCC + " Углеродных Кредитов (CC)!");
            floatingTexts.add(new FloatingText(300, 200, "+" + newCC + " CC", 0xFF2ECC71));
        }

        if (techHydrogenUnlocked && kwTotal > 0) {
            float effH2Cap = hydrogenCapacity * (policyBattery ? 1.5f : 1.0f);
            float h2Gain = (0.35f + (kwTotal / 5000.0f) * 0.45f) * dt;
            if (hydrogenStored < effH2Cap) {
                hydrogenStored = Math.min(effH2Cap, hydrogenStored + h2Gain);
            }
        }

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

        // Tapping on burning turbine extinguishes it!
        if (plot.isBurning) {
            plot.extinguish();
            sound.playSound("repair");
            floatingTexts.add(new FloatingText(screenX, screenY, "💧 ПОТУШЕНО!", 0xFF00B4D8));
            addLog("🧯 Пожар на участке [" + (r + 1) + "," + (c + 1) + "] успешно ликвидирован!");
            return;
        }

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
            plot.isBurning = false;
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
        if (plot.isEmpty() || (plot.condition >= 100.0f && !plot.isBurning)) return false;

        double cost = plot.getRepairCost();
        if (balance >= cost) {
            balance -= cost;
            plot.repair();
            sound.playSound("repair");
            return true;
        }
        return false;
    }

    // Repair All with 15% discount
    public int repairAllTurbines() {
        double rawCost = 0.0;
        int count = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                TurbinePlot p = plots[r][c];
                if (!p.isEmpty() && (p.condition < 100.0f || p.isBroken || p.isBurning)) {
                    rawCost += p.getRepairCost();
                    count++;
                }
            }
        }
        if (count == 0) return 0;

        double finalCost = rawCost * 0.85; // 15% wholesale discount
        if (balance >= finalCost) {
            balance -= finalCost;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    plots[r][c].repair();
                }
            }
            addLog("🛠️ Отремонтированы все турбины (" + count + " шт.) за $" + (int) finalCost + " (скидка 15%)!");
            sound.playSound("repair");
            return count;
        }
        return -1; // insufficient funds
    }

    // Extinguish all fires ($500 per fire)
    public int extinguishAllFires() {
        int count = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (plots[r][c].isBurning) count++;
            }
        }
        if (count == 0) return 0;

        double cost = count * 500.0;
        if (balance >= cost) {
            balance -= cost;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    plots[r][c].extinguish();
                }
            }
            addLog("🧯 Потушены все пожары (" + count + " шт.) за $" + (int) cost + "!");
            sound.playSound("repair");
            return count;
        }
        return -1;
    }

    public boolean selectBiome(Biome b) {
        int idx = b.ordinal();
        if (unlockedBiomes[idx]) {
            currentBiome = b;
            addLog("📍 Переход в биом: " + b.title);
            sound.playSound("upgrade");
            return true;
        }
        if (balance >= b.unlockCost) {
            balance -= b.unlockCost;
            unlockedBiomes[idx] = true;
            currentBiome = b;
            addLog("🎉 Открыт новый биом: " + b.title + "! " + b.description);
            sound.playSound("evolve");
            return true;
        }
        return false;
    }

    public boolean unlockResearch(ResearchItem item) {
        if (item.unlocked) return false;
        if (sciencePoints >= item.costSP) {
            sciencePoints -= item.costSP;
            item.unlocked = true;
            if (item == ResearchItem.HYDROGEN_PLANT) {
                techHydrogenUnlocked = true;
            } else if (item == ResearchItem.ORBITAL_BEAM) {
                techOrbitalLink = true;
            }
            addLog("🔬 Исследование завершено: " + item.title);
            sound.playSound("coin");
            saveState();
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

        addLog("👑 ПРЕСТИЖ R" + prestigeCount + "! Множитель x" + String.format(Locale.US, "%.1f", prestigeMultiplier));
        sound.playSound("rebirth");
        return true;
    }

    public String exportSaveCode() {
        saveState();
        StringBuilder sb = new StringBuilder();
        sb.append((long) balance).append(";");
        sb.append((int) sciencePoints).append(";");
        sb.append(prestigeCount).append(";");
        sb.append(currentBiome.name()).append(";");
        for (int i = 0; i < unlockedBiomes.length; i++) {
            sb.append(unlockedBiomes[i] ? "1" : "0").append(",");
        }
        sb.append(";");
        for (ResearchItem ri : ResearchItem.values()) {
            sb.append(ri.unlocked ? "1" : "0").append(",");
        }
        sb.append(";");
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                TurbinePlot p = plots[r][c];
                sb.append(p.type != null ? p.type.name() : "NONE").append(":")
                  .append(p.level).append(":")
                  .append((int) p.condition).append("|");
            }
        }
        return Base64.encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    public boolean importSaveCode(String code) {
        try {
            byte[] data = Base64.decode(code.trim(), Base64.DEFAULT);
            String raw = new String(data, StandardCharsets.UTF_8);
            String[] parts = raw.split(";");
            if (parts.length >= 6) {
                balance = Double.parseDouble(parts[0]);
                sciencePoints = Float.parseFloat(parts[1]);
                prestigeCount = Integer.parseInt(parts[2]);
                prestigeMultiplier = 1.0 + (prestigeCount * 0.5);
                currentBiome = Biome.valueOf(parts[3]);

                String[] bioArr = parts[4].split(",");
                for (int i = 0; i < Math.min(bioArr.length, unlockedBiomes.length); i++) {
                    unlockedBiomes[i] = "1".equals(bioArr[i]);
                }

                String[] resArr = parts[5].split(",");
                ResearchItem[] rItems = ResearchItem.values();
                for (int i = 0; i < Math.min(resArr.length, rItems.length); i++) {
                    rItems[i].unlocked = "1".equals(resArr[i]);
                }

                if (parts.length >= 7) {
                    String[] plotData = parts[6].split("\\|");
                    int idx = 0;
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            if (idx < plotData.length && !plotData[idx].isEmpty()) {
                                String[] f = plotData[idx].split(":");
                                if (!"NONE".equals(f[0])) {
                                    plots[r][c].type = TurbineType.valueOf(f[0]);
                                    plots[r][c].level = Integer.parseInt(f[1]);
                                    plots[r][c].condition = Float.parseFloat(f[2]);
                                    plots[r][c].isBroken = plots[r][c].condition <= 0;
                                    plots[r][c].isBurning = false;
                                } else {
                                    plots[r][c].type = null;
                                }
                            }
                            idx++;
                        }
                    }
                }
                addLog("📥 Сохранение успешно загружено из кода!");
                saveState();
                return true;
            }
        } catch (Exception ex) {
            addLog("⚠️ Ошибка чтения кода сохранения");
        }
        return false;
    }

    public void sellHydrogen() {
        if (!techHydrogenUnlocked || hydrogenStored <= 0.05f) return;
        double payout = hydrogenStored * 2.40;
        balance += payout;
        addLog("💧 Продано " + String.format(Locale.US, "%.1f", hydrogenStored) + " кг H₂ за +$" + (int) payout);
        floatingTexts.add(new FloatingText(300, 300, "+$" + (int) payout, 0xFF2ECC71));
        sound.playSound("coin");
        hydrogenStored = 0.0f;
    }

    public boolean buyPolicy(String policy) {
        if ("TARIFF".equals(policy)) {
            if (policyGreenTariff || carbonCredits < 50) return false;
            carbonCredits -= 50;
            policyGreenTariff = true;
            addLog("🌿 Принята политика: Зеленый тариф (+25% к ценам)!");
            sound.playSound("upgrade");
            return true;
        } else if ("DURABILITY".equals(policy)) {
            if (policyDurability || carbonCredits < 40) return false;
            carbonCredits -= 40;
            policyDurability = true;
            addLog("🛡️ Принята политика: Эко-Надёжность (-30% износ)!");
            sound.playSound("upgrade");
            return true;
        } else if ("SCIENCE".equals(policy)) {
            if (policyScience || carbonCredits < 60) return false;
            carbonCredits -= 60;
            policyScience = true;
            addLog("🔬 Принята политика: Муниципальный грант (x2 SP)!");
            sound.playSound("upgrade");
            return true;
        } else if ("BATTERY".equals(policy)) {
            if (policyBattery || carbonCredits < 80) return false;
            carbonCredits -= 80;
            policyBattery = true;
            addLog("🔋 Принята политика: Стратегический энергорезерв (+50% баки)!");
            sound.playSound("upgrade");
            return true;
        }
        return false;
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
            ed.putLong("lastSaveTimestamp", System.currentTimeMillis());
            ed.putString("currentBiome", currentBiome.name());
            ed.putString("currentSeason", currentSeason.name());

            ed.putFloat("hydrogenStored", hydrogenStored);
            ed.putFloat("hydrogenCapacity", hydrogenCapacity);
            ed.putBoolean("techHydrogenUnlocked", techHydrogenUnlocked);
            ed.putBoolean("techOrbitalLink", techOrbitalLink);
            ed.putInt("carbonCredits", carbonCredits);
            ed.putFloat("carbonAccumulator", carbonAccumulator);
            ed.putBoolean("policyGreenTariff", policyGreenTariff);
            ed.putBoolean("policyDurability", policyDurability);
            ed.putBoolean("policyScience", policyScience);
            ed.putBoolean("policyBattery", policyBattery);

            for (int i = 0; i < unlockedBiomes.length; i++) {
                ed.putBoolean("bio_" + i, unlockedBiomes[i]);
            }

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
                    ed.putBoolean(prefix + "burning", p.isBurning);
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

            hydrogenStored = prefs.getFloat("hydrogenStored", 0.0f);
            hydrogenCapacity = prefs.getFloat("hydrogenCapacity", 500.0f);
            techHydrogenUnlocked = prefs.getBoolean("techHydrogenUnlocked", false);
            techOrbitalLink = prefs.getBoolean("techOrbitalLink", false);
            carbonCredits = prefs.getInt("carbonCredits", 0);
            carbonAccumulator = prefs.getFloat("carbonAccumulator", 0.0f);
            policyGreenTariff = prefs.getBoolean("policyGreenTariff", false);
            policyDurability = prefs.getBoolean("policyDurability", false);
            policyScience = prefs.getBoolean("policyScience", false);
            policyBattery = prefs.getBoolean("policyBattery", false);

            String bName = prefs.getString("currentBiome", "VALLEY");
            try { currentBiome = Biome.valueOf(bName); } catch (Exception e) { currentBiome = Biome.VALLEY; }

            String sName = prefs.getString("currentSeason", "SPRING");
            try { currentSeason = Season.valueOf(sName); } catch (Exception e) { currentSeason = Season.SPRING; }

            for (int i = 0; i < unlockedBiomes.length; i++) {
                unlockedBiomes[i] = prefs.getBoolean("bio_" + i, i == 0);
            }

            for (ResearchItem item : ResearchItem.values()) {
                item.unlocked = prefs.getBoolean("res_" + item.name(), false);
            }
            if (ResearchItem.HYDROGEN_PLANT.unlocked) techHydrogenUnlocked = true;
            if (ResearchItem.ORBITAL_BEAM.unlocked) techOrbitalLink = true;

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    String prefix = "p_" + r + "_" + c + "_";
                    String typeName = prefs.getString(prefix + "type", "");
                    if (!typeName.isEmpty()) {
                        try {
                            plots[r][c].type = TurbineType.valueOf(typeName);
                            plots[r][c].level = prefs.getInt(prefix + "level", 1);
                            plots[r][c].condition = prefs.getFloat(prefix + "condition", 100.0f);
                            plots[r][c].isBurning = prefs.getBoolean(prefix + "burning", false);
                            plots[r][c].isBroken = plots[r][c].condition <= 0.0f;
                        } catch (Exception ignored) {}
                    } else {
                        plots[r][c].type = null;
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
