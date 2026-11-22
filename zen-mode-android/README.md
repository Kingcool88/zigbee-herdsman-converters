# Zen Mode (Android)

En app som låser mobilen under ett zen-pass: inga notiser och ingen app-användning förrän timern har gått ut.

## Funktioner

| Funktion | Hur den fungerar |
|---|---|
| **Manuellt pass** | Välj 5 min – 12 h och tryck Starta. Passet kan inte avbrytas. |
| **Schema** | T.ex. mån–fre 09:00, 120 min. Pass får gå över midnatt. |
| **Geozon** | Zen startar när du kommer in i en zon och slutar när du lämnar den. |
| **Timer** | Låsskärmen visar nedräkning och sluttid. Nedräkningen syns också i notisfältet. |
| **Inga notiser** | Androids Stör ej-läge med alla visuella effekter avstängda. Larm (väckarklocka) fungerar fortfarande. |
| **Vissa samtal går igenom** | Egen lista med kontakter, via samtalsfiltrering. Går också att släppa igenom stjärnmärkta kontakter. |
| **Partner-upplåsning** | En liten knapp på låsskärmen skickar ett SMS till partnern. Partnern godkänner i appen eller svarar med koden. Då låses mobilen upp i 5 min och låses sedan igen. |
| **Omstart** | Tillståndet ligger i Direct Boot-lagring. Passet fortsätter direkt efter omstart, redan innan PIN är inmatad. Larm och geozoner registreras om. |

### Hur låset fungerar

Android låter inte en vanlig app låsa telefonen helt. Appen har därför två nivåer:

1. **Tillgänglighetstjänst** (standard). Om en annan app hamnar i förgrunden skickas man direkt tillbaka till zen-skärmen. Notisfältet stängs. Telefon och nödsamtal är alltid tillåtna. Inställningar är blockerade även under pausen, så att man inte kan stänga av tjänsten.
2. **Kiosklås / Lock Task Mode** (starkast, valfritt). Kräver att appen är *device owner*. Då går det varken att byta app, nå hemknappen eller starta i felsäkert läge.

Under ett pass (även under pausen) går det inte att ändra inställningar som partner, schema och zoner i appen. Annars kunde man till exempel byta partnernummer till sitt eget.

### Partner-upplåsning (SMS-protokoll)

```
Låst mobil  → partner:  "ZENMODE-REQ 482913: Anna ber om att låsa upp ... Svara med koden 482913 ..."
Partner     → låst mobil: "ZENMODE-OK 482913"   (knappen Godkänn i appen)
                       eller bara "482913"   (svar från vanliga SMS-appen, partnern behöver inte appen)
                       eller "ZENMODE-NO 482913" (Neka)
```

Säkerhetsregler:
- Svaret måste komma från det sparade partnernumret.
- Koden är 6 slumpade siffror och gäller i 15 min.
- Man kan be om paus högst en gång per minut.

## Bygga

Öppna mappen `zen-mode-android/` i Android Studio (Ladybug eller senare) och tryck Run. Från kommandoraden:

```bash
cd zen-mode-android
./gradlew :core:test        # enhetstester för schemalogik, nummer-matchning och SMS-protokoll
./gradlew :app:installDebug
```

Krav: JDK 17+, Android SDK 35, minSdk 29 (Android 10).

## Testa i emulatorn

Tips: skapa en AVD **med Google Play/Google APIs** (krävs för geozoner) men **logga inte in** med något Google-konto om du vill testa kiosklåset.

### 1. Snabb setup via adb (i stället för att klicka igenom behörigheterna)

```bash
PKG=se.zenmode.app
adb shell cmd notification allow_dnd $PKG
adb shell settings put secure enabled_accessibility_services $PKG/$PKG.service.ZenAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
adb shell pm grant $PKG android.permission.SEND_SMS
adb shell pm grant $PKG android.permission.RECEIVE_SMS
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_BACKGROUND_LOCATION
adb shell dumpsys deviceidle whitelist +$PKG
# Valfritt kiosklås:
adb shell dpm set-device-owner $PKG/.receiver.ZenDeviceAdminReceiver
```

Samtalsfiltrering måste godkännas i appen (Inställn. → Samtalsfiltrering → Fixa).

### 2. Testfall

| Test | Gör så här | Förväntat |
|---|---|---|
| Timer | Starta 5 min | Zen-skärmen med nedräkning. Hem/tillbaka/appbyte tar dig tillbaka. Efter 5 min försvinner låset och Stör ej återställs. |
| Notiser | Under zen: `adb shell cmd notification post -t "Test" tag "Hej"` | Ingen notis syns eller hörs. |
| Omstart | Starta 10 min, sedan `adb reboot` | Efter uppstart: zen-skärmen kommer tillbaka och timern fortsätter där den var. |
| Partner (en emulator) | Lägg partnernummer `15555215556`. Tryck *Be om paus*. Läs koden med `adb logcat -s ZenUnlock` (loggas bara i debug-bygge). Svara sedan som partnern: `adb emu sms send 15555215556 <koden>` | Låset försvinner i 5 min och kommer sedan tillbaka. |
| Partner (två emulatorer) | Starta emulator 5554 och 5556 med appen på båda. På 5554: partner = `15555215556`. Tryck *Be om paus*. | 5556 får en notis med Godkänn/Neka. Godkänn låser upp 5554 i 5 min. |
| Fel avsändare | `adb emu sms send 15555215557 <koden>` | Ingenting händer. |
| Tillåtet samtal | Lägg till `15555215556` under Kontakter. Under zen: `adb emu gsm call 15555215556` | Det ringer. Med ett annat nummer är det tyst. |
| Schema | Skapa ett schema som startar om 2 min | Zen startar av sig självt. |
| Geozon | Skapa en zon (t.ex. 59.3293, 18.0686, 200 m). Kör `adb emu geo fix 18.0686 59.3293` (OBS: longitud först). Flytta sedan bort: `adb emu geo fix 18.2 59.4` | Zen startar vid ankomst och slutar vid utresa. Geozoner kan ta 1–2 min i emulatorn. |

### 3. Nödutgång under utveckling

Om du låser in dig och vill avbryta ett test:

```bash
adb shell pm clear se.zenmode.app          # rensar zen-tillståndet
adb shell cmd notification set_dnd off      # stänger Stör ej
```

Ett kiosklås (device owner) tas bort med knappen under *Inställn. → Kiosklås* när inget pass är aktivt.

## Kända begränsningar

- **Felsäkert läge** (safe mode) stänger av alla tredjepartsappar. Det blockeras bara med kiosklåset.
- **Android 13+ vid sideload:** tillgänglighetstjänsten kan vara gråmarkerad. Lös det via *Appinfo → ⋮ → Tillåt begränsade inställningar*.
- **Tillåtna samtal:** appen stänger av Stör ej i 60 s när en tillåten kontakt ringer. Under den minuten kan andra notiser slinka igenom.
- **SMS-behörigheter** är begränsade på Google Play. För Play-publicering behöver partner-upplåsningen bytas till push (t.ex. Firebase Cloud Messaging).
- **Telefonappen** är alltid tillåten (för nödsamtal och inkommande samtal).

## Struktur

```
core/   Ren Kotlin, enhetstestad: modeller, ScheduleCalc, PhoneMatch, UnlockSms
app/    Android:
  core/      ZenController (start/stopp/paus/återställning), DndManager, AlarmScheduler, Kiosk, UnlockRequester
  data/      ZenStore (Direct Boot-lagring)
  service/   ZenAccessibilityService (låset), ZenForegroundService, ZenCallScreeningService
  receiver/  Boot, Alarm, SMS, Partner-knappar, Geozon, DeviceAdmin
  geo/       GeofenceManager
  ui/        MainActivity (flikar), LockActivity (zen-skärmen)
```
