import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { GameState } from '../../types';

interface MarketTabProps {
  gameState: GameState;
  onUpgradeBattery: () => void;
  onDumpBattery: () => void;
}

export const MarketTab: React.FC<MarketTabProps> = ({
  gameState,
  onUpgradeBattery,
  onDumpBattery,
}) => {
  const cap = gameState.batteryCapacity;
  const stored = gameState.batteryStored;
  const pct = cap > 0 ? (stored / cap) * 100 : 0;

  const baseRate = 0.18;
  const currentRate = baseRate * gameState.marketMultiplier;
  const isPeak = gameState.marketMultiplier > 1.8;
  const dumpValue = stored * currentRate;

  const getNextBatCost = () => {
    if (cap <= 0) return 4000;
    if (cap < 5000) return 14000;
    return 45000;
  };

  const getNextBatCapacity = () => {
    if (cap <= 0) return 1000;
    if (cap < 5000) return 5000;
    return 20000;
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Карточка Накопителя */}
      <View style={styles.card}>
        <Text style={styles.cardHeader}>🔋 ПОДСТАНЦИЯ НАКОПИТЕЛЕЙ</Text>
        <Text style={styles.cardStatus}>
          {cap > 0
            ? `Хранилище: ${stored.toFixed(0)} / ${cap} кВт⋅ч (${pct.toFixed(0)}%)`
            : 'Батарея не установлена'}
        </Text>

        <View style={styles.progressBar}>
          <View
            style={[
              styles.progressFill,
              { width: `${Math.min(100, Math.max(0, pct))}%` },
            ]}
          />
        </View>

        <Text style={styles.cardHint}>
          Накапливает избыток энергии при сильном ветре. Защищает город от
          блэкаута в штиль.
        </Text>
      </View>

      {/* Карточка Биржи */}
      <View style={styles.card}>
        <Text style={styles.cardHeader}>⚡ ЭНЕРГЕТИЧЕСКАЯ БИРЖА</Text>
        <View style={styles.priceRow}>
          <Text style={styles.priceLabel}>Текущий тариф:</Text>
          <Text
            style={[
              styles.priceValue,
              { color: isPeak ? '#FF4B4B' : '#FECA57' },
            ]}
          >
            ${currentRate.toFixed(2)} / кВт⋅ч {isPeak ? '🔥 [ПИКОВЫЙ СПРОС!]' : ''}
          </Text>
        </View>

        <TouchableOpacity
          style={[
            styles.dumpButton,
            { backgroundColor: stored > 10 ? '#E67E22' : '#7F8C8D' },
          ]}
          disabled={stored <= 10}
          onPress={onDumpBattery}
          activeOpacity={0.8}
        >
          <Text style={styles.dumpButtonText}>
            ⚡ Сбросить заряд на биржу (+${dumpValue.toFixed(2)})
          </Text>
        </TouchableOpacity>
      </View>

      {/* Кнопка улучшения батареи */}
      <TouchableOpacity
        style={styles.upgradeBatButton}
        onPress={onUpgradeBattery}
        activeOpacity={0.8}
      >
        <Text style={styles.upgradeBatButtonText}>
          {cap <= 0
            ? `🔋 Купить батарею ${getNextBatCapacity()} кВт⋅ч ($${(getNextBatCost() / 1000).toFixed(0)}k)`
            : `🔋 Расширить до ${getNextBatCapacity()} кВт⋅ч (+$${(getNextBatCost() / 1000).toFixed(0)}k)`}
        </Text>
      </TouchableOpacity>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#121722',
  },
  content: {
    padding: 10,
    gap: 10,
  },
  card: {
    backgroundColor: '#192332',
    borderRadius: 10,
    padding: 12,
    borderWidth: 1,
    borderColor: '#2A3C54',
  },
  cardHeader: {
    fontSize: 11,
    fontWeight: '800',
    color: '#3498DB',
    marginBottom: 6,
  },
  cardStatus: {
    fontSize: 12,
    fontWeight: '600',
    color: '#FFFFFF',
    marginBottom: 6,
  },
  progressBar: {
    height: 10,
    backgroundColor: 'rgba(0,0,0,0.4)',
    borderRadius: 5,
    overflow: 'hidden',
    marginBottom: 8,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#3498DB',
    borderRadius: 5,
  },
  cardHint: {
    fontSize: 11,
    color: '#8395A7',
    lineHeight: 15,
  },
  priceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 10,
  },
  priceLabel: {
    fontSize: 12,
    color: '#A4B0BE',
  },
  priceValue: {
    fontSize: 12,
    fontWeight: '800',
  },
  dumpButton: {
    borderRadius: 8,
    paddingVertical: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dumpButtonText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 12,
  },
  upgradeBatButton: {
    backgroundColor: '#2980B9',
    borderRadius: 8,
    paddingVertical: 11,
    alignItems: 'center',
    justifyContent: 'center',
  },
  upgradeBatButtonText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 12,
  },
});
