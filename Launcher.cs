using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

namespace WindTycoonLauncher {
    static class Program {
        [STAThread]
        static void Main() {
            try {
                string baseDir = AppDomain.CurrentDomain.BaseDirectory;
                string jarPath = Path.Combine(baseDir, "WindTycoonApp.jar");
                if (!File.Exists(jarPath)) {
                    jarPath = Path.Combine(baseDir, "WindTycoon.jar");
                }
                if (!File.Exists(jarPath)) {
                    jarPath = Path.Combine(baseDir, "app", "WindTycoonApp.jar");
                }
                if (!File.Exists(jarPath)) {
                    jarPath = Path.Combine(baseDir, "app", "WindTycoon.jar");
                }
                if (!File.Exists(jarPath)) {
                    MessageBox.Show("Файл WindTycoon.jar не найден!\nПоместите WindTycoon.exe рядом с WindTycoon.jar.", 
                                    "Wind Turbine Tycoon", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    return;
                }

                string javaBin = Path.Combine(baseDir, "runtime", "bin", "javaw.exe");
                if (!File.Exists(javaBin)) {
                    javaBin = "javaw.exe";
                }

                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = javaBin;
                psi.Arguments = "-jar \"" + jarPath + "\"";
                psi.WorkingDirectory = baseDir;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;

                try {
                    Process.Start(psi);
                } catch (Exception) {
                    psi.FileName = "java.exe";
                    psi.CreateNoWindow = false;
                    try {
                        Process.Start(psi);
                    } catch (Exception ex2) {
                        MessageBox.Show("Для запуска игры требуется Java 17 или 21.\nУстановите Java: https://adoptium.net\n\nОшибка: " + ex2.Message,
                                        "Требуется Java", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                    }
                }
            } catch (Exception ex) {
                MessageBox.Show("Ошибка запуска: " + ex.Message, "Wind Turbine Tycoon", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }
    }
}
