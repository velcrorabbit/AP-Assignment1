package solution;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;

import baseclasses.DataLoadingException;
import baseclasses.IPassengerNumbersDAO;

/**
 * The PassengerNumbersDAO is responsible for loading an SQLite database
 * containing forecasts of passenger numbers for flights on dates
 */
public class PassengerNumbersDAO implements IPassengerNumbersDAO {
	
	Connection connection = null;
	
	int numberOfEntries;
	
	/**
	 * Returns the number of passenger number entries in the cache
	 * @return the number of passenger number entries in the cache
	 */
	@Override
	public int getNumberOfEntries(){

		try {
			
			if(connection != null) {
				Statement countStatment = connection.createStatement();
				ResultSet countResult = countStatment.executeQuery(
						"SELECT COUNT(Date) AS total FROM PassengerNumbers;");
				numberOfEntries = countResult.getInt("total");
			} else {
				numberOfEntries = 0;
			}
			
		} catch (SQLException | NullPointerException se) {
			se.printStackTrace();
		}
		
		return numberOfEntries;
	}

	/**
	 * Returns the predicted number of passengers for a given flight on a given date, or -1 if no data available
	 * @param flightNumber The flight number of the flight to check for
	 * @param date the date of the flight to check for
	 * @return the predicted number of passengers, or -1 if no data available
	 */
	@Override
	public int getPassengerNumbersFor(int flightNumber, LocalDate date)  {
		int passengerNumber=0;

		try {
			
			if (connection != null) {
			
				PreparedStatement numbersStatement = connection.prepareStatement("SELECT * FROM PassengerNumbers WHERE FlightNumber=? AND Date=?;");
				numbersStatement.setInt(1, flightNumber);
				numbersStatement.setObject(2, date.toString());
	
				
				ResultSet numbersResult = numbersStatement.executeQuery();
				while(numbersResult.next()) {
					passengerNumber = numbersResult.getInt("Passengers");
				}
				
				if (passengerNumber == 0) {
					passengerNumber = -1;
				}
			} else {
				passengerNumber = -1;
			}
			
		} catch (SQLException | NullPointerException se) {
			se.printStackTrace();
		}
		
		return passengerNumber;
	}
	

	/**
	 * Loads the passenger numbers data from the specified SQLite database into a cache for future calls to getPassengerNumbersFor()
	 * Multiple calls to this method are additive, but flight numbers/dates previously cached will be overwritten
	 * The cache can be reset by calling reset() 
	 * @param p The path of the SQLite database to load data from
	 * @throws DataLoadingException If there is a problem loading from the database
	 */
	@Override
	public void loadPassengerNumbersData(Path path) throws DataLoadingException {
		
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + path);
		} catch (SQLException | NullPointerException e) {
			e.printStackTrace();
			throw new DataLoadingException();
		}

	}

	/**
	 * Removes all data from the DAO, ready to start again if needed
	 */
	@Override
	public void reset() {
		
		numberOfEntries = 0;
		
		try {
			loadPassengerNumbersData(null);
			
		} catch (DataLoadingException | NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		connection = null;

	}


}
