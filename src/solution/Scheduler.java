package solution;
import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import baseclasses.*;
import baseclasses.Pilot.Rank;

public class Scheduler implements IScheduler {
	
	private List<CabinCrew> cabinCrew;
	private List<Pilot> pilots;
	private List<Aircraft> planes;
	private List<Route> routes;
	private PassengerNumbersDAO passengers;
	private LocalDate startDate;
	private Schedule schedule;
	
	private IAircraftDAO aircraft;
	
	@Override
	public Schedule generateSchedule(IAircraftDAO aircraft, ICrewDAO crew, IRouteDAO route, IPassengerNumbersDAO passengers,
			LocalDate startDate, LocalDate endDate) {
		
		this.cabinCrew = crew.getAllCabinCrew();
		this.pilots = crew.getAllPilots();
		this.planes = aircraft.getAllAircraft();
		this.passengers = (PassengerNumbersDAO) passengers;
		this.startDate = startDate;
		this.aircraft = aircraft;
		this.routes = route.getAllRoutes();

		schedule = new Schedule(route, startDate, endDate);

		Vector<FlightInfo> vector = new Vector<>();
		for (FlightInfo flight : schedule.getRemainingAllocations()) {
			vector.add(flight);
		}
		
		for (FlightInfo flight: vector) {
			
			Route currentRoute = flight.getFlight();

			try {
				try{
					scheduleAPlane(flight, currentRoute);
					scheduleACaptain(flight);
					scheduleAFirstOfficer(flight);
					scheduleCabinCrew(flight);
				} catch (DoubleBookedException e) {
					e.printStackTrace();
				}

				schedule.completeAllocationFor(flight);
				//System.out.println(schedule.isValid(flight));
			} catch (InvalidAllocationException e) {
				
				System.out.println("Crew needed: " + schedule.getAircraftFor(flight).getCabinCrewRequired());
				for (CabinCrew crewMember : schedule.getCabinCrewOf(flight)) {
					System.out.println("Crew: " + crewMember.getForename());
				}
				System.out.println("Captain: " + schedule.getCaptainOf(flight).getForename());
				System.out.println("First Officer: " + schedule.getFirstOfficerOf(flight).getForename());
				
				e.printStackTrace();
			}

		}
		
		System.out.println("Done");
		/*
		for (FlightInfo completeFlight : schedule.getCompletedAllocations()) {
			printOutcomes(completeFlight, completeFlight.getFlight());
		}
*/
		return schedule;
	}
	

	public void scheduleAPlane(FlightInfo flight, Route currentRoute) throws DoubleBookedException  {	
		
		LocalDate departureDate = flight.getDepartureDateTime().toLocalDate();
		int passengerNumber = passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), departureDate);
		
		List<Aircraft> possiblePlanes = getPossiblePlanesForFlight(flight, currentRoute, passengerNumber);
		
		for (Aircraft plane : possiblePlanes) {
			//System.out.println("Score: " +calculatePlaneScore(plane, flight, currentRoute, passengerNumber)+ " | Starting Location: " +plane.getStartingPosition()+ " | Departing from: " +currentRoute.getDepartureAirportCode());
			schedule.allocateAircraftTo(plane, flight);
			break;
		}		

	}
	
	private List<Aircraft> getPossiblePlanesForFlight(FlightInfo flight, Route currentRoute, int passengers) {
		
		List<Aircraft> bestPlanes = new ArrayList<>();
		
		int topPoints = getTopPlaneScore(flight, currentRoute, passengers);

		for (Aircraft plane : planes) {
			
			int planePoints = calculatePlaneScore(plane, flight, currentRoute, passengers);

			if(planePoints == topPoints) {
				//System.out.println("plane points: " +planePoints+ " | best points: " +topPoints);
				bestPlanes.add(plane);
			}
		}
		
		return bestPlanes;
	}

	private int getTopPlaneScore(FlightInfo flight, Route currentRoute, int passengers) {
		
		int topPoints = 0;
		
		for (Aircraft plane : planes) {
			boolean hasNoConflict = !schedule.hasConflict(plane, flight);
			if(hasNoConflict) { 
				int planePoints = calculatePlaneScore(plane, flight, currentRoute, passengers);
				if(planePoints > topPoints) {
					topPoints = planePoints;
				}
			}
		}		
		return topPoints;
	}
	
	private int calculatePlaneScore(Aircraft plane, FlightInfo flight, Route currentRoute, int passengers) {
		
		boolean hasNoConflict = !schedule.hasConflict(plane, flight);
		
		int planePoints = 0;
		
		if(hasNoConflict) {
			planePoints += planeHasSeats(plane, passengers);
			planePoints += planeInRightLocation(currentRoute, plane);
		} else {
			planePoints = -1;
		} 
		
		return planePoints;

	}
	
	private int planeInRightLocation(Route currentRoute, Aircraft plane) {

		int points = 0;
		
		String departureAirport = currentRoute.getDepartureAirportCode();
		String currentLocation = plane.getStartingPosition();
		
		if(departureAirport.equals(currentLocation)) {
			points = 50000;
		}

		return points;
		
	}
	
	private int planeHasSeats(Aircraft plane, int seatsNeeded){

		int points;
		
		List<Aircraft> planesWithSeats = aircraft.findAircraftBySeats(seatsNeeded);
		
		if (planesWithSeats.contains(plane)) {
			points = 50000 - (plane.getSeats() - seatsNeeded)*10;
		} else {
			if (Utilities.airportIsInUK(plane.getStartingPosition()))
				points = plane.getSeats() * 100;
			else
				points = plane.getSeats() * 10;
		}

		return points;
	}

	public void scheduleACaptain(FlightInfo flight) throws DoubleBookedException {

		Pilot captain = getBestCaptainForFlight(flight);
		
		if (!schedule.hasConflict(captain, flight)) {
			schedule.allocateCaptainTo(captain, flight);
		}
		
	}
	
	private Pilot getBestCaptainForFlight(FlightInfo flight) {		
		
		int topPoints = 0;
		Pilot topCaptain = pilots.get(0);
		
		for (Pilot crew : pilots) {
			
			int pilotPoints = calculatePilotScore("Captain", flight, crew);
			
			if(pilotPoints > topPoints) {
				topPoints = pilotPoints;
				topCaptain = crew;
			}
			
		}

		return topCaptain;
		
	}

	public void scheduleAFirstOfficer(FlightInfo flight) throws DoubleBookedException {
		Pilot firstOfficer = getBestFirstOfficerForFlight(flight);
		
		if (!schedule.hasConflict(firstOfficer, flight)) {
			schedule.allocateFirstOfficerTo(firstOfficer, flight);
		}
	}
	
	private Pilot getBestFirstOfficerForFlight(FlightInfo flight) {
		
		int topPoints = 0;
		Pilot topFirstOfficer = pilots.get(0);
		
		for (Pilot crew : pilots) {
			
			int pilotPoints = calculatePilotScore("First Officer", flight, crew);
			
			if(pilotPoints > topPoints && !schedule.getCaptainOf(flight).equals(crew)) {
				topPoints = pilotPoints;
				topFirstOfficer = crew;
			}
			
		}

		return topFirstOfficer;
		
	}
	
	public void scheduleCabinCrew(FlightInfo flight) throws DoubleBookedException {
		
		
		int crewNeeded = schedule.getAircraftFor(flight).getCabinCrewRequired();
		int crewGot = 0;
		
		while (crewGot <= crewNeeded) {
	
			schedule.allocateCabinCrewTo(getBestCabinCrew(flight), flight);
			crewGot++;

		}
	}
	
	private CabinCrew getBestCabinCrew(FlightInfo flight){
		
		int topPoints = 0;
		CabinCrew topCrew = cabinCrew.get(0);
		
		for (CabinCrew crew : cabinCrew) {
			
			if(!schedule.getCabinCrewOf(flight).contains(crew)) {
				int crewPoints = calculateCrewScore(flight, crew);
					
				if(crewPoints > topPoints) {
					topPoints = crewPoints;
					topCrew = crew;
				}
			}
			
		}
		
		return topCrew;
		

	}
	
	private int calculatePilotScore(String type, FlightInfo flight, Pilot crew) {
		
		boolean correctRank = false;
		
		if (type.equals("Captain")) {
			correctRank = crew.getRank() == Rank.CAPTAIN;
		} else if (type.equals("First Officer")) {
			correctRank = crew.getRank() == Rank.FIRST_OFFICER;
		}
		
		boolean hasNoConflict = !schedule.hasConflict(crew, flight);
		
		int crewPoints = 0;
		
		if (hasNoConflict && correctRank) {
			crewPoints += crewQualifiedPoints(crew, schedule.getAircraftFor(flight));
			crewPoints += crewInCorrectLocationPoints(crew, flight);
			crewPoints += restedInUKPoints(crew, flight);;
			crewPoints += crewMonthlyHoursPoints(crew);
			crewPoints += crewWeekendPoints(crew);
			crewPoints += crewOutsideUKPoints(crew, flight);
		}
		return crewPoints;
	}
	
	private int calculateCrewScore(FlightInfo flight, CabinCrew crew) {
		
		boolean hasNoConflict = !schedule.hasConflict(crew, flight);
		
		int crewPoints = 0;
		
		if (hasNoConflict) {
			crewPoints += crewQualifiedPoints(crew, schedule.getAircraftFor(flight));
			crewPoints += crewInCorrectLocationPoints(crew, flight);
			crewPoints += restedInUKPoints(crew, flight);;
			crewPoints += crewMonthlyHoursPoints(crew);
			crewPoints += crewWeekendPoints(crew);
			crewPoints += crewOutsideUKPoints(crew, flight);
		}

		return crewPoints;
	}
	/**
	 * crew on a plane they're qualified for
	 * @param crew
	 * @param plane
	 * @return
	 */
	private int crewQualifiedPoints(Crew crew, Aircraft plane) {
		
		int points = 0;
		
		if (crew.isQualifiedFor(plane)){
			points = 500000;
		}
		
		return points;
	}
	/**
	 * priority given to crew outside the uk.
	 * @param crew
	 * @return
	 */
	private int crewOutsideUKPoints(Crew crew, FlightInfo flight) {
		int points = 0;

		if(Utilities.airportIsInUK(flight.getFlight().getArrivalAirportCode())) {
			
			if (!Utilities.airportIsInUK(getCrewLastFlight(crew, flight.getFlight()).getArrivalAirportCode())) {
				points = 50000;
			}
		}
		return points;
	}
	/**
	 * check if the crew is in the right location, or has enough time to get there.
	 * @param crew
	 * @param flight
	 * @return
	 */
	private int crewInCorrectLocationPoints(Crew crew, FlightInfo flight) {

		int points = 0;
		
		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);
		
		if (crewFlights.size() > 1) {
		
			String crewPreviousArrival = getCrewLastFlight(crew, flight.getFlight()).getArrivalAirportCode();
			
			String crewCurrentDeparture = flight.getFlight().getDepartureAirportCode();

			if(crewCurrentDeparture.equals(crewPreviousArrival) && !flight.getFlight().getDepartureTime().isAfter(getCrewLastFlight(crew, flight.getFlight()).getArrivalTime().plusHours(4))) {
				points += 50000;
			} else if(flight.getDepartureDateTime().isAfter(crewFlights.get(crewFlights.size()-1).getLandingDateTime().plusHours(48))){
				points += 50000;
			}
		} else {
			if(crew.getHomeBase().equals(flight.getFlight().getDepartureAirportCode()))
				points += 50000;
		}
		
		return points;
	}
	
	private int restedInUKPoints(Crew crew, FlightInfo flight) {

		int points = 0;
		
		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);

		if (crewFlights.size() > 1) {
			
			LocalTime crewPreviousArrival = getCrewLastFlight(crew, flight.getFlight()).getArrivalTime();
			LocalTime crewCurrentDeparture = flight.getFlight().getDepartureTime();
			
			String arrivalAirport = getCrewLastFlight(crew, flight.getFlight()).getArrivalAirportCode();
			String departureAirport = flight.getFlight().getDepartureAirportCode();
			
			if (Utilities.airportIsInUK(arrivalAirport) || Utilities.airportIsInUK(departureAirport)) {
				
				if(crew.getHomeBase().equals(departureAirport) && crewCurrentDeparture.isAfter(crewPreviousArrival.plusHours(12))) {
					points += 50000;
				}
				// Scheduler won't run if this is an if else...
				if(!crew.getHomeBase().contains(departureAirport) && crewCurrentDeparture.isAfter(crewPreviousArrival.plusHours(24))) {
					points += 40000;
				}
			}
		}
		return points;
	}
	
	private int crewWeekendPoints(Crew crew) {
		int points = 0;
				
		List<FlightInfo> crewsFlights = schedule.getCompletedAllocationsFor(crew);
		
		LocalDateTime weekStart = startDate.atStartOfDay();
		LocalDateTime weekEnd = startDate.plusWeeks(1).atStartOfDay().plusHours(23);
		
		List<FlightInfo> flightsThisWeek = new ArrayList<>();
		
		for (FlightInfo flight : crewsFlights) {
			if (flight.getDepartureDateTime().isAfter(weekStart) && flight.getLandingDateTime().isBefore(weekEnd)) {
				flightsThisWeek.add(flight);
			} else {
				weekStart = weekEnd;
				weekEnd = weekStart.plusWeeks(1);
				flightsThisWeek.clear();
				flightsThisWeek.add(flight);
			}
		}
		
		for (int i=1; i < flightsThisWeek.size(); i++) {
			if (flightsThisWeek.get(i).getDepartureDateTime().isAfter(flightsThisWeek.get(i-1).getLandingDateTime().plusHours(36))) {
				points += 30000;
				break;
			}
		}
	
		return points;
		
	}
	
	private int crewMonthlyHoursPoints(Crew crew) {
		
		
		int points = 0;
		
		List<FlightInfo> crewsFlights = schedule.getCompletedAllocationsFor(crew);
		
		LocalDate MonthEnd = startDate.plusMonths(1);
		
		int minutesWorked = 0;
		
		for (FlightInfo flight : crewsFlights) {
			
			if(flight.getLandingDateTime().toLocalDate().isBefore(MonthEnd)) {
				
				minutesWorked += flight.getFlight().getDuration().toMinutes();
				
				if(minutesWorked < 6000) {
					
					points += 5000;

				} else {
					points = 5000 - (minutesWorked - 6000)*10;
				}
				
			} else {
				
				MonthEnd = MonthEnd.plusMonths(1);
				minutesWorked = (int)flight.getFlight().getDuration().toHours();
				
			}
		}
		
		return points;
	}
	
	private Route getCrewLastFlight(Crew crew, Route currentRoute) {
		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);
		
		Route crewLastFlight = currentRoute;
		
		if(crewFlights.size() > 1) {
		
			crewLastFlight = crewFlights.get(crewFlights.size()-1).getFlight();
		
		}
		return crewLastFlight;
	}
	
	@Override
	public void setSchedulerRunner(SchedulerRunner arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	
}
