package com.abc.wechat.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/*
    mybatis + db2
 */
@Configuration
@MapperScan(basePackages = "com.abc.wechat.dao.db2", sqlSessionTemplateRef = "sqlSessionTemplate2")
public class DataSource2Config {

    static final String MAPPER_LOCATION = "classpath:mapper/db2/*.xml";
    //dataSource: autowired
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari.db2")
    public DataSource  dataSource2(){
        DataSource dataSource = DataSourceBuilder.create().build();
        return dataSource;
    }

    //sqlSessionFactory: dataSource + mapper.xml
    @Bean
    public SqlSessionFactory sqlSessionFactory2(@Qualifier("dataSource2") DataSource dataSource) throws Exception {
        final SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION));
        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public DataSourceTransactionManager db2TransactionManager(@Qualifier("dataSource2") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }


    @Bean
    public SqlSessionTemplate sqlSessionTemplate2(@Qualifier("sqlSessionFactory2") SqlSessionFactory sqlSessionFactory){
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}