<?php
require_once __DIR__ . '/bootstrap.php';

try {
    $search = trim($_GET['q'] ?? '');

    $sql = "
        SELECT
            p.id AS product_id,
            p.ref_code,
            p.brand,
            p.model,
            p.barcode,
            p.photo_path,
            c.name AS category_name,
            COUNT(i.id) AS available_qty
        FROM products p
        LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN items i
            ON i.product_id = p.id
           AND i.status = 'IN_STOCK'
    ";

    $params = [];

    if ($search !== '') {
        $sql .= "
            WHERE
                p.ref_code LIKE ?
                OR p.brand LIKE ?
                OR p.model LIKE ?
                OR p.barcode LIKE ?
                OR c.name LIKE ?
        ";
        $like = '%' . $search . '%';
        $params = [$like, $like, $like, $like, $like];
    }

    $sql .= "
        GROUP BY
            p.id, p.ref_code, p.brand, p.model, p.barcode, p.photo_path, c.name
        ORDER BY
            p.brand ASC, p.model ASC
    ";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    foreach ($rows as &$row) {
        $brand = trim((string)($row['brand'] ?? ''));
        $model = trim((string)($row['model'] ?? ''));
        $row['product_name'] = trim($brand . ' ' . $model);
        $row['available_qty'] = (int)($row['available_qty'] ?? 0);
    }

    echo json_encode([
        "status" => "success",
        "data" => $rows
    ], JSON_UNESCAPED_UNICODE);
} catch (Throwable $e) {
    echo json_encode([
        "status" => "error",
        "message" => $e->getMessage()
    ], JSON_UNESCAPED_UNICODE);
}
