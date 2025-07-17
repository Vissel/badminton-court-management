package com.badminton.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badminton.entity.Service;

@Repository
public interface ServiceRepositoty extends JpaRepository<Service, Integer> {

}
