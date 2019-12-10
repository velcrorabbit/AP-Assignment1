package solution;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import baseclasses.*;
import baseclasses.Pilot.Rank;

public class Scheduler implements IScheduler {
	
	List<CabinCrew> cabinCrew;
	List<Pilot> pilots;
	List<Aircraft> aircraft;
	List<Route> routes;
	PassengerNumbersDAO passengers;
	LocalDate startDate;
	LocalDate endDate;
	Schedule schedule;
	
	@Override
	public Schedule generateSchedule(IAircraftDAO aircraft, ICrewDAO crew, IRouteDAO route, IPassengerNumbersDAO passengers,
			LocalDate startDate, LocalDate endDate) {
		
		this.cabinCrew = crew.getAllCabinCrew();
		this.pilots = crew.getAllPilots();
		this.aircraft = aircraft.getAllAircraft();
		this.passengers = (PassengerNumbersDAO) passengers;
		this.startDate = startDate;
		this.endDate = endDate;
		this.routes = route.getAllRoutes();
		
		schedule = new Schedule(route, startDate, endDate);
		
		Vector<FlightInfo> vector = new Vector<>();
		for (FlightInfo flight : schedule.getRemainingAllocations()) {
			vector.add(flight);
		}

		for (FlightInfo flight : vector) {
			Route currentRoute = flight.getFlight();
			
			try {
				while (!this.aircraft.isEmpty()){
					Aircraft currentPlane = getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute);
					schedule.allocateAircraftTo(currentPlane, flight);
					currentPlane.setStartingPosition(currentRoute.getArrivalAirportCode());
					
					Pilot currentPilot = (Pilot) getCaptainForFlight(currentRoute);
					schedule.allocateCaptainTo(currentPilot, flight);
					currentPilot.setHomeBase(currentRoute.getArrivalAirportCode());
					
					Pilot currentFirstOfficer = (Pilot)getFirstOfficerForFlight(currentRoute);
					schedule.allocateFirstOfficerTo(currentFirstOfficer, flight);
					currentFirstOfficer.setHomeBase(currentRoute.getArrivalAirportCode());
					
					for (int i = 0; i <= schedule.getAircraftFor(flight).getCabinCrewRequired(); i++) {
						CabinCrew current = (CabinCrew) getCabinCrewForFlight(currentRoute);
						schedule.allocateCabinCrewTo((CabinCrew)current, flight);
						//current.setHomeBase(currentRoute.getArrivalAirportCode());
					}
	
					schedule.completeAllocationFor(flight);
					
				}

			} catch ( DoubleBookedException | InvalidAllocationException | NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return schedule;
	}
	
	private List<Aircraft> getPossiblePlanesForFlight(Route currentRoute) {
				
		ArrayList<Aircraft> planesWithSeatsInCorrectLocation = new ArrayList<>();
		ArrayList<Aircraft> planesWithSeats = new ArrayList<>();
		ArrayList<Aircraft> planesWithOutSeatsInCorrectLocation = new ArrayList<>();
		
		for (Aircraft plane : aircraft) {
			
			if (plane.getSeats() >= passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), startDate) 
					&& plane.getStartingPosition().equals(currentRoute.getDepartureAirportCode())) {
				planesWithSeatsInCorrectLocation.add(plane);
			}else if (plane.getSeats() >= passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), startDate)) {
				planesWithSeats.add(plane);
			} else if (plane.getStartingPosition().equals(currentRoute.getDepartureAirportCode())) {
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
			return aircraft;
		}
		
		
	}

	private Aircraft getPlaneWithMinimumSeats(List<Aircraft> possiblePlanes, Route currentRoute) {
		
		Aircraft bestPlane = possiblePlanes.get(0);

		for (Aircraft plane : possiblePlanes) {
			if (plane.getSeats() < bestPlane.getSeats() && plane.getStartingPosition().equals(currentRoute.getDepartureAirportCode())) {
				bestPlane = plane;
			}
		}
		return bestPlane;
	}
	
	private Crew getCaptainForFlight(Route currentRoute) { 
		
		Crew currentPilot = null;
		
		
		while (currentPilot == null) {
			for (Pilot pilot : pilots) {
				if(currentRoute.getDepartureAirportCode().equals(pilot.getHomeBase()) 
						&& pilot.getRank() == Rank.CAPTAIN 
						&& pilot.isQualifiedFor(getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute))) {
					currentPilot = pilot;
					break;
				} else if (pilot.getRank() == Rank.CAPTAIN 
						&& pilot.isQualifiedFor(getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute))) {
					currentPilot = pilot;
					break;
				}  else if (currentRoute.getDepartureAirportCode().equals(pilot.getHomeBase()) 
						&& pilot.getRank() == Rank.FIRST_OFFICER 
						&& pilot.isQualifiedFor(getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute))) {
					currentPilot = pilot;
					break;
				} else if (pilot.getRank() == Rank.CAPTAIN) {
					currentPilot = pilot;
					break;
				} else if (pilot.getRank() == Rank.FIRST_OFFICER 
						&& pilot.isQualifiedFor(getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute))) {
					currentPilot = pilot;
					break;
				} else if (pilot.getRank() == Rank.FIRST_OFFICER) {
					currentPilot = pilot;
					break;
				}
				
			}
		}
		return currentPilot;
	}
	
	private Crew getFirstOfficerForFlight(Route currentRoute) {
		Crew currentFirstOfficer = null;
		
		for (Pilot pilot : pilots) {
			if (currentRoute.getDepartureAirportCode().equals(pilot.getHomeBase()) 
					&& pilot.getRank() == Rank.FIRST_OFFICER 
					&& pilot.isQualifiedFor(getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute))) {
				currentFirstOfficer = pilot;
				break;
			} else if (pilot.getRank() == Rank.FIRST_OFFICER 
					&& pilot.isQualifiedFor(getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute))) {
				currentFirstOfficer = pilot;
				break;
			} else if (pilot.getRank() == Rank.FIRST_OFFICER) {
				currentFirstOfficer = pilot;
				break;
			}
		}
		return currentFirstOfficer;
	}

	private Crew getCabinCrewForFlight(Route currentRoute) {
		Crew crew = null;
		
		for (Crew crewMember : cabinCrew) {
			if(currentRoute.getDepartureAirportCode().equals(crewMember.getHomeBase()) 
					&& crewMember.isQualifiedFor(getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute))) {
				crew = crewMember;
				break;
			} else if (crewMember.isQualifiedFor(getPlaneWithMinimumSeats(getPossiblePlanesForFlight(currentRoute), currentRoute)) 
					|| currentRoute.getDepartureAirportCode().equals(crewMember.getHomeBase())){
				crew = crewMember;
				break;
			}
		}
		
		return crew;
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
