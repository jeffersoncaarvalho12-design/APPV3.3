<?php
require_once __DIR__ . '/bootstrap.php';
auth_required();
try {
  $stmt = $pdo->query("SELECT id, CONCAT(IFNULL(ref_code,''), CASE WHEN ref_code IS NULL OR ref_code='' THEN '' ELSE ' - ' END, IFNULL(brand,''), ' ', IFNULL(model,'')) AS label, brand, model FROM products ORDER BY brand, model");
  respond(["status"=>"success","data"=>$stmt->fetchAll(PDO::FETCH_ASSOC)]);
} catch (Throwable $e) {
  respond(["status"=>"error","message"=>$e->getMessage()],500);
}
