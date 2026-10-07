package com.kamsarobar.profile;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "skills")
public class Skill extends NamedTag {

    protected Skill() {
    }

    public Skill(String name) {
        super(name);
    }
}
