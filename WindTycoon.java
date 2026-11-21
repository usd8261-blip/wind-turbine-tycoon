import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Wind Turbine Tycoon (Roblox Edition with Full Save System)
 * Включает:
 * - Автосохранение каждые 20 секунд и при выходе из игры (savegame.dat).
 * - Автозагрузка прогресса при старте.
 * - Кнопка ручного сохранения и сброса («Новая игра»).
 * - Турбо-кликер с комбо x2/x3/x5.
 * - Город на горизонте и срочные контракты.
 * - Аркадные MIDI-звуки.
 * - Цикл Дня и Ночи с неоновой LED-подсветкой.
 * - Торнадо и страхование.
 */
public class WindTycoon extends JFrame {

    // === ЗВУКОВАЯ СИСТЕМА (ЧИСТЫЙ JAVA MIDI) ===
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
                    }
                } catch (Exception ignored) {}
            }).start();
        }
    }

    // === ЭКОНОМИКА И ИГРОВОЕ СОСТОЯНИЕ ===
    private double balance = 1500.0;
    private double totalEnergyGenerated = 0.0;
    private double currentPowerOutput = 0.0;
    private double incomePerSecond = 0.0;

    // Цикл Дня и Ночи
    private double timeOfDay = 0.1;

    // Календарь
    private int gameDays = 1;
    private int gameMonths = 1;

    // Страховка
    private boolean isInsured = false;
    private final double insuranceMonthlyCost = 10.0;
    private final double insurancePayoutPerTurbine = 800.0;

    // Персонал
    private int engineersCount = 0;
    private final double engineerHireCost = 250.0;
    private final double engineerSalaryPerSec = 1.0;

    // Турбо-кликер и Комбо
    private int comboClicks = 0;
    private double comboMultiplier = 1.0;
    private int comboDecayTimer = 0;

    // Городские контракты
    private static class CityContract {
        String title;
        double requiredKw;
        double reward;
        int durationSeconds;
        int remainingSeconds;
        int progressSeconds = 0;
        boolean active = false;
    }
    private CityContract activeContract = null;
    private int contractCooldownTimer = 10;

    // Погода и Торнадо
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

    // Сетка участков (4x3 = 12 платформ)
    private static final int COLS = 4;
    private static final int ROWS = 3;
    private final Plot[][] plots = new Plot[COLS][ROWS];
    private Plot selectedPlot = null;

    // Файл сохранения
    private static final String SAVE_FILE = "savegame.dat";

    // Всплывающие тексты (Floating Cash)
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
    private final List<FloatingText> floatingTexts = new ArrayList<>();

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
    private final List<Sparkle> sparkles = new ArrayList<>();

    // Звезды
    private final List<Point> stars = new ArrayList<>();
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
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Point> windParticles = new ArrayList<>();

    // Типы ветряков
    public enum TurbineType {
        BASIC("Малый ветряк", 1000.0, 50.0, 4.80, 0.04, new Color(46, 204, 113), new Color(0, 255, 180)),
        ADVANCED("Средний ветряк", 3200.0, 180.0, 18.00, 0.07, new Color(52, 152, 219), new Color(0, 220, 255)),
        INDUSTRIAL("Гигант ветропарка", 9000.0, 550.0, 68.00, 0.10, new Color(155, 89, 182), new Color(255, 75, 200));

        final String name;
        final double cost;
        final double maxPowerKw;
        final double baseIncomePerSec;
        final double wearRate;
        final Color themeColor;
        final Color neonColor;

        TurbineType(String name, double cost, double maxPowerKw, double baseIncomePerSec, double wearRate, Color themeColor, Color neonColor) {
            this.name = name;
            this.cost = cost;
            this.maxPowerKw = maxPowerKw;
            this.baseIncomePerSec = baseIncomePerSec;
            this.wearRate = wearRate;
            this.themeColor = themeColor;
            this.neonColor = neonColor;
        }
    }

    public static class Plot {
        int col, row;
        TurbineType turbine = null;
        double durability = 100.0;
        double bladeAngle = 0.0;
        int level = 1;

        Plot(int c, int r) {
            this.col = c;
            this.row = r;
            this.bladeAngle = new Random().nextDouble() * 360.0;
        }

        boolean hasTurbine() {
            return turbine != null;
        }
    }

    // GUI Элементы
    private GameCanvas canvas;
    private JLabel balanceVal;
    private JLabel windVal;
    private JLabel powerVal;
    private JLabel staffVal;
    private JLabel timeBadge;
    private JLabel insuranceBadge;
    private JLabel contractStatusLbl;

    private JLabel selectedPlotTitle;
    private JLabel selectedPlotSub;

    private RobloxButton buildBasicBtn;
    private RobloxButton buildAdvBtn;
    private RobloxButton buildIndBtn;
    private RobloxButton repairBtn;
    private RobloxButton upgradeBtn;
    private RobloxButton hireEngineerBtn;
    private RobloxButton insuranceToggleBtn;
    private RobloxButton saveBtn;
    private RobloxButton resetBtn;
    private JTextArea eventLog;

    private final DecimalFormat dfMoney = new DecimalFormat("#,##0.00");
    private final DecimalFormat dfShort = new DecimalFormat("#,##0.0");

    public WindTycoon() {
        super("⚡ Wind Turbine Tycoon [SAVES & NEON]");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1240, 870);
        setMinimumSize(new Dimension(1060, 740));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Автосохранение при закрытии окна
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

        for (int i = 0; i < 70; i++) {
            stars.add(new Point(random.nextInt(1200), random.nextInt(400)));
        }

        clouds.add(new Cloud(50, 50, 0.4, 1.2));
        clouds.add(new Cloud(350, 120, 0.25, 0.9));
        clouds.add(new Cloud(700, 70, 0.35, 1.1));
        clouds.add(new Cloud(950, 140, 0.2, 0.8));

        for (int i = 0; i < 35; i++) {
            windParticles.add(new Point(random.nextInt(850), random.nextInt(650)));
        }

        initComponents();

        // Загрузка сохранения
        loadGame();

        setupTimers();
        log("🎮 ДОБРО ПОЖАЛОВАТЬ В WIND TYCOON!");
        log("💾 Автосохранение активно каждые 20 сек и при выходе.");
    }

    private void initComponents() {
        // === ВЕРХНЯЯ ROBLOX-ПАНЕЛЬ (HUD) ===
        JPanel topHud = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        topHud.setBackground(new Color(18, 22, 30));
        topHud.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel p1 = createHudPill("💰 Баланс", balanceVal = new JLabel("$1,500.00"), new Color(46, 204, 113));
        JPanel p2 = createHudPill("💨 Ветер", windVal = new JLabel("8.5 м/с • Бриз 🍃"), new Color(0, 210, 211));
        JPanel p3 = createHudPill("⚡ Сеть", powerVal = new JLabel("0.0 кВт"), new Color(254, 202, 87));
        JPanel p4 = createHudPill("👷 Техники", staffVal = new JLabel("0 инж."), new Color(165, 94, 234));
        JPanel p5 = createHudPill("🕒 Время", timeBadge = new JLabel("☀️ День"), new Color(255, 234, 167));
        JPanel p6 = createHudPill("🛡️ Страховка", insuranceBadge = new JLabel("Нет"), new Color(231, 76, 60));

        topHud.add(p1);
        topHud.add(p2);
        topHud.add(p3);
        topHud.add(p4);
        topHud.add(p5);
        topHud.add(p6);
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

        // === ПРАВАЯ ПАНЕЛЬ ДЕЙСТВИЙ (ROBLOX STYLE) ===
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(350, 600));
        sidePanel.setBackground(new Color(23, 27, 38));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // Карточка контракта города
        JPanel contractCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 42, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(0, 180, 216));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        contractCard.setLayout(new BoxLayout(contractCard, BoxLayout.Y_AXIS));
        contractCard.setOpaque(false);
        contractCard.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        contractCard.setMaximumSize(new Dimension(320, 62));
        contractCard.setPreferredSize(new Dimension(320, 62));

        JLabel contractHeader = new JLabel("🏙️ КОНТРАКТ ГОРОДА");
        contractHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        contractHeader.setForeground(new Color(0, 210, 211));

        contractStatusLbl = new JLabel("Ожидание заказа мэрии...");
        contractStatusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        contractStatusLbl.setForeground(Color.WHITE);

        contractCard.add(contractHeader);
        contractCard.add(Box.createRigidArea(new Dimension(0, 2)));
        contractCard.add(contractStatusLbl);
        sidePanel.add(contractCard);

        sidePanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Карточка выбранного слота
        JPanel slotCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(32, 38, 54));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(55, 66, 88));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        slotCard.setLayout(new BoxLayout(slotCard, BoxLayout.Y_AXIS));
        slotCard.setOpaque(false);
        slotCard.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        slotCard.setMaximumSize(new Dimension(320, 56));
        slotCard.setPreferredSize(new Dimension(320, 56));

        selectedPlotTitle = new JLabel("Участок не выбран");
        selectedPlotTitle.setForeground(Color.WHITE);
        selectedPlotTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));

        selectedPlotSub = new JLabel("Кликните по участку на поле");
        selectedPlotSub.setForeground(new Color(160, 180, 205));
        selectedPlotSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        slotCard.add(selectedPlotTitle);
        slotCard.add(Box.createRigidArea(new Dimension(0, 2)));
        slotCard.add(selectedPlotSub);
        sidePanel.add(slotCard);

        sidePanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Кнопки постройки
        buildBasicBtn = new RobloxButton("🔨 Малый ветряк ($1,000)", new Color(46, 204, 113), e -> buildTurbine(TurbineType.BASIC));
        buildAdvBtn = new RobloxButton("⚡ Средний ветряк ($3,200)", new Color(52, 152, 219), e -> buildTurbine(TurbineType.ADVANCED));
        buildIndBtn = new RobloxButton("🌟 Гигант ветропарка ($9,000)", new Color(155, 89, 182), e -> buildTurbine(TurbineType.INDUSTRIAL));

        sidePanel.add(buildBasicBtn);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(buildAdvBtn);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(buildIndBtn);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 7)));

        // Кнопки обслуживания
        repairBtn = new RobloxButton("🔧 Починить ($0.00)", new Color(230, 126, 34), e -> repairSelectedTurbine());
        upgradeBtn = new RobloxButton("⭐ Улучшить Ур.2 ($0.00)", new Color(241, 196, 15), e -> upgradeSelectedTurbine());

        sidePanel.add(repairBtn);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        sidePanel.add(upgradeBtn);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 7)));

        // Персонал и Страховка
        hireEngineerBtn = new RobloxButton("👷 Нанять инженера ($250)", new Color(26, 188, 156), e -> hireEngineer());
        sidePanel.add(hireEngineerBtn);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));

        insuranceToggleBtn = new RobloxButton("📜 Оформить страховку ($10/мес)", new Color(41, 128, 185), e -> toggleInsurance());
        sidePanel.add(insuranceToggleBtn);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 7)));

        // Кнопки Сохранения и Сброса
        JPanel savePanel = new JPanel(new GridLayout(1, 2, 6, 0));
        savePanel.setOpaque(false);
        savePanel.setMaximumSize(new Dimension(320, 32));

        saveBtn = new RobloxButton("💾 Сохранить", new Color(46, 204, 113), e -> saveGame(true));
        resetBtn = new RobloxButton("🔄 Сброс", new Color(192, 57, 43), e -> resetGame());

        savePanel.add(saveBtn);
        savePanel.add(resetBtn);
        sidePanel.add(savePanel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Журнал событий
        JLabel logLabel = new JLabel("📋 ЖУРНАЛ ТАЙКУНА");
        logLabel.setForeground(new Color(150, 170, 195));
        logLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sidePanel.add(logLabel);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 4)));

        eventLog = new JTextArea(5, 20);
        eventLog.setEditable(false);
        eventLog.setBackground(new Color(16, 20, 28));
        eventLog.setForeground(new Color(200, 220, 240));
        eventLog.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        eventLog.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JScrollPane logScroll = new JScrollPane(eventLog);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 50, 70)));
        sidePanel.add(logScroll);

        add(sidePanel, BorderLayout.EAST);
        updateControlButtons();
    }

    private JPanel createHudPill(String title, JLabel valueLabel, Color accentColor) {
        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 36, 48));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(48, 58, 76));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLbl.setForeground(accentColor);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        pill.add(titleLbl);
        pill.add(Box.createRigidArea(new Dimension(0, 2)));
        pill.add(valueLabel);
        return pill;
    }

    private static class RobloxButton extends JButton {
        private Color baseColor;

        public RobloxButton(String text, Color baseColor, java.awt.event.ActionListener al) {
            super(text);
            this.baseColor = baseColor;
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setMaximumSize(new Dimension(320, 32));
            setPreferredSize(new Dimension(320, 32));
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
                g2.setColor(new Color(36, 42, 54));
                g2.fillRoundRect(0, 0, w, h - 2, 8, 8);
                g2.setColor(new Color(50, 58, 74));
                g2.drawRoundRect(0, 0, w - 1, h - 3, 8, 8);
                setForeground(new Color(125, 138, 155));
            } else {
                setForeground(Color.WHITE);
                Color shadowColor = new Color(
                        Math.max(0, baseColor.getRed() - 60),
                        Math.max(0, baseColor.getGreen() - 60),
                        Math.max(0, baseColor.getBlue() - 60)
                );
                g2.setColor(shadowColor);
                g2.fillRoundRect(0, yOffset + 2, w, drawH, 8, 8);

                Color faceColor = baseColor;
                if (rollover && !pressed) {
                    faceColor = new Color(
                            Math.min(255, baseColor.getRed() + 25),
                            Math.min(255, baseColor.getGreen() + 25),
                            Math.min(255, baseColor.getBlue() + 25)
                    );
                }
                g2.setColor(faceColor);
                g2.fillRoundRect(0, yOffset, w, drawH, 8, 8);

                g2.setColor(new Color(255, 255, 255, 45));
                g2.fillRoundRect(2, yOffset + 2, w - 4, drawH / 2 - 2, 6, 6);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void log(String msg) {
        eventLog.append(msg + "\n");
        eventLog.setCaretPosition(eventLog.getDocument().getLength());
    }

    // === СИСТЕМА СОХРАНЕНИЙ (SAVE / LOAD) ===
    private void saveGame(boolean notifyUser) {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(SAVE_FILE), StandardCharsets.UTF_8))) {
            pw.println("balance=" + balance);
            pw.println("engineers=" + engineersCount);
            pw.println("isInsured=" + isInsured);
            pw.println("totalEnergy=" + totalEnergyGenerated);
            pw.println("gameDays=" + gameDays);
            pw.println("gameMonths=" + gameMonths);

            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    Plot p = plots[c][r];
                    if (p.hasTurbine()) {
                        pw.println("plot=" + c + "," + r + "," + p.turbine.name() + "," + p.level + "," + p.durability);
                    }
                }
            }

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
        if (!f.exists()) {
            return; // Первый запуск
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
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
                        }
                    }
                }
            }
            log("💾 Сохранение успешно загружено! Баланс: $" + dfMoney.format(balance));
            updateControlButtons();
            updateUI();
        } catch (Exception ex) {
            log("⚠️ Ошибка при чтении сохранения: " + ex.getMessage());
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
            updateUI();
            canvas.repaint();
        }
    }

    // === ТАЙМЕРЫ И ИГРОВОЙ ЦИКЛ ===
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

        // Автосохранение каждые 20 секунд
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
        for (Cloud cl : clouds) {
            cl.x += cl.speed * (windSpeed / 6.0);
            if (cl.x > w + 120) {
                cl.x = -150;
            }
        }

        int h = canvas.getHeight();
        if (w > 0 && h > 0) {
            for (Point pt : windParticles) {
                pt.x += Math.max(2, (int)(windSpeed * 0.9));
                if (pt.x > w) {
                    pt.x = -random.nextInt(60);
                    pt.y = random.nextInt(h);
                }
            }
        }

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
            if (ft.alpha <= 0.0f) {
                itText.remove();
            }
        }

        Iterator<Sparkle> itSp = sparkles.iterator();
        while (itSp.hasNext()) {
            Sparkle sp = itSp.next();
            sp.x += sp.vx;
            sp.y += sp.vy;
            sp.vy += 0.08;
            sp.alpha -= 0.025f;
            if (sp.alpha <= 0.0f) {
                itSp.remove();
            }
        }
    }

    private void updateComboMultiplier() {
        if (comboClicks >= 30) {
            comboMultiplier = 5.0;
        } else if (comboClicks >= 15) {
            comboMultiplier = 3.0;
        } else if (comboClicks >= 5) {
            comboMultiplier = 2.0;
        } else {
            comboMultiplier = 1.0;
        }
    }

    private void gameLogicTick() {
        gameDays++;
        if (gameDays >= 30) {
            gameDays = 1;
            gameMonths++;
            processMonthlyInsurance();
        }

        updateCityContractLogic();

        double currentTotalKw = 0.0;
        double currentTotalEarnings = 0.0;

        double windEfficiency = 0.35;
        if (windSpeed >= 3.0) {
            windEfficiency = (windSpeed <= 12.0) ? (0.35 + ((windSpeed - 3.0) / 9.0) * 0.65) : 1.0;
        }

        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.75) / ROWS;

        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Plot p = plots[c][r];
                if (p.hasTurbine() && p.durability > 0) {
                    double healthFactor = (p.durability < 35.0) ? (p.durability / 35.0) : 1.0;
                    double levelBonus = 1.0 + (p.level - 1) * 0.35;

                    double kw = p.turbine.maxPowerKw * windEfficiency * healthFactor * levelBonus * comboMultiplier;
                    double turbineIncome = p.turbine.baseIncomePerSec * windEfficiency * healthFactor * levelBonus * comboMultiplier;

                    if (turbineIncome < 1.0) turbineIncome = 1.0;

                    currentTotalKw += kw;
                    currentTotalEarnings += turbineIncome;

                    double stormWear = (windSpeed > 15.0) ? 1.8 : 1.0;
                    p.durability -= (p.turbine.wearRate / p.level) * stormWear;
                    if (p.durability <= 0.0) {
                        p.durability = 0.0;
                        log("⚠️ Внимание! Турбина на участке [" + (c + 1) + "," + (r + 1) + "] сломалась!");
                    }

                    if (cellW > 0) {
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
                    if (p.hasTurbine() && p.durability < 98.0 && pool > 0) {
                        double need = 100.0 - p.durability;
                        double fix = Math.min(need, pool);
                        p.durability += fix;
                        pool -= fix;
                    }
                }
            }
        }

        double salaries = engineersCount * engineerSalaryPerSec;
        balance -= salaries;

        balance += currentTotalEarnings;
        totalEnergyGenerated += (currentTotalKw / 3600.0);

        currentPowerOutput = currentTotalKw;
        incomePerSecond = currentTotalEarnings - salaries;

        updateUI();
    }

    private void updateCityContractLogic() {
        if (activeContract == null) {
            contractCooldownTimer--;
            if (contractCooldownTimer <= 0) {
                activeContract = new CityContract();
                int type = random.nextInt(3);
                if (type == 0) {
                    activeContract.title = "Мэрия: Зарядка электротранспорта";
                    activeContract.requiredKw = 60.0;
                    activeContract.reward = 800.0;
                    activeContract.durationSeconds = 15;
                } else if (type == 1) {
                    activeContract.title = "Городской ночной фестиваль света";
                    activeContract.requiredKw = 120.0;
                    activeContract.reward = 1600.0;
                    activeContract.durationSeconds = 18;
                } else {
                    activeContract.title = "Мегаполис: Дефицит энергии на ТЭЦ";
                    activeContract.requiredKw = 250.0;
                    activeContract.reward = 3500.0;
                    activeContract.durationSeconds = 20;
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
                balance += activeContract.reward;
                SoundSystem.play("quest");
                log("🎉 КОНТРАКТ ВЫПОЛНЕН: " + activeContract.title + "! Получено +$" + dfMoney.format(activeContract.reward));
                floatingTexts.add(new FloatingText(canvas.getWidth() / 2, canvas.getHeight() / 2, "+$" + (int)activeContract.reward + " КОНТРАКТ 🏙️", new Color(0, 210, 211)));
                spawnSparkles(canvas.getWidth() / 2, canvas.getHeight() / 2, new Color(0, 210, 211));
                activeContract = null;
                contractCooldownTimer = 18;
            } else if (activeContract.remainingSeconds <= 0) {
                log("❌ Контракт города провален: не хватило мощности ветропарка.");
                activeContract = null;
                contractCooldownTimer = 12;
            }
        }
    }

    private void processMonthlyInsurance() {
        if (!isInsured) return;
        if (balance >= insuranceMonthlyCost) {
            balance -= insuranceMonthlyCost;
            log("💳 Оплата страховки за месяц " + gameMonths + ": -$10.00. Ферма защищена!");
        } else {
            isInsured = false;
            log("❌ СТРАХОВКА АННУЛИРОВАНА: Недостаточно средств для оплаты $10/мес!");
            updateControlButtons();
        }
    }

    private void updateWeatherEvent() {
        int ev = random.nextInt(100);
        if (ev < 18) {
            spawnTornadoEvent();
        } else if (ev < 45) {
            targetWindSpeed = 8.0 + random.nextDouble() * 4.0;
            weatherCondition = "Бриз 🍃";
        } else if (ev < 70) {
            targetWindSpeed = 13.5 + random.nextDouble() * 4.0;
            weatherCondition = "Сильный ветер 💨";
            log("💨 Сильный ветер! Максимальная выработка.");
        } else if (ev < 85) {
            targetWindSpeed = 3.0 + random.nextDouble() * 2.5;
            weatherCondition = "Штиль ☁️";
            log("☁️ Наступил штиль, выработка снижена.");
        } else {
            targetWindSpeed = 19.5 + random.nextDouble() * 4.0;
            weatherCondition = "Гроза ⚡";
            log("⚡ Грозовой ветер! Высокий доход, но следите за износом.");
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
        log("🚨 ВНИМАНИЕ: Замечен ТОРНАДО в регионе! Шанс удара по ферме: 10%!");

        Timer impactTimer = new Timer(4200, e -> {
            ((Timer)e.getSource()).stop();
            checkTornadoImpact();
        });
        impactTimer.setRepeats(false);
        impactTimer.start();
    }

    private void checkTornadoImpact() {
        if (!tornadoActive) return;

        boolean directHit = (random.nextInt(100) < 10);

        if (directHit) {
            lightningFlash = true;
            SoundSystem.play("siren");
            log("💥 КАТАСТРОФА! Торнадо ударил прямо по ветропарку!");

            int brokenCount = 0;
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    Plot p = plots[c][r];
                    if (p.hasTurbine() && p.durability > 0) {
                        p.durability = 0.0;
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
                } else {
                    log("❌ У вас НЕТ страховки! Ремонт " + brokenCount + " вышек за свой счет!");
                }
            }

            Timer flashReset = new Timer(400, e -> {
                lightningFlash = false;
                ((Timer)e.getSource()).stop();
            });
            flashReset.setRepeats(false);
            flashReset.start();

        } else {
            log("😅 Торнадо прошел мимо ветропарка! Ферма в безопасности.");
        }
    }

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

            if (clicked.hasTurbine() && clicked.durability > 0) {
                clicked.bladeAngle = (clicked.bladeAngle + 50.0) % 360.0;
                comboClicks += 2;
                comboDecayTimer = 35;
                updateComboMultiplier();

                double clickCash = 2.0 * comboMultiplier;
                balance += clickCash;
                SoundSystem.play("click");

                spawnSparkles(mx, my, clicked.turbine.neonColor);
                floatingTexts.add(new FloatingText(mx, my - 15, "+$" + dfMoney.format(clickCash) + (comboMultiplier > 1 ? " x" + (int)comboMultiplier : ""), new Color(254, 202, 87)));
            }

            updateControlButtons();
            canvas.repaint();
        }
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

    private void spawnSparkles(int x, int y, Color c) {
        for (int i = 0; i < 22; i++) {
            sparkles.add(new Sparkle(x, y, c));
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

        log("🎉 Построен " + type.name + " на слоте [" + (selectedPlot.col + 1) + "," + (selectedPlot.row + 1) + "]!");
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

        log("🔧 Турбина на [" + (selectedPlot.col + 1) + "," + (selectedPlot.row + 1) + "] отремонтирована!");
        updateControlButtons();
        updateUI();
    }

    private void upgradeSelectedTurbine() {
        if (selectedPlot == null || !selectedPlot.hasTurbine()) return;
        double upCost = selectedPlot.turbine.cost * 0.6 * selectedPlot.level;

        if (balance < upCost) {
            JOptionPane.showMessageDialog(this, "Нужно $" + dfMoney.format(upCost) + " для улучшения!", "Магазин", JOptionPane.WARNING_MESSAGE);
            return;
        }

        balance -= upCost;
        selectedPlot.level++;
        SoundSystem.play("build");

        int cellW = canvas.getWidth() / COLS;
        int cellH = (int)(canvas.getHeight() * 0.72) / ROWS;
        spawnSparkles(selectedPlot.col * cellW + cellW / 2, (int)(canvas.getHeight() * 0.12) + selectedPlot.row * cellH + (int)(cellH * 0.5), selectedPlot.turbine.neonColor);

        log("⭐ Уровень турбины повышен до Lv." + selectedPlot.level + "!");
        updateControlButtons();
        updateUI();
    }

    private void hireEngineer() {
        if (balance < engineerHireCost) {
            JOptionPane.showMessageDialog(this, "Для найма инженера нужно $" + engineerHireCost, "Персонал", JOptionPane.WARNING_MESSAGE);
            return;
        }
        balance -= engineerHireCost;
        engineersCount++;
        SoundSystem.play("coin");
        log("👷 Нанят сервисный инженер ($250)! Авторемонт ускорен.");
        updateUI();
    }

    private void updateControlButtons() {
        if (isInsured) {
            insuranceToggleBtn.setText("🛡️ Отказаться от страховки");
            insuranceToggleBtn.setBaseColor(new Color(39, 174, 96));
            insuranceBadge.setText("АКТИВНА");
            insuranceBadge.setForeground(new Color(46, 204, 113));
        } else {
            insuranceToggleBtn.setText("📜 Оформить страховку ($10/мес)");
            insuranceToggleBtn.setBaseColor(new Color(41, 128, 185));
            insuranceBadge.setText("Нет полиса");
            insuranceBadge.setForeground(new Color(231, 76, 60));
        }

        if (selectedPlot == null) {
            selectedPlotTitle.setText("Участок не выбран");
            selectedPlotSub.setText("Кликните по участку на поле");
            buildBasicBtn.setEnabled(false);
            buildAdvBtn.setEnabled(false);
            buildIndBtn.setEnabled(false);
            repairBtn.setEnabled(false);
            upgradeBtn.setEnabled(false);
            repairBtn.setText("🔧 Починить ($0.00)");
            upgradeBtn.setText("⭐ Улучшить ($0.00)");
            return;
        }

        String coords = "[" + (selectedPlot.col + 1) + ", " + (selectedPlot.row + 1) + "]";
        if (!selectedPlot.hasTurbine()) {
            selectedPlotTitle.setText("Участок " + coords + " (Свободен)");
            selectedPlotSub.setText("Выберите ветряк для постройки:");
            buildBasicBtn.setEnabled(true);
            buildAdvBtn.setEnabled(true);
            buildIndBtn.setEnabled(true);
            repairBtn.setEnabled(false);
            upgradeBtn.setEnabled(false);
            repairBtn.setText("🔧 Починить");
            upgradeBtn.setText("⭐ Улучшить");
        } else {
            double repCost = (100.0 - selectedPlot.durability) * 1.5;
            double upCost = selectedPlot.turbine.cost * 0.6 * selectedPlot.level;

            selectedPlotTitle.setText(selectedPlot.turbine.name + " " + coords);
            selectedPlotSub.setText("⭐ Lv." + selectedPlot.level + "  |  Состояние: " + dfShort.format(selectedPlot.durability) + "%");

            buildBasicBtn.setEnabled(false);
            buildAdvBtn.setEnabled(false);
            buildIndBtn.setEnabled(false);

            boolean canRepair = selectedPlot.durability < 99.5;
            repairBtn.setEnabled(canRepair);
            repairBtn.setText("🔧 Починить ($" + dfMoney.format(repCost) + ")");

            upgradeBtn.setEnabled(true);
            upgradeBtn.setText("⭐ Улучшить Lv." + (selectedPlot.level + 1) + " ($" + dfMoney.format(upCost) + ")");
        }
    }

    private void updateUI() {
        balanceVal.setText("$" + dfMoney.format(balance));
        windVal.setText(dfShort.format(windSpeed) + " м/с • " + weatherCondition);

        String sign = incomePerSecond >= 0 ? "+$" : "-$";
        powerVal.setText(dfShort.format(currentPowerOutput) + " кВт (" + sign + dfMoney.format(Math.abs(incomePerSecond)) + "/с)");
        staffVal.setText(engineersCount + " инж. (-$" + (int)(engineersCount * engineerSalaryPerSec) + "/с)");

        boolean isNight = (timeOfDay >= 0.45 && timeOfDay <= 0.88);
        if (isNight) {
            timeBadge.setText("🌙 Ночь");
            timeBadge.setForeground(new Color(130, 200, 255));
        } else if (timeOfDay > 0.38 && timeOfDay < 0.45) {
            timeBadge.setText("🌇 Закат");
            timeBadge.setForeground(new Color(255, 159, 67));
        } else {
            timeBadge.setText("☀️ День");
            timeBadge.setForeground(new Color(255, 234, 167));
        }

        if (activeContract != null) {
            contractStatusLbl.setText("<html><b>" + activeContract.title + "</b><br>" +
                    "Требуется: <b>" + (int)activeContract.requiredKw + " кВт</b> &nbsp;|&nbsp; " +
                    "Осталось: <font color='#FECA57'>" + activeContract.remainingSeconds + "с</font><br>" +
                    "Награда: <font color='#2ECC71'>+$" + (int)activeContract.reward + "</font></html>");
        } else {
            contractStatusLbl.setText("<html>Ожидание нового заказа мэрии... (" + contractCooldownTimer + "с)</html>");
        }

        updateControlButtons();
    }

    // === КАНВАС С НЕОНОМ, ГОРОДОМ И ЦИКЛОМ ДНЯ/НОЧИ ===
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

            int groundY = (int)(h * 0.12);
            int farmH = (int)(h * 0.72) - groundY;

            Color grassTop = isNight ? new Color(24, 85, 48) : new Color(46, 204, 113);
            Color grassBot = isNight ? new Color(18, 65, 38) : new Color(39, 174, 96);

            GradientPaint groundGrad = new GradientPaint(0, groundY, grassTop, 0, groundY + farmH, grassBot);
            g2.setPaint(groundGrad);
            g2.fillRect(0, groundY, w, farmH);

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

            if (tornadoActive) {
                drawTornado(g2, (int)tornadoX, h / 2);
            }

            if (lightningFlash) {
                g2.setColor(new Color(255, 255, 255, 185));
                g2.fillRect(0, 0, w, h);
            }

            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, isNight ? 50 : 85));
            int windLineLen = Math.max(18, (int)(windSpeed * 4.2));
            for (Point pt : windParticles) {
                g2.drawLine(pt.x, pt.y, pt.x + windLineLen, pt.y);
            }

            for (Sparkle sp : sparkles) {
                g2.setColor(new Color(sp.color.getRed(), sp.color.getGreen(), sp.color.getBlue(), (int)(sp.alpha * 255)));
                g2.fillOval((int)sp.x - sp.size / 2, (int)sp.y - sp.size / 2, sp.size, sp.size);
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
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

            if (tornadoActive) {
                g2.setColor(new Color(231, 76, 60, 220));
                g2.fillRoundRect(w / 2 - 210, 15, 420, 36, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(w / 2 - 210, 15, 420, 36, 12, 12);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String alert = "🚨 ТОРНАДО В РЕГИОНЕ! ШАНС УДАРА: 10%";
                int aw = g2.getFontMetrics().stringWidth(alert);
                g2.drawString(alert, w / 2 - aw / 2, 38);
            }
        }

        private void drawComboBadge(Graphics2D g2, int cx, int cy) {
            String txt = "⚡ ТУРБО БУСТ x" + (int)comboMultiplier + "!";
            if (comboMultiplier >= 5.0) txt = "🔥 МЕГА ОВЕРДРАЙВ x5! 🔥";

            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
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

            int numBuildings = 14;
            int bWidth = w / numBuildings;
            Random cityRnd = new Random(42);

            for (int i = 0; i < numBuildings; i++) {
                int bx = x + i * bWidth;
                int bh = 30 + cityRnd.nextInt(h - 35);
                int by = y + h - bh;

                Color bColor = isNight ? new Color(22 + (i % 3) * 6, 30 + (i % 3) * 6, 48 + (i % 3) * 8) :
                                         new Color(60 + (i % 3) * 10, 75 + (i % 3) * 10, 95 + (i % 3) * 10);
                g2.setColor(bColor);
                g2.fillRect(bx + 2, by, bWidth - 4, bh);

                int floors = bh / 10;
                for (int fl = 0; fl < floors; fl++) {
                    int winY = by + 6 + fl * 9;
                    for (int winX = bx + 6; winX < bx + bWidth - 8; winX += 7) {
                        if (cityRnd.nextBoolean()) {
                            if (isNight) {
                                g2.setColor(cityRnd.nextInt(3) == 0 ? new Color(0, 240, 255) : new Color(255, 225, 100));
                            } else {
                                g2.setColor(new Color(170, 195, 220, 140));
                            }
                            g2.fillRect(winX, winY, 4, 5);
                        }
                    }
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

            g2.setColor(isNight ? new Color(12, 45, 25, 120) : new Color(25, 110, 60, 110));
            g2.fillRoundRect(x + 2, y + depth + 4, w - 4, h - depth, 20, 20);

            g2.setColor(isNight ? new Color(15, 60, 32) : new Color(30, 130, 68));
            g2.fillRoundRect(x, y + depth, w, h - depth, 18, 18);

            if (isSelected) {
                g2.setColor(isNight ? new Color(50, 170, 100) : new Color(110, 240, 155));
            } else {
                g2.setColor(isNight ? new Color(32, 120, 65) : new Color(88, 214, 141));
            }
            g2.fillRoundRect(x, y, w, h - depth, 18, 18);

            if (isSelected) {
                g2.setColor(new Color(254, 202, 87));
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawRoundRect(x, y, w, h - depth, 18, 18);
            } else {
                g2.setColor(isNight ? new Color(45, 150, 80) : new Color(125, 230, 165));
                g2.setStroke(new BasicStroke(1.5f));
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

                g2.setColor(new Color(39, 174, 96));
                g2.fillRoundRect(padX, padY + 4, padW, padH, 14, 14);

                g2.setColor(new Color(46, 204, 113));
                g2.fillRoundRect(padX, padY, padW, padH, 14, 14);

                g2.setColor(new Color(255, 255, 255, (int)(180 * pulse)));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(padX, padY, padW, padH, 14, 14);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                String btnText = "➕ ПОСТРОИТЬ";
                int strW = g2.getFontMetrics().stringWidth(btnText);
                g2.drawString(btnText, cx - strW / 2, padY + 17);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(new Color(235, 255, 240));
                String costText = "от $1,000";
                int strW2 = g2.getFontMetrics().stringWidth(costText);
                g2.drawString(costText, cx - strW2 / 2, padY + 31);
            }
        }

        private void drawTurbine3D(Graphics2D g2, int cx, int baseCy, Plot plot, boolean isNight) {
            int mastH = 86;
            int hubCy = baseCy - mastH;

            // Фундамент
            g2.setColor(isNight ? new Color(70, 80, 95) : new Color(130, 140, 150));
            g2.fillOval(cx - 28, baseCy - 8, 56, 16);
            g2.setColor(isNight ? new Color(110, 120, 135) : new Color(190, 200, 210));
            g2.fillOval(cx - 26, baseCy - 11, 52, 14);

            // Мачта
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

            // Гондола
            g2.setColor(isNight ? new Color(150, 165, 185) : new Color(235, 242, 250));
            g2.fillRoundRect(cx - 11, hubCy - 7, 22, 14, 6, 6);
            g2.setColor(plot.turbine.themeColor);
            g2.fillRect(cx - 11, hubCy - 2, 22, 4);

            // Маячок безопасности
            boolean beaconOn = (System.currentTimeMillis() / 450) % 2 == 0;
            if (beaconOn) {
                g2.setColor(new Color(255, 75, 75, isNight ? 220 : 140));
                g2.fillOval(cx - 6, hubCy - 15, 12, 12);
                g2.setColor(new Color(255, 40, 40));
                g2.fillOval(cx - 3, hubCy - 12, 6, 6);
            }

            // ЛОПАСТИ С НЕОНОМ
            AffineTransform oldTx = g2.getTransform();
            g2.translate(cx, hubCy);
            g2.rotate(Math.toRadians(plot.bladeAngle));

            double bladeLen = (plot.turbine == TurbineType.BASIC) ? 50.0 :
                              (plot.turbine == TurbineType.ADVANCED) ? 62.0 : 74.0;

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

                    g2.setColor(new Color(235, 60, 60));
                    g2.fillOval(-3, -(int)bladeLen, 6, 10);
                }
            }

            // Ступица
            g2.setColor(isNight ? new Color(160, 180, 205) : new Color(225, 235, 245));
            g2.fillOval(-6, -6, 12, 12);
            g2.setColor(new Color(100, 115, 135));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(-6, -6, 12, 12);

            g2.setTransform(oldTx);

            // Полоска здоровья
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

            // Бейджик уровня
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String lvlStr = "⭐ Lv." + plot.level;
            int lw = g2.getFontMetrics().stringWidth(lvlStr);
            g2.setColor(new Color(25, 35, 45, 180));
            g2.fillRoundRect(cx - lw / 2 - 4, hubCy - 28, lw + 8, 14, 8, 8);
            g2.setColor(new Color(254, 202, 87));
            g2.drawString(lvlStr, cx - lw / 2, hubCy - 17);

            // Статус поломки
            if (plot.durability <= 0.0) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(new Color(231, 76, 60));
                String broke = "⚠️ СЛОМАН!";
                int bw = g2.getFontMetrics().stringWidth(broke);
                g2.drawString(broke, cx - bw / 2, hubCy - 34);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WindTycoon game = new WindTycoon();
            game.setVisible(true);
        });
    }
}
