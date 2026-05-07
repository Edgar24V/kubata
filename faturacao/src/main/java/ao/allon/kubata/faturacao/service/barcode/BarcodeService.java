package ao.allon.kubata.faturacao.service.barcode;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Interface para serviço de geração e manipulação de códigos de barras.
 * Suporta QR Code, EAN-13, Code-128 e PDF-417.
 */
public interface BarcodeService {

    /**
     * Gera uma imagem de código de barras.
     *
     * @param content Conteúdo a ser codificado.
     * @param type Tipo de código de barras.
     * @param width Largura da imagem.
     * @param height Altura da imagem.
     * @return BufferedImage contendo o código de barras.
     * @throws IOException Se houver erro na geração.
     */
    BufferedImage generateBarcode(String content, BarcodeTypeEnum type, int width, int height) throws IOException;

    /**
     * Gera um código de barras e retorna como array de bytes (PNG).
     * Ideal para armazenar no banco ou servir via API.
     *
     * @param content Conteúdo a ser codificado.
     * @param type Tipo de código de barras.
     * @param width Largura da imagem.
     * @param height Altura da imagem.
     * @return byte[] contendo a imagem PNG.
     * @throws IOException Se houver erro na geração.
     */
    byte[] generateBarcodeBytes(String content, BarcodeTypeEnum type, int width, int height) throws IOException;

    /**
     * Gera um QR Code específico para faturas AGT (Angola).
     * O conteúdo deve seguir o padrão:
     * H:Hash;D:Data;T:Total;...
     *
     * @param invoiceData Dados da fatura formatados conforme norma AGT.
     * @param size Tamanho do lado do QR Code (quadrado).
     * @return BufferedImage do QR Code AGT.
     * @throws IOException Se houver erro na geração.
     */
    BufferedImage generateAGTQRCode(String invoiceData, int size) throws IOException;

    /**
     * Valida se o conteúdo é válido para o tipo de código de barras.
     * Ex: EAN-13 deve ter apenas números e tamanho correto.
     *
     * @param content Conteúdo a validar.
     * @param type Tipo de código de barras.
     * @return true se válido, false caso contrário.
     */
    boolean validateContent(String content, BarcodeTypeEnum type);
}
