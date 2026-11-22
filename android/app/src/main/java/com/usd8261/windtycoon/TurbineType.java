package com.usd8261.windtycoon;

public enum TurbineType {
    SMALL("Малый ветряк", 10.0f, 1000.0, 0xFF10B981, "Базовая модель для старта"),
    MEDIUM("Средний ветряк", 35.0f, 3200.0, 0xFF3B82F6, "Увеличенный ротор и надёжный генератор"),
    GIANT("Гигантский ветряк", 110.0f, 9000.0, 0xFF8B5CF6, "Промышленная турбина мегаваттного класса"),
    OFFSHORE("Офшорный ветропарк", 320.0f, 24000.0, 0xFF06B6D4, "Морская платформа с мощными морскими бризами"),
    HYBRID("Солнечно-ветровой гибрид", 850.0f, 55000.0, 0xFFF59E0B, "Двусторонняя генерация 24/7 (ветер + фотопанели)"),
    QUANTUM_VAWT("Квантовый VAWT", 2500.0f, 140000.0, 0xFFEC4899, "Вертикальная ось с магнитной левитацией");

    public final String title;
    public final float baseKW;
    public final double baseCost;
    public final int color;
    public final String description;

    TurbineType(String title, float baseKW, double baseCost, int color, String description) {
        this.title = title;
        this.baseKW = baseKW;
        this.baseCost = baseCost;
        this.color = color;
        this.description = description;
    }

    public TurbineType getNext() {
        int nextIndex = this.ordinal() + 1;
        if (nextIndex < values().length) {
            return values()[nextIndex];
        }
        return null;
    }
}
