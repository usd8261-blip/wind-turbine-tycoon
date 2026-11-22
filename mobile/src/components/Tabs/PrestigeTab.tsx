import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
} from 'react-native';
import { GameState, Achievement, PlotData } from '../../types';

interface PrestigeTabProps {
  gameState: GameState;
  plots: PlotData[][];
  achievements: Achievement[];
  onTriggerRebirth: () => void;
  onClaimAchievement: (id: string) => void;
  onSaveGame: () => void;
  onResetGame: () => void;
}

export const PrestigeTab: React.FC<PrestigeTabProps> = ({
  gameState,
  plots,
  achievements,
  onTriggerRebirth,
  onClaimAchievement,
  onSaveGame,
  onResetGame,
}) => {
  // Count active turbines
  let turbineCount = 0;
  for (let c = 0; c < 3; c++) {
    for (let r = 0; r < 3; r++) {
      if (plots[c]?.[r]?.turbine) turbineCount++;
    }
  }

  const canRebirth = gameState.balance >= 100000 && turbineCount >= 6;
  const currentBonus = gameState.rebirthCount * 50;

  const handleResetPress = () => {
    Alert.alert(
      'Сброс игры',
      'Вы действительно хотите сбросить весь прогресс и начать заново?',
      [
        { text: 'Отмена', style: 'cancel' },
        { text: 'Да, сбросить', style: 'destructive', onPress: onResetGame },
      ]
    );
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Карточка Контракта Города */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>🏙️ СРОЧНЫЙ КОНТРАКТ МЭРИИ</Text>
        {gameState.activeContract ? (
          <View style={styles.contractDetails}>
            <Text style={styles.contractName}>
              {gameState.activeContract.title}
            </Text>
            <View style={styles.contractRow}>
              <Text style={styles.contractText}>
                Нужно: <Text style={styles.bold}>{gameState.activeContract.requiredKw} кВт</Text>
              </Text>
              <Text style={styles.contractTimer}>
                ⏱️ {gameState.activeContract.remainingSeconds}с
              </Text>
            </View>
            <Text style={styles.contractReward}>
              Награда: +${(gameState.activeContract.reward * (1 + gameState.rebirthCount * 0.5)).toFixed(0)}
            </Text>
          </View>
        ) : (
          <Text style={styles.contractEmpty}>
            Ожидание нового заказа мэрии...
          </Text>
        )}
      </View>

      {/* Карточка Rebirth */}
      <View style={[styles.card, styles.rebirthCard]}>
        <Text style={styles.rebirthTitle}>👑 КОРПОРАТИВНЫЙ PRESTIGE / IPO</Text>
        <Text style={styles.rebirthDesc}>
          Выход на биржу и продажа контрольного пакета акций. Сбрасывает баланс и
          ветряки, но сохраняет технологии и даёт постоянный множитель:{' '}
          <Text style={styles.rebirthBonus}>+50% ко всем доходам навсегда</Text>!
        </Text>
        <Text style={styles.rebirthProgress}>
          Текущий бонус: +{currentBonus}% | Требуется: $100k и 6 вышек
        </Text>

        <TouchableOpacity
          style={[
            styles.rebirthButton,
            { backgroundColor: canRebirth ? '#F1C40F' : '#576574' },
          ]}
          disabled={!canRebirth}
          onPress={onTriggerRebirth}
          activeOpacity={0.8}
        >
          <Text style={styles.rebirthButtonText}>
            🌟 СОВЕРШИТЬ REBIRTH (+50% доход)
          </Text>
        </TouchableOpacity>
      </View>

      {/* Достижения */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>🏆 СИСТЕМА ДОСТИЖЕНИЙ</Text>
        <View style={styles.achievementsList}>
          {achievements.map((ach) => (
            <View key={ach.id} style={styles.achItem}>
              <View style={styles.achInfo}>
                <Text style={styles.achTitle}>{ach.title}</Text>
                <Text style={styles.achDesc}>{ach.description}</Text>
              </View>
              {ach.isClaimed ? (
                <View style={styles.claimedBadge}>
                  <Text style={styles.claimedText}>Получено</Text>
                </View>
              ) : ach.isUnlocked ? (
                <TouchableOpacity
                  style={styles.claimButton}
                  onPress={() => onClaimAchievement(ach.id)}
                  activeOpacity={0.8}
                >
                  <Text style={styles.claimButtonText}>+${ach.reward}</Text>
                </TouchableOpacity>
              ) : (
                <View style={styles.lockedBadge}>
                  <Text style={styles.lockedText}>+${ach.reward}</Text>
                </View>
              )}
            </View>
          ))}
        </View>
      </View>

      {/* Кнопки сохранения и сброса */}
      <View style={styles.systemRow}>
        <TouchableOpacity
          style={[styles.systemBtn, { backgroundColor: '#27AE60' }]}
          onPress={onSaveGame}
          activeOpacity={0.8}
        >
          <Text style={styles.systemBtnText}>💾 Сохранить</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.systemBtn, { backgroundColor: '#C0392B' }]}
          onPress={handleResetPress}
          activeOpacity={0.8}
        >
          <Text style={styles.systemBtnText}>🔄 Сброс</Text>
        </TouchableOpacity>
      </View>
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
    gap: 8,
  },
  card: {
    backgroundColor: '#1A2332',
    borderRadius: 10,
    padding: 10,
    borderWidth: 1,
    borderColor: '#2A3C54',
  },
  cardTitle: {
    fontSize: 11,
    fontWeight: '800',
    color: '#00D2D3',
    marginBottom: 6,
  },
  contractDetails: {
    gap: 4,
  },
  contractName: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 12,
  },
  contractRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  contractText: {
    color: '#A4B0BE',
    fontSize: 11,
  },
  bold: {
    fontWeight: '700',
    color: '#FFFFFF',
  },
  contractTimer: {
    color: '#FECA57',
    fontWeight: '800',
    fontSize: 12,
  },
  contractReward: {
    color: '#2ECC71',
    fontWeight: '800',
    fontSize: 12,
  },
  contractEmpty: {
    color: '#8395A7',
    fontSize: 11,
  },
  rebirthCard: {
    borderColor: '#F1C40F',
    backgroundColor: '#202636',
  },
  rebirthTitle: {
    fontSize: 11,
    fontWeight: '800',
    color: '#F1C40F',
    marginBottom: 4,
  },
  rebirthDesc: {
    fontSize: 10,
    color: '#C8D6E5',
    lineHeight: 14,
    marginBottom: 6,
  },
  rebirthBonus: {
    fontWeight: '800',
    color: '#FECA57',
  },
  rebirthProgress: {
    fontSize: 10,
    fontWeight: '700',
    color: '#8395A7',
    marginBottom: 8,
  },
  rebirthButton: {
    borderRadius: 8,
    paddingVertical: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  rebirthButtonText: {
    color: '#1E272E',
    fontWeight: '800',
    fontSize: 12,
  },
  achievementsList: {
    gap: 6,
  },
  achItem: {
    backgroundColor: '#151C28',
    borderRadius: 8,
    padding: 8,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: '#222F3E',
  },
  achInfo: {
    flex: 1,
    marginRight: 8,
  },
  achTitle: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 11,
  },
  achDesc: {
    color: '#8395A7',
    fontSize: 9,
    marginTop: 2,
  },
  claimButton: {
    backgroundColor: '#27AE60',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 5,
  },
  claimButtonText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 10,
  },
  claimedBadge: {
    backgroundColor: '#2C3E50',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 5,
  },
  claimedText: {
    color: '#7F8C8D',
    fontSize: 10,
  },
  lockedBadge: {
    backgroundColor: '#1E272E',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderWidth: 1,
    borderColor: '#2F3640',
  },
  lockedText: {
    color: '#576574',
    fontSize: 10,
  },
  systemRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4,
    marginBottom: 10,
  },
  systemBtn: {
    flex: 1,
    borderRadius: 8,
    paddingVertical: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  systemBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 12,
  },
});
