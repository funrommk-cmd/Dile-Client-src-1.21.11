package dile.ru.utils.render.animation;

@FunctionalInterface
public interface Easing {
    double ease(double value);
}