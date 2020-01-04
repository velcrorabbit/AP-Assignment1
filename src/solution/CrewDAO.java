package solution;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import baseclasses.CabinCrew;
import baseclasses.Crew;
import baseclasses.DataLoadingException;
import baseclasses.ICrewDAO;
import baseclasses.Pilot;
import baseclasses.Pilot.Rank;

import org.json.*;

/**
 * The CrewDAO is responsible for loading data from JSON-based crew files 
 * It contains various methods to help the scheduler find the right pilots and cabin crew
 */
public class CrewDAO implements ICrewDAO {
	
	private List<Pilot> pilots = new ArrayList<>();
	private List<CabinCrew> cabinCrew = new ArrayList<>();
	
	/**
	 * Loads the crew data from the specified file, adding them to the currently loaded crew
	 * Multiple calls to this function, perhaps on different files, would thus be cumulative
	 * @param path A Path pointing to the file from which data could be loaded
	 * @throws DataLoadingException if anything goes wrong. The exception's "cause" indicates the underlying exception
	 */
	@Override
	public void loadCrewData(Path path) throws DataLoadingException {
		try {
			BufferedReader bufferedReader = Files.newBufferedReader(path);
			String json ="";
			String line = "";
			while((line = bufferedReader.readLine()) != null)
				json = json + line;
			
			JSONObject JCrew = new JSONObject(json);
			
			//set all pilots
			
			JSONArray pilotArray = JCrew.getJSONArray("pilots");
			
			for(int i = 0; i<pilotArray.length(); i++) {
				Pilot pilot = new Pilot();
				pilot.setForename(getValueFromJSONObject("forename", pilotArray, i));
				pilot.setSurname(getValueFromJSONObject("surname", pilotArray, i));
				pilot.setRank(Rank.valueOf(getValueFromJSONObject("rank", pilotArray, i)));
				pilot.setHomeBase(getValueFromJSONObject("homebase", pilotArray, i));
				
				JSONArray typeRatings = pilotArray.getJSONObject(i).getJSONArray("typeRatings");
				for (Object type : typeRatings) {
					pilot.setQualifiedFor(type.toString());
				}
				
				pilots.add(pilot);
			}
			
			//set all crew
			
			JSONArray crewArray = JCrew.getJSONArray("cabincrew");	
			
			for(int i = 0; i<crewArray.length(); i++) {
				CabinCrew crew = new CabinCrew();
				crew.setForename(getValueFromJSONObject("forename", crewArray, i));
				crew.setSurname(getValueFromJSONObject("surname", crewArray, i));
				crew.setHomeBase(getValueFromJSONObject("homebase", crewArray, i));
				
				JSONArray typeRatings = crewArray.getJSONObject(i).getJSONArray("typeRatings");
				for (Object type : typeRatings) {
					crew.setQualifiedFor(type.toString());
				}
				
				cabinCrew.add(crew);
			}
			
		} catch (IOException | NullPointerException e) {
			e.printStackTrace();
			throw new DataLoadingException();
		} catch (JSONException je) {
			throw new DataLoadingException();
		}
		
		
	}
	
	private String getValueFromJSONObject(String key, JSONArray array, int i) {
		return array.getJSONObject(i).getString(key);
	}
	
	/**
	 * Returns a list of all the cabin crew based at the airport with the specified airport code
	 * @param airportCode the three-letter airport code of the airport to check for
	 * @return a list of all the cabin crew based at the airport with the specified airport code
	 */
	@Override
	public List<CabinCrew> findCabinCrewByHomeBase(String airportCode) {

		List<CabinCrew> crew = new ArrayList<>();
		
		for(CabinCrew person : cabinCrew) {
			if(person.getHomeBase().equals(airportCode)) {
				crew.add(person);
			}
		}
		
		return crew;
	}

	/**
	 * Returns a list of all the cabin crew based at a specific airport AND qualified to fly a specific aircraft type
	 * @param typeCode the type of plane to find cabin crew for
	 * @param airportCode the three-letter airport code of the airport to check for
	 * @return a list of all the cabin crew based at a specific airport AND qualified to fly a specific aircraft type
	 */
	@Override
	public List<CabinCrew> findCabinCrewByHomeBaseAndTypeRating(String typeCode, String airportCode) {
		List<CabinCrew> crew = new ArrayList<>();
		
		for(CabinCrew person : cabinCrew) {
			if(person.getHomeBase().equals(airportCode) && person.getTypeRatings().contains(typeCode)) {
				crew.add(person);
			}
		}
		
		return crew;
	}

	/**
	 * Returns a list of all the cabin crew currently loaded who are qualified to fly the specified type of plane
	 * @param typeCode the type of plane to find cabin crew for
	 * @return a list of all the cabin crew currently loaded who are qualified to fly the specified type of plane
	 */
	@Override
	public List<CabinCrew> findCabinCrewByTypeRating(String typeCode) {
		List<CabinCrew> crew = new ArrayList<>();
		
		for(CabinCrew person : cabinCrew) {
			if(person.getTypeRatings().contains(typeCode)) {
				crew.add(person);
			}
		}
		
		return crew;
	}

	/**
	 * Returns a list of all the pilots based at the airport with the specified airport code
	 * @param airportCode the three-letter airport code of the airport to check for
	 * @return a list of all the pilots based at the airport with the specified airport code
	 */
	@Override
	public List<Pilot> findPilotsByHomeBase(String airportCode) {
		List<Pilot> crew = new ArrayList<>();
		
		for(Pilot person : pilots) {
			if(person.getHomeBase().equals(airportCode)) {
				crew.add(person);
			}
		}
		
		return crew;
	}

	/**
	 * Returns a list of all the pilots based at a specific airport AND qualified to fly a specific aircraft type
	 * @param typeCode the type of plane to find pilots for
	 * @param airportCode the three-letter airport code of the airport to check for
	 * @return a list of all the pilots based at a specific airport AND qualified to fly a specific aircraft type
	 */
	@Override
	public List<Pilot> findPilotsByHomeBaseAndTypeRating(String typeCode, String airportCode) {
		List<Pilot> crew = new ArrayList<>();
		
		for(Pilot person : pilots) {
			if(person.getHomeBase().equals(airportCode) && person.getTypeRatings().contains(typeCode)) {
				crew.add(person);
			}
		}
		
		return crew;
	}

	/**
	 * Returns a list of all the pilots currently loaded who are qualified to fly the specified type of plane
	 * @param typeCode the type of plane to find pilots for
	 * @return a list of all the pilots currently loaded who are qualified to fly the specified type of plane
	 */
	@Override
	public List<Pilot> findPilotsByTypeRating(String typeCode) {
		List<Pilot> crew = new ArrayList<>();
		
		for(Pilot person : pilots) {
			if(person.getTypeRatings().contains(typeCode)) {
				crew.add(person);
			}
		}
		
		return crew;
	}

	/**
	 * Returns a list of all the cabin crew currently loaded
	 * @return a list of all the cabin crew currently loaded
	 */
	@Override
	public List<CabinCrew> getAllCabinCrew() {

		List<CabinCrew> returnCrew = cabinCrew;
		
		return returnCrew;
	}

	/**
	 * Returns a list of all the crew, regardless of type
	 * @return a list of all the crew, regardless of type
	 */
	@Override
	public List<Crew> getAllCrew() {
		
		List<Crew> allCrew = new ArrayList<>();
		
		allCrew.addAll(pilots);
		allCrew.addAll(cabinCrew);
		
		/*for (Crew person : allCrew) {
			System.out.println(person.getForename());
		}*/		
		return allCrew;
	}

	/**
	 * Returns a list of all the pilots currently loaded
	 * @return a list of all the pilots currently loaded
	 */
	@Override
	public List<Pilot> getAllPilots() {
		
		List<Pilot> returnPilots = pilots;
		
		return returnPilots;
	}

	@Override
	public int getNumberOfCabinCrew() {
		
		return cabinCrew.size();
	}

	/**
	 * Returns the number of pilots currently loaded
	 * @return the number of pilots currently loaded
	 */
	@Override
	public int getNumberOfPilots() {

		return pilots.size();
	}

	/**
	 * Unloads all of the crew currently loaded, ready to start again if needed
	 */
	@Override
	public void reset() {
		pilots = new ArrayList<>();
		cabinCrew = new ArrayList<>();
	}

}
