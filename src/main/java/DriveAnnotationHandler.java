import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import edu.wpi.first.wpilibj2.command.Command;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class DriveAnnotationHandler extends AbstractProcessor {
    private static String getPackageName(Element e) {
        while (e != null) {
            if (e.getKind().equals(ElementKind.PACKAGE)) {
                return ((PackageElement) e).getQualifiedName().toString();
            }
            e = e.getEnclosingElement();
        }

        return null;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Optional<? extends TypeElement> annotationOptional =
                annotations.stream()
                        .filter((te) -> te.getSimpleName().toString().equals("DriveMode"))
                        .findFirst();

        if (!annotationOptional.isPresent()) {
            return false;
        }

        TypeSpec.Builder driveModeClass = TypeSpec.classBuilder("DriveModes").addModifiers(Modifier.PUBLIC);
        MethodSpec.Builder driveModeInitializer = MethodSpec.methodBuilder("initDriveModes").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        TypeElement annotation = annotationOptional.get();
        roundEnv.getElementsAnnotatedWith(annotation)
                .forEach(
                        (element) -> {
                            String driveModeName = element.getSimpleName().toString();
                            String robotModePackage = getPackageName(element);

                            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found drive mode: " + driveModeName + " in package " + robotModePackage);

                            FieldSpec driveModeCommand = FieldSpec.builder(Command.class, Character.toLowerCase(driveModeName.charAt(0)) + driveModeName.substring(1), Modifier.PUBLIC, Modifier.STATIC).build();
                            driveModeInitializer.addStatement("DriveModes.$N = new $T()", driveModeCommand, element.asType());

                            driveModeClass.addField(driveModeCommand);
                        }
                );

        driveModeClass.addMethod(driveModeInitializer.build());

        JavaFile fileBuilder = JavaFile.builder("frc.robot.utils", driveModeClass.build()).build();

        try {
            fileBuilder.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("ControlAnnotations.DriveMode");
    }
}
