<?php
require_once __DIR__ . '/bootstrap.php';
$_SERVER['HTTP_AUTHORIZATION'] = ($_GET['token'] ?? '');
auth_required();
$batch_id = (int)($_GET['batch_id'] ?? 0);
if ($batch_id <= 0) respond(["status"=>"error","message"=>"Lote inválido"],400);
$batch = $pdo->prepare("SELECT b.*, t.name AS tech_name FROM delivery_batches b LEFT JOIN technicians t ON t.id=b.technician_id WHERE b.id=? LIMIT 1");
$batch->execute([$batch_id]);
$b = $batch->fetch(PDO::FETCH_ASSOC);
if (!$b) respond(["status"=>"error","message"=>"Lote não encontrado"],404);
$items = $pdo->prepare("SELECT m.qty, i.serial_number, i.mac_address, p.ref_code, p.brand, p.model FROM movements m LEFT JOIN items i ON i.id=m.item_id LEFT JOIN products p ON p.id=i.product_id WHERE m.batch_id=? AND m.movement='OUT' ORDER BY m.id DESC");
$items->execute([$batch_id]);
$rows = $items->fetchAll(PDO::FETCH_ASSOC);
$total = 0; foreach ($rows as $r) { $total += (int)($r['qty'] ?? 1); }
?><!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=80mm, initial-scale=1"><title>Recibo lote #<?php echo $batch_id; ?></title><style>body{font-family:Arial,sans-serif;font-size:12px;width:72mm;margin:0 auto;padding:6px}.c{text-align:center}.line{border-top:1px dashed #000;margin:6px 0}.item{margin:6px 0}</style></head><body><div class="c"><strong>NET CONECT</strong><br>Recibo de Entrega em Lote</div><div class="line"></div><div>Lote: <strong><?php echo $batch_id; ?></strong></div><div>Técnico: <strong><?php echo htmlspecialchars($b['tech_name'] ?? '-'); ?></strong></div><div>OS: <strong><?php echo htmlspecialchars($b['os_number'] ?? '-'); ?></strong></div><div>Data: <strong><?php echo htmlspecialchars($b['created_at'] ?? date('Y-m-d H:i:s')); ?></strong></div><div>Total de itens: <strong><?php echo $total; ?></strong></div><div class="line"></div><?php foreach ($rows as $r): ?><div class="item"><strong><?php echo htmlspecialchars(trim(($r['brand'] ?? '') . ' ' . ($r['model'] ?? ''))); ?></strong><br>Serial: <?php echo htmlspecialchars($r['serial_number'] ?? '-'); ?><br>MAC: <?php echo htmlspecialchars($r['mac_address'] ?? '-'); ?></div><?php endforeach; ?><div class="line"></div><div class="c">Desenvolvido por Jefferson Carvalho</div></body></html>