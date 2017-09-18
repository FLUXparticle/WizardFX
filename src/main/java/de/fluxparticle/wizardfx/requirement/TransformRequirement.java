package de.fluxparticle.wizardfx.requirement;

import java.util.function.Function;

/**
 * Created by sreinck on 16.09.17.
 *
 * Die Klasse {@link TransformRequirement} ermöglicht es einen Wert erst zu transformieren
 * und dann durch ein weiteres {@link Requirement} prüfen zu lassen, wobei der transformierte Wert
 * durch dieses {@link TransformRequirement} verfügbar bleibt
 *
 * @param <T> Eingabe-Typ
 * @param <U> Ausgabe-Typ
 */
public class TransformRequirement<T, U> extends Requirement<T> {

    /**
     * @param transform eine Funktion, die einen Wert vom Typ T in einen Wert vom Typ U konvertiert
     * @param requirement ein {@link Requirement}, das den transformierten Wert als Eingabe erwartet und überprüft
     */
    public TransformRequirement(Function<T, U> transform, Requirement<U> requirement) {
        super(data -> {
            requirement.dataProperty().bind( data.map(transform) );
            return requirement.errorProperty();
        });
    }

}
