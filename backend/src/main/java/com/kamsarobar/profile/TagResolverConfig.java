package com.kamsarobar.profile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TagResolverConfig {

    @Bean
    public TagResolver<Company> companyResolver(CompanyRepository repository) {
        return new TagResolver<>(repository, Company::new);
    }

    @Bean
    public TagResolver<Skill> skillResolver(SkillRepository repository) {
        return new TagResolver<>(repository, Skill::new);
    }
}
