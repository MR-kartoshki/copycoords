package com.example.copycoords;

import java.io.OutputStream;

// Cross-platform utility for copying text to the system clipboard
public class ClipboardUtils {
    
    // Detect the operating system type
    private static String getOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("mac")) {
            return "macos";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return "linux";
        }
        return "unknown";
    }
    
    // Copy text to clipboard using the appropriate system command
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
    
    // Windows clipboard copy using clip.exe
    private static Process copyToClipboardWindows(String text) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "echo " + text + " | clip.exe");
        Process process = pb.start();
        return process;
    }
    
    // macOS clipboard copy using pbcopy
    private static Process copyToClipboardMacOS(String text) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("pbcopy");
        Process process = pb.start();
        try (OutputStream os = process.getOutputStream()) {
            os.write(text.getBytes());
            os.flush();
        }
        return process;
    }
    
    // Linux clipboard copy using xclip or xsel
    private static Process copyToClipboardLinux(String text) throws Exception {
        // Try xclip first, fall back to xsel if not available
        Process process;
        try {
            ProcessBuilder pb = new ProcessBuilder("xclip", "-selection", "clipboard");
            process = pb.start();
        } catch (Exception e) {
            // xclip not found, try xsel
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
            os.write(text.getBytes());
            os.flush();
        }
        return process;
    }
}
