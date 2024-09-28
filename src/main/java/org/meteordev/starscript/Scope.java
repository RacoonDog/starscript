package org.meteordev.starscript;

import org.meteordev.starscript.utils.SFunction;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Scope {
    /** Creates a new {@link LocalScope} that inherits the variables of the given parent scope. */
    public static LocalScope of(Scope parent) {
        return new LocalScope(parent);
    }

    /** Sets a variable supplier for the provided name. <br><br> See {@link ValueMap#set(String, Supplier)} for dot notation, although local scopes do not support them. */
    public abstract Scope set(String name, Supplier<Value> supplier);

    /** Sets a variable supplier that always returns the same value for the provided name. <br><br> See {@link ValueMap#set(String, Supplier)} for dot notation, although local scopes do not support them. */
    public Scope set(String name, Value value) {
        return set(name, () -> value);
    }

    /** Sets a boolean variable supplier that always returns the same value for the provided name. <br><br> See {@link ValueMap#set(String, Supplier)} for dot notation, although local scopes do not support them. */
    public Scope set(String name, boolean bool) {
        return set(name, Value.bool(bool));
    }

    /** Sets a number variable supplier that always returns the same value for the provided name. <br><br> See {@link ValueMap#set(String, Supplier)} for dot notation, although local scopes do not support them. */
    public Scope set(String name, double number) {
        return set(name, Value.number(number));
    }

    /** Sets a string variable supplier that always returns the same value for the provided name. <br><br> See {@link ValueMap#set(String, Supplier)} for dot notation, although local scopes do not support them. */
    public Scope set(String name, String string) {
        return set(name, Value.string(string));
    }

    /** Sets a function variable supplier that always returns the same value for the provided name. <br><br> See {@link ValueMap#set(String, Supplier)} for dot notation, although local scopes do not support them. */
    public Scope set(String name, SFunction function) {
        return set(name, Value.function(function));
    }

    /** Sets a map variable supplier that always returns the same value for the provided name. <br><br> See {@link ValueMap#set(String, Supplier)} for dot notation, although local scopes do not support them. */
    public Scope set(String name, ValueMap map) {
        return set(name, Value.map(map));
    }

    /** Sets an object variable supplier that always returns the same value for the provided name. <br><br> See {@link ValueMap#set(String, Supplier)} for dot notation, although local scopes do not support them. */
    public Scope set(String name, Object object) {
        return set(name, Value.object(object));
    }

    /** Removes all values from this scope's variables. */
    public abstract void clear();

    /** Removes a single value with the specified name from the variables and returns the removed value. <br> If this is a local scope and the variable is contained within the parent scope, it does not mutate the parent scope's variables but rather override the output of this scope's {@link Scope#get(String)}. <br><br> See {@link ValueMap#remove(String)} for dot notation, although local scopes do not support them. */
    public abstract Supplier<Value> remove(String name);

    /** Gets the variable supplier for the provided name. <br><br> See {@link ValueMap#remove(String)} for dot notation, although local scopes do not support them. */
    public abstract Supplier<Value> get(String name);

    /** Gets the variable supplier for the provided name. */
    public abstract Supplier<Value> getRaw(String name);

    /** Returns an unmodifiable set of all variable names. */
    public abstract Set<String> keys();

    /** Returns a modifiable set of all variable names provided by this scope. */
    public abstract Set<String> scopedKeys();

    /** Returns the underlying {@link ValueMap} for this scope's variables. */
    public abstract ValueMap getVariables();

    /** Returns a new {@link LocalScope} with this scope as its parent. */
    public LocalScope scope() {
        return of(this);
    }

    public static class GlobalScope extends Scope {
        private final ValueMap values = new ValueMap();

        @Override
        public Scope set(String name, Supplier<Value> supplier) {
            values.set(name, supplier);
            return this;
        }

        @Override
        public void clear() {
            values.clear();
        }

        @Override
        public Supplier<Value> remove(String name) {
            return values.remove(name);
        }

        @Override
        public Supplier<Value> get(String name) {
            return values.get(name);
        }

        @Override
        public Supplier<Value> getRaw(String name) {
            return values.getRaw(name);
        }

        @Override
        public Set<String> keys() {
            return Collections.unmodifiableSet(values.keys());
        }

        @Override
        public Set<String> scopedKeys() {
            return values.keys();
        }

        @Override
        public ValueMap getVariables() {
            return values;
        }
    }

    /** Inherits variables from another scope, which can be a global or a local scope */
    public static class LocalScope extends Scope implements AutoCloseable {
        private final Scope parent;
        private final ValueMap overridenValues = new ValueMap();
        private final Set<String> removedKeys = ConcurrentHashMap.newKeySet();

        public LocalScope(Scope parent) {
            this.parent = parent;
        }

        /** Returns a modifiable set of all the parent scope's variable names that are removed from this scope. */
        public Set<String> removedKeys() {
            return removedKeys;
        }

        @Override
        public Scope set(String name, Supplier<Value> supplier) {
            if (name.contains(".")) throw new UnsupportedOperationException("Local scopes do not support dot notation");
            overridenValues.set(name, supplier);
            return this;
        }

        @Override
        public void clear() {
            overridenValues.clear();
            removedKeys.clear();
        }

        @Override
        public Supplier<Value> remove(String name) {
            if (name.contains(".")) throw new UnsupportedOperationException("Local scopes do not support dot notation");
            Supplier<Value> valueSupplier = getRaw(name);
            if (valueSupplier != null) removedKeys.add(name);
            return valueSupplier;
        }

        @Override
        public Supplier<Value> get(String name) {
            if (name.contains(".")) throw new UnsupportedOperationException("Local scopes do not support dot notation");
            return getRaw(name);
        }

        @Override
        public Supplier<Value> getRaw(String name) {
            if (removedKeys.contains(name)) return null;
            Supplier<Value> valueSupplier = overridenValues.getRaw(name);
            return  valueSupplier != null ? valueSupplier : parent.getRaw(name);
        }

        @Override
        public Set<String> keys() {
            return Stream.concat(overridenValues.keys().stream(),
                parent.keys().stream().filter(s -> !removedKeys.contains(s))
            ).collect(Collectors.toSet());
        }

        @Override
        public Set<String> scopedKeys() {
            return overridenValues.keys();
        }

        @Override
        public ValueMap getVariables() {
            return overridenValues;
        }

        // no-op, required to support try-with-resources syntax
        @Override
        public void close() {}
    }
}
