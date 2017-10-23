package com.icesoft.msdb.domain;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.icesoft.msdb.domain.enums.DurationType;
import com.icesoft.msdb.domain.enums.SessionType;

/**
 * A EventSession.
 */
@Entity
@Table(name = "event_session")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EventSession extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @NotNull
    @Size(max = 10)
    @Column(name = "shortname", length = 10, nullable = false)
    private String shortname;

    @NotNull
    @Column(name = "session_start_time", nullable = false)
    private ZonedDateTime sessionStartTime;

    @NotNull
    @Column(name = "duration", nullable = false)
    private Integer duration;
    
    @Column(name = "max_duration")
    private Integer maxDuration;
    
    @Column(name= "duration_type")
    private Integer durationType;
    
    @Column(name= "session_type")
    @Enumerated(EnumType.ORDINAL)
    private SessionType sessionType;
    
    @Column(name= "additional_lap")
    private Boolean additionalLap;
    
    @ManyToOne
    private EventEdition eventEdition;
    
    @ManyToOne
    private PointsSystem pointsSystem;
    
    @Column(name="ps_multiplier")
    private Float psMultiplier = 1.0f;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public EventSession name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortname() {
        return shortname;
    }

    public EventSession shortname(String shortname) {
        this.shortname = shortname;
        return this;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public ZonedDateTime getSessionStartTime() {
        return sessionStartTime;
    }

    public EventSession sessionStartTime(ZonedDateTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
        return this;
    }

    public void setSessionStartTime(ZonedDateTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public EventSession duration(Integer duration) {
        this.duration = duration;
        return this;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Integer getMaxDuration() {
		return maxDuration;
	}
    
    public EventSession maxDuration(Integer maxDuration) {
    	this.maxDuration = maxDuration;
    	return this;
    }

	public void setMaxDuration(Integer maxDuration) {
		this.maxDuration = maxDuration;
	}

	public Integer getDurationType() {
		return durationType;
	}
    
    public EventSession durationType(Integer durationType) {
    	this.durationType = durationType;
    	return this;
    }

	public void setDurationType(Integer durationType) {
		this.durationType = durationType;
	}

	public SessionType getSessionType() {
		return sessionType;
	}

	public void setSessionType(SessionType sessionType) {
		this.sessionType = sessionType;
	}
	
	public EventSession sessionType(SessionType sessionType) {
		this.sessionType = sessionType;
		return this;
	}
	
	public int getSessionTypeValue() {
		return sessionType.getValue() - 1;
	}
	
	public boolean isRace() {
		return sessionType.equals(SessionType.RACE);
	}

	public Boolean getAdditionalLap() {
		return additionalLap;
	}

	public void setAdditionalLap(Boolean additionalLap) {
		this.additionalLap = additionalLap;
	}
	
	public EventSession additionalLap(Boolean additionalLap) {
		this.additionalLap = additionalLap;
		return this;
	}

	public void setEventEdition(EventEdition eventEdition) {
    	this.eventEdition = eventEdition;
    }
    
    public EventSession eventEdition(EventEdition eventEdition) {
    	this.eventEdition = eventEdition;
    	return this;
    }
    
    public EventEdition getEventEdition() {
    	return eventEdition;
    }

	public PointsSystem getPointsSystem() {
		return pointsSystem;
	}

	public void setPointsSystem(PointsSystem pointsSystem) {
		this.pointsSystem = pointsSystem;
	}
	
	public Float getPsMultiplier() {
		return psMultiplier;
	}

	public void setPsMultiplier(Float psMultiplier) {
		this.psMultiplier = psMultiplier;
	}

	public Long getSeriesId() {
		if (eventEdition!= null && eventEdition.getSeriesEdition() != null) {
			return eventEdition.getSeriesEdition().getId();
		}
		return null;
	}
	
	public String getSeriesName() {
		if (eventEdition!= null && eventEdition.getSeriesEdition() != null) {
			return eventEdition.getSeriesEdition().getSeries().getName();
		}
		return null;
	}
	
	public boolean isFinished(ZonedDateTime now) {
		return getSessionEndTime().isBefore(ZonedDateTime.now(ZoneId.of("UTC")));
	}
	
	public ZonedDateTime getSessionEndTime() {
		DurationType durationType = DurationType.valueOf(getDurationType());
		TemporalUnit temp = durationType.equals(DurationType.MINUTES) ? ChronoUnit.MINUTES :
				durationType.equals(DurationType.HOURS) ? ChronoUnit.HOURS : null;
		
		if (getSessionType().equals(SessionType.RACE) && getMaxDuration() != null) {
			return getSessionStartTime().plus(getMaxDuration(), ChronoUnit.HOURS);
		}
		if (temp == null) return getSessionStartTime().plus(2, ChronoUnit.HOURS);
		else return getSessionStartTime().plus(getDuration(), temp);
		
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventSession eventSession = (EventSession) o;
        if (eventSession.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), eventSession.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "EventSession{" +
        		"id=" + getId() +
                ", name='" + getName() + "'" +
                ", shortname='" + getShortname() + "'" +
                ", sessionStartTime='" + getSessionStartTime() + "'" +
                ", duration='" + getDuration() + "'" +
            ", durationType='" + getDurationType() + "'" +
            ", sessionType='" + getSessionType() + "'" +
            ", additionalLap='" + getAdditionalLap() + "'" +
            '}';
    }
}
