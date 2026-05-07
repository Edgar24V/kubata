package ao.allon.kubata.faturacao.integration.scale;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ScaleSerialService {

    private SerialPort port;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public List<String> listPorts() {
        List<String> list = new ArrayList<>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            list.add(p.getSystemPortName());
        }
        return list;
    }

    public boolean connect(String portName, int baudRate) {
        disconnect();
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 1000);
        return port.openPort();
    }

    public void startCapture(Consumer<String> onRawLine, Consumer<BigDecimal> onWeight) {
        if (port == null || !port.isOpen()) return;
        if (running.get()) return;
        running.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(port.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (running.get() && (line = br.readLine()) != null) {
                    if (onRawLine != null) onRawLine.accept(line);
                    BigDecimal w = WeightParser.parse(line);
                    if (w != null && onWeight != null) onWeight.accept(w);
                }
            } catch (Exception ignored) {
            } finally {
                stopCapture();
            }
        });
    }

    public void stopCapture() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public void disconnect() {
        stopCapture();
        if (port != null) {
            try { port.closePort(); } catch (Exception ignored) {}
            port = null;
        }
    }
}
