package ar.edu.itba.paw.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

@EnableWebMvc
@ComponentScan({"ar.edu.itba.paw.webapp.controller"})
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("classpath:/schema.sql")
    private Resource schemaSql;


    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver r = new InternalResourceViewResolver();
        r.setPrefix("/WEB-INF/jsp/");
        r.setSuffix(".jsp");
        return r;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(final DataSource ds) {
        final DataSourceInitializer dsi = new DataSourceInitializer();
        dsi.setDataSource(ds);
        dsi.setDatabasePopulator(databasePopulator());
        return dsi;
    }

    @Bean
    public DataSource dataSource() {
        String port = "5433";
        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        ds.setDriverClass(org.postgresql.Driver.class);
        ds.setUrl("jdbc:postgresql://localhost:" + port + "/paw?sslmode=disable");
        ds.setUsername("postgres");
        ds.setPassword("postgres");
        return ds;
    }


    private DatabasePopulator databasePopulator() {
        ResourceDatabasePopulator dbp = new ResourceDatabasePopulator();
        dbp.setContinueOnError(true);   // tolera drops/creates idempotentes
        dbp.setIgnoreFailedDrops(true);
        dbp.addScript(schemaSql);       // primero schema
        return dbp;
    }
}
