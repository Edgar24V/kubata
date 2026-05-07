-- V17: Create Banking Module Tables
CREATE TABLE contas_bancarias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    banco VARCHAR(50) NOT NULL,
    numero_conta VARCHAR(50) NOT NULL UNIQUE,
    iban VARCHAR(50) NOT NULL UNIQUE,
    swift VARCHAR(20),
    moeda VARCHAR(3) NOT NULL DEFAULT 'AOA',
    saldo DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    descricao VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE movimentos_bancarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conta_bancaria_id INTEGER NOT NULL,
    data_movimento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo VARCHAR(20) NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    saldo_anterior DECIMAL(19, 2) NOT NULL,
    saldo_atual DECIMAL(19, 2) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    categoria VARCHAR(100),
    referencia_documento VARCHAR(100),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (conta_bancaria_id) REFERENCES contas_bancarias(id)
);
