import { Audio } from 'expo-av';

class SoundManager {
  private bgmSound: Audio.Sound | null = null;
  private isBgmPlaying = false;
  private isMuted = false;

  async initBgm() {
    try {
      await Audio.setAudioModeAsync({
        playsInSilentModeIOS: true,
        staysActiveInBackground: false,
        shouldDuckAndroid: true,
      });

      // Try loading bundled bgm.wav / mp3
      const { sound } = await Audio.Sound.createAsync(
        require('../assets/music/bgm.wav'),
        { isLooping: true, volume: 0.55 },
        (status) => {
          if (status.isLoaded) {
            this.isBgmPlaying = status.isPlaying;
          }
        }
      );
      this.bgmSound = sound;
      if (!this.isMuted) {
        await this.bgmSound.playAsync();
      }
    } catch (e) {
      console.log('BGM init fallback (audio file optional):', e);
    }
  }

  async toggleBgm(): Promise<boolean> {
    this.isMuted = !this.isMuted;
    if (this.bgmSound) {
      try {
        if (this.isMuted) {
          await this.bgmSound.pauseAsync();
        } else {
          await this.bgmSound.playAsync();
        }
      } catch (err) {
        console.log('Error toggling BGM:', err);
      }
    }
    return !this.isMuted;
  }

  isMusicActive(): boolean {
    return !this.isMuted;
  }

  async unload() {
    if (this.bgmSound) {
      try {
        await this.bgmSound.unloadAsync();
      } catch {}
    }
  }
}

export const soundManager = new SoundManager();
