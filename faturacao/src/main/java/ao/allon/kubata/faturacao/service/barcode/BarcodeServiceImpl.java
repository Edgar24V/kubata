package ao.allon.kubata.faturacao.service.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.pdf417.PDF417Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementação do serviço de geração de códigos de barras usando ZXing.
 */
@Service
public class BarcodeServiceImpl implements BarcodeService {

    @Override
    public BufferedImage generateBarcode(String content, BarcodeTypeEnum type, int width, int height) throws IOException {
        try {
            BitMatrix bitMatrix = createBitMatrix(content, type, width, height);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            throw new IOException("Erro ao gerar código de barras: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateBarcodeBytes(String content, BarcodeTypeEnum type, int width, int height) throws IOException {
        try {
            BitMatrix bitMatrix = createBitMatrix(content, type, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException e) {
            throw new IOException("Erro ao gerar bytes do código de barras: " + e.getMessage(), e);
        }
    }

    @Override
    public BufferedImage generateAGTQRCode(String invoiceData, int size) throws IOException {
        // Para QR Code AGT, garantimos correcao de erro alta e UTF-8
        return generateBarcode(invoiceData, BarcodeTypeEnum.QR_CODE, size, size);
    }

    @Override
    public boolean validateContent(String content, BarcodeTypeEnum type) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        switch (type) {
            case EAN_13:
                return content.matches("\\d{12,13}");
            case CODE_128:
                // Code 128 suporta ASCII completo, mas verificamos caracteres invalidos se necessario
                return content.length() <= 80; // Limite razoavel
            case QR_CODE:
                return content.length() <= 2953; // Limite teorico, mas na pratica menos
            default:
                return true;
        }
    }

    private BitMatrix createBitMatrix(String content, BarcodeTypeEnum type, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        switch (type) {
            case QR_CODE:
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // Alta correcao para documentos fiscais
                hints.put(EncodeHintType.MARGIN, 1); // Margem minima
                return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            
            case EAN_13:
                hints.put(EncodeHintType.MARGIN, 2);
                return new EAN13Writer().encode(content, BarcodeFormat.EAN_13, width, height, hints);
            
            case CODE_128:
                hints.put(EncodeHintType.MARGIN, 2);
                return new Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height, hints);

            case PDF_417:
                return new PDF417Writer().encode(content, BarcodeFormat.PDF_417, width, height, hints);
                
            default:
                throw new IllegalArgumentException("Tipo de código de barras não suportado: " + type);
        }
    }
}
