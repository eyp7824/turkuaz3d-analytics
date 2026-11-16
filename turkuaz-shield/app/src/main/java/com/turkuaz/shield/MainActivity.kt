package com.turkuaz.shield

import android.app.Activity
import android.os.Bundle
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import java.util.Locale
import java.security.MessageDigest

class MainActivity : Activity() {
    private val bg = Color.rgb(3, 16, 31)
    private val panel = Color.rgb(9, 38, 64)
    private val cyan = Color.rgb(34, 224, 239)
    private lateinit var content: LinearLayout
    private val pickApk = 2101
    private val pickCleanup = 2102
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); home() }
    private fun shape(color:Int): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius=24f; setStroke(2, cyan) }
    private fun label(s:String, size:Float=18f, color:Int=Color.WHITE):TextView = TextView(this).apply { text=s; textSize=size; setTextColor(color); gravity=Gravity.CENTER; setPadding(10,16,10,16) }
    private fun screen(title:String) {
        val sc=ScrollView(this).apply { setBackgroundColor(bg) }
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(22,28,22,30) }
        sc.addView(content); setContentView(sc)
        val heading=label(title,27f,cyan).apply { setTypeface(null,Typeface.BOLD) }
        content.addView(heading)
    }
    private fun button(title:String, detail:String="", onClick:()->Unit) {
        val t=label(if(detail.isEmpty()) title else "$title\n$detail",18f)
        t.background=shape(panel)
        val lp=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,10,0,10) }
        content.addView(t,lp); t.setOnClickListener { onClick() }
    }
    private fun home() {
        screen("⬡  Turkuaz Shield")
        content.addView(label("Akıllı Telefon Koruması",16f,cyan))
        content.addView(label("◈",94f,cyan))
        content.addView(label("Cihaz Bilgileri",22f))
        val battery=registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level=battery?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1) ?: -1
        val scale=battery?.getIntExtra(BatteryManager.EXTRA_SCALE,100) ?: 100
        val percent=if(level>=0 && scale>0) level*100/scale else -1
        val stat=StatFs(Environment.getDataDirectory().path)
        val total=stat.totalBytes/1073741824.0; val free=stat.availableBytes/1073741824.0
        content.addView(label("Pil: ${if(percent>=0) "$percent%" else "Bilinmiyor"}  •  Depolama: ${String.format(Locale.US,"%.1f",total-free)} / ${String.format(Locale.US,"%.1f",total)} GB",16f))
        button("🧹  Güvenli Temizlik","Depolama incelemesi") { storage() }
        button("🔄  Güncelleme Artıkları","Güvenli inceleme ve sınırlar") { leftovers() }
        button("🛡  Güvenlik Kontrolü","Yüklü uygulamalar ve izinler") { apps() }
        button("🔍  APK Dosyası İncele","SHA-256 parmak izi hesapla") { chooseApk() }
        button("🚫  Reklam Kalkanı","Koruma seçenekleri") { ads() }
        button("⚡  Şarj Kontrolü","Gerçek pil durumu") { charge() }
        button("ℹ  Hakkında","Sürüm 0.1.0") { about() }
    }
    private fun back() { button("← Ana Ekran") { home() } }
    private fun storage() { screen("Güvenli Temizlik"); val st=StatFs(Environment.getDataDirectory().path)
        content.addView(label("Kullanılabilir alan: ${String.format(Locale.US,"%.2f",st.availableBytes/1073741824.0)} GB"))
        content.addView(label("Bu sürüm otomatik dosya silmez. Dosyalarınız ve hesap oturumlarınız korunur.",16f))
        button("İndirilen dosyaları incele") { chooseCleanup() }
        button("Sistem depolama ayarlarını aç") { startActivity(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }; back() }
    private fun leftovers() { screen("Güncelleme Artıkları"); content.addView(label("Android diğer uygulamaların korumalı güncelleme dosyalarını göstermeyebilir. Bu sürüm dosya silmez ve sahte tarama sonucu üretmez.",17f)); back() }
    private fun apps() { screen("Uygulama Güvenlik Kontrolü")
        val packages=packageManager.getInstalledApplications(PackageManager.GET_META_DATA).sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
        content.addView(label("Görülebilen uygulama sayısı: ${packages.size}",16f,cyan))
        packages.take(150).forEach { app -> content.addView(label("${packageManager.getApplicationLabel(app)}\n${app.packageName}",13f)) }
        content.addView(label("Bu liste antivirüs taraması değildir. Zararlı yazılım teşhisi yapılmadı.",15f)); back() }
    private fun chooseCleanup() {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type="*/*"
        }
        startActivityForResult(intent,pickCleanup)
    }
    private fun chooseApk() {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type="*/*"
        }
        startActivityForResult(intent,pickApk)
    }
    @Deprecated("Legacy document picker callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if((requestCode!=pickApk && requestCode!=pickCleanup) || resultCode!=RESULT_OK) return
        val uri=data?.data ?: return
        if(requestCode==pickCleanup) {
            screen("Dosya İncelemesi")
            val name=android.provider.OpenableColumns.DISPLAY_NAME
            val size=android.provider.OpenableColumns.SIZE
            val info=try {
                contentResolver.query(uri,arrayOf(name,size),null,null,null)?.use { cursor ->
                    if(cursor.moveToFirst()) {
                        val fileName=cursor.getString(0) ?: "Bilinmiyor"
                        val fileSize=if(cursor.isNull(1)) "Bilinmiyor" else "${cursor.getLong(1)} bayt"
                        "Dosya: $fileName\\nBoyut: $fileSize"
                    } else "Dosya bilgisi bulunamadı"
                } ?: "Dosya bilgisi bulunamadı"
            } catch(e:Exception) { "Dosya bilgisi okunamadı" }
            content.addView(label(info,17f))
            content.addView(label("Bu sürümde otomatik silme yoktur. Silme işlemi yapılmadı.",16f,cyan))
            back()
            return
        }
        screen("APK Dosyası İnceleme")
        content.addView(label("Dosya okunuyor ve SHA-256 hesaplanıyor...",17f))
        Thread {
            val result=try {
                val digest=MessageDigest.getInstance("SHA-256")
                var count=0L
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Dosya açılamadı" }
                    val buffer=ByteArray(32768)
                    while(true) {
                        val n=input.read(buffer)
                        if(n<0) break
                        digest.update(buffer,0,n)
                        count+=n
                    }
                }
                val hash=digest.digest().joinToString("") { "%02x".format(it) }
                "Dosya boyutu: $count bayt\\nSHA-256: $hash\\n\\nBu işlem yalnızca dosyanın parmak izini hesaplar; virüs tespiti yapmaz."
            } catch(e:Exception) { "Dosya incelenemedi: ${e.message}" }
            runOnUiThread {
                screen("APK Dosyası İnceleme")
                content.addView(label(result,15f))
                back()
            }
        }.start()
    }
    private fun ads() { screen("Reklam Kalkanı"); content.addView(label("Reklam engelleme motoru henüz etkin değil. Sistem reklamlarını kapatma ve özel DNS ayarlarını açabilirsiniz.",16f))
        button("Özel DNS ayarlarını aç") { try { startActivity(Intent("android.settings.PRIVATE_DNS_SETTINGS")) } catch(e:Exception) { startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)) } }; back() }
    private fun charge() { screen("Şarj Kontrolü"); val b=registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp=b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1) ?: -1
        val status=b?.getIntExtra(BatteryManager.EXTRA_STATUS,-1) ?: -1
        content.addView(label("Pil sıcaklığı: ${if(temp>=0) "${temp/10.0} °C" else "Bilinmiyor"}",22f,cyan))
        content.addView(label("Şarj durumu: ${when(status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Şarj oluyor"
            BatteryManager.BATTERY_STATUS_FULL -> "Tam dolu"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Pil kullanılıyor"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Şarj olmuyor"
            else -> "Bilinmiyor"
        }}",18f))
        val health=b?.getIntExtra(BatteryManager.EXTRA_HEALTH,-1) ?: -1
        val healthText=when(health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "İyi"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Aşırı sıcak"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Arızalı"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Aşırı voltaj"
            BatteryManager.BATTERY_HEALTH_COLD -> "Çok soğuk"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Belirtilmeyen hata"
            else -> "Bilinmiyor"
        }
        content.addView(label("Sistemin bildirdiği pil durumu: $healthText",17f))
        val plugged=b?.getIntExtra(BatteryManager.EXTRA_PLUGGED,0) ?: 0
        val source=when(plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Priz adaptörü"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Kablosuz"
            else -> "Bağlı değil / bilinmiyor"
        }
        content.addView(label("Güç kaynağı: $source",17f))
        content.addView(label("Standart / Hızlı / Turbo şarj modları sistem izni olmadan değiştirilemez. Bu sürüm donanım modunu değiştirmez.",16f)); back() }
    private fun about() { screen("Turkuaz Shield 0.1.0"); content.addView(label("İlk kaynak kodu prototipi. Henüz antivirüs, VPN reklam filtreleme veya otomatik temizlik motoru içermez.",16f)); back() }
    @Deprecated("System back navigation") override fun onBackPressed() { home() }
}