package com.kejelah.pencarikeje;

import com.kejelah.pencarikeje.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PencarikejeApplicationIT extends AbstractIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	/** Definition of Done: Flyway migrations run cleanly against an empty database. */
	@Test
	void migrationsCreateEverySchemaObject() {
		var tables = jdbcTemplate.queryForList(
				"select table_name from information_schema.tables where table_schema = 'public'", String.class);

		assertThat(tables).contains("users", "statuses", "applications", "application_progress");
	}

	@Test
	void statusesAreSeededInPresentationOrder() {
		var codes = jdbcTemplate.queryForList(
				"select code from statuses order by display_order", String.class);

		assertThat(codes).containsExactly(
				"APPLIED", "RECRUITER_VIEWED", "HR_SCREENING", "INTERVIEW", "TECHNICAL_INTERVIEW",
				"FINAL_INTERVIEW", "OFFER", "ACCEPTED", "REJECTED", "RECONSIDERED", "WITHDRAWN");
	}

	/**
	 * The seed inserts explicit ids, which does not advance the BIGSERIAL
	 * sequence. V5 fixes that with setval; without it this insert would collide
	 * on the primary key.
	 */
	@Test
	void statusSequenceIsUsableAfterSeeding() {
		jdbcTemplate.update(
				"insert into statuses (code, name, display_order) values ('TEST_ONLY', 'Test Only', 999)");

		Long id = jdbcTemplate.queryForObject(
				"select id from statuses where code = 'TEST_ONLY'", Long.class);

		assertThat(id).isGreaterThan(11L);
		jdbcTemplate.update("delete from statuses where code = 'TEST_ONLY'");
	}
}
