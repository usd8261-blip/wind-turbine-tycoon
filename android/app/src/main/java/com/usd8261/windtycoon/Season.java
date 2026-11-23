package com.usd8261.windtycoon;

public enum Season {
    SPRING("🌸 Весна", 0xFF27AE60, 0xFF1E8449, 1.0f, 0.80f, "Таяние снегов: износ оборудования снижен на -20%"),
    SUMMER("☀️ Лето", 0xFF2ECC71, 0xFF27AE60, 1.0f, 1.0f, "Яркое солнце: солнечные гибриды получают +50% мощности"),
    AUTUMN("🍁 Осень", 0xFFD35400, 0xFFA04000, 1.15f, 1.0f, "Сезон ветров: базовый ветер усилен на +15%"),
    WINTER("❄️ Зима", 0xFFEBF5FB, 0xFFD4E6F1, 1.0f, 1.25f, "Отопительный пик: тариф продажи электроэнергии x1.35!");

    public final String title;
    public final int groundTop;
    public final int groundBot;
    public final float windMult;
    public final float wearMult;
    public final String bonusDesc;

    Season(String title, int groundTop, int groundBot, float windMult, float wearMult, String bonusDesc) {
        this.title = title;
        this.groundTop = groundTop;
        this.groundBot = groundBot;
        this.windMult = windMult;
        this.wearMult = wearMult;
        this.bonusDesc = bonusDesc;
    }

    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
