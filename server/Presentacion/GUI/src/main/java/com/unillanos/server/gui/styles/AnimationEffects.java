package com.unillanos.server.gui.styles;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Clase utilitaria para efectos de animación y hover en la interfaz.
 * Proporciona transiciones suaves y efectos visuales elegantes.
 */
public final class AnimationEffects {

    private AnimationEffects() {
        // Clase utilitaria
    }

    // Duración estándar de animaciones
    private static final Duration ANIMATION_DURATION = Duration.millis(200);
    private static final Duration HOVER_DURATION = Duration.millis(150);
    private static final Duration FADE_DURATION = Duration.millis(300);

    /**
     * Aplica efecto de hover suave a un nodo.
     * Aumenta ligeramente la escala y agrega sombra.
     */
    public static void applyHoverEffect(Node node) {
        DropShadow hoverShadow = new DropShadow();
        hoverShadow.setColor(Color.rgb(0, 0, 0, 0.15));
        hoverShadow.setRadius(8);
        hoverShadow.setOffsetX(0);
        hoverShadow.setOffsetY(2);

        node.setOnMouseEntered(e -> {
            ScaleTransition scaleIn = new ScaleTransition(HOVER_DURATION, node);
            scaleIn.setFromX(1.0);
            scaleIn.setFromY(1.0);
            scaleIn.setToX(1.02);
            scaleIn.setToY(1.02);

            FadeTransition fadeIn = new FadeTransition(HOVER_DURATION, node);
            fadeIn.setFromValue(1.0);
            fadeIn.setToValue(0.95);

            ParallelTransition hoverIn = new ParallelTransition(scaleIn, fadeIn);
            hoverIn.play();

            node.setEffect(hoverShadow);
        });

        node.setOnMouseExited(e -> {
            ScaleTransition scaleOut = new ScaleTransition(HOVER_DURATION, node);
            scaleOut.setFromX(1.02);
            scaleOut.setFromY(1.02);
            scaleOut.setToX(1.0);
            scaleOut.setToY(1.0);

            FadeTransition fadeOut = new FadeTransition(HOVER_DURATION, node);
            fadeOut.setFromValue(0.95);
            fadeOut.setToValue(1.0);

            ParallelTransition hoverOut = new ParallelTransition(scaleOut, fadeOut);
            hoverOut.play();

            node.setEffect(null);
        });
    }

    /**
     * Aplica efecto de pulso suave para elementos importantes.
     */
    public static void applyPulseEffect(Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1000), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);

        FadeTransition fade = new FadeTransition(Duration.millis(1000), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.7);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);

        ParallelTransition pulseEffect = new ParallelTransition(pulse, fade);
        pulseEffect.play();
    }

    /**
     * Aplica efecto de desvanecimiento (fade) al aparecer.
     */
    public static void applyFadeInEffect(Node node) {
        node.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(FADE_DURATION, node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    /**
     * Aplica efecto de deslizamiento desde la izquierda.
     */
    public static void applySlideInFromLeft(Node node) {
        node.setTranslateX(-200);
        node.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(ANIMATION_DURATION, node);
        slideIn.setFromX(-200);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(ANIMATION_DURATION, node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition slideEffect = new ParallelTransition(slideIn, fadeIn);
        slideEffect.play();
    }

    /**
     * Aplica efecto de deslizamiento desde la derecha.
     */
    public static void applySlideInFromRight(Node node) {
        node.setTranslateX(200);
        node.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(ANIMATION_DURATION, node);
        slideIn.setFromX(200);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(ANIMATION_DURATION, node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition slideEffect = new ParallelTransition(slideIn, fadeIn);
        slideEffect.play();
    }

    /**
     * Aplica efecto de aparición desde abajo.
     */
    public static void applySlideInFromBottom(Node node) {
        node.setTranslateY(50);
        node.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(ANIMATION_DURATION, node);
        slideIn.setFromY(50);
        slideIn.setToY(0);

        FadeTransition fadeIn = new FadeTransition(ANIMATION_DURATION, node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition slideEffect = new ParallelTransition(slideIn, fadeIn);
        slideEffect.play();
    }

    /**
     * Aplica efecto de rotación suave al hacer hover.
     */
    public static void applyRotateHoverEffect(Node node) {
        node.setOnMouseEntered(e -> {
            RotateTransition rotateIn = new RotateTransition(HOVER_DURATION, node);
            rotateIn.setFromAngle(0);
            rotateIn.setToAngle(5);
            rotateIn.play();
        });

        node.setOnMouseExited(e -> {
            RotateTransition rotateOut = new RotateTransition(HOVER_DURATION, node);
            rotateOut.setFromAngle(5);
            rotateOut.setToAngle(0);
            rotateOut.play();
        });
    }

    /**
     * Aplica efecto de rebote al hacer clic.
     */
    public static void applyBounceClickEffect(Node node) {
        node.setOnMousePressed(e -> {
            ScaleTransition bounceIn = new ScaleTransition(Duration.millis(100), node);
            bounceIn.setFromX(1.0);
            bounceIn.setFromY(1.0);
            bounceIn.setToX(0.95);
            bounceIn.setToY(0.95);
            bounceIn.play();
        });

        node.setOnMouseReleased(e -> {
            ScaleTransition bounceOut = new ScaleTransition(Duration.millis(100), node);
            bounceOut.setFromX(0.95);
            bounceOut.setFromY(0.95);
            bounceOut.setToX(1.0);
            bounceOut.setToY(1.0);
            bounceOut.play();
        });
    }

    /**
     * Aplica efecto de ondulación (ripple) al hacer clic.
     */
    public static void applyRippleEffect(Node node) {
        node.setOnMousePressed(e -> {
            ScaleTransition ripple = new ScaleTransition(Duration.millis(300), node);
            ripple.setFromX(1.0);
            ripple.setFromY(1.0);
            ripple.setToX(1.1);
            ripple.setToY(1.1);

            FadeTransition fade = new FadeTransition(Duration.millis(300), node);
            fade.setFromValue(1.0);
            fade.setToValue(0.8);
            fade.setAutoReverse(true);
            fade.setCycleCount(2);

            ParallelTransition rippleEffect = new ParallelTransition(ripple, fade);
            rippleEffect.play();
        });
    }

    /**
     * Aplica efecto de desvanecimiento al desaparecer.
     */
    public static void applyFadeOutEffect(Node node, Runnable onFinished) {
        FadeTransition fadeOut = new FadeTransition(FADE_DURATION, node);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        fadeOut.play();
    }

    /**
     * Aplica efecto de escala al aparecer.
     */
    public static void applyScaleInEffect(Node node) {
        node.setScaleX(0);
        node.setScaleY(0);
        node.setOpacity(0);

        ScaleTransition scaleIn = new ScaleTransition(ANIMATION_DURATION, node);
        scaleIn.setFromX(0);
        scaleIn.setFromY(0);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        FadeTransition fadeIn = new FadeTransition(ANIMATION_DURATION, node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition scaleEffect = new ParallelTransition(scaleIn, fadeIn);
        scaleEffect.play();
    }

    /**
     * Aplica efecto de blur suave.
     */
    public static void applyBlurEffect(Node node) {
        GaussianBlur blur = new GaussianBlur();
        blur.setRadius(5);
        node.setEffect(blur);
    }

    /**
     * Remueve efecto de blur.
     */
    public static void removeBlurEffect(Node node) {
        node.setEffect(null);
    }

    /**
     * Aplica efecto de shake (vibración) para errores.
     */
    public static void applyShakeEffect(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setToX(10);
        shake.setAutoReverse(true);
        shake.setCycleCount(6);

        shake.setOnFinished(e -> {
            node.setTranslateX(0);
        });

        shake.play();
    }

    /**
     * Aplica efecto de latido (heartbeat) para elementos importantes.
     */
    public static void applyHeartbeatEffect(Node node) {
        ScaleTransition heartbeat = new ScaleTransition(Duration.millis(600), node);
        heartbeat.setFromX(1.0);
        heartbeat.setFromY(1.0);
        heartbeat.setToX(1.1);
        heartbeat.setToY(1.1);
        heartbeat.setAutoReverse(true);
        heartbeat.setCycleCount(Animation.INDEFINITE);

        heartbeat.play();
    }

    /**
     * Crea una transición suave para cambios de color.
     */
    public static void applyColorTransition(Node node, String fromColor, String toColor) {
        // Esta funcionalidad requeriría CSS transitions que no están disponibles directamente en JavaFX
        // Se puede simular con FadeTransition y cambios de estilo
        FadeTransition fadeOut = new FadeTransition(Duration.millis(100), node);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.5);
        fadeOut.setOnFinished(e -> {
            // Cambiar el estilo aquí
            FadeTransition fadeIn = new FadeTransition(Duration.millis(100), node);
            fadeIn.setFromValue(0.5);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    /**
     * Aplica efecto de loading (rotación continua).
     */
    public static void applyLoadingEffect(Node node) {
        RotateTransition loading = new RotateTransition(Duration.millis(1000), node);
        loading.setFromAngle(0);
        loading.setToAngle(360);
        loading.setCycleCount(Animation.INDEFINITE);
        loading.play();
    }

    /**
     * Detiene todas las animaciones de un nodo.
     */
    public static void stopAllAnimations(Node node) {
        // JavaFX no tiene un método directo para esto, pero podemos resetear propiedades
        node.setScaleX(1.0);
        node.setScaleY(1.0);
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setRotate(0);
        node.setOpacity(1.0);
        node.setEffect(null);
    }
}
