package com.usd8261.windtycoon;

public enum Biome {
    VALLEY("Зеленая Долина", 0.0, 1.0f, 1.0f, "Сбалансированная стартовая локация с мягким ветром", 0xFF2ECC71),
    PEAKS("Горные Вершины", 75000.0, 1.35f, 1.15f, "Скалистые хребты: +35% к скорости ветра, +15% доход", 0xFFBDC3C7),
    OCEAN("Морской Шельф", 200000.0, 1.15f, 1.50f, "Морской бриз: +50% к доходу офшорных турбин", 0xFF00B4D8);

    public final String title;
    public final double unlockCost;
    public final float windModifier;
    public final float incomeModifier;
    public final String description;
    public final int color;

    Biome(String title, double unlockCost, float windModifier, float incomeModifier, String description, int color) {
        this.title = title;
        this.unlockCost = unlockCost;
        this.windModifier = windModifier;
        this.incomeModifier = incomeModifier;
        this.description = description;
        this.color = color;
    }
}
