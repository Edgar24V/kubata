-- Migration V7: Adicionar colunas que existem na entidade Empresa mas não estão no schema inicial

ALTER TABLE empresas ADD COLUMN banco TEXT;
ALTER TABLE empresas ADD COLUMN caixa_postal TEXT;
ALTER TABLE empresas ADD COLUMN telemovel TEXT;
ALTER TABLE empresas ADD COLUMN iban TEXT;
ALTER TABLE empresas ADD COLUMN conta_bancaria TEXT;
ALTER TABLE empresas ADD COLUMN descricao_actividade TEXT;
ALTER TABLE empresas ADD COLUMN data_constituicao TEXT;
ALTER TABLE empresas ADD COLUMN conservatoria TEXT;
ALTER TABLE empresas ADD COLUMN matricula_comercial TEXT;

ALTER TABLE empresas ADD COLUMN numero_certificado_agt TEXT;
ALTER TABLE empresas ADD COLUMN versao_certificado_agt TEXT;
ALTER TABLE empresas ADD COLUMN data_certificado_agt TEXT;
ALTER TABLE empresas ADD COLUMN hash_certificado_agt TEXT;

ALTER TABLE empresas ADD COLUMN moeda_base TEXT DEFAULT 'AOA';
ALTER TABLE empresas ADD COLUMN casas_decimais_valor INTEGER DEFAULT 2;
ALTER TABLE empresas ADD COLUMN casas_decimais_quantidade INTEGER DEFAULT 3;
ALTER TABLE empresas ADD COLUMN exercicio_actual INTEGER;

ALTER TABLE empresas ADD COLUMN logotipo BLOB;
ALTER TABLE empresas ADD COLUMN logotipo_mime_type TEXT;

ALTER TABLE empresas ADD COLUMN rodape_documento TEXT;
ALTER TABLE empresas ADD COLUMN mensagem_fatura TEXT;

ALTER TABLE empresas ADD COLUMN nome_comercial TEXT;
ALTER TABLE empresas ADD COLUMN tipo_contribuinte TEXT;
ALTER TABLE empresas ADD COLUMN municipio TEXT;
ALTER TABLE empresas ADD COLUMN provincia TEXT;
ALTER TABLE empresas ADD COLUMN pais TEXT DEFAULT 'AO';
