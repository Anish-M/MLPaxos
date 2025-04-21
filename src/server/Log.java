package server;
import java.io.Serializable;
import java.util.SortedMap;

public class Log implements Serializable {
    private final SortedMap<Integer, LogEntry> log;

    public Log(SortedMap<Integer, LogEntry> log) {
        this.log = log;
    }

    public SortedMap<Integer, LogEntry> getLog() {
        return log;
    }

    public LogEntry getLogEntry(int index) {
        return log.get(index);
    }

    public void setLogEntry(int index, LogEntry entry) {
        log.put(index, entry);
    }

    public int size() {
        return log.size();
    }

    public void remove(int index) {
        log.remove(index);
    }

    public int getLastIndex() {
        return log.lastKey();
    }

    public LogEntry getLastEntry() {
        return log.get(log.lastKey());
    }

    public Log subLog(int fromIndex, int toIndex) {
        return new Log(log.subMap(fromIndex, toIndex));
    }

    public Log subLog(int fromIndex) {
        return new Log(log.tailMap(fromIndex));
    }

    public Log subLog() {
        return new Log(log);
    }

    public boolean containsIndex(int index) {
        return log.containsKey(index);
    }
}
