package ar.edu.itba.paw.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

@EnableWebMvc
@ComponentScan({
        "ar.edu.itba.paw.webapp.controller",
        "ar.edu.itba.paw.service",
        "ar.edu.itba.paw.persistence",
        "ar.edu.itba.paw.service.config"
})
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("classpath:schema.sql")
    private Resource schemaSql;

    @Bean
    public InternalResourceViewResolver viewResolver() {
        var r = new InternalResourceViewResolver();
        r.setPrefix("/WEB-INF/jsp/");
        r.setSuffix(".jsp");
        return r;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    @Bean
    public DataSource dataSource() {
        var ds = new org.springframework.jdbc.datasource.SimpleDriverDataSource();
        ds.setDriverClass(org.postgresql.Driver.class);
        ds.setUrl("jdbc:postgresql://localhost:5433/paw");
        ds.setUsername("postgres");
        ds.setPassword("postgres");
        return ds;
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(final DataSource ds) {
        var dsi = new DataSourceInitializer();
        dsi.setDataSource(ds);

        boolean hasSchema = (schemaSql != null && schemaSql.exists());
        dsi.setEnabled(hasSchema);

        if (hasSchema) {
            dsi.setDatabasePopulator(databasePopulator());
        }
        return dsi;
    }

    private DatabasePopulator databasePopulator() {
        var pop = new ResourceDatabasePopulator();
        pop.addScript(schemaSql);
        pop.setContinueOnError(true);
        pop.setIgnoreFailedDrops(true);
        return pop;
    }
}
