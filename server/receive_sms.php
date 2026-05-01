<?php
/**
 * BankPrep – bKash SMS Receiver Endpoint
 * Place at: /api/sms/receive  (or any URL you configure in the app)
 *
 * Method: POST
 * Content-Type: application/json
 */

header('Content-Type: application/json');

// ── Security: optional secret key ─────────────────────────────────────────
$expectedKey = defined('SMS_FORWARDER_SECRET') ? SMS_FORWARDER_SECRET : '';
if (!empty($expectedKey)) {
    $receivedKey = $_SERVER['HTTP_X_SECRET_KEY'] ?? '';
    if ($receivedKey !== $expectedKey) {
        http_response_code(403);
        echo json_encode(['error' => 'Unauthorized']);
        exit;
    }
}

// ── Parse JSON body ────────────────────────────────────────────────────────
$raw  = file_get_contents('php://input');
$body = json_decode($raw, true);

if (!$body || empty($body['message'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing message field']);
    exit;
}

$message   = trim($body['message']);
$sim       = $body['sim']       ?? 'Unknown SIM';
$timestamp = $body['timestamp'] ?? (time() * 1000);
$source    = $body['source']    ?? 'unknown';

// ── Parse bKash SMS ────────────────────────────────────────────────────────
$parsed = parseBkashMessage($message);

// ── Store to database (adapt to your DB class) ────────────────────────────
// Example using PDO:
/*
$stmt = $pdo->prepare("
    INSERT INTO sms_payments
        (raw_message, sim_slot, amount, sender_number, trx_id, balance, ref, received_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
");
$stmt->execute([
    $message,
    $sim,
    $parsed['amount'],
    $parsed['sender'],
    $parsed['trx_id'],
    $parsed['balance'],
    $parsed['ref'],
]);
$insertId = $pdo->lastInsertId();
*/

// ── Auto-verify matching payment ──────────────────────────────────────────
// If you have a payments table, match by TrxID and mark as verified:
/*
if (!empty($parsed['trx_id'])) {
    $pdo->prepare("
        UPDATE payments SET status='verified', verified_at=NOW()
        WHERE trx_id = ? AND status='pending'
    ")->execute([$parsed['trx_id']]);
}
*/

// ── Response ──────────────────────────────────────────────────────────────
echo json_encode([
    'status'  => 'ok',
    'parsed'  => $parsed,
    'sim'     => $sim,
]);

// ── Parser function ────────────────────────────────────────────────────────
function parseBkashMessage(string $msg): array {
    $result = [
        'amount'  => null,
        'sender'  => null,
        'ref'     => null,
        'fee'     => null,
        'balance' => null,
        'trx_id'  => null,
        'date'    => null,
        'time'    => null,
    ];

    // Amount: "Tk 1.00" or "Tk 1,000.00"
    if (preg_match('/received\s+Tk\s+([\d,]+\.?\d*)/i', $msg, $m))
        $result['amount'] = str_replace(',', '', $m[1]);

    // Sender phone
    if (preg_match('/from\s+(01[\d]{9})/i', $msg, $m))
        $result['sender'] = $m[1];

    // Ref
    if (preg_match('/Ref\s+([^\.\n]+)/i', $msg, $m))
        $result['ref'] = trim($m[1]);

    // Fee
    if (preg_match('/Fee\s+Tk\s+([\d,]+\.?\d*)/i', $msg, $m))
        $result['fee'] = str_replace(',', '', $m[1]);

    // Balance
    if (preg_match('/Balance\s+Tk\s+([\d,]+\.?\d*)/i', $msg, $m))
        $result['balance'] = str_replace(',', '', $m[1]);

    // TrxID
    if (preg_match('/TrxID\s+([A-Z0-9]+)/i', $msg, $m))
        $result['trx_id'] = $m[1];

    // Date & time: "13/04/2026 11:17"
    if (preg_match('/at\s+(\d{2}\/\d{2}\/\d{4})\s+(\d{2}:\d{2})/i', $msg, $m)) {
        $result['date'] = $m[1];
        $result['time'] = $m[2];
    }

    return $result;
}
