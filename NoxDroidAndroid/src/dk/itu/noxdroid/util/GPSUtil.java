package dk.itu.noxdroid.util;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;

public class GPSUtil {
	// instantiate the calculator
	private static GeodeticCalculator geoCalc = new GeodeticCalculator();
			// select a reference elllipsoid
	private static Ellipsoid reference = Ellipsoid.WGS84;
	
	/***
	 * 
	 * @param gc1 GPS coordinate 
	 * @param gc2 GPS coordinate
	 * @return the distance between two gps coordinates 
	 */
	public static double getGPSDelta(GlobalCoordinates gc1, GlobalCoordinates gc2) {
		if (geoCalc == null) {
			geoCalc = new  GeodeticCalculator();
		}
		
		GeodeticCurve geoCurve = geoCalc.calculateGeodeticCurve(reference, gc1, gc2);
		
		return geoCurve.getEllipsoidalDistance() / 1000000.0 ;
	}
}
