package com.icesoft.msdb.domain.stats;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="tyreProviderStatistics")
public class TyreProviderStatistics extends ParticipantStatisticsSnapshot {

	@Id
	private String tyreProvId;
	@Version
	private Long version;
	
	public TyreProviderStatistics(String tyreProvId) {
		this.tyreProvId = tyreProvId;
	}
}
