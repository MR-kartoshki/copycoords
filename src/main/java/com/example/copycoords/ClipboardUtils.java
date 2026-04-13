
package com.example.copycoords;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ClipboardUtils {

    private static String getOperatingSystem() {
        String osName = System.getProperty("os.name", "unknown").toLowerCase();
        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("mac")) {
            return "macos";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return "linux";
        }
        return "unknown";
    }

    public static void copyToClipboard(String text) throws Exception {
        String os = getOperatingSystem();
        
        Process process;
        switch (os) {
            case "windows":
                process = copyToClipboardWindows(text);
                break;
            case "macos":
                process = copyToClipboardMacOS(text);
                break;
            case "linux":
                process = copyToClipboardLinux(text);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Clipboard command failed with exit code: " + exitCode);
        }
    }

    private static Process copyToClipboardWindows(String text) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("clip.exe");
        Process process = pb.start();
        try (OutputStream os = process.getOutputStream()) {
            os.write(('\uFEFF' + text).getBytes(StandardCharsets.UTF_16LE));
            os.flush();
        }
        return process;
    }

    private static Process copyToClipboardMacOS(String text) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("pbcopy");
        Process process = pb.start();
        try (OutputStream os = process.getOutputStream()) {
            os.write(text.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        return process;
    }

    private static Process copyToClipboardLinux(String text) throws Exception {

        Process process;
        try {
            ProcessBuilder pb = new ProcessBuilder("xclip", "-selection", "clipboard");
            process = pb.start();
        } catch (Exception e) {

            try {
                ProcessBuilder pb = new ProcessBuilder("xsel", "--clipboard", "--input");
                process = pb.start();
            } catch (Exception e2) {
                throw new Exception("Neither xclip nor xsel found. Please install one of them:\n" +
                    "  Ubuntu/Debian: sudo apt-get install xclip\n" +
                    "  Fedora/RHEL: sudo dnf install xclip\n" +
                    "  Arch: sudo pacman -S xclip");
            }
        }
        
        try (OutputStream os = process.getOutputStream()) {
            os.write(text.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        return process;
    }
}

