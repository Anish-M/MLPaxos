package server;

import java.util.*;
import server.LogEntry;


@SuppressWarnings("hiding")
interface Multimap<K, V> {
    void put(K key, V value);

    Collection<V> get(K key);

    boolean containsKey(K key);

    boolean remove(K key, V value);

    boolean removeAll(K key);

    Set<K> keySet();

    Collection<V> values();

    void clear();
}

// LogEntry, Integer

class ArrayListMultimap<K, V> implements Multimap<K, V> {
    private final Map<LogEntry, List<Integer>> map;

    private ArrayListMultimap() {
        this.map = new HashMap<>();
    }

    public static ArrayListMultimap<LogEntry, Integer> create() {
        return new ArrayListMultimap<>();
    }

    @Override
    public void put(K key, V value) {
        // cast key to LogEntry and value to Integer
        LogEntry logEntry = (LogEntry) key;
        Integer integer = (Integer) value;

        for (Map.Entry<LogEntry, List<Integer>> entry : map.entrySet()) {
            if (entry.getKey().getSlot() == logEntry.getSlot()) {
                entry.getValue().add(integer);
                return;
            }
        }

        List<Integer> list = new ArrayList<>();
        list.add(integer);
        map.put(logEntry, list);
    }

    @Override
    public Collection<V> get(K key) {
        // cast key to LogEntry
        LogEntry logEntry = (LogEntry) key;

        for (Map.Entry<LogEntry, List<Integer>> entry : map.entrySet()) {
            if (entry.getKey().getSlot() == logEntry.getSlot()) {
                return (Collection<V>) entry.getValue();
            }
        }

        return new ArrayList<>();
    }

    @Override
    public boolean containsKey(K key) {
        // cast key to LogEntry
        LogEntry logEntry = (LogEntry) key;

        for (Map.Entry<LogEntry, List<Integer>> entry : map.entrySet()) {
            if (entry.getKey().getSlot() == logEntry.getSlot()) {
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean remove(K key, V value) {
        // cast key to LogEntry and value to Integer
        LogEntry logEntry = (LogEntry) key;
        Integer integer = (Integer) value;

        for (Map.Entry<LogEntry, List<Integer>> entry : map.entrySet()) {
            if (entry.getKey().getSlot() == logEntry.getSlot()) {
                return entry.getValue().remove(integer);
            }
        }

        return false;
    }


    @Override
    public boolean removeAll(K key) {
        // cast key to LogEntry
        LogEntry logEntry = (LogEntry) key;

        for (Map.Entry<LogEntry, List<Integer>> entry : map.entrySet()) {
            if (entry.getKey().getSlot() == logEntry.getSlot()) {
                map.remove(entry.getKey());
                return true;
            }
        }

        return false;
    }


    @Override
    public Set<K> keySet() {
        for (Map.Entry<LogEntry, List<Integer>> entry : map.entrySet()) {
            return (Set<K>) entry.getKey();
        }
        return new HashSet<>();
    }


    @Override
    public Collection<V> values() {
        for (Map.Entry<LogEntry, List<Integer>> entry : map.entrySet()) {
            return (Collection<V>) entry.getValue();
        }
        return new ArrayList<>();
    }


    @Override
    public void clear() {
        for (Map.Entry<LogEntry, List<Integer>> entry : map.entrySet()) {
            map.remove(entry.getKey());
        }
    }
}
