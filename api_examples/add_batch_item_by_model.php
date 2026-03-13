<?php
require_once __DIR__ . '/bootstrap.php';
require_method('POST');
auth_required();
$data = json_input();
$batch_id = (int)($data['batch_id'] ?? 0);
$product_id = (int)($data['product_id'] ?? 0);
$quantity = (int)($data['quantity'] ?? 0);
$note = trim($data['note'] ?? '');
if ($batch_id <= 0 || $product_id <= 0 || $quantity <= 0) respond(["status"=>"error","message"=>"Lote, produto e quantidade são obrigatórios"],400);
$stmt = $pdo->prepare("SELECT id FROM items WHERE product_id = ? AND status = 'IN_STOCK' ORDER BY id ASC LIMIT $quantity");
$stmt->execute([$product_id]);
$items = $stmt->fetchAll(PDO::FETCH_ASSOC);
if (count($items) < $quantity) respond(["status"=>"error","message"=>"Quantidade insuficiente em estoque"],400);
foreach ($items as $it) {
  $item_id = (int)$it['id'];
  $pdo->prepare("UPDATE items SET status='OUT' WHERE id=?")->execute([$item_id]);
  $pdo->prepare("INSERT INTO movements (item_id, movement, qty, batch_id, note, created_at) VALUES (?,'OUT',1,?,?,NOW())")->execute([$item_id,$batch_id,$note ?: null]);
}
respond(["status"=>"success","message"=>"Itens adicionados por modelo ao lote","data"=>["added"=>count($items)]]);
