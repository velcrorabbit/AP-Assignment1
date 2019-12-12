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
					&& plane.getStartingPosition().equals(currentRoute.getDepartureAirportCode()) 
					&& !schedule.hasConflict(plane, flight)) {
				
				planesWithSeatsInCorrectLocation.add(plane);
				
			} else if (plane.getSeats() >= passengers.getPassengerNumbersFor(currentRoute.getFlightNumber(), startDate) 
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
	
	public void getACaptain(FlightInfo flight) throws DoubleBookedException {
		for (Pilot captain : getPossibleCaptainsForFlight(flight)) {
			if(captain.getRank().equals(Rank.CAPTAIN)) {
				if(!schedule.hasConflict(captain, flight)) {
					schedule.allocateCaptainTo(captain, flight);
					break;		
				}
			}
		}
	}
	
	private List<Pilot> getPossibleCaptainsForFlight(FlightInfo flight) {
		
		ArrayList<Pilot> captainsLocationRankQualified = new ArrayList<>();
		ArrayList<Pilot> captainsRankQualified = new ArrayList<>();
		ArrayList<Pilot> captainsRank = new ArrayList<>();
		
		for (Pilot captain : pilots) {
			if(flight.getFlight().getDepartureAirportCode().equals(captain.getHomeBase()) 
					&& captain.getRank() == Rank.CAPTAIN 
					&& captain.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(captain, flight)) {
				
				captainsLocationRankQualified.add(captain);
				
			} else if (captain.getRank() == Rank.CAPTAIN 
					&& captain.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(captain, flight)) {
				
				captainsRankQualified.add(captain);
				
			} else if (captain.getRank() == Rank.CAPTAIN
					&& !schedule.hasConflict(captain, flight)) {
				
				captainsRank.add(captain);
				
			}
		}
		
		if (!captainsLocationRankQualified.isEmpty()) {
			return captainsLocationRankQualified;
		} else if (!captainsRankQualified.isEmpty()) {
			return captainsRankQualified;
		} else if (!captainsRank.isEmpty()) {
			return captainsRank;
		} else {
			return pilots;
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
		
		ArrayList<Pilot> firstOfficersLocationRankQualified = new ArrayList<>();
		ArrayList<Pilot> firstOfficersRankQualified = new ArrayList<>();
		ArrayList<Pilot> firstOfficerRank = new ArrayList<>();
		
		for (Pilot firstOfficer : pilots) {
			if(flight.getFlight().getDepartureAirportCode().equals(firstOfficer.getHomeBase()) 
					&& firstOfficer.getRank() == Rank.FIRST_OFFICER 
					&& firstOfficer.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(firstOfficer, flight)) {
				
				firstOfficersLocationRankQualified.add(firstOfficer);
				
			} else if (firstOfficer.getRank() == Rank.FIRST_OFFICER  
					&& firstOfficer.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(firstOfficer, flight)) {
				
				firstOfficersRankQualified.add(firstOfficer);
				
			} else if (firstOfficer.getRank() == Rank.FIRST_OFFICER 
					&& !schedule.hasConflict(firstOfficer, flight)) {
				
				firstOfficerRank.add(firstOfficer);
				
			}
		}
		
		if (!firstOfficersLocationRankQualified.isEmpty()) {
			return firstOfficersLocationRankQualified;
		} else if (!firstOfficersRankQualified.isEmpty()) {
			return firstOfficersRankQualified;
		} else if (!firstOfficerRank.isEmpty()) {
			return firstOfficerRank;
		} else {
			return pilots;
		}

	}
	
	public void getCabinCrew(FlightInfo flight) throws DoubleBookedException {
		for (CabinCrew crewMember : getPossibleCrewForFlight(flight)) {
			if(!schedule.hasConflict(crewMember, flight) && schedule.getCabinCrewOf(flight).size() <= schedule.getAircraftFor(flight).getCabinCrewRequired()) {
				schedule.allocateCabinCrewTo(crewMember, flight);
			}
			if (schedule.getCabinCrewOf(flight).size() == schedule.getAircraftFor(flight).getCabinCrewRequired()) {
				break;
			}
		}
	}
	
	private List<CabinCrew> getPossibleCrewForFlight(FlightInfo flight){
		ArrayList<CabinCrew> crewLocationQualified = new ArrayList<>();
		ArrayList<CabinCrew> crewQualified = new ArrayList<>();
		
		for (CabinCrew crew : cabinCrew) {
			if(flight.getFlight().getDepartureAirportCode().equals(crew.getHomeBase()) 
					&& crew.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(crew, flight)) {
				
				crewLocationQualified.add(crew);
				
			} else if (crew.isQualifiedFor(schedule.getAircraftFor(flight))
					&& !schedule.hasConflict(crew, flight)) {
				
				crewQualified.add(crew);
				
			}
		}
		
		if (!crewLocationQualified.isEmpty()) {
			return crewLocationQualified;
		} else if (!crewQualified.isEmpty()) {
			return crewQualified;
		} else {
			return cabinCrew;
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
