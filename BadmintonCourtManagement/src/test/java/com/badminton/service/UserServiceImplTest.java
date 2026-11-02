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
        when(appUserRepository.findActiveByUsername("admin1"))
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
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(user(1L, "coord1", RoleName.COORDINATOR)));
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(user(2L, "admin2", RoleName.ADMINISTRATOR)));
        when(appUserRepository.findActiveById(3L)).thenReturn(Optional.of(user(3L, "rootuser", RoleName.ROOT)));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.updateUserStatus(1L, false).isSuccess());
        assertFalse(service.updateUserStatus(2L, false).isSuccess());
        assertFalse(service.updateUserStatus(3L, false).isSuccess());
    }

    @Test
    void administratorCannotAssignAdministratorRole() {
        authenticate("admin1", "ROLE_ADMINISTRATOR");
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(user(1L, "coord1", RoleName.COORDINATOR)));

        assertFalse(service.updateUserRole(1L, "ADMINISTRATOR").isSuccess());
        assertFalse(service.updateUserRole(1L, "ROOT").isSuccess());
    }

    @Test
    void rootCanManageEveryAccountButNotItself() {
        authenticate("rootuser", "ROLE_ROOT");
        AppUser self = user(1L, "rootuser", RoleName.ROOT);
        AppUser admin = user(2L, "admin1", RoleName.ADMINISTRATOR);
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(self));
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(admin));
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
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(user(1L, "coord1", RoleName.COORDINATOR)));
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(user(2L, "admin1", RoleName.ADMINISTRATOR)));

        assertTrue(service.assignManager(1L, 2L).isSuccess());
        verify(linkRepository, times(1)).save(any());

        SecurityContextHolder.clearContext();
        authenticate("admin1", "ROLE_ADMINISTRATOR");
        assertFalse(service.assignManager(1L, 2L).isSuccess());
    }

    @Test
    void assignManagerRejectsNonCoordinatorAndNonAdministrator() {
        authenticate("rootuser", "ROLE_ROOT");
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(user(1L, "coord1", RoleName.COORDINATOR)));
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(user(2L, "coord2", RoleName.COORDINATOR)));
        when(appUserRepository.findActiveById(3L)).thenReturn(Optional.of(user(3L, "admin1", RoleName.ADMINISTRATOR)));

        // Manager is not an administrator (both are coordinators).
        assertFalse(service.assignManager(1L, 2L).isSuccess());
        // Target is not a coordinator.
        assertFalse(service.assignManager(3L, 1L).isSuccess());
        verify(linkRepository, never()).save(any());
    }

    @Test
    void rootRoleIsNeverGrantable() {
        authenticate("rootuser", "ROLE_ROOT");
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(user(2L, "admin1", RoleName.ADMINISTRATOR)));

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
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(coord));
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(admin));
        when(linkRepository.findByManagedUser_UserId(1L)).thenReturn(Optional.of(existing));

        assertTrue(service.assignManager(1L, 2L).isSuccess());
        assertSame(admin, existing.getManager());
        assertSame(coord, existing.getManagedUser());
        // Delete + insert in one transaction trips uk_link_managed_user, because
        // Hibernate flushes inserts before deletes.
        verify(linkRepository, never()).deleteByManagedUser_UserId(any());
        verify(linkRepository, times(1)).save(existing);
    }

    @Test
    void onlyRootCanRemoveUserAndRootAccountsStay() {
        AppUser coord = user(1L, "coord1", RoleName.COORDINATOR);
        AppUser otherRoot = user(3L, "root2", RoleName.ROOT);
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(coord));
        when(appUserRepository.findActiveById(3L)).thenReturn(Optional.of(otherRoot));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // An administrator can never remove an account.
        authenticate("admin1", "ROLE_ADMINISTRATOR");
        assertFalse(service.deleteUser(1L).isSuccess());

        // Root accounts are provisioned outside the app and stay untouchable.
        SecurityContextHolder.clearContext();
        authenticate("rootuser", "ROLE_ROOT");
        assertFalse(service.deleteUser(3L).isSuccess());
        assertFalse(otherRoot.isDeleted());

        assertTrue(service.deleteUser(1L).isSuccess());
        assertTrue(coord.isDeleted());
        assertFalse(coord.isActive());
        verify(appUserRepository, times(1)).save(any());
        verify(jwtService, times(1)).revokeAllForUser(coord);
        verify(linkRepository).deleteByManagedUser_UserId(1L);
        verify(linkRepository).deleteByManager_UserId(1L);
    }

    @Test
    void rootCannotRemoveItself() {
        authenticate("rootuser", "ROLE_ROOT");
        AppUser self = user(1L, "rootuser", RoleName.ROOT);
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(self));

        assertFalse(service.deleteUser(1L).isSuccess());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void removedUsersNeverReachTheList() {
        authenticate("rootuser", "ROLE_ROOT");
        when(appUserRepository.findAllActive())
                .thenReturn(List.of(user(2L, "admin1", RoleName.ADMINISTRATOR)));
        when(linkRepository.findAll()).thenReturn(List.of());

        var result = service.listUsers();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        // The listing must query the active-only projection, never findAll().
        verify(appUserRepository, never()).findAll();
    }

    @Test
    void rootRemovesSeveralSelectedAccountsInOneRequest() {
        authenticate("rootuser", "ROLE_ROOT");
        AppUser coord1 = user(1L, "coord1", RoleName.COORDINATOR);
        AppUser coord2 = user(2L, "coord2", RoleName.COORDINATOR);
        AppUser admin = user(3L, "admin1", RoleName.ADMINISTRATOR);
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(coord1));
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(coord2));
        when(appUserRepository.findActiveById(3L)).thenReturn(Optional.of(admin));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.deleteUsers(List.of(1L, 2L, 3L));

        assertTrue(result.isSuccess());
        assertEquals(3, result.getData().getRequested());
        assertEquals(List.of("coord1", "coord2", "admin1"), result.getData().getRemoved());
        assertTrue(result.getData().getSkipped().isEmpty());
        assertTrue(coord1.isDeleted() && coord2.isDeleted() && admin.isDeleted());
        verify(jwtService, times(3)).revokeAllForUser(any());
        verify(linkRepository, times(3)).deleteByManagedUser_UserId(any());
        verify(linkRepository, times(3)).deleteByManager_UserId(any());
    }

    @Test
    void bulkRemovalSkipsTheActorRootAccountsAndMissingRows() {
        authenticate("rootuser", "ROLE_ROOT");
        AppUser self = user(1L, "rootuser", RoleName.ROOT);
        AppUser otherRoot = user(2L, "root2", RoleName.ROOT);
        AppUser coord = user(3L, "coord1", RoleName.COORDINATOR);
        when(appUserRepository.findActiveById(1L)).thenReturn(Optional.of(self));
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(otherRoot));
        when(appUserRepository.findActiveById(3L)).thenReturn(Optional.of(coord));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.deleteUsers(List.of(1L, 2L, 3L, 99L));

        // One blocked account must not cancel the rest of the selection.
        assertTrue(result.isSuccess());
        assertEquals(4, result.getData().getRequested());
        assertEquals(List.of("coord1"), result.getData().getRemoved());
        assertEquals(3, result.getData().getSkipped().size());
        assertFalse(self.isDeleted());
        assertFalse(otherRoot.isDeleted());
        verify(appUserRepository, times(1)).save(any());
    }

    @Test
    void bulkRemovalFailsWhenNothingInTheSelectionCanBeRemoved() {
        authenticate("rootuser", "ROLE_ROOT");
        when(appUserRepository.findActiveById(2L)).thenReturn(Optional.of(user(2L, "root2", RoleName.ROOT)));

        var result = service.deleteUsers(List.of(2L));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("root2"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void administratorCannotRemoveUsersInBulk() {
        authenticate("admin1", "ROLE_ADMINISTRATOR");

        assertFalse(service.deleteUsers(List.of(1L, 2L)).isSuccess());
        verify(appUserRepository, never()).findActiveById(any());
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
