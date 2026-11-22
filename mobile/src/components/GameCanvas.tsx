import React, { useRef, useEffect } from 'react';
import { View, StyleSheet, Dimensions } from 'react-native';
import { WebView } from 'react-native-webview';
import * as Haptics from 'expo-haptics';
import { PlotData, GameState, TURBINE_TYPES } from '../types';

const WebViewComponent: any = WebView;

interface GameCanvasProps {
  gameState: GameState;
  plots: PlotData[][];
  onPlotClick: (col: number, row: number, isCrit: boolean) => void;
  onSelectPlot: (col: number, row: number) => void;
}

export const GameCanvas: React.FC<GameCanvasProps> = ({
  gameState,
  plots,
  onPlotClick,
  onSelectPlot,
}) => {
  const webViewRef = useRef<any>(null);

  // Sync state to WebView
  useEffect(() => {
    if (webViewRef.current) {
      const data = JSON.stringify({
        type: 'UPDATE_STATE',
        gameState,
        plots,
      });
      webViewRef.current.postMessage(data);
    }
  }, [gameState, plots]);

  const handleMessage = (event: any) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);
      if (data.type === 'PLOT_CLICK') {
        if (gameState.hapticsEnabled) {
          if (data.isCrit) {
            Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
          } else {
            Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
          }
        }
        onPlotClick(data.col, data.row, data.isCrit);
      } else if (data.type === 'PLOT_SELECT') {
        if (gameState.hapticsEnabled) {
          Haptics.selectionAsync();
        }
        onSelectPlot(data.col, data.row);
      }
    } catch (e) {
      console.warn('WebView msg error:', e);
    }
  };

  const htmlContent = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; user-select: none; -webkit-user-select: none; }
    html, body { width: 100%; height: 100%; overflow: hidden; background: #0F131B; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
    canvas { width: 100%; height: 100%; display: block; touch-action: none; }
  </style>
</head>
<body>
  <canvas id="game"></canvas>
  <script>
    const canvas = document.getElementById('game');
    const ctx = canvas.getContext('2d');
    let dpr = window.devicePixelRatio || 1;

    let width = 0;
    let height = 0;

    function resize() {
      dpr = window.devicePixelRatio || 1;
      width = window.innerWidth;
      height = window.innerHeight;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      ctx.scale(dpr, dpr);
    }
    window.addEventListener('resize', resize);
    resize();

    // Sound Synthesizer (Web Audio API)
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    let audioCtx = null;
    function initAudio() {
      if (!audioCtx) audioCtx = new AudioContext();
      if (audioCtx.state === 'suspended') audioCtx.resume();
    }
    window.addEventListener('touchstart', initAudio, { once: true });
    window.addEventListener('click', initAudio, { once: true });

    function playSound(type) {
      if (!audioCtx) return;
      try {
        const now = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);

        if (type === 'click') {
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(800, now);
          osc.frequency.exponentialRampToValueAtTime(1400, now + 0.05);
          gain.gain.setValueAtTime(0.12, now);
          gain.gain.exponentialRampToValueAtTime(0.01, now + 0.05);
          osc.start(now);
          osc.stop(now + 0.05);
        } else if (type === 'crit') {
          osc.type = 'sawtooth';
          osc.frequency.setValueAtTime(523.25, now);
          osc.frequency.setValueAtTime(659.25, now + 0.05);
          osc.frequency.setValueAtTime(1046.50, now + 0.1);
          gain.gain.setValueAtTime(0.2, now);
          gain.gain.exponentialRampToValueAtTime(0.01, now + 0.22);
          osc.start(now);
          osc.stop(now + 0.22);
        } else if (type === 'coin') {
          osc.type = 'sine';
          osc.frequency.setValueAtTime(987.77, now);
          osc.frequency.setValueAtTime(1318.51, now + 0.08);
          gain.gain.setValueAtTime(0.15, now);
          gain.gain.exponentialRampToValueAtTime(0.01, now + 0.2);
          osc.start(now);
          osc.stop(now + 0.2);
        } else if (type === 'build') {
          osc.type = 'triangle';
          osc.frequency.setValueAtTime(220, now);
          osc.frequency.exponentialRampToValueAtTime(880, now + 0.15);
          gain.gain.setValueAtTime(0.2, now);
          gain.gain.exponentialRampToValueAtTime(0.01, now + 0.18);
          osc.start(now);
          osc.stop(now + 0.18);
        }
      } catch (e) {}
    }

    // State cache
    let gameState = {
      timeOfDay: 0.2,
      windSpeed: 8.5,
      weatherCondition: 'Бриз',
      selectedPlotCoord: null,
      comboMultiplier: 1,
      totalEnergyGenerated: 0
    };
    let plots = [];
    const TURBINES = ${JSON.stringify(TURBINE_TYPES)};

    // Particle & VFX arrays
    let floatingTexts = [];
    let sparkles = [];
    let clouds = [
      { x: 30, y: 25, speed: 0.18, w: 70 },
      { x: 180, y: 45, speed: 0.12, w: 90 },
      { x: 320, y: 15, speed: 0.22, w: 80 }
    ];
    let birds = [
      { x: 50, y: 60, vx: 0.8, vy: 0.1, wing: 0 },
      { x: 75, y: 68, vx: 0.85, vy: -0.05, wing: 1 },
      { x: 95, y: 55, vx: 0.78, vy: 0.08, wing: 2 }
    ];
    let rainDrops = [];
    for (let i = 0; i < 40; i++) {
      rainDrops.push({ x: Math.random() * 400, y: Math.random() * 600, speed: 6 + Math.random() * 4 });
    }

    // Drones
    let drones = [
      { x: 80, y: 120, vx: 0.6, vy: 0.4, isWelding: false }
    ];

    window.addEventListener('message', (e) => {
      try {
        const msg = JSON.parse(e.data);
        if (msg.type === 'UPDATE_STATE') {
          gameState = msg.gameState;
          plots = msg.plots;
          // Sync drone count
          while (drones.length < (gameState.droneCount || 0)) {
            drones.push({ x: 50 + Math.random() * 250, y: 50 + Math.random() * 150, vx: 0.5, vy: 0.3, isWelding: false });
          }
        }
      } catch(err) {}
    });

    document.addEventListener('message', (e) => {
      try {
        const msg = JSON.parse(e.data);
        if (msg.type === 'UPDATE_STATE') {
          gameState = msg.gameState;
          plots = msg.plots;
        }
      } catch(err) {}
    });

    // Touch / Click handler
    canvas.addEventListener('pointerdown', (e) => {
      initAudio();
      const rect = canvas.getBoundingClientRect();
      const mx = e.clientX - rect.left;
      const my = e.clientY - rect.top;

      const groundY = height * 0.12;
      const farmH = height * 0.74 - groundY;
      const cellW = width / 3;
      const cellH = farmH / 3;

      if (my >= groundY && my <= groundY + farmH) {
        const c = Math.floor(mx / cellW);
        const r = Math.floor((my - groundY) / cellH);

        if (c >= 0 && c < 3 && r >= 0 && r < 3) {
          const plot = plots[c] && plots[c][r];
          if (plot && plot.turbine) {
            plot.bladeAngle = (plot.bladeAngle + 50) % 360;
            const isCrit = Math.random() < 0.15;
            const cash = (isCrit ? 10 : 2) * (gameState.comboMultiplier || 1);

            playSound(isCrit ? 'crit' : 'click');

            // Floating text
            floatingTexts.push({
              x: mx,
              y: my - 15,
              text: isCrit ? '💥 КРИТ x5!' : '+' + cash.toFixed(2) + '$',
              color: isCrit ? '#FF4B4B' : '#FECA57',
              opacity: 1,
              vy: -1.6
            });

            // Sparkles
            const tCfg = TURBINES[plot.turbine];
            const spColor = isCrit ? '#FECA57' : (tCfg ? tCfg.neonColor : '#00FFB4');
            for (let i = 0; i < (isCrit ? 16 : 8); i++) {
              const ang = Math.random() * Math.PI * 2;
              const spd = 1.5 + Math.random() * 3.5;
              sparkles.push({
                x: mx,
                y: my,
                vx: Math.cos(ang) * spd,
                vy: Math.sin(ang) * spd,
                color: spColor,
                life: 25,
                maxLife: 25
              });
            }

            if (window.ReactNativeWebView) {
              window.ReactNativeWebView.postMessage(JSON.stringify({
                type: 'PLOT_CLICK',
                col: c,
                row: r,
                isCrit
              }));
            }
          } else {
            playSound('click');
            if (window.ReactNativeWebView) {
              window.ReactNativeWebView.postMessage(JSON.stringify({
                type: 'PLOT_SELECT',
                col: c,
                row: r
              }));
            }
          }
        }
      }
    });

    // Main 60 FPS Render Loop
    function render() {
      requestAnimationFrame(render);
      if (width <= 0 || height <= 0) return;

      const timeOfDay = gameState.timeOfDay || 0.2;
      const isNight = timeOfDay >= 0.45 && timeOfDay <= 0.88;
      const isDusk = timeOfDay > 0.38 && timeOfDay < 0.45;
      const isStorm = gameState.weatherCondition && gameState.weatherCondition.includes('Буря');
      const isRain = gameState.weatherCondition && (gameState.weatherCondition.includes('Дождь') || isStorm);

      // 1. SKY GRADIENT
      let skyTop, skyBot;
      if (isStorm) {
        skyTop = '#1C2430';
        skyBot = '#0F141C';
      } else if (isNight) {
        skyTop = '#0C1222';
        skyBot = '#18223A';
      } else if (isDusk) {
        skyTop = '#552D6E';
        skyBot = '#F57D50';
      } else {
        skyTop = '#5AB9F5';
        skyBot = '#3791D7';
      }

      const grad = ctx.createLinearGradient(0, 0, 0, height);
      grad.addColorStop(0, skyTop);
      grad.addColorStop(1, skyBot);
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, width, height);

      // Stars at night
      if (isNight && !isStorm) {
        ctx.fillStyle = '#FFFFFF';
        for (let i = 0; i < 25; i++) {
          const sx = (i * 47) % width;
          const sy = (i * 29) % (height * 0.25);
          ctx.globalAlpha = 0.3 + ((Math.sin(Date.now() * 0.003 + i) + 1) * 0.3);
          ctx.fillRect(sx, sy, 2, 2);
        }
        ctx.globalAlpha = 1.0;
      }

      // 2. CITY SKYLINE ON HORIZON
      const cityY = height * 0.72;
      const totalKwh = gameState.totalEnergyGenerated || 0;
      const buildingCount = 14;
      const bWidth = width / buildingCount;

      for (let i = 0; i < buildingCount; i++) {
        const bHeight = 40 + ((i * 37) % 55) + Math.min(60, totalKwh * 0.003);
        const bx = i * bWidth;
        const by = cityY - bHeight;

        ctx.fillStyle = isNight ? '#161F2E' : '#2C3E50';
        ctx.fillRect(bx + 1, by, bWidth - 2, bHeight + 40);

        // Windows
        if (isNight) {
          ctx.fillStyle = ((i % 3 === 0) ? '#00DCFF' : '#FECA57');
          for (let wy = by + 6; wy < cityY - 8; wy += 8) {
            for (let wx = bx + 4; wx < bx + bWidth - 4; wx += 6) {
              if ((i + wx + wy) % 5 !== 0) {
                ctx.globalAlpha = 0.75;
                ctx.fillRect(wx, wy, 3, 4);
              }
            }
          }
          ctx.globalAlpha = 1.0;
        }
      }

      // 3. GREEN HILLS & FIELD
      const groundY = height * 0.12;
      const farmH = height * 0.74 - groundY;
      const cellW = width / 3;
      const cellH = farmH / 3;

      // Base field background
      ctx.fillStyle = isNight ? '#153225' : '#2ECC71';
      ctx.beginPath();
      ctx.rect(0, groundY, width, height - groundY);
      ctx.fill();

      // 4. DRAW 3x3 PLOTS
      for (let r = 0; r < 3; r++) {
        for (let c = 0; c < 3; c++) {
          const px = c * cellW + 4;
          const py = groundY + r * cellH + 4;
          const pw = cellW - 8;
          const ph = cellH - 8;

          const isSelected = gameState.selectedPlotCoord &&
            gameState.selectedPlotCoord.col === c &&
            gameState.selectedPlotCoord.row === r;

          // Foundation pad
          ctx.fillStyle = isNight ? '#1A3F2F' : '#27AE60';
          ctx.beginPath();
          ctx.roundRect(px, py, pw, ph, 10);
          ctx.fill();

          if (isSelected) {
            ctx.strokeStyle = '#FECA57';
            ctx.lineWidth = 3;
            ctx.stroke();
          } else {
            ctx.strokeStyle = isNight ? '#22553E' : '#2ECC71';
            ctx.lineWidth = 1.5;
            ctx.stroke();
          }

          const plot = plots[c] && plots[c][r];
          if (plot && plot.turbine) {
            const tCfg = TURBINES[plot.turbine] || TURBINES.BASIC;
            const cx = px + pw / 2;
            const baseCy = py + ph * 0.88;
            const hubCy = py + ph * 0.42;

            // Mast
            ctx.strokeStyle = isNight ? '#8C9BAE' : '#ECEFF1';
            ctx.lineWidth = 5;
            ctx.beginPath();
            ctx.moveTo(cx, baseCy);
            ctx.lineTo(cx, hubCy);
            ctx.stroke();

            // Nacelle
            ctx.fillStyle = isNight ? '#96A5B9' : '#FFFFFF';
            ctx.beginPath();
            ctx.roundRect(cx - 10, hubCy - 6, 20, 12, 4);
            ctx.fill();

            // Hazard Beacon
            const beaconOn = Math.floor(Date.now() / 450) % 2 === 0;
            if (beaconOn) {
              ctx.fillStyle = '#FF3B30';
              ctx.beginPath();
              ctx.arc(cx, hubCy - 10, 3, 0, Math.PI * 2);
              ctx.fill();
            }

            // ROTATING BLADES
            plot.bladeAngle = (plot.bladeAngle + (gameState.windSpeed * 0.4) + 0.5) % 360;
            const bladeLen = (plot.turbine === 'BASIC') ? 28 :
                             (plot.turbine === 'ADVANCED') ? 34 :
                             (plot.turbine === 'INDUSTRIAL') ? 40 :
                             (plot.turbine === 'OFFSHORE') ? 46 :
                             (plot.turbine === 'SOLAR_HYBRID') ? 50 : 54;

            ctx.save();
            ctx.translate(cx, hubCy);
            ctx.rotate((plot.bladeAngle * Math.PI) / 180);

            for (let b = 0; b < 3; b++) {
              ctx.rotate((120 * Math.PI) / 180);
              ctx.beginPath();
              ctx.moveTo(-2.5, 0);
              ctx.lineTo(0, -bladeLen);
              ctx.lineTo(2.5, 0);
              ctx.closePath();

              if (isNight) {
                ctx.fillStyle = '#222A38';
                ctx.fill();
                ctx.strokeStyle = tCfg.neonColor;
                ctx.lineWidth = 2;
                ctx.stroke();
              } else {
                ctx.fillStyle = '#FFFFFF';
                ctx.fill();
                ctx.strokeStyle = '#D1D8E0';
                ctx.lineWidth = 1;
                ctx.stroke();

                // Red blade tip
                ctx.fillStyle = '#FF3838';
                ctx.fillRect(-2, -bladeLen, 4, 6);
              }
            }

            // Hub center
            ctx.fillStyle = isNight ? '#A4B0BE' : '#E4E9ED';
            ctx.beginPath();
            ctx.arc(0, 0, 5, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();

            // Durability Bar
            const barW = Math.min(pw * 0.7, 50);
            const barH = 5;
            const barX = cx - barW / 2;
            const barY = py + ph * 0.92;
            ctx.fillStyle = 'rgba(0,0,0,0.45)';
            ctx.fillRect(barX, barY, barW, barH);

            const hp = plot.durability || 100;
            const hpCol = hp > 50 ? '#2ECC71' : hp > 20 ? '#FECA57' : '#FF4B4B';
            ctx.fillStyle = hpCol;
            ctx.fillRect(barX + 1, barY + 1, (barW - 2) * (hp / 100), barH - 2);

            // Level pill
            ctx.fillStyle = 'rgba(15,20,30,0.85)';
            ctx.beginPath();
            ctx.roundRect(cx - 18, hubCy - 26, 36, 12, 6);
            ctx.fill();
            ctx.fillStyle = '#FECA57';
            ctx.font = 'bold 8px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText('Lv.' + (plot.level || 1), cx, hubCy - 17);

          } else {
            // Empty plot text
            const cx = px + pw / 2;
            const cy = py + ph / 2;
            ctx.fillStyle = isNight ? 'rgba(255,255,255,0.4)' : 'rgba(255,255,255,0.7)';
            ctx.font = 'bold 11px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText('+ СЛОТ', cx, cy - 4);
            ctx.font = '9px sans-serif';
            ctx.fillText('Свободен', cx, cy + 9);
          }
        }
      }

      // 5. DRONES
      drones.forEach((d) => {
        d.x += d.vx;
        d.y += d.vy;
        if (d.x < 30 || d.x > width - 30) d.vx *= -1;
        if (d.y < 40 || d.y > height * 0.6) d.vy *= -1;

        // Draw quadcopter
        ctx.fillStyle = '#2C3E50';
        ctx.fillRect(d.x - 7, d.y - 3, 14, 6);
        ctx.fillStyle = '#00DCFF';
        ctx.fillRect(d.x - 10, d.y - 5, 4, 2);
        ctx.fillRect(d.x + 6, d.y - 5, 4, 2);
        ctx.fillStyle = '#FECA57';
        ctx.beginPath();
        ctx.arc(d.x, d.y + 2, 2, 0, Math.PI * 2);
        ctx.fill();
      });

      // 6. CLOUDS & BIRDS
      clouds.forEach((c) => {
        c.x += c.speed;
        if (c.x > width + 100) c.x = -100;
        ctx.fillStyle = isNight ? 'rgba(40,55,75,0.4)' : 'rgba(255,255,255,0.7)';
        ctx.beginPath();
        ctx.ellipse(c.x, c.y, c.w / 2, 16, 0, 0, Math.PI * 2);
        ctx.fill();
      });

      birds.forEach((b) => {
        b.x += b.vx;
        b.y += b.vy;
        b.wing = (b.wing + 0.15) % (Math.PI * 2);
        if (b.x > width + 40) b.x = -40;

        ctx.strokeStyle = isNight ? '#576574' : '#2C3E50';
        ctx.lineWidth = 1.5;
        const wy = Math.sin(b.wing) * 3;
        ctx.beginPath();
        ctx.moveTo(b.x - 5, b.y + wy);
        ctx.lineTo(b.x, b.y);
        ctx.lineTo(b.x + 5, b.y + wy);
        ctx.stroke();
      });

      // 7. RAIN
      if (isRain) {
        ctx.strokeStyle = 'rgba(200, 230, 255, 0.45)';
        ctx.lineWidth = 1.2;
        rainDrops.forEach((r) => {
          r.y += r.speed;
          r.x -= 1.2;
          if (r.y > height) {
            r.y = -10;
            r.x = Math.random() * width;
          }
          ctx.beginPath();
          ctx.moveTo(r.x, r.y);
          ctx.lineTo(r.x - 2, r.y + 12);
          ctx.stroke();
        });
      }

      // 8. SPARKLES
      for (let i = sparkles.length - 1; i >= 0; i--) {
        const s = sparkles[i];
        s.x += s.vx;
        s.y += s.vy;
        s.life--;
        if (s.life <= 0) {
          sparkles.splice(i, 1);
          continue;
        }
        ctx.globalAlpha = s.life / s.maxLife;
        ctx.fillStyle = s.color;
        ctx.beginPath();
        ctx.arc(s.x, s.y, 2.5, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.globalAlpha = 1.0;

      // 9. FLOATING TEXTS
      for (let i = floatingTexts.length - 1; i >= 0; i--) {
        const ft = floatingTexts[i];
        ft.y += ft.vy;
        ft.opacity -= 0.024;
        if (ft.opacity <= 0) {
          floatingTexts.splice(i, 1);
          continue;
        }
        ctx.globalAlpha = Math.max(0, ft.opacity);
        ctx.fillStyle = ft.color;
        ctx.font = 'bold 13px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(ft.text, ft.x, ft.y);
      }
      ctx.globalAlpha = 1.0;
    }

    render();
  </script>
</body>
</html>
`;

  return (
    <View style={styles.container}>
      <WebViewComponent
        ref={webViewRef}
        originWhitelist={['*']}
        source={{ html: htmlContent }}
        onMessage={handleMessage}
        scrollEnabled={false}
        bounces={false}
        overScrollMode="never"
        style={styles.webView}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    height: Math.floor(Dimensions.get('window').height * 0.44),
    width: '100%',
    backgroundColor: '#0F131B',
  },
  webView: {
    backgroundColor: 'transparent',
    flex: 1,
  },
});
