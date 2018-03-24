package com.icesoft.msdb.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.icesoft.msdb.domain.enums.SessionType;
import com.icesoft.msdb.service.dto.DriverPointsDTO;

public class PointsRaceByRace {
	
	private class DriverPoints {
		private final String driverName;
		private final Float points;
		
		DriverPoints(String name, Float points) {
			this.driverName = name;
			this.points = points;
		}
		
		public String getDriverName() {
			return driverName;
		}
		
		public Float getPoints() {
			return points;
		}
	}
	
	private class SessionPoints {
		private final String sessionName;
		private final List<DriverPoints> points;
		
		public SessionPoints(String sessionName) {
			this.sessionName = sessionName;
			this.points = new ArrayList<>();
		}
		
		public String getSessionName() {
			return sessionName;
		}
		public List<DriverPoints> getPoints() {
			return points;
		}
		
		public void addDriverPoints(DriverPoints driverPoints) {
			this.points.add(driverPoints);
		}
	}

	private class EventPoints {
		private final String eventName;
		private final List<SessionPoints> points;
		
		public EventPoints(String eventName) {
			super();
			this.eventName = eventName;
			this.points = new ArrayList<>();
		}

		public String getEventName() {
			return eventName;
		}
		public List<SessionPoints> getPoints() {
			return points;
		}
		public void addSessionPoints(SessionPoints sp) {
			points.add(sp);
		}
	}
	
	private final List<EventPoints> seriesPoints = new ArrayList<>();
	
	public void addDriverPoints(String eventName, String sessionName, SessionType sessionType, String driverName, Float points) {
		DriverPoints driverPoints = new DriverPoints(driverName, points);
		SessionPoints sessionPoints;
		EventPoints eventPoints;
		
		Optional<EventPoints> optEventPoints = seriesPoints.stream().filter(p -> p.getEventName().equals(eventName)).findFirst();
		if (!optEventPoints.isPresent()) {
			eventPoints = new EventPoints(eventName);
			seriesPoints.add(eventPoints);
		} else {
			eventPoints = optEventPoints.get();
		}
		
		Optional<SessionPoints> optSessionPoints = eventPoints.getPoints().stream().filter(s -> s.getSessionName().equals(sessionName)).findFirst();
		if (!optSessionPoints.isPresent()) {
			sessionPoints = new SessionPoints(sessionName);
			eventPoints.addSessionPoints(sessionPoints);
		} else {
			sessionPoints = optSessionPoints.get();
		}
		
		sessionPoints.addDriverPoints(driverPoints);
	}
	
	public Object[][] getResultsMatrix(List<DriverPointsDTO> standings) {
		int numRaces = 0; 
		for(EventPoints ep: seriesPoints) {
			numRaces += ep.getPoints().size();
		}
		Object[][] result = new Object[standings.size() + 1][numRaces + 2];
		
		int indexRaces = 1;
		for(EventPoints ep: seriesPoints) {
			String raceName = ep.getEventName(); 
			if (ep.getPoints().size() > 1) {
				for(SessionPoints sp: ep.getPoints()) {
					result[0][indexRaces] = raceName + "-" + sp.getSessionName();
					indexRaces++;
				}
			} else {
				result[0][indexRaces] = raceName;
				indexRaces++;
			}
		}
		
		int indexDrivers = 1;
		for(DriverPointsDTO driver: standings) {
			result[indexDrivers][0] = driver.getDriverName();
			result[indexDrivers][numRaces + 1] = driver.getPoints();
			
			int eventIndex = 1;
			for(EventPoints ep: seriesPoints) {
				for(SessionPoints sp: ep.getPoints()) {
					Optional<DriverPoints> driverPoints = sp.getPoints().stream().filter(dp -> dp.getDriverName().equals(driver.getDriverName())).findFirst();
					if (driverPoints.isPresent()) {
						Float points = driverPoints.get().getPoints();
						if (points % 1.0 != 0) {
							result[indexDrivers][eventIndex] = String.format("%s", points);
						} else {
							result[indexDrivers][eventIndex] = String.format("%.0f", points);
						}
					} else {
						result[indexDrivers][eventIndex] = "0";
					}
					eventIndex++;
				}
			}
			
			indexDrivers++;
		}
		
		result[0][0] = "";
		result[0][numRaces + 1] = "Total";
		
		return result;
	}
}
