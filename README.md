# bKash SMS Forwarder – Android App

Monitors incoming bKash SMS on your Android phone and forwards them to your BankPrep server via HTTP POST.

---

## Features
- ✅ Listens for real bKash SMS (sender filter: "bKash")
- ✅ **SIM selection**: Both SIMs / SIM 1 only / SIM 2 only
- ✅ Shows detected carrier name for each SIM slot
- ✅ Forwards JSON payload to your server URL
- ✅ Optional secret key header (X-Secret-Key)
- ✅ Foreground service – survives screen off & battery optimization
- ✅ Auto-starts on device reboot
- ✅ Test send button (sends sample bKash message)
- ✅ Full forward log with timestamps
- ✅ Ocean green theme matching BankPrep design

---

## JSON Payload Sent to Your Server

```json
{
  "message": "You have received Tk 1.00 from 01729752392. Ref nazim. Fee Tk 0.00. Balance Tk 6,908.21. TrxID DDD14QS409 at 13/04/2026 11:17",
  "sim": "SIM 1 (Grameenphone)",
  "timestamp": 1713000000000,
  "source": "bkash_sms_forwarder"
}
```

Headers sent:
- `Content-Type: application/json`
- `X-App-Source: BkashSMSForwarder/1.0`
- `X-Secret-Key: <your key>` (if configured)

---

## Build Instructions (Android Studio)

### Requirements
- Android Studio Hedgehog (2023.1) or newer
- JDK 17+
- Android SDK 34

### Steps
1. Open Android Studio
2. File → Open → select the `BkashSMSForwarder` folder
3. Wait for Gradle sync to complete
4. Build → Generate Signed APK  
   OR press the ▶ Run button to install directly on device

### Build via Command Line
```bash
cd BkashSMSForwarder
chmod +x gradlew
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

---

## Server-side PHP (BankPrep Integration)

Add this endpoint to your BankPrep server:

```php
<?php
// POST /api/sms/receive
header('Content-Type: application/json');

// Optional: verify secret key
$secretKey = $_SERVER['HTTP_X_SECRET_KEY'] ?? '';
if (!empty(getenv('SMS_SECRET')) && $secretKey !== getenv('SMS_SECRET')) {
    http_response_code(403);
    echo json_encode(['error' => 'Unauthorized']);
    exit;
}

$body = json_decode(file_get_contents('php://input'), true);
$message   = $body['message']   ?? '';
$sim       = $body['sim']       ?? '';
$timestamp = $body['timestamp'] ?? time() * 1000;

if (empty($message)) {
    http_response_code(400);
    echo json_encode(['error' => 'No message']);
    exit;
}

// Parse bKash message and trigger your verification logic here
// e.g. extract TrxID, amount, sender, balance...

// Log to database
// $db->query("INSERT INTO sms_log (message, sim, received_at) VALUES (?, ?, NOW())", [$message, $sim]);

echo json_encode(['status' => 'ok', 'received' => true]);
```

---

## Permissions Required
| Permission | Reason |
|---|---|
| RECEIVE_SMS | Listen for incoming SMS |
| READ_SMS | Read SMS content |
| READ_PHONE_STATE | Detect which SIM slot received SMS |
| INTERNET | Forward to server |
| FOREGROUND_SERVICE | Run in background reliably |
| RECEIVE_BOOT_COMPLETED | Auto-start on reboot |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | Stay alive in background |

---

## Settings
| Setting | Description |
|---|---|
| Server URL | Your BankPrep endpoint (required) |
| Secret Key | Sent as X-Secret-Key header |
| SMS Sender Filter | Default: "bKash" |
| SIM Preference | Both / SIM 1 only / SIM 2 only |
| Verify SSL | Enable/disable SSL cert verification |
