package dev.thatredox.chunky.animate.util;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObservableValue<T> {
    public interface ChangeListener<T> {
        void onChange(T newValue);
    }

    public static class ObservableInterface<T> {
        private final ObservableValue<T> value;

        protected ObservableInterface(ObservableValue<T> value) {
            this.value = value;
        }

        public T getValue() {
            return value.getValue();
        }

        public void addListener(ChangeListener<T> listener) {
            value.addListener(listener);
        }

        public void removeListener(ChangeListener<T> listener) {
            value.removeListener(listener);
        }
    }

    protected final ExecutorService executor = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "ObservableValue Update Thread"));

    protected final CopyOnWriteArraySet<ChangeListener<T>> changeListeners = new CopyOnWriteArraySet<>();
    protected T value;

    public ObservableValue(T initialValue) {
        this.value = initialValue;
    }

    public ObservableInterface<T> getObservableInterface() {
        return new ObservableInterface<>(this);
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        this.update(value);
    }

    public void update() {
        this.update(this.value);
    }

    public void update(T newValue) {
        changeListeners.forEach(listener -> executor.execute(() -> listener.onChange(newValue)));
    }

    public void addListener(ChangeListener<T> listener) {
        changeListeners.add(listener);
    }

    public void removeListener(ChangeListener<T> listener) {
        changeListeners.remove(listener);
    }
}
