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
import android.view.View
import android.view.WindowManager
import android.util.TypedValue
import android.widget.*
import java.util.Locale
import java.security.MessageDigest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.app.AlertDialog
import android.provider.DocumentsContract
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class MainActivity : Activity() {
    private val bg = Color.rgb(3, 16, 31)
    private val panel = Color.rgb(9, 38, 64)
    private val cyan = Color.rgb(34, 224, 239)
    private lateinit var content: LinearLayout
    private val pickApk = 2101
    private val pickCleanup = 2102
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); home() }
    private fun dp(v:Int):Int=(v*resources.displayMetrics.density+0.5f).toInt()
    private fun shape(color:Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius=dp(15).toFloat(); setStroke(dp(1),cyan)
    }
    private fun label(s:String,size:Float=16f,color:Int=Color.WHITE):TextView=TextView(this).apply {
        text=s; textSize=size; setTextColor(color); gravity=Gravity.CENTER_VERTICAL
        setPadding(dp(12),dp(8),dp(12),dp(8))
    }
    private fun screen(title:String) {
        window.statusBarColor=bg
        window.navigationBarColor=bg
        window.decorView.systemUiVisibility=0
        val sc=ScrollView(this).apply {
            setBackgroundColor(bg); isFillViewport=true; clipToPadding=false
        }
        content=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(8),dp(16),dp(28))
        }
        sc.addView(content)
        setContentView(sc)
        val heading=label(title,23f,cyan).apply {
            setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER
            setPadding(dp(4),dp(8),dp(4),dp(8))
        }
        content.addView(heading)
    }
    private fun button(title:String,detail:String="",onClick:()->Unit) {
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=shape(panel)
            setPadding(dp(14),dp(9),dp(14),dp(9))
            isClickable=true; isFocusable=true
        }
        val heading=label(title,17f).apply {
            setTypeface(null,Typeface.BOLD)
            gravity=Gravity.CENTER_VERTICAL
            setPadding(0,0,0,0)
            maxLines=1
            ellipsize=android.text.TextUtils.TruncateAt.END
        }
        box.addView(heading)
        if(detail.isNotEmpty()) box.addView(label(detail,12f,Color.rgb(171,204,217)).apply {
            setPadding(0,dp(2),0,0); maxLines=2
        })
        val lp=LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,dp(5),0,dp(5)) }
        content.addView(box,lp)
        box.setOnClickListener { onClick() }
    }
    private fun shieldMark(): View = object : View(this) {
        private val pen=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onMeasure(w:Int,h:Int) { setMeasuredDimension(MeasureSpec.getSize(w),dp(104)) }
        override fun onDraw(c:Canvas) {
            super.onDraw(c)
            val x=width/2f; val y=dp(7).toFloat()
            val p=Path().apply {
                moveTo(x,y); lineTo(x+dp(42),y+dp(15))
                lineTo(x+dp(36),y+dp(62)); quadTo(x+dp(26),y+dp(78),x,y+dp(91))
                quadTo(x-dp(26),y+dp(78),x-dp(36),y+dp(62))
                lineTo(x-dp(42),y+dp(15)); close()
            }
            pen.style=Paint.Style.FILL; pen.color=panel; c.drawPath(p,pen)
            pen.style=Paint.Style.STROKE; pen.strokeWidth=dp(3).toFloat()
            pen.color=cyan; c.drawPath(p,pen)
            pen.style=Paint.Style.FILL; pen.typeface=Typeface.DEFAULT_BOLD
            pen.textAlign=Paint.Align.CENTER; pen.textSize=dp(42).toFloat()
            c.drawText("T",x,y+dp(62),pen)
        }
    }
    private fun home() {
        screen("TURKUAZ SHIELD")
        content.addView(shieldMark())
        content.addView(label("Akıllı Telefon Koruması",13f,cyan).apply { gravity=Gravity.CENTER; setPadding(0,0,0,dp(4)) })
        val battery=registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level=battery?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1) ?: -1
        val scale=battery?.getIntExtra(BatteryManager.EXTRA_SCALE,100) ?: 100
        val percent=if(level>=0 && scale>0) level*100/scale else -1
        val stat=StatFs(Environment.getDataDirectory().path)
        val total=stat.totalBytes/1073741824.0; val free=stat.availableBytes/1073741824.0
        content.addView(label("Pil  ${if(percent>=0) "$percent%" else "—"}     •     Depolama  ${String.format(Locale.US,"%.1f",total-free)} / ${String.format(Locale.US,"%.1f",total)} GB",14f).apply { gravity=Gravity.CENTER; setPadding(dp(2),dp(8),dp(2),dp(12)) })
        button("Güvenli Temizlik","Depolama incelemesi") { storage() }
        button("Güncelleme Artıkları","Güncelleme dosyası incelemesi") { leftovers() }
        button("Güvenlik Kontrolü","Yüklü uygulamalar ve izinler") { apps() }
        button("APK Dosyası İncele","SHA-256 parmak izi hesapla") { chooseApk() }
        button("Reklam Kalkanı","Koruma seçenekleri") { ads() }
        button("Güvenlik Ayarları","Cihaz güvenlik seçenekleri") { securitySettings() }
        button("Şarj Kontrolü","Gerçek pil durumu") { charge() }
        button("Hakkında","Sürüm 0.1.0") { about() }
    }
    private fun back() { button("← Ana Ekran") { home() } }
    private fun storage() { screen("Güvenli Temizlik"); val st=StatFs(Environment.getDataDirectory().path)
        content.addView(label("Kullanılabilir alan: ${String.format(Locale.US,"%.2f",st.availableBytes/1073741824.0)} GB"))
        content.addView(label("Bu sürüm otomatik dosya silmez. Dosyalarınız ve hesap oturumlarınız korunur.",16f))
        button("İndirilen dosyaları incele") { chooseCleanup() }
        button("Sistem depolama ayarlarını aç") { try { startActivity(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) } catch(e:Exception) { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } }; back() }
    private fun leftovers() {
        screen("Güncelleme Artıkları")
        content.addView(label("İndirilen güncelleme paketlerini seçip boyutlarını inceleyebilirsin. Sistem korumalı güncelleme alanlarına erişim yoktur.",16f))
        button("Güncelleme paketini seç") { chooseCleanup() }
        button("Sistem depolama ayarları") {
            try { startActivity(Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }
            catch(e:Exception) { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
        }
        back()
    }
    private fun apps() { screen("Uygulama Güvenlik Kontrolü")
        val packages=packageManager.getInstalledApplications(PackageManager.GET_META_DATA).sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
        content.addView(label("Görülebilen uygulama sayısı: ${packages.size}. Kullanıcı uygulamalarına dokunarak izinlerini ve sistem bilgilerini inceleyebilirsin.",15f,cyan))
        packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM)==0 }.take(100).forEach { app ->
            val isSystem=(app.flags and ApplicationInfo.FLAG_SYSTEM)!=0
            val pkg=try { @Suppress("DEPRECATION") packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS) } catch(e:Exception) { null }
            val permissions=pkg?.requestedPermissions?.size ?: 0
            val source=if(isSystem) "Sistem uygulaması" else "Kullanıcı uygulaması"
            button(packageManager.getApplicationLabel(app).toString(), "$source • $permissions izin") {
                screen(packageManager.getApplicationLabel(app).toString())
                content.addView(label("Paket: ${app.packageName}",14f,cyan))
                val names=pkg?.requestedPermissions?.toList() ?: emptyList()
                content.addView(label(if(names.isEmpty()) "Bildirilen izin bulunamadı." else names.joinToString("\\n") { it.substringAfterLast(".") },14f))
                button("Android uygulama bilgilerini aç") {
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:${app.packageName}")))
                    } catch(e:Exception) { Toast.makeText(this,"Ayarlar açılamadı",Toast.LENGTH_SHORT).show() }
                }
                back()
            }
        }
        content.addView(label("İzin sayısı tek başına risk göstergesi değildir. Bu liste antivirüs taraması değildir; zararlı yazılım teşhisi yapılmadı.",15f)); back() }
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
                        "Dosya: $fileName\nBoyut: $fileSize"
                    } else "Dosya bilgisi bulunamadı"
                } ?: "Dosya bilgisi bulunamadı"
            } catch(e:Exception) { "Dosya bilgisi okunamadı" }
            content.addView(label(info,17f))
            content.addView(label("Yalnızca seçtiğin dosyayı silebilirsin. Uygulama başka dosyalara veya hesaplarına dokunmaz.",15f,cyan))
            button("Seçilen dosyayı sil") {
                AlertDialog.Builder(this)
                    .setTitle("Dosyayı sil?")
                    .setMessage("Seçtiğin dosya kalıcı olarak silinebilir. Bu işlem geri alınamayabilir. Devam edilsin mi?")
                    .setNegativeButton("Vazgeç",null)
                    .setPositiveButton("Sil") { _,_ ->
                        val result=try {
                            if(DocumentsContract.isDocumentUri(this,uri) && DocumentsContract.deleteDocument(contentResolver,uri)) "Seçilen dosya silindi."
                            else "Dosya sağlayıcısı silmeye izin vermedi."
                        } catch(e:Exception) { "Silme başarısız: ${e.message ?: "Erişim engellendi"}" }
                        screen("Temizlik Sonucu")
                        content.addView(label(result,17f,cyan))
                        back()
                    }.show()
            }
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
                "Dosya boyutu: $count bayt\nSHA-256: $hash\n\nBu işlem yalnızca dosyanın parmak izini hesaplar; virüs tespiti yapmaz."
            } catch(e:Exception) { "Dosya incelenemedi: ${e.message}" }
            runOnUiThread {
                screen("APK Dosyası İnceleme")
                content.addView(label(result,15f))
                back()
            }
        }.start()
    }
    private fun ads() { screen("Reklam Kalkanı"); content.addView(label("Özel DNS ayarlarından DNS tabanlı reklam filtrelemesi yapılandırılabilir. Bu sürümde uygulama içi VPN filtresi yoktur.",16f))
        button("Özel DNS ayarlarını aç") { try { startActivity(Intent("android.settings.PRIVATE_DNS_SETTINGS")) } catch(e:Exception) { startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)) } }; back() }
    private fun securitySettings() {
        screen("Güvenlik Ayarları")
        content.addView(label("Android sisteminin sunduğu güvenlik seçenekleri. Bu uygulama sistem ayarlarını izinsiz değiştirmez.",16f))
        button("Uygulama güvenliği ayarları") {
            try { startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)) }
            catch(e:Exception) { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
        }
        button("Uygulama izinlerini yönet") {
            try { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS)) }
            catch(e:Exception) { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
        }
        back()
    }
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
        content.addView(label("Standart / Hızlı / Turbo şarj modları sistem izni olmadan değiştirilemez. Bu sürüm donanım modunu değiştirmez.",16f)); button("Pil ayarlarını aç") { try { startActivity(Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)) } catch(e:Exception) { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } }; back() }
    private fun about() { screen("Turkuaz Shield 0.1.0"); content.addView(label("İlk kaynak kodu prototipi. Henüz antivirüs, VPN reklam filtreleme veya otomatik temizlik motoru içermez.",16f)); back() }
    @Deprecated("System back navigation") override fun onBackPressed() { home() }
}