<?php
require_once __DIR__ . '/bootstrap.php';
require_method('POST');
auth_required();
$data = json_input();
$product_id = (int)($data['product_id'] ?? 0);
$quantity = (int)($data['quantity'] ?? 0);
$location = trim($data['location'] ?? '');
if ($product_id <= 0 || $quantity <= 0) respond(["status"=>"error","message"=>"Produto e quantidade são obrigatórios"],400);
function gen_serial($product_id,$index){ return 'SEM-SERIAL-' . $product_id . '-' . date('YmdHis') . '-' . $index . '-' . strtoupper(bin2hex(random_bytes(3))); }
for ($i=1; $i <= $quantity; $i++) {
  $serial = gen_serial($product_id,$i);
  $pdo->prepare("INSERT INTO items (product_id, serial_number, mac_address, status, location) VALUES (?,?,?,?,?)")->execute([$product_id,$serial,null,'IN_STOCK',$location ?: null]);
  $item_id = (int)$pdo->lastInsertId();
  $pdo->prepare("INSERT INTO movements (item_id, movement) VALUES (?,'IN')")->execute([$item_id]);
}
respond(["status"=>"success","message"=>"Entrada por modelo registrada com sucesso","data"=>["quantity"=>$quantity]]);
