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
	//private LocalDate startDate;
	//private LocalDate endDate;
	private Schedule schedule;
	
	@Override
	public Schedule generateSchedule(IAircraftDAO aircraft, ICrewDAO crew, IRouteDAO route, IPassengerNumbersDAO passengers,
			LocalDate startDate, LocalDate endDate) {
		
		this.cabinCrew = crew.getAllCabinCrew();
		this.pilots = crew.getAllPilots();
		this.planes = aircraft.getAllAircraft();
		this.passengers = (PassengerNumbersDAO) passengers;
		//this.startDate = startDate;
		//this.endDate = endDate;
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

		return schedule;
	}
	
	public void getAPlane(FlightInfo flight, Route currentRoute) throws DoubleBookedException  {	
		Aircraft bestPlane = getPossiblePlanesForFlight(flight, currentRoute).get(0);
		
		for (Aircraft plane : getPossiblePlanesForFlight(flight, currentRoute)) {
			if (plane.getSeats() < bestPlane.getSeats()) {
				bestPlane = plane;
				
			}
		}		
		schedule.allocateAircraftTo(bestPlane, flight);
	}
	
	private List<Aircraft> getPossiblePlanesForFlight(FlightInfo flight, Route currentRoute) {
		
		ArrayList<Aircraft> planesWithSeatsInCorrectLocation = new ArrayList<>();
		ArrayList<Aircraft> planesWithSeats = new ArrayList<>();
		ArrayList<Aircraft> planesWithOutSeatsInCorrectLocation = new ArrayList<>();
		
		for (Aircraft plane : planes) {
			
			if (planeHasSeats(plane, currentRoute, flight) 
					&& plane.getStartingPosition().equals(currentRoute.getDepartureAirportCode()) 
					&& !schedule.hasConflict(plane, flight)) {
				
				planesWithSeatsInCorrectLocation.add(plane);
				
			} else if (planeHasSeats(plane, currentRoute, flight) 
					&& !schedule.hasConflict(plane, flight)) {
				
				planesWithSeats.add(plane);
				
			} else if (plane.getStartingPosition().equals(currentRoute.getDepartureAirportCode()) 
					&& !schedule.hasConflict(plane, flight)) {
				
				planesWithOutSeatsInCorrectLocation.add(plane);
				
			}
		
		}
		
		if (!planesWithSeatsInCorrectLocation.isEmpty()) {
			return planesWithSeatsInCorrectLocation;
		} else if (!planesWithOutSeatsInCorrectLocation.isEmpty()) {
			return planesWithOutSeatsInCorrectLocation;
		} else if (!planesWithSeats.isEmpty()) {
			return planesWithSeats;
		} else {
			return planes;
		}
		
	}
	
	private boolean planeHasSeats(Aircraft plane, Route currentRoute, FlightInfo flight){
		
		LocalDate departureDate = flight.getDepartureDateTime().toLocalDate();
		int passengerNumber = passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), departureDate);
		
		if (plane.getSeats() >= passengerNumber) {
			return true;
		} else {
			return false;
		}
	}
	
	public void getACaptain(FlightInfo flight) throws DoubleBookedException {
		for (Pilot captain : getPossibleCaptainsForFlight(flight)) {
			if (!schedule.hasConflict(captain, flight)) {
				schedule.allocateCaptainTo(captain, flight);
				break;
			}
		}
	}
	
	private List<Pilot> getPossibleCaptainsForFlight(FlightInfo flight) {
		
		ArrayList<Pilot> crewQualifiedRestedLocationUnderHours = new ArrayList<>();
		ArrayList<Pilot> crewQualifiedRestedLocation = new ArrayList<>();
		ArrayList<Pilot> crewQualifiedRested = new ArrayList<>();
		ArrayList<Pilot> crewQualified = new ArrayList<>();
		ArrayList<Pilot> crewLeft = new ArrayList<>();
		
		for (Pilot crew : pilots) {
			if (!schedule.hasConflict(crew, flight) && crew.getRank() == Rank.CAPTAIN) {
				if(crew.isQualifiedFor(schedule.getAircraftFor(flight))
					&& crewIsRested(crew, flight)
					&& crewInCorrectLocation(crew, flight)
					&& !crewHitMaxHours(crew)) {
					
					crewQualifiedRestedLocationUnderHours.add(crew);
				} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))
						&& crewIsRested(crew, flight)
						&& crewInCorrectLocation(crew, flight)) {
						
					crewQualifiedRestedLocation.add(crew);
				} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))
						&& crewIsRested(crew, flight)) {
						
					crewQualifiedRested.add(crew);
				} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))) {
					
					crewQualified.add(crew);
				} else {
					
					crewLeft.add(crew);
				}
			}
		}
		
		if (!crewQualifiedRestedLocationUnderHours.isEmpty()) {
			return crewQualifiedRestedLocationUnderHours;
		} else if (!crewQualifiedRestedLocation.isEmpty()) {
			return crewQualifiedRestedLocation;
		} else if (!crewQualifiedRested.isEmpty()) {
			return crewQualifiedRested;
		} else if (!crewQualified.isEmpty()) {
			return crewQualified;
		} else {
			return crewLeft;
		}

	}

	public void getAFirstOfficer(FlightInfo flight) throws DoubleBookedException {
		for (Pilot firstOfficer : getPossibleFirstOfficersForFlight(flight)) {
			if (!schedule.hasConflict(firstOfficer, flight)) {
				schedule.allocateFirstOfficerTo(firstOfficer, flight);
				break;
			}	
		}
	}
	
	private List<Pilot> getPossibleFirstOfficersForFlight(FlightInfo flight) {
		
		ArrayList<Pilot> crewQualifiedRestedLocationUnderHours = new ArrayList<>();
		ArrayList<Pilot> crewQualifiedRestedLocation = new ArrayList<>();
		ArrayList<Pilot> crewQualifiedRested = new ArrayList<>();
		ArrayList<Pilot> crewQualified = new ArrayList<>();
		ArrayList<Pilot> crewLeft = new ArrayList<>();

		
		for (Pilot crew : pilots) {
			if (!schedule.hasConflict(crew, flight) && crew.getRank() == Rank.FIRST_OFFICER ) {
				if(crew.isQualifiedFor(schedule.getAircraftFor(flight))
					&& crewIsRested(crew, flight)
					&& crewInCorrectLocation(crew, flight)
					&& !crewHitMaxHours(crew)) {
					
					crewQualifiedRestedLocationUnderHours.add(crew);
				} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))
						&& crewIsRested(crew, flight)
						&& crewInCorrectLocation(crew, flight)) {
						
					crewQualifiedRestedLocation.add(crew);
				} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))
						&& crewIsRested(crew, flight)) {
						
					crewQualifiedRested.add(crew);
				} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))) {
					
					crewQualified.add(crew);
				} else {
					crewLeft.add(crew);
				}
			}
		}
		
		if (!crewQualifiedRestedLocationUnderHours.isEmpty()) {
			return crewQualifiedRestedLocationUnderHours;
		} else if (!crewQualifiedRestedLocation.isEmpty()) {
			return crewQualifiedRestedLocation;
		} else if (!crewQualifiedRested.isEmpty()) {
			return crewQualifiedRested;
		} else if (!crewQualified.isEmpty()) {
			return crewQualified;
		} else {
			return crewLeft;
		}

	}
	
	public void getCabinCrew(FlightInfo flight) throws DoubleBookedException {
		
		List<CabinCrew> possibleCrew = getCabinCrewInBestOrder(flight);
		
		for (CabinCrew crewMember : possibleCrew) {
			
			if(!schedule.hasConflict(crewMember, flight) 
					&& schedule.getCabinCrewOf(flight).size() <= schedule.getAircraftFor(flight).getCabinCrewRequired()) {
				
				schedule.allocateCabinCrewTo(crewMember, flight);
				
			}
			if (schedule.getCabinCrewOf(flight).size() == schedule.getAircraftFor(flight).getCabinCrewRequired()) {
				
				break;
				
			}
		}
	}
	
	private List<CabinCrew> getCabinCrewInBestOrder(FlightInfo flight){
		
		ArrayList<CabinCrew> crewQualifiedRestedLocationUnderHours = new ArrayList<>();
		ArrayList<CabinCrew> crewQualifiedRestedLocation = new ArrayList<>();
		ArrayList<CabinCrew> crewQualifiedRested = new ArrayList<>();
		ArrayList<CabinCrew> crewQualified = new ArrayList<>();
		ArrayList<CabinCrew> crewLeft = new ArrayList<>();
		
		for (CabinCrew crew : cabinCrew) {
			if (!schedule.hasConflict(crew, flight)) {
				if(crew.isQualifiedFor(schedule.getAircraftFor(flight))
						&& crewIsRested(crew, flight)
						&& crewInCorrectLocation(crew, flight)
						&& !crewHitMaxHours(crew)) {
					
					crewQualifiedRestedLocationUnderHours.add(crew);
				} else if(crew.isQualifiedFor(schedule.getAircraftFor(flight))
						&& crewIsRested(crew, flight)
						&& crewInCorrectLocation(crew, flight)) {
					
					crewQualifiedRestedLocation.add(crew);
				} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))
						&& crewIsRested(crew, flight)) {
					
					crewQualifiedRested.add(crew);
				} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))) {
					
					crewQualified.add(crew);
				} else {
					crewLeft.add(crew);
				}
			}
		}
		
		ArrayList<CabinCrew> crewInOrder = new ArrayList<>();
		
		crewInOrder.addAll(crewQualifiedRestedLocationUnderHours);
		crewInOrder.addAll(crewQualifiedRestedLocation);
		crewInOrder.addAll(crewQualifiedRested);
		crewInOrder.addAll(crewQualified);
		crewInOrder.addAll(crewLeft);
		
		return crewInOrder;
		
	}
	
	private boolean crewInCorrectLocation(Crew crew, FlightInfo flight) {
		
		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);
		
		boolean isFlightDepartingFromPreviousArrival = false;
		boolean isAtHomeBase = false;
		boolean hadTimeToTravel = false;
		
		if (crewFlights.size() > 1) {
		
			String crewPreviousArrival = crewFlights.get(crewFlights.size()-2).getFlight().getArrivalAirportCode();
			
			String crewCurrentDeparture = crewFlights.get(crewFlights.size()-1).getFlight().getDepartureAirportCode();

			isFlightDepartingFromPreviousArrival = crewPreviousArrival.equals(crewCurrentDeparture);
			
			if(!isFlightDepartingFromPreviousArrival && !Utilities.airportIsInUK(crewCurrentDeparture)) {
				if(crewFlights.get(crewFlights.size()-1).getDepartureDateTime().isAfter(crewFlights.get(crewFlights.size()-2).getLandingDateTime().plusHours(48))){
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
			
			LocalTime crewPreviousArrival = crewFlights.get(crewFlights.size()-2).getFlight().getArrivalTime();
			LocalTime crewCurrentDeparture = crewFlights.get(crewFlights.size()-1).getFlight().getDepartureTime();
					
			return crewCurrentDeparture.isAfter(crewPreviousArrival.plusHours(12));
		
		} else {
			return true;
		}
	}
	
	private boolean crewHitMaxHours(Crew crew) {
		List<FlightInfo> crewsFlights = schedule.getCompletedAllocationsFor(crew);
		
		int hoursWorked = 0;
		
		for (FlightInfo flight : crewsFlights) {
			hoursWorked += flight.getFlight().getDuration().toHours();
		}
		
		if (hoursWorked > 100) {
			return true;
		} else {
			return false;
		}
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
