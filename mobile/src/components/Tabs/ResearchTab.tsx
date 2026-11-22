import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { GameState } from '../../types';

interface ResearchTabProps {
  gameState: GameState;
  onResearchAero: () => void;
  onResearchCarbon: () => void;
  onResearchLightning: () => void;
  onResearchRadar: () => void;
  onResearchDrones: () => void;
  onBuyDrone: () => void;
}

export const ResearchTab: React.FC<ResearchTabProps> = ({
  gameState,
  onResearchAero,
  onResearchCarbon,
  onResearchLightning,
  onResearchRadar,
  onResearchDrones,
  onBuyDrone,
}) => {
  const sp = gameState.sciencePoints;
  const aeroLvl = gameState.techAeroLevel;
  const carbonLvl = gameState.techCarbonLevel;

  const aeroCost = aeroLvl === 0 ? 40 : aeroLvl === 1 ? 120 : 300;
  const carbonCost = carbonLvl === 0 ? 50 : carbonLvl === 1 ? 150 : 400;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Баланс науки */}
      <View style={styles.scienceBadge}>
        <Text style={styles.scienceBadgeTitle}>🔬 ЛАБОРАТОРИЯ ТЕХНОЛОГИЙ</Text>
        <Text style={styles.scienceBalance}>
          Очки исследований: {sp.toFixed(1)} SP
        </Text>
      </View>

      {/* Аэродинамика */}
      <TouchableOpacity
        style={[
          styles.techCard,
          { borderColor: aeroLvl >= 3 ? '#27AE60' : '#2A3C54' },
        ]}
        disabled={aeroLvl >= 3}
        onPress={onResearchAero}
        activeOpacity={0.8}
      >
        <View style={styles.techHeader}>
          <Text style={styles.techTitle}>🍃 Аэродинамика лопастей</Text>
          <Text style={styles.techStatus}>
            {aeroLvl >= 3 ? 'МАКС (Lv.3)' : `Lv.${aeroLvl + 1} (${aeroCost} SP)`}
          </Text>
        </View>
        <Text style={styles.techDesc}>
          Увеличивает выработку энергии всех турбин на +15% за уровень.
        </Text>
      </TouchableOpacity>

      {/* Углепластик */}
      <TouchableOpacity
        style={[
          styles.techCard,
          { borderColor: carbonLvl >= 3 ? '#27AE60' : '#2A3C54' },
        ]}
        disabled={carbonLvl >= 3}
        onPress={onResearchCarbon}
        activeOpacity={0.8}
      >
        <View style={styles.techHeader}>
          <Text style={styles.techTitle}>🛡️ Углепластиковые сплавы</Text>
          <Text style={styles.techStatus}>
            {carbonLvl >= 3
              ? 'МАКС (Lv.3)'
              : `Lv.${carbonLvl + 1} (${carbonCost} SP)`}
          </Text>
        </View>
        <Text style={styles.techDesc}>
          Снижает скорость механического износа и поломок на -25% за уровень.
        </Text>
      </TouchableOpacity>

      {/* Громоотвод */}
      <TouchableOpacity
        style={[
          styles.techCard,
          { borderColor: gameState.techLightningRod ? '#27AE60' : '#2A3C54' },
        ]}
        disabled={gameState.techLightningRod}
        onPress={onResearchLightning}
        activeOpacity={0.8}
      >
        <View style={styles.techHeader}>
          <Text style={styles.techTitle}>⚡ Громоотвод и защита сети</Text>
          <Text style={styles.techStatus}>
            {gameState.techLightningRod ? 'ИЗУЧЕНО' : '100 SP'}
          </Text>
        </View>
        <Text style={styles.techDesc}>
          Молнии больше не разрушают вышки. Удар заряжает батарею (+350 кВт) и
          дает бонус +$1,500.
        </Text>
      </TouchableOpacity>

      {/* Радар бурь */}
      <TouchableOpacity
        style={[
          styles.techCard,
          { borderColor: gameState.techStormRadar ? '#27AE60' : '#2A3C54' },
        ]}
        disabled={gameState.techStormRadar}
        onPress={onResearchRadar}
        activeOpacity={0.8}
      >
        <View style={styles.techHeader}>
          <Text style={styles.techTitle}>📡 Радар раннего оповещения</Text>
          <Text style={styles.techStatus}>
            {gameState.techStormRadar ? 'ИЗУЧЕНО' : '150 SP'}
          </Text>
        </View>
        <Text style={styles.techDesc}>
          Автоматически глушит роторы при ударе торнадо, сводя урон к нулю.
        </Text>
      </TouchableOpacity>

      {/* Чертежи дронов */}
      <TouchableOpacity
        style={[
          styles.techCard,
          { borderColor: gameState.techDronesUnlocked ? '#27AE60' : '#2A3C54' },
        ]}
        disabled={gameState.techDronesUnlocked}
        onPress={onResearchDrones}
        activeOpacity={0.8}
      >
        <View style={styles.techHeader}>
          <Text style={styles.techTitle}>🤖 Чертеж ремонтных дронов</Text>
          <Text style={styles.techStatus}>
            {gameState.techDronesUnlocked ? 'ИЗУЧЕНО' : '220 SP'}
          </Text>
        </View>
        <Text style={styles.techDesc}>
          Открывает доступ к производству летающих ремонтных дронов на базе.
        </Text>
      </TouchableOpacity>

      {/* Покупка дрона */}
      <TouchableOpacity
        style={[
          styles.droneBuyButton,
          {
            backgroundColor: gameState.techDronesUnlocked
              ? '#16A085'
              : '#485460',
          },
        ]}
        disabled={!gameState.techDronesUnlocked}
        onPress={onBuyDrone}
        activeOpacity={0.8}
      >
        <Text style={styles.droneBuyText}>
          🚁 Купить ремонтного дрона ($5,000) [{gameState.droneCount} шт]
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
    gap: 8,
  },
  scienceBadge: {
    backgroundColor: '#1E1B2E',
    borderRadius: 10,
    padding: 10,
    borderWidth: 1,
    borderColor: '#3D2F64',
  },
  scienceBadgeTitle: {
    fontSize: 10,
    fontWeight: '800',
    color: '#9B59B6',
    marginBottom: 4,
  },
  scienceBalance: {
    fontSize: 13,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  techCard: {
    backgroundColor: '#192332',
    borderRadius: 8,
    padding: 10,
    borderWidth: 1,
    gap: 3,
  },
  techHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  techTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  techStatus: {
    fontSize: 11,
    fontWeight: '800',
    color: '#FECA57',
  },
  techDesc: {
    fontSize: 10,
    color: '#8395A7',
    lineHeight: 14,
  },
  droneBuyButton: {
    borderRadius: 8,
    paddingVertical: 11,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 4,
  },
  droneBuyText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 12,
  },
});
