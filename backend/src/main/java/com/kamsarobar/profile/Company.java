package com.kamsarobar.profile;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class Company extends NamedTag {

    protected Company() {
    }

    public Company(String name) {
        super(name);
    }
}
