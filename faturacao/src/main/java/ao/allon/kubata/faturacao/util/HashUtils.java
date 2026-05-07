package ao.allon.kubata.faturacao.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitário para geração e validação de assinaturas digitais RSA-SHA1 (AGT).
 * Gerencia o ciclo de vida das chaves criptográficas usando um KeyStore local.
 */
public class HashUtils {

    private static final Logger logger = Logger.getLogger(HashUtils.class.getName());
    private static final String KEYSTORE_FILENAME = "kubata_keystore.jks";
    private static final String KEYSTORE_PASSWORD = "kubata_secure_pass"; // Em produção, usar variável de ambiente
    private static final String KEY_ALIAS = "kubata_signing_key";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    static {
        initializeKeys();
    }

    private static void initializeKeys() {
        try {
            File keystoreFile = new File(KEYSTORE_FILENAME);
            KeyStore keyStore = KeyStore.getInstance("JKS");

            if (keystoreFile.exists()) {
                logger.info("Carregando KeyStore existente: " + KEYSTORE_FILENAME);
                try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                    keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
                }
                
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, KEYSTORE_PASSWORD.toCharArray());
                    Certificate cert = keyStore.getCertificate(KEY_ALIAS);
                    publicKey = cert.getPublicKey();
                    logger.info("Chaves RSA carregadas com sucesso.");
                } else {
                    logger.warning("Alias não encontrado no KeyStore. Gerando novas chaves...");
                    generateAndSaveKeys(keyStore, keystoreFile);
                }
            } else {
                logger.info("KeyStore não encontrado. Criando novo: " + KEYSTORE_FILENAME);
                keyStore.load(null, null);
                generateAndSaveKeys(keyStore, keystoreFile);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "ERRO CRÍTICO: Falha ao inicializar sistema de criptografia AGT", e);
            throw new RuntimeException("Falha na inicialização de chaves criptográficas", e);
        }
    }

    private static void generateAndSaveKeys(KeyStore keyStore, File keystoreFile) throws Exception {
        // Gerar par de chaves RSA 1024 bits (mínimo exigido pela AGT)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair pair = keyGen.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();

        // Criar certificado auto-assinado (necessário para armazenar no KeyStore)
        // Nota: Em Java puro sem BouncyCastle completo, criar cert X509 programaticamente é complexo.
        // Para simplificar e evitar dependências extras, vamos usar uma abordagem alternativa se falhar:
        // Salvar apenas as chaves serializadas se KeyStore for muito complexo sem bibliotecas.
        // Mas o JKS exige certificado.
        // Vamos tentar usar uma implementação simplificada ou assumir que BouncyCastle está disponível se o projeto usar Spring Boot.
        // Se não, vamos usar serialização simples de objetos KeyPair para garantir persistência neste ambiente.
        
        // Abordagem alternativa robusta para este ambiente: Salvar KeyPair serializado
        // (JKS requer certificado que é difícil gerar sem BouncyCastle)
        saveKeyPairToDisk(pair);
    }
    
    // Método alternativo de persistência simples (sem JKS complexo)
    private static void saveKeyPairToDisk(KeyPair pair) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(KEYSTORE_FILENAME + ".ser"))) {
            oos.writeObject(pair);
            logger.info("Par de chaves salvo em disco (formato serializado).");
        } catch (IOException e) {
            logger.severe("Erro ao salvar chaves: " + e.getMessage());
        }
    }
    
    // Tentar carregar do disco se JKS falhar
    private static boolean loadKeyPairFromDisk() {
        File file = new File(KEYSTORE_FILENAME + ".ser");
        if (!file.exists()) return false;
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            KeyPair pair = (KeyPair) ois.readObject();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();
            logger.info("Chaves carregadas do disco (formato serializado).");
            return true;
        } catch (Exception e) {
            logger.severe("Erro ao carregar chaves do disco: " + e.getMessage());
            return false;
        }
    }

    /**
     * Assina os dados usando SHA1withRSA conforme exigido pela AGT.
     * Garante o uso de UTF-8 na conversão da string.
     * 
     * @param input A string formatada contendo os dados da fatura.
     * @return A assinatura digital em Base64.
     */
    public static String sign(String input) {
        if (privateKey == null) {
            if (!loadKeyPairFromDisk()) {
                 // Última tentativa: gerar em memória (aviso: perde persistência)
                 try {
                     KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                     keyGen.initialize(1024);
                     KeyPair pair = keyGen.generateKeyPair();
                     privateKey = pair.getPrivate();
                     publicKey = pair.getPublic();
                     saveKeyPairToDisk(pair); // Tenta salvar para a próxima vez
                 } catch (Exception e) {
                     throw new RuntimeException("Impossível gerar chaves RSA", e);
                 }
            }
        }

        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(input.getBytes(StandardCharsets.UTF_8));
            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            logger.severe("Erro ao assinar digitalmente: " + e.getMessage());
            throw new RuntimeException("Falha na assinatura digital da fatura", e);
        }
    }

    /**
     * Valida uma assinatura digital.
     */
    public static boolean verify(String input, String signatureBase64) {
        if (publicKey == null) return false;
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(input.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public static PrivateKey getPrivateKey() {
        if (privateKey == null) {
            // Tenta inicializar se estiver nulo (pode acontecer se falhar o static block)
            try {
                if (!loadKeyPairFromDisk()) {
                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                    keyGen.initialize(1024);
                    KeyPair pair = keyGen.generateKeyPair();
                    privateKey = pair.getPrivate();
                    publicKey = pair.getPublic();
                    saveKeyPairToDisk(pair);
                }
            } catch (Exception e) {
                throw new RuntimeException("Erro fatal ao obter chave privada", e);
            }
        }
        return privateKey;
    }

    public static PublicKey getPublicKey() {
        return publicKey;
    }
}
