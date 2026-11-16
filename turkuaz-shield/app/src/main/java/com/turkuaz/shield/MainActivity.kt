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
import android.graphics.RectF

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
    private fun dashboardRing(): View = object : View(this) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onMeasure(w:Int,h:Int) { setMeasuredDimension(MeasureSpec.getSize(w),dp(180)) }
        override fun onDraw(c:Canvas) {
            super.onDraw(c)
            val x=width/2f; val y=dp(88).toFloat(); val radius=dp(69).toFloat()
            val enabled=getPreferences(MODE_PRIVATE).getBoolean("audit_enabled",false)
            p.style=Paint.Style.STROKE; p.strokeWidth=dp(12).toFloat()
            p.strokeCap=Paint.Cap.ROUND; p.color=Color.rgb(20,70,93)
            c.drawCircle(x,y,radius,p)
            p.color=if(enabled) cyan else Color.rgb(123,147,163)
            c.drawArc(RectF(x-radius,y-radius,x+radius,y+radius),-90f,if(enabled) 330f else 90f,false,p)
            p.style=Paint.Style.FILL; p.textAlign=Paint.Align.CENTER
            p.typeface=Typeface.DEFAULT_BOLD; p.textSize=dp(27).toFloat()
            c.drawText(if(enabled) "AÇIK" else "KAPALI",x,y-dp(2),p)
            p.color=Color.WHITE; p.textSize=dp(12).toFloat()
            c.drawText("Güvenlik denetimi",x,y+dp(22),p)
        }
    }.apply {
        setOnClickListener { toggleAudit() }
        isClickable=true
        contentDescription="Güvenlik denetimini aç veya kapat"
    }
    private fun toggleAudit() {
        val prefs=getPreferences(MODE_PRIVATE)
        val enabled=!prefs.getBoolean("audit_enabled",false)
        prefs.edit().putBoolean("audit_enabled",enabled).apply()
        if(enabled) runAudit() else home()
    }
    private fun runAudit() {
        screen("Güvenlik Denetimi")
        content.addView(label("Yüklü uygulamaların hassas izinleri kontrol ediliyor...",17f,cyan))
        Thread {
            val result=try {
                val sensitive=setOf("CAMERA","RECORD_AUDIO","ACCESS_FINE_LOCATION","READ_CONTACTS",
                    "READ_SMS","SEND_SMS","READ_CALL_LOG","ACCESS_BACKGROUND_LOCATION")
                val apps=packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM)==0 }
                var withSensitive=0
                var grantedCount=0
                apps.forEach { app ->
                    val info=try {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(app.packageName,PackageManager.GET_PERMISSIONS)
                    } catch(e:Exception) { null }
                    val names=info?.requestedPermissions ?: emptyArray<String>()
                    val flags=info?.requestedPermissionsFlags
                    var found=false
                    names.forEachIndexed { index,name ->
                        if(name.substringAfterLast(".") in sensitive) {
                            found=true
                            if(flags?.getOrNull(index)?.let {
                                (it and PackageInfo.REQUESTED_PERMISSION_GRANTED)!=0
                            } == true) grantedCount++
                        }
                    }
                    if(found) withSensitive++
                }
                "İncelenen uygulama: ${apps.size}\nHassas izin isteyen uygulama: $withSensitive\nVerilmiş hassas izin: $grantedCount"
            } catch(e:Exception) { "Denetim tamamlanamadı: ${e.message}" }
            runOnUiThread {
                if(!isFinishing && !isDestroyed) {
                    getPreferences(MODE_PRIVATE).edit()
                        .putString("last_audit",result)
                        .putLong("last_audit_time",System.currentTimeMillis()).apply()
                    screen("Denetim Sonucu")
                    content.addView(label(result,19f,cyan))
                    content.addView(label("Bu denetim uygulama izinlerini kontrol eder. Antivirüs veya sürekli arka plan koruması değildir.",14f))
                    button("Uygulamaları tek tek incele") { apps() }
                    button("Denetimi yeniden çalıştır") { runAudit() }
                    button("Denetimi kapat") { toggleAudit() }
                    back()
                }
            }
        }.start()
    }
    private fun home() {
        screen("TURKUAZ SHIELD")
        content.addView(label("AKILLI TELEFON KORUMASI",12f,cyan).apply {
            gravity=Gravity.CENTER
            letterSpacing=0.15f
        })
        content.addView(shieldMark())
        content.addView(dashboardRing())
        val auditPrefs=getPreferences(MODE_PRIVATE)
        val enabled=auditPrefs.getBoolean("audit_enabled",false)
        content.addView(label(if(enabled) "DENETİM AÇIK • Yeniden taramak için dokun" else "DENETİM KAPALI • Başlatmak için dokun",14f,cyan).apply {
            gravity=Gravity.CENTER
            setOnClickListener { if(enabled) runAudit() else toggleAudit() }
        })
        if(enabled) {
            val previous=auditPrefs.getString("last_audit","Henüz denetim yapılmadı.") ?: ""
            content.addView(label(previous,13f,Color.rgb(171,204,217)).apply {
                gravity=Gravity.CENTER
            })
        }

        val battery=registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level=battery?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1) ?: -1
        val scale=battery?.getIntExtra(BatteryManager.EXTRA_SCALE,100) ?: 100
        val percent=if(level>=0 && scale>0) level*100/scale else -1
        val stat=StatFs(Environment.getDataDirectory().path)
        val total=stat.totalBytes/1073741824.0
        val free=stat.availableBytes/1073741824.0
        val status=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            background=shape(panel)
            setPadding(dp(6),dp(10),dp(6),dp(10))
        }
        val batteryText=label("⚡ PİL\n${if(percent>=0) "$percent%" else "—"}",17f,cyan).apply {
            gravity=Gravity.CENTER; setTypeface(null,Typeface.BOLD)
        }
        val storageText=label("◉ DEPOLAMA\n${String.format(Locale.US,"%.1f",total-free)} / ${String.format(Locale.US,"%.1f",total)} GB",15f,Color.WHITE).apply {
            gravity=Gravity.CENTER
        }
        status.addView(batteryText,LinearLayout.LayoutParams(0,-2,1f))
        status.addView(storageText,LinearLayout.LayoutParams(0,-2,1.3f))
        content.addView(status,LinearLayout.LayoutParams(-1,-2).apply {
            setMargins(0,dp(8),0,dp(14))
        })
        content.addView(label("KORUMA ARAÇLARI",15f,cyan).apply {
            setTypeface(null,Typeface.BOLD)
        })
        val items=listOf(
            Triple("✦","Güvenlik\nKontrolü",0),
            Triple("▣","APK\nİncele",1),
            Triple("✧","Güvenli\nTemizlik",2),
            Triple("◈","Reklam\nKalkanı",3),
            Triple("⚡","Şarj\nKontrolü",4),
            Triple("◷","Güncelleme\nArtıkları",5),
            Triple("⚙","Güvenlik\nAyarları",6),
            Triple("ⓘ","Hakkında",7)
        )
        val accents=intArrayOf(
            Color.rgb(34,224,239),Color.rgb(117,174,255),
            Color.rgb(54,223,164),Color.rgb(255,185,83),
            Color.rgb(255,207,83),Color.rgb(179,148,255),
            Color.rgb(121,207,240),Color.rgb(183,201,215)
        )
        items.chunked(2).forEach { pair ->
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            pair.forEach { (symbol,name,index) ->
                val tile=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL
                    gravity=Gravity.CENTER
                    background=GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(Color.rgb(16,53,81),Color.rgb(7,28,49))
                    ).apply {
                        cornerRadius=dp(18).toFloat()
                        setStroke(dp(1),Color.rgb(32,91,114))
                    }
                    setPadding(dp(6),dp(15),dp(6),dp(15))
                    isClickable=true
                    isFocusable=true
                    contentDescription=name.replace("\n"," ")
                }
                tile.addView(label(symbol,31f,accents[index]).apply {
                    gravity=Gravity.CENTER
                    setTypeface(null,Typeface.BOLD)
                })
                tile.addView(label(name,14f,Color.WHITE).apply {
                    gravity=Gravity.CENTER
                    textAlignment=View.TEXT_ALIGNMENT_CENTER
                    setTypeface(null,Typeface.BOLD)
                    minHeight=dp(48)
                })
                tile.setOnClickListener {
                    when(index) {
                        0 -> apps()
                        1 -> chooseApk()
                        2 -> storage()
                        3 -> ads()
                        4 -> charge()
                        5 -> leftovers()
                        6 -> securitySettings()
                        else -> about()
                    }
                }
                row.addView(tile,LinearLayout.LayoutParams(0,dp(132),1f).apply {
                    setMargins(dp(4),dp(4),dp(4),dp(4))
                })
            }
            content.addView(row)
        }
        content.addView(label("İzin incelemesi antivirüs taraması değildir.",12f,Color.rgb(158,182,197)).apply {
            gravity=Gravity.CENTER
        })
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
    private fun apps() {
        screen("Uygulama Güvenlik Kontrolü")
        val packages=packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM)==0 }
            .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }
        content.addView(label("Bulunan kullanıcı uygulamaları: ${packages.size}",17f,cyan))
        content.addView(label("Bu inceleme uygulamaların bildirdiği izinleri gösterir. Zararlı yazılım teşhisi değildir.",14f))
        val sensitive=setOf("CAMERA","RECORD_AUDIO","ACCESS_FINE_LOCATION","READ_CONTACTS",
            "READ_SMS","SEND_SMS","READ_CALL_LOG","READ_PHONE_STATE","ACCESS_BACKGROUND_LOCATION")
        var flagged=0
        val records=packages.map { app ->
            val pkg=try {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(app.packageName,PackageManager.GET_PERMISSIONS)
            } catch(e:Exception) { null }
            val names=pkg?.requestedPermissions?.toList() ?: emptyList()
            val flags=pkg?.requestedPermissionsFlags
            val relevant=names.filter { sensitive.contains(it.substringAfterLast(".")) }
            if(relevant.isNotEmpty()) flagged++
            Triple(app,Pair(names,flags),relevant)
        }
        content.addView(label("Hassas izin isteyen uygulamalar: ${flagged}",15f,cyan))
        records.forEach { (app,permissionData,relevant) ->
            val (names,flags)=permissionData
            button(packageManager.getApplicationLabel(app).toString(),
                "${names.size} izin • ${relevant.size} hassas izin isteği") {
                screen(packageManager.getApplicationLabel(app).toString())
                content.addView(label("Paket: ${app.packageName}",13f,cyan))
                if(relevant.isNotEmpty()) {
                    content.addView(label("Hassas izin istekleri: ${relevant.joinToString(", ") { it.substringAfterLast(".") }}",14f,cyan))
                }
                if(names.isEmpty()) content.addView(label("Bildirilen izin yok.",14f))
                names.forEachIndexed { index,name ->
                    val granted=flags?.getOrNull(index)?.let {
                        (it and PackageInfo.REQUESTED_PERMISSION_GRANTED)!=0
                    } ?: false
                    content.addView(label("${if(granted) "✓ Verilmiş" else "— İstenmiş"} • ${name.substringAfterLast(".")}",13f))
                }
                button("Uygulamanın izin ayarlarını aç") {
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:${app.packageName}")))
                    } catch(e:Exception) {
                        Toast.makeText(this,"Uygulama ayarları açılamadı",Toast.LENGTH_SHORT).show()
                    }
                }
                back()
            }
        }
        back()
    }
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
                val details=try {
                    val temp=java.io.File.createTempFile("shield_",".apk",cacheDir)
                    try {
                        contentResolver.openInputStream(uri).use { stream ->
                            requireNotNull(stream) { "Dosya okunamadı" }
                            temp.outputStream().use { stream.copyTo(it) }
                        }
                        @Suppress("DEPRECATION")
                        val info=packageManager.getPackageArchiveInfo(temp.absolutePath,PackageManager.GET_PERMISSIONS)
                        if(info==null) "\nGeçerli APK paketi tanınmadı."
                        else {
                            val perms=info.requestedPermissions?.toList() ?: emptyList()
                            "\nPaket: ${info.packageName}\nİstenen izinler (${perms.size}):\n" +
                                perms.joinToString("\n") { it.substringAfterLast(".") }
                        }
                    } finally { temp.delete() }
                } catch(e:Exception) { "\nAPK izinleri okunamadı: ${e.message}" }
                "Dosya boyutu: $count bayt\nSHA-256: $hash\n$details\n\nBu analiz virüs tespiti yapmaz."
            } catch(e:Exception) { "Dosya incelenemedi: ${e.message}" }
            runOnUiThread {
                screen("APK Dosyası İnceleme")
                content.addView(label(result,15f))
                back()
            }
        }.start()
    }
    private fun ads() {
        screen("Reklam Kalkanı")
        val mode=try {
            android.provider.Settings.Global.getString(contentResolver,"private_dns_mode")
        } catch(e:Exception) { null }
        val hostname=try {
            android.provider.Settings.Global.getString(contentResolver,"private_dns_specifier")
        } catch(e:Exception) { null }
        val status=when(mode) {
            "hostname" -> "Özel DNS etkin: ${hostname ?: "Sunucu bilinmiyor"}"
            "opportunistic" -> "Otomatik DNS modu"
            "off" -> "Özel DNS kapalı"
            else -> "Özel DNS durumu okunamadı"
        }
        content.addView(label(status,18f,cyan))
        content.addView(label("DNS tabanlı filtreleme için Android Özel DNS ayarından bir filtreleme hizmeti seçebilirsin. DNS filtreleme her reklamı engellemez.",15f))
        button("Özel DNS ayarlarını aç","DNS hizmetini Android ayarlarında yönet") {
            try { startActivity(Intent("android.settings.PRIVATE_DNS_SETTINGS")) }
            catch(e:Exception) { startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)) }
        }
        button("Durumu yenile") { ads() }
        content.addView(label("Bu uygulama şu anda VPN veya kendi reklam filtreleme motorunu çalıştırmaz.",13f))
        back()
    }
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