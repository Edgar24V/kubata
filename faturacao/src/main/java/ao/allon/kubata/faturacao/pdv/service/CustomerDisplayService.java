package ao.allon.kubata.faturacao.pdv.service;

import ao.allon.kubata.faturacao.pdv.controller.PdvController;
import ao.allon.kubata.faturacao.pdv.view.CustomerDisplayView;
import ao.allon.kubata.faturacao.service.AuditLogService;
import ao.allon.kubata.faturacao.service.EmpresaService;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.stereotype.Service;

@Service
public class CustomerDisplayService {

    private final PdvController pdvController;
    private final EmpresaService empresaService;
    private final AuditLogService audit;
    private Stage stage;
    private CustomerDisplayView view;

    public CustomerDisplayService(PdvController pdvController, EmpresaService empresaService, AuditLogService audit) {
        this.pdvController = pdvController;
        this.empresaService = empresaService;
        this.audit = audit;
    }

    public void start() {
        Platform.runLater(() -> {
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("ao/allon/kubata/pdv");
                boolean enabled = prefs.getBoolean("customerDisplay.enabled", true);
                if (!enabled) {
                    return;
                }
                if (stage != null && stage.isShowing()) {
                    return;
                }
                String loja = null;
                try { loja = empresaService.getDadosEmpresa().getNome(); } catch (Exception ignored) {}
                view = new CustomerDisplayView(loja);
                view.bindTo(pdvController.getCarrinho());

                stage = new Stage(StageStyle.UNDECORATED);
                stage.setTitle("Visor do Cliente");
                stage.setScene(new javafx.scene.Scene(view));
                int idx = prefs.getInt("customerDisplay.monitorIndex", 1);
                positionOnScreenIndex(stage, idx);
                stage.show();
                audit.log("CUSTOMER_DISPLAY_START", "display iniciado");
            } catch (Exception ex) {
                audit.log("CUSTOMER_DISPLAY_ERROR", "Falha ao iniciar: " + ex.getMessage());
            }
        });
    }

    public void stop() {
        Platform.runLater(() -> {
            try {
                if (stage != null) {
                    stage.close();
                    stage = null;
                    audit.log("CUSTOMER_DISPLAY_STOP", "display encerrado");
                }
            } catch (Exception ex) {
                audit.log("CUSTOMER_DISPLAY_ERROR", "Falha ao parar: " + ex.getMessage());
            }
        });
    }

    private void positionOnScreenIndex(Stage s, int index) {
        var screens = Screen.getScreens();
        if (index >= 0 && index < screens.size()) {
            Screen target = screens.get(index);
            Rectangle2D b = target.getVisualBounds();
            s.setX(b.getMinX());
            s.setY(b.getMinY());
            s.setWidth(b.getWidth());
            s.setHeight(b.getHeight());
            s.setFullScreen(true);
        } else if (screens.size() > 1) {
            Screen secondary = screens.get(1);
            Rectangle2D b = secondary.getVisualBounds();
            s.setX(b.getMinX());
            s.setY(b.getMinY());
            s.setWidth(b.getWidth());
            s.setHeight(b.getHeight());
            s.setFullScreen(true);
        } else {
            // Fallback quando não existir segunda tela
            Screen primary = Screen.getPrimary();
            Rectangle2D b = primary.getVisualBounds();
            double w = Math.min(800, b.getWidth());
            double h = Math.min(480, b.getHeight());
            s.setWidth(w);
            s.setHeight(h);
            s.setX(b.getMaxX() - w - 16);
            s.setY(b.getMaxY() - h - 16);
            audit.log("CUSTOMER_DISPLAY_FALLBACK", "Sem segundo monitor - modo janela");
        }
    }
}
