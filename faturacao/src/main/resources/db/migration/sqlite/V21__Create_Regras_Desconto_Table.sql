CREATE TABLE IF NOT EXISTS regras_desconto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    escopo VARCHAR(16) NOT NULL,
    categoria_id INTEGER,
    produto_id INTEGER,
    imposto_id INTEGER,
    max_percent DECIMAL(5,2) NOT NULL,
    inicio DATE,
    fim DATE,
    requer_aprovacao BOOLEAN NOT NULL DEFAULT 0,
    prioridade INTEGER NOT NULL DEFAULT 0,
    motivo VARCHAR(255),
    FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (imposto_id) REFERENCES impostos(id)
);

CREATE INDEX IF NOT EXISTS idx_regras_desconto_scope ON regras_desconto(escopo);
CREATE INDEX IF NOT EXISTS idx_regras_desconto_produto ON regras_desconto(produto_id);
CREATE INDEX IF NOT EXISTS idx_regras_desconto_categoria ON regras_desconto(categoria_id);
CREATE INDEX IF NOT EXISTS idx_regras_desconto_imposto ON regras_desconto(imposto_id);

INSERT INTO regras_desconto (created_at, active, escopo, max_percent, prioridade, requer_aprovacao)
VALUES (CURRENT_TIMESTAMP, 1, 'GLOBAL', 30.00, 0, 0);
