package de.fluxparticle.wizardfx;

import de.fluxparticle.wizardfx.requirement.Requirement;
import de.fluxparticle.wizardfx.requirement.SimpleRequirement;
import de.fluxparticle.wizardfx.requirement.TransformRequirement;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Created by sreinck on 15.09.17.
 */
public class WizardTest extends Application {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String ERROR_DATE_FORMAT = "must be date of form YYYY-MM-DD";

    private static final Pattern EMAIL = Pattern.compile("[a-z.+]+@[a-z.]+\\.[a-z]+");

    private static final Pattern PHONE = Pattern.compile("\\+?[0-9 ]+");

    private static final String STYLE_NORMAL = "";

    private static final String STYLE_ERROR = "-fx-background-color: lightcoral";

    protected enum FlightType {

        ONE_WAY_FLIGHT("one-way flight"),
        RETURN_FLIGHT("return flight");

        private final String display;

        FlightType(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Wizard root = new Wizard(step1(), step2());
        root.setOnFinish(event -> Platform.exit());

        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.setTitle("Test Wizard");
        primaryStage.show();
    }

    private static WizardStep step1() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        WizardStep step = new WizardStep("Personal Data", content);

        {
            // Name muss einfach nur vorhanden sein
            SimpleRequirement<String> requirement = new SimpleRequirement<>(str -> str.isEmpty() ? Optional.of("required") : Optional.empty());
            step.getRequirements().add(requirement);
            content.getChildren().add(textfield("Name", requirement));
        }
        {
            // Email muss vorhanden sein und dem Pattern genügen. Es werden jeweils unterschiedliche Fehlermeldungen angezeigt.
            SimpleRequirement<String> requirement = new SimpleRequirement<>(str -> str.isEmpty() ? Optional.of("required") : EMAIL.matcher(str).matches() ? Optional.empty() : Optional.of("must be valid email"));
            step.getRequirements().add(requirement);
            content.getChildren().add(textfield("Email", requirement));
        }
        {
            // Telefonnummer muss nicht vorhanden sein, aber wenn, dann muss sie richtig sein.
            SimpleRequirement<String> requirement = new SimpleRequirement<>(str -> str.isEmpty() ? Optional.empty() : PHONE.matcher(str).matches() ? Optional.empty() : Optional.of("must be valid phone number"));
            step.getRequirements().add(requirement);
            content.getChildren().add(textfield("Phone", requirement));
        }

        return step;
    }

    private static WizardStep step2() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setFillWidth(false);

        WizardStep step = new WizardStep("Flight Details", content);

        BooleanBinding oneWayFlight;
        {
            ComboBox<FlightType> cbFlightType = new ComboBox<>();

            // Bei einem einfachen Flug muss sich die GUI etwas anders verhalten, daher wird dieses Binding hier gespeichert.
            oneWayFlight = cbFlightType.valueProperty().isEqualTo(FlightType.ONE_WAY_FLIGHT);

            cbFlightType.getItems().addAll(FlightType.values());
            cbFlightType.setValue(FlightType.ONE_WAY_FLIGHT);
            content.getChildren().add(cbFlightType);
        }

        // Startwert für die Datumsfelder ist das aktuelle Datum
        String initDate = dateToString(LocalDate.now());

        // Das Startdatum muss einfach nur korrekt geparst worden sein
        SimpleRequirement<Optional<LocalDate>> startDateRequirement = new SimpleRequirement<>(optDate -> {
            if (optDate.isPresent()) {
                return Optional.empty();
            } else {
                return Optional.of(ERROR_DATE_FORMAT);
            }
        });

        {
            /*
             * Durch das TransformRequirement wird der String erst in ein Optional<LocalDate> konvertiert
             * und dann in die Überprüfung oben gegeben.
             */
            TransformRequirement<String, Optional<LocalDate>> transformedStartDateRequirement = new TransformRequirement<>(WizardTest::stringToDate, startDateRequirement);
            Node tfStartDate = textfield("start date", initDate, transformedStartDateRequirement);
            step.getRequirements().add(transformedStartDateRequirement);
            content.getChildren().add(tfStartDate);
        }

        /*
         * Wenn das Rückflugdatum vorhanden (also gültig) ist, wird geprüft, ob es vor dem Startdatum liegt
         * und ggf. eine andere Fehlermeldung ausgegeben.
         *
         * Hierfür habe ich das TransformRequirement um auf das bereits konvertierte Startdatum zugreifen zu können.
         */
        Requirement<Optional<LocalDate>> returnDateRequirement = new Requirement<>(monadicObservableValue -> {
            return monadicObservableValue.flatMap((Optional<LocalDate> returnDate) -> {
                if (returnDate.isPresent()) {
                    return EasyBind.combine(startDateRequirement.dataProperty(), oneWayFlight, (startDate, oneWay) -> {
                        if (!oneWay && startDate.isPresent() && returnDate.get().isBefore(startDate.get())) {
                            return Optional.of("return date must not be before start date");
                        } else {
                            return Optional.<String>empty();
                        }
                    });
                } else {
                    return new SimpleObjectProperty<>(Optional.of(ERROR_DATE_FORMAT));
                }
            });
        });

        {
            /*
             * Hier das gleiche wie für das Startdatum-Feld. Nur die Überprüfung oben ist ein wenig komplizierter.
             */
            TransformRequirement<String, Optional<LocalDate>> transformedReturnDateRequirement = new TransformRequirement<>(WizardTest::stringToDate, returnDateRequirement);
            Node tfReturnDate = textfield("return date", initDate, transformedReturnDateRequirement, oneWayFlight);
            step.getRequirements().add(transformedReturnDateRequirement);
            content.getChildren().add(tfReturnDate);
        }

        return step;
    }

    private static Function<ObservableValue<String>, ObservableValue<Optional<String>>> simpleValidator(Function<String, Optional<String>> validator) {
        return observableStringValue -> EasyBind.map(observableStringValue, validator);
    }

    private static Node textfield(String name, Requirement<String> requirement) {
        return textfield(name, "", requirement);
    }

    private static Node textfield(String name, String text, Requirement<String> requirement) {
        return textfield(name, text, requirement, new SimpleObjectProperty<>(false));
    }

    private static Node textfield(String name, String text, Requirement<String> requirement, ObservableValue<Boolean> disable) {
        TextField textField = new TextField(text);
        textField.disableProperty().bind(disable);

        requirement.dataProperty().bind(textField.textProperty());

        ObservableValue<Optional<String>> errorText = requirement.errorProperty();

        // Die Fehlermeldung als Tooltip anzeigen.
        MonadicBinding<Tooltip> tooltip = EasyBind.map(errorText, error -> error.map(Tooltip::new).orElse(null));
        textField.tooltipProperty().bind( tooltip );

        // Durch den Style anzeigen, dass dieses Feld fehlerhaft ist.
        MonadicBinding<String> style = EasyBind.map(errorText, error -> error.isPresent() ? STYLE_ERROR : STYLE_NORMAL);
        textField.styleProperty().bind( style );

        return new VBox(2, new Label(name), textField);
    }

    private static String dateToString(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    /**
     * @param string möglichst ein gültiges Datum
     * @return das Datum als LocaleDate, falls es geparst werden konnte
     */
    private static Optional<LocalDate> stringToDate(String string) {
        try {
            return Optional.of(LocalDate.from(DATE_FORMAT.parse(string)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
