package de.fluxparticle.wizardfx.requirement;

import java.util.Optional;
import java.util.function.Function;

/**
 * Created by sreinck on 16.09.17.
 */
public class SimpleRequirement<T> extends Requirement<T> {

    /**
     * @param validator eine einfache Funktion, die einen Wert vom Typ T in eine optionale Fehlermeldung konvertiert
     */
    public SimpleRequirement(Function<T, Optional<String>> validator) {
        super(monadicObservableValue -> monadicObservableValue.map(validator));
    }

}
