import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { GameState } from '../types';

interface HeaderHudProps {
  gameState: GameState;
  currentPowerOutput: number;
  incomePerSec: number;
  onToggleMusic: () => void;
  onToggleHaptics: () => void;
}

export const HeaderHud: React.FC<HeaderHudProps> = ({
  gameState,
  currentPowerOutput,
  incomePerSec,
  onToggleMusic,
  onToggleHaptics,
}) => {
  const isNight = gameState.timeOfDay >= 0.45 && gameState.timeOfDay <= 0.88;
  const isDusk = gameState.timeOfDay > 0.38 && gameState.timeOfDay < 0.45;
  const timeText = isNight ? '🌙 Ночь' : isDusk ? '🌇 Закат' : '☀️ День';

  const formatMoney = (val: number) => {
    return val.toLocaleString('ru-RU', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  return (
    <View style={styles.container}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Баланс */}
        <View style={styles.pill}>
          <Text style={styles.pillTitle}>💰 Баланс</Text>
          <Text style={[styles.pillValue, { color: '#2ECC71' }]}>
            ${formatMoney(gameState.balance)}
          </Text>
        </View>

        {/* Ветер */}
        <View style={styles.pill}>
          <Text style={styles.pillTitle}>💨 Ветер</Text>
          <Text style={[styles.pillValue, { color: '#00D2D3' }]}>
            {gameState.windSpeed.toFixed(1)} м/с
          </Text>
        </View>

        {/* Сеть */}
        <View style={styles.pill}>
          <Text style={styles.pillTitle}>⚡ Сеть</Text>
          <Text style={[styles.pillValue, { color: '#FECA57' }]}>
            {currentPowerOutput.toFixed(1)} кВт (+${incomePerSec.toFixed(2)}/с)
          </Text>
        </View>

        {/* Батарея */}
        <View style={styles.pill}>
          <Text style={styles.pillTitle}>🔋 Батарея</Text>
          <Text style={[styles.pillValue, { color: '#3498DB' }]}>
            {gameState.batteryCapacity > 0
              ? `${gameState.batteryStored.toFixed(0)}/${gameState.batteryCapacity} кВт⋅ч`
              : 'Нет'}
          </Text>
        </View>

        {/* Наука */}
        <View style={styles.pill}>
          <Text style={styles.pillTitle}>🔬 Наука</Text>
          <Text style={[styles.pillValue, { color: '#9B59B6' }]}>
            {gameState.sciencePoints.toFixed(1)} SP
          </Text>
        </View>

        {/* Престиж */}
        <View style={styles.pill}>
          <Text style={styles.pillTitle}>👑 Престиж</Text>
          <Text style={[styles.pillValue, { color: '#F1C40F' }]}>
            R{gameState.rebirthCount} (x{(1 + gameState.rebirthCount * 0.5).toFixed(1)})
          </Text>
        </View>

        {/* Время */}
        <View style={styles.pill}>
          <Text style={styles.pillTitle}>🕒 Время</Text>
          <Text style={[styles.pillValue, { color: '#FFEAA7' }]}>{timeText}</Text>
        </View>

        {/* Музыка */}
        <TouchableOpacity
          style={[styles.pill, styles.clickablePill]}
          onPress={onToggleMusic}
          activeOpacity={0.7}
        >
          <Text style={styles.pillTitle}>🎵 Музыка</Text>
          <Text style={[styles.pillValue, { color: gameState.musicEnabled ? '#54A0FF' : '#8395A7' }]}>
            {gameState.musicEnabled ? '🔊 Вкл' : '🔇 Выкл'}
          </Text>
        </TouchableOpacity>

        {/* Haptics */}
        <TouchableOpacity
          style={[styles.pill, styles.clickablePill]}
          onPress={onToggleHaptics}
          activeOpacity={0.7}
        >
          <Text style={styles.pillTitle}>📳 Вибрация</Text>
          <Text style={[styles.pillValue, { color: gameState.hapticsEnabled ? '#1DD1A1' : '#8395A7' }]}>
            {gameState.hapticsEnabled ? 'Вкл' : 'Выкл'}
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#0F131B',
    paddingVertical: 5,
    borderBottomWidth: 1,
    borderBottomColor: '#1A2232',
  },
  scrollContent: {
    paddingHorizontal: 8,
    gap: 6,
  },
  pill: {
    backgroundColor: '#161E2E',
    borderRadius: 8,
    paddingHorizontal: 9,
    paddingVertical: 4,
    minWidth: 70,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#243046',
  },
  clickablePill: {
    borderColor: '#3B4D6E',
  },
  pillTitle: {
    fontSize: 9,
    color: '#8395A7',
    fontWeight: '600',
    marginBottom: 1,
  },
  pillValue: {
    fontSize: 11,
    fontWeight: '700',
  },
});
