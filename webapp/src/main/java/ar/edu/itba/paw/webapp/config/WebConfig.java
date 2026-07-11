package ar.edu.itba.paw.webapp.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.validation.Validator;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@EnableTransactionManagement
@EnableAsync
@ComponentScan({
        "ar.edu.itba.paw.service",
        "ar.edu.itba.paw.persistence",
        "ar.edu.itba.paw.service.config",
        "ar.edu.itba.paw.webapp.config"

})
@Configuration
@PropertySource(value = {
        "classpath:application.properties",
})
public class WebConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Value("classpath:/db/schema.sql")
    private Resource schemaSql;

    @Value("${local.db.host}")
    private String dbHost;
    @Value("${local.db.port}")
    private String dbPort;
    @Value("${local.db.name}")
    private String dbName;

    @Value("${local.db.user}")
    private String dbUser;
    @Value("${local.db.pass}")
    private String dbPass;

    @Value("${local.schema.init:false}")
    private boolean runSchema;

    @Bean
    public MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        messageSource.addBasenames("classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.displayName());
        messageSource.setCacheSeconds((int) TimeUnit.MINUTES.toSeconds(5));
        messageSource.setDefaultLocale(new Locale("en"));
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    @Bean
    public DataSource dataSource() {
        var ds = new org.springframework.jdbc.datasource.SimpleDriverDataSource();
        ds.setDriverClass(org.postgresql.Driver.class);
        ds.setUrl(String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName));
        ds.setUsername(dbUser);
        ds.setPassword(dbPass);
        return ds;
    }

//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManager() {
//        final LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
//        emf.setPackagesToScan("ar.edu.itba.paw.webapp.model");
//        emf.setDataSource(dataSource());
//
//        final JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
//        emf.setJpaVendorAdapter(vendorAdapter);
//
//        final Properties properties = new Properties();
//        properties.setProperty("hibernate.hbm2ddl.auto", "update");
//        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL92Dialect");
//
//        //Esto es para debuggear hibernate, pero nunca en prod
//        //properties.setProperty("hibernate.show_sql", "true");
//        //properties.setProperty("format_sql", "true");
//
//        emf.setJpaProperties(properties);
//        return emf;
//    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPackagesToScan("ar.edu.itba.paw.model");
        emf.setDataSource(dataSource());
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        var props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "none");
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL92Dialect");
        emf.setJpaProperties(props);
        return emf;
    }


    @Bean
    public JpaTransactionManager transactionManager(final EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource ds) {
        var dsi = new DataSourceInitializer();
        dsi.setDataSource(ds);

        var pop = new ResourceDatabasePopulator();
        pop.addScript(schemaSql);
        pop.setIgnoreFailedDrops(true);
        dsi.setDatabasePopulator(pop);
        dsi.setEnabled(runSchema);
        return dsi;
    }


    @Bean
    public Validator getValidator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setValidationMessageSource(messageSource());
        return factoryBean;
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        var ex = new  ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("Async-");
        ex.initialize();
        return ex;
    }
}
