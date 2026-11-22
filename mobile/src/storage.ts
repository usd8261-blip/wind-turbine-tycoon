import AsyncStorage from '@react-native-async-storage/async-storage';
import { GameState, PlotData, Achievement } from './types';

const SAVE_KEY = '@wind_turbine_tycoon_save_v1';

export interface FullSaveData {
  state: Partial<GameState>;
  plots: PlotData[][];
  achievements: Achievement[];
}

export async function saveGameData(
  state: GameState,
  plots: PlotData[][],
  achievements: Achievement[]
): Promise<boolean> {
  try {
    const payload: FullSaveData = {
      state: {
        balance: state.balance,
        windSpeed: state.windSpeed,
        weatherCondition: state.weatherCondition,
        totalEnergyGenerated: state.totalEnergyGenerated,
        gameDays: state.gameDays,
        gameMonths: state.gameMonths,
        engineersCount: state.engineersCount,
        isInsured: state.isInsured,
        timeOfDay: state.timeOfDay,
        batteryCapacity: state.batteryCapacity,
        batteryStored: state.batteryStored,
        sciencePoints: state.sciencePoints,
        techAeroLevel: state.techAeroLevel,
        techCarbonLevel: state.techCarbonLevel,
        techLightningRod: state.techLightningRod,
        techStormRadar: state.techStormRadar,
        techDronesUnlocked: state.techDronesUnlocked,
        droneCount: state.droneCount,
        rebirthCount: state.rebirthCount,
        cityContractsCompleted: state.cityContractsCompleted,
        hapticsEnabled: state.hapticsEnabled,
        musicEnabled: state.musicEnabled,
      },
      plots,
      achievements,
    };
    await AsyncStorage.setItem(SAVE_KEY, JSON.stringify(payload));
    return true;
  } catch (err) {
    console.warn('Failed to save game:', err);
    return false;
  }
}

export async function loadGameData(): Promise<FullSaveData | null> {
  try {
    const json = await AsyncStorage.getItem(SAVE_KEY);
    if (!json) return null;
    return JSON.parse(json) as FullSaveData;
  } catch (err) {
    console.warn('Failed to load game save:', err);
    return null;
  }
}

export async function clearGameData(): Promise<void> {
  try {
    await AsyncStorage.removeItem(SAVE_KEY);
  } catch (err) {
    console.warn('Failed to clear save data:', err);
  }
}
