
Net Conect App V3.1

Principais mudanças:
- Menu principal com Entrada, Saída, Lote de saída e Histórico.
- Tema verde claro e botões coloridos por função.
- Login com logo Net Conect e créditos Jefferson Carvalho.
- Saída com seleção de técnico e nomes em português.
- Lote preparado para recibo por /api/receipt_batch.php usando token.
- Entrada preparada para os modos: unitária, em lote e por modelo.

IMPORTANTE
- Para as novas telas funcionarem totalmente, copie os arquivos de exemplo da pasta api_examples para a pasta /api do seu sistema.
- O arquivo catalog_products.php é usado pelas telas de Entrada e Lote por modelo.
- O arquivo receipt_batch.php evita o redirecionamento para o login do sistema web ao imprimir o lote pelo app.
- A parte de foto obrigatória na saída ficou preparada na interface e no fluxo da V3.1, mas depende de uma rodada final de integração do endpoint de upload.
