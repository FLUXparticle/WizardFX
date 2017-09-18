package de.fluxparticle.wizardfx.requirement;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.fxmisc.easybind.monadic.MonadicObservableValue;

import java.util.Optional;
import java.util.function.Function;

/**
 * Created by sreinck on 16.09.17.
 */
public class Requirement<T> {

    private final ObjectProperty<T> data = new SimpleObjectProperty<>();

    private final MonadicBinding<Optional<String>> error;

    /**
     * Es wird ein MonadicObservableValue eingesetzt, {@code data} am Anfang den Wert null enthält und ein monadisches
     * Binding nur ausgelöst wird, wenn der Wert nicht null ist.
     *
     * @param validator eine Funktion, die einen monadischen ObservableValue in eine optionale Fehlermeldung konvertiert
     */
    public Requirement(Function<MonadicObservableValue<T>, MonadicBinding<Optional<String>>> validator) {
        error = validator.apply(EasyBind.monadic(data));
    }

    public ObjectProperty<T> dataProperty() {
        return data;
    }

    public MonadicBinding<Optional<String>> errorProperty() {
        return error;
    }

}
