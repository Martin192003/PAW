package ar.edu.itba.paw.model.common;


import java.time.LocalTime;

public class TimeRange {
    private LocalTime start;
    private LocalTime end;

    public TimeRange(LocalTime start, LocalTime end) {
        this.start = start; this.end = end;
    }

    public boolean includes(LocalTime t) {
        if (start.equals(end)) return false;
        if (start.isBefore(end)) {
            return !t.isBefore(start) && t.isBefore(end);
        } else {
            return !t.isBefore(start) || t.isBefore(end);
        }
    }

    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }

    public void setStart(LocalTime start) { this.start = start; }
    public void setEnd(LocalTime end) { this.end = end; }

    @Override
    public String toString() {
        return start + " - " + end;
    }

}