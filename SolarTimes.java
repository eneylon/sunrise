import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import static java.lang.Math.*;

/**
 * Calculates sunrise and sunset times for a fixed location using the USNO algorithm.
 *
 * <p>Latitude is positive North, negative South.
 * Longitude is positive East, negative West.
 *
 * <p>Returns an empty {@link Optional} when the sun does not rise or set on the
 * requested day (polar summer/winter).
 *
 * <pre>{@code
 *   SolarTimes dublin = new SolarTimes(53.3498, -6.2603);
 *   dublin.getTodaysSunrise(ZoneId.of("Europe/Dublin")).ifPresent(System.out::println);
 * }</pre>
 */
public class SolarTimes {

    /** Zenith for official sunrise/sunset: 90°50' accounts for refraction and solar disc radius. */
    private static final double ZENITH = 90.833;

    private static final double TO_RAD   = PI / 180.0;
    private static final double FROM_RAD = 180.0 / PI;

    /** Which solar event to calculate. */
    public enum SunEvent { SUNRISE, SUNSET }

    private final double latitude;
    private final double longitude;

    /**
     * @param latitude   decimal degrees, -90 to +90 (positive = North)
     * @param longitude  decimal degrees, -180 to +180 (positive = East)
     * @throws IllegalArgumentException if coordinates are out of range
     */
    public SolarTimes(double latitude, double longitude) {
        if (latitude  < -90  || latitude  > 90)  throw new IllegalArgumentException("Latitude must be between -90 and 90, got: "   + latitude);
        if (longitude < -180 || longitude > 180)  throw new IllegalArgumentException("Longitude must be between -180 and 180, got: " + longitude);
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns today's sunrise time in the given time zone, or empty if the sun
     * does not rise today at this location.
     */
    public Optional<LocalTime> getTodaysSunrise(ZoneId zone) {
        return calculate(today(zone), SunEvent.SUNRISE, zone);
    }

    /**
     * Returns today's sunset time in the given time zone, or empty if the sun
     * does not set today at this location.
     */
    public Optional<LocalTime> getTodaysSunset(ZoneId zone) {
        return calculate(today(zone), SunEvent.SUNSET, zone);
    }

    /**
     * Returns tomorrow's sunrise time in the given time zone, or empty if the
     * sun does not rise tomorrow at this location.
     */
    public Optional<LocalTime> getTomorrowsSunrise(ZoneId zone) {
        return calculate(today(zone).plusDays(1), SunEvent.SUNRISE, zone);
    }

    /**
     * Returns tomorrow's sunset time in the given time zone, or empty if the
     * sun does not set tomorrow at this location.
     */
    public Optional<LocalTime> getTomorrowsSunset(ZoneId zone) {
        return calculate(today(zone).plusDays(1), SunEvent.SUNSET, zone);
    }

    /**
     * Calculates the time of a solar event for an arbitrary date and time zone.
     *
     * @param date  the date to calculate for
     * @param event {@link SunEvent#SUNRISE} or {@link SunEvent#SUNSET}
     * @param zone  time zone for the returned {@link LocalTime}
     * @return the event time, or empty if the sun does not rise/set on this date
     */
    public Optional<LocalTime> calculate(LocalDate date, SunEvent event, ZoneId zone) {
        boolean morn = (event == SunEvent.SUNRISE);
        int dayOfYear = date.getDayOfYear();

        double lngHour = longitude / 15.0;
        double t = morn
                ? dayOfYear + (6.0  - lngHour) / 24.0
                : dayOfYear + (18.0 - lngHour) / 24.0;

        // Sun's mean anomaly
        double M = (0.9856 * t) - 3.289;

        // Sun's true longitude, normalised to [0, 360)
        double L = M + (1.916 * sin(TO_RAD * M)) + (0.020 * sin(2 * TO_RAD * M)) + 282.634;
        L = normalise(L, 360);

        // Right ascension, adjusted to the same quadrant as L
        double RA = FROM_RAD * atan(0.91764 * tan(TO_RAD * L));
        RA = normalise(RA, 360);
        RA = (RA + (floor(L / 90) * 90 - floor(RA / 90) * 90)) / 15.0;

        // Sun's declination
        double sinDec = 0.39782 * sin(TO_RAD * L);
        double cosDec = cos(asin(sinDec));

        // Local hour angle — value outside [-1, 1] means no sunrise/sunset today
        double cosH = (cos(TO_RAD * ZENITH) - sinDec * sin(TO_RAD * latitude))
                    / (cosDec * cos(TO_RAD * latitude));

        if (cosH > 1 || cosH < -1) return Optional.empty();

        double H = morn
                ? (360 - FROM_RAD * acos(cosH)) / 15.0
                : (FROM_RAD * acos(cosH)) / 15.0;

        // Local mean time, then UTC, normalised to [0, 24)
        double T  = H + RA - (0.06571 * t) - 6.622;
        double UT = normalise(T - lngHour, 24);

        // Convert UTC decimal hours to a LocalTime in the requested zone
        int utcHour   = (int) UT;
        int utcMinute = (int) ((UT - utcHour) * 60);
        LocalTime utcTime = LocalTime.of(utcHour, utcMinute);

        // Shift from UTC to the target zone on the correct date
        LocalTime zonedTime = date.atTime(utcTime)
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(zone)
                .toLocalTime();

        return Optional.of(zonedTime);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static LocalDate today(ZoneId zone) {
        return LocalDate.now(zone);
    }

    /** Normalises {@code value} to [0, {@code range}) without loops. */
    private static double normalise(double value, double range) {
        return value - range * floor(value / range);
    }
}