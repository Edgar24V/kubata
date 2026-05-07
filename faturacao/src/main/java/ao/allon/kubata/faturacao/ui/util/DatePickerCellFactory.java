package ao.allon.kubata.faturacao.ui.util;

import java.lang.reflect.InvocationTargetException;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class DatePickerCellFactory {

    // Método reutilizável para adicionar DatePicker em qualquer coluna
    public static <T> void addDatePickerToColumn(TableColumn<T, LocalDate> column, String propertyName) {
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));

        column.setCellFactory(new Callback<TableColumn<T, LocalDate>, TableCell<T, LocalDate>>() {
            @Override
            public TableCell<T, LocalDate> call(TableColumn<T, LocalDate> param) {
                return new TableCell<T, LocalDate>() {
                    private DatePicker datePicker;

                    @Override
                    protected void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            if (datePicker == null) {
                                datePicker = new DatePicker();
                                datePicker.setPrefWidth(120);
                                datePicker.setEditable(true);

                                // Evento ao selecionar uma data
                                datePicker.setOnAction(new EventHandler<ActionEvent>() {
                                    @Override
                                    public void handle(ActionEvent event) {
                                        LocalDate selectedDate = datePicker.getValue();
                                        if (selectedDate != null) {
                                            // Usa reflexão para chamar o setter dinâmico
                                            try {
                                                // Atualiza o valor na célula correspondente da tabela
                                                getTableView().getItems().get(getIndex())
                                                    .getClass().getMethod("set" + capitalize(propertyName), LocalDate.class)
                                                    .invoke(getTableView().getItems().get(getIndex()), selectedDate);
                                                
                                                // Commit da edição
                                                commitEdit(selectedDate);  // Commite a alteração na célula
                                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                                               
                                            }
                                        }
                                    }
                                });
                            }
                            // Atualiza o valor do DatePicker com o valor atual da célula
                            if (item != null) {
                                datePicker.setValue(item);  // Atualiza com a data atual
                            } else {
                                datePicker.setValue(null);  // Caso o item seja nulo, deixa o DatePicker em branco
                            }
                            setGraphic(datePicker);  // Exibe o DatePicker
                            setText(null);
                        }
                    }
                };
            }
        });
    }

    // Método para capitalizar a primeira letra do nome do método (para chamadas dinâmicas de setter)
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
