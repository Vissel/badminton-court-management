package com.badminton.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badminton.entity.Service;

@Repository
public interface ServiceRepositoty extends JpaRepository<Service, Integer> {
	Optional<Service> findBySerName(String serName);

	List<Service> findAllByIsActive(boolean isActive);
}
