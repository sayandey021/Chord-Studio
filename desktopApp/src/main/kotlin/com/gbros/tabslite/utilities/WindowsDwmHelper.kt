package com.gbros.tabslite.utilities

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference
import java.awt.Window

object WindowsDwmHelper {

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33
    private const val DWMWA_BORDER_COLOR = 34
    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_TEXT_COLOR = 36

    private const val DWMWCP_ROUND = 2 // Windows 11 rounded corners

    private interface Dwmapi : Library {
        fun DwmSetWindowAttribute(
            hwnd: HWND,
            dwAttribute: Int,
            pvAttribute: Pointer,
            cbAttribute: Int
        ): Int

        companion object {
            val INSTANCE: Dwmapi? = try {
                if (System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) {
                    Native.load("dwmapi", Dwmapi::class.java)
                } else null
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun applyWindows11DarkTheme(window: Window, captionColorHex: Int = 0x151821) {
        try {
            if (!System.getProperty("os.name", "").contains("Windows", ignoreCase = true)) return
            val dwmapi = Dwmapi.INSTANCE ?: return

            val hwndPointer = Native.getWindowPointer(window) ?: return
            val hwnd = HWND(hwndPointer)

            // 1. Enable Windows 11 Immersive Dark Mode for Titlebar and Frame
            val darkMode = IntByReference(1)
            try {
                dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkMode.pointer, 4)
            } catch (_: Throwable) {}
            try {
                dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1, darkMode.pointer, 4)
            } catch (_: Throwable) {}

            // 2. Enforce Windows 11 Rounded Corners Preference
            try {
                val cornerPref = IntByReference(DWMWCP_ROUND)
                dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, cornerPref.pointer, 4)
            } catch (_: Throwable) {}

            // 3. Set Custom Titlebar Caption Color (Convert RGB 0xRRGGBB to Win32 BGR 0x00BBGGRR)
            try {
                val r = (captionColorHex shr 16) and 0xFF
                val g = (captionColorHex shr 8) and 0xFF
                val b = captionColorHex and 0xFF
                val bgrColor = (b shl 16) or (g shl 8) or r

                val captionColor = IntByReference(bgrColor)
                dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, captionColor.pointer, 4)
            } catch (_: Throwable) {}

            // 4. Set Text Color to Crisp Light Slate (RGB 0xF1F5F9 -> BGR 0x00F9F5F1)
            try {
                val textColor = IntByReference(0x00F9F5F1)
                dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, textColor.pointer, 4)
            } catch (_: Throwable) {}

            // 5. Set Border Color to subtle slate (RGB 0x262A36 -> BGR 0x00362A26)
            try {
                val borderColor = IntByReference(0x00362A26)
                dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, borderColor.pointer, 4)
            } catch (_: Throwable) {}
        } catch (e: Throwable) {
            println("Windows DWM notice: ${e.message}")
        }
    }
}
