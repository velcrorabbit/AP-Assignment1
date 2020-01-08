package solution;

import java.nio.file.Paths;
import java.time.LocalDate;

import baseclasses.*;

/**
 * This class allows you to run the code in your classes yourself, for testing and development
 */
public class Main {

	public static void main(String[] args) throws DataLoadingException {	
		
		IAircraftDAO aircraft = new AircraftDAO();
		ICrewDAO crew = new CrewDAO();
		IPassengerNumbersDAO numbers = new PassengerNumbersDAO();
		IRouteDAO routes = new RouteDAO();

		aircraft.reset();
		crew.reset();
		numbers.reset();
		routes.reset();
		
		aircraft.loadAircraftData(Paths.get("./data/mini_aircraft.csv"));
		crew.loadCrewData(Paths.get("./data/mini_crew.json"));
		numbers.loadPassengerNumbersData(Paths.get("./data/mini_passengers.db"));
		routes.loadRouteData(Paths.get("./data/mini_routes.xml"));

		Scheduler scheduler = new Scheduler();
		scheduler.generateSchedule(aircraft, crew, routes, numbers, LocalDate.parse("2020-07-01"), LocalDate.parse("2020-08-31"));
	}
}
