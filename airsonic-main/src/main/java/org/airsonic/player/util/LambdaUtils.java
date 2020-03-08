package org.airsonic.player.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LambdaUtils {
    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, S, E extends Exception> {
        S apply(T t) throws E;
    }

    @FunctionalInterface
    public interface ThrowingBiFunction<T, R, S, E extends Exception> {
        S apply(T t, R r) throws E;
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, R, E extends Exception> {
        void accept(T t, R r) throws E;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }

    public static <T, E extends Exception> Supplier<T> uncheckSupplier(ThrowingSupplier<T, E> throwingSupplier) {
        return () -> {
            try {
                return throwingSupplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, E extends Exception> Consumer<T> uncheckConsumer(ThrowingConsumer<T, E> throwingConsumer) {
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, R, E extends Exception> BiConsumer<T, R> uncheckBiConsumer(
            ThrowingBiConsumer<T, R, E> throwingBiConsumer) {
        return (i, j) -> {
            try {
                throwingBiConsumer.accept(i, j);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, S, E extends Exception> Function<T, S> uncheckFunction(
            ThrowingFunction<T, S, E> throwingFunction) {
        return i -> {
            try {
                return throwingFunction.apply(i);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, R, S, E extends Exception> BiFunction<T, R, S> uncheckBiFunction(
            ThrowingBiFunction<T, R, S, E> throwingBiFunction) {
        return (i, j) -> {
            try {
                return throwingBiFunction.apply(i, j);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
