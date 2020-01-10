package solution;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import baseclasses.Aircraft;
import baseclasses.Aircraft.Manufacturer;
import baseclasses.DataLoadingException;
import baseclasses.IAircraftDAO;


/**
 * The AircraftDAO class is responsible for loading aircraft data from CSV files
 * and contains methods to help the system find aircraft when scheduling
 */
public class AircraftDAO implements IAircraftDAO {
	
	private List<Aircraft> planes = new ArrayList<>();
	
	/**
	 * Loads the aircraft data from the specified file, adding them to the currently loaded aircraft
	 * Multiple calls to this function, perhaps on different files, would thus be cumulative
	 * @param path A Path pointing to the file from which data could be loaded
	 * @throws DataLoadingException if anything goes wrong. The exception's "cause" indicates the underlying exception
     *
	 * Initially, this contains some starter code to help you get started in reading the CSV file...
	 */
	@Override
	public void loadAircraftData(Path path) throws DataLoadingException {	
		try {
			//open the file
			BufferedReader reader = Files.newBufferedReader(path);
			
			//read the file line by line
			String line = "";
			
			//skip the first line of the file - headers
			reader.readLine();

			while( (line = reader.readLine()) != null) {
				//each line has fields separated by commas, split into an array of fields
				String[] fields = line.split(",");
				// Create an aircraft object for each aircraft in the file
				Aircraft aircraft = new Aircraft();
				aircraft.setTailCode(fields[0]);
				aircraft.setTypeCode(fields[1]);
				aircraft.setManufacturer(Manufacturer.valueOf(fields[2].toUpperCase()));
				aircraft.setModel(fields[3]);
				aircraft.setSeats(Integer.parseInt(fields[4]));
				aircraft.setCabinCrewRequired(Integer.parseInt(fields[5]));
				aircraft.setStartingPosition(fields[6]);
				// Add the aircraft to the planes Array list.
				planes.add(aircraft);
							
			}			
			reader.close();

		} catch (IOException | NullPointerException ioe) { 
			//There was a problem reading the file
			throw new DataLoadingException(ioe);
		} catch(IllegalArgumentException iae) {
			System.out.println("Garbage file detected");
			throw new DataLoadingException(iae);	
		}

	}
	
	/**
	 * Returns a list of all the loaded Aircraft with at least the specified number of seats
	 * @param seats the number of seats required
	 * @return a List of all the loaded aircraft with at least this many seats
	 */
	@Override
	public List<Aircraft> findAircraftBySeats(int seats) {
		
		List<Aircraft> aircraft = new ArrayList<>();
		
		for (Aircraft plane : planes) {
			if (plane.getSeats() >= seats) {
				aircraft.add(plane);
			}
		}
		
		return aircraft;
	}

	/**
	 * Returns a list of all the loaded Aircraft that start at the specified airport code
	 * @param startingPosition the three letter airport code of the airport at which the desired aircraft start
	 * @return a List of all the loaded aircraft that start at the specified airport
	 */
	@Override
	public List<Aircraft> findAircraftByStartingPosition(String startingPosition) {

		List<Aircraft> aircraft = new ArrayList<>();
		
		for (Aircraft plane : planes) {
			if (plane.getStartingPosition().equals(startingPosition)) {
				aircraft.add(plane);
			}
		}
		
		return aircraft;
	}

	/**
	 * Returns the individual Aircraft with the specified tail code.
	 * @param tailCode the tail code for which to search
	 * @return the aircraft with that tail code, or null if not found
	 */
	@Override
	public Aircraft findAircraftByTailCode(String tailCode) {
		
		Aircraft aircraft = null;
		
		for (Aircraft plane : planes) {
			if (plane.getTailCode().equals(tailCode)) {
				aircraft = plane;
			}
		}
		
		return aircraft;
	}

	/**
	 * Returns a List of all the loaded Aircraft with the specified type code
	 * @param typeCode the type code of the aircraft you wish to find
	 * @return a List of all the loaded Aircraft with the specified type code
	 */
	@Override
	public List<Aircraft> findAircraftByType(String typeCode) {
		
		List<Aircraft> aircraft = new ArrayList<>();
		
		for (Aircraft plane : planes) {
			if (plane.getTypeCode().equals(typeCode)) {
				aircraft.add(plane);
			}
		}
		
		return aircraft;
	}

	/**
	 * Returns a List of all the currently loaded aircraft
	 * @return a List of all the currently loaded aircraft
	 */
	@Override
	public List<Aircraft> getAllAircraft() {
		List<Aircraft> returnPlanes = planes;
		
		return returnPlanes;
	}

	/**
	 * Returns the number of aircraft currently loaded 
	 * @return the number of aircraft currently loaded
	 */
	@Override
	public int getNumberOfAircraft() {
		return planes.size();
	}

	/**
	 * Unloads all of the aircraft currently loaded, ready to start again if needed
	 */
	@Override
	public void reset() {
		planes.clear();
	}
}