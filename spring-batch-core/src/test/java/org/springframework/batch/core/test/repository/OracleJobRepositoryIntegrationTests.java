/*
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.test.repository;

import javax.sql.DataSource;

import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Official Docker images for Oracle are not publicly available. Oracle support is tested
 * semi-manually for the moment: 1. Build a docker image for oracle/database:11.2.0.2-xe:
 * https://github.com/oracle/docker-images/tree/main/OracleDatabase/SingleInstance#running-oracle-database-11gr2-express-edition-in-a-container
 * 2. Run the test `testJobExecution`
 *
 * @author Mahmoud Ben Hassine
 */
@Testcontainers
@SpringJUnitConfig
@Disabled("Official Docker images for Oracle are not publicly available")
class OracleJobRepositoryIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName ORACLE_IMAGE = DockerImageName.parse("oracle/database:11.2.0.2-xe");

	@Container
	public static OracleContainer oracle = new OracleContainer(ORACLE_IMAGE);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@BeforeEach
	void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-oracle.sql"));
		databasePopulator.execute(this.dataSource);
	}

	@Test
	void testJobExecution() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		// when
		JobExecution jobExecution = this.jobLauncher.run(this.job, jobParameters);

		// then
		assertNotNull(jobExecution);
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Configuration
	@EnableBatchProcessing
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			OracleDataSource oracleDataSource = new OracleDataSource();
			oracleDataSource.setUser(oracle.getUsername());
			oracleDataSource.setPassword(oracle.getPassword());
			oracleDataSource.setDatabaseName(oracle.getDatabaseName());
			oracleDataSource.setServerName(oracle.getHost());
			oracleDataSource.setPortNumber(oracle.getOraclePort());
			return oracleDataSource;
		}

		@Bean
		public Job job(JobBuilderFactory jobs, StepBuilderFactory steps) {
			return jobs.get("job")
					.start(steps.get("step").tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED).build())
					.build();
		}

	}

}
