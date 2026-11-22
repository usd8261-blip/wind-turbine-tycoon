package com.usd8261.windtycoon;

public class Contract {
    public final String title;
    public final float targetKW;
    public final double reward;
    public float remainingSeconds;
    public final float totalSeconds;
    public boolean completed = false;

    public Contract(String title, float targetKW, double reward, float seconds) {
        this.title = title;
        this.targetKW = targetKW;
        this.reward = reward;
        this.remainingSeconds = seconds;
        this.totalSeconds = seconds;
    }

    public static Contract generateRandom() {
        int r = (int) (Math.random() * 3);
        switch (r) {
            case 0:
                return new Contract("Промзона: Дефицит энергии", 80.0f, 1500.0, 45.0f);
            case 1:
                return new Contract("Мегаполис: Сбой на угольной ТЭЦ", 250.0f, 4200.0, 60.0f);
            default:
                return new Contract("Дата-центр: Зеленый сертификат", 600.0f, 12000.0, 75.0f);
        }
    }
}
