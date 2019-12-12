package solution;
import java.time.LocalDate;
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
	
	@Override
	public Schedule generateSchedule(IAircraftDAO aircraft, ICrewDAO crew, IRouteDAO route, IPassengerNumbersDAO passengers,
			LocalDate startDate, LocalDate endDate) {
		
		this.cabinCrew = crew.getAllCabinCrew();
		this.pilots = crew.getAllPilots();
		this.planes = aircraft.getAllAircraft();
		this.passengers = (PassengerNumbersDAO) passengers;
		this.startDate = startDate;
		//this.endDate = endDate;
		//this.routes = route.getAllRoutes();

		schedule = new Schedule(route, startDate, endDate);

		Vector<FlightInfo> vector = new Vector<>();
		for (FlightInfo flight : schedule.getRemainingAllocations()) {
			vector.add(flight);
		}
		
		for (FlightInfo flight: vector) {
			
			Route currentRoute = flight.getFlight();
			
			//System.out.println(currentRoute.getFlightNumber());

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
				//System.out.println(schedule.isCompleted());
			} catch (InvalidAllocationException e) {
				
				System.out.println(schedule.getAircraftFor(flight).getModel());
				System.out.println(schedule.getCabinCrewOf(flight).toString());
				System.out.println(schedule.getCaptainOf(flight).getForename());
				System.out.println(schedule.getFirstOfficerOf(flight).getForename());
				
				e.printStackTrace();
			}

		}
		
		System.out.println("Done");

		return schedule;
	}
	
	public void getAPlane(FlightInfo flight, Route currentRoute) throws DoubleBookedException  {	
		Aircraft bestPlane = getPossiblePlanesForFlight(flight, currentRoute).get(0);
		
		for (Aircraft plane : getPossiblePlanesForFlight(flight, currentRoute)) {
			if (!schedule.hasConflict(plane, flight) && plane.getSeats() < bestPlane.getSeats()) {
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
			
			if (plane.getSeats() >= passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), startDate)
					&& isPlaneInAndOutSamePlace(flight, plane)
					&& !schedule.hasConflict(plane, flight)) {
				
				planesWithSeatsInCorrectLocation.add(plane);
				
			} else if (isPlaneInAndOutSamePlace(flight, plane)
					&& !schedule.hasConflict(plane, flight)) {
				
				planesWithOutSeatsInCorrectLocation.add(plane);
				
			} else if (plane.getSeats() >= passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), startDate)
					&& !schedule.hasConflict(plane, flight)) {
				
				planesWithSeats.add(plane);
				
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
	
	private boolean isPlaneInAndOutSamePlace(FlightInfo flight, Aircraft plane) {
		boolean goodLocation = false;
		
		List<FlightInfo> planeFlights = schedule.getCompletedAllocationsFor(plane);
		
		if (planeFlights.size() > 1) {
			if (flight.getFlight().getDepartureAirport().equals(planeFlights.get(planeFlights.size()-2).getFlight().getArrivalAirport())
					|| plane.getStartingPosition().equals(flight.getFlight().getDepartureAirport())){
				goodLocation = true;
			}
		}
		return goodLocation;
		
	}
	
	public void getACaptain(FlightInfo flight) throws DoubleBookedException {
		for (Pilot captain : getPossibleCaptainsForFlight(flight)) {
			if(!schedule.hasConflict(captain, flight)) {
				schedule.allocateCaptainTo(captain, flight);
				break;		
			}
		}
	}
	
	private List<Pilot> getPossibleCaptainsForFlight(FlightInfo flight) {
		
		ArrayList<Pilot> captains = new ArrayList<>();
		
		for (Pilot person : pilots) {
			if (person.getRank() == Rank.CAPTAIN) {
				captains.add(person);
			}
				
		}
		
		ArrayList<Pilot> captainsLocationRankQualifiedRested = new ArrayList<>();
		ArrayList<Pilot> captainsRankQualifiedRested = new ArrayList<>();
		ArrayList<Pilot> captainsRank = new ArrayList<>();
		
		for (Pilot captain : captains) {
			
			if(flight.getFlight().getDepartureAirportCode().equals(captain.getHomeBase())
					&& captain.isQualifiedFor(schedule.getAircraftFor(flight).getTypeCode())
					&& !schedule.hasConflict(captain, flight)
					&& hadEnoughRestInUk(captain, flight)) {

				captainsLocationRankQualifiedRested.add(captain);
				
			} else if (captain.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(captain, flight)
					&& hadEnoughRestInUk(captain, flight)) {
				
				captainsRankQualifiedRested.add(captain);
				
			} else if (!schedule.hasConflict(captain, flight)) {
				
				captainsRank.add(captain);
				
			}
		}
		
		if (!captainsLocationRankQualifiedRested.isEmpty()) {
			return captainsLocationRankQualifiedRested;
		} else if (!captainsRankQualifiedRested.isEmpty()) {
			return captainsRankQualifiedRested;
		} else if (!captainsRank.isEmpty()) {
			return captainsRank;
		} else {
			return captains;
		}

	}

	public void getAFirstOfficer(FlightInfo flight) throws DoubleBookedException {
		for (Pilot firstOfficer : getPossibleFirstOfficersForFlight(flight)) {
			if(firstOfficer.getRank().equals(Rank.FIRST_OFFICER)) {
				if(!schedule.hasConflict(firstOfficer, flight)) {
					schedule.allocateFirstOfficerTo(firstOfficer, flight);
					break;		
				}
			}
		}
	}
	
	private List<Pilot> getPossibleFirstOfficersForFlight(FlightInfo flight) {
		
		ArrayList<Pilot> firstOfficers = new ArrayList<>();
		
		for (Pilot person : pilots) {
			if (person.getRank() == Rank.FIRST_OFFICER) {
				firstOfficers.add(person);
			}
				
		}
		
		ArrayList<Pilot> firstOfficersLocationRankQualifiedRested = new ArrayList<>();
		ArrayList<Pilot> firstOfficersRankQualifiedRested = new ArrayList<>();
		ArrayList<Pilot> firstOfficerRankRested = new ArrayList<>();
		ArrayList<Pilot> firstOfficerRank = new ArrayList<>();
		
		for (Pilot firstOfficer : firstOfficers) {
			if(flight.getFlight().getDepartureAirportCode().equals(firstOfficer.getHomeBase())
					&& firstOfficer.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(firstOfficer, flight)
					&& hadEnoughRestInUk(firstOfficer, flight)) {
				
				firstOfficersLocationRankQualifiedRested.add(firstOfficer);
				
			} else if (firstOfficer.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(firstOfficer, flight)
					&& hadEnoughRestInUk(firstOfficer, flight)) {
				
				firstOfficersRankQualifiedRested.add(firstOfficer);
				
			} else if (!schedule.hasConflict(firstOfficer, flight)
					&& hadEnoughRestInUk(firstOfficer, flight)) {
				
				firstOfficerRankRested.add(firstOfficer);
				
			} else if (!schedule.hasConflict(firstOfficer, flight)) {
				
				firstOfficerRank.add(firstOfficer);
				
			}
		}
		
		if (!firstOfficersLocationRankQualifiedRested.isEmpty()) {
			return firstOfficersLocationRankQualifiedRested;
		} else if (!firstOfficersRankQualifiedRested.isEmpty()) {
			return firstOfficersRankQualifiedRested;
		} else if (!firstOfficerRankRested.isEmpty()) {
			return firstOfficerRankRested;
		} else if (!firstOfficerRank.isEmpty()) {
			return firstOfficerRank;
		} else {
			return pilots;
		}

	}
	
	public void getCabinCrew(FlightInfo flight) throws DoubleBookedException {
		for (CabinCrew crewMember : getPossibleCabinCrewForFlight(flight)) {
			if(!schedule.hasConflict(crewMember, flight) && schedule.getCabinCrewOf(flight).size() <= schedule.getAircraftFor(flight).getCabinCrewRequired()) {
				schedule.allocateCabinCrewTo(crewMember, flight);
			}
			if (schedule.getCabinCrewOf(flight).size() == schedule.getAircraftFor(flight).getCabinCrewRequired()) {
				break;
			}
		}
	}
	
	private List<CabinCrew> getPossibleCabinCrewForFlight(FlightInfo flight){

		ArrayList<CabinCrew> crewLocationQualifiedRested = new ArrayList<>();
		ArrayList<CabinCrew> crewQualifiedRested = new ArrayList<>();
		ArrayList<CabinCrew> crewQualified = new ArrayList<>();
		
		for (CabinCrew crew : cabinCrew) {
			if(flight.getFlight().getDepartureAirportCode().equals(crew.getHomeBase()) 
					&& crew.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(crew, flight)
					&& hadEnoughRestInUk(crew, flight)) {
				
				crewLocationQualifiedRested.add(crew);
				
			} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(crew, flight)
					&& hadEnoughRestInUk(crew, flight)) {
				
				crewQualifiedRested.add(crew);
				
			} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(crew, flight)) {
				
				crewQualified.add(crew);
				
			}
		}
		
		if (!crewLocationQualifiedRested.isEmpty()) {
			return crewLocationQualifiedRested;
		} else if (!crewQualifiedRested.isEmpty()) {
			return crewQualifiedRested;
		} else if (!crewQualified.isEmpty()) {
			return crewQualified;
		} else {
			return cabinCrew;
		}
		
	}

	private boolean hadEnoughRestInUk(Crew crew, FlightInfo flight) {
		
		boolean rested = true;
		
		List<FlightInfo> flights = schedule.getCompletedAllocationsFor(crew);
		
		if(flights.size() > 1 && Utilities.airportIsInUK(flights.get(flights.size()-1).getFlight().getDepartureAirport())) {
			if(flight.getDepartureDateTime().isAfter(flights.get(flights.size()-2).getLandingDateTime().plusHours(24))) {
				rested = true;
			}
			rested = false;
		}
		return rested;
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
