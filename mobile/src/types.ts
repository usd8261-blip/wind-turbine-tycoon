export type TurbineTypeId =
  | 'BASIC'
  | 'ADVANCED'
  | 'INDUSTRIAL'
  | 'OFFSHORE'
  | 'SOLAR_HYBRID'
  | 'QUANTUM_VAWT';

export interface TurbineConfig {
  id: TurbineTypeId;
  name: string;
  shortName: string;
  cost: number;
  maxPowerKw: number;
  baseIncomePerSec: number;
  wearRate: number;
  themeColor: string;
  neonColor: string;
  perk: string;
}

export const TURBINE_TYPES: Record<TurbineTypeId, TurbineConfig> = {
  BASIC: {
    id: 'BASIC',
    name: 'Малый ветряк',
    shortName: 'Малый',
    cost: 1000,
    maxPowerKw: 50,
    baseIncomePerSec: 4.8,
    wearRate: 0.04,
    themeColor: '#2ECC71',
    neonColor: '#00FFB4',
    perk: 'Классический ротор',
  },
  ADVANCED: {
    id: 'ADVANCED',
    name: 'Средний ветряк',
    shortName: 'Средний',
    cost: 3200,
    maxPowerKw: 180,
    baseIncomePerSec: 18.0,
    wearRate: 0.07,
    themeColor: '#3498DB',
    neonColor: '#00DCFF',
    perk: 'Усиленная мачта',
  },
  INDUSTRIAL: {
    id: 'INDUSTRIAL',
    name: 'Гигант ветропарка',
    shortName: 'Гигант',
    cost: 9000,
    maxPowerKw: 550,
    baseIncomePerSec: 68.0,
    wearRate: 0.1,
    themeColor: '#9B59B6',
    neonColor: '#FF4BC8',
    perk: 'Высокая мощность',
  },
  OFFSHORE: {
    id: 'OFFSHORE',
    name: 'Офшорный ветряк',
    shortName: 'Офшор',
    cost: 24000,
    maxPowerKw: 1800,
    baseIncomePerSec: 210.0,
    wearRate: 0.08,
    themeColor: '#1877F2',
    neonColor: '#64DCFF',
    perk: 'Морской бриз (+30% в штиль)',
  },
  SOLAR_HYBRID: {
    id: 'SOLAR_HYBRID',
    name: 'Ветро-Солнечный',
    shortName: 'Гибрид',
    cost: 55000,
    maxPowerKw: 4500,
    baseIncomePerSec: 560.0,
    wearRate: 0.06,
    themeColor: '#F39C12',
    neonColor: '#FFDC32',
    perk: 'Солнечные батареи днем',
  },
  QUANTUM_VAWT: {
    id: 'QUANTUM_VAWT',
    name: 'Квантовый VAWT',
    shortName: 'VAWT',
    cost: 140000,
    maxPowerKw: 12000,
    baseIncomePerSec: 1500.0,
    wearRate: 0.02,
    themeColor: '#E91E63',
    neonColor: '#FF50A0',
    perk: 'Вертикальный ротор, мин. износ',
  },
};

export const TURBINE_ORDER: TurbineTypeId[] = [
  'BASIC',
  'ADVANCED',
  'INDUSTRIAL',
  'OFFSHORE',
  'SOLAR_HYBRID',
  'QUANTUM_VAWT',
];

export function getNextTier(currentId: TurbineTypeId): TurbineTypeId | null {
  const idx = TURBINE_ORDER.indexOf(currentId);
  if (idx >= 0 && idx + 1 < TURBINE_ORDER.length) {
    return TURBINE_ORDER[idx + 1];
  }
  return null;
}

export interface PlotData {
  col: number;
  row: number;
  turbine: TurbineTypeId | null;
  level: number;
  durability: number;
  bladeAngle: number;
}

export interface CityContract {
  title: string;
  requiredKw: number;
  remainingSeconds: number;
  reward: number;
}

export interface Achievement {
  id: string;
  title: string;
  description: string;
  reward: number;
  isUnlocked: boolean;
  isClaimed: boolean;
}

export interface RepairDrone {
  x: number;
  y: number;
  targetCol: number;
  targetRow: number;
  isWelding: boolean;
}

export interface FloatingText {
  id: number;
  x: number;
  y: number;
  text: string;
  color: string;
  opacity: number;
  vy: number;
}

export interface Sparkle {
  id: number;
  x: number;
  y: number;
  vx: number;
  vy: number;
  color: string;
  life: number;
  maxLife: number;
}

export interface GameState {
  balance: number;
  windSpeed: number;
  weatherCondition: string;
  totalEnergyGenerated: number;
  gameDays: number;
  gameMonths: number;
  engineersCount: number;
  isInsured: boolean;
  timeOfDay: number; // 0..1 (0.5 = midday, 0.7 = night)
  batteryCapacity: number;
  batteryStored: number;
  marketMultiplier: number;
  sciencePoints: number;
  techAeroLevel: number;
  techCarbonLevel: number;
  techLightningRod: boolean;
  techStormRadar: boolean;
  techDronesUnlocked: boolean;
  droneCount: number;
  rebirthCount: number;
  cityContractsCompleted: number;
  activeContract: CityContract | null;
  selectedPlotCoord: { col: number; row: number } | null;
  hapticsEnabled: boolean;
  musicEnabled: boolean;
  comboMultiplier: number;
}
