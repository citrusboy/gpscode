/*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
/*::                                                                         :*/
/*::  This routine calculates the distance between two points (given the     :*/
/*::  latitude/longitude of those points). It is being used to calculate     :*/
/*::  the distance between two locations using GeoDataSource (TM) prodducts  :*/
/*::                                                                         :*/
/*::  Definitions:                                                           :*/
/*::    South latitudes are negative, east longitudes are positive           :*/
/*::                                                                         :*/
/*::  Passed to function:                                                    :*/
/*::    lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)  :*/
/*::    lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)  :*/
/*::    unit = the unit you desire for results                               :*/
/*::           where: 'M' is statute miles (default)                         :*/
/*::                  'K' is kilometers                                      :*/
/*::                  'N' is nautical miles                                  :*/
/*::  Worldwide cities and other features databases with latitude longitude  :*/
/*::  are available at https://www.geodatasource.com                         :*/
/*::                                                                         :*/
/*::  For enquiries, please contact sales@geodatasource.com                  :*/
/*::                                                                         :*/
/*::  Official Web site: https://www.geodatasource.com                       :*/
/*::                                                                         :*/
/*::           GeoDataSource.com (C) All Rights Reserved 2018                :*/
/*::                                                                         :*/
/*::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/

import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;

class DistanceCalculator {
    private static final String DELIMITER = ",";

    public static void main(String[] args) throws java.lang.Exception {
//        System.out.println(distance(32.9697, -96.80322, 29.46786, -98.53506, "M") + " Miles\n");
//        System.out.println(distance(32.9697, -96.80322, 29.46786, -98.53506, "K") + " Kilometers\n");
//        System.out.println(distance(32.9697, -96.80322, 29.46786, -98.53506, "N") + " Nautical Miles\n");
//
//        System.out.println(distance(36.49455, -87.86444, 36.68192, -87.91806, "M") + " Miles");

        readFile(args[0]);
    }

    private static void readFile(String filename) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line = null;
        CheckIn previousCheckIn = new CheckIn();
        Totals totals = new Totals();

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.length() > 0 && !line.startsWith("#")) {
                processLine(line, previousCheckIn, totals);
            }
        }

        printDailySummary(totals, previousCheckIn.getTimestamp());

        reader.close();
    }

    private static void processLine(String line, CheckIn previousCheckIn, Totals totals) throws Exception {
        StringBuffer lineBuffer = new StringBuffer(line);
        StringTokenizer tokenizer = new StringTokenizer(line, DELIMITER);
        Date positionTimestamp = parseTimestamp(tokenizer.nextToken());

        tokenizer.nextToken();
        tokenizer.nextToken();

        Double latitude = Double.parseDouble(tokenizer.nextToken());
        Double longitude = Double.parseDouble(tokenizer.nextToken());

        double distance = 0.0;

        if (previousCheckIn.getLatitude() != null) {
            distance = distance(previousCheckIn.getLatitude(), previousCheckIn.getLongitude(),
                    latitude, longitude, "M");

            if (!areSameDay(previousCheckIn.getTimestamp(), positionTimestamp)) {
                printDailySummary(totals, previousCheckIn.getTimestamp());
                totals.resetDailyTotals();
            } else {
                totals.accumulateDailyDistance(distance, positionTimestamp.getTime() - previousCheckIn.getTimestamp().getTime());
            }

            totals.accumulate(distance);
        }

        lineBuffer.append("," + distance + "," + totals.getDistance());


        System.out.println(lineBuffer);

        previousCheckIn.setLatitude(latitude);
        previousCheckIn.setLongitude(longitude);
        previousCheckIn.setTimestamp(positionTimestamp);
    }

    private static void printDailySummary(Totals totals, Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("E MM/dd/yyyy");

        System.out.println(formatter.format(date) +
                "; distance: " + formatDecimal(totals.getDailyDistance()) + "; moving time: " +
                formatTimeDelta(totals.getDailyMovingTime()) + "; speed: " +
                formatDecimal(totals.getDailyDistance() / (totals.getDailyMovingTime() / 3600)) + " mph");

    }

    private static String formatTimeDelta(double delta) {
        long hours = (long) delta / 3600;
        long minutes = (long) (delta % 3600) / 60;

        return hours + " hrs " + minutes + " min";
    }

    private static double formatDecimal(double number) {
        return (Math.round(number * 10) / 10.0);
    }

    private static boolean areSameDay(Date firstDate, Date secondDate) {
        Calendar firstCalendar = new GregorianCalendar();
        firstCalendar.setTime(firstDate);

        Calendar secondCalendar = new GregorianCalendar();
        secondCalendar.setTime(secondDate);

        return firstCalendar.get(Calendar.DAY_OF_YEAR) == secondCalendar.get(Calendar.DAY_OF_YEAR);
    }

    private static final Date parseTimestamp(String dateStr) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        return formatter.parse(dateStr);
    }

/*
Timestamp,ESN,Check-in type,Lat,Long,Unknown,Message,Unknown,Distance since last check-in,Distance this day,Cumulative distance,Speed since last check-in,Speed this day,Speed for the trip (moving hours)
05/31/2019 08:24:56,0-3020839,UNLIMITED-TRACK,36.57510,-87.89905,"","",null
05/31/2019 08:15:01,0-3020839,UNLIMITED-TRACK,36.56844,-87.89409,"","",null
*/

    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit == "K") {
                dist = dist * 1.609344;
            } else if (unit == "N") {
                dist = dist * 0.8684;
            }
            return (dist);
        }
    }

    private static class CheckIn {
        private Double latitude;
        private Double longitude;
        private Date timestamp;

        public CheckIn() {
            latitude = null;
            longitude = null;
            timestamp = null;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        private Date getTimestamp() {
            return timestamp;
        }

        public void setLatitude(double inLatitude) {
            latitude = inLatitude;
        }

        public void setLongitude(double inLongitude) {
            longitude = inLongitude;
        }

        public void setTimestamp(Date inTimestamp) {
            timestamp = inTimestamp;
        }
    }

    private static class Totals {
        private double distance;
        private double dailyDistance;
        private double dailyMovingTimeInSec;


        public double getDistance() {
            return distance;
        }

        private double getDailyMovingTime() {
            return dailyMovingTimeInSec;
        }

        public double getDailyDistance() {
            return dailyDistance;
        }

        public void accumulateDailyDistance(double distance, double time) {
            if (distance > 0.01) {
                dailyDistance += distance;
                this.dailyMovingTimeInSec += (time / 1000);
            }
        }

        public void resetDailyTotals() {
            dailyDistance = 0;
            dailyMovingTimeInSec = 0;
        }

        public void accumulate(double distance) {
            if (distance > 0.05) {
                this.distance += distance;
            }
        }
    }
}