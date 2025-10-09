package ar.edu.itba.paw.webapp.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import javax.sql.DataSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.validation.Validator;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@EnableWebMvc
@ComponentScan({
        "ar.edu.itba.paw.webapp.controller",
        "ar.edu.itba.paw.service",
        "ar.edu.itba.paw.persistence",
        "ar.edu.itba.paw.service.config",
        "ar.edu.itba.paw.webapp.config"

})
@Configuration
@PropertySource(value = {
        "classpath:application.properties",
})
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Value("classpath:/db/schema.sql")
    private Resource schemaSql;

    @Value("${app.db.host}")
    private String dbHost;
    @Value("${app.db.port}")
    private String dbPort;
    @Value("${app.db.name}")
    private String dbName;

    @Value("${app.db.user}")
    private String dbUser;
    @Value("${app.db.pass}")
    private String dbPass;

    @Value("${app.schema.init:false}")
    private boolean runSchema;

    @Bean
    public InternalResourceViewResolver viewResolver() {
        var r = new InternalResourceViewResolver();
        r.setPrefix("/WEB-INF/jsp/");
        r.setSuffix(".jsp");
        return r;
    }

    @Bean
    public MessageSource messageSource() {
        final ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        messageSource.addBasenames("classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.displayName());
        messageSource.setCacheSeconds((int) TimeUnit.MINUTES.toSeconds(5));

        return messageSource;
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
        ds.setUrl(String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName));
        ds.setUsername(dbUser);
        ds.setPassword(dbPass);
        return ds;
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource ds) {
        var dsi = new DataSourceInitializer();
        dsi.setDataSource(ds);

        var pop = new ResourceDatabasePopulator();
        pop.addScript(schemaSql);
        pop.setContinueOnError(true);
        pop.setIgnoreFailedDrops(true);
        dsi.setDatabasePopulator(pop);
        dsi.setEnabled(runSchema);
        return dsi;
    }

    @Bean
    public org.springframework.web.multipart.commons.CommonsMultipartResolver multipartResolver() {
        var r = new org.springframework.web.multipart.commons.CommonsMultipartResolver();
        r.setMaxUploadSizePerFile(10 * 1024 * 1024); // 10MB
        return r;
    }

    private DatabasePopulator databasePopulator() {
        var pop = new ResourceDatabasePopulator();
        pop.addScript(schemaSql);
        return pop;
    }

}
