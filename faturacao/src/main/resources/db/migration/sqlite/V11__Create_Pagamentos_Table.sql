CREATE TABLE pagamentos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    fatura_id INTEGER NOT NULL,
    metodo VARCHAR(50) NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    FOREIGN KEY (fatura_id) REFERENCES faturas(id)
);

