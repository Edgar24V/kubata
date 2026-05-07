-- V23__Add_FichaTecnica_Tables.sql

CREATE TABLE fichas_tecnicas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    produto_id INTEGER NOT NULL UNIQUE,
    observacoes TEXT,
    FOREIGN KEY (produto_id) REFERENCES produtos (id)
);

CREATE TABLE fichas_tecnicas_itens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    ficha_tecnica_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    quantidade DECIMAL(19, 4) NOT NULL,
    FOREIGN KEY (ficha_tecnica_id) REFERENCES fichas_tecnicas (id),
    FOREIGN KEY (produto_id) REFERENCES produtos (id)
);
