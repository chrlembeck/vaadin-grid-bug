package de.chrlembeck.vaadintest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.chrlembeck.vaadintest.TestEntity;

@Repository
public interface TestRepository extends JpaRepository<TestEntity, Integer> {

}
