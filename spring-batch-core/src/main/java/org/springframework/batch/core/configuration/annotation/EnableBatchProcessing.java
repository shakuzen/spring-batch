/*
 * Copyright 2012-2022 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.sql.DataSource;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Import;

/**
 * <p>
 * Enable Spring Batch features and provide a base configuration for setting up batch jobs
 * in an &#064;Configuration class, roughly equivalent to using the {@code <batch:*>} XML
 * namespace.
 * </p>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableBatchProcessing
 * &#064;Import(DataSourceConfiguration.class)
 * public class AppConfig {
 *
 * 	&#064;Autowired
 * 	private JobBuilderFactory jobs;
 *
 * 	&#064;Bean
 * 	public Job job() {
 * 		return jobs.get(&quot;myJob&quot;).start(step1()).next(step2()).build();
 * 	}
 *
 * 	&#064;Bean
 *    protected Step step1() {
 *       ...
 *    }
 *
 * 	&#064;Bean
 *    protected Step step2() {
 *     ...
 *    }
 * }
 * </pre>
 *
 * You should provide a {@link DataSource} as a bean in the context or else implement
 * {@link BatchConfigurer} in the configuration class itself -- for example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableBatchProcessing
 * public class AppConfig extends DefaultBatchConfigurer {
 *
 *    &#064;Bean
 *    public Job job() {
 *       ...
 *    }
 *
 *    &#064;Override
 *    protected JobRepository createJobRepository() {
 *       ...
 *    }
 *
 *  ...
 *
 * }
 * </pre>
 *
 * If multiple {@link javax.sql.DataSource} instances are defined in the context, the
 * primary autowire candidate is used. Otherwise, an exception is thrown.
 *
 * Note that only one of your configuration classes needs to have the
 * <code>&#064;EnableBatchProcessing</code> annotation. Once you have an
 * <code>&#064;EnableBatchProcessing</code> class in your configuration, you have an
 * instance of {@link StepScope} and
 * {@link org.springframework.batch.core.scope.JobScope}, so your beans inside steps can
 * have <code>&#064;Scope("step")</code> and <code>&#064;Scope("job")</code> respectively.
 * You can also use <code>&#064;Autowired</code> to insert some useful beans into your
 * context:
 *
 * <ul>
 * <li>a {@link JobRepository} (bean name "jobRepository" of type
 * {@link org.springframework.batch.core.repository.support.SimpleJobRepository})</li>
 * <li>a {@link JobLauncher} (bean name "jobLauncher" of type
 * {@link TaskExecutorJobLauncher})</li>
 * <li>a {@link JobRegistry} (bean name "jobRegistry" of type
 * {@link org.springframework.batch.core.configuration.support.MapJobRegistry})</li>
 * <li>a {@link org.springframework.batch.core.explore.JobExplorer} (bean name
 * "jobExplorer" of type
 * {@link org.springframework.batch.core.explore.support.SimpleJobExplorer})</li>
 * <li>a {@link JobBuilderFactory} (bean name "jobBuilders") as a convenience to prevent
 * you from having to inject the job repository into every job, as in the earlier
 * examples</li>
 * <li>a {@link StepBuilderFactory} (bean name "stepBuilders") as a convenience to prevent
 * you from having to inject the job repository and transaction manager into every
 * step</li>
 * </ul>
 *
 * The transaction manager provided by this annotation is of type
 * {@link org.springframework.jdbc.support.JdbcTransactionManager} and is configured with
 * the {@link javax.sql.DataSource} provided within the context.
 *
 * To use a custom transaction manager, you should provide a custom
 * {@link BatchConfigurer} -- for example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableBatchProcessing
 * public class AppConfig extends DefaultBatchConfigurer {
 *
 *    &#064;Bean
 *    public Job job() {
 *       ...
 *    }
 *
 *    &#064;Override
 *    public PlatformTransactionManager getTransactionManager() {
 *       return new MyTransactionManager();
 *    }
 *
 *  ...
 *
 * }
 * </pre>
 *
 * If the configuration is specified as <code>modular=true</code>, the context also
 * contains an {@link AutomaticJobRegistrar}. The job registrar is useful for modularizing
 * your configuration if there are multiple jobs. It works by creating separate child
 * application contexts to contain job configurations and register those jobs. The jobs
 * can then create steps and other dependent components without needing to worry about
 * bean definition name clashes. Beans of type {@link ApplicationContextFactory} are
 * automatically registered with the job registrar. Example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableBatchProcessing(modular=true)
 * public class AppConfig {
 *
 *    &#064;Bean
 *    public ApplicationContextFactory someJobs() {
 *       return new GenericApplicationContextFactory(SomeJobConfiguration.class);
 *    }
 *
 *    &#064;Bean
 *    public ApplicationContextFactory moreJobs() {
 *       return new GenericApplicationContextFactory(MoreJobConfiguration.class);
 *    }
 *
 *  ...
 *
 * }
 * </pre>
 *
 * Note that a modular parent context, in general, should <em>not</em> itself contain
 * &#64;Bean definitions for job, especially if a {@link BatchConfigurer} is provided,
 * because cyclic configuration dependencies are likely to develop.
 *
 * <p>
 * For reference, compare the first example shown earlier to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * {@code
 * <batch>
 *     <job-repository />
 *     <job id="myJob">
 *       <step id="step1" .../>
 *       <step id="step2" .../>
 *     </job>
 *     <beans:bean id="transactionManager" .../>
 *     <beans:bean id="jobLauncher" class=
"org.springframework.batch.core.launch.support.TaskExecutorJobLauncher">
 *       <beans:property name="jobRepository" ref="jobRepository" />
 *     </beans:bean>
 * </batch>
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(BatchConfigurationSelector.class)
public @interface EnableBatchProcessing {

	/**
	 * Indicate whether the configuration is going to be modularized into multiple
	 * application contexts. If true, you should not create any &#64;Bean Job definitions
	 * in this context but, rather, supply them in separate (child) contexts through an
	 * {@link ApplicationContextFactory}.
	 * @return boolean indicating whether the configuration is going to be modularized
	 * into multiple application contexts. Defaults to {@code false}.
	 */
	boolean modular() default false;

}
