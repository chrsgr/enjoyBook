import android.os.Build

object ApiConfig {
    fun getBaseUrl(): String {
        // Controlla se siamo in un emulatore
        return if (isEmulator()) {
            "http://10.0.2.2:4000"
        } else {
            "http://192.168.1.209:4000"
        }
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT
    }
}