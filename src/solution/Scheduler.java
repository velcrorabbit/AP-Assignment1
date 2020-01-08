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
	private HashMap<Route, Route> linkedRoutes = new HashMap<>();
	
	@Override
	public Schedule generateSchedule(IAircraftDAO aircraft, ICrewDAO crew, IRouteDAO route, IPassengerNumbersDAO passengers,
			LocalDate startDate, LocalDate endDate) {
		
		this.cabinCrew = crew.getAllCabinCrew();
		this.pilots = crew.getAllPilots();
		this.planes = aircraft.getAllAircraft();
		this.passengers = (PassengerNumbersDAO) passengers;
		this.startDate = startDate;
		this.routes = route.getAllRoutes();

		schedule = new Schedule(route, startDate, endDate);

		linkRoutes();
		
		Vector<FlightInfo> vector = new Vector<>();
		for (FlightInfo flight : schedule.getRemainingAllocations()) {
			vector.add(flight);
		}
		
		for (FlightInfo outBoundFlight: vector) {
			if (linkedRoutes.containsKey(outBoundFlight.getFlight())) {
				try {
					try{
						scheduleAPlane(outBoundFlight, outBoundFlight.getFlight());
						scheduleACaptain(outBoundFlight);
						scheduleAFirstOfficer(outBoundFlight);
						scheduleCabinCrew(outBoundFlight);
					} catch (DoubleBookedException e) {
						e.printStackTrace();
					}

					schedule.completeAllocationFor(outBoundFlight);
				} catch (InvalidAllocationException e) {
					
					e.printStackTrace();
				}
				
				for (FlightInfo secondFlight: vector) {
					if(linkedRoutes.get(outBoundFlight.getFlight()).equals(secondFlight.getFlight())) {	
						try {
							try{
								schedule.allocateAircraftTo(schedule.getAircraftFor(outBoundFlight), secondFlight);
								schedule.allocateCaptainTo(schedule.getCaptainOf(outBoundFlight), secondFlight);
								schedule.allocateFirstOfficerTo(schedule.getFirstOfficerOf(outBoundFlight), secondFlight);
								for (CabinCrew outBoundCrew : schedule.getCabinCrewOf(outBoundFlight)) {
									schedule.allocateCabinCrewTo(outBoundCrew, secondFlight);
								}
							} catch (DoubleBookedException e) {
								e.printStackTrace();
							}
							schedule.completeAllocationFor(secondFlight);
						} catch (InvalidAllocationException e) {

							e.printStackTrace();
						}
						break;
					}
				}
			}
		}
		for (FlightInfo flights: vector) {
			
			if (!linkedRoutes.containsKey(flights.getFlight()) && !linkedRoutes.containsValue(flights.getFlight())) {
				try {
					try{
						scheduleAPlane(flights, flights.getFlight());
						scheduleACaptain(flights);
						scheduleAFirstOfficer(flights);
						scheduleCabinCrew(flights);
					} catch (DoubleBookedException e) {
						e.printStackTrace();
					}

					schedule.completeAllocationFor(flights);
					//System.out.println(schedule.isValid(flight));
				} catch (InvalidAllocationException e) {
					
					System.out.println("Crew needed: " + schedule.getAircraftFor(flights).getCabinCrewRequired());
					for (CabinCrew crewMember : schedule.getCabinCrewOf(flights)) {
						System.out.println("Crew: " + crewMember.getForename());
					}
					System.out.println("Captain: " + schedule.getCaptainOf(flights).getForename());
					System.out.println("First Officer: " + schedule.getFirstOfficerOf(flights).getForename());
					
					e.printStackTrace();
				}
			}
		}
		System.out.println("Done");
		return schedule;
	}
	/*
	 * add the best plane to the flight
	 */
	public void scheduleAPlane(FlightInfo flight, Route currentRoute) throws DoubleBookedException  {	
		
		LocalDate departureDate = flight.getDepartureDateTime().toLocalDate();
		int passengerNumber = passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), departureDate);
		
		List<Aircraft> possiblePlanes = getPossiblePlanesForFlight(flight, currentRoute, passengerNumber);
		
		for (Aircraft plane : possiblePlanes) {
			schedule.allocateAircraftTo(plane, flight);
			break;
		}		
	}
	/*
	 * get the planes that equal the best planes points
	 */
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
	/*
	 * get the plane with the most points
	 */
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
	/*
	 * work out how many points each plane has
	 */
	private int calculatePlaneScore(Aircraft plane, FlightInfo flight, Route currentRoute, int passengers) {
		
		boolean hasNoConflict = !schedule.hasConflict(plane, flight);
		
		int planePoints = 0;
		
		if(hasNoConflict) {
			planePoints += planeHasSeats(plane, passengers, currentRoute);
			planePoints += planeLeavingStartLocation(currentRoute, plane);
		}
		return planePoints;
	}
	/**
	 * check if the plane is in the right location
	 * @param currentRoute
	 * @param plane
	 * @return
	 */
	private int planeLeavingStartLocation(Route currentRoute, Aircraft plane) {

		int points = 0;
		
		String departureAirport = currentRoute.getDepartureAirportCode();
		String currentLocation = plane.getStartingPosition();
		
		if(departureAirport.equals(currentLocation)) {
			points = 50000;
		}
		return points;	
	}
	/*
	 * check if the plane has the right number of seats
	 */
	private int planeHasSeats(Aircraft plane, int seatsNeeded, Route currentRoute){

		int points = 0;
				
		if (plane.getSeats() >= seatsNeeded) {
			points = 60000 - (plane.getSeats() - seatsNeeded)*10;
		} else {
			if (Utilities.airportIsInUK(currentRoute.getDepartureAirportCode()))
				points = plane.getSeats() * 100;
			else
				points = plane.getSeats() * 10;
		}
		return points;
	}
	/*
	 * add the best captain to the flight
	 */
	public void scheduleACaptain(FlightInfo flight) throws DoubleBookedException {

		Pilot captain = getBestCaptainForFlight(flight);
		
		if (!schedule.hasConflict(captain, flight)) {
			schedule.allocateCaptainTo(captain, flight);
		}	
	}
	/*
	 * get the captain with the most points
	 */
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
	/*
	 * add the best first officer to the flight
	 */
	public void scheduleAFirstOfficer(FlightInfo flight) throws DoubleBookedException {
		Pilot firstOfficer = getBestFirstOfficerForFlight(flight);
		
		if (!schedule.hasConflict(firstOfficer, flight)) {
			schedule.allocateFirstOfficerTo(firstOfficer, flight);
		}
	}
	/*
	 * get the first officer with the most points
	 */
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
	/*
	 * Add the best crew member to the flight until all the positions are full.
	 */
	public void scheduleCabinCrew(FlightInfo flight) throws DoubleBookedException {
		
		
		int crewNeeded = schedule.getAircraftFor(flight).getCabinCrewRequired();
		int crewGot = 0;
		
		while (crewGot < crewNeeded) {
	
			schedule.allocateCabinCrewTo(getBestCabinCrew(flight), flight);
			crewGot++;

		}
	}
	/*
	 * gets the crew with the most points.
	 */
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
	/*
	 * work out how many pilot points a pilot has
	 */
	private int calculatePilotScore(String type, FlightInfo flight, Pilot crew) {
		
		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);
		boolean hasNoConflict = !schedule.hasConflict(crew, flight);
		int crewPoints = 0;
		boolean correctRank = false;
		
		if (type.equals("Captain")) {
			correctRank = crew.getRank() == Rank.CAPTAIN;
		} else if (type.equals("First Officer")) {
			correctRank = crew.getRank() == Rank.FIRST_OFFICER;
		}
		
		if (hasNoConflict && correctRank) {
			crewPoints += crewQualifiedPoints(crew, schedule.getAircraftFor(flight));
			crewPoints += crewInCorrectLocationPoints(crew, flight, crewFlights);
			crewPoints += restedInUKPoints(crew, flight, crewFlights);;
			crewPoints += crewMonthlyHoursPoints(crew, crewFlights);
			crewPoints += crewWeekendPoints(crew, crewFlights);
			crewPoints += crewOutsideUKPoints(crew, flight, crewFlights);
		}
		return crewPoints;
	}
	/*
	 * work out how many points a crew member has for that flight
	 */
	private int calculateCrewScore(FlightInfo flight, CabinCrew crew) {
		
		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);
		boolean hasNoConflict = !schedule.hasConflict(crew, flight);
		int crewPoints = 0;
		
		if (hasNoConflict) {
			crewPoints += crewQualifiedPoints(crew, schedule.getAircraftFor(flight));
			crewPoints += crewInCorrectLocationPoints(crew, flight, crewFlights);
			crewPoints += restedInUKPoints(crew, flight, crewFlights);;
			crewPoints += crewMonthlyHoursPoints(crew, crewFlights);
			crewPoints += crewWeekendPoints(crew, crewFlights);
			crewPoints += crewOutsideUKPoints(crew, flight, crewFlights);
		}
		return crewPoints;
	}
	/*
	 * crew on a plane they're qualified for
	 */
	private int crewQualifiedPoints(Crew crew, Aircraft plane) {
		
		int points = 0;
		
		if (crew.isQualifiedFor(plane)){
			points = 500000;
		}
		
		return points;
	}
	/*
	 * priority given to crew outside the uk.
	 */
	private int crewOutsideUKPoints(Crew crew, FlightInfo flight, List<FlightInfo> crewFlights) {
		int points = 0;

		if(Utilities.airportIsInUK(flight.getFlight().getArrivalAirportCode())) {
			
			if (!Utilities.airportIsInUK(getCrewLastFlight(crew, flight.getFlight(), crewFlights).getArrivalAirportCode())) {
				points = 5000;
			}
		}
		return points;
	}
	/*
	 * check if the crew is in the right location, or has enough time to get there.
	 */
	private int crewInCorrectLocationPoints(Crew crew, FlightInfo flight, List<FlightInfo> crewFlights) {

		int points = 0;
		
		String crewCurrentDeparture = flight.getFlight().getDepartureAirportCode();
		
		if (crewFlights.size() > 1) {
		
			Route crewPreviousArrival = getCrewLastFlight(crew, flight.getFlight(), crewFlights);
			
			if(crewCurrentDeparture.equals(crewPreviousArrival.getArrivalAirportCode()) 
					&& !flight.getFlight().getDepartureTime().isAfter(crewPreviousArrival.getArrivalTime().plusHours(4))) {
				points = 50000;
			} else if(flight.getDepartureDateTime().isAfter(crewFlights.get(crewFlights.size()-1).getLandingDateTime().plusHours(48))){
				points = 5000;
			}
		} else if(crewAtHomeBase(crew, crewCurrentDeparture)){
			points = 4000;
		}
		
		return points;
	}
	/*
	 * check if crew is at their home base
	 */
	private boolean crewAtHomeBase(Crew crew, String crewCurrentDeparture) {
		//System.out.println("At home: " +crew.getHomeBase().equals(crewCurrentDeparture)+ " | Homebase: " +crew.getHomeBase()+ " | Departure: " + crewCurrentDeparture);
		return crew.getHomeBase().equals(crewCurrentDeparture);
	}
	/*
	 * check if the crew has had a 12 hour break at home, or 24 hour break not at home between uk landings.
	 */
	private int restedInUKPoints(Crew crew, FlightInfo flight, List<FlightInfo> crewFlights) {

		int points = 0;

		if (crewFlights.size() > 1) {
			
			Route crewLastRoute = getCrewLastFlight(crew, flight.getFlight(), crewFlights);
			
			LocalTime crewPreviousArrival = crewLastRoute.getArrivalTime();
			LocalTime crewCurrentDeparture = flight.getFlight().getDepartureTime();
			
			String arrivalAirport = crewLastRoute.getArrivalAirportCode();
			String departureAirport = flight.getFlight().getDepartureAirportCode();
			
			if (Utilities.airportIsInUK(arrivalAirport) && Utilities.airportIsInUK(departureAirport)) {
				
				if(crewAtHomeBase(crew, departureAirport) && crewCurrentDeparture.isAfter(crewPreviousArrival.plusHours(12))) {
					points = 70000;
				}
				// Scheduler won't run if this is an if else...
				if(!crewAtHomeBase(crew, departureAirport) && crewCurrentDeparture.isAfter(crewPreviousArrival.plusHours(24))) {
					points = 70000;
				}
			}
		} else {
			points = 8000;
		}
		return points;
	}
	/*
	 * check if the crew has had a 36 hours break at some point in the week.
	 */
	private int crewWeekendPoints(Crew crew, List<FlightInfo> crewsFlights) {
		int points = 0;
		
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
				points += 3000;
				break;
			}
		}
	
		return points;
		
	}
	/*
	 * check if the crew has worked under 6000 hours in the month
	 */
	private int crewMonthlyHoursPoints(Crew crew, List<FlightInfo> crewsFlights) {
		
		int points = 0;
		
		LocalDate MonthEnd = startDate.plusMonths(1);
		
		int minutesWorked = 0;
		
		for (FlightInfo flight : crewsFlights) {
			
			if(flight.getLandingDateTime().toLocalDate().isBefore(MonthEnd)) {
				
				minutesWorked += flight.getFlight().getDuration().toMinutes();
				
				if(minutesWorked < 600) {
					
					points += 500;

				} else {
					points = 600 - (minutesWorked - 600);
				}
				
			} else {
				
				MonthEnd = MonthEnd.plusMonths(1);
				minutesWorked = (int)flight.getFlight().getDuration().toHours();
				
			}
		}
		
		return points;
	}
	/*
	 * get the last flight the crew has been allocated to.
	 */
	private Route getCrewLastFlight(Crew crew, Route currentRoute, List<FlightInfo> crewFlights) {
		
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

	private void linkRoutes(){

		Route outBound;
		Route inBound;

		for (Route route : routes) {

			if(!linkedRoutes.containsKey(route)) {

				outBound = route;

				for (Route nextFlight : routes) {

					if(!linkedRoutes.containsValue(nextFlight)) {

						if (outBound.getArrivalAirportCode().equals(nextFlight.getDepartureAirportCode())
								&& nextFlight.getArrivalAirportCode().equals(outBound.getDepartureAirportCode())
								&& nextFlight.getDepartureTime().isAfter(outBound.getArrivalTime())) {

							inBound = nextFlight;
							linkedRoutes.put(outBound, inBound);
							//System.out.println("Outbound: " +outBound.getFlightNumber()+ " | Inbound: " +inBound.getFlightNumber());
							break;

						}
						
					}
				}
			}
		}
	}
}
