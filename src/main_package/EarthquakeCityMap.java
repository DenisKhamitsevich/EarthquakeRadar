package main_package;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;

/**
 * EarthquakeCityMap An application with an interactive map displaying
 * earthquake data. Author: UC San Diego Intermediate Software Development MOOC
 * team
 * 
 * @author Denis Khamitsevich
 */
public class EarthquakeCityMap extends PApplet {

	private static final long serialVersionUID = 1L;

	// feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";

	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";

	// The map
	private UnfoldingMap map;

	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;

	private CommonMarker lastSelected;
	private CommonMarker lastClicked;
	private String type_key;
	Object[] array_of_markers;
	private boolean object_clicked = false;

	public void setup() {
		size(1230, 700, OPENGL);
		map = new UnfoldingMap(this, 200, 50, 650, 600, new Google.GoogleMapProvider());	
		MapUtils.createDefaultEventDispatcher(this, map);
		map.minScale = 3;

		// load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);

		// read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for (Feature city : cities) {
			cityMarkers.add(new CityMarker(city));
		}

		// read in earthquake RSS feed
		List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
		quakeMarkers = new ArrayList<Marker>();

		for (PointFeature feature : earthquakes) {
			if (isLand(feature)) {
				quakeMarkers.add(new LandQuakeMarker(feature));
			}
			else {
				quakeMarkers.add(new OceanQuakeMarker(feature));
			}
		}
		map.addMarkers(quakeMarkers);
		map.addMarkers(cityMarkers);
		sortAndPrint(20);

	} 

	public void draw() {
		background(192, 192, 192);
		map.draw();
		addKey();
		addSortedQuakes(20);
		draw_marker_title(quakeMarkers);
		draw_marker_title(cityMarkers);

	}

	//draws the title of the selected quake
	private void draw_marker_title(List<Marker> markers) {
		for (Marker m : markers) {
			CommonMarker marker = (CommonMarker) m;
			if (marker.isInside(map, mouseX, mouseY)) {
				if ((mouseX < 200) || (mouseX > 850) || (mouseY > 650) || (mouseY < 50)) {
					marker.setSelected(false);
					return;
				}
				if (!marker.isHidden())
					marker.showTitle(g, mouseX, mouseY);
				return;

			}
		}

	}


	//sorts quakes
	private void sortAndPrint(int numToPrint) {
		array_of_markers = quakeMarkers.toArray();
		Arrays.sort(array_of_markers);

	}

	//adds sorted quakes to the map
	private void addSortedQuakes(int numToPrint) {
		fill(0);
		textSize(12);
		int amount = Integer.min(numToPrint, array_of_markers.length);
		for (int i = 0; i < amount; i++) {
			text(array_of_markers[i].toString(), 880, 110 + i * 27);
		}

	}

	/**
	 * Event handler that gets called automatically when the mouse moves.
	 */
	@Override
	public void mouseMoved() {
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;

		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
	}

	// If there is a marker selected
	private void selectMarkerIfHover(List<Marker> markers) {
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}

		for (Marker m : markers) {
			CommonMarker marker = (CommonMarker) m;
			if (marker.isInside(map, mouseX, mouseY)) {

				if ((mouseX < 200) || (mouseX > 850) || (mouseY > 650) || (mouseY < 50)) {
					marker.setSelected(false);
					return;
				}
				if (marker.isHidden())
					return;
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}

	}

	/**
	 * The event handler for mouse clicks It will display an earthquake and its
	 * threat circle of cities Or if a city is clicked, it will display all the
	 * earthquakes where the city is in the threat circle
	 */
	@Override
	public void mouseClicked() {
		if (mouseX >= 880) {
			if (mouseY <= 650) {
				int index = (mouseY - 110) / 27;
				int remainder = (mouseY - 110) % 27;
				if (remainder <= 6)
					show_top_quake(index);
				else {
					hideMarkers(false);
					object_clicked = false;
				}
			} else
				hideMarkers(false);

		} else {

			if (mouseX >= 200) {

				if (object_clicked) {
					hideMarkers(false);
					object_clicked = false;
					lastClicked = null;
					return;
				}
				if (lastClicked != null) {

					lastClicked = null;

				} else {

					checkEarthquakesForClick();
					if (lastClicked == null) {
						checkCitiesForClick();
					}
					if (lastClicked != null)
						object_clicked = true;

				}
			} else {
				type_key = "not_found";
				if ((mouseX >= 75) && (mouseX <= 140) && (mouseY >= 100) && (mouseY <= 106))
					type_key = "citymarker";
				else if ((mouseX >= 75) && (mouseX <= 145) && (mouseY >= 120) && (mouseY <= 126))
					type_key = "landquake";
				else if ((mouseX >= 75) && (mouseX <= 150) && (mouseY >= 140) && (mouseY <= 146))
					type_key = "oceanquake";
				else if ((mouseX >= 75) && (mouseX <= 115) && (mouseY >= 190) && (mouseY <= 196))
					type_key = "shallow";
				else if ((mouseX >= 75) && (mouseX <= 145) && (mouseY >= 210) && (mouseY <= 216))
					type_key = "intermediate";
				else if ((mouseX >= 75) && (mouseX <= 105) && (mouseY >= 230) && (mouseY <= 236))
					type_key = "deep";
				else if ((mouseX >= 75) && (mouseX <= 125) && (mouseY >= 250) && (mouseY <= 256))
					type_key = "past_hour";
				if (type_key != "not_found") {
					change_key_markers(type_key);
					object_clicked = true;
				} else {
					hideMarkers(false);
					object_clicked = false;
					lastClicked = null;
				}

			}
		}

	}

	//unhides the selected marker and hides all the others 
	public void show_top_quake(int index) {
		hideMarkers(true);
		object_clicked = true;
		for (Marker m : quakeMarkers) {
			if (m == array_of_markers[index]) {
				m.setHidden(false);
				lastSelected = (CommonMarker) m;
				m.setSelected(true);
				return;
			}

		}

	}

	//hides/unhides markers depending on the choice of the user
	private void change_key_markers(String value) {
		for (Marker item : cityMarkers) {
			if (value != "citymarker")
				item.setHidden(true);
			else
				item.setHidden(false);
		}
		if ((value == "shallow") || (value == "intermediate") || (value == "deep") || (value == "past_hour"))
			change_key_depth_markers(value);
		else {
			for (Marker item : quakeMarkers) {
				if ((item instanceof LandQuakeMarker)) {
					if (value != "landquake")
						item.setHidden(true);
					else
						item.setHidden(false);
				} else {
					if (value != "oceanquake")
						item.setHidden(true);
					else
						item.setHidden(false);
				}

			}
		}

	}

	//helper method for change_key_markers
	private void change_key_depth_markers(String value) {

		for (Marker item : quakeMarkers) {
			float depth = Float.parseFloat(item.getProperty("depth").toString());
			switch (value) {
			case "shallow": {
				if (depth < 70)
					item.setHidden(false);
				else
					item.setHidden(true);
				break;
			}
			case "intermediate": {
				if ((depth >= 70) && (depth <= 300))
					item.setHidden(false);
				else
					item.setHidden(true);
				break;
			}
			case "deep": {
				if (depth > 300)
					item.setHidden(false);
				else
					item.setHidden(true);
				break;
			}
			case "past_hour": {
				String age = item.getStringProperty("age");
				if ("Past Hour".equals(age) || "Past Day".equals(age)) {
					item.setHidden(false);
				} else
					item.setHidden(true);

			}
			}

		}
	}

	// Helper method that will check if a city marker was clicked on
	// and respond appropriately
	private void checkCitiesForClick() {
		if (lastClicked != null)
			return;
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker) marker;
				
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker) mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation()) > quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}
	}

	// Helper method that will check if an earthquake marker was clicked on
	// and respond appropriately
	private void checkEarthquakesForClick() {
		if (lastClicked != null)
			return;
		
		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker) m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation()) > marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				return;
			}
		}
	}

	// loop over and hide/unhide all markers
	private void hideMarkers(boolean ind) {
		for (Marker marker : quakeMarkers) {
			marker.setHidden(ind);
		}

		for (Marker marker : cityMarkers) {
			marker.setHidden(ind);
		}
	}

	// helper method to draw key in GUI
	private void addKey() {
		fill(255, 250, 240);
		int xbase = 25;
		int ybase = 50;
		rect(xbase, ybase, 150, 250);
		rect(870, 50, 350, 600);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(15);
		text("Largest earthquakes of the past week", 900, 65);
		text("Earthquake Key", xbase + 20, ybase + 20);
		textSize(12);

		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase - CityMarker.TRI_SIZE, tri_xbase - CityMarker.TRI_SIZE,
				tri_ybase + CityMarker.TRI_SIZE, tri_xbase + CityMarker.TRI_SIZE, tri_ybase + CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);
		text("Land Quake", xbase + 50, ybase + 70);
		text("Ocean Quake", xbase + 50, ybase + 90);
		text("Size ~ Magnitude", xbase + 25, ybase + 110);

		fill(255, 255, 255);
		ellipse(xbase + 35, ybase + 70, 10, 10);
		rect(xbase + 35 - 5, ybase + 90 - 5, 10, 10);

		fill(color(255, 255, 0));
		ellipse(xbase + 35, ybase + 140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase + 35, ybase + 160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase + 35, ybase + 180, 12, 12);
		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase + 50, ybase + 140);
		text("Intermediate", xbase + 50, ybase + 160);
		text("Deep", xbase + 50, ybase + 180);

		text("Past hour", xbase + 50, ybase + 200);

		fill(255, 255, 255);
		int centerx = xbase + 35;
		int centery = ybase + 200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx - 8, centery - 8, centerx + 8, centery + 8);
		line(centerx - 8, centery + 8, centerx + 8, centery - 8);

	}

	// Checks whether this quake occurred on land. If it did, it sets the
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.
	private boolean isLand(PointFeature earthquake) {
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}

		return false;
	}

	// helper method to test whether a given earthquake is in a given country
	private boolean isInCountry(PointFeature earthquake, Marker country) {
	
		Location checkLoc = earthquake.getLocation();

		if (country.getClass() == MultiMarker.class) {

			for (Marker marker : ((MultiMarker) country).getMarkers()) {

				if (((AbstractShapeMarker) marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));

					return true;
				}
			}
		}

		else if (((AbstractShapeMarker) country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));

			return true;
		}
		return false;
	}

}
