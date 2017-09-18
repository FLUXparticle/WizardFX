package de.fluxparticle.wizardfx;

import de.fluxparticle.wizardfx.requirement.Requirement;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import org.fxmisc.easybind.EasyBind;

import java.util.Optional;

/**
 * Created by sreinck on 15.09.17.
 */
public class WizardStep {

    private final StringProperty text = new SimpleStringProperty();

    private final ObjectProperty<Node> content = new SimpleObjectProperty<>();

    private final ObservableList<Requirement<?>> requirements = FXCollections.observableArrayList();

    private final ReadOnlyBooleanWrapper allValid = new ReadOnlyBooleanWrapper();

    /**
     * @param text Name dieses Schritts
     * @param content Inhalt, der für dieses Schritt angezeigt werden soll
     */
    public WizardStep(String text, Node content) {
        this.text.setValue(text);
        this.content.setValue(content);

        ObservableList<ObservableValue<Optional<String>>> errors = EasyBind.map(requirements, Requirement::errorProperty);

        allValid.bind(EasyBind.combine(errors, stream -> stream.noneMatch(Optional::isPresent)));
    }

    public String getText() {
        return text.get();
    }

    public StringProperty textProperty() {
        return text;
    }

    public void setText(String text) {
        this.text.set(text);
    }

    public Node getContent() {
        return content.get();
    }

    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    public void setContent(Node content) {
        this.content.set(content);
    }

    public ObservableList<Requirement<?>> getRequirements() {
        return requirements;
    }

    public ReadOnlyBooleanProperty allValidProperty() {
        return allValid.getReadOnlyProperty();
    }

}
