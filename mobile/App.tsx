import React, { useState, useEffect, useRef } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  SafeAreaView,
  Alert,
} from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { HeaderHud } from './src/components/HeaderHud';
import { GameCanvas } from './src/components/GameCanvas';
import { BuildTab } from './src/components/Tabs/BuildTab';
import { MarketTab } from './src/components/Tabs/MarketTab';
import { ResearchTab } from './src/components/Tabs/ResearchTab';
import { PrestigeTab } from './src/components/Tabs/PrestigeTab';
import {
  GameState,
  PlotData,
  Achievement,
  TURBINE_TYPES,
  TURBINE_ORDER,
  TurbineTypeId,
  getNextTier,
} from './src/types';
import { saveGameData, loadGameData, clearGameData } from './src/storage';
import { soundManager } from './src/soundSystem';

const INITIAL_ACHIEVEMENTS: Achievement[] = [
  {
    id: 'first_kw',
    title: '⚡ Первый киловатт',
    description: 'Произведено суммарно 100 кВт энергии',
    reward: 500,
    isUnlocked: false,
    isClaimed: false,
  },
  {
    id: 'rich_tycoon',
    title: '💰 Ветровой магнат',
    description: 'Накопите $50,000 на счету',
    reward: 5000,
    isUnlocked: false,
    isClaimed: false,
  },
  {
    id: 'green_district',
    title: '🌿 Зеленый квартал',
    description: 'Застройте 6 участков ветряками',
    reward: 3000,
    isUnlocked: false,
    isClaimed: false,
  },
  {
    id: 'quantum_leap',
    title: '🌀 Квантовый скачок',
    description: 'Постройте Квантовый VAWT на поле',
    reward: 25000,
    isUnlocked: false,
    isClaimed: false,
  },
  {
    id: 'city_hero',
    title: '🏙️ Спаситель города',
    description: 'Выполните 3 срочных контракта мэрии',
    reward: 10000,
    isUnlocked: false,
    isClaimed: false,
  },
  {
    id: 'lightning_master',
    title: '⚡ Громовержец',
    description: 'Отразите грозовой разряд громоотводом',
    reward: 7500,
    isUnlocked: false,
    isClaimed: false,
  },
  {
    id: 'first_rebirth',
    title: '👑 Первый Rebirth',
    description: 'Совершите свой первый Rebirth корпорации',
    reward: 20000,
    isUnlocked: false,
    isClaimed: false,
  },
  {
    id: 'robotic_future',
    title: '🤖 Роботизация',
    description: 'Купите 3 ремонтных дронов на базу',
    reward: 8000,
    isUnlocked: false,
    isClaimed: false,
  },
];

function createInitialPlots(): PlotData[][] {
  const plots: PlotData[][] = [];
  for (let c = 0; c < 3; c++) {
    plots[c] = [];
    for (let r = 0; r < 3; r++) {
      plots[c][r] = {
        col: c,
        row: r,
        turbine: null,
        level: 1,
        durability: 100.0,
        bladeAngle: (c * 40 + r * 60) % 360,
      };
    }
  }
  return plots;
}

export default function App() {
  const [activeTab, setActiveTab] = useState<'BUILD' | 'MARKET' | 'RESEARCH' | 'PRESTIGE'>('BUILD');

  const [gameState, setGameState] = useState<GameState>({
    balance: 1500.0,
    windSpeed: 8.5,
    weatherCondition: 'Бриз 🍃',
    totalEnergyGenerated: 0.0,
    gameDays: 1,
    gameMonths: 1,
    engineersCount: 0,
    isInsured: false,
    timeOfDay: 0.25,
    batteryCapacity: 0.0,
    batteryStored: 0.0,
    marketMultiplier: 1.0,
    sciencePoints: 0.0,
    techAeroLevel: 0,
    techCarbonLevel: 0,
    techLightningRod: false,
    techStormRadar: false,
    techDronesUnlocked: false,
    droneCount: 0,
    rebirthCount: 0,
    cityContractsCompleted: 0,
    activeContract: null,
    selectedPlotCoord: { col: 1, row: 1 },
    hapticsEnabled: true,
    musicEnabled: true,
    comboMultiplier: 1,
  });

  const [plots, setPlots] = useState<PlotData[][]>(createInitialPlots);
  const [achievements, setAchievements] = useState<Achievement[]>(INITIAL_ACHIEVEMENTS);

  const [currentPowerOutput, setCurrentPowerOutput] = useState(0.0);
  const [incomePerSec, setIncomePerSec] = useState(0.0);

  const contractCooldownTimer = useRef(15);
  const saveCounter = useRef(0);

  // Init audio and load save on start
  useEffect(() => {
    soundManager.initBgm();

    loadGameData().then((saved) => {
      if (saved) {
        if (saved.state) {
          setGameState((prev) => ({ ...prev, ...saved.state }));
        }
        if (saved.plots) {
          setPlots(saved.plots);
        }
        if (saved.achievements) {
          setAchievements(saved.achievements);
        }
      }
    });

    return () => {
      soundManager.unload();
    };
  }, []);

  // Main Simulation Loop (1 second tick)
  useEffect(() => {
    const timer = setInterval(() => {
      setGameState((prev) => {
        let {
          balance,
          windSpeed,
          weatherCondition,
          totalEnergyGenerated,
          timeOfDay,
          batteryCapacity,
          batteryStored,
          marketMultiplier,
          sciencePoints,
          techAeroLevel,
          techCarbonLevel,
          droneCount,
          rebirthCount,
          cityContractsCompleted,
          activeContract,
          engineersCount,
        } = prev;

        // 1. Time & Weather
        timeOfDay = (timeOfDay + 0.005) % 1.0;

        // Smooth wind variation
        const windDelta = (Math.random() - 0.48) * 0.8;
        windSpeed = Math.max(1.5, Math.min(24.0, windSpeed + windDelta));

        if (windSpeed < 4.0) {
          weatherCondition = 'Штиль ☁️';
        } else if (windSpeed > 16.0) {
          weatherCondition = 'Шторм 🌪️';
        } else if (Math.random() < 0.08) {
          weatherCondition = 'Гроза ⚡';
        } else {
          weatherCondition = 'Бриз 🍃';
        }

        // Market multiplier
        if (Math.random() < 0.12) {
          const isPeak = Math.random() < 0.25;
          marketMultiplier = isPeak ? 2.2 + Math.random() * 0.8 : 0.8 + Math.random() * 0.7;
        }

        // 2. Calculate Power Output
        let totalKw = 0;
        const rebirthBonus = 1.0 + rebirthCount * 0.5;
        const aeroBonus = 1.0 + techAeroLevel * 0.15;
        const wearReduction = 1.0 - techCarbonLevel * 0.25;

        setPlots((currentPlots) => {
          const updated = currentPlots.map((row) =>
            row.map((plot) => {
              if (!plot.turbine || plot.durability <= 0) return plot;

              const cfg = TURBINE_TYPES[plot.turbine];
              const kw = cfg.maxPowerKw * (windSpeed / 8.5) * aeroBonus * rebirthBonus * (plot.durability / 100);
              totalKw += kw;

              // Durability decay
              let wear = cfg.wearRate * wearReduction;
              if (engineersCount > 0) wear -= engineersCount * 0.03;
              if (droneCount > 0) wear -= droneCount * 0.05;

              const newDurability = Math.max(0, Math.min(100, plot.durability - wear));
              return { ...plot, durability: newDurability };
            })
          );
          return updated;
        });

        // 3. Economy and Income
        const baseRate = 0.18;
        const currentRate = baseRate * marketMultiplier;
        const secIncome = totalKw * currentRate * 0.1;
        balance += secIncome;
        totalEnergyGenerated += totalKw * 0.01;

        // Science SP generation
        sciencePoints += totalKw * 0.0008 * rebirthBonus;

        // 4. Battery storage management
        if (batteryCapacity > 0) {
          if (windSpeed > 10.0 && batteryStored < batteryCapacity) {
            batteryStored = Math.min(batteryCapacity, batteryStored + totalKw * 0.05);
          } else if (windSpeed < 4.0 && batteryStored > 0) {
            batteryStored = Math.max(0, batteryStored - 2.0);
          }
        }

        // 5. Contract processing
        if (activeContract) {
          activeContract.remainingSeconds -= 1;
          if (totalKw >= activeContract.requiredKw) {
            if (activeContract.remainingSeconds <= 0) {
              const contractCash = activeContract.reward * rebirthBonus;
              balance += contractCash;
              cityContractsCompleted += 1;
              activeContract = null;
              contractCooldownTimer.current = 25;
            }
          } else {
            if (activeContract.remainingSeconds <= 0) {
              activeContract = null;
              contractCooldownTimer.current = 15;
            }
          }
        } else {
          contractCooldownTimer.current -= 1;
          if (contractCooldownTimer.current <= 0) {
            const req = 100 + Math.floor(Math.random() * 400);
            activeContract = {
              title: 'Снабжение Эко-города',
              requiredKw: req,
              remainingSeconds: 40,
              reward: req * 35,
            };
          }
        }

        setCurrentPowerOutput(totalKw);
        setIncomePerSec(secIncome);

        // Auto-save every 15 seconds
        saveCounter.current += 1;
        if (saveCounter.current % 15 === 0) {
          saveGameData(prev, plots, achievements);
        }

        return {
          ...prev,
          balance,
          windSpeed,
          weatherCondition,
          totalEnergyGenerated,
          timeOfDay,
          batteryCapacity,
          batteryStored,
          marketMultiplier,
          sciencePoints,
          activeContract,
          cityContractsCompleted,
        };
      });

      // Check achievements
      setAchievements((prevAchs) =>
        prevAchs.map((ach) => {
          if (ach.isUnlocked) return ach;
          let unlock = false;
          if (ach.id === 'first_kw' && gameState.totalEnergyGenerated >= 100) unlock = true;
          if (ach.id === 'rich_tycoon' && gameState.balance >= 50000) unlock = true;
          if (ach.id === 'green_district') {
            let count = 0;
            plots.forEach((col) => col.forEach((p) => { if (p.turbine) count++; }));
            if (count >= 6) unlock = true;
          }
          if (ach.id === 'quantum_leap') {
            let hasVawt = false;
            plots.forEach((col) => col.forEach((p) => { if (p.turbine === 'QUANTUM_VAWT') hasVawt = true; }));
            if (hasVawt) unlock = true;
          }
          if (ach.id === 'city_hero' && gameState.cityContractsCompleted >= 3) unlock = true;
          if (ach.id === 'first_rebirth' && gameState.rebirthCount >= 1) unlock = true;
          if (ach.id === 'robotic_future' && gameState.droneCount >= 3) unlock = true;

          return unlock ? { ...ach, isUnlocked: true } : ach;
        })
      );
    }, 1000);

    return () => clearInterval(timer);
  }, [plots, achievements, gameState.totalEnergyGenerated, gameState.balance, gameState.cityContractsCompleted, gameState.rebirthCount, gameState.droneCount]);

  // Click on plot handler (Touch / Crit)
  const handlePlotClick = (col: number, row: number, isCrit: boolean) => {
    const cash = (isCrit ? 10 : 2) * gameState.comboMultiplier * (1 + gameState.rebirthCount * 0.5);
    setGameState((prev) => ({
      ...prev,
      balance: prev.balance + cash,
      sciencePoints: prev.sciencePoints + (isCrit ? 1.0 : 0.05),
      selectedPlotCoord: { col, row },
    }));
  };

  const handleSelectPlot = (col: number, row: number) => {
    setGameState((prev) => ({
      ...prev,
      selectedPlotCoord: { col, row },
    }));
    setActiveTab('BUILD');
  };

  // Build / Evolution Logic
  const handleBuildOrUpgrade = (typeId: TurbineTypeId) => {
    const coord = gameState.selectedPlotCoord;
    if (!coord) return;

    const plot = plots[coord.col]?.[coord.row];
    if (!plot) return;

    const targetCfg = TURBINE_TYPES[typeId];

    if (!plot.turbine) {
      if (gameState.balance < targetCfg.cost) {
        Alert.alert('Недостаточно средств', `Для постройки ${targetCfg.name} нужно $${targetCfg.cost.toLocaleString()}`);
        return;
      }
      setGameState((prev) => ({ ...prev, balance: prev.balance - targetCfg.cost }));
      setPlots((prev) => {
        const copy = prev.map((c) => c.map((p) => ({ ...p })));
        copy[coord.col][coord.row].turbine = typeId;
        copy[coord.col][coord.row].level = 1;
        copy[coord.col][coord.row].durability = 100;
        return copy;
      });
    } else {
      const currentIdx = TURBINE_ORDER.indexOf(plot.turbine);
      const targetIdx = TURBINE_ORDER.indexOf(typeId);

      if (targetIdx > currentIdx) {
        const curCfg = TURBINE_TYPES[plot.turbine];
        const diff = targetCfg.cost - curCfg.cost;
        if (gameState.balance < diff) {
          Alert.alert('Недостаточно средств', `Для эволюции в ${targetCfg.name} требуется +$${diff.toLocaleString()}`);
          return;
        }
        setGameState((prev) => ({ ...prev, balance: prev.balance - diff }));
        setPlots((prev) => {
          const copy = prev.map((c) => c.map((p) => ({ ...p })));
          copy[coord.col][coord.row].turbine = typeId;
          copy[coord.col][coord.row].level = 1;
          copy[coord.col][coord.row].durability = 100;
          return copy;
        });
      }
    }
  };

  const handleEvolveSelected = () => {
    const coord = gameState.selectedPlotCoord;
    if (!coord) return;
    const plot = plots[coord.col]?.[coord.row];
    if (!plot || !plot.turbine) return;

    const nextTierId = getNextTier(plot.turbine);
    if (nextTierId) {
      handleBuildOrUpgrade(nextTierId);
    } else {
      // Overclock
      const cfg = TURBINE_TYPES[plot.turbine];
      const upCost = cfg.cost * 0.5 * plot.level;
      if (gameState.balance < upCost) {
        Alert.alert('Недостаточно средств', `Для форсирования уровня нужно $${upCost.toLocaleString()}`);
        return;
      }
      setGameState((prev) => ({ ...prev, balance: prev.balance - upCost }));
      setPlots((prev) => {
        const copy = prev.map((c) => c.map((p) => ({ ...p })));
        copy[coord.col][coord.row].level += 1;
        copy[coord.col][coord.row].durability = 100;
        return copy;
      });
    }
  };

  const handleRepairSelected = () => {
    const coord = gameState.selectedPlotCoord;
    if (!coord) return;
    const plot = plots[coord.col]?.[coord.row];
    if (!plot || !plot.turbine || plot.durability >= 99.5) return;

    const repCost = (100.0 - plot.durability) * 1.5;
    if (gameState.balance < repCost) {
      Alert.alert('Недостаточно средств', 'Не хватает денег на полный ремонт!');
      return;
    }
    setGameState((prev) => ({ ...prev, balance: prev.balance - repCost }));
    setPlots((prev) => {
      const copy = prev.map((c) => c.map((p) => ({ ...p })));
      copy[coord.col][coord.row].durability = 100;
      return copy;
    });
  };

  const handleDemolishSelected = () => {
    const coord = gameState.selectedPlotCoord;
    if (!coord) return;
    const plot = plots[coord.col]?.[coord.row];
    if (!plot || !plot.turbine) return;

    const cfg = TURBINE_TYPES[plot.turbine];
    const refund = cfg.cost * 0.5;

    Alert.alert(
      'Демонтаж ветряка',
      `Снести ${cfg.name}? Возврат средств: +$${refund.toLocaleString()}`,
      [
        { text: 'Отмена', style: 'cancel' },
        {
          text: 'Снести',
          style: 'destructive',
          onPress: () => {
            setGameState((prev) => ({ ...prev, balance: prev.balance + refund }));
            setPlots((prev) => {
              const copy = prev.map((c) => c.map((p) => ({ ...p })));
              copy[coord.col][coord.row].turbine = null;
              copy[coord.col][coord.row].level = 1;
              copy[coord.col][coord.row].durability = 100;
              return copy;
            });
          },
        },
      ]
    );
  };

  const handleHireEngineer = () => {
    if (gameState.balance < 250) {
      Alert.alert('Недостаточно средств', 'Для найма инженера нужно $250');
      return;
    }
    setGameState((prev) => ({
      ...prev,
      balance: prev.balance - 250,
      engineersCount: prev.engineersCount + 1,
    }));
  };

  const handleToggleInsurance = () => {
    setGameState((prev) => ({ ...prev, isInsured: !prev.isInsured }));
  };

  // Market handlers
  const handleUpgradeBattery = () => {
    const cost = gameState.batteryCapacity <= 0 ? 4000 : gameState.batteryCapacity < 5000 ? 14000 : 45000;
    const nextCap = gameState.batteryCapacity <= 0 ? 1000 : gameState.batteryCapacity < 5000 ? 5000 : 20000;

    if (gameState.balance < cost) {
      Alert.alert('Недостаточно средств', `Для расширения батареи нужно $${cost.toLocaleString()}`);
      return;
    }

    setGameState((prev) => ({
      ...prev,
      balance: prev.balance - cost,
      batteryCapacity: nextCap,
    }));
  };

  const handleDumpBattery = () => {
    const rate = 0.18 * gameState.marketMultiplier;
    const value = gameState.batteryStored * rate;
    setGameState((prev) => ({
      ...prev,
      balance: prev.balance + value,
      batteryStored: 0,
    }));
  };

  // Research handlers
  const handleResearchAero = () => {
    const cost = gameState.techAeroLevel === 0 ? 40 : gameState.techAeroLevel === 1 ? 120 : 300;
    if (gameState.sciencePoints < cost) {
      Alert.alert('Недостаточно науки', `Требуется ${cost} SP`);
      return;
    }
    setGameState((prev) => ({
      ...prev,
      sciencePoints: prev.sciencePoints - cost,
      techAeroLevel: prev.techAeroLevel + 1,
    }));
  };

  const handleResearchCarbon = () => {
    const cost = gameState.techCarbonLevel === 0 ? 50 : gameState.techCarbonLevel === 1 ? 150 : 400;
    if (gameState.sciencePoints < cost) {
      Alert.alert('Недостаточно науки', `Требуется ${cost} SP`);
      return;
    }
    setGameState((prev) => ({
      ...prev,
      sciencePoints: prev.sciencePoints - cost,
      techCarbonLevel: prev.techCarbonLevel + 1,
    }));
  };

  const handleResearchLightning = () => {
    if (gameState.sciencePoints < 100) {
      Alert.alert('Недостаточно науки', 'Требуется 100 SP');
      return;
    }
    setGameState((prev) => ({
      ...prev,
      sciencePoints: prev.sciencePoints - 100,
      techLightningRod: true,
    }));
  };

  const handleResearchRadar = () => {
    if (gameState.sciencePoints < 150) {
      Alert.alert('Недостаточно науки', 'Требуется 150 SP');
      return;
    }
    setGameState((prev) => ({
      ...prev,
      sciencePoints: prev.sciencePoints - 150,
      techStormRadar: true,
    }));
  };

  const handleResearchDrones = () => {
    if (gameState.sciencePoints < 220) {
      Alert.alert('Недостаточно науки', 'Требуется 220 SP');
      return;
    }
    setGameState((prev) => ({
      ...prev,
      sciencePoints: prev.sciencePoints - 220,
      techDronesUnlocked: true,
    }));
  };

  const handleBuyDrone = () => {
    if (gameState.balance < 5000) {
      Alert.alert('Недостаточно средств', 'Для покупки дрона нужно $5,000');
      return;
    }
    setGameState((prev) => ({
      ...prev,
      balance: prev.balance - 5000,
      droneCount: prev.droneCount + 1,
    }));
  };

  // Prestige handlers
  const handleTriggerRebirth = () => {
    setGameState((prev) => ({
      ...prev,
      balance: 1500.0,
      rebirthCount: prev.rebirthCount + 1,
      batteryStored: 0,
    }));
    setPlots(createInitialPlots());
    Alert.alert('🎉 Успешный IPO / Rebirth!', 'Вы получили постоянный множитель +50% ко всем доходам навсегда!');
  };

  const handleClaimAchievement = (id: string) => {
    const ach = achievements.find((a) => a.id === id);
    if (!ach || !ach.isUnlocked || ach.isClaimed) return;

    setGameState((prev) => ({ ...prev, balance: prev.balance + ach.reward }));
    setAchievements((prev) =>
      prev.map((a) => (a.id === id ? { ...a, isClaimed: true } : a))
    );
  };

  const handleSaveGame = async () => {
    const success = await saveGameData(gameState, plots, achievements);
    if (success) {
      Alert.alert('Сохранено', 'Игровой прогресс успешно сохранён на устройстве!');
    }
  };

  const handleResetGame = async () => {
    await clearGameData();
    setGameState({
      balance: 1500.0,
      windSpeed: 8.5,
      weatherCondition: 'Бриз 🍃',
      totalEnergyGenerated: 0.0,
      gameDays: 1,
      gameMonths: 1,
      engineersCount: 0,
      isInsured: false,
      timeOfDay: 0.25,
      batteryCapacity: 0.0,
      batteryStored: 0.0,
      marketMultiplier: 1.0,
      sciencePoints: 0.0,
      techAeroLevel: 0,
      techCarbonLevel: 0,
      techLightningRod: false,
      techStormRadar: false,
      techDronesUnlocked: false,
      droneCount: 0,
      rebirthCount: 0,
      cityContractsCompleted: 0,
      activeContract: null,
      selectedPlotCoord: { col: 1, row: 1 },
      hapticsEnabled: true,
      musicEnabled: true,
      comboMultiplier: 1,
    });
    setPlots(createInitialPlots());
    setAchievements(INITIAL_ACHIEVEMENTS);
  };

  const handleToggleMusic = async () => {
    const active = await soundManager.toggleBgm();
    setGameState((prev) => ({ ...prev, musicEnabled: active }));
  };

  const handleToggleHaptics = () => {
    setGameState((prev) => ({ ...prev, hapticsEnabled: !prev.hapticsEnabled }));
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar style="light" />

      {/* Верхний статус-бар */}
      <HeaderHud
        gameState={gameState}
        currentPowerOutput={currentPowerOutput}
        incomePerSec={incomePerSec}
        onToggleMusic={handleToggleMusic}
        onToggleHaptics={handleToggleHaptics}
      />

      {/* 2D Интерактивное поле игры */}
      <GameCanvas
        gameState={gameState}
        plots={plots}
        onPlotClick={handlePlotClick}
        onSelectPlot={handleSelectPlot}
      />

      {/* Нижняя панель вкладок */}
      <View style={styles.tabContainer}>
        <View style={styles.tabBar}>
          <TouchableOpacity
            style={[styles.tabItem, activeTab === 'BUILD' && styles.tabActive]}
            onPress={() => setActiveTab('BUILD')}
            activeOpacity={0.8}
          >
            <Text style={[styles.tabLabel, activeTab === 'BUILD' && { color: '#2ECC71' }]}>
              🔨 Стройка
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.tabItem, activeTab === 'MARKET' && styles.tabActive]}
            onPress={() => setActiveTab('MARKET')}
            activeOpacity={0.8}
          >
            <Text style={[styles.tabLabel, activeTab === 'MARKET' && { color: '#3498DB' }]}>
              🔋 Биржа
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.tabItem, activeTab === 'RESEARCH' && styles.tabActive]}
            onPress={() => setActiveTab('RESEARCH')}
            activeOpacity={0.8}
          >
            <Text style={[styles.tabLabel, activeTab === 'RESEARCH' && { color: '#9B59B6' }]}>
              🔬 Наука
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.tabItem, activeTab === 'PRESTIGE' && styles.tabActive]}
            onPress={() => setActiveTab('PRESTIGE')}
            activeOpacity={0.8}
          >
            <Text style={[styles.tabLabel, activeTab === 'PRESTIGE' && { color: '#F1C40F' }]}>
              👑 Престиж
            </Text>
          </TouchableOpacity>
        </View>

        {/* Контент активной вкладки */}
        <View style={styles.tabContent}>
          {activeTab === 'BUILD' && (
            <BuildTab
              gameState={gameState}
              plots={plots}
              onBuildOrUpgrade={handleBuildOrUpgrade}
              onEvolveSelected={handleEvolveSelected}
              onRepairSelected={handleRepairSelected}
              onDemolishSelected={handleDemolishSelected}
              onHireEngineer={handleHireEngineer}
              onToggleInsurance={handleToggleInsurance}
            />
          )}

          {activeTab === 'MARKET' && (
            <MarketTab
              gameState={gameState}
              onUpgradeBattery={handleUpgradeBattery}
              onDumpBattery={handleDumpBattery}
            />
          )}

          {activeTab === 'RESEARCH' && (
            <ResearchTab
              gameState={gameState}
              onResearchAero={handleResearchAero}
              onResearchCarbon={handleResearchCarbon}
              onResearchLightning={handleResearchLightning}
              onResearchRadar={handleResearchRadar}
              onResearchDrones={handleResearchDrones}
              onBuyDrone={handleBuyDrone}
            />
          )}

          {activeTab === 'PRESTIGE' && (
            <PrestigeTab
              gameState={gameState}
              plots={plots}
              achievements={achievements}
              onTriggerRebirth={handleTriggerRebirth}
              onClaimAchievement={handleClaimAchievement}
              onSaveGame={handleSaveGame}
              onResetGame={handleResetGame}
            />
          )}
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#0F131B',
  },
  tabContainer: {
    flex: 1,
    backgroundColor: '#121722',
  },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: '#161E2E',
    borderTopWidth: 1,
    borderTopColor: '#243046',
    borderBottomWidth: 1,
    borderBottomColor: '#243046',
  },
  tabItem: {
    flex: 1,
    paddingVertical: 9,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tabActive: {
    borderBottomWidth: 2,
    borderBottomColor: '#2ECC71',
    backgroundColor: '#1C273C',
  },
  tabLabel: {
    fontSize: 11,
    fontWeight: '700',
    color: '#8395A7',
  },
  tabContent: {
    flex: 1,
  },
});
