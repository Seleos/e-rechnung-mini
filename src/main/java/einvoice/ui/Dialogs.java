package einvoice.ui;

import einvoice.i18n.Messages;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

/** Message dialogs with buttons in the selected language. */
final class Dialogs {
    private Dialogs() {
    }

    static void error(String header, Throwable cause) {
        String detail = cause.getMessage() == null ? cause.toString() : cause.getMessage();
        error(header, detail);
    }

    static void error(String header, String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR, detail, okButton());
        alert.setHeaderText(header);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    static boolean confirm(String header, String detail) {
        ButtonType ok = okButton();
        ButtonType cancel = new ButtonType(Messages.get("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, detail, ok, cancel);
        alert.setHeaderText(header);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert.showAndWait().filter(ok::equals).isPresent();
    }

    private static ButtonType okButton() {
        return new ButtonType(Messages.get("action.ok"), ButtonBar.ButtonData.OK_DONE);
    }
}
