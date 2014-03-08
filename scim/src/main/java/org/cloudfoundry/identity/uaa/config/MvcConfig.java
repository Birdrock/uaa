package org.cloudfoundry.identity.uaa.config;

import org.cloudfoundry.identity.uaa.web.ForwardAwareInternalResourceViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

import java.util.Arrays;
import java.util.HashMap;

@Configuration
public class MvcConfig extends WebMvcConfigurationSupport {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("/");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    public ForwardAwareInternalResourceViewResolver forwardAwareInternalResourceViewResolver() {
        ForwardAwareInternalResourceViewResolver viewResolver = new ForwardAwareInternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/jsp/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    public BeanNameViewResolver beanNameViewResolver() {
        return new BeanNameViewResolver();
    }

    public MappingJacksonJsonView mappingJacksonJsonView() {
        MappingJacksonJsonView mappingJacksonJsonView = new MappingJacksonJsonView();
        mappingJacksonJsonView.setExtractValueFromSingleKeyModel(true);
        return mappingJacksonJsonView;
    }

    @Bean
    public ContentNegotiatingViewResolver viewResolver() {
        ContentNegotiatingViewResolver viewResolver = new ContentNegotiatingViewResolver();
        HashMap<String, String> mediaTypes = new HashMap<String, String>();
        mediaTypes.put("json", "application/json");
        viewResolver.setMediaTypes(mediaTypes);
        viewResolver.setViewResolvers(Arrays.<ViewResolver>asList(forwardAwareInternalResourceViewResolver(), beanNameViewResolver()));
        viewResolver.setDefaultViews(Arrays.<View>asList(mappingJacksonJsonView()));
        return viewResolver;
    }
}
