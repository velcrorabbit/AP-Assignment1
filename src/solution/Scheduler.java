package solution;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import baseclasses.*;
import baseclasses.Pilot.Rank;

public class Scheduler implements IScheduler {
	
	private List<CabinCrew> cabinCrew;
	private List<Pilot> pilots;
	private List<Aircraft> planes;
	//private List<Route> routes;
	private PassengerNumbersDAO passengers;
	private LocalDate startDate;
	//private LocalDate endDate;
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
		//this.endDate = endDate;
		
		this.aircraft = aircraft;
		//this.routes = route.getAllRoutes();

		schedule = new Schedule(route, startDate, endDate);

		Vector<FlightInfo> vector = new Vector<>();
		for (FlightInfo flight : schedule.getRemainingAllocations()) {
			vector.add(flight);
		}
		
		for (FlightInfo flight: vector) {
			
			Route currentRoute = flight.getFlight();

			try {
				try{
					getAPlane(flight, currentRoute);
					getACaptain(flight);
					getAFirstOfficer(flight);
					getCabinCrew(flight);
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
	
	public void getAPlane(FlightInfo flight, Route currentRoute) throws DoubleBookedException  {	
		
		LocalDate departureDate = flight.getDepartureDateTime().toLocalDate();
		int passengerNumber = passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), departureDate);
		
		List<Aircraft> possiblePlanes = getPossiblePlanesForFlight(flight, currentRoute, passengerNumber);
		
		Aircraft bestPlane = possiblePlanes.get(0);
		
		for (Aircraft plane : getPossiblePlanesForFlight(flight, currentRoute, passengerNumber)) {
			if (plane.getSeats() < bestPlane.getSeats()) {
				bestPlane = plane;
				
			}
		}		
		schedule.allocateAircraftTo(bestPlane, flight);
		
	}
	
	
	
	
	private List<Aircraft> getPossiblePlanesForFlight(FlightInfo flight, Route currentRoute, int passengers) {
		
		List<Aircraft> bestPlanes = new ArrayList<>();
		
		int topPoints = 0;
		
		for (Aircraft plane : planes) {
			
			int planePoints = calculatePlaneScore(plane, flight, currentRoute, passengers);
			if(planePoints > topPoints) {
				topPoints = planePoints;
			}
		}

		for (Aircraft plane : planes) {
			boolean hasSeats = planeHasSeats(plane, passengers);
			boolean inCorrectLocation = planeInRightLocation(currentRoute, plane);
			boolean hasNoConflict = !schedule.hasConflict(plane, flight);
			
			int planePoints = 0;
			
			if(hasNoConflict) {
				if (hasSeats) {
					planePoints += 1000;
				}
				if (inCorrectLocation) {
					planePoints += 10000;
				}
			}
			if(planePoints == topPoints) {
				bestPlanes.add(plane);
			}
		}
		
		return bestPlanes;
	}
	
	private int calculatePlaneScore(Aircraft plane, FlightInfo flight, Route currentRoute, int passengers) {
		
		boolean hasSeats = planeHasSeats(plane, passengers);
		boolean inCorrectLocation = planeInRightLocation(currentRoute, plane);
		boolean hasNoConflict = !schedule.hasConflict(plane, flight);
		
		int planePoints = 0;
		
		if(hasNoConflict) {
			if (hasSeats) {
				planePoints += 1000;
			}
			if (inCorrectLocation) {
				planePoints += 10000;
			}
		}
		
		return planePoints;

	}
	
	private boolean planeInRightLocation(Route currentRoute, Aircraft plane) {

		List<FlightInfo> previousFlights = schedule.getCompletedAllocationsFor(plane);
		
		String currentLocation = plane.getStartingPosition();
		if (previousFlights.size() > 1) {
			currentLocation = previousFlights.get(previousFlights.size()-1).getFlight().getArrivalAirportCode();
		}
		
		
		if (currentRoute.getDepartureAirportCode().equals(currentLocation)) {
			return true;
		} else {
			return false;
		}
		
	}
	
	private boolean planeHasSeats(Aircraft plane, int passsengers){

		List<Aircraft> planesWithSeats = aircraft.findAircraftBySeats(passsengers);
		
		if (planesWithSeats.contains(plane)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void getACaptain(FlightInfo flight) throws DoubleBookedException {

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

	public void getAFirstOfficer(FlightInfo flight) throws DoubleBookedException {
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
			
			if(pilotPoints > topPoints) {
				topPoints = pilotPoints;
				topFirstOfficer = crew;
			}
			
		}

		return topFirstOfficer;
		
	}
	
	public void getCabinCrew(FlightInfo flight) throws DoubleBookedException {
		
		
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
		boolean qualified = crew.isQualifiedFor(schedule.getAircraftFor(flight));
		boolean rested = crewIsRested(crew, flight);
		boolean location =  crewInCorrectLocation(crew, flight);
		boolean underMaxHours = !crewHitMaxHours(crew);
		
		int crewPoints = 0;
		
		if (hasNoConflict && correctRank) {
			if(qualified) {
				crewPoints += 50000;
			}
			if(rested) {
				crewPoints += 20000;
			}
			if(location) {
				crewPoints += 6000;
			}
			if(underMaxHours) {
				crewPoints += 5000;
			}
		}
		return crewPoints;
	}
	
	private int calculateCrewScore(FlightInfo flight, CabinCrew crew) {
		
		
		boolean hasNoConflict = !schedule.hasConflict(crew, flight);
		boolean qualified = crew.isQualifiedFor(schedule.getAircraftFor(flight));
		boolean rested = crewIsRested(crew, flight);
		boolean location =  crewInCorrectLocation(crew, flight);
		boolean underMaxHours = !crewHitMaxHours(crew);
		
		int crewPoints = 0;
		
		if (hasNoConflict) {
			if(qualified) {
				crewPoints += 50000;
			}
			if(rested) {
				crewPoints += 20000;
			}
			if(location) {
				crewPoints += 6000;
			}
			if(underMaxHours) {
				crewPoints += 5000;
			}
		}
		return crewPoints;
	}
	
	private boolean crewInCorrectLocation(Crew crew, FlightInfo flight) {

		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);
		
		boolean isFlightDepartingFromPreviousArrival = false;
		boolean isAtHomeBase = false;
		boolean hadTimeToTravel = false;
		
		if (crewFlights.size() > 1) {
		
			String crewPreviousArrival = crewFlights.get(crewFlights.size()-1).getFlight().getArrivalAirportCode();
			
			String crewCurrentDeparture = flight.getFlight().getDepartureAirportCode();

			isFlightDepartingFromPreviousArrival = crewPreviousArrival.equals(crewCurrentDeparture);
			
			if(!isFlightDepartingFromPreviousArrival) {
				if(flight.getDepartureDateTime().isAfter(crewFlights.get(crewFlights.size()-1).getLandingDateTime().plusHours(48))){
					hadTimeToTravel = true;
				}
			}
		} else {
			isAtHomeBase = crew.getHomeBase() == flight.getFlight().getDepartureAirportCode();
		}
		
		if (isFlightDepartingFromPreviousArrival || isAtHomeBase || hadTimeToTravel)
			return true;
		
		return false;
	}
	
	private boolean crewIsRested(Crew crew, FlightInfo flight) {

		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);

		if (crewFlights.size() > 1) {
			
			LocalTime crewPreviousArrival = crewFlights.get(crewFlights.size()-1).getFlight().getArrivalTime();
			LocalTime crewCurrentDeparture = flight.getFlight().getDepartureTime();
			
			if(Utilities.airportIsInUK(crewFlights.get(crewFlights.size()-1).getFlight().getArrivalAirportCode()) 
					|| Utilities.airportIsInUK(flight.getFlight().getDepartureAirport())) {
				
				return crewCurrentDeparture.isAfter(crewPreviousArrival.plusHours(12));
			}

		}
		return true;


	}
	
	private boolean crewHitMaxHours(Crew crew) {
		
		List<FlightInfo> crewsFlights = schedule.getCompletedAllocationsFor(crew);
		
		LocalDate MonthEnd = startDate.plusMonths(1);
		
		long minutesWorked = 0;
		
		for (FlightInfo flight : crewsFlights) {
			
			if(flight.getLandingDateTime().toLocalDate().isBefore(MonthEnd)) {
				
				minutesWorked += flight.getFlight().getDuration().toMinutes();
				
				if(minutesWorked > 6000) {
					
					return false;

				}
				
			} else {
				
				MonthEnd = MonthEnd.plusMonths(1);
				minutesWorked = flight.getFlight().getDuration().toHours();
				
			}
		}
		
		return true;
	}
	
	@Override
	public void setSchedulerRunner(SchedulerRunner arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
	
public void printOutcomes (FlightInfo flight, Route currentRoute) {
		
		Aircraft plane = schedule.getAircraftFor(flight);
		int passengerNumbers = this.passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), flight.getDepartureDateTime().toLocalDate());
		int seats = plane.getSeats();
		boolean planeHasSeats = planeHasSeats(plane, passengerNumbers);
		List<FlightInfo> completedAllocations = schedule.getCompletedAllocationsFor(plane);
		
		String currentAirport = "(Start)" + plane.getStartingPosition();
		
		if(completedAllocations.size() > 1) {
			currentAirport = "(Arrival)" + completedAllocations.get(completedAllocations.size()-1).getFlight().getArrivalAirportCode();
		}
		
		String departureAirport = currentRoute.getDepartureAirportCode();
		
		boolean planeInRightLocation = planeInRightLocation(currentRoute, schedule.getAircraftFor(flight));

		
		System.out.println("Plane Has Seats: " 
				+ planeHasSeats
				+ "		 | Plane Seats: "
				+ seats 
				+ "			 | People : " 
				+ passengerNumbers);
	
		System.out.println("Plane in right location: " 
				+ planeInRightLocation 
				+ "	 | Current Location: "
				+ currentAirport
				+ "	 | Current Departure: "
				+ departureAirport);
		System.out.println("-----------------------");
	}

}
