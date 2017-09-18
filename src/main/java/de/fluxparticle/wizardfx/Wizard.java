package de.fluxparticle.wizardfx;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by sreinck on 15.09.17.
 */
public class Wizard extends BorderPane {

    private final List<WizardStep> steps;

    private final IntegerProperty progress = new SimpleIntegerProperty(0);

    private final IntegerProperty active = new SimpleIntegerProperty(0);

    private final ObjectProperty<EventHandler<ActionEvent>> onFinish = new SimpleObjectProperty<>();

    /**
     * @param steps Schritte für dieses Wizard. Können später nicht mehr verändert werden.
     */
    public Wizard(WizardStep... steps) {
        this.steps = Arrays.asList(steps);

        {
            Optional<Node> progressIndicator = IntStream.range(0, steps.length)
                    .mapToObj(i -> {
                        Circle circle = new Circle(10);
                        circle.setStrokeWidth(3);

                        /*
                         * Alle Kreise, die sich im schon bearbeiteten Bereich befinden,
                         * bekommen einen clickHandler, der auf die jeweilige Seite umschaltet
                         */
                        EventHandler<MouseEvent> clickHandler = event -> active.set(i);
                        MonadicBinding<EventHandler<MouseEvent>> onMouseClicked = EasyBind.map(progress.asObject(), p -> i <= p ? clickHandler : null);
                        circle.onMouseClickedProperty().bind(onMouseClicked);

                        // Jenachdem, ob alle Anforderungen des jeweiligen Schrittes erfüllt sind, bekommt dieser Kreis die Farbe grün oder rot
                        MonadicBinding<Color> color = EasyBind.map(steps[i].allValidProperty(), valid -> valid ? Color.GREEN : Color.RED);

                        // Alle bearbeiteten Seiten bekommen die Farbe entsprechend ihrer Gültigkeit, alle anderen sind schwarz
                        circle.strokeProperty().bind(EasyBind.combine(progress.asObject(), color, (p, c) -> i <= p ? c : Color.BLACK));

                        // Die aktuelle Seite bekommt die Farbe entsprechend ihrer Gültigkeit, alle anderen sind durchsichtig
                        circle.fillProperty().bind(EasyBind.combine(active.asObject(), color, (a, c) -> a == i ? c : Color.TRANSPARENT));

                        return (Node) circle;
                    })
                    .reduce((l, r) -> {
                        HBox hBox = new HBox(l, new Path(new MoveTo(0, 0), new HLineTo(100)), r);
                        hBox.setAlignment(Pos.CENTER);
                        return hBox;
                    });

            if (progressIndicator.isPresent()) {
                VBox vBox = new VBox(progressIndicator.get());
                vBox.setPadding(new Insets(10));
                vBox.setAlignment(Pos.CENTER);
                setTop(vBox);
            }
        }

        // Im Center wird immer die Node des aktuellen Schrittes angezeigt
        centerProperty().bind(
                EasyBind.select(active.asObject())
                        .selectObject(a -> steps[a].contentProperty())
        );

        {
            HBox hBox = new HBox(5);
            hBox.setPadding(new Insets(10));

            {
                /*
                 * Der Prev-Button ist genau dann sichtbar, wenn wir nicht auf der ersten Seite sind,
                 * und aktiviert die vorhergehende Seite.
                 */
                Button bPrev = new Button("Prev");
                bPrev.disableProperty().bind( EasyBind.map( active.asObject(), a -> a == 0 ) );
                bPrev.setOnAction(event -> active.set(active.get() - 1));
                hBox.getChildren().add(bPrev);
            }

            {
                /*
                 * Erzeugt einen flexiblen Abstand zwischen den Buttons, damit Prev immer ganz links
                 * und die anderen beiden Buttons immer ganz rechts stehen.
                 */
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                hBox.getChildren().add(spacer);
            }

            {
                /*
                 * Der next Button ist immer dann aktiviert, wenn wir uns entweder nicht auf der neusten Seite befinden
                 * oder diese Seite in Ordnung ist. Auf der letzten Seite ist der Button immer deaktiviert.
                 */
                Button bNext = new Button("Next");

                MonadicBinding<Boolean> thisStepValid = EasyBind.select(active.asObject())
                        .selectObject(a -> steps[a].allValidProperty());

                MonadicBinding<Boolean> disable = EasyBind.combine(active.asObject(), progress.asObject(), thisStepValid, (a, p, valid) ->
                        a == steps.length - 1 || (a.equals(p) && !valid)
                );
                bNext.disableProperty().bind(disable);

                // Bei einer Aktion wird zur nächsten Seite gegangen und ggf. der progress erhöht.
                bNext.setOnAction(event -> {
                    int next = active.get() + 1;
                    if (next > progress.get()) {
                        progress.set(next);
                    }
                    active.set(next);
                });
                hBox.getChildren().add(bNext);
            }

            {
                /*
                 * Der Finish-Button kann nur aktiviert sind, wenn alle Seiten bisher angezeigt wurden
                 * und alle Seiten in Ordnung sind.
                 */
                Button bFinish = new Button("Finish");

                ObservableValue<Boolean> allStepsValid = Stream.of(steps)
                        .map(wizardStep -> (ObservableValue<Boolean>) wizardStep.allValidProperty())
                        .reduce((a, b) -> EasyBind.combine(a, b, (u, v) -> u && v))
                        .orElseGet(() -> new SimpleBooleanProperty(true));

                bFinish.disableProperty().bind( EasyBind.combine(  progress.asObject(), allStepsValid, (p, valid) -> !(p == steps.length - 1 && valid) ) );
                bFinish.onActionProperty().bind( onFinish );
                hBox.getChildren().add(bFinish);
            }

            setBottom(hBox);
        }
    }

    public EventHandler<ActionEvent> getOnFinish() {
        return onFinish.get();
    }

    public ObjectProperty<EventHandler<ActionEvent>> onFinishProperty() {
        return onFinish;
    }

    public void setOnFinish(EventHandler<ActionEvent> onFinish) {
        this.onFinish.set(onFinish);
    }

}
