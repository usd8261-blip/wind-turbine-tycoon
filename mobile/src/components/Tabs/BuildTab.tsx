import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import {
  PlotData,
  GameState,
  TURBINE_TYPES,
  TURBINE_ORDER,
  TurbineTypeId,
  getNextTier,
} from '../../types';

interface BuildTabProps {
  gameState: GameState;
  plots: PlotData[][];
  onBuildOrUpgrade: (typeId: TurbineTypeId) => void;
  onEvolveSelected: () => void;
  onRepairSelected: () => void;
  onDemolishSelected: () => void;
  onHireEngineer: () => void;
  onToggleInsurance: () => void;
}

export const BuildTab: React.FC<BuildTabProps> = ({
  gameState,
  plots,
  onBuildOrUpgrade,
  onEvolveSelected,
  onRepairSelected,
  onDemolishSelected,
  onHireEngineer,
  onToggleInsurance,
}) => {
  const coord = gameState.selectedPlotCoord;
  const selectedPlot = coord ? plots[coord.col]?.[coord.row] : null;

  const currentTurbine = selectedPlot?.turbine
    ? TURBINE_TYPES[selectedPlot.turbine]
    : null;
  const nextTierId = selectedPlot?.turbine
    ? getNextTier(selectedPlot.turbine)
    : null;
  const nextTurbine = nextTierId ? TURBINE_TYPES[nextTierId] : null;

  const formatCost = (val: number) => {
    if (val >= 1000) {
      return (val / 1000).toFixed(val % 1000 === 0 ? 0 : 1) + 'k';
    }
    return val.toString();
  };

  const repCost = selectedPlot
    ? Math.max(0, (100.0 - selectedPlot.durability) * 1.5)
    : 0;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Карточка выбранного слота */}
      <View style={styles.plotCard}>
        <View style={styles.plotHeader}>
          <Text style={styles.plotTitle}>
            {selectedPlot
              ? currentTurbine
                ? `${currentTurbine.name} [${selectedPlot.col + 1}, ${selectedPlot.row + 1}]`
                : `Участок [${selectedPlot.col + 1}, ${selectedPlot.row + 1}] (Свободен)`
              : 'Участок не выбран (коснитесь поля)'}
          </Text>
          {selectedPlot?.turbine && (
            <Text style={styles.plotLevel}>Lv.{selectedPlot.level}</Text>
          )}
        </View>

        {selectedPlot?.turbine ? (
          <View style={styles.durabilityRow}>
            <Text style={styles.durabilityText}>
              Прочность: {selectedPlot.durability.toFixed(1)}%
            </Text>
            <View style={styles.durabilityBar}>
              <View
                style={[
                  styles.durabilityFill,
                  {
                    width: `${Math.max(0, Math.min(100, selectedPlot.durability))}%`,
                    backgroundColor:
                      selectedPlot.durability > 50
                        ? '#2ECC71'
                        : selectedPlot.durability > 25
                        ? '#FECA57'
                        : '#FF4B4B',
                  },
                ]}
              />
            </View>
          </View>
        ) : (
          <Text style={styles.plotSub}>
            Выберите модель ниже для возведения новой электростанции
          </Text>
        )}
      </View>

      {/* Кнопка Главной Эволюции */}
      {selectedPlot?.turbine ? (
        <TouchableOpacity
          style={[
            styles.actionButton,
            {
              backgroundColor: nextTurbine ? nextTurbine.themeColor : '#F1C40F',
            },
          ]}
          onPress={onEvolveSelected}
          activeOpacity={0.8}
        >
          <Text style={styles.actionButtonText}>
            {nextTurbine
              ? `🚀 Эволюция: ${nextTurbine.shortName} (+$${formatCost(nextTurbine.cost - currentTurbine!.cost)})`
              : `⭐ Форсировать Lv.${selectedPlot.level + 1} ($${formatCost(currentTurbine!.cost * 0.5 * selectedPlot.level)})`}
          </Text>
        </TouchableOpacity>
      ) : null}

      {/* Сетка выбора 6 типов ветряков */}
      <View style={styles.grid}>
        {TURBINE_ORDER.map((id) => {
          const cfg = TURBINE_TYPES[id];
          let btnText = `${cfg.shortName} ($${formatCost(cfg.cost)})`;
          let disabled = false;

          if (selectedPlot?.turbine) {
            const currentIdx = TURBINE_ORDER.indexOf(selectedPlot.turbine);
            const targetIdx = TURBINE_ORDER.indexOf(id);

            if (targetIdx === currentIdx) {
              btnText = `${cfg.shortName} (Установлен)`;
              disabled = true;
            } else if (targetIdx < currentIdx) {
              btnText = `${cfg.shortName} (Пройден)`;
              disabled = true;
            } else {
              const diff = cfg.cost - currentTurbine!.cost;
              btnText = `⬆ ${cfg.shortName} (+${formatCost(diff)})`;
              disabled = false;
            }
          }

          return (
            <TouchableOpacity
              key={id}
              style={[
                styles.gridButton,
                { backgroundColor: disabled ? '#2C3A4E' : cfg.themeColor },
              ]}
              disabled={disabled}
              onPress={() => onBuildOrUpgrade(id)}
              activeOpacity={0.8}
            >
              <Text
                style={[
                  styles.gridButtonText,
                  disabled && { color: '#7F8C8D' },
                ]}
                numberOfLines={1}
              >
                {btnText}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {/* Ремонт и Демонтаж */}
      {selectedPlot?.turbine && (
        <View style={styles.dualRow}>
          <TouchableOpacity
            style={[
              styles.dualButton,
              { backgroundColor: selectedPlot.durability < 99.5 ? '#E67E22' : '#7F8C8D' },
            ]}
            disabled={selectedPlot.durability >= 99.5}
            onPress={onRepairSelected}
            activeOpacity={0.8}
          >
            <Text style={styles.dualButtonText}>
              {selectedPlot.durability < 99.5
                ? `🔧 Починить ($${repCost.toFixed(2)})`
                : '🔧 Исправен (100%)'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.dualButton, { backgroundColor: '#C0392B' }]}
            onPress={onDemolishSelected}
            activeOpacity={0.8}
          >
            <Text style={styles.dualButtonText}>
              🗑️ Снести (+${formatCost(currentTurbine!.cost * 0.5)})
            </Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Персонал и Страховка */}
      <View style={styles.bottomServices}>
        <TouchableOpacity
          style={[styles.serviceButton, { backgroundColor: '#16A085' }]}
          onPress={onHireEngineer}
          activeOpacity={0.8}
        >
          <Text style={styles.serviceButtonText}>
            👷 Техник ($250, -$1/с) [{gameState.engineersCount} чел]
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            styles.serviceButton,
            { backgroundColor: gameState.isInsured ? '#27AE60' : '#2980B9' },
          ]}
          onPress={onToggleInsurance}
          activeOpacity={0.8}
        >
          <Text style={styles.serviceButtonText}>
            {gameState.isInsured
              ? '🛡️ Страховка: АКТИВНА ($800 выплата)'
              : '📜 Оформить страховку ($10/мес)'}
          </Text>
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
  plotCard: {
    backgroundColor: '#1A2332',
    borderRadius: 10,
    padding: 10,
    borderWidth: 1,
    borderColor: '#2A3B52',
  },
  plotHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  plotTitle: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 13,
  },
  plotLevel: {
    color: '#FECA57',
    fontWeight: '800',
    fontSize: 12,
  },
  plotSub: {
    color: '#8395A7',
    fontSize: 11,
    marginTop: 4,
  },
  durabilityRow: {
    marginTop: 6,
    gap: 3,
  },
  durabilityText: {
    color: '#A4B0BE',
    fontSize: 10,
  },
  durabilityBar: {
    height: 6,
    backgroundColor: 'rgba(0,0,0,0.4)',
    borderRadius: 3,
    overflow: 'hidden',
  },
  durabilityFill: {
    height: '100%',
    borderRadius: 3,
  },
  actionButton: {
    borderRadius: 8,
    paddingVertical: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  actionButtonText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 13,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  gridButton: {
    width: '48.9%',
    borderRadius: 8,
    paddingVertical: 9,
    paddingHorizontal: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  gridButtonText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 11,
  },
  dualRow: {
    flexDirection: 'row',
    gap: 6,
  },
  dualButton: {
    flex: 1,
    borderRadius: 8,
    paddingVertical: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dualButtonText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 11,
  },
  bottomServices: {
    gap: 6,
    marginTop: 2,
  },
  serviceButton: {
    borderRadius: 8,
    paddingVertical: 9,
    alignItems: 'center',
    justifyContent: 'center',
  },
  serviceButtonText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 11,
  },
});
