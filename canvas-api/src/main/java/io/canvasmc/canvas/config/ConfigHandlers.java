package io.canvasmc.canvas.config;

import io.canvasmc.canvas.config.annotation.Comment;
import io.canvasmc.canvas.config.annotation.Experimental;
import io.canvasmc.canvas.config.annotation.NamespacedKey;
import io.canvasmc.canvas.config.annotation.Pattern;
import io.canvasmc.canvas.config.annotation.RegisteredHandler;
import io.canvasmc.canvas.config.annotation.numeric.NegativeNumericValue;
import io.canvasmc.canvas.config.annotation.numeric.NonNegativeNumericValue;
import io.canvasmc.canvas.config.annotation.numeric.NonPositiveNumericValue;
import io.canvasmc.canvas.config.annotation.numeric.PositiveNumericValue;
import io.canvasmc.canvas.config.annotation.numeric.Range;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Arrays;

public class ConfigHandlers {
    @RegisteredHandler("comment")
    public static class CommentProcessor implements AnnotationContextProvider<Comment> {
        @Override
        public void apply(final StringWriter yamlWriter, final String indent, final String fullKey, final Field field, final @NotNull Comment comment) {
            if (comment.breakLineBefore()) {
                yamlWriter.append("\n");
            }
            Arrays.stream(comment.value()).forEach(val -> yamlWriter.append(indent).append("## ").append(val).append("\n"));
        }

        public Class<Comment> comment() {
            return Comment.class;
        }
    }

    @RegisteredHandler("experimental")
    public static class ExperimentalProcessor implements AnnotationContextProvider<Experimental> {
        @Override
        public void apply(final @NotNull StringWriter yamlWriter, final String indent, final String fullKey, final Field field, final @NotNull Experimental annotation) {
            yamlWriter.append(indent).append("## ").append("=== EXPERIMENTAL FEATURE ===").append("\n");
        }

        public Class<Experimental> experimental() {
            return Experimental.class;
        }
    }

    @RegisteredHandler("range")
    public static class RangeProcessor implements AnnotationValidationProvider<Range> {
        @Override
        public boolean validate(final String fullKey, final Field field, final Range range, final Object value) throws ValidationException {
            if (value instanceof Number number) {
                float floatValue = number.floatValue();
                int from = range.from();
                int to = range.to();
                if (!(range.inclusive() ? (floatValue >= from && floatValue <= to) : floatValue > from && floatValue < to)) {
                    throw new ValidationException("Number for key, '" + fullKey + "' was out of bounds! Value must be between " + from + " and " + to);
                }
                return true;
            } else {
                throw new ValidationException("Range validation was applied to a non-numeric object.");
            }
        }

        public Class<Range> range() {
            return Range.class;
        }
    }

    @RegisteredHandler("nonNegative")
    public static class NonNegativeProcessor implements AnnotationValidationProvider<NonNegativeNumericValue> {
        @Override
        public boolean validate(final String fullKey, final Field field, final NonNegativeNumericValue annotation, final Object value) throws ValidationException {
            if (value instanceof Number number) {
                if (!(number.floatValue() >= 0)) {
                    throw new ValidationException("Value must be a non-negative value");
                }
                return true;
            }
            throw new ValidationException("NonNegativeNumericValue validation applied to a non-numeric object.");
        }

        public Class<NonNegativeNumericValue> nonNegative() {
            return NonNegativeNumericValue.class;
        }
    }

    @RegisteredHandler("nonPositive")
    public static class NonPositiveProcessor implements AnnotationValidationProvider<NonPositiveNumericValue> {
        public Class<NonPositiveNumericValue> nonPositive() {
            return NonPositiveNumericValue.class;
        }

        @Override
        public boolean validate(final String fullKey, final Field field, final NonPositiveNumericValue annotation, final Object value) throws ValidationException {
            if (value instanceof Number number) {
                if (!(number.floatValue() <= 0)) {
                    throw new ValidationException("Value must be a non-positive value");
                }
                return true;
            }
            throw new ValidationException("NonPositiveNumericValue validation applied to a non-numeric object.");
        }
    }

    @RegisteredHandler("negative")
    public static class NegativeProcessor implements AnnotationValidationProvider<NegativeNumericValue> {
        @Override
        public boolean validate(final String fullKey, final Field field, final NegativeNumericValue annotation, final Object value) throws ValidationException {
            if (value instanceof Number number) {
                if (!(number.floatValue() < 0)) {
                    throw new ValidationException("Value must be a negative value");
                }
                return true;
            }
            throw new ValidationException("NegativeNumericValue validation applied to a non-numeric object.");
        }

        public Class<NegativeNumericValue> negative() {
            return NegativeNumericValue.class;
        }
    }

    @RegisteredHandler("positive")
    public static class PositiveProcessor implements AnnotationValidationProvider<PositiveNumericValue> {
        @Override
        public boolean validate(final String fullKey, final Field field, final PositiveNumericValue annotation, final Object value) throws ValidationException {
            if (value instanceof Number number) {
                if (!(number.floatValue() > 0)) {
                    throw new ValidationException("Value must be a positive value");
                }
                return true;
            }
            throw new ValidationException("PositiveNumericValue validation applied to a non-numeric object.");
        }

        public Class<PositiveNumericValue> positive() {
            return PositiveNumericValue.class;
        }
    }

    @RegisteredHandler("pattern")
    public static class PatternProcessor implements AnnotationValidationProvider<Pattern> {
        @Override
        public boolean validate(final String fullKey, final Field field, final Pattern annotation, final Object value) throws ValidationException {
            if (value == null) {
                return false; // quick escape
            }
            return value.toString().matches(annotation.pattern());
        }

        public Class<Pattern> pattern() {
            return Pattern.class;
        }
    }

    @RegisteredHandler("namespacedKey")
    public static class NamespacedKeyProcessor implements AnnotationValidationProvider<NamespacedKey> {
        @Override
        public boolean validate(final String fullKey, final Field field, final NamespacedKey annotation, final Object value) throws ValidationException {
            if (value == null) {
                return false; // quick escape
            }
            String string = value.toString();
            if (string.isEmpty() || string.length() > Short.MAX_VALUE) throw new ValidationException("String is empty or exceeds char limit");

            String[] components = string.split(":", 3);
            if (components.length > 2) {
                throw new ValidationException("Contains multiple ':' characters");
            }

            String key = (components.length == 2) ? components[1] : "";
            if (components.length == 1) {
                String comp0 = components[0];
                if (comp0.isEmpty() || !Key.parseableValue(comp0)) {
                    throw new ValidationException("Invalid namespaced key, comp0 was empty or unparsable");
                }

                return true; // valid, use minecraft
            } else if (components.length == 2 && !Key.parseableValue(key)) {
                throw new ValidationException("Invalid/unparsable key");
            }

            String namespace = components[0];
            if (namespace.isEmpty()) {
                return true; // valid, use minecraft
            }

            if (!Key.parseableNamespace(namespace)) {
                throw new ValidationException("Invalid/unparsable namespace");
            }

            return true; // passed
        }

        public Class<NamespacedKey> namespacedKey() {
            return NamespacedKey.class;
        }
    }
}
