package com.badminton.service;

import com.badminton.config.cache.AppCache;
import com.badminton.entity.AppUser;
import com.badminton.entity.AppUserManagerLink;
import com.badminton.entity.Role;
import com.badminton.enums.RoleName;
import com.badminton.repository.AppUserManagerLinkRepository;
import com.badminton.repository.AppUserRepository;
import com.badminton.repository.RoleRepository;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.util.RsaKeyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserRepository playerRepository = mock(UserRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final AppUserManagerLinkRepository linkRepository = mock(AppUserManagerLinkRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final AppCache appCache = new AppCache();
    private final RsaKeyService rsaKeyService = mock(RsaKeyService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final UserServiceImpl service = new UserServiceImpl(
            encoder, playerRepository, appUserRepository, linkRepository, roleRepository, appCache, rsaKeyService,
            jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administratorCanCreateCoordinatorButNotPeerAdministrator() {
        authenticate("admin1", "ROLE_ADMINISTRATOR");
        when(rsaKeyService.decrypt(any())).thenReturn("raw");
        when(appUserRepository.findByUsername("admin1"))
                .thenReturn(Optional.of(user(99L, "admin1", RoleName.ADMINISTRATOR)));
        when(roleRepository.findByRoleName(RoleName.COORDINATOR)).thenReturn(Optional.of(role(RoleName.COORDINATOR)));
        when(appUserRepository.save(any())).thenAnswer(inv -> {
            AppUser user = inv.getArgument(0);
            user.setUserId(1L);
            return user;
        });

        assertTrue(service.saveAdminUser(dto("coord1", "COORDINATOR")));
        assertFalse(service.saveAdminUser(dto("admin2", "ADMINISTRATOR")));
        assertFalse(service.saveAdminUser(dto("root2", "ROOT")));
        verify(appUserRepository, times(1)).save(any());
        verify(linkRepository, times(1)).save(any());
    }

    @Test
    void administratorCanDeactivateCoordinatorButNotAdministratorOrRoot() {
        authenticate("admin1", "ROLE_ADMINISTRATOR");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user(1L, "coord1", RoleName.COORDINATOR)));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user(2L, "admin2", RoleName.ADMINISTRATOR)));
        when(appUserRepository.findById(3L)).thenReturn(Optional.of(user(3L, "rootuser", RoleName.ROOT)));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.updateUserStatus(1L, false).isSuccess());
        assertFalse(service.updateUserStatus(2L, false).isSuccess());
        assertFalse(service.updateUserStatus(3L, false).isSuccess());
    }

    @Test
    void administratorCannotAssignAdministratorRole() {
        authenticate("admin1", "ROLE_ADMINISTRATOR");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user(1L, "coord1", RoleName.COORDINATOR)));

        assertFalse(service.updateUserRole(1L, "ADMINISTRATOR").isSuccess());
        assertFalse(service.updateUserRole(1L, "ROOT").isSuccess());
    }

    @Test
    void rootCanManageEveryAccountButNotItself() {
        authenticate("rootuser", "ROLE_ROOT");
        AppUser self = user(1L, "rootuser", RoleName.ROOT);
        AppUser admin = user(2L, "admin1", RoleName.ADMINISTRATOR);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(self));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByRoleName(RoleName.COORDINATOR)).thenReturn(Optional.of(role(RoleName.COORDINATOR)));

        assertFalse(service.updateUserStatus(1L, false).isSuccess());
        assertFalse(service.updateUserRole(1L, "COORDINATOR").isSuccess());
        assertTrue(service.updateUserStatus(2L, false).isSuccess());
        assertTrue(service.updateUserRole(2L, "COORDINATOR").isSuccess());
        assertTrue(admin.getRoles().stream().allMatch(r -> r.getRoleName() == RoleName.COORDINATOR));
    }

    @Test
    void onlyRootCanAssignManagerToCoordinator() {
        authenticate("rootuser", "ROLE_ROOT");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user(1L, "coord1", RoleName.COORDINATOR)));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user(2L, "admin1", RoleName.ADMINISTRATOR)));

        assertTrue(service.assignManager(1L, 2L).isSuccess());
        verify(linkRepository, times(1)).save(any());

        SecurityContextHolder.clearContext();
        authenticate("admin1", "ROLE_ADMINISTRATOR");
        assertFalse(service.assignManager(1L, 2L).isSuccess());
    }

    @Test
    void assignManagerRejectsNonCoordinatorAndNonAdministrator() {
        authenticate("rootuser", "ROLE_ROOT");
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user(1L, "coord1", RoleName.COORDINATOR)));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user(2L, "coord2", RoleName.COORDINATOR)));
        when(appUserRepository.findById(3L)).thenReturn(Optional.of(user(3L, "admin1", RoleName.ADMINISTRATOR)));

        // Manager is not an administrator (both are coordinators).
        assertFalse(service.assignManager(1L, 2L).isSuccess());
        // Target is not a coordinator.
        assertFalse(service.assignManager(3L, 1L).isSuccess());
        verify(linkRepository, never()).save(any());
    }

    @Test
    void rootRoleIsNeverGrantable() {
        authenticate("rootuser", "ROLE_ROOT");
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user(2L, "admin1", RoleName.ADMINISTRATOR)));

        // A root actor cannot create another root account...
        assertFalse(service.saveAdminUser(dto("root2", "ROOT")));

        // ...nor promote an existing account to Root (any casing).
        var result = service.updateUserRole(2L, "Root");
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Root role"));

        verify(roleRepository, never()).findByRoleName(RoleName.ROOT);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void assignManagerUpdatesExistingLinkInPlace() {
        authenticate("rootuser", "ROLE_ROOT");
        AppUser coord = user(1L, "coord1", RoleName.COORDINATOR);
        AppUser admin = user(2L, "admin1", RoleName.ADMINISTRATOR);
        AppUserManagerLink existing = new AppUserManagerLink();
        existing.setLinkId(7L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(coord));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(linkRepository.findByManagedUser_UserId(1L)).thenReturn(Optional.of(existing));

        assertTrue(service.assignManager(1L, 2L).isSuccess());
        assertSame(admin, existing.getManager());
        assertSame(coord, existing.getManagedUser());
        // Delete + insert in one transaction trips uk_link_managed_user, because
        // Hibernate flushes inserts before deletes.
        verify(linkRepository, never()).deleteByManagedUser_UserId(any());
        verify(linkRepository, times(1)).save(existing);
    }

    private void authenticate(String username, String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private RegisterUserDTO dto(String username, String role) {
        RegisterUserDTO dto = new RegisterUserDTO();
        dto.setUserName(username);
        dto.setPassword("encrypted");
        dto.setRole(role);
        return dto;
    }

    private Role role(RoleName roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        return role;
    }

    private AppUser user(Long id, String username, RoleName roleName) {
        AppUser user = new AppUser(username, "pw", role(roleName));
        user.setUserId(id);
        return user;
    }
}
