<?php
require_once __DIR__ . '/bootstrap.php';

$userId = auth_required();

$stmt = $pdo->prepare("SELECT id, username, photo_path, role FROM users WHERE id = ? LIMIT 1");
$stmt->execute([$userId]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$user) {
    respond([
        "status" => "error",
        "message" => "Usuário não encontrado"
    ], 404);
}

respond([
    "status" => "success",
    "data" => $user
]);
