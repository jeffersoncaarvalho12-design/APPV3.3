<?php
require_once __DIR__ . '/bootstrap.php';
require_method('POST');
auth_required();
$data = json_input();
$product_id = (int)($data['product_id'] ?? 0);
$serial = trim($data['serial'] ?? '');
$location = trim($data['location'] ?? '');
if ($product_id <= 0 || $serial === '') respond(["status"=>"error","message"=>"Produto e serial são obrigatórios"],400);
$mac = strlen(preg_replace('/[^0-9A-F]/i','',$serial)) === 12 ? strtoupper(implode(':', str_split(strtoupper(preg_replace('/[^0-9A-F]/i','',$serial)),2))) : null;
$serial_to_save = $mac ? 'MAC-' . str_replace(':','',$mac) : $serial;
$pdo->prepare("INSERT INTO items (product_id, serial_number, mac_address, status, location) VALUES (?,?,?,?,?)")->execute([$product_id,$serial_to_save,$mac,'IN_STOCK',$location ?: null]);
$item_id = (int)$pdo->lastInsertId();
$pdo->prepare("INSERT INTO movements (item_id, movement) VALUES (?,'IN')")->execute([$item_id]);
respond(["status"=>"success","message"=>"Entrada registrada com sucesso","data"=>["item_id"=>$item_id]]);
