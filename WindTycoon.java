import javax.sound.midi.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.awt.datatransfer.StringSelection;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * ⚡ Wind Turbine Tycoon (Ultimate Edition)
 * Включает все 7 расширенных систем + систему фоновой музыки:
 * 1. 🔋 Накопители энергии и Биржа (Battery Substation & Energy Market)
 * 2. 🔬 Дерево исследований (R&D Lab / Science Tree)
 * 3. 🌊 Новые биомы и турбины (Офшорные, Солнечно-ветровые гибриды, Квантовый VAWT)
 * 4. 👑 Престиж / Ребёрт (IPO / Rebirth с постоянным множителем дохода)
 * 5. 🤖 Автоматизация и дроны (Летающие ремонтные дроны со сваркой)
 * 6. 🏆 Случайные события, квесты и эко-рост города (Саммит, Молнии, Птицы, Ачивки)
 * 7. 🎨 Визуальные и звуковые фишки (Критический клик x5, Дождь, Птицы в небе, 10 MIDI-звуков)
 * 8. 🎵 Музыкальный плеер: поддержка фонового трека из папки music/ с зацикливанием
 */
public class WindTycoon extends JFrame {

    // ==========================================
    // === 1. ЗВУКОВАЯ СИСТЕМА (ЧИСТЫЙ JAVA MIDI)
    // ==========================================
    private static class SoundSystem {
        private static Synthesizer synth;
        private static MidiChannel channel;

        static {
            try {
                synth = MidiSystem.getSynthesizer();
                synth.open();
                MidiChannel[] channels = synth.getChannels();
                if (channels != null && channels.length > 0) {
                    channel = channels[0];
                }
            } catch (Exception ignored) {}
        }

        public static void play(String type) {
            new Thread(() -> {
                try {
                    if (channel == null) return;
                    switch (type) {
                        case "click":
                            channel.programChange(12); // Marimba
                            channel.noteOn(68 + (int)(Math.random() * 8), 90);
                            Thread.sleep(50);
                            channel.allNotesOff();
                            break;
                        case "crit":
                            channel.programChange(81); // Lead Synth
                            channel.noteOn(76, 110);
                            Thread.sleep(50);
                            channel.noteOn(83, 115);
                            Thread.sleep(50);
                            channel.noteOn(88, 120);
                            Thread.sleep(120);
                            channel.allNotesOff();
                            break;
                        case "coin":
                            channel.programChange(11); // Vibraphone
                            channel.noteOn(76, 95);
                            Thread.sleep(60);
                            channel.noteOn(83, 105);
                            Thread.sleep(120);
                            channel.allNotesOff();
                            break;
                        case "quest":
                            channel.programChange(14); // Tubular Bells
                            int[] notes = {72, 76, 79, 84};
                            for (int n : notes) {
                                channel.noteOn(n, 105);
                                Thread.sleep(70);
                            }
                            Thread.sleep(250);
                            channel.allNotesOff();
                            break;
                        case "build":
                            channel.programChange(80); // Synth Lead
                            channel.noteOn(60, 85);
                            Thread.sleep(60);
                            channel.noteOn(65, 95);
                            Thread.sleep(60);
                            channel.noteOn(72, 105);
                            Thread.sleep(120);
                            channel.allNotesOff();
                            break;
                        case "siren":
                            channel.programChange(81);
                            channel.noteOn(50, 100);
                            Thread.sleep(150);
                            channel.noteOn(58, 110);
                            Thread.sleep(150);
                            channel.allNotesOff();
                            break;
                        case "research":
                            channel.programChange(46); // Orchestral Harp
                            int[] rNotes = {60, 64, 67, 72, 76};
                            for (int n : rNotes) {
                                channel.noteOn(n, 90);
                                Thread.sleep(45);
                            }
                            Thread.sleep(180);
                            channel.allNotesOff();
                            break;
                        case "market":
                            channel.programChange(13); // Xylophone
                            channel.noteOn(72, 100);
                            Thread.sleep(60);
                            channel.noteOn(84, 110);
                            Thread.sleep(100);
                            channel.allNotesOff();
                            break;
                        case "rebirth":
                            channel.programChange(19); // Church Organ / Fanfare
                            int[] fan = {60, 64, 67, 72, 79, 84};
                            for (int n : fan) {
                                channel.noteOn(n, 115);
                                Thread.sleep(80);
                            }
                            Thread.sleep(400);
                            channel.allNotesOff();
                            break;
                        case "thunder":
                            channel.programChange(127); // Gunshot / Rumble
                            channel.noteOn(35, 125);
                            Thread.sleep(200);
                            channel.noteOn(32, 110);
                            Thread.sleep(300);
                            channel.allNotesOff();
                            break;
                    }
                } catch (Exception ignored) {}
            }).start();
        }
    }

    // ==========================================
    // === 1.1 ПЛЕЕР ФОНОВОЙ МУЗЫКИ (WAV / BGM)
    // ==========================================
    public static class MusicPlayer {
        private static Clip musicClip = null;
        private static boolean isMuted = false;

        public static void initAndPlay() {
            new Thread(() -> {
                try {
                    File musicFile = findMusicFile();
                    if (musicFile != null && musicFile.exists()) {
                        if (musicClip != null) {
                            musicClip.stop();
                            musicClip.close();
                        }
                        AudioInputStream ais = AudioSystem.getAudioInputStream(musicFile);
                        musicClip = AudioSystem.getClip();
                        musicClip.open(ais);

                        if (musicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                            FloatControl gain = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
                            gain.setValue(-6.0f);
                        }

                        if (!isMuted) {
                            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Music error: " + ex.getMessage());
                }
            }).start();
        }

        public static File findMusicFile() {
            String[] preferred = {"music/bgm.wav", "music/music.wav", "bgm.wav", "music.wav"};
            for (String p : preferred) {
                File f = new File(p);
                if (f.exists() && f.length() > 0) return f;
            }
            File mDir = new File("music");
            if (mDir.exists() && mDir.isDirectory()) {
                File[] list = mDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
                if (list != null && list.length > 0) return list[0];
            }
            return null;
        }

        public static boolean toggleMute() {
            isMuted = !isMuted;
            if (musicClip != null) {
                if (isMuted) {
                    musicClip.stop();
                } else {
                    musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } else if (!isMuted) {
                initAndPlay();
            }
            return !isMuted;
        }

        public static boolean isMuted() {
            return isMuted;
        }
    }

    // ==========================================
    // === 2. ТИПЫ ВЕТРЯКОВ И ОБЪЕКТЫ ИГРЫ
    // ==========================================
    public enum TurbineType {
        BASIC("Малый ветряк", 1000.0, 50.0, 4.80, 0.04, new Color(46, 204, 113), new Color(0, 255, 180), "Классический ротор"),
        ADVANCED("Средний ветряк", 3200.0, 180.0, 18.00, 0.07, new Color(52, 152, 219), new Color(0, 220, 255), "Усиленная мачта"),
        INDUSTRIAL("Гигант ветропарка", 9000.0, 550.0, 68.00, 0.10, new Color(155, 89, 182), new Color(255, 75, 200), "Высокая мощность"),
        OFFSHORE("Офшорный ветряк", 24000.0, 1800.0, 210.00, 0.08, new Color(24, 119, 242), new Color(100, 220, 255), "Морской бриз (+30% в штиль)"),
        SOLAR_HYBRID("Ветро-Солнечный", 55000.0, 4500.0, 560.00, 0.06, new Color(243, 156, 18), new Color(255, 220, 50), "Солнечные батареи днем"),
        QUANTUM_VAWT("Квантовый VAWT", 140000.0, 12000.0, 1500.00, 0.02, new Color(233, 30, 99), new Color(255, 80, 160), "Вертикальный ротор, мин. износ");

        final String name;
        final double cost;
        final double maxPowerKw;
        final double baseIncomePerSec;
        final double wearRate;
        final Color themeColor;
        final Color neonColor;
        final String perk;

        TurbineType(String name, double cost, double maxPowerKw, double baseIncomePerSec, double wearRate, Color themeColor, Color neonColor, String perk) {
            this.name = name;
            this.cost = cost;
            this.maxPowerKw = maxPowerKw;
            this.baseIncomePerSec = baseIncomePerSec;
            this.wearRate = wearRate;
            this.themeColor = themeColor;
            this.neonColor = neonColor;
            this.perk = perk;
        }

        public TurbineType getNextTier() {
            int nextOrd = this.ordinal() + 1;
            TurbineType[] all = TurbineType.values();
            return (nextOrd < all.length) ? all[nextOrd] : null;
        }
    }

    public static class Plot {
        int col, row;
        TurbineType turbine = null;
        double durability = 100.0;
        double bladeAngle = 0.0;
        int level = 1;
        boolean isBurning = false;

        Plot(int c, int r) {
            this.col = c;
            this.row = r;
            this.bladeAngle = new Random().nextDouble() * 360.0;
        }

        boolean hasTurbine() {
            return turbine != null;
        }
    }

    // Ремонтный дрон
    private static class RepairDrone {
        double x, y;
        Plot targetPlot = null;
        double rotorAngle = 0.0;
        boolean isWelding = false;

        RepairDrone(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // Птица в небе
    private static class Bird {
        double x, y, speed, wingAngle;
        Bird(double x, double y, double speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.wingAngle = Math.random() * Math.PI;
        }
    }

    // Дождевая капля
    private static class RainDrop {
        int x, y, speed, len;
        RainDrop(int x, int y, int speed, int len) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.len = len;
        }
    }

    // Контракт города
    private static class CityContract {
        String title;
        double requiredKw;
        double reward;
        int durationSeconds;
        int remainingSeconds;
        int progressSeconds = 0;
        boolean active = false;
    }

    // Всплывающие тексты
    private static class FloatingText {
        double x, y;
        String text;
        Color color;
        float alpha = 1.0f;
        double vy = -1.3;

        FloatingText(double x, double y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }
    }

    // Искры
    private static class Sparkle {
        double x, y, vx, vy;
        Color color;
        float alpha = 1.0f;
        int size;

        Sparkle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            double angle = Math.random() * Math.PI * 2;
            double speed = 1.8 + Math.random() * 3.5;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            this.size = 4 + (int)(Math.random() * 5);
        }
    }

    // Облака
    private static class Cloud {
        double x, y, speed, scale;
        Cloud(double x, double y, double speed, double scale) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.scale = scale;
        }
    }

    // ==========================================
    // === 2.1 ВРЕМЕНА ГОДА И БИОМЫ
    // ==========================================
    public enum Season {
        SPRING("🌸 Весна", new Color(46, 204, 113), new Color(39, 174, 96), 1.0, 1.0, "Мягкий климат: износ снижен на 20%"),
        SUMMER("☀️ Лето", new Color(60, 179, 113), new Color(40, 140, 90), 0.95, 1.5, "Солнечный зной: гибридные турбины +50% энергии"),
        AUTUMN("🍁 Осень", new Color(205, 120, 45), new Color(145, 80, 25), 1.25, 0.9, "Сезон ветров: скорость ветра +25%"),
        WINTER("❄️ Зима", new Color(225, 238, 252), new Color(180, 205, 230), 1.1, 1.0, "Морозный пик: тариф города x1.35 за отопление");

        final String title;
        final Color groundTop;
        final Color groundBot;
        final double windFactor;
        final double solarFactor;
        final String desc;

        Season(String title, Color groundTop, Color groundBot, double windFactor, double solarFactor, String desc) {
            this.title = title;
            this.groundTop = groundTop;
            this.groundBot = groundBot;
            this.windFactor = windFactor;
            this.solarFactor = solarFactor;
            this.desc = desc;
        }
    }

    public enum Biome {
        VALLEY("🌲 Долина", "Классическая равнина, умеренный климат", 0.0, 1.0, 1.0),
        PEAKS("🏔️ Перевал", "Горы: сильный ветер (+35%), скалы", 15000.0, 1.35, 1.15),
        OCEAN("🌊 Шельф", "Океан: морской бриз, Офшор турбины +50%", 35000.0, 1.15, 1.4);

        final String title;
        final String desc;
        final double unlockCost;
        final double windModifier;
        final double incomeModifier;

        Biome(String title, String desc, double unlockCost, double windModifier, double incomeModifier) {
            this.title = title;
            this.desc = desc;
            this.unlockCost = unlockCost;
            this.windModifier = windModifier;
            this.incomeModifier = incomeModifier;
        }
    }

    private static class SnowFlake {
        double x, y, speed, size;
        double sway, swaySpeed;
        SnowFlake(double x, double y, double speed, double size) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
            this.sway = Math.random() * Math.PI * 2;
            this.swaySpeed = 0.02 + Math.random() * 0.03;
        }
    }

    private static class SmokeParticle {
        double x, y, vx, vy;
        float alpha = 0.85f;
        float size;
        boolean isFire;
        Color color;

        SmokeParticle(double x, double y, boolean isFire) {
            this.x = x;
            this.y = y;
            this.isFire = isFire;
            this.vx = (Math.random() - 0.5) * 1.8;
            this.vy = isFire ? (-1.8 - Math.random() * 2.2) : (-1.2 - Math.random() * 1.6);
            this.size = isFire ? (5 + (float)(Math.random() * 5)) : (7 + (float)(Math.random() * 9));
            if (isFire) {
                this.color = Math.random() < 0.5 ? new Color(255, 75, 40) : new Color(255, 185, 30);
            } else {
                int shade = 50 + (int)(Math.random() * 60);
                this.color = new Color(shade, shade, shade);
            }
        }
    }

    // ==========================================
    // === 3. СОСТОЯНИЕ ИГРЫ И ЭКОНОМИКА
    // ==========================================
    private double balance = 1500.0;
    private double totalEnergyGenerated = 0.0;
    private double currentPowerOutput = 0.0;
    private double incomePerSecond = 0.0;
    private int cityContractsCompleted = 0;

    // Цикл Дня и Ночи
    private double timeOfDay = 0.1;
    private int gameDays = 1;
    private int gameMonths = 1;

    // Страховка
    private boolean isInsured = false;
    private final double insuranceMonthlyCost = 10.0;
    private final double insurancePayoutPerTurbine = 800.0;

    // Инженеры
    private int engineersCount = 0;
    private final double engineerHireCost = 250.0;
    private final double engineerSalaryPerSec = 1.0;

    // Турбо-кликер
    private int comboClicks = 0;
    private double comboMultiplier = 1.0;
    private int comboDecayTimer = 0;

    // Контракты города
    private CityContract activeContract = null;
    private int contractCooldownTimer = 8;

    // Погода и торнадо
    private double windSpeed = 8.5;
    private double targetWindSpeed = 8.5;
    private String weatherCondition = "Бриз 🍃";
    private final Random random = new Random();

    private boolean tornadoActive = false;
    private double tornadoX = -100;
    private double tornadoTargetX = 0;
    private double tornadoAngle = 0;
    private int tornadoLifeFrames = 0;
    private boolean lightningFlash = false;

    // События
    private int climateSummitTimer = 0;

    // 1. 🔋 Накопители энергии и Биржа
    private double batteryCapacity = 0.0;
    private double batteryStored = 0.0;
    private double marketPricePerKwh = 0.18;
    private double marketMultiplier = 1.0;
    private int marketPeakTimer = 0;

    // 2. 🔬 Дерево исследований (R&D)
    private double sciencePoints = 0.0;
    private int techAeroLevel = 0;
    private int techCarbonLevel = 0;
    private boolean techLightningRod = false;
    private boolean techStormRadar = false;
    private boolean techDronesUnlocked = false;

    // 4. 👑 Престиж / Ребёрт (IPO)
    private int rebirthCount = 0;
    private int greenTokens = 0;

    // 5. 🤖 Ремонтные дроны
    private int droneCount = 0;
    private final List<RepairDrone> drones = new ArrayList<>();

    // 6. 🏆 Достижения
    private final Set<String> achievements = new HashSet<>();

    // 7. 🗺️ Биомы и локации
    private Biome currentBiome = Biome.VALLEY;
    private final Set<String> unlockedBiomes = new HashSet<>();

    // 8. 🍂 Времена года
    private Season currentSeason = Season.SPRING;
    private int seasonDayCounter = 0;

    // 9. 👔 Персонал и Специалисты
    private boolean hiredMeteorologist = false;
    private boolean hiredBroker = false;
    private boolean hiredChiefMechanic = false;
    private String nextWeatherCondition = "Бриз 🍃";
    private double nextTargetWind = 8.5;
    private int forecastCountdown = 14;

    // 10. 💤 QoL, Инциденты и Офлайн-прогресс
    private long lastSaveTime = 0L;
    private int rareBirdEventTimer = 0;
    private final List<SnowFlake> snowFlakes = new ArrayList<>();
    private final List<SmokeParticle> smokeParticles = new ArrayList<>();

    // Сетка участков (4x3 = 12 платформ)
    private static final int COLS = 4;
    private static final int ROWS = 3;
    private final Plot[][] plots = new Plot[COLS][ROWS];
    private Plot selectedPlot = null;

    // Анимационные списки
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Sparkle> sparkles = new ArrayList<>();
    private final List<Point> stars = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Point> windParticles = new ArrayList<>();
    private final List<Bird> birds = new ArrayList<>();
    private final List<RainDrop> rainDrops = new ArrayList<>();

    // Файл сохранения
    private static final String SAVE_FILE = "savegame.dat";

    // Форматирование чисел
    private final DecimalFormat dfMoney = new DecimalFormat("#,##0.00");
    private final DecimalFormat dfShort = new DecimalFormat("#,##0.0");

    // ==========================================
    // === 4. GUI ЭЛЕМЕНТЫ
    // ==========================================
    private GameCanvas canvas;
    private JLabel balanceVal;
    private JLabel windVal;
    private JLabel powerVal;
    private JLabel batteryVal;
    private JLabel scienceVal;
    private JLabel rebirthBadge;
    private JLabel timeBadge;
    private JLabel musicVal;
    private JLabel contractStatusLbl;

    // Нижняя консоль управления и навигация по вкладкам
    private CardLayout tabCardLayout;
    private JPanel tabCardContainer;
    private StyledButton tabBtnBuild;
    private StyledButton tabBtnMarket;
    private StyledButton tabBtnResearch;
    private StyledButton tabBtnPrestige;
    private StyledButton tabBtnLog;
    private JLabel tickerLabel;

    // Элементы вкладки "Стройка"
    private JLabel selectedPlotTitle;
    private JLabel selectedPlotSub;
    private JProgressBar selectedPlotDurabilityBar;
    private StyledButton buildBasicBtn;
    private StyledButton buildAdvBtn;
    private StyledButton buildIndBtn;
    private StyledButton buildOffshoreBtn;
    private StyledButton buildSolarBtn;
    private StyledButton buildVawtBtn;
    private StyledButton repairBtn;
    private StyledButton repairAllBtn;
    private StyledButton extinguishBtn;
    private StyledButton upgradeBtn;
    private StyledButton demolishBtn;
    private StyledButton hireEngineerBtn;
    private StyledButton insuranceToggleBtn;
    private StyledButton biomeValleyBtn;
    private StyledButton biomePeaksBtn;
    private StyledButton biomeOceanBtn;

    // Элементы вкладки "Биржа"
    private JLabel batteryStatusLbl;
    private JProgressBar batteryProgressBar;
    private JLabel marketPriceLbl;
    private JLabel forecastLbl;
    private JLabel brokerStatusLbl;
    private StyledButton buyBatteryBtn;
    private StyledButton sellBatteryBtn;

    // Элементы вкладки "Наука"
    private JLabel scienceBalanceLbl;
    private StyledButton upgAeroBtn;
    private StyledButton upgCarbonBtn;
    private StyledButton upgLightningBtn;
    private StyledButton upgRadarBtn;
    private StyledButton upgDronesUnlockBtn;
    private StyledButton buyDroneBtn;
    private StyledButton hireMeteorologistBtn;
    private StyledButton hireBrokerBtn;
    private StyledButton hireChiefMechanicBtn;

    // Элементы вкладки "Престиж & Квесты"
    private JLabel rebirthInfoLbl;
    private StyledButton rebirthBtn;
    private JPanel achievementsPanel;
    private StyledButton exportSaveBtn;
    private StyledButton importSaveBtn;
    private StyledButton saveBtn;
    private StyledButton resetBtn;

    // Журнал событий (Центр событий)
    public enum LogCategory {
        ALL("Все", "🌐", new Color(148, 163, 184)),
        FINANCE("Финансы", "💰", new Color(46, 204, 113)),
        BUILD("Стройка", "🔨", new Color(52, 152, 219)),
        WEATHER("Погода", "🌦️", new Color(241, 196, 15)),
        INCIDENT("Аварии", "🚨", new Color(231, 76, 60)),
        SCIENCE("Наука", "🔬", new Color(155, 89, 182)),
        GENERAL("Инфо", "ℹ️", new Color(0, 210, 211));

        public final String title;
        public final String icon;
        public final Color color;

        LogCategory(String title, String icon, Color color) {
            this.title = title;
            this.icon = icon;
            this.color = color;
        }

        public static LogCategory detect(String msg) {
            if (msg.contains("💰") || msg.contains("$") || msg.contains("Бирж") || msg.contains("Баланс") || 
                msg.contains("Продано") || msg.contains("Доход") || msg.contains("Контракт") || msg.contains("грант") || msg.contains("тариф")) {
                return FINANCE;
            }
            if (msg.contains("🔨") || msg.contains("Построен") || msg.contains("Улучшен") || msg.contains("Эволюция") || 
                msg.contains("Снесен") || msg.contains("Биом") || msg.contains("Долина") || msg.contains("Горы") || msg.contains("Шельф")) {
                return BUILD;
            }
            if (msg.contains("🔥") || msg.contains("ПОЖАР") || msg.contains("⚠️") || msg.contains("Поломка") || 
                msg.contains("Сломан") || msg.contains("авари") || msg.contains("Потушен") || msg.contains("Тушение") || msg.contains("ТОРНАДО")) {
                return INCIDENT;
            }
            if (msg.contains("🌦️") || msg.contains("Шторм") || msg.contains("Гроза") || msg.contains("Торнадо") || 
                msg.contains("Сезон") || msg.contains("Ветер") || msg.contains("Штиль") || msg.contains("Бриз") || msg.contains("Молния") || msg.contains("снег")) {
                return WEATHER;
            }
            if (msg.contains("🔬") || msg.contains("SP") || msg.contains("Исследован") || msg.contains("Специалист") || 
                msg.contains("Метеоролог") || msg.contains("Брокер") || msg.contains("Механик") || msg.contains("Дрон") || 
                msg.contains("Престиж") || msg.contains("IPO") || msg.contains("Rebirth") || msg.contains("R&D")) {
                return SCIENCE;
            }
            return GENERAL;
        }
    }

    public static class LogEntry {
        public final String time;
        public final LogCategory category;
        public final String message;

        public LogEntry(String time, LogCategory category, String message) {
            this.time = time;
            this.category = category;
            this.message = message;
        }
    }

    private final List<LogEntry> logEntries = new ArrayList<>();
    private LogCategory currentLogFilter = LogCategory.ALL;
    private JTextPane logTextPane;
    private JLabel logStatsLabel;
    private JLabel logSummaryStats;
    private final SimpleDateFormat logTimeFmt = new SimpleDateFormat("HH:mm:ss");

    // ==========================================
    // === 5. КОНСТРУКТОР И ИНИЦИАЛИЗАЦИЯ
    // ==========================================
    public WindTycoon() {
        super("⚡ Wind Turbine Tycoon [ULTIMATE EDITION]");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1320, 920);
        setMinimumSize(new Dimension(1150, 780));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveGame(false);
                System.exit(0);
            }
        });

        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                plots[c][r] = new Plot(c, r);
            }
        }

        for (int i = 0; i < 75; i++) {
            stars.add(new Point(random.nextInt(1280), random.nextInt(400)));
        }

        clouds.add(new Cloud(50, 50, 0.4, 1.2));
        clouds.add(new Cloud(350, 110, 0.25, 0.9));
        clouds.add(new Cloud(700, 70, 0.35, 1.1));
        clouds.add(new Cloud(950, 130, 0.2, 0.8));

        for (int i = 0; i < 35; i++) {
            windParticles.add(new Point(random.nextInt(850), random.nextInt(650)));
        }

        for (int i = 0; i < 60; i++) {
            rainDrops.add(new RainDrop(random.nextInt(1280), random.nextInt(800), 12 + random.nextInt(8), 10 + random.nextInt(10)));
            snowFlakes.add(new SnowFlake(random.nextInt(1280), random.nextInt(800), 1.2 + random.nextDouble() * 2.2, 3 + random.nextInt(4)));
        }

        birds.add(new Bird(-50, 80, 2.2));
        birds.add(new Bird(-80, 100, 2.0));
        birds.add(new Bird(-120, 75, 2.3));

        unlockedBiomes.add(Biome.VALLEY.name());

        initComponents();
        loadGame();
        checkOfflineProgress();
        setupTimers();
        MusicPlayer.initAndPlay();

        log("🎮 ДОБРО ПОЖАЛОВАТЬ В ОБНОВЛЕННЫЙ WIND TYCOON!");
        log("✨ Включены новые системы: Биомы, Времена года, Пожары, Штаб специалистов и Кросс-сохранения!");
        if (MusicPlayer.findMusicFile() != null) {
            log("🎵 Фоновая музыка обнаружена и воспроизводится!");
        } else {
            log("🎵 Музыка: закиньте любой аудиофайл в папку 'music' для фонового саундтрека!");
        }
    }

    private void initComponents() {
        // === ВЕРХНИЙ HUD (STATUS PILLS) ===
        JPanel topHud = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 7));
        topHud.setBackground(new Color(15, 19, 27));
        topHud.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));

        topHud.add(createHudPill("💰 Баланс", balanceVal = new JLabel("$1,500.00"), new Color(46, 204, 113)));
        topHud.add(createHudPill("💨 Ветер", windVal = new JLabel("8.5 м/с • Бриз 🍃"), new Color(0, 210, 211)));
        topHud.add(createHudPill("⚡ Сеть", powerVal = new JLabel("0.0 кВт"), new Color(254, 202, 87)));
        topHud.add(createHudPill("🔋 Батарея", batteryVal = new JLabel("Нет"), new Color(52, 152, 219)));
        topHud.add(createHudPill("🔬 Наука", scienceVal = new JLabel("0.0 SP"), new Color(155, 89, 182)));
        topHud.add(createHudPill("👑 Престиж", rebirthBadge = new JLabel("R0 (x1.0)"), new Color(241, 196, 15)));
        topHud.add(createHudPill("🕒 Время", timeBadge = new JLabel("☀️ День"), new Color(255, 234, 167)));

        // Кнопка включения/выключения фоновой музыки
        JPanel musicPill = createHudPill("🎵 Музыка", musicVal = new JLabel("🔊 Вкл"), new Color(100, 200, 255));
        musicPill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        musicPill.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boolean active = MusicPlayer.toggleMute();
                musicVal.setText(active ? "🔊 Вкл" : "🔇 Выкл");
                musicVal.setForeground(active ? Color.WHITE : new Color(140, 150, 165));
                SoundSystem.play("click");
            }
        });
        topHud.add(musicPill);

        add(topHud, BorderLayout.NORTH);

        // === ИГРОВОЙ КАНВАС ===
        canvas = new GameCanvas();
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleCanvasClick(e.getX(), e.getY());
            }
        });
        add(canvas, BorderLayout.CENTER);

        // === НИЖНЯЯ ПАНЕЛЬ УПРАВЛЕНИЯ (BOTTOM DECK) ===
        JPanel bottomDeck = createBottomDeck();
        add(bottomDeck, BorderLayout.SOUTH);
        updateControlButtons();
        switchTab("BUILD");
    }

    private JPanel createBottomDeck() {
        JPanel deck = new JPanel(new BorderLayout());
        deck.setPreferredSize(new Dimension(1300, 275));
        deck.setBackground(new Color(15, 18, 25));
        deck.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(38, 48, 66)));

        // 1. Верхняя панель: Переключатели вкладок + Интерактивный новостной тикер
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        // Вкладки
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabsPanel.setOpaque(false);

        tabBtnBuild = new StyledButton("🔨 СТРОИТЕЛЬСТВО", new Color(46, 204, 113), e -> switchTab("BUILD"));
        tabBtnMarket = new StyledButton("🔋 ЭНЕРГОБИРЖА", new Color(52, 152, 219), e -> switchTab("MARKET"));
        tabBtnResearch = new StyledButton("🔬 НАУКА & R&D", new Color(155, 89, 182), e -> switchTab("RESEARCH"));
        tabBtnPrestige = new StyledButton("👑 ПРЕСТИЖ & СИСТЕМА", new Color(241, 196, 15), e -> switchTab("PRESTIGE"));
        tabBtnLog = new StyledButton("📋 ЦЕНТР СОБЫТИЙ", new Color(0, 210, 211), e -> switchTab("LOG"));

        Dimension tabBtnSize = new Dimension(175, 28);
        tabBtnBuild.setPreferredSize(tabBtnSize);
        tabBtnMarket.setPreferredSize(tabBtnSize);
        tabBtnResearch.setPreferredSize(tabBtnSize);
        tabBtnPrestige.setPreferredSize(tabBtnSize);
        tabBtnLog.setPreferredSize(tabBtnSize);

        tabsPanel.add(tabBtnBuild);
        tabsPanel.add(tabBtnMarket);
        tabsPanel.add(tabBtnResearch);
        tabsPanel.add(tabBtnPrestige);
        tabsPanel.add(tabBtnLog);

        // Интерактивный тикер
        JPanel tickerBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 30, 44));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(45, 55, 75));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        tickerBar.setOpaque(false);
        tickerBar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tickerBar.setToolTipText("Нажмите, чтобы открыть подробный Центр событий (Журнал)");
        tickerBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switchTab("LOG");
            }
        });

        JLabel tickerIcon = new JLabel("📢 ПОСЛЕДНЕЕ:");
        tickerIcon.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
        tickerIcon.setForeground(new Color(254, 202, 87));

        tickerLabel = new JLabel("Добро пожаловать в Wind Turbine Tycoon!");
        tickerLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        tickerLabel.setForeground(new Color(220, 235, 255));
        tickerLabel.setPreferredSize(new Dimension(340, 20));

        tickerBar.add(tickerIcon);
        tickerBar.add(tickerLabel);

        topBar.add(tabsPanel, BorderLayout.WEST);
        topBar.add(tickerBar, BorderLayout.EAST);

        deck.add(topBar, BorderLayout.NORTH);

        // 2. Карточный контейнер вкладок
        tabCardLayout = new CardLayout();
        tabCardContainer = new JPanel(tabCardLayout);
        tabCardContainer.setOpaque(false);
        tabCardContainer.setBorder(BorderFactory.createEmptyBorder(2, 12, 8, 12));

        tabCardContainer.add(createBuildTab(), "BUILD");
        tabCardContainer.add(createMarketTab(), "MARKET");
        tabCardContainer.add(createResearchTab(), "RESEARCH");
        tabCardContainer.add(createPrestigeTab(), "PRESTIGE");
        tabCardContainer.add(createLogTab(), "LOG");

        deck.add(tabCardContainer, BorderLayout.CENTER);
        return deck;
    }

    private JPanel createCardPanel(String title, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(22, 27, 38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(40, 50, 68));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(0, 6));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        if (title != null && !title.isEmpty()) {
            JLabel header = new JLabel(title);
            header.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
            header.setForeground(accentColor != null ? accentColor : new Color(0, 210, 211));
            card.add(header, BorderLayout.NORTH);
        }
        return card;
    }

    private void switchTab(String tabName) {
        tabCardLayout.show(tabCardContainer, tabName);
        updateTabButtonStyles(tabName);
        SoundSystem.play("click");
    }

    private void updateTabButtonStyles(String activeTab) {
        if (tabBtnBuild == null) return;
        tabBtnBuild.setBaseColor("BUILD".equals(activeTab) ? new Color(46, 204, 113) : new Color(32, 42, 56));
        tabBtnMarket.setBaseColor("MARKET".equals(activeTab) ? new Color(52, 152, 219) : new Color(32, 42, 56));
        tabBtnResearch.setBaseColor("RESEARCH".equals(activeTab) ? new Color(155, 89, 182) : new Color(32, 42, 56));
        tabBtnPrestige.setBaseColor("PRESTIGE".equals(activeTab) ? new Color(241, 196, 15) : new Color(32, 42, 56));
        tabBtnLog.setBaseColor("LOG".equals(activeTab) ? new Color(0, 210, 211) : new Color(32, 42, 56));
    }

    // === ВКЛАДКА 1: СТРОИТЕЛЬСТВО & БИОМЫ ===
    private JPanel createBuildTab() {
        JPanel p = new JPanel(new GridLayout(1, 3, 10, 0));
        p.setOpaque(false);

        // Колонка 1: Выбранный участок
        JPanel col1 = createCardPanel("📍 ВЫБРАННЫЙ УЧАСТОК", new Color(0, 210, 211));
        JPanel slotContent = new JPanel();
        slotContent.setLayout(new BoxLayout(slotContent, BoxLayout.Y_AXIS));
        slotContent.setOpaque(false);

        selectedPlotTitle = new JLabel("Участок не выбран");
        selectedPlotTitle.setForeground(Color.WHITE);
        selectedPlotTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 12));

        selectedPlotSub = new JLabel("Кликните по участку на поле");
        selectedPlotSub.setForeground(new Color(160, 180, 205));
        selectedPlotSub.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));

        selectedPlotDurabilityBar = new JProgressBar(0, 100);
        selectedPlotDurabilityBar.setValue(0);
        selectedPlotDurabilityBar.setStringPainted(true);
        selectedPlotDurabilityBar.setString("0%");
        selectedPlotDurabilityBar.setForeground(new Color(46, 204, 113));
        selectedPlotDurabilityBar.setBackground(new Color(15, 20, 30));
        selectedPlotDurabilityBar.setBorderPainted(false);
        selectedPlotDurabilityBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        upgradeBtn = new StyledButton("🚀 Эволюция модели", new Color(241, 196, 15), e -> upgradeSelectedTurbine());
        upgradeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JPanel repDemoGrid = new JPanel(new GridLayout(1, 2, 5, 0));
        repDemoGrid.setOpaque(false);
        repDemoGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        repairBtn = new StyledButton("🔧 Починить ($0)", new Color(230, 126, 34), e -> repairSelectedTurbine());
        demolishBtn = new StyledButton("🗑️ Снести (50%)", new Color(192, 57, 43), e -> demolishSelectedTurbine());
        repDemoGrid.add(repairBtn);
        repDemoGrid.add(demolishBtn);

        extinguishBtn = new StyledButton("🧯 Потушить пожары ($250)", new Color(231, 76, 60), e -> extinguishAllFires());
        extinguishBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        extinguishBtn.setVisible(false);

        slotContent.add(selectedPlotTitle);
        slotContent.add(Box.createRigidArea(new Dimension(0, 3)));
        slotContent.add(selectedPlotSub);
        slotContent.add(Box.createRigidArea(new Dimension(0, 5)));
        slotContent.add(selectedPlotDurabilityBar);
        slotContent.add(Box.createRigidArea(new Dimension(0, 8)));
        slotContent.add(upgradeBtn);
        slotContent.add(Box.createRigidArea(new Dimension(0, 5)));
        slotContent.add(repDemoGrid);
        slotContent.add(Box.createRigidArea(new Dimension(0, 5)));
        slotContent.add(extinguishBtn);

        col1.add(slotContent, BorderLayout.CENTER);
        p.add(col1);

        // Колонка 2: Каталог турбин
        JPanel col2 = createCardPanel("🏭 КАТАЛОГ ВЕТРЯКОВ", new Color(46, 204, 113));
        JPanel buildGrid = new JPanel(new GridLayout(3, 2, 6, 6));
        buildGrid.setOpaque(false);

        buildBasicBtn = new StyledButton("🔨 Малый ($1k)", new Color(46, 204, 113), e -> handleBuildOrUpgrade(TurbineType.BASIC));
        buildAdvBtn = new StyledButton("⚡ Средний ($3.2k)", new Color(52, 152, 219), e -> handleBuildOrUpgrade(TurbineType.ADVANCED));
        buildIndBtn = new StyledButton("🌟 Гигант ($9k)", new Color(155, 89, 182), e -> handleBuildOrUpgrade(TurbineType.INDUSTRIAL));
        buildOffshoreBtn = new StyledButton("🌊 Офшор ($24k)", new Color(24, 119, 242), e -> handleBuildOrUpgrade(TurbineType.OFFSHORE));
        buildSolarBtn = new StyledButton("☀️ Гибрид ($55k)", new Color(243, 156, 18), e -> handleBuildOrUpgrade(TurbineType.SOLAR_HYBRID));
        buildVawtBtn = new StyledButton("🌀 VAWT ($140k)", new Color(233, 30, 99), e -> handleBuildOrUpgrade(TurbineType.QUANTUM_VAWT));

        buildGrid.add(buildBasicBtn);
        buildGrid.add(buildAdvBtn);
        buildGrid.add(buildIndBtn);
        buildGrid.add(buildOffshoreBtn);
        buildGrid.add(buildSolarBtn);
        buildGrid.add(buildVawtBtn);
        col2.add(buildGrid, BorderLayout.CENTER);
        p.add(col2);

        // Колонка 3: Обслуживание, биомы и персонал
        JPanel col3 = createCardPanel("🌐 ИНФРАСТРУКТУРА & БИОМЫ", new Color(241, 196, 15));
        JPanel infraContent = new JPanel();
        infraContent.setLayout(new BoxLayout(infraContent, BoxLayout.Y_AXIS));
        infraContent.setOpaque(false);

        repairAllBtn = new StyledButton("🔧 Починить всё ($0.00)", new Color(39, 174, 96), e -> repairAllTurbines());
        repairAllBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JPanel biomeBar = new JPanel(new GridLayout(1, 3, 4, 0));
        biomeBar.setOpaque(false);
        biomeBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        biomeValleyBtn = new StyledButton("🌲 Долина", new Color(46, 204, 113), e -> selectBiome(Biome.VALLEY));
        biomePeaksBtn = new StyledButton("🏔️ Горы", new Color(149, 165, 166), e -> selectBiome(Biome.PEAKS));
        biomeOceanBtn = new StyledButton("🌊 Шельф", new Color(52, 152, 219), e -> selectBiome(Biome.OCEAN));
        biomeBar.add(biomeValleyBtn);
        biomeBar.add(biomePeaksBtn);
        biomeBar.add(biomeOceanBtn);

        hireEngineerBtn = new StyledButton("👷 Нанять техника ($250, -$1/с)", new Color(26, 188, 156), e -> hireEngineer());
        hireEngineerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        insuranceToggleBtn = new StyledButton("📜 Оформить страховку ($10/мес)", new Color(41, 128, 185), e -> toggleInsurance());
        insuranceToggleBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        infraContent.add(repairAllBtn);
        infraContent.add(Box.createRigidArea(new Dimension(0, 6)));
        infraContent.add(biomeBar);
        infraContent.add(Box.createRigidArea(new Dimension(0, 6)));
        infraContent.add(hireEngineerBtn);
        infraContent.add(Box.createRigidArea(new Dimension(0, 6)));
        infraContent.add(insuranceToggleBtn);

        col3.add(infraContent, BorderLayout.CENTER);
        p.add(col3);

        return p;
    }

    // === ВКЛАДКА 2: ЭНЕРГОБИРЖА & КОНТРАКТЫ ===
    private JPanel createMarketTab() {
        JPanel p = new JPanel(new GridLayout(1, 3, 10, 0));
        p.setOpaque(false);

        // Колонка 1: Подстанция накопителей
        JPanel col1 = createCardPanel("🔋 ПОДСТАНЦИЯ НАКОПИТЕЛЕЙ", new Color(52, 152, 219));
        JPanel batContent = new JPanel();
        batContent.setLayout(new BoxLayout(batContent, BoxLayout.Y_AXIS));
        batContent.setOpaque(false);

        batteryStatusLbl = new JLabel("Хранилище: 0 / 0 кВт⋅ч (0%)");
        batteryStatusLbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        batteryStatusLbl.setForeground(Color.WHITE);

        batteryProgressBar = new JProgressBar(0, 100);
        batteryProgressBar.setValue(0);
        batteryProgressBar.setForeground(new Color(0, 210, 211));
        batteryProgressBar.setBackground(new Color(15, 20, 30));
        batteryProgressBar.setBorderPainted(false);
        batteryProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        buyBatteryBtn = new StyledButton("🔋 Купить батарею 1,000 кВт⋅ч ($4k)", new Color(41, 128, 185), e -> upgradeBattery());
        buyBatteryBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        sellBatteryBtn = new StyledButton("⚡ Сбросить заряд на биржу ($0.00)", new Color(230, 126, 34), e -> dumpBatteryToMarket());
        sellBatteryBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        batContent.add(batteryStatusLbl);
        batContent.add(Box.createRigidArea(new Dimension(0, 6)));
        batContent.add(batteryProgressBar);
        batContent.add(Box.createRigidArea(new Dimension(0, 10)));
        batContent.add(buyBatteryBtn);
        batContent.add(Box.createRigidArea(new Dimension(0, 6)));
        batContent.add(sellBatteryBtn);
        col1.add(batContent, BorderLayout.CENTER);
        p.add(col1);

        // Колонка 2: Биржа и контракт города
        JPanel col2 = createCardPanel("📊 БИРЖА & КОНТРАКТЫ", new Color(155, 89, 182));
        JPanel mktContent = new JPanel();
        mktContent.setLayout(new BoxLayout(mktContent, BoxLayout.Y_AXIS));
        mktContent.setOpaque(false);

        marketPriceLbl = new JLabel("Тариф: $0.18 / кВт⋅ч [Обычный]");
        marketPriceLbl.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        marketPriceLbl.setForeground(new Color(254, 202, 87));

        JLabel contractHeader = new JLabel("🏙️ ТЕНДЕР МЭРИИ ГОРОДА:");
        contractHeader.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
        contractHeader.setForeground(new Color(0, 210, 211));

        contractStatusLbl = new JLabel("Ожидание заказа мэрии...");
        contractStatusLbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        contractStatusLbl.setForeground(Color.WHITE);

        mktContent.add(marketPriceLbl);
        mktContent.add(Box.createRigidArea(new Dimension(0, 10)));
        mktContent.add(contractHeader);
        mktContent.add(Box.createRigidArea(new Dimension(0, 4)));
        mktContent.add(contractStatusLbl);
        col2.add(mktContent, BorderLayout.CENTER);
        p.add(col2);

        // Колонка 3: Прогноз погоды и ИИ-брокер
        JPanel col3 = createCardPanel("🌦️ СИТУАЦИОННЫЙ ЦЕНТР", new Color(0, 180, 216));
        JPanel fcContent = new JPanel();
        fcContent.setLayout(new BoxLayout(fcContent, BoxLayout.Y_AXIS));
        fcContent.setOpaque(false);

        forecastLbl = new JLabel("🌦️ Прогноз погоды: [Наймите метеоролога]");
        forecastLbl.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
        forecastLbl.setForeground(new Color(130, 200, 255));

        brokerStatusLbl = new JLabel("📈 ИИ-Брокер: Не нанят");
        brokerStatusLbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        brokerStatusLbl.setForeground(new Color(175, 190, 210));

        JLabel hintLbl = new JLabel("<html><font color='#94A3B8'>💡 Метеоролог предсказывает погоду, а ИИ-брокер автоматически продает заряд на пиках цен.</font></html>");
        hintLbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));

        fcContent.add(forecastLbl);
        fcContent.add(Box.createRigidArea(new Dimension(0, 8)));
        fcContent.add(brokerStatusLbl);
        fcContent.add(Box.createRigidArea(new Dimension(0, 10)));
        fcContent.add(hintLbl);
        col3.add(fcContent, BorderLayout.CENTER);
        p.add(col3);

        return p;
    }

    // === ВКЛАДКА 3: ДЕРЕВО ИССЛЕДОВАНИЙ (R&D) ===
    private JPanel createResearchTab() {
        JPanel p = new JPanel(new GridLayout(1, 3, 10, 0));
        p.setOpaque(false);

        // Колонка 1: Очки науки и базовые исследования
        JPanel col1 = createCardPanel("🔬 ЛАБОРАТОРИЯ (R&D)", new Color(155, 89, 182));
        JPanel spContent = new JPanel();
        spContent.setLayout(new BoxLayout(spContent, BoxLayout.Y_AXIS));
        spContent.setOpaque(false);

        scienceBalanceLbl = new JLabel("🔬 Научный баланс: 0.0 SP");
        scienceBalanceLbl.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        scienceBalanceLbl.setForeground(new Color(230, 180, 255));

        upgAeroBtn = new StyledButton("🍃 Аэродинамика Lv.1 (40 SP)", new Color(46, 204, 113), e -> researchAero());
        upgAeroBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        upgCarbonBtn = new StyledButton("🛡️ Углепластик Lv.1 (50 SP)", new Color(52, 152, 219), e -> researchCarbon());
        upgCarbonBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        spContent.add(scienceBalanceLbl);
        spContent.add(Box.createRigidArea(new Dimension(0, 10)));
        spContent.add(upgAeroBtn);
        spContent.add(Box.createRigidArea(new Dimension(0, 6)));
        spContent.add(upgCarbonBtn);
        col1.add(spContent, BorderLayout.CENTER);
        p.add(col1);

        // Колонка 2: Защита и Дроны
        JPanel col2 = createCardPanel("⚡ ИНЖЕНЕРНЫЕ СИСТЕМЫ", new Color(230, 126, 34));
        JPanel techContent = new JPanel();
        techContent.setLayout(new BoxLayout(techContent, BoxLayout.Y_AXIS));
        techContent.setOpaque(false);

        upgLightningBtn = new StyledButton("⚡ Громоотвод (100 SP)", new Color(241, 196, 15), e -> researchLightning());
        upgLightningBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        upgRadarBtn = new StyledButton("📡 Радар раннего оповещения (150 SP)", new Color(155, 89, 182), e -> researchRadar());
        upgRadarBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        upgDronesUnlockBtn = new StyledButton("🤖 Чертеж дронов (220 SP)", new Color(230, 126, 34), e -> researchDrones());
        upgDronesUnlockBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        buyDroneBtn = new StyledButton("🚁 Купить ремонтного дрона ($5,000)", new Color(26, 188, 156), e -> buyDrone());
        buyDroneBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        techContent.add(upgLightningBtn);
        techContent.add(Box.createRigidArea(new Dimension(0, 5)));
        techContent.add(upgRadarBtn);
        techContent.add(Box.createRigidArea(new Dimension(0, 5)));
        techContent.add(upgDronesUnlockBtn);
        techContent.add(Box.createRigidArea(new Dimension(0, 5)));
        techContent.add(buyDroneBtn);
        col2.add(techContent, BorderLayout.CENTER);
        p.add(col2);

        // Колонка 3: Специалисты
        JPanel col3 = createCardPanel("👔 ШТАБ СПЕЦИАЛИСТОВ", new Color(254, 202, 87));
        JPanel staffContent = new JPanel();
        staffContent.setLayout(new BoxLayout(staffContent, BoxLayout.Y_AXIS));
        staffContent.setOpaque(false);

        hireMeteorologistBtn = new StyledButton("🌦️ Метеоролог ($3.5k / 60 SP)", new Color(0, 180, 216), e -> hireSpecialist("METEOROLOGIST"));
        hireMeteorologistBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        hireBrokerBtn = new StyledButton("📈 ИИ-Брокер ($7k / 100 SP)", new Color(155, 89, 182), e -> hireSpecialist("BROKER"));
        hireBrokerBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        hireChiefMechanicBtn = new StyledButton("🛡️ Главный Механик ($5k / 80 SP)", new Color(243, 156, 18), e -> hireSpecialist("MECHANIC"));
        hireChiefMechanicBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        staffContent.add(hireMeteorologistBtn);
        staffContent.add(Box.createRigidArea(new Dimension(0, 5)));
        staffContent.add(hireBrokerBtn);
        staffContent.add(Box.createRigidArea(new Dimension(0, 5)));
        staffContent.add(hireChiefMechanicBtn);
        col3.add(staffContent, BorderLayout.CENTER);
        p.add(col3);

        return p;
    }

    // === ВКЛАДКА 4: ПРЕСТИЖ (REBIRTH) И СОХРАНЕНИЯ ===
    private JPanel createPrestigeTab() {
        JPanel p = new JPanel(new GridLayout(1, 3, 10, 0));
        p.setOpaque(false);

        // Колонка 1: Rebirth
        JPanel col1 = createCardPanel("👑 ВЫХОД НА IPO (REBIRTH)", new Color(241, 196, 15));
        JPanel rebContent = new JPanel();
        rebContent.setLayout(new BoxLayout(rebContent, BoxLayout.Y_AXIS));
        rebContent.setOpaque(false);

        rebirthInfoLbl = new JLabel("Бонус: +0%  |  Требуется: $100,000 и 6 вышек");
        rebirthInfoLbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        rebirthInfoLbl.setForeground(Color.WHITE);

        rebirthBtn = new StyledButton("🌟 СОВЕРШИТЬ REBIRTH (+50% доход)", new Color(241, 196, 15), e -> triggerRebirth());
        rebirthBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel rebDesc = new JLabel("<html><font color='#94A3B8'>Сброс поля и баланса в обмен на вечный множитель прибыли +50% и стартовые токены.</font></html>");
        rebDesc.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));

        rebContent.add(rebirthInfoLbl);
        rebContent.add(Box.createRigidArea(new Dimension(0, 8)));
        rebContent.add(rebirthBtn);
        rebContent.add(Box.createRigidArea(new Dimension(0, 8)));
        rebContent.add(rebDesc);
        col1.add(rebContent, BorderLayout.CENTER);
        p.add(col1);

        // Колонка 2: Достижения
        JPanel col2 = createCardPanel("🏆 ДОСТИЖЕНИЯ ТАЙКУНА", new Color(46, 204, 113));
        achievementsPanel = new JPanel(new GridLayout(4, 2, 4, 3));
        achievementsPanel.setOpaque(false);
        col2.add(achievementsPanel, BorderLayout.CENTER);
        updateAchievementsView();
        p.add(col2);

        // Колонка 3: Сохранения
        JPanel col3 = createCardPanel("💾 УПРАВЛЕНИЕ ДАННЫМИ", new Color(52, 152, 219));
        JPanel saveGrid = new JPanel(new GridLayout(2, 2, 6, 6));
        saveGrid.setOpaque(false);

        saveBtn = new StyledButton("💾 Сохранить", new Color(46, 204, 113), e -> saveGame(true));
        resetBtn = new StyledButton("🔄 Сброс", new Color(192, 57, 43), e -> resetGame());
        exportSaveBtn = new StyledButton("📋 Экспорт (Base64)", new Color(52, 152, 219), e -> exportSaveCode());
        importSaveBtn = new StyledButton("📥 Импорт (Код)", new Color(155, 89, 182), e -> importSaveCode());

        saveGrid.add(saveBtn);
        saveGrid.add(resetBtn);
        saveGrid.add(exportSaveBtn);
        saveGrid.add(importSaveBtn);
        col3.add(saveGrid, BorderLayout.CENTER);
        p.add(col3);

        return p;
    }

    // === ВКЛАДКА 5: ЦЕНТР СОБЫТИЙ И ЖУРНАЛ ОПЕРАТОРА ===
    private JPanel createLogTab() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);

        // Левая панель: фильтры и действия
        JPanel leftPanel = createCardPanel("🔍 ФИЛЬТРЫ И ДЕЙСТВИЯ", new Color(0, 210, 211));
        leftPanel.setPreferredSize(new Dimension(310, 200));

        JPanel filterControls = new JPanel();
        filterControls.setLayout(new BoxLayout(filterControls, BoxLayout.Y_AXIS));
        filterControls.setOpaque(false);

        JPanel catGrid = new JPanel(new GridLayout(3, 2, 5, 5));
        catGrid.setOpaque(false);

        for (LogCategory cat : LogCategory.values()) {
            StyledButton btn = new StyledButton(cat.icon + " " + cat.title, cat.color, e -> setLogFilter(cat));
            btn.setPreferredSize(new Dimension(135, 24));
            catGrid.add(btn);
        }
        filterControls.add(catGrid);
        filterControls.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel actionGrid = new JPanel(new GridLayout(1, 2, 5, 0));
        actionGrid.setOpaque(false);
        StyledButton clearBtn = new StyledButton("🧹 Очистить", new Color(231, 76, 60), e -> clearLog());
        StyledButton copyBtn = new StyledButton("📋 Копировать", new Color(41, 128, 185), e -> copyLogToClipboard());
        actionGrid.add(clearBtn);
        actionGrid.add(copyBtn);
        filterControls.add(actionGrid);

        filterControls.add(Box.createRigidArea(new Dimension(0, 8)));
        logSummaryStats = new JLabel("Всего записей: 0");
        logSummaryStats.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        logSummaryStats.setForeground(new Color(148, 163, 184));
        filterControls.add(logSummaryStats);

        leftPanel.add(filterControls, BorderLayout.CENTER);
        p.add(leftPanel, BorderLayout.WEST);

        // Правая панель: лента событий
        JPanel rightPanel = createCardPanel("📋 ЛЕНТА СОБЫТИЙ ОПЕРАТОРА", new Color(148, 163, 184));
        logTextPane = new JTextPane();
        logTextPane.setContentType("text/html");
        logTextPane.setEditable(false);
        logTextPane.setBackground(new Color(15, 18, 25));

        JScrollPane logScroll = new JScrollPane(logTextPane);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(38, 48, 66)));
        logScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rightPanel.add(logScroll, BorderLayout.CENTER);

        p.add(rightPanel, BorderLayout.CENTER);

        refreshLogDisplay();
        return p;
    }

    private void updateAchievementsView() {
        if (achievementsPanel == null) return;
        achievementsPanel.removeAll();

        String[][] allAch = {
                {"FIRST_TURBINE", "🌱 Первый ток"},
                {"HIGH_ROLLER", "💰 $50k Баланс"},
                {"SURVIVOR", "🌪️ Смерч пройден"},
                {"SCIENCE", "🔬 Инноватор"},
                {"DRONE_ERA", "🤖 Эра дронов"},
                {"BATTERY_MASTER", "🔋 Энергобанк"},
                {"CONTRACT_ACE", "🏙️ Мега-контракт"},
                {"REBIRTH_HERO", "👑 Легенда IPO"}
        };

        for (String[] ach : allAch) {
            boolean unlocked = achievements.contains(ach[0]);
            JLabel lbl = new JLabel((unlocked ? "✅ " : "🔒 ") + ach[1]);
            lbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
            lbl.setForeground(unlocked ? new Color(46, 204, 113) : new Color(120, 135, 155));
            achievementsPanel.add(lbl);
        }
        achievementsPanel.revalidate();
        achievementsPanel.repaint();
    }

    private void unlockAchievement(String id, String title, double reward) {
        if (!achievements.contains(id)) {
            achievements.add(id);
            balance += reward;
            SoundSystem.play("quest");
            log("🏆 ДОСТИЖЕНИЕ: '" + title + "'! Награда: +$" + dfMoney.format(reward));
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 3, "🏆 " + title + " +$" + (int)reward, new Color(254, 202, 87)));
            spawnSparkles(canvas.getWidth() / 2, canvas.getHeight() / 3, new Color(254, 202, 87));
            updateAchievementsView();
        }
    }

    private JPanel createHudPill(String title, JLabel valueLabel, Color accentColor) {
        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(28, 34, 46));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(46, 56, 74));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(Font.DIALOG, Font.BOLD, 9));
        titleLbl.setForeground(accentColor);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        pill.add(titleLbl);
        pill.add(Box.createRigidArea(new Dimension(0, 1)));
        pill.add(valueLabel);
        return pill;
    }

    // Стилизованная неоновая кнопка
    private static class StyledButton extends JButton {
        private Color baseColor;

        public StyledButton(String text, Color baseColor, ActionListener al) {
            super(text);
            this.baseColor = baseColor;
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(new Font(Font.DIALOG, Font.BOLD, 10));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(100, 26));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addActionListener(al);
        }

        public void setBaseColor(Color c) {
            this.baseColor = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int bottomShadow = 3;
            boolean pressed = getModel().isPressed();
            boolean rollover = getModel().isRollover();

            int yOffset = pressed ? 2 : 0;
            int drawH = h - bottomShadow;

            if (!isEnabled()) {
                g2.setColor(new Color(34, 40, 52));
                g2.fillRoundRect(0, 0, w, h - 2, 7, 7);
                g2.setColor(new Color(48, 56, 70));
                g2.drawRoundRect(0, 0, w - 1, h - 3, 7, 7);
                setForeground(new Color(115, 128, 145));
            } else {
                setForeground(Color.WHITE);
                Color shadowColor = new Color(
                        Math.max(0, baseColor.getRed() - 60),
                        Math.max(0, baseColor.getGreen() - 60),
                        Math.max(0, baseColor.getBlue() - 60)
                );
                g2.setColor(shadowColor);
                g2.fillRoundRect(0, yOffset + 2, w, drawH, 7, 7);

                Color faceColor = baseColor;
                if (rollover && !pressed) {
                    faceColor = new Color(
                            Math.min(255, baseColor.getRed() + 25),
                            Math.min(255, baseColor.getGreen() + 25),
                            Math.min(255, baseColor.getBlue() + 25)
                    );
                }
                g2.setColor(faceColor);
                g2.fillRoundRect(0, yOffset, w, drawH, 7, 7);

                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(2, yOffset + 2, w - 4, drawH / 2 - 2, 5, 5);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void setLogFilter(LogCategory cat) {
        currentLogFilter = cat;
        refreshLogDisplay();
        SoundSystem.play("click");
    }

    private void copyLogToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (LogEntry e : logEntries) {
            sb.append("[").append(e.time).append("] [").append(e.category.title).append("] ").append(e.message).append("\n");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        SoundSystem.play("click");
        if (tickerLabel != null) {
            tickerLabel.setText("✅ Журнал скопирован в буфер обмена (" + logEntries.size() + " записей)");
        }
    }

    private void clearLog() {
        logEntries.clear();
        refreshLogDisplay();
        SoundSystem.play("click");
    }

    private void refreshLogDisplay() {
        if (logTextPane == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='background-color:#0F1219; color:#E2E8F0; font-family:sans-serif; font-size:11px; margin:4px;'>");

        int financeCount = 0, buildCount = 0, weatherCount = 0, incidentCount = 0, scienceCount = 0;
        for (LogEntry e : logEntries) {
            switch (e.category) {
                case FINANCE: financeCount++; break;
                case BUILD: buildCount++; break;
                case WEATHER: weatherCount++; break;
                case INCIDENT: incidentCount++; break;
                case SCIENCE: scienceCount++; break;
                default: break;
            }
        }

        List<LogEntry> filtered = new ArrayList<>();
        for (LogEntry e : logEntries) {
            if (currentLogFilter == LogCategory.ALL || e.category == currentLogFilter) {
                filtered.add(e);
            }
        }

        int start = Math.max(0, filtered.size() - 150);
        for (int i = start; i < filtered.size(); i++) {
            LogEntry e = filtered.get(i);
            String hex = String.format("#%02x%02x%02x", e.category.color.getRed(), e.category.color.getGreen(), e.category.color.getBlue());
            sb.append("<div style='margin-bottom:3px; line-height:1.25;'>")
              .append("<span style='color:#64748B;'>[").append(e.time).append("]</span> ")
              .append("<span style='color:").append(hex).append("; font-weight:bold;'>")
              .append(e.category.icon).append(" ").append(e.category.title).append(":</span> ")
              .append("<span>").append(escapeHtml(e.message)).append("</span>")
              .append("</div>");
        }

        sb.append("</body></html>");
        logTextPane.setText(sb.toString());
        logTextPane.setCaretPosition(logTextPane.getDocument().getLength());

        if (logSummaryStats != null) {
            logSummaryStats.setText("<html>Всего: <b>" + logEntries.size() + "</b> (фильтр: " + currentLogFilter.title + ")<br>" +
                    "💰: " + financeCount + " &nbsp;🔨: " + buildCount + " &nbsp;🌦️: " + weatherCount + "<br>" +
                    "🚨: " + incidentCount + " &nbsp;🔬: " + scienceCount + "</html>");
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void log(String msg) {
        log(LogCategory.detect(msg), msg);
    }

    private void log(LogCategory cat, String msg) {
        String time = logTimeFmt.format(new Date());
        LogEntry entry = new LogEntry(time, cat, msg);
        logEntries.add(entry);
        if (logEntries.size() > 500) {
            logEntries.remove(0);
        }

        if (tickerLabel != null) {
            tickerLabel.setText(cat.icon + " " + msg);
            tickerLabel.setToolTipText("[" + time + "] " + msg);
        }

        refreshLogDisplay();
    }

    // ==========================================
    // === 6. СОХРАНЕНИЕ И ЗАГРУЗКА
    // ==========================================
    private void writeSaveData(PrintWriter pw) {
        pw.println("balance=" + balance);
        pw.println("engineers=" + engineersCount);
        pw.println("isInsured=" + isInsured);
        pw.println("totalEnergy=" + totalEnergyGenerated);
        pw.println("gameDays=" + gameDays);
        pw.println("gameMonths=" + gameMonths);
        pw.println("cityContractsCompleted=" + cityContractsCompleted);

        pw.println("batteryCapacity=" + batteryCapacity);
        pw.println("batteryStored=" + batteryStored);
        pw.println("sciencePoints=" + sciencePoints);
        pw.println("techAeroLevel=" + techAeroLevel);
        pw.println("techCarbonLevel=" + techCarbonLevel);
        pw.println("techLightningRod=" + techLightningRod);
        pw.println("techStormRadar=" + techStormRadar);
        pw.println("techDronesUnlocked=" + techDronesUnlocked);
        pw.println("rebirthCount=" + rebirthCount);
        pw.println("greenTokens=" + greenTokens);
        pw.println("droneCount=" + droneCount);

        pw.println("currentBiome=" + currentBiome.name());
        StringBuilder sbBiomes = new StringBuilder();
        for (String b : unlockedBiomes) {
            if (sbBiomes.length() > 0) sbBiomes.append(";");
            sbBiomes.append(b);
        }
        pw.println("unlockedBiomes=" + sbBiomes.toString());

        pw.println("currentSeason=" + currentSeason.name());
        pw.println("seasonDayCounter=" + seasonDayCounter);

        pw.println("hiredMeteorologist=" + hiredMeteorologist);
        pw.println("hiredBroker=" + hiredBroker);
        pw.println("hiredChiefMechanic=" + hiredChiefMechanic);

        pw.println("lastSaveTime=" + System.currentTimeMillis());

        StringBuilder sbAch = new StringBuilder();
        for (String ach : achievements) {
            if (sbAch.length() > 0) sbAch.append(";");
            sbAch.append(ach);
        }
        pw.println("achievements=" + sbAch.toString());

        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.hasTurbine()) {
                    pw.println("plot=" + c + "," + r + "," + p.turbine.name() + "," + p.level + "," + p.durability + "," + p.isBurning);
                }
            }
        }
    }

    private void readSaveData(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("balance=")) {
                balance = Double.parseDouble(line.substring(8));
            } else if (line.startsWith("engineers=")) {
                engineersCount = Integer.parseInt(line.substring(10));
            } else if (line.startsWith("isInsured=")) {
                isInsured = Boolean.parseBoolean(line.substring(10));
            } else if (line.startsWith("totalEnergy=")) {
                totalEnergyGenerated = Double.parseDouble(line.substring(12));
            } else if (line.startsWith("gameDays=")) {
                gameDays = Integer.parseInt(line.substring(9));
            } else if (line.startsWith("gameMonths=")) {
                gameMonths = Integer.parseInt(line.substring(11));
            } else if (line.startsWith("cityContractsCompleted=")) {
                cityContractsCompleted = Integer.parseInt(line.substring(23));
            } else if (line.startsWith("batteryCapacity=")) {
                batteryCapacity = Double.parseDouble(line.substring(16));
            } else if (line.startsWith("batteryStored=")) {
                batteryStored = Double.parseDouble(line.substring(14));
            } else if (line.startsWith("sciencePoints=")) {
                sciencePoints = Double.parseDouble(line.substring(14));
            } else if (line.startsWith("techAeroLevel=")) {
                techAeroLevel = Integer.parseInt(line.substring(14));
            } else if (line.startsWith("techCarbonLevel=")) {
                techCarbonLevel = Integer.parseInt(line.substring(16));
            } else if (line.startsWith("techLightningRod=")) {
                techLightningRod = Boolean.parseBoolean(line.substring(17));
            } else if (line.startsWith("techStormRadar=")) {
                techStormRadar = Boolean.parseBoolean(line.substring(15));
            } else if (line.startsWith("techDronesUnlocked=")) {
                techDronesUnlocked = Boolean.parseBoolean(line.substring(19));
            } else if (line.startsWith("rebirthCount=")) {
                rebirthCount = Integer.parseInt(line.substring(13));
            } else if (line.startsWith("greenTokens=")) {
                greenTokens = Integer.parseInt(line.substring(12));
            } else if (line.startsWith("droneCount=")) {
                droneCount = Integer.parseInt(line.substring(11));
                syncDronesList();
            } else if (line.startsWith("currentBiome=")) {
                try { currentBiome = Biome.valueOf(line.substring(13)); } catch (Exception ignored) {}
            } else if (line.startsWith("unlockedBiomes=")) {
                String[] ub = line.substring(15).split(";");
                for (String b : ub) { if (!b.isEmpty()) unlockedBiomes.add(b); }
            } else if (line.startsWith("currentSeason=")) {
                try { currentSeason = Season.valueOf(line.substring(14)); } catch (Exception ignored) {}
            } else if (line.startsWith("seasonDayCounter=")) {
                seasonDayCounter = Integer.parseInt(line.substring(17));
            } else if (line.startsWith("hiredMeteorologist=")) {
                hiredMeteorologist = Boolean.parseBoolean(line.substring(19));
            } else if (line.startsWith("hiredBroker=")) {
                hiredBroker = Boolean.parseBoolean(line.substring(12));
            } else if (line.startsWith("hiredChiefMechanic=")) {
                hiredChiefMechanic = Boolean.parseBoolean(line.substring(19));
            } else if (line.startsWith("lastSaveTime=")) {
                lastSaveTime = Long.parseLong(line.substring(13));
            } else if (line.startsWith("achievements=")) {
                String[] achs = line.substring(13).split(";");
                for (String a : achs) {
                    if (!a.isEmpty()) achievements.add(a);
                }
            } else if (line.startsWith("plot=")) {
                String[] parts = line.substring(5).split(",");
                if (parts.length >= 5) {
                    int c = Integer.parseInt(parts[0]);
                    int r = Integer.parseInt(parts[1]);
                    TurbineType t = TurbineType.valueOf(parts[2]);
                    int lvl = Integer.parseInt(parts[3]);
                    double dur = Double.parseDouble(parts[4]);

                    if (c >= 0 && c < COLS && r >= 0 && r < ROWS) {
                        plots[c][r].turbine = t;
                        plots[c][r].level = lvl;
                        plots[c][r].durability = dur;
                        if (parts.length >= 6) {
                            plots[c][r].isBurning = Boolean.parseBoolean(parts[5]);
                        }
                    }
                }
            }
        }
    }

    private void saveGame(boolean notifyUser) {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(SAVE_FILE), StandardCharsets.UTF_8))) {
            writeSaveData(pw);
            if (notifyUser) {
                SoundSystem.play("coin");
                log("💾 Игра успешно сохранена в " + SAVE_FILE + "!");
                floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2, "💾 СОХРАНЕНО!", new Color(46, 204, 113)));
            }
        } catch (Exception ex) {
            log("⚠️ Ошибка сохранения: " + ex.getMessage());
        }
    }

    private void loadGame() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            readSaveData(br);
            log("💾 Сохранение успешно загружено! Баланс: $" + dfMoney.format(balance));
            updateControlButtons();
            updateAchievementsView();
            updateUI();
        } catch (Exception ex) {
            log("⚠️ Ошибка при чтении сохранения: " + ex.getMessage());
        }
    }

    private void checkOfflineProgress() {
        if (lastSaveTime <= 0) return;
        long elapsedSec = (System.currentTimeMillis() - lastSaveTime) / 1000;
        if (elapsedSec < 15) return;
        long cappedSec = Math.min(elapsedSec, 8 * 3600); // capped at 8 hours

        double baseIncome = 0.0;
        double basePower = 0.0;
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.hasTurbine() && p.durability > 10.0 && !p.isBurning) {
                    baseIncome += p.turbine.baseIncomePerSec * p.level;
                    basePower += p.turbine.maxPowerKw * p.level;
                }
            }
        }
        if (baseIncome <= 0) return;

        double rebirthMult = 1.0 + (rebirthCount * 0.5);
        double offlineEarnings = baseIncome * 0.45 * cappedSec * rebirthMult;
        double offlineEnergy = (basePower * 0.45 * cappedSec) / 3600.0;

        balance += offlineEarnings;
        totalEnergyGenerated += offlineEnergy;

        long hours = elapsedSec / 3600;
        long mins = (elapsedSec % 3600) / 60;
        String timeStr = (hours > 0 ? hours + " ч " : "") + mins + " мин";

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "<html><body style='width: 320px; font-family: sans-serif; background-color: #1a2230; color: #ffffff; padding: 10px;'>" +
                    "<h2 style='color: #00d2d3; margin-top:0;'>👋 С возвращением!</h2>" +
                    "<p>Пока вас не было (<b>" + timeStr + "</b>), ваш ветропарк продолжал вырабатывать чистую энергию!</p>" +
                    "<hr style='border: 1px solid #2e3d52;'>" +
                    "<p>⚡ Выработано: <b style='color: #00e5ff;'>" + dfShort.format(offlineEnergy) + " кВт⋅ч</b></p>" +
                    "<p>💰 Получено чистой прибыли: <b style='color: #2ecc71;'>+$" + dfMoney.format(offlineEarnings) + "</b></p>" +
                    "</body></html>",
                    "⚡ Wind Tycoon: Офлайн-прибыль",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        log("👋 ОФЛАЙН-ПРОГРЕСС: За время отсутствия (" + timeStr + ") начислено +$" + dfMoney.format(offlineEarnings) + "!");
    }

    private void exportSaveCode() {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            writeSaveData(pw);
            pw.flush();
            String plain = sw.toString();
            String b64 = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

            StringSelection sel = new StringSelection(b64);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);

            JTextArea ta = new JTextArea(b64);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setEditable(false);
            ta.setRows(6);
            ta.setColumns(35);
            JScrollPane sp = new JScrollPane(ta);

            JOptionPane.showMessageDialog(this, sp,
                    "📋 Код сохранения скопирован в буфер обмена!\nВы можете использовать его на Android или другом ПК:",
                    JOptionPane.INFORMATION_MESSAGE);
            log("📋 Экспорт сохранения завершен! Код скопирован в буфер обмена.");
        } catch (Exception ex) {
            log("⚠️ Ошибка экспорта сохранения: " + ex.getMessage());
        }
    }

    private void importSaveCode() {
        String input = JOptionPane.showInputDialog(this,
                "Вставьте код сохранения (Base64) для импорта:",
                "📥 Импорт сохранения", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            byte[] bytes = Base64.getDecoder().decode(input.trim());
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(new StringReader(decoded));
            readSaveData(br);
            log("✅ Сохранение успешно импортировано! Приятной игры!");
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2, "✅ СЕЙВ ИМПОРТИРОВАН!", new Color(46, 204, 113)));
            SoundSystem.play("quest");
            updateControlButtons();
            updateAchievementsView();
            updateUI();
            canvas.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось прочитать код сохранения! Убедитесь, что скопировали его полностью.",
                    "Ошибка импорта", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectBiome(Biome b) {
        if (currentBiome == b) return;
        if (!unlockedBiomes.contains(b.name())) {
            if (balance < b.unlockCost) {
                JOptionPane.showMessageDialog(this,
                        "Для открытия биома '" + b.title + "' требуется $" + dfMoney.format(b.unlockCost) + "!\n" + b.desc,
                        "Открытие биома", JOptionPane.WARNING_MESSAGE);
                return;
            }
            balance -= b.unlockCost;
            unlockedBiomes.add(b.name());
            SoundSystem.play("quest");
            log("🗺️ ОТКРЫТ НОВЫЙ БИОМ: " + b.title + "! " + b.desc);
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 3, "🗺️ " + b.title + "!", new Color(46, 204, 113)));
        }

        currentBiome = b;
        SoundSystem.play("click");
        log("🗺️ Вы переместились на локацию: " + b.title + " (" + b.desc + ")");
        updateControlButtons();
        updateUI();
        canvas.repaint();
    }

    private void repairAllTurbines() {
        double totalCost = 0.0;
        int count = 0;
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.hasTurbine() && (p.durability < 99.5 || p.isBurning)) {
                    count++;
                    double need = (100.0 - p.durability) / 100.0;
                    totalCost += p.turbine.cost * 0.15 * need;
                }
            }
        }
        totalCost *= 0.85; // 15% оптовая скидка
        if (count == 0) return;
        if (balance < totalCost) {
            JOptionPane.showMessageDialog(this, "Нужно $" + dfMoney.format(totalCost) + " для массового ремонта всех турбин!", "Ремонт", JOptionPane.WARNING_MESSAGE);
            return;
        }

        balance -= totalCost;
        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;
        int groundY = (int)(canvas.getHeight() * 0.12);

        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.hasTurbine() && (p.durability < 100.0 || p.isBurning)) {
                    p.durability = 100.0;
                    p.isBurning = false;
                    spawnSparkles(c * cellW + cellW / 2, groundY + r * cellH + 30, new Color(46, 204, 113));
                }
            }
        }

        SoundSystem.play("build");
        log("🔧 МАССОВЫЙ РЕМОНТ: Отремонтировано " + count + " турбин со скидкой 15% за $" + dfMoney.format(totalCost) + "!");
        floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 3, "🔧 ВСЕ ВЕТРЯКИ ПОЧИНЕНЫ!", new Color(46, 204, 113)));
        updateControlButtons();
        updateUI();
        canvas.repaint();
    }

    private void extinguishAllFires() {
        int burningCount = 0;
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (plots[c][r].isBurning) burningCount++;
            }
        }
        if (burningCount == 0) return;
        double cost = 250.0;
        if (balance < cost) {
            JOptionPane.showMessageDialog(this, "Нужно $250 для вызова пожарного расчета!", "Пожар", JOptionPane.WARNING_MESSAGE);
            return;
        }

        balance -= cost;
        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;
        int groundY = (int)(canvas.getHeight() * 0.12);

        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.isBurning) {
                    p.isBurning = false;
                    p.durability = Math.max(25.0, p.durability);
                    spawnSparkles(c * cellW + cellW / 2, groundY + r * cellH + 25, new Color(100, 200, 255));
                }
            }
        }

        SoundSystem.play("build");
        log("🧯 Пожарная служба ликвидировала возгорания на всех турбинах!");
        floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 3, "🧯 ПОЖАР ЛИКВИДИРОВАН!", new Color(52, 152, 219)));
        updateControlButtons();
        updateUI();
        canvas.repaint();
    }

    private void hireSpecialist(String type) {
        if ("METEOROLOGIST".equals(type)) {
            if (hiredMeteorologist) return;
            double money = 3500.0;
            double sp = 60.0;
            if (balance < money || sciencePoints < sp) {
                JOptionPane.showMessageDialog(this, "Нужно $3,500 и 60 SP для найма Метеоролога!", "Штаб", JOptionPane.WARNING_MESSAGE);
                return;
            }
            balance -= money;
            sciencePoints -= sp;
            hiredMeteorologist = true;
            SoundSystem.play("research");
            log("🌦️ Нанят Метеоролог (Dr. Ветров)! Теперь доступен точный прогноз погоды и предупреждение о бурях.");
        } else if ("BROKER".equals(type)) {
            if (hiredBroker) return;
            double money = 7000.0;
            double sp = 100.0;
            if (balance < money || sciencePoints < sp) {
                JOptionPane.showMessageDialog(this, "Нужно $7,000 и 100 SP для найма ИИ-Брокера!", "Штаб", JOptionPane.WARNING_MESSAGE);
                return;
            }
            balance -= money;
            sciencePoints -= sp;
            hiredBroker = true;
            SoundSystem.play("market");
            log("📈 Нанят ИИ-Брокер (Quantum Trader)! Батареи автоматически сбрасываются при пиковых тарифах.");
        } else if ("MECHANIC".equals(type)) {
            if (hiredChiefMechanic) return;
            double money = 5000.0;
            double sp = 80.0;
            if (balance < money || sciencePoints < sp) {
                JOptionPane.showMessageDialog(this, "Нужно $5,000 и 80 SP для найма Главного Механика!", "Штаб", JOptionPane.WARNING_MESSAGE);
                return;
            }
            balance -= money;
            sciencePoints -= sp;
            hiredChiefMechanic = true;
            SoundSystem.play("build");
            log("🛡️ Нанят Главный Механик (Мастер Петро)! Износ турбин снижен на 30%, шанс 35% спасения от аварии.");
        }
        updateControlButtons();
        updateUI();
    }

    private void syncDronesList() {
        while (drones.size() < droneCount) {
            drones.add(new RepairDrone(50 + random.nextInt(700), 50 + random.nextInt(300)));
        }
    }

    private void resetGame() {
        int ans = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите сбросить прогресс и начать заново?",
                "Сброс игры", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (ans == JOptionPane.YES_OPTION) {
            File f = new File(SAVE_FILE);
            if (f.exists()) f.delete();

            balance = 1500.0;
            engineersCount = 0;
            isInsured = false;
            totalEnergyGenerated = 0.0;
            gameDays = 1;
            gameMonths = 1;
            cityContractsCompleted = 0;

            batteryCapacity = 0.0;
            batteryStored = 0.0;
            sciencePoints = 0.0;
            techAeroLevel = 0;
            techCarbonLevel = 0;
            techLightningRod = false;
            techStormRadar = false;
            techDronesUnlocked = false;
            rebirthCount = 0;
            greenTokens = 0;
            droneCount = 0;
            drones.clear();
            achievements.clear();

            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    plots[c][r].turbine = null;
                    plots[c][r].level = 1;
                    plots[c][r].durability = 100.0;
                }
            }
            selectedPlot = null;
            log("🔄 Прогресс сброшен. Начата новая игра!");
            updateControlButtons();
            updateAchievementsView();
            updateUI();
            canvas.repaint();
        }
    }

    // ==========================================
    // === 7. ТАЙМЕРЫ И ИГРОВАЯ ЛОГИКА
    // ==========================================
    private void setupTimers() {
        Timer renderTimer = new Timer(16, e -> {
            updateVisuals();
            canvas.repaint();
        });
        renderTimer.start();

        Timer logicTimer = new Timer(1000, e -> gameLogicTick());
        logicTimer.start();

        Timer weatherTimer = new Timer(14000, e -> updateWeatherEvent());
        weatherTimer.start();

        Timer marketTimer = new Timer(8000, e -> updateMarketFluctuations());
        marketTimer.start();

        Timer autoSaveTimer = new Timer(20000, e -> saveGame(false));
        autoSaveTimer.start();
    }

    private void updateVisuals() {
        timeOfDay = (timeOfDay + 0.00025) % 1.0;
        windSpeed += (targetWindSpeed - windSpeed) * 0.02;
        double baseSpeed = (windSpeed / 12.0) * 5.0;

        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.hasTurbine() && p.durability > 0) {
                    p.bladeAngle = (p.bladeAngle + baseSpeed * comboMultiplier) % 360.0;
                }
            }
        }

        if (tornadoActive) {
            tornadoX += (tornadoTargetX - tornadoX) * 0.025;
            tornadoAngle += 15.0;
            tornadoLifeFrames++;

            if (tornadoLifeFrames > 220) {
                tornadoTargetX = canvas.getWidth() + 200;
                if (tornadoX > canvas.getWidth() + 150) {
                    tornadoActive = false;
                    lightningFlash = false;
                }
            }
        }

        int w = canvas.getWidth();
        int h = canvas.getHeight();

        for (Cloud cl : clouds) {
            cl.x += cl.speed * (windSpeed / 6.0);
            if (cl.x > w + 120) cl.x = -150;
        }

        if (w > 0 && h > 0) {
            for (Point pt : windParticles) {
                pt.x += Math.max(2, (int)(windSpeed * 0.9));
                if (pt.x > w) {
                    pt.x = -random.nextInt(60);
                    pt.y = random.nextInt(h);
                }
            }

            if (weatherCondition.contains("Гроза") || tornadoActive) {
                for (RainDrop rd : rainDrops) {
                    rd.y += rd.speed;
                    rd.x += (int)(windSpeed * 0.5);
                    if (rd.y > h) {
                        rd.y = -15;
                        rd.x = random.nextInt(w);
                    }
                }
            }

            for (Bird b : birds) {
                b.x += b.speed + (windSpeed * 0.1);
                b.wingAngle += 0.25;
                if (b.x > w + 60) {
                    b.x = -50 - random.nextInt(100);
                    b.y = 50 + random.nextInt(160);
                }
            }

            if (currentSeason == Season.WINTER) {
                for (SnowFlake sf : snowFlakes) {
                    sf.y += sf.speed;
                    sf.sway += sf.swaySpeed;
                    sf.x += Math.sin(sf.sway) * 1.2 + (windSpeed * 0.15);
                    if (sf.y > h) {
                        sf.y = -10;
                        sf.x = random.nextInt(w);
                    }
                }
            }

            int cellW_vis = w / COLS;
            int cellH_vis = (int)(h * 0.72) / ROWS;
            int groundY_vis = (int)(h * 0.12);
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    Plot p = plots[c][r];
                    if (p.hasTurbine() && p.isBurning && cellW_vis > 0) {
                        double px = c * cellW_vis + cellW_vis / 2.0;
                        double py = groundY_vis + r * cellH_vis + (cellH_vis * 0.15);
                        smokeParticles.add(new SmokeParticle(px, py, true));
                        if (Math.random() < 0.6) {
                            smokeParticles.add(new SmokeParticle(px, py, false));
                        }
                    }
                }
            }

            Iterator<SmokeParticle> itSm = smokeParticles.iterator();
            while (itSm.hasNext()) {
                SmokeParticle sm = itSm.next();
                sm.x += sm.vx + (windSpeed * 0.05);
                sm.y += sm.vy;
                sm.alpha -= 0.03f;
                if (sm.alpha <= 0.0f) itSm.remove();
            }
        }

        updateDronesPhysics();

        if (comboDecayTimer > 0) {
            comboDecayTimer--;
        } else if (comboClicks > 0) {
            comboClicks--;
            updateComboMultiplier();
        }

        Iterator<FloatingText> itText = floatingTexts.iterator();
        while (itText.hasNext()) {
            FloatingText ft = itText.next();
            ft.y += ft.vy;
            ft.alpha -= 0.015f;
            if (ft.alpha <= 0.0f) itText.remove();
        }

        Iterator<Sparkle> itSp = sparkles.iterator();
        while (itSp.hasNext()) {
            Sparkle sp = itSp.next();
            sp.x += sp.vx;
            sp.y += sp.vy;
            sp.vy += 0.08;
            sp.alpha -= 0.025f;
            if (sp.alpha <= 0.0f) itSp.remove();
        }
    }

    private void updateDronesPhysics() {
        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;
        int groundY = (int)(canvas.getHeight() * 0.12);

        for (RepairDrone drone : drones) {
            drone.rotorAngle = (drone.rotorAngle + 35.0) % 360.0;

            if (drone.targetPlot == null || (drone.targetPlot.durability >= 99.5 && !drone.targetPlot.isBurning) || !drone.targetPlot.hasTurbine()) {
                drone.targetPlot = null;
                drone.isWelding = false;

                // Приоритет 1: горящие турбины
                for (int c = 0; c < COLS; c++) {
                    for (int r = 0; r < ROWS; r++) {
                        Plot p = plots[c][r];
                        if (p.hasTurbine() && p.isBurning) {
                            drone.targetPlot = p;
                            break;
                        }
                    }
                    if (drone.targetPlot != null) break;
                }

                // Приоритет 2: изношенные турбины
                if (drone.targetPlot == null) {
                    double minDur = 99.5;
                    for (int c = 0; c < COLS; c++) {
                        for (int r = 0; r < ROWS; r++) {
                            Plot p = plots[c][r];
                            if (p.hasTurbine() && p.durability < minDur) {
                                minDur = p.durability;
                                drone.targetPlot = p;
                            }
                        }
                    }
                }
            }

            if (drone.targetPlot != null && cellW > 0 && cellH > 0) {
                double targetX = drone.targetPlot.col * cellW + cellW / 2.0;
                double targetY = groundY + drone.targetPlot.row * cellH + (cellH * 0.25);

                double dx = targetX - drone.x;
                double dy = targetY - drone.y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist > 10.0) {
                    drone.x += (dx / dist) * 4.5;
                    drone.y += (dy / dist) * 4.5;
                    drone.isWelding = false;
                } else {
                    if (drone.targetPlot.isBurning) {
                        drone.targetPlot.isBurning = false;
                        sparkles.add(new Sparkle(drone.x, drone.y + 6, new Color(100, 200, 255)));
                        log("🤖 Ремонтный дрон ликвидировал возгорание на слоте [" + (drone.targetPlot.col + 1) + "," + (drone.targetPlot.row + 1) + "]!");
                        SoundSystem.play("build");
                    } else {
                        drone.isWelding = true;
                        drone.targetPlot.durability = Math.min(100.0, drone.targetPlot.durability + 0.35);
                        if (Math.random() < 0.4) {
                            sparkles.add(new Sparkle(drone.x, drone.y + 6, new Color(0, 220, 255)));
                        }
                    }
                }
            } else {
                drone.isWelding = false;
                drone.x += Math.sin(System.currentTimeMillis() * 0.002) * 1.5;
            }
        }
    }

    private void updateComboMultiplier() {
        if (comboClicks >= 30) comboMultiplier = 5.0;
        else if (comboClicks >= 15) comboMultiplier = 3.0;
        else if (comboClicks >= 5) comboMultiplier = 2.0;
        else comboMultiplier = 1.0;
    }

    private void gameLogicTick() {
        gameDays++;
        if (gameDays >= 30) {
            gameDays = 1;
            gameMonths++;
            processMonthlyInsurance();
        }

        // Смена сезонов каждые 7 дней
        seasonDayCounter++;
        if (seasonDayCounter >= 7) {
            seasonDayCounter = 0;
            currentSeason = Season.values()[(currentSeason.ordinal() + 1) % Season.values().length];
            log("🍂 СМЕНА СЕЗОНА: Наступила " + currentSeason.title + "! " + currentSeason.desc);
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, 70, currentSeason.title + "!", new Color(254, 202, 87)));
        }

        // Прогноз погоды
        if (hiredMeteorologist && forecastCountdown > 0) {
            forecastCountdown--;
            if (forecastCountdown <= 0) forecastCountdown = 14;
        }

        // Эко-патруль (краснокнижные птицы)
        rareBirdEventTimer++;
        if (rareBirdEventTimer >= 80) {
            rareBirdEventTimer = 0;
            balance += 1500.0;
            SoundSystem.play("quest");
            log("🦅 ЭКО-ПАТРУЛЬ: Стая краснокнижных орлов безопасно пролетела над парком! Грант: +$1,500!");
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, 90, "🦅 ЭКО-ГРАНТ +$1,500!", new Color(254, 202, 87)));
            spawnSparkles(canvas.getWidth() / 2, 100, new Color(254, 202, 87));
        }

        if (climateSummitTimer > 0) climateSummitTimer--;
        if (marketPeakTimer > 0) marketPeakTimer--;

        updateCityContractLogic();

        double currentTotalKw = 0.0;
        double currentTotalEarnings = 0.0;
        int activeTurbines = 0;
        int totalTurbineLevels = 0;

        double windEfficiency = 0.35;
        if (windSpeed >= 3.0) {
            windEfficiency = (windSpeed <= 12.0) ? (0.35 + ((windSpeed - 3.0) / 9.0) * 0.65) : 1.0;
        }

        double rebirthMultiplier = 1.0 + (rebirthCount * 0.5);
        double aeroMultiplier = 1.0 + (techAeroLevel * 0.15);
        double wearMultiplier = Math.max(0.12, 1.0 - (techCarbonLevel * 0.25));

        // Эффекты специалистов и сезонов на износ
        if (hiredChiefMechanic) wearMultiplier *= 0.70;
        if (currentSeason == Season.SPRING) wearMultiplier *= 0.80;

        double summitMultiplier = (climateSummitTimer > 0) ? 2.0 : 1.0;
        double winterTariff = (currentSeason == Season.WINTER) ? 1.35 : 1.0;
        double summerSolar = (currentSeason == Season.SUMMER) ? 1.50 : 1.0;
        double biomeIncomeMult = currentBiome.incomeModifier;

        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;

        boolean isDaytime = (timeOfDay < 0.45 || timeOfDay > 0.88);

        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.hasTurbine() && p.durability > 0) {
                    activeTurbines++;
                    totalTurbineLevels += p.level;

                    double localWindEff = windEfficiency;
                    if (p.turbine == TurbineType.OFFSHORE && localWindEff < 0.65) {
                        localWindEff = 0.65;
                    }

                    double healthFactor = (p.durability < 35.0) ? (p.durability / 35.0) : 1.0;
                    double levelBonus = 1.0 + (p.level - 1) * 0.35;

                    double kw = p.turbine.maxPowerKw * localWindEff * healthFactor * levelBonus * comboMultiplier * aeroMultiplier;
                    double turbineIncome = p.turbine.baseIncomePerSec * localWindEff * healthFactor * levelBonus * comboMultiplier * rebirthMultiplier * summitMultiplier * winterTariff * biomeIncomeMult;

                    if (p.turbine == TurbineType.SOLAR_HYBRID && isDaytime) {
                        turbineIncome += (140.0 * levelBonus * rebirthMultiplier * summerSolar);
                        kw += (800.0 * levelBonus * summerSolar);
                    }

                    if (p.turbine == TurbineType.OFFSHORE && currentBiome == Biome.OCEAN) {
                        kw *= 1.50;
                        turbineIncome *= 1.50;
                    }

                    if (turbineIncome < 1.0) turbineIncome = 1.0;

                    currentTotalKw += kw;
                    currentTotalEarnings += turbineIncome;

                    double stormWear = (windSpeed > 15.0) ? 1.8 : 1.0;
                    p.durability -= (p.turbine.wearRate / p.level) * stormWear * wearMultiplier;

                    // Логика горения
                    if (p.isBurning) {
                        p.durability = Math.max(0.0, p.durability - 2.5);
                        if (p.durability <= 0.0) {
                            p.isBurning = false;
                        }
                    } else if (!tornadoActive && p.hasTurbine() && p.durability > 15.0) {
                        boolean highStress = (windSpeed > 17.0 && p.durability < 65.0);
                        double fireChance = highStress ? 0.025 : 0.0007;
                        if (random.nextDouble() < fireChance) {
                            p.isBurning = true;
                            SoundSystem.play("siren");
                            log("🔥 АВАРИЯ! Перегрев и возгорание на слоте [" + (c + 1) + "," + (r + 1) + "]! Потушите кликом!");
                            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, 70, "🔥 ПОЖАР НА ТУРБИНЕ!", new Color(231, 76, 60)));
                        }
                    }

                    if (p.durability <= 0.0) {
                        if (hiredChiefMechanic && random.nextDouble() < 0.35) {
                            p.durability = 45.0;
                            p.isBurning = false;
                            log("🛡️ Главный механик спас турбину [" + (c + 1) + "," + (r + 1) + "] от аварии и восстановил 45% прочности!");
                            SoundSystem.play("build");
                            if (cellW > 0 && cellH > 0) {
                                spawnSparkles(c * cellW + cellW / 2, (int)(canvas.getHeight() * 0.12) + r * cellH + 30, new Color(243, 156, 18));
                            }
                        } else {
                            p.durability = 0.0;
                            p.isBurning = false;
                            log("⚠️ Внимание! Турбина на [" + (c + 1) + "," + (r + 1) + "] сломалась!");
                        }
                    }

                    if (cellW > 0 && Math.random() < 0.45) {
                        int tx = c * cellW + cellW / 2 + random.nextInt(16) - 8;
                        int ty = (int)(canvas.getHeight() * 0.12) + r * cellH + (int)(cellH * 0.42);
                        floatingTexts.add(new FloatingText(tx, ty, "+$" + dfMoney.format(turbineIncome), new Color(46, 204, 113)));
                    }
                }
            }
        }

        if (engineersCount > 0) {
            double pool = engineersCount * 2.2;
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    Plot p = plots[c][r];
                    if (p.hasTurbine() && p.durability < 98.0 && !p.isBurning && pool > 0) {
                        double need = 100.0 - p.durability;
                        double fix = Math.min(need, pool);
                        p.durability += fix;
                        pool -= fix;
                    }
                }
            }
        }

        if (batteryCapacity > 0 && currentTotalKw > 0) {
            double chargeSurplus = currentTotalKw * 0.05;
            if (batteryStored < batteryCapacity) {
                batteryStored = Math.min(batteryCapacity, batteryStored + chargeSurplus);
            }
        }

        if (windSpeed < 4.0 && batteryStored > 10.0 && activeContract != null && currentTotalKw < activeContract.requiredKw) {
            double needed = activeContract.requiredKw - currentTotalKw;
            double discharge = Math.min(needed, batteryStored);
            batteryStored -= discharge;
            currentTotalKw += discharge;
        }

        // Авто-сброс ИИ-брокера
        if (hiredBroker && batteryStored >= 200.0) {
            double effPrice = marketPricePerKwh * marketMultiplier * winterTariff;
            if (marketPeakTimer > 0 || effPrice >= 0.30) {
                dumpBatteryToMarket();
            }
        }

        double spGained = (0.3 + (totalTurbineLevels * 0.15)) * rebirthMultiplier;
        sciencePoints += spGained;

        double salaries = engineersCount * engineerSalaryPerSec;
        balance -= salaries;
        balance += currentTotalEarnings;
        totalEnergyGenerated += (currentTotalKw / 3600.0);

        currentPowerOutput = currentTotalKw;
        incomePerSecond = currentTotalEarnings - salaries;

        checkAutomaticAchievements(activeTurbines);
        updateUI();
    }

    private void checkAutomaticAchievements(int activeTurbines) {
        if (activeTurbines >= 1) unlockAchievement("FIRST_TURBINE", "🌱 Первый киловатт", 500.0);
        if (balance >= 50000.0) unlockAchievement("HIGH_ROLLER", "💰 Энергетический магнат", 5000.0);
        if (droneCount >= 1) unlockAchievement("DRONE_ERA", "🤖 Эра автоматизации", 2500.0);
        if (batteryStored >= 1000.0) unlockAchievement("BATTERY_MASTER", "🔋 Мастер накопителей", 3000.0);
        if (cityContractsCompleted >= 5) unlockAchievement("CONTRACT_ACE", "🏙️ Почетный энергетик", 8000.0);
        if (rebirthCount >= 1) unlockAchievement("REBIRTH_HERO", "👑 Легенда зеленой биржи", 25000.0);
    }

    private void updateCityContractLogic() {
        if (activeContract == null) {
            contractCooldownTimer--;
            if (contractCooldownTimer <= 0) {
                activeContract = new CityContract();
                int type = random.nextInt(4);
                if (type == 0) {
                    activeContract.title = "Мэрия: Зарядка парка электробусов";
                    activeContract.requiredKw = 75.0;
                    activeContract.reward = 950.0;
                    activeContract.durationSeconds = 15;
                } else if (type == 1) {
                    activeContract.title = "Городской фестиваль неонового света";
                    activeContract.requiredKw = 180.0;
                    activeContract.reward = 2200.0;
                    activeContract.durationSeconds = 18;
                } else if (type == 2) {
                    activeContract.title = "Мегаполис: Сбой на угольной ТЭЦ";
                    activeContract.requiredKw = 450.0;
                    activeContract.reward = 6000.0;
                    activeContract.durationSeconds = 20;
                } else {
                    activeContract.title = "Иннополис: Питание суперкомпьютера ИИ";
                    activeContract.requiredKw = 1200.0;
                    activeContract.reward = 18000.0;
                    activeContract.durationSeconds = 22;
                }
                activeContract.remainingSeconds = activeContract.durationSeconds;
                activeContract.active = true;
                log("🏙️ НОВЫЙ КОНТРАКТ: '" + activeContract.title + "'! Нужно " + (int)activeContract.requiredKw + " кВт.");
            }
        } else {
            activeContract.remainingSeconds--;

            if (currentPowerOutput >= activeContract.requiredKw) {
                activeContract.progressSeconds++;
            }

            if (activeContract.progressSeconds >= (activeContract.durationSeconds * 0.65)) {
                double rew = activeContract.reward * (1.0 + rebirthCount * 0.5);
                balance += rew;
                cityContractsCompleted++;
                SoundSystem.play("quest");
                log("🎉 КОНТРАКТ ВЫПОЛНЕН: " + activeContract.title + "! Получено +$" + dfMoney.format(rew));
                floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2, "+$" + (int)rew + " КОНТРАКТ 🏙️", new Color(0, 210, 211)));
                spawnSparkles(canvas.getWidth() / 2, canvas.getHeight() / 2, new Color(0, 210, 211));
                activeContract = null;
                contractCooldownTimer = 15;
            } else if (activeContract.remainingSeconds <= 0) {
                log("❌ Контракт города не выполнен вовремя: не хватило мощности.");
                activeContract = null;
                contractCooldownTimer = 10;
            }
        }
    }

    private void processMonthlyInsurance() {
        if (!isInsured) return;
        if (balance >= insuranceMonthlyCost) {
            balance -= insuranceMonthlyCost;
            log("💳 Оплата страховки за месяц " + gameMonths + ": -$10.00. Ветропарк под защитой.");
        } else {
            isInsured = false;
            log("❌ СТРАХОВКА АННУЛИРОВАНА: Недостаточно средств для оплаты $10/мес!");
            updateControlButtons();
        }
    }

    private void updateWeatherEvent() {
        if (hiredMeteorologist && nextWeatherCondition != null) {
            weatherCondition = nextWeatherCondition;
            targetWindSpeed = nextTargetWind;
        } else {
            determineNextWeather();
            weatherCondition = nextWeatherCondition;
            targetWindSpeed = nextTargetWind;
        }

        if (weatherCondition.contains("ТОРНАДО")) {
            spawnTornadoEvent();
        } else if (weatherCondition.contains("Гроза")) {
            spawnLightningStormEvent();
        } else if (weatherCondition.contains("Саммит")) {
            spawnClimateSummitEvent();
        } else if (weatherCondition.contains("Сильный ветер")) {
            log("💨 Сильный ветер! Высокий КПД ветропарка.");
        } else if (weatherCondition.contains("Штиль")) {
            log("☁️ Наступил штиль, выработка снижена.");
        } else {
            log("🍃 Погода: " + weatherCondition);
        }

        determineNextWeather();
        forecastCountdown = 14;
    }

    private void determineNextWeather() {
        int ev = random.nextInt(100);
        double biomeMult = currentBiome.windModifier;
        double seasonMult = currentSeason.windFactor;

        if (ev < 15) {
            nextWeatherCondition = "ТОРНАДО 🌪️";
            nextTargetWind = 24.0 * biomeMult;
        } else if (ev < 30) {
            nextWeatherCondition = "Гроза ⚡";
            nextTargetWind = (18.0 + random.nextDouble() * 5.0) * biomeMult;
        } else if (ev < 45) {
            nextWeatherCondition = "Саммит ООН 🌍";
            nextTargetWind = (10.0 + random.nextDouble() * 4.0) * biomeMult * seasonMult;
        } else if (ev < 65) {
            nextWeatherCondition = "Сильный ветер 💨";
            nextTargetWind = (13.5 + random.nextDouble() * 4.0) * biomeMult * seasonMult;
        } else if (ev < 80) {
            nextWeatherCondition = "Штиль ☁️";
            nextTargetWind = (3.0 + random.nextDouble() * 2.0) * biomeMult;
        } else {
            nextWeatherCondition = "Бриз 🍃";
            nextTargetWind = (8.0 + random.nextDouble() * 4.0) * biomeMult * seasonMult;
        }
    }

    private void spawnClimateSummitEvent() {
        climateSummitTimer = 25;
        SoundSystem.play("quest");
        log("🌍 КЛИМАТИЧЕСКИЙ САММИТ: Зеленые субсидии x2 на все доходы в течение 25 секунд!");
        floatingTexts.add(new FloatingText(canvas.getWidth() / 2, 80, "🌍 СУБСИДИИ x2 (САММИТ ООН)!", new Color(46, 204, 113)));
    }

    private void spawnLightningStormEvent() {
        targetWindSpeed = 19.0 + random.nextDouble() * 5.0;
        weatherCondition = "Гроза ⚡";
        lightningFlash = true;
        SoundSystem.play("thunder");

        Timer flashOff = new Timer(250, e -> {
            lightningFlash = false;
            ((Timer)e.getSource()).stop();
        });
        flashOff.setRepeats(false);
        flashOff.start();

        if (techLightningRod) {
            double chargedKw = 350.0;
            batteryStored = Math.min(batteryCapacity, batteryStored + chargedKw);
            balance += 1500.0;
            log("⚡ ГРОМООТВОД СРАБОТАЛ: Молния перехвачена! +350 кВт в батарею и премия экологов +$1,500!");
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, 120, "⚡ МОЛНИЯ ПЕРЕХВАЧЕНА! +$1,500", new Color(254, 202, 87)));
        } else {
            List<Plot> built = new ArrayList<>();
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    if (plots[c][r].hasTurbine() && plots[c][r].durability > 0) built.add(plots[c][r]);
                }
            }
            if (!built.isEmpty()) {
                Plot hit = built.get(random.nextInt(built.size()));
                hit.durability = Math.max(0.0, hit.durability - 50.0);
                if (Math.random() < 0.40) {
                    hit.isBurning = true;
                    log("⚡ МОЛНИЯ УДАРИЛА по ветряку [" + (hit.col + 1) + "," + (hit.row + 1) + "]! Гондола загорелась!");
                } else {
                    log("⚡ МОЛНИЯ УДАРИЛА по ветряку [" + (hit.col + 1) + "," + (hit.row + 1) + "]! Изучите громоотвод в лаборатории!");
                }
            }
        }
    }

    private void spawnTornadoEvent() {
        tornadoActive = true;
        tornadoX = -80;
        tornadoTargetX = canvas.getWidth() * 0.45;
        tornadoLifeFrames = 0;
        targetWindSpeed = 24.0;
        weatherCondition = "ТОРНАДО 🌪️";

        SoundSystem.play("siren");
        log("🚨 ВНИМАНИЕ: Смерч в регионе! Вероятность удара: 10%!");

        Timer impactTimer = new Timer(4200, e -> {
            ((Timer)e.getSource()).stop();
            checkTornadoImpact();
        });
        impactTimer.setRepeats(false);
        impactTimer.start();
    }

    private void checkTornadoImpact() {
        if (!tornadoActive) return;

        if (techStormRadar) {
            log("🛡️ РАДАР БУРЬ: Лопасти автоматически заторможены! Ветропарк пережил торнадо БЕЗ урона!");
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2 - 40, "🛡️ ЗАЩИТА РАДАРА!", new Color(46, 204, 113)));
            unlockAchievement("SURVIVOR", "🌪️ Повелитель стихий", 3000.0);
            return;
        }

        boolean directHit = (random.nextInt(100) < 10);
        if (directHit) {
            lightningFlash = true;
            SoundSystem.play("thunder");
            log("💥 КАТАСТРОФА! Торнадо ударил прямо по ветропарку!");

            int brokenCount = 0;
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    Plot p = plots[c][r];
                    if (p.hasTurbine() && p.durability > 0) {
                        p.durability = 0.0;
                        if (Math.random() < 0.35) p.isBurning = true;
                        brokenCount++;
                        spawnSparkles(c * (canvas.getWidth()/COLS) + 60, r * (canvas.getHeight()/ROWS) + 60, new Color(231, 76, 60));
                    }
                }
            }

            if (brokenCount > 0) {
                if (isInsured) {
                    double payout = brokenCount * insurancePayoutPerTurbine;
                    balance += payout;
                    SoundSystem.play("quest");
                    log("🛡️ СТРАХОВАЯ КОМПАНИЯ выплатила компенсацию: +$" + dfMoney.format(payout) + "!");
                    floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2 - 40, "+$" + (int)payout + " СТРАХОВКА 💼", new Color(254, 202, 87)));
                    unlockAchievement("SURVIVOR", "🌪️ Повелитель стихий", 3000.0);
                } else {
                    log("❌ У вас НЕТ страховки! Ремонт вышек за свой счет!");
                }
            }

            Timer flashReset = new Timer(400, e -> {
                lightningFlash = false;
                ((Timer)e.getSource()).stop();
            });
            flashReset.setRepeats(false);
            flashReset.start();
        } else {
            log("😅 Торнадо обошел ветропарк стороной!");
        }
    }

    private void updateMarketFluctuations() {
        if (marketPeakTimer <= 0) {
            if (random.nextInt(100) < 18) {
                marketMultiplier = 2.4 + random.nextDouble() * 0.8;
                marketPeakTimer = 16;
                SoundSystem.play("market");
                log("⚡ БИРЖА: Пиковый спрос! Цены на электроэнергию выросли до $" + dfMoney.format(marketPricePerKwh * marketMultiplier) + " / кВт⋅ч!");
            } else {
                marketMultiplier = 0.8 + random.nextDouble() * 0.5;
            }
        }
        updateControlButtons();
    }

    // ==========================================
    // === 8. ВЗАИМОДЕЙСТВИЕ И ДЕЙСТВИЯ ИГРОКА
    // ==========================================
    private void handleCanvasClick(int mx, int my) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        int groundY = (int)(h * 0.12);
        int farmH = (int)(h * 0.72) - groundY;
        int cellW = w / COLS;
        int cellH = farmH / ROWS;

        if (my < groundY || my > groundY + farmH) return;

        int c = mx / cellW;
        int r = (my - groundY) / cellH;

        if (c >= 0 && c < COLS && r >= 0 && r < ROWS) {
            Plot clicked = plots[c][r];
            selectedPlot = clicked;

            if (clicked.isBurning) {
                clicked.isBurning = false;
                SoundSystem.play("build");
                spawnSparkles(mx, my, new Color(100, 200, 255));
                floatingTexts.add(new FloatingText(mx, my - 20, "🧯 ПОТУШЕНО!", new Color(52, 152, 219)));
                log("🧯 Вы потушили пожар на слоте [" + (c + 1) + "," + (r + 1) + "]!");
                updateControlButtons();
                canvas.repaint();
                return;
            }

            if (clicked.hasTurbine() && clicked.durability > 0) {
                clicked.bladeAngle = (clicked.bladeAngle + 50.0) % 360.0;
                comboClicks += 2;
                comboDecayTimer = 35;
                updateComboMultiplier();

                boolean isCrit = (random.nextInt(100) < 15);
                double clickCash = (isCrit ? 10.0 : 2.0) * comboMultiplier * (1.0 + rebirthCount * 0.5);
                balance += clickCash;

                if (isCrit) {
                    SoundSystem.play("crit");
                    sciencePoints += 1.0;
                    spawnSparkles(mx, my, new Color(254, 202, 87));
                    floatingTexts.add(new FloatingText(mx, my - 20, "💥 КРИТ x5! +$" + dfMoney.format(clickCash), new Color(255, 75, 75)));
                } else {
                    SoundSystem.play("click");
                    spawnSparkles(mx, my, clicked.turbine.neonColor);
                    floatingTexts.add(new FloatingText(mx, my - 15, "+$" + dfMoney.format(clickCash) + (comboMultiplier > 1 ? " x" + (int)comboMultiplier : ""), new Color(254, 202, 87)));
                }
            }

            updateControlButtons();
            canvas.repaint();
        }
    }

    private void handleBuildOrUpgrade(TurbineType type) {
        if (selectedPlot == null) return;
        if (!selectedPlot.hasTurbine()) {
            buildTurbine(type);
        } else {
            if (type.ordinal() > selectedPlot.turbine.ordinal()) {
                evolveTurbineTo(selectedPlot, type);
            }
        }
    }

    private void evolveTurbineTo(Plot plot, TurbineType next) {
        double diff = next.cost - plot.turbine.cost;
        if (balance < diff) {
            JOptionPane.showMessageDialog(this,
                    "Для эволюции в " + next.name + " нужно $" + dfMoney.format(diff) + "!",
                    "Эволюция ветряка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        balance -= diff;
        TurbineType old = plot.turbine;
        plot.turbine = next;
        plot.level = 1;
        plot.durability = 100.0;

        SoundSystem.play("build");
        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;
        spawnSparkles(plot.col * cellW + cellW / 2, (int)(canvas.getHeight() * 0.12) + plot.row * cellH + (int)(cellH * 0.5), next.neonColor);

        log("🚀 ЭВОЛЮЦИЯ: Ветряк [" + (plot.col + 1) + "," + (plot.row + 1) + "] улучшен: " + old.name + " ➔ " + next.name + "!");
        floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2, "🚀 " + next.name.toUpperCase() + "!", next.themeColor));

        updateControlButtons();
        updateUI();
        canvas.repaint();
    }

    private void demolishSelectedTurbine() {
        if (selectedPlot == null || !selectedPlot.hasTurbine()) return;
        int ans = JOptionPane.showConfirmDialog(this,
                "Снести ветряк '" + selectedPlot.turbine.name + "'?\nВозврат средств: +$" + dfMoney.format(selectedPlot.turbine.cost * 0.5),
                "Демонтаж", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans == JOptionPane.YES_OPTION) {
            double refund = selectedPlot.turbine.cost * 0.5;
            balance += refund;
            log("🗑️ Демонтирован " + selectedPlot.turbine.name + ". Возврат: +$" + dfMoney.format(refund));
            selectedPlot.turbine = null;
            selectedPlot.level = 1;
            selectedPlot.durability = 100.0;
            SoundSystem.play("coin");
            updateControlButtons();
            updateUI();
            canvas.repaint();
        }
    }

    private void buildTurbine(TurbineType type) {
        if (selectedPlot == null) return;
        if (selectedPlot.hasTurbine()) {
            JOptionPane.showMessageDialog(this, "Этот участок уже занят!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (balance < type.cost) {
            JOptionPane.showMessageDialog(this, "Недостаточно средств ($" + dfMoney.format(type.cost) + ")!", "Магазин", JOptionPane.WARNING_MESSAGE);
            return;
        }

        balance -= type.cost;
        selectedPlot.turbine = type;
        selectedPlot.durability = 100.0;
        selectedPlot.level = 1;

        SoundSystem.play("build");
        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;
        spawnSparkles(selectedPlot.col * cellW + cellW / 2, (int)(canvas.getHeight() * 0.12) + selectedPlot.row * cellH + (int)(cellH * 0.7), type.neonColor);

        log("🎉 Построен " + type.name + " на участке [" + (selectedPlot.col + 1) + "," + (selectedPlot.row + 1) + "]!");
        updateControlButtons();
        updateUI();
    }

    private void repairSelectedTurbine() {
        if (selectedPlot == null || !selectedPlot.hasTurbine()) return;
        if (selectedPlot.durability >= 99.5) return;

        double repCost = (100.0 - selectedPlot.durability) * 1.5;
        if (balance < repCost) {
            JOptionPane.showMessageDialog(this, "Недостаточно денег на ремонт!", "Финансы", JOptionPane.WARNING_MESSAGE);
            return;
        }

        balance -= repCost;
        selectedPlot.durability = 100.0;
        SoundSystem.play("build");

        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;
        spawnSparkles(selectedPlot.col * cellW + cellW / 2, (int)(canvas.getHeight() * 0.12) + selectedPlot.row * cellH + (int)(cellH * 0.7), new Color(241, 196, 15));

        log("🔧 Турбина на [" + (selectedPlot.col + 1) + "," + (selectedPlot.row + 1) + "] полностью отремонтирована!");
        updateControlButtons();
        updateUI();
    }

    private void upgradeSelectedTurbine() {
        if (selectedPlot == null || !selectedPlot.hasTurbine()) return;
        TurbineType next = selectedPlot.turbine.getNextTier();

        if (next != null) {
            evolveTurbineTo(selectedPlot, next);
        } else {
            double upCost = selectedPlot.turbine.cost * 0.5 * selectedPlot.level;
            if (balance < upCost) {
                JOptionPane.showMessageDialog(this, "Нужно $" + dfMoney.format(upCost) + " для форсирования уровня!", "Улучшение", JOptionPane.WARNING_MESSAGE);
                return;
            }

            balance -= upCost;
            selectedPlot.level++;
            selectedPlot.durability = 100.0;
            SoundSystem.play("build");

            int cellW = canvas.getWidth() / COLS;
            int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;
            spawnSparkles(selectedPlot.col * cellW + cellW / 2, (int)(canvas.getHeight() * 0.12) + selectedPlot.row * cellH + (int)(cellH * 0.5), selectedPlot.turbine.neonColor);

            log("⭐ Квантовый VAWT форсирован до Lv." + selectedPlot.level + "! Выработка увеличена!");
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2, "⭐ Lv." + selectedPlot.level + "!", new Color(241, 196, 15)));
            updateControlButtons();
            updateUI();
        }
    }

    private void hireEngineer() {
        if (balance < engineerHireCost) {
            JOptionPane.showMessageDialog(this, "Для найма инженера нужно $" + engineerHireCost, "Персонал", JOptionPane.WARNING_MESSAGE);
            return;
        }
        balance -= engineerHireCost;
        engineersCount++;
        SoundSystem.play("coin");
        log("👷 Нанят сервисный техник ($250)! Скорость авторемонта увеличена.");
        updateUI();
    }

    private void toggleInsurance() {
        if (!isInsured) {
            if (balance < insuranceMonthlyCost) {
                JOptionPane.showMessageDialog(this, "Для договора нужно $" + insuranceMonthlyCost + "!", "Страхование", JOptionPane.WARNING_MESSAGE);
                return;
            }
            balance -= insuranceMonthlyCost;
            isInsured = true;
            SoundSystem.play("coin");
            log("🛡️ Договор страхования заключен! $10/мес. Выплата при торнадо: $800 за вышку.");
        } else {
            isInsured = false;
            log("❌ Договор страхования расторгнут.");
        }
        updateControlButtons();
        updateUI();
    }

    private void upgradeBattery() {
        double cost;
        double newCap;
        if (batteryCapacity <= 0) {
            cost = 4000.0;
            newCap = 1000.0;
        } else if (batteryCapacity < 5000.0) {
            cost = 14000.0;
            newCap = 5000.0;
        } else if (batteryCapacity < 25000.0) {
            cost = 45000.0;
            newCap = 25000.0;
        } else {
            cost = 120000.0;
            newCap = batteryCapacity + 50000.0;
        }

        if (balance < cost) {
            JOptionPane.showMessageDialog(this, "Нужно $" + dfMoney.format(cost) + " для расширения батареи!", "Батарея", JOptionPane.WARNING_MESSAGE);
            return;
        }

        balance -= cost;
        batteryCapacity = newCap;
        SoundSystem.play("build");
        log("🔋 Накопительная подстанция расширена до " + (int)batteryCapacity + " кВт⋅ч!");
        updateControlButtons();
        updateUI();
    }

    private void dumpBatteryToMarket() {
        if (batteryStored <= 0.1) {
            JOptionPane.showMessageDialog(this, "Батарея разряжена! Накопите энергию от ветра.", "Биржа", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double rate = marketPricePerKwh * marketMultiplier;
        double payout = batteryStored * rate;
        balance += payout;
        double dumped = batteryStored;
        batteryStored = 0.0;

        SoundSystem.play("market");
        log("⚡ БИРЖА: Продано " + dfShort.format(dumped) + " кВт⋅ч по тарифу $" + dfMoney.format(rate) + "! Получено +$" + dfMoney.format(payout));
        floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2, "+$" + dfMoney.format(payout) + " БИРЖА ⚡", new Color(0, 210, 211)));
        spawnSparkles(canvas.getWidth() / 2, canvas.getHeight() / 2, new Color(254, 202, 87));

        updateControlButtons();
        updateUI();
    }

    private void researchAero() {
        int[] spCosts = {40, 120, 300};
        if (techAeroLevel >= 3) return;
        int cost = spCosts[techAeroLevel];
        if (sciencePoints < cost) {
            JOptionPane.showMessageDialog(this, "Нужно " + cost + " SP для аэродинамики!", "Лаборатория", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sciencePoints -= cost;
        techAeroLevel++;
        SoundSystem.play("research");
        unlockAchievement("SCIENCE", "🔬 Инноватор", 1500.0);
        log("🔬 ИССЛЕДОВАНО: Аэродинамика лопастей Lv." + techAeroLevel + "! Выработка +15%!");
        updateControlButtons();
    }

    private void researchCarbon() {
        int[] spCosts = {50, 150, 400};
        if (techCarbonLevel >= 3) return;
        int cost = spCosts[techCarbonLevel];
        if (sciencePoints < cost) {
            JOptionPane.showMessageDialog(this, "Нужно " + cost + " SP для сплавов!", "Лаборатория", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sciencePoints -= cost;
        techCarbonLevel++;
        SoundSystem.play("research");
        unlockAchievement("SCIENCE", "🔬 Инноватор", 1500.0);
        log("🔬 ИССЛЕДОВАНО: Углепластиковые сплавы Lv." + techCarbonLevel + "! Скорость износа снижена на 25%!");
        updateControlButtons();
    }

    private void researchLightning() {
        if (techLightningRod) return;
        if (sciencePoints < 100) {
            JOptionPane.showMessageDialog(this, "Нужно 100 SP для громоотвода!", "Лаборатория", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sciencePoints -= 100;
        techLightningRod = true;
        SoundSystem.play("research");
        unlockAchievement("SCIENCE", "🔬 Инноватор", 1500.0);
        log("🔬 ИССЛЕДОВАНО: Громоотвод и защита сети! Молнии теперь заряжают батареи и приносят премию!");
        updateControlButtons();
    }

    private void researchRadar() {
        if (techStormRadar) return;
        if (sciencePoints < 150) {
            JOptionPane.showMessageDialog(this, "Нужно 150 SP для радара!", "Лаборатория", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sciencePoints -= 150;
        techStormRadar = true;
        SoundSystem.play("research");
        unlockAchievement("SCIENCE", "🔬 Инноватор", 1500.0);
        log("🔬 ИССЛЕДОВАНО: Радар раннего оповещения! Турбины автоматически защищены от торнадо!");
        updateControlButtons();
    }

    private void researchDrones() {
        if (techDronesUnlocked) return;
        if (sciencePoints < 220) {
            JOptionPane.showMessageDialog(this, "Нужно 220 SP для чертежей дронов!", "Лаборатория", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sciencePoints -= 220;
        techDronesUnlocked = true;
        SoundSystem.play("research");
        unlockAchievement("SCIENCE", "🔬 Инноватор", 1500.0);
        log("🔬 ИССЛЕДОВАНО: Ангар ремонтных дронов! Теперь вы можете нанимать автономных дронов!");
        updateControlButtons();
    }

    private void buyDrone() {
        if (!techDronesUnlocked) {
            JOptionPane.showMessageDialog(this, "Сначала исследуйте технологию дронов в лаборатории!", "Дроны", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double cost = 5000.0;
        if (balance < cost) {
            JOptionPane.showMessageDialog(this, "Нужно $" + dfMoney.format(cost) + " для покупки дрона!", "Дроны", JOptionPane.WARNING_MESSAGE);
            return;
        }
        balance -= cost;
        droneCount++;
        syncDronesList();
        SoundSystem.play("build");
        unlockAchievement("DRONE_ERA", "🤖 Эра автоматизации", 2500.0);
        log("🤖 Приобретен ремонтный дрон #" + droneCount + "! Он автоматически чинит поврежденные гондолы.");
        updateControlButtons();
        updateUI();
    }

    private void triggerRebirth() {
        int turbineCount = 0;
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (plots[c][r].hasTurbine()) turbineCount++;
            }
        }

        if (balance < 100000.0 || turbineCount < 6) {
            JOptionPane.showMessageDialog(this,
                    "Для Rebirth (IPO) нужно накопить минимум $100,000 и построить не менее 6 вышек!\nУ вас: $" + dfMoney.format(balance) + ", вышек: " + turbineCount,
                    "IPO / Rebirth", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ans = JOptionPane.showConfirmDialog(this,
                "👑 ВЫХОД НА IPO (REBIRTH):\n" +
                "- Поле и деньги сбросятся.\n" +
                "- Вы получите ПОСТОЯННЫЙ бонус +50% ко всем доходам НАВСЕГДА!\n" +
                "- Исследования и достижения СОХРАНЯЮТСЯ!\n\nПродолжить?",
                "Подтверждение Rebirth", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (ans == JOptionPane.YES_OPTION) {
            rebirthCount++;
            greenTokens += 15 * rebirthCount;
            balance = 2500.0 * rebirthCount;
            engineersCount = 0;
            isInsured = false;
            activeContract = null;

            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    plots[c][r].turbine = null;
                    plots[c][r].level = 1;
                    plots[c][r].durability = 100.0;
                }
            }
            selectedPlot = null;

            SoundSystem.play("rebirth");
            unlockAchievement("REBIRTH_HERO", "👑 Легенда зеленой биржи", 25000.0);
            log("👑 ПОЗДРАВЛЯЕМ! Совершен Rebirth #" + rebirthCount + "! Множитель дохода увеличен до x" + (1.0 + rebirthCount * 0.5) + "!");
            floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2, "👑 REBIRTH #" + rebirthCount + "! 🌟", new Color(241, 196, 15)));
            spawnSparkles(canvas.getWidth() / 2, canvas.getHeight() / 2, new Color(241, 196, 15));

            updateControlButtons();
            updateUI();
            canvas.repaint();
        }
    }

    private void spawnSparkles(int x, int y, Color c) {
        for (int i = 0; i < 22; i++) {
            sparkles.add(new Sparkle(x, y, c));
        }
    }

    private void updateControlButtons() {
        if (isInsured) {
            insuranceToggleBtn.setText("🛡️ Страховка: АКТИВНА");
            insuranceToggleBtn.setBaseColor(new Color(39, 174, 96));
        } else {
            insuranceToggleBtn.setText("📜 Оформить страховку ($10/мес)");
            insuranceToggleBtn.setBaseColor(new Color(41, 128, 185));
        }

        if (selectedPlot == null) {
            selectedPlotTitle.setText("Участок не выбран");
            selectedPlotSub.setText("Кликните по участку на поле");
            if (selectedPlotDurabilityBar != null) {
                selectedPlotDurabilityBar.setValue(0);
                selectedPlotDurabilityBar.setString("0%");
            }
            buildBasicBtn.setText("🔨 Малый ($1k)");
            buildAdvBtn.setText("⚡ Средний ($3.2k)");
            buildIndBtn.setText("🌟 Гигант ($9k)");
            buildOffshoreBtn.setText("🌊 Офшор ($24k)");
            buildSolarBtn.setText("☀️ Гибрид ($55k)");
            buildVawtBtn.setText("🌀 VAWT ($140k)");
            buildBasicBtn.setEnabled(false);
            buildAdvBtn.setEnabled(false);
            buildIndBtn.setEnabled(false);
            buildOffshoreBtn.setEnabled(false);
            buildSolarBtn.setEnabled(false);
            buildVawtBtn.setEnabled(false);
            repairBtn.setEnabled(false);
            upgradeBtn.setEnabled(false);
            demolishBtn.setEnabled(false);
            repairBtn.setText("🔧 Починить ($0)");
            upgradeBtn.setText("🚀 Эволюция модели");
            upgradeBtn.setBaseColor(new Color(241, 196, 15));
            demolishBtn.setText("🗑️ Снести (50%)");
        } else {
            String coords = "[" + (selectedPlot.col + 1) + ", " + (selectedPlot.row + 1) + "]";
            if (!selectedPlot.hasTurbine()) {
                selectedPlotTitle.setText("Участок " + coords + " (Свободен)");
                selectedPlotSub.setText("Выберите ветряк для постройки:");
                if (selectedPlotDurabilityBar != null) {
                    selectedPlotDurabilityBar.setValue(0);
                    selectedPlotDurabilityBar.setString("Свободный участок");
                }
                buildBasicBtn.setText("🔨 Малый ($1k)");
                buildAdvBtn.setText("⚡ Средний ($3.2k)");
                buildIndBtn.setText("🌟 Гигант ($9k)");
                buildOffshoreBtn.setText("🌊 Офшор ($24k)");
                buildSolarBtn.setText("☀️ Гибрид ($55k)");
                buildVawtBtn.setText("🌀 VAWT ($140k)");
                buildBasicBtn.setEnabled(true);
                buildAdvBtn.setEnabled(true);
                buildIndBtn.setEnabled(true);
                buildOffshoreBtn.setEnabled(true);
                buildSolarBtn.setEnabled(true);
                buildVawtBtn.setEnabled(true);
                repairBtn.setEnabled(false);
                upgradeBtn.setEnabled(false);
                demolishBtn.setEnabled(false);
                repairBtn.setText("🔧 Починить");
                upgradeBtn.setText("🚀 Эволюция модели");
                upgradeBtn.setBaseColor(new Color(241, 196, 15));
                demolishBtn.setText("🗑️ Снести");
            } else {
                TurbineType cur = selectedPlot.turbine;
                selectedPlotTitle.setText(cur.name + " " + coords);
                selectedPlotSub.setText("⭐ Lv." + selectedPlot.level + "  |  Состояние: " + dfShort.format(selectedPlot.durability) + "%");
                if (selectedPlotDurabilityBar != null) {
                    int dur = (int)selectedPlot.durability;
                    selectedPlotDurabilityBar.setValue(dur);
                    selectedPlotDurabilityBar.setString("Прочность: " + dfShort.format(selectedPlot.durability) + "%");
                    if (dur < 35) {
                        selectedPlotDurabilityBar.setForeground(new Color(231, 76, 60));
                    } else if (dur < 75) {
                        selectedPlotDurabilityBar.setForeground(new Color(241, 196, 15));
                    } else {
                        selectedPlotDurabilityBar.setForeground(new Color(46, 204, 113));
                    }
                }

                updateGridButtonForEvolution(buildBasicBtn, "🔨 Малый", TurbineType.BASIC, cur);
                updateGridButtonForEvolution(buildAdvBtn, "⚡ Средний", TurbineType.ADVANCED, cur);
                updateGridButtonForEvolution(buildIndBtn, "🌟 Гигант", TurbineType.INDUSTRIAL, cur);
                updateGridButtonForEvolution(buildOffshoreBtn, "🌊 Офшор", TurbineType.OFFSHORE, cur);
                updateGridButtonForEvolution(buildSolarBtn, "☀️ Гибрид", TurbineType.SOLAR_HYBRID, cur);
                updateGridButtonForEvolution(buildVawtBtn, "🌀 VAWT", TurbineType.QUANTUM_VAWT, cur);

                boolean canRepair = selectedPlot.durability < 99.5;
                double repCost = (100.0 - selectedPlot.durability) * 1.5;
                repairBtn.setEnabled(canRepair);
                repairBtn.setText(canRepair ? "🔧 Починить ($" + dfMoney.format(repCost) + ")" : "🔧 Исправен (100%)");

                TurbineType next = cur.getNextTier();
                if (next != null) {
                    double diff = next.cost - cur.cost;
                    upgradeBtn.setEnabled(true);
                    upgradeBtn.setText("🚀 Эволюция: " + next.name + " (+$" + dfMoney.format(diff) + ")");
                    upgradeBtn.setBaseColor(next.themeColor);
                } else {
                    double upCost = cur.cost * 0.5 * selectedPlot.level;
                    upgradeBtn.setEnabled(true);
                    upgradeBtn.setText("⭐ Форсировать Lv." + (selectedPlot.level + 1) + " ($" + dfMoney.format(upCost) + ")");
                    upgradeBtn.setBaseColor(new Color(241, 196, 15));
                }

                double refund = cur.cost * 0.5;
                demolishBtn.setEnabled(true);
                demolishBtn.setText("🗑️ Снести (+$" + dfMoney.format(refund) + ")");
            }
        }

        // Кнопка массового ремонта
        double totalRepCost = 0.0;
        int brokenCount = 0;
        int burningCount = 0;
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.hasTurbine()) {
                    if (p.isBurning) burningCount++;
                    if (p.durability < 99.5 || p.isBurning) {
                        brokenCount++;
                        double need = (100.0 - p.durability) / 100.0;
                        totalRepCost += p.turbine.cost * 0.15 * need;
                    }
                }
            }
        }
        totalRepCost *= 0.85; // 15% wholesale discount
        if (brokenCount > 0) {
            repairAllBtn.setEnabled(balance >= totalRepCost);
            repairAllBtn.setText("🔧 Починить всё (" + brokenCount + " шт -15%: $" + dfMoney.format(totalRepCost) + ")");
        } else {
            repairAllBtn.setEnabled(false);
            repairAllBtn.setText("✅ Все турбины исправны (100%)");
        }

        if (burningCount > 0) {
            extinguishBtn.setVisible(true);
            extinguishBtn.setEnabled(balance >= 250.0);
            extinguishBtn.setText("🧯 Потушить пожары (" + burningCount + " шт: $250)");
        } else {
            extinguishBtn.setVisible(false);
        }

        // Кнопки биомов
        biomeValleyBtn.setText(currentBiome == Biome.VALLEY ? "🌲 Долина [АКТИВ]" : "🌲 Долина");
        biomeValleyBtn.setEnabled(currentBiome != Biome.VALLEY);

        boolean peaksUnlocked = unlockedBiomes.contains("PEAKS");
        biomePeaksBtn.setText(currentBiome == Biome.PEAKS ? "🏔️ Горы [АКТИВ]" : (peaksUnlocked ? "🏔️ Горы" : "🏔️ Горы ($15k)"));
        biomePeaksBtn.setEnabled(currentBiome != Biome.PEAKS);

        boolean oceanUnlocked = unlockedBiomes.contains("OCEAN");
        biomeOceanBtn.setText(currentBiome == Biome.OCEAN ? "🌊 Шельф [АКТИВ]" : (oceanUnlocked ? "🌊 Шельф" : "🌊 Шельф ($35k)"));
        biomeOceanBtn.setEnabled(currentBiome != Biome.OCEAN);

        // Обновление кнопок Биржи
        if (batteryCapacity <= 0) {
            buyBatteryBtn.setText("🔋 Купить батарею 1,000 кВт⋅ч ($4k)");
            batteryStatusLbl.setText("Батарея не установлена");
            batteryProgressBar.setValue(0);
        } else {
            double pct = (batteryStored / batteryCapacity) * 100.0;
            batteryStatusLbl.setText("Хранилище: " + dfShort.format(batteryStored) + " / " + (int)batteryCapacity + " кВт⋅ч (" + (int)pct + "%)");
            batteryProgressBar.setValue((int)pct);
            buyBatteryBtn.setText("🔋 Расширить накопитель (+$" + (int)(batteryCapacity < 5000 ? 14 : 45) + "k)");
        }

        double rate = marketPricePerKwh * marketMultiplier * ((currentSeason == Season.WINTER) ? 1.35 : 1.0);
        boolean isPeak = marketMultiplier > 1.8;
        marketPriceLbl.setText("Тариф: $" + dfMoney.format(rate) + " / кВт⋅ч " + (isPeak ? "⚡ [ПИКОВЫЙ СПРОС!]" : "[Обычный]"));
        marketPriceLbl.setForeground(isPeak ? new Color(255, 75, 75) : new Color(254, 202, 87));

        if (forecastLbl != null) {
            if (hiredMeteorologist) {
                forecastLbl.setText("🌦️ Прогноз: " + nextWeatherCondition + " (~" + dfShort.format(nextTargetWind) + " м/с через " + forecastCountdown + "с)");
            } else {
                forecastLbl.setText("🌦️ Прогноз погоды: [Наймите метеоролога в науке]");
            }
        }

        if (brokerStatusLbl != null) {
            brokerStatusLbl.setText("📈 ИИ-Брокер: " + (hiredBroker ? "🟢 АКТИВЕН (авто-сброс на пиках)" : "⚪ Не нанят"));
        }

        double dumpValue = batteryStored * rate;
        sellBatteryBtn.setText("⚡ Сбросить заряд на биржу (+$" + dfMoney.format(dumpValue) + ")");

        // Обновление кнопок Науки
        scienceBalanceLbl.setText("🔬 Очки исследований: " + dfShort.format(sciencePoints) + " SP");
        upgAeroBtn.setText(techAeroLevel >= 3 ? "🍃 Аэродинамика: МАКС (Lv.3)" : "🍃 Аэродинамика Lv." + (techAeroLevel + 1) + " (" + (techAeroLevel == 0 ? 40 : techAeroLevel == 1 ? 120 : 300) + " SP)");
        upgAeroBtn.setEnabled(techAeroLevel < 3);

        upgCarbonBtn.setText(techCarbonLevel >= 3 ? "🛡️ Углепластик: МАКС (Lv.3)" : "🛡️ Углепластик Lv." + (techCarbonLevel + 1) + " (" + (techCarbonLevel == 0 ? 50 : techCarbonLevel == 1 ? 150 : 400) + " SP)");
        upgCarbonBtn.setEnabled(techCarbonLevel < 3);

        upgLightningBtn.setText(techLightningRod ? "⚡ Громоотвод: УСТАНОВЛЕН" : "⚡ Громоотвод и защита (100 SP)");
        upgLightningBtn.setEnabled(!techLightningRod);

        upgRadarBtn.setText(techStormRadar ? "📡 Радар бурь: АКТИВЕН" : "📡 Радар бурь (150 SP)");
        upgRadarBtn.setEnabled(!techStormRadar);

        upgDronesUnlockBtn.setText(techDronesUnlocked ? "🤖 Чертежи дронов: ИЗУЧЕНО" : "🤖 Чертежи дронов (220 SP)");
        upgDronesUnlockBtn.setEnabled(!techDronesUnlocked);

        buyDroneBtn.setText("🚁 Купить ремонтного дрона ($5,000) [" + droneCount + " шт]");
        buyDroneBtn.setEnabled(techDronesUnlocked);

        // Специалисты
        hireMeteorologistBtn.setText(hiredMeteorologist ? "✅ Метеоролог нанят" : "🌦️ Метеоролог ($3.5k / 60 SP)");
        hireMeteorologistBtn.setEnabled(!hiredMeteorologist);

        hireBrokerBtn.setText(hiredBroker ? "✅ ИИ-Брокер нанят" : "📈 ИИ-Брокер ($7k / 100 SP)");
        hireBrokerBtn.setEnabled(!hiredBroker);

        hireChiefMechanicBtn.setText(hiredChiefMechanic ? "✅ Главный Механик нанят" : "🛡️ Главный Механик ($5k / 80 SP)");
        hireChiefMechanicBtn.setEnabled(!hiredChiefMechanic);

        // Обновление Rebirth
        rebirthInfoLbl.setText("Бонус: +" + (rebirthCount * 50) + "%  |  Требуется: $100,000 и 6 вышек");
    }

    private void updateGridButtonForEvolution(StyledButton btn, String baseName, TurbineType target, TurbineType current) {
        if (target == current) {
            btn.setText(baseName + " (Текущий)");
            btn.setEnabled(false);
        } else if (target.ordinal() < current.ordinal()) {
            btn.setText(baseName + " (Пройден)");
            btn.setEnabled(false);
        } else {
            double diff = target.cost - current.cost;
            String diffStr = diff >= 1000 ? "$" + (int)(diff / 1000) + "k" : "$" + (int)diff;
            btn.setText("⬆ " + baseName + " (+" + diffStr + ")");
            btn.setEnabled(true);
        }
    }

    private void updateUI() {
        balanceVal.setText("$" + dfMoney.format(balance));
        windVal.setText(dfShort.format(windSpeed) + " м/с • " + currentBiome.title + " • " + weatherCondition);

        String sign = incomePerSecond >= 0 ? "+$" : "-$";
        powerVal.setText(dfShort.format(currentPowerOutput) + " кВт (" + sign + dfMoney.format(Math.abs(incomePerSecond)) + "/с)");

        if (batteryCapacity > 0) {
            batteryVal.setText(dfShort.format(batteryStored) + " кВт⋅ч");
        } else {
            batteryVal.setText("Нет");
        }

        scienceVal.setText(dfShort.format(sciencePoints) + " SP");
        rebirthBadge.setText("R" + rebirthCount + " (x" + dfShort.format(1.0 + rebirthCount * 0.5) + ")");

        boolean isNight = (timeOfDay >= 0.45 && timeOfDay <= 0.88);
        String todStr = isNight ? "🌙 Ночь" : (timeOfDay > 0.38 && timeOfDay < 0.45) ? "🌇 Закат" : "☀️ День";
        timeBadge.setText(currentSeason.title + " • " + todStr);
        if (isNight) {
            timeBadge.setForeground(new Color(130, 200, 255));
        } else if (timeOfDay > 0.38 && timeOfDay < 0.45) {
            timeBadge.setForeground(new Color(255, 159, 67));
        } else {
            timeBadge.setForeground(new Color(255, 234, 167));
        }

        if (activeContract != null) {
            contractStatusLbl.setText("<html><b>" + activeContract.title + "</b><br>" +
                    "Нужно: <b>" + (int)activeContract.requiredKw + " кВт</b> &nbsp;|&nbsp; " +
                    "Осталось: <font color='#FECA57'>" + activeContract.remainingSeconds + "с</font><br>" +
                    "Награда: <font color='#2ECC71'>+$" + (int)(activeContract.reward * (1.0 + rebirthCount * 0.5)) + "</font></html>");
        } else {
            contractStatusLbl.setText("<html>Ожидание нового заказа мэрии... (" + contractCooldownTimer + "с)</html>");
        }

        updateControlButtons();
    }

    // ==========================================
    // === 9. КАНВАС С НЕОНОМ, ДРОНАМИ И ГОРОДОМ
    // ==========================================
    private class GameCanvas extends JPanel {
        public GameCanvas() {
            setBackground(new Color(41, 128, 185));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            boolean isNight = (timeOfDay >= 0.45 && timeOfDay <= 0.88);
            boolean isDusk = (timeOfDay > 0.38 && timeOfDay < 0.45);

            Color skyTop, skyBot;
            if (tornadoActive) {
                skyTop = new Color(28, 36, 48);
                skyBot = new Color(15, 20, 28);
            } else if (isNight) {
                skyTop = new Color(12, 18, 34);
                skyBot = new Color(24, 34, 58);
            } else if (isDusk) {
                skyTop = new Color(85, 45, 110);
                skyBot = new Color(245, 125, 80);
            } else {
                skyTop = new Color(90, 185, 245);
                skyBot = new Color(55, 145, 215);
            }

            GradientPaint skyGrad = new GradientPaint(0, 0, skyTop, 0, h, skyBot);
            g2.setPaint(skyGrad);
            g2.fillRect(0, 0, w, h);

            if (isNight && !tornadoActive) {
                g2.setColor(new Color(255, 255, 255, 160));
                for (Point st : stars) {
                    if (st.x < w && st.y < h * 0.4) {
                        g2.fillOval(st.x, st.y, 2, 2);
                    }
                }
            }

            drawClouds(g2, isNight);
            drawBirds(g2, isNight);

            int groundY = (int)(h * 0.12);
            int farmH = (int)(h * 0.72) - groundY;

            Color gTop, gBot;
            if (currentBiome == Biome.OCEAN) {
                gTop = isNight ? new Color(8, 28, 52) : new Color(20, 85, 145);
                gBot = isNight ? new Color(4, 16, 32) : new Color(12, 55, 95);
            } else if (currentBiome == Biome.PEAKS) {
                if (currentSeason == Season.WINTER) {
                    gTop = isNight ? new Color(120, 135, 150) : new Color(220, 232, 245);
                    gBot = isNight ? new Color(80, 95, 110) : new Color(175, 190, 205);
                } else {
                    gTop = isNight ? new Color(38, 44, 52) : new Color(95, 105, 115);
                    gBot = isNight ? new Color(24, 28, 35) : new Color(65, 75, 85);
                }
            } else { // VALLEY
                gTop = isNight ? currentSeason.groundTop.darker().darker() : currentSeason.groundTop;
                gBot = isNight ? currentSeason.groundBot.darker().darker() : currentSeason.groundBot;
            }

            GradientPaint groundGrad = new GradientPaint(0, groundY, gTop, 0, groundY + farmH, gBot);
            g2.setPaint(groundGrad);
            g2.fillRect(0, groundY, w, farmH);

            if (currentBiome == Biome.OCEAN) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1.5f));
                long wt = System.currentTimeMillis();
                for (int wy = groundY + 20; wy < groundY + farmH; wy += 35) {
                    int shift = (int)((wt / 40 + wy * 3) % 80) - 40;
                    for (int wx = shift - 60; wx < w + 60; wx += 60) {
                        g2.drawArc(wx, wy, 40, 10, 0, 180);
                    }
                }
            }

            int cellW = w / COLS;
            int cellH = farmH / ROWS;

            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    int px = c * cellW + 12;
                    int py = groundY + r * cellH + 10;
                    int pw = cellW - 24;
                    int ph = cellH - 20;

                    Plot plot = plots[c][r];
                    boolean isSelected = (selectedPlot != null && selectedPlot.col == c && selectedPlot.row == r);
                    drawTycoonPlot(g2, px, py, pw, ph, plot, isSelected, isNight);
                }
            }

            drawCitySkyline(g2, 0, groundY + farmH, w, h - (groundY + farmH), isNight);
            drawDrones(g2);

            if (tornadoActive) {
                drawTornado(g2, (int)tornadoX, h / 2);
            }

            if (weatherCondition.contains("Гроза") || tornadoActive) {
                g2.setColor(new Color(180, 210, 255, 110));
                g2.setStroke(new BasicStroke(1.2f));
                for (RainDrop rd : rainDrops) {
                    g2.drawLine(rd.x, rd.y, rd.x + (int)(windSpeed * 0.4), rd.y + rd.len);
                }
            }

            if (lightningFlash) {
                g2.setColor(new Color(255, 255, 255, 190));
                g2.fillRect(0, 0, w, h);
            }

            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, isNight ? 50 : 85));
            int windLineLen = Math.max(18, (int)(windSpeed * 4.2));
            for (Point pt : windParticles) {
                g2.drawLine(pt.x, pt.y, pt.x + windLineLen, pt.y);
            }

            if (currentSeason == Season.WINTER) {
                g2.setColor(new Color(255, 255, 255, isNight ? 185 : 230));
                for (int i = 0; i < snowFlakes.size(); i++) {
                    SnowFlake sf = snowFlakes.get(i);
                    g2.fillOval((int)sf.x, (int)sf.y, (int)sf.size, (int)sf.size);
                }
            }

            for (int i = 0; i < smokeParticles.size(); i++) {
                SmokeParticle smp = smokeParticles.get(i);
                int alphaInt = (int)(Math.max(0, Math.min(1, smp.alpha)) * 255);
                g2.setColor(new Color(smp.color.getRed(), smp.color.getGreen(), smp.color.getBlue(), alphaInt));
                g2.fillOval((int)smp.x - (int)smp.size / 2, (int)smp.y - (int)smp.size / 2, (int)smp.size, (int)smp.size);
            }

            for (Sparkle sp : sparkles) {
                g2.setColor(new Color(sp.color.getRed(), sp.color.getGreen(), sp.color.getBlue(), (int)(sp.alpha * 255)));
                g2.fillOval((int)sp.x - sp.size / 2, (int)sp.y - sp.size / 2, sp.size, sp.size);
            }

            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            for (FloatingText ft : floatingTexts) {
                int alphaInt = (int)(Math.max(0, Math.min(1, ft.alpha)) * 255);
                g2.setColor(new Color(0, 0, 0, alphaInt));
                g2.drawString(ft.text, (int)ft.x + 1, (int)ft.y + 1);
                g2.setColor(new Color(ft.color.getRed(), ft.color.getGreen(), ft.color.getBlue(), alphaInt));
                g2.drawString(ft.text, (int)ft.x, (int)ft.y);
            }

            if (comboMultiplier > 1.0 || comboClicks > 0) {
                drawComboBadge(g2, w / 2, groundY - 12);
            }

            boolean anyBurning = false;
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    if (plots[c][r].isBurning) {
                        anyBurning = true;
                        break;
                    }
                }
                if (anyBurning) break;
            }
            if (anyBurning) {
                long pulse = System.currentTimeMillis();
                int fireAlpha = (int)(180 + 70 * Math.sin(pulse * 0.012));
                g2.setColor(new Color(231, 76, 60, fireAlpha));
                g2.fillRoundRect(w / 2 - 190, 52, 380, 30, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
                String fireTxt = "🔥 АВАРИЯ: ПОЖАР НА ТУРБИНЕ! НАЖМИТЕ ДЛЯ ТУШЕНИЯ!";
                int fw = g2.getFontMetrics().stringWidth(fireTxt);
                g2.drawString(fireTxt, w / 2 - fw / 2, 72);
            }

            if (tornadoActive) {
                g2.setColor(new Color(231, 76, 60, 220));
                g2.fillRoundRect(w / 2 - 210, 15, 420, 36, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(w / 2 - 210, 15, 420, 36, 12, 12);

                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
                String alert = techStormRadar ? "🚨 ТОРНАДО В РЕГИОНЕ! РАДАР ЗАЩИЩАЕТ ВЕТРОПАРК 🛡️" : "🚨 ТОРНАДО В РЕГИОНЕ! ШАНС УДАРА: 10%";
                int aw = g2.getFontMetrics().stringWidth(alert);
                g2.drawString(alert, w / 2 - aw / 2, 38);
            }

            if (climateSummitTimer > 0) {
                g2.setColor(new Color(46, 204, 113, 220));
                g2.fillRoundRect(20, 15, 260, 28, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
                g2.drawString("🌍 САММИТ ООН: СУБСИДИИ x2 (" + climateSummitTimer + "с)", 30, 33);
            }
        }

        private void drawBirds(Graphics2D g2, boolean isNight) {
            g2.setColor(isNight ? new Color(60, 75, 95) : new Color(40, 50, 65));
            g2.setStroke(new BasicStroke(1.5f));
            for (Bird b : birds) {
                int bx = (int)b.x;
                int by = (int)b.y;
                int wingFlap = (int)(Math.sin(b.wingAngle) * 5.0);
                g2.drawLine(bx - 6, by - wingFlap, bx, by);
                g2.drawLine(bx, by, bx + 6, by - wingFlap);
            }
        }

        private void drawDrones(Graphics2D g2) {
            for (RepairDrone d : drones) {
                int dx = (int)d.x;
                int dy = (int)d.y;

                g2.setColor(new Color(45, 55, 75));
                g2.fillRoundRect(dx - 10, dy - 5, 20, 10, 4, 4);

                g2.setColor(new Color(90, 105, 125));
                g2.drawLine(dx - 14, dy - 8, dx + 14, dy + 8);
                g2.drawLine(dx - 14, dy + 8, dx + 14, dy - 8);

                g2.setColor(new Color(0, 210, 211, 160));
                g2.drawOval(dx - 18, dy - 11, 8, 4);
                g2.drawOval(dx + 10, dy - 11, 8, 4);
                g2.drawOval(dx - 18, dy + 7, 8, 4);
                g2.drawOval(dx + 10, dy + 7, 8, 4);

                g2.setColor(d.isWelding ? new Color(255, 220, 50) : new Color(46, 204, 113));
                g2.fillOval(dx - 2, dy - 2, 4, 4);

                if (d.isWelding && d.targetPlot != null) {
                    if (d.targetPlot.isBurning) {
                        g2.setColor(new Color(52, 152, 219, 220));
                        g2.setStroke(new BasicStroke(2.2f));
                        g2.drawLine(dx, dy + 5, dx, dy + 22);
                    } else {
                        g2.setColor(new Color(0, 230, 255, 200));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawLine(dx, dy + 5, dx, dy + 18);
                    }
                }
            }
        }

        private void drawComboBadge(Graphics2D g2, int cx, int cy) {
            String txt = "⚡ ТУРБО БУСТ x" + (int)comboMultiplier + "!";
            if (comboMultiplier >= 5.0) txt = "🔥 МЕГА ОВЕРДРАЙВ x5! 🔥";

            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
            int tw = g2.getFontMetrics().stringWidth(txt);

            Color bg = (comboMultiplier >= 5) ? new Color(231, 76, 60, 220) :
                       (comboMultiplier >= 3) ? new Color(243, 156, 18, 220) : new Color(46, 204, 113, 220);

            g2.setColor(bg);
            g2.fillRoundRect(cx - tw / 2 - 12, cy - 14, tw + 24, 28, 14, 14);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(cx - tw / 2 - 12, cy - 14, tw + 24, 28, 14, 14);

            g2.drawString(txt, cx - tw / 2, cy + 6);
        }

        private void drawCitySkyline(Graphics2D g2, int x, int y, int w, int h, boolean isNight) {
            Color citySky = isNight ? new Color(15, 22, 36) : new Color(40, 55, 75);
            g2.setColor(citySky);
            g2.fillRect(x, y, w, h);

            int ecoLevel = 1;
            if (totalEnergyGenerated > 8000.0) ecoLevel = 4;
            else if (totalEnergyGenerated > 2500.0) ecoLevel = 3;
            else if (totalEnergyGenerated > 500.0) ecoLevel = 2;

            int numBuildings = 14;
            int bWidth = w / numBuildings;
            Random cityRnd = new Random(42);

            for (int i = 0; i < numBuildings; i++) {
                int bx = x + i * bWidth;
                int bh = 30 + cityRnd.nextInt(h - 35) + (ecoLevel * 8);
                int by = y + h - bh;

                Color bColor;
                if (ecoLevel >= 4) {
                    bColor = isNight ? new Color(18 + (i % 3) * 6, 40 + (i % 3) * 8, 55 + (i % 3) * 8) :
                                       new Color(45 + (i % 3) * 10, 85 + (i % 3) * 10, 110 + (i % 3) * 10);
                } else {
                    bColor = isNight ? new Color(22 + (i % 3) * 6, 30 + (i % 3) * 6, 48 + (i % 3) * 8) :
                                       new Color(60 + (i % 3) * 10, 75 + (i % 3) * 10, 95 + (i % 3) * 10);
                }
                g2.setColor(bColor);
                g2.fillRect(bx + 2, by, bWidth - 4, bh);

                int floors = bh / 10;
                for (int fl = 0; fl < floors; fl++) {
                    int winY = by + 6 + fl * 9;
                    for (int winX = bx + 6; winX < bx + bWidth - 8; winX += 7) {
                        if (cityRnd.nextBoolean()) {
                            if (isNight) {
                                if (ecoLevel >= 4 && cityRnd.nextInt(4) == 0) {
                                    g2.setColor(new Color(0, 255, 180));
                                } else {
                                    g2.setColor(cityRnd.nextInt(3) == 0 ? new Color(0, 240, 255) : new Color(255, 225, 100));
                                }
                            } else {
                                g2.setColor(new Color(170, 195, 220, 140));
                            }
                            g2.fillRect(winX, winY, 4, 5);
                        }
                    }
                }

                if (ecoLevel >= 3 && i % 2 == 0) {
                    g2.setColor(new Color(46, 204, 113, isNight ? 180 : 230));
                    g2.fillRoundRect(bx + 4, by - 6, bWidth - 8, 7, 4, 4);
                }

                if (ecoLevel >= 4 && i % 3 == 0) {
                    g2.setColor(new Color(0, 220, 255, isNight ? 190 : 120));
                    g2.drawLine(bx + bWidth / 2, by, bx + bWidth / 2, by - 16);
                    g2.fillOval(bx + bWidth / 2 - 2, by - 18, 4, 4);
                }
            }

            g2.setColor(new Color(25, 30, 40));
            g2.fillRect(x, y + h - 14, w, 14);
            g2.setColor(new Color(241, 196, 15, isNight ? 180 : 100));
            for (int dx = x; dx < x + w; dx += 28) {
                g2.fillRect(dx, y + h - 8, 14, 2);
            }
        }

        private void drawTornado(Graphics2D g2, int tx, int ty) {
            AffineTransform old = g2.getTransform();
            g2.translate(tx, ty);

            int layers = 14;
            for (int i = 0; i < layers; i++) {
                int yOffset = (i - layers / 2) * 22;
                int ringW = 25 + i * 9;
                int ringH = 14 + i * 4;

                double rot = Math.toRadians(tornadoAngle + i * 25);
                AffineTransform rtx = g2.getTransform();
                g2.rotate(rot, 0, yOffset);

                g2.setColor(new Color(45, 52, 65, 140));
                g2.fillOval(-ringW / 2, yOffset - ringH / 2, ringW, ringH);

                g2.setColor(new Color(25, 30, 40, 190));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(-ringW / 2, yOffset - ringH / 2, ringW, ringH);

                g2.setTransform(rtx);
            }

            g2.setTransform(old);
        }

        private void drawClouds(Graphics2D g2, boolean isNight) {
            Color cloudCol = tornadoActive ? new Color(140, 150, 165, 180) :
                             isNight ? new Color(90, 105, 130, 80) : new Color(255, 255, 255, 140);
            g2.setColor(cloudCol);
            for (Cloud cl : clouds) {
                int cx = (int)cl.x;
                int cy = (int)cl.y;
                int s = (int)(40 * cl.scale);
                g2.fillOval(cx, cy, s * 2, s);
                g2.fillOval(cx + s / 2, cy - s / 3, s * 2, (int)(s * 1.2));
                g2.fillOval(cx + s, cy, (int)(s * 1.8), s);
            }
        }

        private void drawTycoonPlot(Graphics2D g2, int x, int y, int w, int h, Plot plot, boolean isSelected, boolean isNight) {
            int depth = 8;

            Color sideCol, baseCol, surfCol, selCol, borderCol;
            if (currentBiome == Biome.OCEAN) {
                sideCol = isNight ? new Color(10, 25, 45, 140) : new Color(15, 55, 90, 140);
                baseCol = isNight ? new Color(15, 38, 65) : new Color(24, 75, 120);
                surfCol = isNight ? new Color(20, 52, 90) : new Color(35, 105, 160);
                selCol = isNight ? new Color(40, 95, 150) : new Color(52, 152, 219);
                borderCol = new Color(241, 196, 15);
            } else if (currentBiome == Biome.PEAKS) {
                if (currentSeason == Season.WINTER) {
                    sideCol = isNight ? new Color(60, 70, 80, 140) : new Color(150, 165, 180, 140);
                    baseCol = isNight ? new Color(75, 85, 95) : new Color(175, 190, 205);
                    surfCol = isNight ? new Color(105, 118, 132) : new Color(215, 228, 240);
                    selCol = new Color(245, 250, 255);
                } else {
                    sideCol = isNight ? new Color(25, 28, 35, 140) : new Color(60, 68, 78, 140);
                    baseCol = isNight ? new Color(35, 40, 48) : new Color(80, 90, 100);
                    surfCol = isNight ? new Color(50, 58, 68) : new Color(110, 120, 132);
                    selCol = new Color(145, 158, 172);
                }
                borderCol = new Color(189, 195, 199);
            } else { // VALLEY
                if (currentSeason == Season.WINTER) {
                    sideCol = isNight ? new Color(90, 105, 120, 130) : new Color(170, 185, 200, 130);
                    baseCol = isNight ? new Color(110, 125, 140) : new Color(195, 210, 225);
                    surfCol = isNight ? new Color(130, 148, 165) : new Color(230, 240, 250);
                    selCol = new Color(255, 255, 255);
                } else if (currentSeason == Season.AUTUMN) {
                    sideCol = isNight ? new Color(65, 40, 15, 120) : new Color(130, 80, 30, 120);
                    baseCol = isNight ? new Color(80, 50, 20) : new Color(165, 100, 35);
                    surfCol = isNight ? new Color(100, 65, 25) : new Color(195, 125, 45);
                    selCol = new Color(243, 156, 18);
                } else {
                    sideCol = isNight ? new Color(12, 45, 25, 120) : new Color(25, 110, 60, 110);
                    baseCol = isNight ? new Color(15, 60, 32) : new Color(30, 130, 68);
                    surfCol = isNight ? new Color(32, 120, 65) : new Color(88, 214, 141);
                    selCol = isNight ? new Color(50, 170, 100) : new Color(110, 240, 155);
                }
                borderCol = isNight ? new Color(45, 150, 80) : new Color(125, 230, 165);
            }

            g2.setColor(sideCol);
            g2.fillRoundRect(x + 2, y + depth + 4, w - 4, h - depth, 20, 20);

            g2.setColor(baseCol);
            g2.fillRoundRect(x, y + depth, w, h - depth, 18, 18);

            g2.setColor(isSelected ? selCol : surfCol);
            g2.fillRoundRect(x, y, w, h - depth, 18, 18);

            if (isSelected) {
                g2.setColor(new Color(254, 202, 87));
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawRoundRect(x, y, w, h - depth, 18, 18);
            } else {
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y, w, h - depth, 18, 18);
            }

            if (plot.isBurning) {
                long pulseTime = System.currentTimeMillis();
                int fireAlpha = (int)(180 + 70 * Math.sin(pulseTime * 0.015));
                g2.setColor(new Color(231, 76, 60, fireAlpha));
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawRoundRect(x, y, w, h - depth, 18, 18);
            }

            int cx = x + w / 2;
            int cy = y + (int)((h - depth) * 0.76);

            if (plot.hasTurbine()) {
                drawTurbine3D(g2, cx, cy, plot, isNight);
            } else {
                int padW = Math.min(130, w - 30);
                int padH = 40;
                int padX = cx - padW / 2;
                int padY = y + (h - depth) / 2 - padH / 2;

                long t = System.currentTimeMillis();
                float pulse = (float)(0.7 + 0.3 * Math.sin(t * 0.005));

                Color btnBg1 = (currentBiome == Biome.OCEAN) ? new Color(24, 110, 180) :
                               (currentBiome == Biome.PEAKS) ? new Color(75, 85, 95) : new Color(39, 174, 96);
                Color btnBg2 = (currentBiome == Biome.OCEAN) ? new Color(41, 128, 185) :
                               (currentBiome == Biome.PEAKS) ? new Color(95, 105, 118) : new Color(46, 204, 113);

                g2.setColor(btnBg1);
                g2.fillRoundRect(padX, padY + 4, padW, padH, 14, 14);

                g2.setColor(btnBg2);
                g2.fillRoundRect(padX, padY, padW, padH, 14, 14);

                g2.setColor(new Color(255, 255, 255, (int)(180 * pulse)));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(padX, padY, padW, padH, 14, 14);

                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                String btnText = (currentBiome == Biome.OCEAN) ? "➕ ПЛАТФОРМА" : "➕ ПОСТРОИТЬ";
                int strW = g2.getFontMetrics().stringWidth(btnText);
                g2.drawString(btnText, cx - strW / 2, padY + 17);

                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
                g2.setColor(new Color(235, 255, 240));
                String costText = "от $1,000";
                int strW2 = g2.getFontMetrics().stringWidth(costText);
                g2.drawString(costText, cx - strW2 / 2, padY + 31);
            }
        }

        private void drawTurbine3D(Graphics2D g2, int cx, int baseCy, Plot plot, boolean isNight) {
            int mastH = 86;
            int hubCy = baseCy - mastH;

            if (plot.turbine == TurbineType.OFFSHORE || currentBiome == Biome.OCEAN) {
                g2.setColor(new Color(0, 119, 182, 160));
                g2.fillOval(cx - 36, baseCy - 12, 72, 24);
                g2.setColor(new Color(0, 180, 216, 220));
                g2.drawOval(cx - 36, baseCy - 12, 72, 24);

                g2.setColor(new Color(241, 196, 15));
                g2.fillRect(cx - 24, baseCy - 8, 48, 6);
                g2.setColor(new Color(212, 143, 0));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(cx - 18, baseCy - 2, cx - 10, baseCy + 8);
                g2.drawLine(cx + 18, baseCy - 2, cx + 10, baseCy + 8);
            } else if (plot.turbine == TurbineType.SOLAR_HYBRID) {
                g2.setColor(new Color(25, 45, 80));
                g2.fillRoundRect(cx - 32, baseCy - 10, 64, 18, 6, 6);
                g2.setColor(new Color(52, 152, 219));
                g2.drawRoundRect(cx - 32, baseCy - 10, 64, 18, 6, 6);

                g2.setColor(new Color(241, 196, 15, 180));
                g2.drawLine(cx - 28, baseCy - 1, cx + 28, baseCy - 1);
                g2.drawLine(cx - 10, baseCy - 9, cx - 10, baseCy + 7);
                g2.drawLine(cx + 10, baseCy - 9, cx + 10, baseCy + 7);
            } else {
                g2.setColor(isNight ? new Color(70, 80, 95) : new Color(130, 140, 150));
                g2.fillOval(cx - 28, baseCy - 8, 56, 16);
                g2.setColor(isNight ? new Color(110, 120, 135) : new Color(190, 200, 210));
                g2.fillOval(cx - 26, baseCy - 11, 52, 14);
            }

            Path2D mast = new Path2D.Double();
            mast.moveTo(cx - 8, baseCy - 4);
            mast.lineTo(cx + 8, baseCy - 4);
            mast.lineTo(cx + 3, hubCy);
            mast.lineTo(cx - 3, hubCy);
            mast.closePath();

            GradientPaint mastShading = new GradientPaint(
                    cx - 8, baseCy, isNight ? new Color(150, 165, 185) : new Color(245, 248, 255),
                    cx + 8, baseCy, isNight ? new Color(80, 95, 115) : new Color(160, 175, 190)
            );
            g2.setPaint(mastShading);
            g2.fill(mast);

            g2.setColor(isNight ? new Color(75, 90, 110) : new Color(135, 150, 165));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(mast);

            if (plot.turbine == TurbineType.QUANTUM_VAWT) {
                g2.setColor(new Color(40, 45, 60));
                g2.fillRoundRect(cx - 14, hubCy + 20, 28, 16, 6, 6);

                double rotAngle = Math.toRadians(plot.bladeAngle);
                int helixH = 70;
                int helixTopY = hubCy - 20;

                g2.setColor(new Color(255, 80, 160, isNight ? 220 : 180));
                g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                for (int side = -1; side <= 1; side += 2) {
                    Path2D helix = new Path2D.Double();
                    for (int step = 0; step <= 20; step++) {
                        double t = step / 20.0;
                        double curY = helixTopY + t * helixH;
                        double curX = cx + Math.sin(rotAngle + t * Math.PI * 2) * 26.0 * side;
                        if (step == 0) helix.moveTo(curX, curY);
                        else helix.lineTo(curX, curY);
                    }
                    g2.draw(helix);
                }

                g2.setColor(new Color(255, 255, 255, 230));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(cx, helixTopY, cx, helixTopY + helixH);

            } else {
                g2.setColor(isNight ? new Color(150, 165, 185) : new Color(235, 242, 250));
                g2.fillRoundRect(cx - 11, hubCy - 7, 22, 14, 6, 6);
                g2.setColor(plot.turbine.themeColor);
                g2.fillRect(cx - 11, hubCy - 2, 22, 4);

                boolean beaconOn = (System.currentTimeMillis() / 450) % 2 == 0;
                if (beaconOn) {
                    g2.setColor(new Color(255, 75, 75, isNight ? 220 : 140));
                    g2.fillOval(cx - 6, hubCy - 15, 12, 12);
                    g2.setColor(new Color(255, 40, 40));
                    g2.fillOval(cx - 3, hubCy - 12, 6, 6);
                }

                AffineTransform oldTx = g2.getTransform();
                g2.translate(cx, hubCy);
                g2.rotate(Math.toRadians(plot.bladeAngle));

                double bladeLen = (plot.turbine == TurbineType.BASIC) ? 50.0 :
                                  (plot.turbine == TurbineType.ADVANCED) ? 62.0 :
                                  (plot.turbine == TurbineType.INDUSTRIAL) ? 74.0 :
                                  (plot.turbine == TurbineType.OFFSHORE) ? 82.0 : 76.0;

                for (int i = 0; i < 3; i++) {
                    g2.rotate(Math.toRadians(120.0));
                    Path2D blade = new Path2D.Double();
                    blade.moveTo(-3, 0);
                    blade.curveTo(-5, -bladeLen * 0.45, -4, -bladeLen * 0.85, 0, -bladeLen);
                    blade.curveTo(4, -bladeLen * 0.85, 5, -bladeLen * 0.45, 3, 0);
                    blade.closePath();

                    if (isNight) {
                        g2.setColor(new Color(30, 40, 55));
                        g2.fill(blade);

                        g2.setColor(new Color(plot.turbine.neonColor.getRed(), plot.turbine.neonColor.getGreen(), plot.turbine.neonColor.getBlue(), 90));
                        g2.setStroke(new BasicStroke(4.0f));
                        g2.draw(blade);

                        g2.setColor(plot.turbine.neonColor);
                        g2.setStroke(new BasicStroke(1.8f));
                        g2.draw(blade);

                        g2.fillOval(-3, -(int)bladeLen, 6, 9);
                    } else {
                        g2.setColor(new Color(255, 255, 255));
                        g2.fill(blade);
                        g2.setColor(new Color(185, 195, 210));
                        g2.setStroke(new BasicStroke(1.0f));
                        g2.draw(blade);

                        g2.setColor(plot.turbine.themeColor);
                        g2.fillOval(-3, -(int)bladeLen, 6, 10);
                    }
                }

                g2.setColor(isNight ? new Color(160, 180, 205) : new Color(225, 235, 245));
                g2.fillOval(-6, -6, 12, 12);
                g2.setColor(new Color(100, 115, 135));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(-6, -6, 12, 12);

                g2.setTransform(oldTx);
            }

            int barW = 58;
            int barH = 7;
            int barX = cx - barW / 2;
            int barY = baseCy + 8;

            g2.setColor(new Color(20, 30, 40, 190));
            g2.fillRoundRect(barX, barY, barW, barH, 6, 6);

            Color hpColor = (plot.durability > 50) ? new Color(46, 204, 113) :
                            (plot.durability > 25) ? new Color(241, 196, 15) : new Color(231, 76, 60);

            int curW = (int)((plot.durability / 100.0) * (barW - 2));
            if (curW > 0) {
                g2.setColor(hpColor);
                g2.fillRoundRect(barX + 1, barY + 1, curW, barH - 2, 4, 4);
            }

            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
            String lvlStr = "⭐ Lv." + plot.level;
            int lw = g2.getFontMetrics().stringWidth(lvlStr);
            g2.setColor(new Color(25, 35, 45, 180));
            g2.fillRoundRect(cx - lw / 2 - 4, hubCy - 28, lw + 8, 14, 8, 8);
            g2.setColor(new Color(254, 202, 87));
            g2.drawString(lvlStr, cx - lw / 2, hubCy - 17);

            if (plot.durability <= 0.0) {
                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
                g2.setColor(new Color(231, 76, 60));
                String broke = "⚠️ СЛОМАН!";
                int bw = g2.getFontMetrics().stringWidth(broke);
                g2.drawString(broke, cx - bw / 2, hubCy - 34);
            }

            if (plot.isBurning) {
                long pulseTime = System.currentTimeMillis();
                int flameR = 14 + (int)(Math.sin(pulseTime * 0.02) * 5);
                g2.setColor(new Color(255, 69, 0, 185));
                g2.fillOval(cx - flameR, hubCy - flameR, flameR * 2, flameR * 2);
                g2.setColor(new Color(255, 215, 0, 220));
                g2.fillOval(cx - flameR / 2, hubCy - flameR / 2, flameR, flameR);

                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
                g2.setColor(new Color(255, 60, 60));
                String fireStr = "🔥 ПОЖАР!";
                int fw = g2.getFontMetrics().stringWidth(fireStr);
                g2.drawString(fireStr, cx - fw / 2, hubCy - 36);
            }
        }
    }

    // ==========================================
    // === 10. ТОЧКА ВХОДА (MAIN)
    // ==========================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WindTycoon game = new WindTycoon();
            game.setVisible(true);
        });
    }
}
