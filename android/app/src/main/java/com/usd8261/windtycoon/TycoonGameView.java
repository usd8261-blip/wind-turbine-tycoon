package com.usd8261.windtycoon;

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
import java.util.Locale;

public class TycoonGameView extends View {

    public enum Tab { BUILD, MARKET, SCIENCE, PRESTIGE }

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

    // Clouds
    private final float[] cloudX = {40, 220, 480};
    private final float[] cloudY = {80, 140, 95};
    private final float[] cloudSpeed = {0.35f, 0.2f, 0.45f};

    public TycoonGameView(Context context, GameEngine engine) {
        super(context);
        this.engine = engine;
        this.sound = SoundManager.getInstance(context);
        pStroke.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        engine.update();

        // 1. Sky & Sun/Moon & Clouds & Skyline
        drawSkyAndEnvironment(canvas, w, h);

        // 2. Top HUD
        float hudBottom = drawTopHUD(canvas, w);

        // 3. Main Field (3x3 Grid)
        float gridTop = hudBottom + 12;
        float tabAreaHeight = h * 0.38f;
        float gridHeight = h - gridTop - tabAreaHeight;
        drawTurbineGrid(canvas, w, gridTop, gridHeight);

        // 4. Floating Particles
        drawFloatingParticles(canvas);

        // 5. Bottom Tabs & Drawer Panel
        drawBottomPanel(canvas, w, h, h - tabAreaHeight, tabAreaHeight);

        // 6. Bottom Event Log Bar
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

        // Floating Clouds
        pFill.setColor(0x40FFFFFF);
        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] = (cloudX[i] + cloudSpeed[i]) % (w + 160);
            float cx = cloudX[i] - 80;
            float cy = cloudY[i];
            canvas.drawCircle(cx, cy, 32, pFill);
            canvas.drawCircle(cx + 25, cy - 8, 26, pFill);
            canvas.drawCircle(cx - 22, cy + 4, 22, pFill);
            canvas.drawCircle(cx + 45, cy + 6, 20, pFill);
        }

        // City skyline silhouettes at horizon
        pFill.setColor(0xFF1E293B);
        float horizonY = h * 0.42f;
        float bWidth = w / 14.0f;
        for (int i = 0; i < 14; i++) {
            float bH = 35 + ((i * 37) % 65);
            canvas.drawRect(i * bWidth, horizonY - bH, (i + 1) * bWidth - 2, horizonY, pFill);

            // Blinking yellow windows at night
            if (t > 0.75f || t < 0.30f) {
                pFill.setColor(0xAAFEF08A);
                for (int wy = (int) (horizonY - bH + 8); wy < horizonY - 6; wy += 12) {
                    if ((i + wy) % 3 == 0) {
                        canvas.drawRect(i * bWidth + 4, wy, i * bWidth + 10, wy + 6, pFill);
                    }
                }
                pFill.setColor(0xFF1E293B);
            }
        }

        // Rolling Green Landscape
        pFill.setColor(0xFF047857);
        canvas.drawRect(0, horizonY, w, h, pFill);
    }

    private float drawTopHUD(Canvas canvas, int w) {
        float startY = 32;
        float rowH = 38;
        float pad = 10;

        // Row 1: Balance & Income & Audio
        float colW = (w - pad * 3 - 50) / 2.0f;

        // Balance Card
        drawPill(canvas, pad, startY, colW, rowH, 0xDD111827, 0xFF10B981,
                "💰 $" + formatNumber(engine.balance), 0xFF10B981, 16, true);

        // Net Output & $/s Card
        double inc = engine.getIncomePerSecond();
        String netStr = String.format(Locale.US, "⚡ %.0f кВт (+%.1f$/с)", engine.getTotalOutputKW(), inc);
        drawPill(canvas, pad * 2 + colW, startY, colW, rowH, 0xDD111827, 0xFF3B82F6,
                netStr, 0xFF60A5FA, 13, true);

        // Audio Toggle Button
        float audioX = pad * 2 + colW * 2 + 6;
        drawPill(canvas, audioX, startY, 44, rowH, 0xDD111827, 0xFF64748B,
                sound.isSoundEnabled() ? "🔊" : "🔇", 0xFFFFFFFF, 16, true);

        // Row 2: Wind, SP, Prestige, Time
        float r2Y = startY + rowH + 6;
        float subW = (w - pad * 5) / 4.0f;

        // Wind
        drawPill(canvas, pad, r2Y, subW, 28, 0xBB111827, 0,
                String.format(Locale.US, "💨 %.1fм/с", engine.windSpeed), 0xFF38BDF8, 11, false);

        // Science
        drawPill(canvas, pad * 2 + subW, r2Y, subW, 28, 0xBB111827, 0,
                String.format(Locale.US, "🔬 %.1f SP", engine.sciencePoints), 0xFFA78BFA, 11, false);

        // Prestige
        drawPill(canvas, pad * 3 + subW * 2, r2Y, subW, 28, 0xBB111827, 0,
                "👑 R" + engine.prestigeCount + " x" + String.format(Locale.US, "%.1f", engine.prestigeMultiplier),
                0xFFFBBF24, 11, false);

        // Time of Day
        String timeStr = "☀️ День";
        if (engine.timeOfDay > 0.75f || engine.timeOfDay < 0.25f) timeStr = "🌙 Ночь";
        else if (engine.timeOfDay >= 0.25f && engine.timeOfDay < 0.35f) timeStr = "🌅 Рассвет";
        else if (engine.timeOfDay >= 0.65f && engine.timeOfDay <= 0.75f) timeStr = "🌇 Закат";

        drawPill(canvas, pad * 4 + subW * 3, r2Y, subW, 28, 0xBB111827, 0,
                timeStr, 0xFFCBD5E1, 11, false);

        return r2Y + 28;
    }

    private void drawTurbineGrid(Canvas canvas, int w, float topY, float gridH) {
        float pad = 10;
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

        // Background Pad
        rectTemp.set(x, y, x + w, y + h);
        pFill.setColor(selected ? 0xFF065F46 : 0xFF047857);
        canvas.drawRoundRect(rectTemp, 14, 14, pFill);

        // Border
        pStroke.setColor(selected ? 0xFF34D399 : 0x40FFFFFF);
        pStroke.setStrokeWidth(selected ? 3.0f : 1.0f);
        canvas.drawRoundRect(rectTemp, 14, 14, pStroke);

        if (plot.isEmpty()) {
            // Empty plot
            pText.setColor(0xEEFFFFFF);
            pText.setTextSize(14);
            pText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("➕ СТРОЙКА", x + w / 2, y + h / 2 - 2, pText);

            pText.setColor(0xFF34D399);
            pText.setTextSize(11);
            canvas.drawText("от $1,000", x + w / 2, y + h / 2 + 16, pText);
            return;
        }

        // Active Turbine drawing
        float hubX = x + w / 2;
        float hubY = y + h * 0.45f;
        float baseH = y + h - 8;

        // Shadow under turbine
        pFill.setColor(0x30000000);
        canvas.drawOval(hubX - 22, baseH - 4, hubX + 22, baseH + 4, pFill);

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
            // Vertical helical blades
            pFill.setColor(plot.type.color);
            for (int i = 0; i < 4; i++) {
                canvas.save();
                canvas.rotate(i * 90);
                rectTemp.set(-4, -bladeLen, 4, 0);
                canvas.drawRoundRect(rectTemp, 3, 3, pFill);
                canvas.restore();
            }
        } else {
            // 3 Aero blades
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

                // Vibrant wing tip color
                pFill.setColor(plot.type.color);
                canvas.drawCircle(0, -bladeLen * 0.92f, 2.2f, pFill);
                pFill.setColor(0xFFFFFFFF);

                canvas.restore();
            }
        }

        // Hub center cap
        pFill.setColor(0xFF1E293B);
        canvas.drawCircle(0, 0, 4.5f, pFill);
        canvas.restore();

        // Level pill (top left)
        drawMiniBadge(canvas, x + 6, y + 6, "Lv." + plot.level, 0xFF1E293B, 0xFF60A5FA);

        // Health bar (bottom)
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

        // Warning indicator if broken
        if (plot.isBroken) {
            drawMiniBadge(canvas, x + w - 46, y + 6, "⚠️ СЛОМАН", 0xFF991B1B, 0xFFFCA5A5);
        }
    }

    private void drawBottomPanel(Canvas canvas, int w, int h, float panelY, float panelH) {
        // Dark panel background
        rectTemp.set(0, panelY, w, h);
        pFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(rectTemp, 20, 20, pFill);

        // Tab Selector Header
        float tabH = 42;
        float tabW = w / 4.0f;
        Tab[] tabs = Tab.values();
        String[] tabNames = {"🏗️ Стройка", "📈 Биржа", "🔬 Наука", "👑 Престиж"};

        for (int i = 0; i < tabs.length; i++) {
            boolean active = (tabs[i] == currentTab);
            float tx = i * tabW;

            rectTemp.set(tx + 2, panelY + 4, tx + tabW - 2, panelY + tabH - 2);
            pFill.setColor(active ? 0xFF1E293B : 0x00000000);
            canvas.drawRoundRect(rectTemp, 8, 8, pFill);

            if (active) {
                pStroke.setColor(0xFF38BDF8);
                pStroke.setStrokeWidth(2.0f);
                canvas.drawRoundRect(rectTemp, 8, 8, pStroke);
            }

            pText.setColor(active ? 0xFF38BDF8 : 0xFF94A3B8);
            pText.setTextSize(13);
            pText.setTextAlign(Paint.Align.CENTER);
            pText.setFakeBoldText(active);
            canvas.drawText(tabNames[i], tx + tabW / 2, panelY + tabH / 2 + 4, pText);
            pText.setFakeBoldText(false);
        }

        // Tab Content Area
        float contentY = panelY + tabH + 6;
        float contentH = panelH - tabH - 34; // leave 34px for bottom log ticker

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
        }
    }

    private void drawBuildTab(Canvas canvas, int w, float y, float h) {
        TurbinePlot plot = engine.plots[selectedRow][selectedCol];
        float pad = 12;

        if (plot.isEmpty()) {
            // Show Build options for empty plot
            pText.setColor(0xFFE2E8F0);
            pText.setTextSize(14);
            pText.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Выберите ветряк для участка [" + (selectedRow + 1) + "," + (selectedCol + 1) + "]:", pad, y + 16, pText);

            float btnY = y + 26;
            float btnH = (h - 36) / 3.0f;
            float btnW = (w - pad * 3) / 2.0f;

            TurbineType[] types = TurbineType.values();
            for (int i = 0; i < Math.min(6, types.length); i++) {
                int col = i % 2;
                int row = i / 2;
                float bx = pad + col * (btnW + pad);
                float by = btnY + row * (btnH + 4);

                TurbineType tt = types[i];
                boolean canAfford = engine.balance >= tt.baseCost;
                drawActionButton(canvas, bx, by, btnW, btnH - 4,
                        tt.title + " ($" + (int) tt.baseCost + ")",
                        String.format(Locale.US, "+%.0f кВт", tt.baseKW),
                        canAfford ? 0xFF059669 : 0xFF334155, canAfford ? 0xFF10B981 : 0xFF64748B);
            }
            return;
        }

        // Active Plot Actions (Upgrade, Evolve, Repair)
        pText.setColor(0xFFF8FAFC);
        pText.setTextSize(14);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setFakeBoldText(true);
        canvas.drawText(plot.type.title + " [Lv." + plot.level + "] на участке [" + (selectedRow + 1) + "," + (selectedCol + 1) + "]", pad, y + 16, pText);
        pText.setFakeBoldText(false);

        float kw = plot.getOutputKW(engine.windSpeed, ResearchItem.BLADE_AERO.unlocked ? 1.2f : 1.0f);
        pText.setColor(0xFF94A3B8);
        pText.setTextSize(12);
        canvas.drawText(String.format(Locale.US, "Выработка: %.1f кВт | Состояние: %.1f%%", kw, plot.condition), pad, y + 34, pText);

        // Buttons
        float bY = y + 44;
        float bH = 46;
        float bW = (w - pad * 3) / 2.0f;

        // Button 1: Upgrade Level
        boolean canUp = (plot.level < 10) && (engine.balance >= plot.getUpgradeCost());
        String upTitle = (plot.level >= 10) ? "МАКС УРОВЕНЬ" : "Улучшить Lv." + (plot.level + 1);
        String upSub = (plot.level >= 10) ? "Достигнут максимум" : "Цена: $" + formatNumber(plot.getUpgradeCost());
        drawActionButton(canvas, pad, bY, bW, bH, upTitle, upSub,
                canUp ? 0xFF2563EB : 0xFF1E293B, canUp ? 0xFF3B82F6 : 0xFF475569);

        // Button 2: Evolution (Upgrade to next model)
        TurbineType next = plot.getNextEvolutionType();
        boolean canEvolve = plot.canEvolve() && (engine.balance >= plot.getEvolutionCost());
        String evoTitle = (next != null) ? "⚡ Эволюция: " + next.title : "ФИНАЛЬНАЯ МОДЕЛЬ";
        String evoSub = (next != null) ? "Цена: $" + formatNumber(plot.getEvolutionCost()) : "Высший тир";
        drawActionButton(canvas, pad * 2 + bW, bY, bW, bH, evoTitle, evoSub,
                canEvolve ? 0xFFD97706 : 0xFF1E293B, canEvolve ? 0xFFF59E0B : 0xFF475569);

        // Row 2 of buttons: Repair & Tech support
        float b2Y = bY + bH + 8;
        boolean canRepair = (plot.condition < 100.0f) && (engine.balance >= plot.getRepairCost());
        String repTitle = (plot.condition >= 100.0f) ? "Турбина исправна" : "Починить";
        String repSub = (plot.condition >= 100.0f) ? "100%" : "Цена: $" + formatNumber(plot.getRepairCost());
        drawActionButton(canvas, pad, b2Y, bW, bH, repTitle, repSub,
                canRepair ? 0xFF059669 : 0xFF1E293B, canRepair ? 0xFF10B981 : 0xFF475569);

        // Button: Hire Tech
        String techTitle = engine.hasTechSupport ? "Техник: Нанят" : "Нанять техника";
        String techSub = engine.hasTechSupport ? "-$1/с (износ -60%)" : "$250 (-$1/с)";
        drawActionButton(canvas, pad * 2 + bW, b2Y, bW, bH, techTitle, techSub,
                engine.hasTechSupport ? 0xFF0D9488 : 0xFF0284C7, 0xFF14B8A6);
    }

    private void drawMarketTab(Canvas canvas, int w, float y, float h) {
        float pad = 12;

        pText.setColor(0xFFF8FAFC);
        pText.setTextSize(15);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setFakeBoldText(true);
        canvas.drawText("⚡ Энергетическая Биржа & Контракты", pad, y + 16, pText);
        pText.setFakeBoldText(false);

        // Live Market Price Card
        float cardY = y + 26;
        float cardH = 50;
        rectTemp.set(pad, cardY, w - pad, cardY + cardH);
        pFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(rectTemp, 10, 10, pFill);

        pText.setColor(0xFF38BDF8);
        pText.setTextSize(14);
        canvas.drawText(String.format(Locale.US, "Текущая цена: $%.3f / кВт·ч", engine.marketPrice), pad + 12, cardY + 22, pText);

        pText.setColor(0xFF94A3B8);
        pText.setTextSize(12);
        canvas.drawText("Спрос города: " + (engine.marketPrice > 0.4f ? "🔥 Высокий (Пик!)" : "Обычный"), pad + 12, cardY + 40, pText);

        // Contract Card
        float contY = cardY + cardH + 10;
        float contH = 80;
        rectTemp.set(pad, contY, w - pad, contY + contH);
        pFill.setColor(0xFF1E1B4B);
        canvas.drawRoundRect(rectTemp, 12, 12, pFill);
        pStroke.setColor(0xFF6366F1);
        pStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(rectTemp, 12, 12, pStroke);

        if (engine.currentContract != null) {
            Contract c = engine.currentContract;
            pText.setColor(0xFFC7D2FE);
            pText.setTextSize(13);
            pText.setFakeBoldText(true);
            canvas.drawText("📋 " + c.title, pad + 12, contY + 22, pText);
            pText.setFakeBoldText(false);

            pText.setColor(0xFFFBBF24);
            pText.setTextSize(12);
            canvas.drawText("Награда: +$" + (int) c.reward + " | Нужно: " + (int) c.targetKW + " кВт", pad + 12, contY + 42, pText);

            // Progress bar
            float prgW = (w - pad * 2 - 24);
            float currentKW = engine.getTotalOutputKW();
            float ratio = Math.min(1.0f, currentKW / c.targetKW);

            rectTemp.set(pad + 12, contY + 52, pad + 12 + prgW, contY + 62);
            pFill.setColor(0x60000000);
            canvas.drawRoundRect(rectTemp, 4, 4, pFill);

            rectTemp.set(pad + 12, contY + 52, pad + 12 + prgW * ratio, contY + 62);
            pFill.setColor(0xFF10B981);
            canvas.drawRoundRect(rectTemp, 4, 4, pFill);

            pText.setColor(0xFFE2E8F0);
            pText.setTextSize(11);
            canvas.drawText(String.format(Locale.US, "%.0f / %.0f кВт (Осталось: %.0fc)", currentKW, c.targetKW, c.remainingSeconds), pad + 12, contY + 74, pText);
        }
    }

    private void drawScienceTab(Canvas canvas, int w, float y, float h) {
        float pad = 12;

        pText.setColor(0xFFF8FAFC);
        pText.setTextSize(15);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setFakeBoldText(true);
        canvas.drawText("🔬 Научно-исследовательская лаборатория (R&D)", pad, y + 16, pText);
        pText.setFakeBoldText(false);

        float itemY = y + 26;
        float itemH = (h - 32) / 4.0f;

        ResearchItem[] items = ResearchItem.values();
        for (int i = 0; i < items.length; i++) {
            ResearchItem res = items[i];
            float iy = itemY + i * itemH;

            rectTemp.set(pad, iy, w - pad, iy + itemH - 4);
            pFill.setColor(res.unlocked ? 0xFF064E3B : 0xFF1E293B);
            canvas.drawRoundRect(rectTemp, 8, 8, pFill);

            pText.setColor(res.unlocked ? 0xFF34D399 : 0xFFE2E8F0);
            pText.setTextSize(13);
            pText.setFakeBoldText(true);
            canvas.drawText(res.icon + " " + res.title, pad + 10, iy + 18, pText);
            pText.setFakeBoldText(false);

            pText.setColor(0xFF94A3B8);
            pText.setTextSize(11);
            canvas.drawText(res.description, pad + 10, iy + 33, pText);

            // Unlock button on the right
            float btnW = 90;
            float btnH = itemH - 12;
            float btnX = w - pad - btnW - 6;
            float btnY = iy + 4;

            boolean canUnlock = (!res.unlocked) && (engine.sciencePoints >= res.costSP);
            String btnText = res.unlocked ? "ИЗУЧЕНО" : (int) res.costSP + " SP";
            drawActionButton(canvas, btnX, btnY, btnW, btnH, btnText, "",
                    res.unlocked ? 0xFF047857 : (canUnlock ? 0xFF7C3AED : 0xFF334155),
                    res.unlocked ? 0xFF10B981 : (canUnlock ? 0xFFA78BFA : 0xFF64748B));
        }
    }

    private void drawPrestigeTab(Canvas canvas, int w, float y, float h) {
        float pad = 12;

        pText.setColor(0xFFFBBF24);
        pText.setTextSize(16);
        pText.setTextAlign(Paint.Align.LEFT);
        pText.setFakeBoldText(true);
        canvas.drawText("👑 Престиж / IPO Энергохолдинга", pad, y + 20, pText);
        pText.setFakeBoldText(false);

        pText.setColor(0xFFE2E8F0);
        pText.setTextSize(13);
        canvas.drawText("Сбросьте ферму, чтобы получить постоянный множитель:", pad, y + 42, pText);

        pText.setColor(0xFF34D399);
        pText.setTextSize(15);
        pText.setFakeBoldText(true);
        canvas.drawText("Текущий бонус: x" + String.format(Locale.US, "%.1f", engine.prestigeMultiplier) + " ➔ Будет: x" + String.format(Locale.US, "%.1f", engine.prestigeMultiplier + 0.5), pad, y + 66, pText);
        pText.setFakeBoldText(false);

        // Rebirth Button
        boolean canRebirth = engine.balance >= 100000.0;
        float btnY = y + 84;
        float btnW = w - pad * 2;
        float btnH = 55;

        String title = canRebirth ? "👑 СОВЕРШИТЬ ПРЕСТИЖ (+50% ДОХОДА)" : "НУЖНО $100,000 ДЛЯ ПРЕСТИЖА";
        String sub = canRebirth ? "Баланс будет сброшен, множитель навсегда останется!" : "Текущий баланс: $" + formatNumber(engine.balance);
        drawActionButton(canvas, pad, btnY, btnW, btnH, title, sub,
                canRebirth ? 0xFFD97706 : 0xFF1E293B, canRebirth ? 0xFFF59E0B : 0xFF475569);
    }

    private void drawActionButton(Canvas canvas, float x, float y, float w, float h, String title, String sub, int bg, int border) {
        rectTemp.set(x, y, x + w, y + h);
        pFill.setColor(bg);
        canvas.drawRoundRect(rectTemp, 8, 8, pFill);

        pStroke.setColor(border);
        pStroke.setStrokeWidth(1.2f);
        canvas.drawRoundRect(rectTemp, 8, 8, pStroke);

        pText.setColor(0xFFFFFFFF);
        pText.setTextSize(12);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setFakeBoldText(true);

        if (sub.isEmpty()) {
            canvas.drawText(title, x + w / 2, y + h / 2 + 4, pText);
        } else {
            canvas.drawText(title, x + w / 2, y + h * 0.42f, pText);
            pText.setColor(0xFFCBD5E1);
            pText.setTextSize(10);
            pText.setFakeBoldText(false);
            canvas.drawText(sub, x + w / 2, y + h * 0.78f, pText);
        }
        pText.setFakeBoldText(false);
    }

    private void drawPill(Canvas canvas, float x, float y, float w, float h, int bg, int border, String text, int textColor, float textSize, boolean bold) {
        rectTemp.set(x, y, x + w, y + h);
        pFill.setColor(bg);
        canvas.drawRoundRect(rectTemp, 10, 10, pFill);

        if (border != 0) {
            pStroke.setColor(border);
            pStroke.setStrokeWidth(1.2f);
            canvas.drawRoundRect(rectTemp, 10, 10, pStroke);
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
            pText.setTextSize(16);
            pText.setFakeBoldText(true);
            pText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(ft.text, ft.x, ft.y, pText);
        }
        pText.setAlpha(255);
        pText.setFakeBoldText(false);
    }

    private void drawEventLogTicker(Canvas canvas, int w, int h) {
        float barY = h - 26;
        rectTemp.set(0, barY, w, h);
        pFill.setColor(0xFF020617);
        canvas.drawRect(rectTemp, pFill);

        pText.setColor(0xFF94A3B8);
        pText.setTextSize(11);
        pText.setTextAlign(Paint.Align.LEFT);

        String msg = engine.eventLog.isEmpty() ? "Добро пожаловать в Wind Turbine Tycoon!" : engine.eventLog.get(0);
        canvas.drawText("📢 " + msg, 12, barY + 17, pText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        float x = event.getX();
        float y = event.getY();
        int w = getWidth();
        int h = getHeight();

        // 1. Audio Button tap
        float pad = 10;
        float rowH = 38;
        float colW = (w - pad * 3 - 50) / 2.0f;
        float audioX = pad * 2 + colW * 2 + 6;
        if (x >= audioX && x <= audioX + 44 && y >= 32 && y <= 32 + rowH) {
            sound.toggleSound();
            return true;
        }

        // 2. Tab Selection
        float tabAreaHeight = h * 0.38f;
        float panelY = h - tabAreaHeight;
        float tabH = 42;
        if (y >= panelY && y <= panelY + tabH) {
            int tabIndex = (int) (x / (w / 4.0f));
            if (tabIndex >= 0 && tabIndex < Tab.values().length) {
                currentTab = Tab.values()[tabIndex];
                sound.playSound("click");
                return true;
            }
        }

        // 3. Grid Taps (select plot or manual click boost)
        float gridTop = 32 + rowH + 6 + 28 + 12;
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
                        currentTab = Tab.BUILD; // switch to build tab to see actions

                        // If active turbine, trigger manual click energy boost!
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

        // 4. Tab Action Buttons Taps
        if (y > panelY + tabH && y < h - 26) {
            handleTabActionClick(x, y, panelY + tabH + 6, tabAreaHeight - tabH - 34, w);
        }

        return true;
    }

    private void handleTabActionClick(float x, float y, float contentY, float contentH, int w) {
        float pad = 12;
        TurbinePlot plot = engine.plots[selectedRow][selectedCol];

        if (currentTab == Tab.BUILD) {
            if (plot.isEmpty()) {
                // Building options
                float btnY = contentY + 26;
                float btnH = (contentH - 36) / 3.0f;
                float btnW = (w - pad * 3) / 2.0f;

                TurbineType[] types = TurbineType.values();
                for (int i = 0; i < Math.min(6, types.length); i++) {
                    int col = i % 2;
                    int row = i / 2;
                    float bx = pad + col * (btnW + pad);
                    float by = btnY + row * (btnH + 4);

                    if (x >= bx && x <= bx + btnW && y >= by && y <= by + btnH) {
                        engine.buildTurbine(selectedRow, selectedCol, types[i]);
                        return;
                    }
                }
            } else {
                // Upgrade, Evolve, Repair
                float bY = contentY + 44;
                float bH = 46;
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

                // Repair Button
                float b2Y = bY + bH + 8;
                if (x >= pad && x <= pad + bW && y >= b2Y && y <= b2Y + bH) {
                    engine.repairTurbine(selectedRow, selectedCol);
                    return;
                }

                // Tech Support Toggle
                if (x >= pad * 2 + bW && x <= pad * 2 + bW * 2 && y >= b2Y && y <= b2Y + bH) {
                    if (!engine.hasTechSupport && engine.balance >= 250) {
                        engine.balance -= 250;
                        engine.hasTechSupport = true;
                        sound.playSound("coin");
                        engine.addLog("👷 Нанята сервисная бригада техников!");
                    }
                    return;
                }
            }
        } else if (currentTab == Tab.SCIENCE) {
            float itemY = contentY + 26;
            float itemH = (contentH - 32) / 4.0f;
            ResearchItem[] items = ResearchItem.values();

            for (int i = 0; i < items.length; i++) {
                float iy = itemY + i * itemH;
                float btnW = 90;
                float btnH = itemH - 12;
                float btnX = w - pad - btnW - 6;
                float btnY = iy + 4;

                if (x >= btnX && x <= btnX + btnW && y >= btnY && y <= btnY + btnH) {
                    engine.unlockResearch(items[i]);
                    return;
                }
            }
        } else if (currentTab == Tab.PRESTIGE) {
            float btnY = contentY + 84;
            float btnW = w - pad * 2;
            float btnH = 55;

            if (x >= pad && x <= pad + btnW && y >= btnY && y <= btnY + btnH) {
                engine.triggerPrestige();
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
