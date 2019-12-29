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
	private LocalDate endDate;
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
		this.endDate = endDate;
		
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
		
		List<Aircraft> planesInCorrectLocation = aircraft.findAircraftByStartingPosition(currentRoute.getDepartureAirportCode());
		
		for (Aircraft plane : planes) {
			
			boolean hasSeats = planeHasSeats(plane, currentRoute, flight);
			boolean inCorrectLocation = planesInCorrectLocation.contains(plane);
			boolean hasNoConflict = !schedule.hasConflict(plane, flight);
			
			if (hasSeats && inCorrectLocation && hasNoConflict) {
				
				planesWithSeatsInCorrectLocation.add(plane);
				
			} else if (hasSeats && hasNoConflict) {
				
				planesWithSeats.add(plane);
				
			} else if (inCorrectLocation && hasNoConflict) {
				
				planesWithOutSeatsInCorrectLocation.add(plane);
				
			}
		}
		
		if (!planesWithSeatsInCorrectLocation.isEmpty()) {
			return planesWithSeatsInCorrectLocation;
		} else if (!planesWithSeats.isEmpty()) {
			return planesWithSeats;
		} else if (!planesWithOutSeatsInCorrectLocation.isEmpty()) {
			return planesWithOutSeatsInCorrectLocation;
		} else {
			return planes;
		}
		
	}
	
	private boolean planeHasSeats(Aircraft plane, Route currentRoute, FlightInfo flight){
		
		LocalDate departureDate = flight.getDepartureDateTime().toLocalDate();
		int passengerNumber = passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), departureDate);
		
		List<Aircraft> planesWithSeats = aircraft.findAircraftBySeats(passengerNumber);
		
		if (planesWithSeats.contains(plane)) {
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
			
			boolean hasNoConflict = !schedule.hasConflict(crew, flight);
			boolean isCaptain = crew.getRank() == Rank.CAPTAIN;
			boolean qualified = crew.isQualifiedFor(schedule.getAircraftFor(flight));
			boolean rested = crewIsRested(crew, flight);
			boolean correctLocation = crewInCorrectLocation(crew, flight);
			boolean underMaxHours = !crewHitMaxHours(crew);
			
			if (hasNoConflict && isCaptain) {
				if(rested && correctLocation && underMaxHours && qualified) {
					
					crewQualifiedRestedLocationUnderHours.add(crew);
					
				} else if (rested && correctLocation && qualified) {
						
					crewQualifiedRestedLocation.add(crew);
					
				} else if (rested && qualified) {
						
					crewQualifiedRested.add(crew);
					
				} else if (qualified) {
					
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
			
			boolean hasNoConflict = !schedule.hasConflict(crew, flight);
			boolean isFirstOfficer = crew.getRank() == Rank.FIRST_OFFICER;
			boolean qualified = crew.isQualifiedFor(schedule.getAircraftFor(flight));
			boolean rested = crewIsRested(crew, flight);
			boolean correctLocation = crewInCorrectLocation(crew, flight);
			boolean underMaxHours = !crewHitMaxHours(crew);
			
			if (hasNoConflict && isFirstOfficer) {
				if(rested && correctLocation && underMaxHours && qualified) {
					
					crewQualifiedRestedLocationUnderHours.add(crew);
					
				} else if (rested && correctLocation && qualified) {
						
					crewQualifiedRestedLocation.add(crew);
					
				} else if (rested && qualified) {
						
					crewQualifiedRested.add(crew);
					
				} else if (qualified) {
					
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
			
			boolean hasNoConflict = !schedule.hasConflict(crew, flight);
			boolean qualified = crew.isQualifiedFor(schedule.getAircraftFor(flight));
			boolean rested = crewIsRested(crew, flight);
			boolean correctLocation = crewInCorrectLocation(crew, flight);
			boolean underMaxHours = !crewHitMaxHours(crew);
			
			if (hasNoConflict) {
				
				if(qualified && rested && correctLocation && underMaxHours) {
					
					crewQualifiedRestedLocationUnderHours.add(crew);
					
				} else if(qualified && rested && correctLocation) {
					
					crewQualifiedRestedLocation.add(crew);
					
				} else if (qualified && rested) {
					
					crewQualifiedRested.add(crew);
					
				} else if (qualified) {
					
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
		/*
		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);
		
		boolean isFlightDepartingFromPreviousArrival = false;
		boolean isAtHomeBase = false;
		boolean hadTimeToTravel = false;
		
		if (crewFlights.size() > 1) {
		
			String crewPreviousArrival = crewFlights.get(crewFlights.size()-2).getFlight().getArrivalAirportCode();
			
			String crewCurrentDeparture = crewFlights.get(crewFlights.size()-1).getFlight().getDepartureAirportCode();

			isFlightDepartingFromPreviousArrival = crewPreviousArrival.equals(crewCurrentDeparture);
			
			if(!isFlightDepartingFromPreviousArrival) {
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
		*/
		return true;
	}
	
	private boolean crewIsRested(Crew crew, FlightInfo flight) {
/*
		List<FlightInfo> crewFlights = schedule.getCompletedAllocationsFor(crew);

		if (crewFlights.size() > 1) {
			
			LocalTime crewPreviousArrival = crewFlights.get(crewFlights.size()-2).getFlight().getArrivalTime();
			LocalTime crewCurrentDeparture = crewFlights.get(crewFlights.size()-1).getFlight().getDepartureTime();
			
			if(Utilities.airportIsInUK(crewFlights.get(crewFlights.size()-2).getFlight().getArrivalAirportCode()) 
					|| Utilities.airportIsInUK(crewFlights.get(crewFlights.size()-1).getFlight().getDepartureAirportCode())) {
				
				return crewCurrentDeparture.isAfter(crewPreviousArrival.plusHours(12));
			}

		}
		*/
		return true;

	}
	
	private boolean crewHitMaxHours(Crew crew) {
		/*
		List<FlightInfo> crewsFlights = schedule.getCompletedAllocationsFor(crew);
		
		LocalDate MonthStart = startDate.plusMonths(1);
		
		int hoursWorked = 0;
		
		
		for (FlightInfo flight : crewsFlights) {
			if(flight.getLandingDateTime().toLocalDate().isBefore(MonthStart)) {
				hoursWorked += flight.getFlight().getDuration().toHours();
				if(hoursWorked > 100) {
					return false;
				}
			} else {
				MonthStart = MonthStart.plusMonths(1);
				hoursWorked = 0;
			}
		}
		*/
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

}
