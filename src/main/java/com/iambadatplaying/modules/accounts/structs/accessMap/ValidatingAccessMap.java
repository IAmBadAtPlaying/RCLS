package com.iambadatplaying.modules.accounts.structs.accessMap;

import com.iambadatplaying.modules.accounts.structs.Lockable;

import java.util.*;

public class ValidatingAccessMap<K, V extends Lockable> implements Map<K, V> {

    private final Map<K, V> mapImplementation;

    public ValidatingAccessMap(Map<K, V> map) {
        this.mapImplementation = map;
    }

    public ValidatingAccessMap() {
        this.mapImplementation = new HashMap<>();
    }

    public static <K, V extends Lockable> ValidatingAccessMap<K, V> of(Map<K, V> map) {
        return new ValidatingAccessMap<>(map);
    }

    @Override
    public int size() {
        return mapImplementation.size();
    }

    @Override
    public boolean isEmpty() {
        return mapImplementation.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return mapImplementation.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return mapImplementation.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return mapImplementation.get(key);
    }

    @Override
    public V put(K key, V value) throws InvalidAccessException {
        V prevValue = mapImplementation.get(key);
        if (prevValue != null && !prevValue.isUnlocked()) {
            throw new InvalidAccessException("Cannot override locked value");
        }
        mapImplementation.put(key, value);
        return value;
    }

    @Override
    public V remove(Object key) throws InvalidAccessException {
        V prevValue = mapImplementation.get(key);
        if (prevValue != null && !prevValue.isUnlocked()) {
            throw new InvalidAccessException("Cannot remove locked value");
        }
        return mapImplementation.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() throws InvalidAccessException {
        //Check if all values are unlocked
        for (V value : mapImplementation.values()) {
            if (!value.isUnlocked()) {
                throw new InvalidAccessException("Cannot clear map with locked values");
            }
        }
        mapImplementation.clear();
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(mapImplementation.keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(mapImplementation.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(mapImplementation.entrySet());
    }
}
