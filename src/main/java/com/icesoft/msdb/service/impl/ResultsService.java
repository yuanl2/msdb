package com.icesoft.msdb.service.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icesoft.msdb.domain.Driver;
import com.icesoft.msdb.domain.DriverEventPoints;
import com.icesoft.msdb.domain.EventEntryResult;
import com.icesoft.msdb.domain.EventSession;
import com.icesoft.msdb.domain.PointsSystem;
import com.icesoft.msdb.domain.TeamEventPoints;
import com.icesoft.msdb.repository.DriverEventPointsRepository;
import com.icesoft.msdb.repository.EventEntryResultRepository;
import com.icesoft.msdb.repository.EventSessionRepository;
import com.icesoft.msdb.repository.TeamEventPointsRepository;
import com.icesoft.msdb.repository.impl.JDBCRepositoryImpl;
import com.icesoft.msdb.service.StatisticsService;
import com.icesoft.msdb.service.dto.DriverPointsDTO;
import com.icesoft.msdb.service.dto.TeamPointsDTO;

@Service
@Transactional
public class ResultsService {
	private final Logger log = LoggerFactory.getLogger(ResultsService.class);
	
	@Autowired private EventSessionRepository sessionRepo;
	@Autowired private EventEntryResultRepository resultsRepo;
	@Autowired private DriverEventPointsRepository driverPointsRepo;
	@Autowired private TeamEventPointsRepository teamPointsRepo;
	@Autowired private JDBCRepositoryImpl viewsRepo;
	
	@Autowired private StatisticsService statsService;
	
	@Autowired private CacheHandler cacheHandler;

	public void processSessionResults(Long sessionId) {
		//TODO: Improve points system and series definition to handle other classifications (manufacturers, for instance)
		EventSession session = sessionRepo.findOne(sessionId);
		Map<Long, DriverEventPoints> drivers = new HashMap<>();
		Map<Long, TeamEventPoints> teams = new HashMap<>();
		PointsSystem ps = session.getPointsSystem();
		
		if (ps == null) {
			log.error("Skipping session {}-{} as it does not award points", session.getEventEdition().getLongEventName(), session.getName());
		} else {
			driverPointsRepo.deleteSessionPoints(sessionId);
			teamPointsRepo.deleteSessionPoints(sessionId);

			List<EventEntryResult> results = resultsRepo.findBySessionIdAndSessionEventEditionIdOrderByFinalPositionAscLapsCompletedDesc(session.getId(), session.getEventEdition().getId());
			int[] points = ps.disclosePoints();
			for(int i = 0; i < results.size() && i < points.length; i++) {
				//TODO: Handle shared drives (half points)
				EventEntryResult result = results.get(i);
				if (result.getFinalPosition() < 800) {
					if (session.getEventEdition().getSeriesEdition().getTeamsStandings()) {
						TeamEventPoints tep = teams.get(result.getEntry().getTeam().getId());
						if (tep == null) {
							tep = new TeamEventPoints();
							tep.setSession(session);
							tep.setTeam(result.getEntry().getTeam());
						}
						tep.addPoints((float)points[result.getFinalPosition() - 1]);
						log.debug(String.format("Team %s: %s points for position %s", result.getEntry().getTeam().getName(), (float)points[result.getFinalPosition() - 1], result.getFinalPosition()));
						teams.put(result.getEntry().getTeam().getId(), tep);
					}
					
					if (session.getEventEdition().getSeriesEdition().getManufacturersStandings()) {
						//TODO
					}
					
					for(Driver d : result.getEntry().getDrivers()) {
						log.debug(result.getFinalPosition() + "-" + d.getFullName() + ": " + points[result.getFinalPosition() - 1]);
						DriverEventPoints dep = 
								Optional.ofNullable(drivers.get(d.getId()))
									.orElse(new DriverEventPoints());
						dep.setDriver(d);
						dep.setSession(session);
						dep.addPoints((float)points[result.getFinalPosition() - 1] * session.getPsMultiplier());
						log.debug(String.format("Driver %s: %s(x%s) points for position %s", 
								d.getFullName(), (float)points[result.getFinalPosition() - 1], session.getPsMultiplier(), result.getFinalPosition()));
						drivers.put(d.getId(), dep);
						if (result.getStartingPosition() != null &&  result.getStartingPosition() == 1) {
							if (ps.getPointsPole() != 0 && session.getPsMultiplier().equals(new Float(1.0f))) {
								dep.addPoints(ps.getPointsPole().floatValue());
								log.debug(String.format("Driver %s: %s points for pole", d.getFullName(), ps.getPointsPole()));
							}
						}
					}
				}
			}
			if (ps.getPointsFastLap() != 0) {
				List<EventEntryResult> fastestLapOrder = results.parallelStream().sorted(
						(r1, r2)->Long.compare(
									r1.getBestLapTime() == null ? Long.MAX_VALUE : r1.getBestLapTime(), 
									r2.getBestLapTime() == null ? Long.MAX_VALUE : r2.getBestLapTime()))
						.collect(Collectors.toList());
				boolean lapPct = false;
				boolean topXPositions = true; int topX = 10;
				EventEntryResult fastestEntry;
				if (topXPositions) {
					fastestLapOrder = fastestLapOrder.parallelStream().filter(eer -> eer.getFinalPosition() <= topX).collect(Collectors.toList());
				}
				fastestEntry = fastestLapOrder.get(0);
				if (fastestEntry.getBestLapTime() != null) {
					for(Driver d : fastestEntry.getEntry().getDrivers()) {
						DriverEventPoints dep = 
								Optional.ofNullable(drivers.get(d.getId()))
									.orElse(new DriverEventPoints());
						dep.addPoints(ps.getPointsFastLap().floatValue());
						log.debug(String.format("Driver %s: %s points for fastest lap", d.getFullName(), ps.getPointsFastLap()));
						drivers.put(d.getId(), dep);
					}
				} else {
					log.warn("No fastest lap recorded... skipping");
				}
			}
			if (ps.getPointsLeadLap() != 0) {
				List<EventEntryResult> ledLaps = results.parallelStream()
						.filter(r -> r.getLapsLed() > 0)
						.sorted((r1, r2) -> Integer.compare(r2.getLapsLed(), r1.getLapsLed())).collect(Collectors.toList());
				int maxLedLaps = 0;
				for(EventEntryResult r : ledLaps) {
					boolean addPointsMostLeadLaps = false;
					if ((r.getLapsLed() > maxLedLaps && maxLedLaps == 0) || r.getLapsLed() == maxLedLaps) {
						maxLedLaps = r.getLapsLed();
						addPointsMostLeadLaps = true;
					}
					for(Driver d : r.getEntry().getDrivers()) {
						DriverEventPoints dep = 
								Optional.ofNullable(drivers.get(d.getId()))
									.orElse(new DriverEventPoints());

						dep.addPoints(ps.getPointsLeadLap().floatValue());
						log.debug(String.format("Driver %s: %s points for leading %s laps", d.getFullName(), ps.getPointsLeadLap(), r.getLapsLed()));
						if (addPointsMostLeadLaps) {
							dep.addPoints(ps.getPointsMostLeadLaps().floatValue());
							log.debug(String.format("Driver %s: %s points for most led laps", d.getFullName(), ps.getPointsMostLeadLaps()));
						}
						drivers.put(d.getId(), dep);
					}
				}
			}
			
			drivers.entrySet().parallelStream().forEach(entry -> driverPointsRepo.save(entry.getValue()));
			teams.entrySet().parallelStream().forEach(entry -> teamPointsRepo.save(entry.getValue()));
			
			cacheHandler.resetDriversStandingsCache(session.getSeriesId());
		}
		log.info("Processed result for {}-{}. Updating statistics...", session.getEventEdition().getLongEventName(), session.getName());
		statsService.buildEventStatistics(session.getEventEdition());
		log.info("Statistics updated");
	}
	
	public List<DriverPointsDTO> getDriversStandings(Long seriesId) {
		List<DriverPointsDTO> standings = viewsRepo.getDriversStandings(seriesId);
		Map<Long, List<Object[]>> positions = viewsRepo.getDriversResultsInSeries(seriesId);
		
		Comparator<DriverPointsDTO> c = (o1, o2) -> {
			if (o1.getPoints().floatValue() != o2.getPoints().floatValue()) {
				return o1.getPoints().compareTo(o2.getPoints());
			} else {
				List<Object[]> positionsD1 = positions.get(o1.getDriverId());
				List<Object[]> positionsD2 = positions.get(o2.getDriverId());
				
				return sortPositions(positionsD1, positionsD2);
			}
		};
		
		standings.sort(c.reversed());
		return standings;
	}
	
	public List<TeamPointsDTO> getTeamsStandings(Long seriesId) {
		List<TeamPointsDTO> standings = viewsRepo.getTeamsStandings(seriesId);
		Map<Long, List<Object[]>> positions = viewsRepo.getTeamsResultsInSeries(seriesId);
		
		Comparator<TeamPointsDTO> c = (o1, o2) -> {
			if (o1.getPoints().floatValue() != o2.getPoints().floatValue()) {
				return o1.getPoints().compareTo(o2.getPoints());
			} else {
				List<Object[]> positionsT1 = positions.get(o1.getTeamId());
				List<Object[]> positionsT2 = positions.get(o2.getTeamId());
				
				return sortPositions(positionsT1, positionsT2);
			}
		};
		
		standings.sort(c.reversed());
		return standings;
	}

	private int sortPositions(List<Object[]> positionsD1, List<Object[]> positionsD2) {
		Comparator<Object[]> pc = (pc1, pc2) -> ((Integer)pc1[0]).compareTo((Integer)pc2[0]);
		if (positionsD1 != null) positionsD1.sort(pc);
		if (positionsD2 != null) positionsD2.sort(pc);
		
		if (positionsD1 == null || positionsD1.isEmpty()) {
			if (positionsD2 == null || positionsD2.isEmpty()) {
				return 0;
			} else {
				return -1;
			}
		} else {
			if (positionsD2 == null || positionsD2.isEmpty()) {
				return 1;
			} else {
				for(int i = 0; i < positionsD1.size(); i++) {
					if (i + 1 > positionsD2.size()) {
						return 1;
					}
					Integer pD1, tD1, pD2, tD2;
					pD1 = (Integer)positionsD1.get(i)[0];
					tD1 = (Integer)positionsD1.get(i)[1];
					pD2 = (Integer)positionsD2.get(i)[0];
					tD2 = (Integer)positionsD2.get(i)[1];
					if (pD1.intValue() > pD2.intValue()) {
						return -1;
					} else if (pD1.intValue() < pD2.intValue()) {
						return 1;
					} else {
						if (tD1.intValue() > tD2.intValue()) {
							return 1;
						} else if (tD1.intValue() < tD2.intValue()) {
							return -1;
						}
					}
				}
				if (positionsD2.size() > positionsD1.size()) {
					return -1;
				}
				return 0;
			}
		}
	}
}
