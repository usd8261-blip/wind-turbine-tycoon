package com.usd8261.windtycoon;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TycoonGameView extends View {

    public enum Tab { BUILD, MARKET, SCIENCE, PRESTIGE, LOG }

    private static class SnowFlake {
        float x, y, speed, size;
        SnowFlake(float x, float y, float speed, float size) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
        }
    }

    private final GameEngine engine;
    private final SoundManager sound;
    private Tab currentTab = Tab.BUILD;

    private int selectedRow = 0;
    private int selectedCol = 0;

    // Paints
    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectTemp = new RectF();

    // Clouds & Snow
    private final float[] cloudX = {40, 220, 480};
    private final float[] cloudY = {80, 140, 95};
    private final float[] cloudSpeed = {0.35f, 0.2f, 0.45f};
    private final List<SnowFlake> snowFlakes = new ArrayList<>();

    public TycoonGameView(Context context, GameEngine engine) {
        super(context);
        this.engine = engine;
        this.sound = SoundManager.getInstance(context);
        pStroke.setStyle(Paint.Style.STROKE);

        for (int i = 0; i < 45; i++) {
            snowFlakes.add(new SnowFlake(
                    (float) (Math.random() * 800),
                    (float) (Math.random() * 1200),
                    1.2f + (float) (Math.random() * 2.5),
                    2.0f + (float) (Math.random() * 3.0)
            ));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        engine.update();

        // 1. Sky & Sun/Moon & Clouds & Biome Horizon
        drawSkyAndEnvironment(canvas, w, h);

        // 2. Top HUD
        float hudBottom = drawTopHUD(canvas, w);

        // 3. Main Field (3x3 Grid)
        float gridTop = hudBottom + 10;
        float tabAreaHeight = h * 0.42f;
        float gridHeight = h - gridTop - tabAreaHeight;
        drawTurbineGrid(canvas, w, gridTop, gridHeight);

        // 4. Floating Particles
        drawFloatingParticles(canvas);

        // 5. Snow Particles in Winter
        if (engine.currentSeason == Season.WINTER) {
            pFill.setColor(0xDDFFFFFF);
            for (SnowFlake sf : snowFlakes) {
                sf.y += sf.speed;
                if (sf.y > h) {
                    sf.y = -10;
                    sf.x = (float) (Math.random() * w);
                }
                canvas.drawCircle(sf.x, sf.y, sf.size, pFill);
            }
        }

        // 6. Bottom Tabs & Drawer Panel
        drawBottomPanel(canvas, w, h, h - tabAreaHeight, tabAreaHeight);

        // 7. Bottom Event Log Bar
        drawEventLogTicker(canvas, w, h);

        // Continuous 60 FPS animation loop
        postInvalidateOnAnimation();
    }

    private void drawSkyAndEnvironment(Canvas canvas, int w, int h) {
        // Sky gradient based on time of day
        int topSky, bottomSky;
        float t = engine.timeOfDay;
        if (t < 0.25f) { // Night -> Dawn
            topSky = 0xFF080C16;
            bottomSky = 0xFF1E1B4B;
        } else if (t < 0.35f) { // Dawn -> Sunrise
            topSky = 0xFF1E293B;
            bottomSky = 0xFFFB923C;
        } else if (t < 0.70f) { // Day
            topSky = 0xFF0284C7;
            bottomSky = 0xFF38BDF8;
        } else if (t < 0.82f) { // Sunset
            topSky = 0xFF4338CA;
            bottomSky = 0xFFF43F5E;
        } else { // Dusk -> Night
            topSky = 0xFF0F172A;
            bottomSky = 0xFF1E1B4B;
        }

        LinearGradient skyGrad = new LinearGradient(0, 0, 0, h * 0.5f, topSky, bottomSky, Shader.TileMode.CLAMP);
        pFill.setShader(skyGrad);
        canvas.drawRect(0, 0, w, h, pFill);
        pFill.setShader(null);

        // Clouds
        pFill.setColor(0x40FFFFFF);
        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] = (cloudX[i] + cloudSpeed[i]) % (w + 120);
            float cx = cloudX[i] - 60;
            float cy = cloudY[i];
            canvas.drawCircle(cx, cy, 32, pFill);
            canvas.drawCircle(cx + 25, cy - 8, 38, pFill);
            canvas.drawCircle(cx + 50, cy, 28, pFill);
        }

        // Biome Horizon Ground
        float horizonY = h * 0.52f;
        int groundTop = engine.currentSeason.groundTop;
        int groundBot = engine.currentSeason.groundBot;

        if (engine.currentBiome == Biome.OCEAN) {
            groundTop = 0xFF0077B6;
            groundBot = 0xFF03045E;
        } else if (engine.currentBiome == Biome.PEAKS) {
            if (engine.currentSeason == Season.WINTER) {
                groundTop = 0xFFE2E8F0;
                groundBot = 0xFF94A3B8;
            } else {
                groundTop = 0xFF475569;
                groundBot = 0xFF1E293B;
            }
        }

        LinearGradient gGrad = new LinearGradient(0, horizonY, 0, h, groundTop, groundBot, Shader.TileMode.CLAMP);
        pFill.setShader(gGrad);
        canvas.drawRect(0, horizonY, w, h, pFill);
        pFill.setShader(null);

        // Ocean Waves
        if (engine.currentBiome == Biome.OCEAN) {
            pStroke.setColor(0x35FFFFFF);
            pStroke.setStrokeWidth(1.5f);
            long tm = System.currentTimeMillis();
            for (float wy = horizonY + 20; wy < h * 0.65f; wy += 28) {
                float shift = (float) (((tm / 30 + wy * 4) % 120) - 60);
                for (float wx = shift - 80; wx < w + 80; wx += 60) {
                    rectTemp.set(wx, wy, wx + 40, wy + 16);
                    canvas.drawArc(rectTemp, 180, 180, false, pStroke);
                }
            }
        }
    }

    private float drawTopHUD(Canvas canvas, int w) {
        float startY = 30;
        float rowH = 34;
        float pad = 8;
        float colW = (w - pad * 3 - 48) / 2.0f;

        // Row 1: Balance & Output
        drawPill(canvas, pad, startY, colW, rowH, 0xDD111827, 0xFF10B981,
                "💰 $" + formatNumber(engine.balance), 0xFF10B981, 15, true);

        double inc = engine.getIncomePerSecond();
        String netStr = String.format(Locale.US, "⚡ %.0f кВт (+%.1f$/с)", engine.getTotalOutputKW(), inc);
        drawPill(canvas, pad * 2 + colW, startY, colW, rowH, 0xDD111827, 0xFF3B82F6,
                netStr, 0xFF60A5FA, 12, true);

        // Audio Toggle
        float audioX = pad * 2 + colW * 2 + 6;
        drawPill(canvas, audioX, startY, 42, rowH, 0xDD111827, 0xFF64748B,
                sound.isSoundEnabled() ? "🔊" : "🔇", 0xFFFFFFFF, 15, true);

        // Row 2: Wind, SP, Season, Biome
        float r2Y = startY + rowH + 5;
        float subW = (w - pad * 5) / 4.0f;

        drawPill(canvas, pad, r2Y, subW, 26, 0xBB111827, 0,
                String.format(Locale.US, "💨 %.1fм/с", engine.windSpeed), 0xFF38BDF8, 10, false);

        drawPill(canvas, pad * 2 + subW, r2Y, subW, 26, 0xBB111827, 0,
                String.format(Locale.US, "🔬 %.1f SP", engine.sciencePoints), 0xFFA78BFA, 10, false);

        drawPill(canvas, pad * 3 + subW * 2, r2Y, subW, 26, 0xBB111827, 0,
                engine.currentSeason.title, 0xFFFBBF24, 10, true);

        drawPill(canvas, pad * 4 + subW * 3, r2Y, subW, 26, 0xBB111827, 0,
                engine.currentBiome.title, engine.currentBiome.color, 10, true);

        return r2Y + 26;
    }

    private void drawTurbineGrid(Canvas canvas, int w, float topY, float gridH) {
        float pad = 8;
        float cellW = (w - pad * 4) / 3.0f;
        float cellH = (gridH - pad * 4) / 3.0f;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                float x = pad + c * (cellW + pad);
                float y = topY + pad + r * (cellH + pad);
                boolean isSelected = (r == selectedRow && c == selectedCol);

                drawPlotCell(canvas, r, c, x, y, cellW, cellH, isSelected);
            }
        }
    }

    private void drawPlotCell(Canvas canvas, int r, int c, float x, float y, float w, float h, boolean selected) {
        TurbinePlot plot = engine.plots[r][c];

        // Background Pad depending on Biome
        rectTemp.set(x, y, x + w, y + h);
        int padBg = selected ? 0xFF065F46 : 0xFF047857;
        if (engine.currentBiome == Biome.OCEAN) {
            padBg = selected ? 0xFF0E7490 : 0xFF155E75;
        } else if (engine.currentBiome == Biome.PEAKS) {
            padBg = selected ? 0xFF334155 : 0xFF1E293B;
        } else if (engine.currentSeason == Season.WINTER) {
            padBg = selected ? 0xFF94A3B8 : 0xFFCBD5E1;
        }

        pFill.setColor(padBg);
        canvas.drawRoundRect(rectTemp, 14, 14, pFill);

        // Border (Flashing Red if Burning!)
        if (plot.isBurning) {
            long pt = System.currentTimeMillis();
            pStroke.setColor(0xFFEF4444);
            pStroke.setStrokeWidth(3.5f);
        } else {
            pStroke.setColor(selected ? 0xFF34D399 : 0x40FFFFFF);
            pStroke.setStrokeWidth(selected ? 3.0f : 1.0f);
        }
        canvas.drawRoundRect(rectTemp, 14, 14, pStroke);

        if (plot.isEmpty()) {
            pText.setColor(0xEEFFFFFF);
            pText.setTextSize(13);
            pText.setTextAlign(Paint.Align.CENTER);
            String buildTxt = (engine.currentBiome == Biome.OCEAN) ? "➕ ШЕЛЬФ" : "➕ СТРОЙКА";
            canvas.drawText(buildTxt, x + w / 2, y + h / 2 - 2, pText);

            pText.setColor(0xFF34D399);
            pText.setTextSize(11);
            canvas.drawText("от $1,000", x + w / 2, y + h / 2 + 16, pText);
            return;
        }

        // Active Turbine drawing
        float hubX = x + w / 2;
        float hubY = y + h * 0.44f;
        float baseH = y + h - 8;

        // Offshore Marine Foundation / Jacket
        if (plot.type == TurbineType.OFFSHORE || engine.currentBiome == Biome.OCEAN) {
            pFill.setColor(0xFFFBBF24); // Steel yellow jacket
            canvas.drawRect(hubX - 18, baseH - 4, hubX + 18, baseH + 2, pFill);
            pStroke.setColor(0xFFD97706);
            pStroke.setStrokeWidth(2.0f);
            canvas.drawLine(hubX - 14, baseH, hubX - 8, baseH + 6, pStroke);
            canvas.drawLine(hubX + 14, baseH, hubX + 8, baseH + 6, pStroke);
        } else {
            pFill.setColor(0x30000000);
            canvas.drawOval(hubX - 20, baseH - 3, hubX + 20, baseH + 4, pFill);
        }

        // Tower
        pFill.setColor(0xFFE2E8F0);
        Path tower = new Path();
        tower.moveTo(hubX - 3.5f, hubY);
        tower.lineTo(hubX + 3.5f, hubY);
        tower.lineTo(hubX + 7.0f, baseH);
        tower.lineTo(hubX - 7.0f, baseH);
        tower.close();
        canvas.drawPath(tower, pFill);

        // Nacelle
        pFill.setColor(plot.type.color);
        rectTemp.set(hubX - 6, hubY - 5, hubX + 10, hubY + 5);
        canvas.drawRoundRect(rectTemp, 3, 3, pFill);

        // Hub & Blades (with rotation)
        canvas.save();
        canvas.translate(hubX, hubY);
        canvas.rotate(plot.bladeAngle);

        float bladeLen = Math.min(w, h) * 0.38f;
        pFill.setColor(0xFFFFFFFF);

        if (plot.type == TurbineType.QUANTUM_VAWT) {
            pFill.setColor(plot.type.color);
            for (int i = 0; i < 4; i++) {
                canvas.save();
                canvas.rotate(i * 90);
                rectTemp.set(-4, -bladeLen, 4, 0);
                canvas.drawRoundRect(rectTemp, 3, 3, pFill);
                canvas.restore();
            }
        } else {
            for (int i = 0; i < 3; i++) {
                canvas.save();
                canvas.rotate(i * 120);

                Path blade = new Path();
                blade.moveTo(-2, 0);
                blade.lineTo(-1, -bladeLen * 0.8f);
                blade.lineTo(0, -bladeLen);
                blade.lineTo(2.5f, -bladeLen * 0.7f);
                blade.lineTo(1.5f, 0);
                blade.close();
                canvas.drawPath(blade, pFill);

                pFill.setColor(plot.type.color);
                canvas.drawCircle(0, -bladeLen * 0.92f, 2.2f, pFill);
                pFill.setColor(0xFFFFFFFF);

                canvas.restore();
            }
        }

        // Center cap
        pFill.setColor(0xFF1E293B);
        canvas.drawCircle(0, 0, 4.5f, pFill);
        canvas.restore();

        // Level pill (top left)
        drawMiniBadge(canvas, x + 6, y + 6, "Lv." + plot.level, 0xFF1E293B, 0xFF60A5FA);

        // Health bar
        float barW = w - 16;
        float barH = 5;
        float barX = x + 8;
        float barY = y + h - 10;
        rectTemp.set(barX, barY, barX + barW, barY + barH);
        pFill.setColor(0x80000000);
        canvas.drawRoundRect(rectTemp, 3, 3, pFill);

        float hpW = barW * (plot.condition / 100.0f);
        rectTemp.set(barX, barY, barX + hpW, barY + barH);
        pFill.setColor(plot.condition > 50 ? 0xFF10B981 : (plot.condition > 20 ? 0xFFF59E0B : 0xFFEF4444));
        canvas.drawRoundRect(rectTemp, 3, 3, pFill);

        // Warnings: Broken or Fire
        if (plot.isBurning) {
            pFill.setColor(0xCCEF4444);
            canvas.drawCircle(hubX, hubY, 15, pFill);
            pFill.setColor(0xFFFBBF24);
            canvas.drawCircle(hubX, hubY, 8, pFill);
            drawMiniBadge(canvas, x + w - 52, y + 6, "🔥 ПОЖАР!", 0xFFB91C1C, 0xFFFEE2E2);
        } else if (plot.isBroken) {
            drawMiniBadge(canvas, x + w - 50, y + 6, "⚠️ СЛОМАН", 0xFF991B1B, 0xFFFCA5A5);
        }
    }

    private void drawBottomPanel(Canvas canvas, int w, int h, float panelY, float panelH) {
        rectTemp.set(0, panelY, w, h);
        pFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(rectTemp, 20, 20, pFill);

        // Tabs Header
        float tabH = 38;
        float tabW = w / 5.0f;
        Tab[] tabs = Tab.values();
        String[] tabNames = {"🏗️ Стройка", "📈 Биржа", "🔬 Штаб", "👑 Престиж", "📋 Журнал"};

        for (int i = 0; i < tabs.length; i++) {
            boolean active = (tabs[i] == currentTab);
            float tx = i * tabW;

            rectTemp.set(tx + 2, panelY + 3, tx + tabW - 2, panelY + tabH - 2);
            pFill.setColor(active ? 0xFF1E293B : 0x00000000);
            canvas.drawRoundRect(rectTemp, 8, 8, pFill);

            if (active) {
                pStroke.setColor(0xFF38BDF8);
                pStroke.setStrokeWidth(2.0f);
                canvas.drawRoundRect(rectTemp, 8, 8, pStroke);
            }

            pText.setColor(active ? 0xFF38BDF8 : 0xFF94A3B8);
            pText.setTextSize(w < 420 ? 10 : 11);
            pText.setTextAlign(Paint.Align.CENTER);
            pText.setFakeBoldText(active);
            canvas.drawText(tabNames[i], tx + tabW / 2, panelY + tabH / 2 + 4, pText);
            pText.setFakeBoldText(false);
        }

        // Tab Content
        float contentY = panelY + tabH + 4;
        float contentH = panelH - tabH - 30;

        switch (currentTab) {
            case BUILD:
                drawBuildTab(canvas, w, contentY, contentH);
                break;
            case MARKET:
                drawMarketTab(canvas, w, contentY, contentH);
                break;
            case SCIENCE:
                drawScienceTab(canvas, w, contentY, contentH);
                break;
            case PRESTIGE:
                drawPrestigeTab(canvas, w, contentY, contentH);
                break;
            case LOG:
                drawLogTab(canvas, w, contentY, contentH);
                break;
        }
    }

    private void drawBuildTab(Canvas canvas, int w, float y, float h) {
        TurbinePlot plot = engine.plots[selectedRow][selectedCol];
        float pad = 10;

        // Top Row: Biome Bar (Valley, Peaks, Ocean)
        float bioH = 26;
        float bioW = (w - pad * 4) / 3.0f;
        Biome[] biomes = Biome.values();
        for (int i = 0; i < biomes.length; i++) {
            Biome b = biomes[i];
            float bx = pad + i * (bioW + pad);
            boolean isCur = (engine.currentBiome == b);
            boolean isUnlocked = engine.unlockedBiomes[i];

            String bTitle = b.title.split(" ")[0];
            String bSub = isCur ? "АКТИВЕН" : (isUnlocked ? "ВЫБРАТЬ" : "$" + (int) (b.unlockCost / 1000) + "k");
            drawActionButton(canvas, bx, y, bioW, bioH, bTitle + " (" + bSub + ")", "",
                    isCur ? 0xFF0284C7 : (isUnlocked ? 0xFF1E293B : 0xFF334155),
                    isCur ? 0xFF38BDF8 : 0xFF64748B);
        }

        float afterBioY = y + bioH + 6;

        if (plot.isEmpty()) {
            pText.setColor(0xFFE2E8F0);
            pText.setTextSize(13);
            pText.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Постройка на участке [" + (selectedRow + 1) + "," + (selectedCol + 1) + "]:", pad, afterBioY + 14, pText);

            float btnY = afterBioY + 20;
            float btnH = (h - bioH - 26) / 3.0f;
            float btnW = (w - pad * 3) / 2.0f;

            TurbineType[] types = TurbineType.values();
            for (int i = 0; i < Math.min(6, types.length); i++) {
                int col = i % 2;
                int row = i / 2;
                float bx = pad + col * (btnW + pad);
                float by = btnY + row * (btnH + 3);

                TurbineType tt = types[i];
                boolean canAfford = engine.balance >= tt.baseCost;
                drawActionButton(canvas, bx, by, btnW, btnH - 3,
                        tt.title + " ($" + (int) tt.baseCost + ")",
                        String.format(Locale.US, "+%.0f кВт", tt.baseKW),
                        canAfford ? 0xFF059669 : 0xFF334155, canAfford ? 0xFF10B981 : 0xFF64748B);
            }
            return;
        }

        // Active Plot Actions
        pText.setColor(0xFFF8FAFC);
        pText.setTextSize(13);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setFakeBoldText(true);
        canvas.drawText(plot.type.title + " [Lv." + plot.level + "] (Участок " + (selectedRow + 1) + "," + (selectedCol + 1) + ")", pad, afterBioY + 12, pText);
        pText.setFakeBoldText(false);

        // Buttons Grid
        float bY = afterBioY + 20;
        float bH = 38;
        float bW = (w - pad * 3) / 2.0f;

        // Button 1: Upgrade Level
        boolean canUp = (plot.level < 10) && (engine.balance >= plot.getUpgradeCost());
        String upTitle = (plot.level >= 10) ? "МАКС УРОВЕНЬ" : "Улучшить Lv." + (plot.level + 1);
        String upSub = (plot.level >= 10) ? "Максимум" : "$" + formatNumber(plot.getUpgradeCost());
        drawActionButton(canvas, pad, bY, bW, bH, upTitle, upSub,
                canUp ? 0xFF2563EB : 0xFF1E293B, canUp ? 0xFF3B82F6 : 0xFF475569);

        // Button 2: Evolution
        TurbineType next = plot.getNextEvolutionType();
        boolean canEvolve = plot.canEvolve() && (engine.balance >= plot.getEvolutionCost());
        String evoTitle = (next != null) ? "⚡ Эволюция: " + next.title : "ФИНАЛЬНАЯ МОДЕЛЬ";
        String evoSub = (next != null) ? "$" + formatNumber(plot.getEvolutionCost()) : "Высший тир";
        drawActionButton(canvas, pad * 2 + bW, bY, bW, bH, evoTitle, evoSub,
                canEvolve ? 0xFFD97706 : 0xFF1E293B, canEvolve ? 0xFFF59E0B : 0xFF475569);

        // Row 2: Repair Single & Repair ALL (-15%)
        float b2Y = bY + bH + 6;
        boolean canRepair = (plot.condition < 100.0f || plot.isBurning) && (engine.balance >= plot.getRepairCost());
        String repTitle = plot.isBurning ? "🧯 Потушить!" : (plot.condition >= 100.0f ? "Исправен" : "Починить");
        String repSub = "$" + formatNumber(plot.getRepairCost());
        drawActionButton(canvas, pad, b2Y, bW, bH, repTitle, repSub,
                canRepair ? 0xFF059669 : 0xFF1E293B, canRepair ? 0xFF10B981 : 0xFF475569);

        // Repair ALL with wholesale discount
        drawActionButton(canvas, pad * 2 + bW, b2Y, bW, bH, "🛠️ ПОЧИНИТЬ ВСЁ", "Скидка 15%",
                0xFF0D9488, 0xFF14B8A6);
    }

    private void drawMarketTab(Canvas canvas, int w, float y, float h) {
        float pad = 10;

        pText.setColor(0xFFF8FAFC);
        pText.setTextSize(14);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setFakeBoldText(true);
        canvas.drawText("⚡ Биржа электроэнергии & Метеосводка", pad, y + 14, pText);
        pText.setFakeBoldText(false);

        // Market Price & Forecast Card
        float cardY = y + 22;
        float cardH = 46;
        rectTemp.set(pad, cardY, w - pad, cardY + cardH);
        pFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(rectTemp, 10, 10, pFill);

        pText.setColor(0xFF38BDF8);
        pText.setTextSize(13);
        canvas.drawText(String.format(Locale.US, "Тариф: $%.3f/кВт·ч | Спрос: %s",
                engine.marketPrice, (engine.marketPrice > 0.4f ? "🔥 ПИК" : "Обычный")), pad + 10, cardY + 18, pText);

        pText.setColor(0xFFFBBF24);
        pText.setTextSize(11);
        String foreStr = ResearchItem.METEOROLOGIST.unlocked ?
                "📡 Прогноз: " + engine.weatherForecast + " (" + engine.forecastSeconds + "с)" :
                "📡 Назначьте Метеоролога в Штабе для прогноза";
        canvas.drawText(foreStr, pad + 10, cardY + 36, pText);

        // Contract Card
        float contY = cardY + cardH + 8;
        float contH = 75;
        rectTemp.set(pad, contY, w - pad, contY + contH);
        pFill.setColor(0xFF1E1B4B);
        canvas.drawRoundRect(rectTemp, 12, 12, pFill);
        pStroke.setColor(0xFF6366F1);
        pStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(rectTemp, 12, 12, pStroke);

        if (engine.currentContract != null) {
            Contract c = engine.currentContract;
            pText.setColor(0xFFC7D2FE);
            pText.setTextSize(12);
            pText.setFakeBoldText(true);
            canvas.drawText("📋 " + c.title, pad + 10, contY + 20, pText);
            pText.setFakeBoldText(false);

            pText.setColor(0xFFFBBF24);
            pText.setTextSize(11);
            canvas.drawText("Награда: +$" + (int) c.reward + " | Цель: " + (int) c.targetKW + " кВт", pad + 10, contY + 38, pText);

            float prgW = (w - pad * 2 - 20);
            float currentKW = engine.getTotalOutputKW();
            float ratio = Math.min(1.0f, currentKW / c.targetKW);

            rectTemp.set(pad + 10, contY + 48, pad + 10 + prgW, contY + 56);
            pFill.setColor(0x60000000);
            canvas.drawRoundRect(rectTemp, 4, 4, pFill);

            rectTemp.set(pad + 10, contY + 48, pad + 10 + prgW * ratio, contY + 56);
            pFill.setColor(0xFF10B981);
            canvas.drawRoundRect(rectTemp, 4, 4, pFill);

            pText.setColor(0xFFE2E8F0);
            pText.setTextSize(10);
            canvas.drawText(String.format(Locale.US, "%.0f / %.0f кВт (Осталось: %.0fc)", currentKW, c.targetKW, c.remainingSeconds), pad + 10, contY + 68, pText);
        }
    }

    private void drawScienceTab(Canvas canvas, int w, float y, float h) {
        float pad = 10;

        pText.setColor(0xFFF8FAFC);
        pText.setTextSize(14);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setFakeBoldText(true);
        canvas.drawText("🔬 Штаб Специалистов & Лаборатория (R&D)", pad, y + 14, pText);
        pText.setFakeBoldText(false);

        float itemY = y + 20;
        ResearchItem[] items = ResearchItem.values();
        float itemH = (h - 24) / Math.min(5, items.length);

        for (int i = 0; i < Math.min(5, items.length); i++) {
            ResearchItem res = items[i];
            float iy = itemY + i * itemH;

            rectTemp.set(pad, iy, w - pad, iy + itemH - 3);
            pFill.setColor(res.unlocked ? 0xFF064E3B : 0xFF1E293B);
            canvas.drawRoundRect(rectTemp, 8, 8, pFill);

            pText.setColor(res.unlocked ? 0xFF34D399 : 0xFFE2E8F0);
            pText.setTextSize(12);
            pText.setFakeBoldText(true);
            canvas.drawText(res.icon + " " + res.title, pad + 8, iy + 16, pText);
            pText.setFakeBoldText(false);

            pText.setColor(0xFF94A3B8);
            pText.setTextSize(10);
            canvas.drawText(res.description, pad + 8, iy + 30, pText);

            float btnW = 85;
            float btnH = itemH - 8;
            float btnX = w - pad - btnW - 4;
            float btnY = iy + 2;

            boolean canUnlock = (!res.unlocked) && (engine.sciencePoints >= res.costSP);
            String btnText = res.unlocked ? "АКТИВЕН" : (int) res.costSP + " SP";
            drawActionButton(canvas, btnX, btnY, btnW, btnH, btnText, "",
                    res.unlocked ? 0xFF047857 : (canUnlock ? 0xFF7C3AED : 0xFF334155),
                    res.unlocked ? 0xFF10B981 : (canUnlock ? 0xFFA78BFA : 0xFF64748B));
        }
    }

    private void drawPrestigeTab(Canvas canvas, int w, float y, float h) {
        float pad = 10;

        pText.setColor(0xFFFBBF24);
        pText.setTextSize(15);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setFakeBoldText(true);
        canvas.drawText("👑 Престиж / IPO & Сохранения", pad, y + 16, pText);
        pText.setFakeBoldText(false);

        // Rebirth Button
        boolean canRebirth = engine.balance >= 100000.0;
        float btnY = y + 26;
        float btnW = w - pad * 2;
        float btnH = 46;

        String title = canRebirth ? "👑 СОВЕРШИТЬ ПРЕСТИЖ (+50% ДОХОДА)" : "ТРЕБУЕТСЯ $100,000 ДЛЯ ПРЕСТИЖА";
        String sub = "Текущий бонус: x" + String.format(Locale.US, "%.1f", engine.prestigeMultiplier);
        drawActionButton(canvas, pad, btnY, btnW, btnH, title, sub,
                canRebirth ? 0xFFD97706 : 0xFF1E293B, canRebirth ? 0xFFF59E0B : 0xFF475569);

        // Export & Import Save Code Buttons
        float b2Y = btnY + btnH + 10;
        float halfW = (btnW - pad) / 2.0f;

        drawActionButton(canvas, pad, b2Y, halfW, 40, "📋 ЭКСПОРТ КОДА", "В буфер обмена",
                0xFF0284C7, 0xFF38BDF8);

        drawActionButton(canvas, pad + halfW + pad, b2Y, halfW, 40, "📥 ИМПОРТ ИЗ БУФЕРА", "Загрузить",
                0xFF7C3AED, 0xFFA78BFA);
    }

    private void drawLogTab(Canvas canvas, int w, float y, float h) {
        float pad = 10;
        float btnW = (w - pad * 3) / 2.0f;
        float btnH = 34;

        drawActionButton(canvas, pad, y, btnW, btnH, "🧹 ОЧИСТИТЬ", "Журнал", 0xFF991B1B, 0xFFEF4444);
        drawActionButton(canvas, pad * 2 + btnW, y, btnW, btnH, "📋 СКОПИРОВАТЬ", "Все события", 0xFF0369A1, 0xFF38BDF8);

        float listY = y + btnH + 8;
        float entryH = 24;
        int maxEntries = (int) ((h - btnH - 12) / entryH);

        for (int i = 0; i < Math.min(maxEntries, engine.eventLog.size()); i++) {
            float ey = listY + i * entryH;
            rectTemp.set(pad, ey, w - pad, ey + entryH - 3);
            pFill.setColor(i % 2 == 0 ? 0xFF1E293B : 0xFF0F172A);
            canvas.drawRoundRect(rectTemp, 5, 5, pFill);

            String logMsg = engine.eventLog.get(i);
            pText.setColor(0xFFF1F5F9);
            pText.setTextSize(10);
            pText.setTextAlign(Paint.Align.LEFT);
            pText.setFakeBoldText(false);

            String display = logMsg;
            float maxW = w - pad * 2 - 14;
            if (pText.measureText(display) > maxW) {
                while (display.length() > 6 && pText.measureText(display + "...") > maxW) {
                    display = display.substring(0, display.length() - 1);
                }
                display += "...";
            }
            canvas.drawText(display, pad + 8, ey + entryH * 0.65f, pText);
        }
    }

    private void drawActionButton(Canvas canvas, float x, float y, float w, float h, String title, String sub, int bg, int border) {
        rectTemp.set(x, y, x + w, y + h);
        pFill.setColor(bg);
        canvas.drawRoundRect(rectTemp, 8, 8, pFill);

        pStroke.setColor(border);
        pStroke.setStrokeWidth(1.2f);
        canvas.drawRoundRect(rectTemp, 8, 8, pStroke);

        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(11);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(true);

        if (sub.isEmpty()) {
            canvas.drawText(title, x + w / 2, y + h / 2 + 4, pText);
        } else {
            canvas.drawText(title, x + w / 2, y + h * 0.42f, pText);
            pText.setColor(0xFFCBD5E1);
            pText.setTextSize(9);
            pText.setFakeBoldText(false);
            canvas.drawText(sub, x + w / 2, y + h * 0.78f, pText);
        }
        pText.setFakeBoldText(false);
    }

    private void drawPill(Canvas canvas, float x, float y, float w, float h, int bg, int border, String text, int textColor, float textSize, boolean bold) {
        rectTemp.set(x, y, x + w, y + h);
        pFill.setColor(bg);
        canvas.drawRoundRect(rectTemp, 8, 8, pFill);

        if (border != 0) {
            pStroke.setColor(border);
            pStroke.setStrokeWidth(1.2f);
            canvas.drawRoundRect(rectTemp, 8, 8, pStroke);
        }

        pText.setColor(textColor);
        pText.setTextSize(textSize);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(bold);
        canvas.drawText(text, x + w / 2, y + h / 2 + textSize * 0.35f, pText);
        pText.setFakeBoldText(false);
    }

    private void drawMiniBadge(Canvas canvas, float x, float y, String text, int bg, int textColor) {
        pText.setTextSize(9);
        pText.setFakeBoldText(true);
        float tw = pText.measureText(text) + 8;
        rectTemp.set(x, y, x + tw, y + 14);
        pFill.setColor(bg);
        canvas.drawRoundRect(rectTemp, 4, 4, pFill);

        pText.setColor(textColor);
        pText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x + tw / 2, y + 10, pText);
        pText.setFakeBoldText(false);
    }

    private void drawFloatingParticles(Canvas canvas) {
        for (GameEngine.FloatingText ft : engine.floatingTexts) {
            pText.setColor(ft.color);
            pText.setAlpha((int) (ft.alpha * 255));
            pText.setTextSize(15);
            pText.setFakeBoldText(true);
            pText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(ft.text, ft.x, ft.y, pText);
        }
        pText.setAlpha(255);
        pText.setFakeBoldText(false);
    }

    private void drawEventLogTicker(Canvas canvas, int w, int h) {
        float barY = h - 24;
        rectTemp.set(0, barY, w, h);
        pFill.setColor(0xFF020617);
        canvas.drawRect(rectTemp, pFill);

        pText.setColor(0xFF94A3B8);
        pText.setTextSize(10);
        pText.setTextAlign(Paint.Align.LEFT);

        String msg = engine.eventLog.isEmpty() ? "Добро пожаловать в Wind Turbine Tycoon!" : engine.eventLog.get(0);
        canvas.drawText("📢 " + msg, 10, barY + 16, pText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        float x = event.getX();
        float y = event.getY();
        int w = getWidth();
        int h = getHeight();

        // 1. Audio Button
        float pad = 8;
        float rowH = 34;
        float colW = (w - pad * 3 - 48) / 2.0f;
        float audioX = pad * 2 + colW * 2 + 6;
        if (x >= audioX && x <= audioX + 42 && y >= 30 && y <= 30 + rowH) {
            sound.toggleSound();
            return true;
        }

        // 2. Tabs
        float tabAreaHeight = h * 0.42f;
        float panelY = h - tabAreaHeight;
        float tabH = 38;
        if (y >= panelY && y <= panelY + tabH) {
            int tabIndex = (int) (x / (w / 5.0f));
            if (tabIndex >= 0 && tabIndex < Tab.values().length) {
                currentTab = Tab.values()[tabIndex];
                sound.playSound("click");
                return true;
            }
        }

        // 3. Grid Taps
        float gridTop = 30 + rowH + 5 + 26 + 10;
        float gridHeight = h - gridTop - tabAreaHeight;
        if (y >= gridTop && y <= gridTop + gridHeight) {
            float cellW = (w - pad * 4) / 3.0f;
            float cellH = (gridHeight - pad * 4) / 3.0f;

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    float cx = pad + c * (cellW + pad);
                    float cy = gridTop + pad + r * (cellH + pad);

                    if (x >= cx && x <= cx + cellW && y >= cy && y <= cy + cellH) {
                        selectedRow = r;
                        selectedCol = c;
                        currentTab = Tab.BUILD;

                        // Tap turbine (extinguishes if burning or boosts energy)
                        if (!engine.plots[r][c].isEmpty()) {
                            engine.tapTurbine(r, c, x, y);
                        } else {
                            sound.playSound("click");
                        }
                        return true;
                    }
                }
            }
        }

        // 4. Tab Actions
        if (y > panelY + tabH && y < h - 24) {
            handleTabActionClick(x, y, panelY + tabH + 4, tabAreaHeight - tabH - 30, w);
        }

        // 5. Bottom Event Log Bar Tap
        if (y >= h - 24) {
            currentTab = Tab.LOG;
            sound.playSound("click");
            return true;
        }

        return true;
    }

    private void handleTabActionClick(float x, float y, float contentY, float contentH, int w) {
        float pad = 10;
        TurbinePlot plot = engine.plots[selectedRow][selectedCol];

        if (currentTab == Tab.BUILD) {
            // Biome Buttons tap
            float bioH = 26;
            float bioW = (w - pad * 4) / 3.0f;
            if (y >= contentY && y <= contentY + bioH) {
                for (int i = 0; i < 3; i++) {
                    float bx = pad + i * (bioW + pad);
                    if (x >= bx && x <= bx + bioW) {
                        engine.selectBiome(Biome.values()[i]);
                        return;
                    }
                }
            }

            float afterBioY = contentY + bioH + 6;

            if (plot.isEmpty()) {
                float btnY = afterBioY + 20;
                float btnH = (contentH - bioH - 26) / 3.0f;
                float btnW = (w - pad * 3) / 2.0f;

                TurbineType[] types = TurbineType.values();
                for (int i = 0; i < Math.min(6, types.length); i++) {
                    int col = i % 2;
                    int row = i / 2;
                    float bx = pad + col * (btnW + pad);
                    float by = btnY + row * (btnH + 3);

                    if (x >= bx && x <= bx + btnW && y >= by && y <= by + btnH) {
                        engine.buildTurbine(selectedRow, selectedCol, types[i]);
                        return;
                    }
                }
            } else {
                float bY = afterBioY + 20;
                float bH = 38;
                float bW = (w - pad * 3) / 2.0f;

                // Upgrade Button
                if (x >= pad && x <= pad + bW && y >= bY && y <= bY + bH) {
                    engine.upgradeTurbine(selectedRow, selectedCol);
                    return;
                }

                // Evolution Button
                if (x >= pad * 2 + bW && x <= pad * 2 + bW * 2 && y >= bY && y <= bY + bH) {
                    engine.evolveTurbine(selectedRow, selectedCol);
                    return;
                }

                // Row 2: Repair Single & Repair ALL (-15%)
                float b2Y = bY + bH + 6;
                if (x >= pad && x <= pad + bW && y >= b2Y && y <= b2Y + bH) {
                    engine.repairTurbine(selectedRow, selectedCol);
                    return;
                }

                // Repair All Button
                if (x >= pad * 2 + bW && x <= pad * 2 + bW * 2 && y >= b2Y && y <= b2Y + bH) {
                    engine.repairAllTurbines();
                    return;
                }
            }
        } else if (currentTab == Tab.SCIENCE) {
            float itemY = contentY + 20;
            ResearchItem[] items = ResearchItem.values();
            float itemH = (contentH - 24) / Math.min(5, items.length);

            for (int i = 0; i < Math.min(5, items.length); i++) {
                float iy = itemY + i * itemH;
                float btnW = 85;
                float btnH = itemH - 8;
                float btnX = w - pad - btnW - 4;
                float btnY = iy + 2;

                if (x >= btnX && x <= btnX + btnW && y >= btnY && y <= btnY + btnH) {
                    engine.unlockResearch(items[i]);
                    return;
                }
            }
        } else if (currentTab == Tab.PRESTIGE) {
            float btnY = contentY + 26;
            float btnW = w - pad * 2;
            float btnH = 46;

            // Rebirth
            if (x >= pad && x <= pad + btnW && y >= btnY && y <= btnY + btnH) {
                engine.triggerPrestige();
                return;
            }

            // Export & Import
            float b2Y = btnY + btnH + 10;
            float halfW = (btnW - pad) / 2.0f;

            // Export to Clipboard
            if (x >= pad && x <= pad + halfW && y >= b2Y && y <= b2Y + 40) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    ClipData clip = ClipData.newPlainText("WindTycoonSave", engine.exportSaveCode());
                    cm.setPrimaryClip(clip);
                    engine.addLog("📋 Код сохранения скопирован в буфер обмена!");
                    sound.playSound("coin");
                }
                return;
            }

            // Import from Clipboard
            if (x >= pad + halfW + pad && x <= pad + btnW && y >= b2Y && y <= b2Y + 40) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                    if (text != null) {
                        boolean ok = engine.importSaveCode(text.toString());
                        if (ok) sound.playSound("upgrade");
                    }
                }
            }
        } else if (currentTab == Tab.LOG) {
            float btnW = (w - pad * 3) / 2.0f;
            float btnH = 34;

            // Clear Log
            if (x >= pad && x <= pad + btnW && y >= contentY && y <= contentY + btnH) {
                engine.eventLog.clear();
                sound.playSound("click");
                return;
            }

            // Copy Log to Clipboard
            if (x >= pad * 2 + btnW && x <= w - pad && y >= contentY && y <= contentY + btnH) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : engine.eventLog) {
                        sb.append(s).append("\n");
                    }
                    ClipData clip = ClipData.newPlainText("WindTycoonLog", sb.toString());
                    cm.setPrimaryClip(clip);
                    sound.playSound("coin");
                }
                return;
            }
        }
    }

    private String formatNumber(double num) {
        if (num >= 1_000_000) {
            return String.format(Locale.US, "%.2fM", num / 1_000_000);
        } else if (num >= 1_000) {
            return String.format(Locale.US, "%.1fk", num / 1_000);
        }
        return String.format(Locale.US, "%.0f", num);
    }
}
