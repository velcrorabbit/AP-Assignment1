package solution;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import baseclasses.DataLoadingException;
import baseclasses.IRouteDAO;
import baseclasses.Route;

/**
 * The RouteDAO parses XML files of route information, each route specifying
 * where the airline flies from, to, and on which day of the week
 */
public class RouteDAO implements IRouteDAO {
	
	List<Route> routes = new ArrayList<>();

	/**
	 * Finds all flights that depart on the specified day of the week
	 * @param dayOfWeek A three letter day of the week, e.g. "Tue"
	 * @return A list of all routes that depart on this day
	 */
	@Override
	public List<Route> findRoutesByDayOfWeek(String dayOfWeek) {
		List<Route> currentRoutes = new ArrayList<>();
		
		for (Route route : routes) {
			if (route.getDayOfWeek().equals(dayOfWeek)) {
				currentRoutes.add(route);
			}
		}
		
		return currentRoutes;

	}

	/**
	 * Finds all of the flights that depart from a specific airport on a specific day of the week
	 * @param airportCode the three letter code of the airport to search for, e.g. "MAN"
	 * @param dayOfWeek the three letter day of the week code to searh for, e.g. "Tue"
	 * @return A list of all routes from that airport on that day
	 */
	@Override
	public List<Route> findRoutesByDepartureAirportAndDay(String airportCode, String dayOfWeek) {
		List<Route> currentRoutes = new ArrayList<>();
		
		for (Route route : routes) {
			if (route.getDayOfWeek().equals(dayOfWeek) && route.getDepartureAirportCode().equals(airportCode)) {
				currentRoutes.add(route); 
			}
		}
		
		return currentRoutes;
	}

	/**
	 * Finds all of the flights that depart from a specific airport
	 * @param airportCode the three letter code of the airport to search for, e.g. "MAN"
	 * @return A list of all of the routes departing the specified airport
	 */
	@Override
	public List<Route> findRoutesDepartingAirport(String airportCode) {

		List<Route> currentRoutes = new ArrayList<>();
		
		for (Route route : routes) {
			if (route.getDepartureAirportCode().equals(airportCode)) {
				currentRoutes.add(route);
			}
		}
		
		return currentRoutes;
	}

	/**
	 * Finds all of the flights that depart on the specified date
	 * @param date the date to search for
	 * @return A list of all routes that depart on this date
	 */
	@Override
	public List<Route> findRoutesbyDate(LocalDate date) {
		List<Route> currentRoutes = new ArrayList<>();
		
		for (Route route : routes) {
			if (date.getDayOfWeek().toString().contains(route.getDayOfWeek().toUpperCase())) {
				currentRoutes.add(route);
			}
		}
		
		return currentRoutes;
	}

	/**
	 * Returns The full list of all currently loaded routes
	 * @return The full list of all currently loaded routes
	 */
	@Override
	public List<Route> getAllRoutes() {
		return routes;
	}

	/**
	 * Returns The number of routes currently loaded
	 * @return The number of routes currently loaded
	 */
	@Override
	public int getNumberOfRoutes() {
		return routes.size();
	}

	/**
	 * Loads the route data from the specified file, adding them to the currently loaded routes
	 * Multiple calls to this function, perhaps on different files, would thus be cumulative
	 * @param p A Path pointing to the file from which data could be loaded
	 * @throws DataLoadingException if anything goes wrong. The exception's "cause" indicates the underlying exception
	 */
	@Override
	public void loadRouteData(Path path) throws DataLoadingException {
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = db.parse(path.toString());
			Element root = doc.getDocumentElement();
			
			NodeList routeNodes = root.getElementsByTagName("Route");

			
			for (int i=0; i<routeNodes.getLength(); i++) {
				
				Route route = new Route();
				
				Node currentNode = routeNodes.item(i);
			
				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
					
					Element currentRoute = (Element) currentNode;
					
					route.setFlightNumber(Integer.parseInt(getValueFromXMLKey(currentRoute, "FlightNumber")));
					route.setDayOfWeek(getValueFromXMLKey(currentRoute, "DayOfWeek"));
					route.setDepartureTime(LocalTime.parse(getValueFromXMLKey(currentRoute, "DepartureTime")));
					route.setDepartureAirport(getValueFromXMLKey(currentRoute, "DepartureAirport"));
					route.setDepartureAirportCode(getValueFromXMLKey(currentRoute, "DepartureAirportCode"));
					route.setArrivalTime(LocalTime.parse(getValueFromXMLKey(currentRoute, "ArrivalTime")));
					route.setArrivalAirport(getValueFromXMLKey(currentRoute, "ArrivalAirport"));
					route.setArrivalAirportCode(getValueFromXMLKey(currentRoute, "ArrivalAirportCode"));
					route.setDuration(Duration.parse(getValueFromXMLKey(currentRoute, "Duration")));
				}
				
				routes.add(route);

			}
			
		} catch (ParserConfigurationException | SAXException | IOException | NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new DataLoadingException();
		}

	}

	private String getValueFromXMLKey(Element currentRoute, String tagName) {
		return currentRoute.getElementsByTagName(tagName).item(0).getTextContent();
	}
	
	

	/**
	 * Unloads all of the crew currently loaded, ready to start again if needed
	 */
	@Override
	public void reset() {
		routes = new ArrayList<>();

	}

}
