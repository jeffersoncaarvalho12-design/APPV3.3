<?php
require_once __DIR__ . '/bootstrap.php';
require_method('POST');
auth_required();
$data = json_input();
$product_id = (int)($data['product_id'] ?? 0);
$serials_raw = trim($data['serials'] ?? '');
$location = trim($data['location'] ?? '');
if ($product_id <= 0 || $serials_raw === '') respond(["status"=>"error","message"=>"Produto e seriais são obrigatórios"],400);
$lines = preg_split('/
||
/', $serials_raw); $lines = array_values(array_filter(array_map('trim',$lines)));
$ok = 0; $fail = [];
foreach ($lines as $line) {
  try {
    $mac = strlen(preg_replace('/[^0-9A-F]/i','',$line)) === 12 ? strtoupper(implode(':', str_split(strtoupper(preg_replace('/[^0-9A-F]/i','',$line)),2))) : null;
    $serial_to_save = $mac ? 'MAC-' . str_replace(':','',$mac) : $line;
    $pdo->prepare("INSERT INTO items (product_id, serial_number, mac_address, status, location) VALUES (?,?,?,?,?)")->execute([$product_id,$serial_to_save,$mac,'IN_STOCK',$location ?: null]);
    $item_id = (int)$pdo->lastInsertId();
    $pdo->prepare("INSERT INTO movements (item_id, movement) VALUES (?,'IN')")->execute([$item_id]);
    $ok++;
  } catch (Throwable $e) { $fail[] = $line; }
}
respond(["status"=>"success","message"=>"Entrada em lote processada","data"=>["ok_count"=>$ok,"fail_lines"=>$fail]]);
