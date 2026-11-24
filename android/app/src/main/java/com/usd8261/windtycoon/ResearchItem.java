package com.usd8261.windtycoon;

public enum ResearchItem {
    BLADE_AERO("Аэродинамика лопастей", 15.0f, "+20% к выработке всех турбин", "🧪"),
    GENERATOR("Сверхпроводящий генератор", 35.0f, "+25% к цене продажи на бирже", "⚡"),
    AUTO_DRONES("Автономные ремонтные дроны", 60.0f, "Дроны сами чинят повреждённые турбины", "🤖"),
    AI_FORECAST("ИИ прогнозирования погоды", 100.0f, "Пиковые цены на бирже +50%", "🧠"),
    METEOROLOGIST("Главный метеоролог", 30.0f, "Прогноз погодных циклонов и ветра", "📡"),
    AI_BROKER("AI-Брокер", 50.0f, "Авто-продажа в пиковые тарифы", "📈"),
    CHIEF_MECHANIC("Главный механик", 40.0f, "-30% износа и спасение турбин", "🔧"),
    HYDROGEN_PLANT("Завод электролиза H₂", 120.0f, "Генерация водорода по $2.40/кг", "💧"),
    ORBITAL_BEAM("Орбитальный СВЧ-Луч", 350.0f, "+400 кВт чистой энергии 24/7", "🛰️");

    public final String title;
    public final float costSP;
    public final String description;
    public final String icon;
    public boolean unlocked = false;

    ResearchItem(String title, float costSP, String description, String icon) {
        this.title = title;
        this.costSP = costSP;
        this.description = description;
        this.icon = icon;
    }
}
