package com.badminton.service;

import com.badminton.config.cache.AppCache;
import com.badminton.entity.AppUser;
import com.badminton.entity.AppUserManagerLink;
import com.badminton.entity.Player;
import com.badminton.entity.Role;
import com.badminton.enums.RoleName;
import com.badminton.model.CacheObject;
import com.badminton.repository.AppUserManagerLinkRepository;
import com.badminton.repository.AppUserRepository;
import com.badminton.repository.RoleRepository;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.requestmodel.ResetUserRequest;
import com.badminton.response.AppUserResponse;
import com.badminton.response.UserDeleteBatchResponse;
import com.badminton.response.result.Result;
import com.badminton.util.RsaKeyService;
import com.badminton.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    // Rank order enforces the management hierarchy: an actor may only manage
    // accounts whose highest role is strictly below the actor's own rank.
    private static final int ROOT_RANK = 3;
    private static final int ADMIN_RANK = 2;
    private static final int COORDINATOR_RANK = 1;
    private static final String USER_NOT_PRESENT = "tài khoản không còn tồn tại";

    private final PasswordEncoder encoder;
    private final UserRepository playerRepository;
    private final AppUserRepository appUserRepository;
    private final AppUserManagerLinkRepository appUserManagerLinkRepository;
    private final RoleRepository roleRepository;
    private final AppCache appCache;
    private final RsaKeyService rsaKeyService;
    private final JwtService jwtService;

    public UserServiceImpl(PasswordEncoder encoder, UserRepository playerRepository,
            AppUserRepository appUserRepository, AppUserManagerLinkRepository appUserManagerLinkRepository,
            RoleRepository roleRepository,
            AppCache appCache, RsaKeyService rsaKeyService, JwtService jwtService) {
        this.encoder = encoder;
        this.playerRepository = playerRepository;
        this.appUserRepository = appUserRepository;
        this.appUserManagerLinkRepository = appUserManagerLinkRepository;
        this.roleRepository = roleRepository;
        this.appCache = appCache;
        this.rsaKeyService = rsaKeyService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public boolean saveAdminUser(RegisterUserDTO userDTO) {
        if (userDTO.getUserId() != null || appUserRepository.existsByUsername(userDTO.getUserName()))
            return false;
        try {
            RoleName roleName = userDTO.getRole() == null || userDTO.getRole().isBlank()
                    ? RoleName.ADMINISTRATOR
                    : RoleName.valueOf(userDTO.getRole().trim().toUpperCase());
            // The Root role is provisioned outside of user management: it can never
            // be granted here, not even by another root account.
            if (roleName == RoleName.PLAYER || roleName == RoleName.ROOT
                    || !canManageRank(rankOf(roleName)))
                return false;
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role is not configured"));
            String rawPassword = rsaKeyService.decrypt(userDTO.getPassword());
            AppUser created = appUserRepository
                    .save(new AppUser(userDTO.getUserName(), encoder.encode(rawPassword), role));
            linkToCurrentManager(created, roleName);
            return created.getUserId() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean savePlayer(RegisterUserDTO userDTO) {
        return playerRepository.save(new Player(userDTO.getUserName(), null)).getPlayerId() != 0;
    }

    @Override
    public ResponseEntity<String> generateResetPassToken(String userName) {
        AppUser user = appUserRepository.findActiveByUsername(userName).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("User is not present.");
        if (!canManageRank(rankOf(user)))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Insufficient permission to manage this user.");
        String token = encoder.encode(userName + UUID.randomUUID() + System.currentTimeMillis());
        appCache.put(token, new CacheObject(userName, System.currentTimeMillis()));
        return ResponseEntity.ok(token);
    }

    @Override
    public ResponseEntity<String> resetPassword(ResetUserRequest request) {
        try {
            request.setNewPass(rsaKeyService.decrypt(request.getNewPass()));
            request.setRepeatNewPass(rsaKeyService.decrypt(request.getRepeatNewPass()));
            validateRequest(request);
            AppUser user = appUserRepository.findActiveByUsername(request.getUserName())
                    .orElseThrow(() -> new IllegalArgumentException("User is not present."));
            Assert.isTrue(canManageRank(rankOf(user)), "Insufficient permission to manage this user.");
            user.setPassword(encoder.encode(request.getNewPass()));
            appUserRepository.save(user);
            appCache.remove(request.getResetToken());
            return ResponseEntity.ok("Reset password for user " + request.getUserName() + " successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private void validateRequest(ResetUserRequest request) {
        Assert.isTrue(appCache.contains(request.getResetToken()), "Token is invalid");
        CacheObject cacheObject = (CacheObject) appCache.get(request.getResetToken());
        Assert.notNull(cacheObject, "Token is invalid");
        Assert.isTrue(System.currentTimeMillis() - cacheObject.getExpiryTime() < 1000 * 60 * 3, "Token is expired.");
        Assert.isTrue(request.getUserName().equals(cacheObject.getValue()), "User is not present.");
        Assert.isTrue(request.getNewPass().equals(request.getRepeatNewPass()), "Two passwords must match.");
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<AppUserResponse>> listUsers() {
        // Backend-side scoping based on the current logged-in user:
        // ROOT sees every account; ADMINISTRATOR only sees the coordinators
        // linked to them; COORDINATOR has no access to this feature.
        int actorRank = actorRank();
        if (actorRank < ADMIN_RANK)
            return forbidden();
        List<AppUser> users = actorRank == ROOT_RANK
                ? appUserRepository.findAllActive()
                : linkedCoordinatorsOf(actor());
        Map<Long, AppUser> managersByManagedId = appUserManagerLinkRepository.findAll().stream()
                .collect(Collectors.toMap(link -> link.getManagedUser().getUserId(), AppUserManagerLink::getManager));
        Result<List<AppUserResponse>> result = new Result<>();
        result.setSuccess(true);
        result.setData(users.stream().map(user -> {
            AppUserResponse response = AppUserResponse.from(user);
            AppUser manager = managersByManagedId.get(user.getUserId());
            if (manager != null) {
                response.setManagerUserId(manager.getUserId());
                response.setManagerUsername(manager.getUsername());
            }
            return response;
        }).toList());
        return result;
    }

    /**
     * Coordinators currently linked to the given manager via the manager-link
     * table.
     */
    private List<AppUser> linkedCoordinatorsOf(AppUser manager) {
        if (manager == null)
            return List.of();
        return appUserManagerLinkRepository.findByManager_UserId(manager.getUserId()).stream()
                .map(AppUserManagerLink::getManagedUser)
                .filter(user -> rankOf(user) == COORDINATOR_RANK)
                .filter(user -> !user.isDeleted())
                .toList();
    }

    @Override
    @Transactional
    public Result<AppUserResponse> updateUserRole(Long userId, String roleName) {
        AppUser user = findManageableUser(userId);
        if (user == null)
            return forbidden();
        if (user.getUsername().equals(actorName()))
            return badRequest("Cannot change your own role.");
        RoleName targetRole;
        try {
            targetRole = RoleName.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return badRequest("Invalid role: " + roleName);
        }
        // Root is never assignable: it is reserved for the provisioned account.
        if (targetRole == RoleName.ROOT)
            return badRequest("The Root role cannot be assigned.");
        if (targetRole == RoleName.PLAYER || !canManageRank(rankOf(targetRole)))
            return forbidden();
        user.getRoles().clear();
        user.getRoles().add(roleRepository.findByRoleName(targetRole)
                .orElseThrow(() -> new IllegalStateException("Role is not configured")));
        syncManagerLink(user, targetRole);
        Result<AppUserResponse> result = new Result<>();
        result.setSuccess(true);
        result.setData(AppUserResponse.from(appUserRepository.save(user)));
        return result;
    }

    /**
     * Keeps the manager-link table in sync with a user's new role: the link is
     * dropped whenever the user leaves COORDINATOR, and set when an
     * ADMINISTRATOR promotes or demotes an account to COORDINATOR.
     */
    private void syncManagerLink(AppUser user, RoleName newRole) {
        AppUser manager = actor();
        if (newRole != RoleName.COORDINATOR || manager == null || actorRank() != ADMIN_RANK) {
            appUserManagerLinkRepository.deleteByManagedUser_UserId(user.getUserId());
            return;
        }
        linkManager(user, manager);
    }

    /** Links a newly created coordinator to the current administrator actor. */
    private void linkToCurrentManager(AppUser created, RoleName roleName) {
        if (roleName != RoleName.COORDINATOR || actorRank() != ADMIN_RANK)
            return;
        AppUser manager = actor();
        if (manager == null)
            return;
        linkManager(created, manager);
    }

    /**
     * Points the unique manager link of the given user at the given manager,
     * creating the row only when it is missing. The row is updated in place
     * rather than deleted and re-inserted: Hibernate flushes inserts before
     * deletes inside one transaction, so a delete followed by an insert trips
     * the uk_link_managed_user unique key.
     */
    private void linkManager(AppUser managedUser, AppUser manager) {
        AppUserManagerLink link = appUserManagerLinkRepository
                .findByManagedUser_UserId(managedUser.getUserId())
                .orElseGet(AppUserManagerLink::new);
        link.setManager(manager);
        link.setManagedUser(managedUser);
        appUserManagerLinkRepository.save(link);
    }

    @Override
    @Transactional
    public Result<AppUserResponse> updateUserStatus(Long userId, boolean active) {
        AppUser user = findManageableUser(userId);
        if (user == null)
            return forbidden();
        if (user.getUsername().equals(actorName()))
            return badRequest("Cannot deactivate your own account.");
        user.setActive(active);
        if (!active) {
            jwtService.revokeAllForUser(user);
        }
        Result<AppUserResponse> result = new Result<>();
        result.setSuccess(true);
        result.setData(AppUserResponse.from(appUserRepository.save(user)));
        return result;
    }

    /**
     * ROOT-only removal. The account is soft-deleted (is_deleted set) instead of
     * erased: every user query filters it out and it can no longer sign in, while
     * its row stays for the audit trail and the invoice/debt references. Root
     * accounts are provisioned outside the app and are never removable here.
     */
    @Override
    @Transactional
    public Result<AppUserResponse> deleteUser(Long userId) {
        if (actorRank() != ROOT_RANK)
            return forbidden();
        AppUser user = appUserRepository.findActiveById(userId).orElse(null);
        if (user == null)
            return badRequest(USER_NOT_PRESENT);
        String blocked = removalBlockedReason(user);
        if (blocked != null)
            return badRequest(blocked);
        Result<AppUserResponse> result = new Result<>();
        result.setSuccess(true);
        result.setData(AppUserResponse.from(markRemoved(user)));
        return result;
    }

    /**
     * ROOT-only bulk removal behind the multi-select in user management. Each
     * selected account is checked on its own: the removable ones are hidden,
     * the others are returned in {@code skipped}, so one blocked account never
     * cancels the rest of the selection.
     */
    @Override
    @Transactional
    public Result<UserDeleteBatchResponse> deleteUsers(List<Long> userIds) {
        if (actorRank() != ROOT_RANK)
            return forbidden();
        List<Long> ids = userIds == null
                ? List.of()
                : userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty())
            return badRequest("Chưa chọn tài khoản nào để xoá.");
        UserDeleteBatchResponse batch = new UserDeleteBatchResponse();
        batch.setRequested(ids.size());
        for (Long userId : ids) {
            AppUser user = appUserRepository.findActiveById(userId).orElse(null);
            if (user == null) {
                batch.getSkipped().add(userId + ": " + USER_NOT_PRESENT);
                continue;
            }
            String blocked = removalBlockedReason(user);
            if (blocked != null) {
                batch.getSkipped().add(user.getUsername() + ": " + blocked);
                continue;
            }
            batch.getRemoved().add(markRemoved(user).getUsername());
        }
        if (batch.getRemoved().isEmpty())
            return badRequest("Không xoá được tài khoản nào. " + String.join(" ", batch.getSkipped()));
        Result<UserDeleteBatchResponse> result = new Result<>();
        result.setSuccess(true);
        result.setData(batch);
        return result;
    }

    /**
     * Reason a selected account must stay, or null when it may be removed.
     * Root accounts are provisioned outside the app, and nobody removes the
     * account they are signed in with.
     */
    private String removalBlockedReason(AppUser user) {
        if (user.getUsername().equals(actorName()))
            return "không thể xoá tài khoản đang đăng nhập";
        if (rankOf(user) >= ROOT_RANK)
            return "tài khoản Root không thể xoá";
        return null;
    }

    /**
     * Hides one account: soft-delete marker plus deactivation (keeps it unusable
     * even if someone clears the flag by hand), manager links dropped in both
     * directions and every refresh token revoked.
     */
    private AppUser markRemoved(AppUser user) {
        user.setDeleted(true);
        user.setActive(false);
        Long userId = user.getUserId();
        // Manager links point at the removed row: drop both directions so no
        // coordinator listing keeps pointing at an account that is gone.
        appUserManagerLinkRepository.deleteByManagedUser_UserId(userId);
        appUserManagerLinkRepository.deleteByManager_UserId(userId);
        jwtService.revokeAllForUser(user);
        return appUserRepository.save(user);
    }

    /**
     * ROOT-only mapping between an administrator and one of their coordinators.
     * A null managerUserId clears the link; otherwise the coordinator is
     * (re)assigned to the given administrator.
     */
    @Override
    @Transactional
    public Result<AppUserResponse> assignManager(Long userId, Long managerUserId) {
        if (actorRank() != ROOT_RANK)
            return forbidden();
        AppUser user = appUserRepository.findActiveById(userId).orElse(null);
        if (user == null)
            return badRequest("User is not present.");
        if (rankOf(user) != COORDINATOR_RANK)
            return badRequest("Only coordinators can be assigned to an administrator.");
        if (managerUserId == null) {
            appUserManagerLinkRepository.deleteByManagedUser_UserId(userId);
            return okUser(user, null);
        }
        AppUser manager = appUserRepository.findActiveById(managerUserId).orElse(null);
        if (manager == null || !hasRole(manager, RoleName.ADMINISTRATOR))
            return badRequest("Manager must be an administrator.");
        linkManager(user, manager);
        return okUser(user, manager);
    }

    private Result<AppUserResponse> okUser(AppUser user, AppUser manager) {
        AppUserResponse response = AppUserResponse.from(user);
        if (manager != null) {
            response.setManagerUserId(manager.getUserId());
            response.setManagerUsername(manager.getUsername());
        }
        Result<AppUserResponse> result = new Result<>();
        result.setSuccess(true);
        result.setData(response);
        return result;
    }

    private boolean hasRole(AppUser user, RoleName role) {
        return user.getRoles().stream().anyMatch(r -> r.getRoleName() == role);
    }

    @Override
    public boolean checkUserExistByName(String username) {
        return appUserRepository.existsByUsername(username);
    }

    /**
     * Returns the target user only when the current actor is allowed to manage
     * it: ROOT manages everyone, ADMINISTRATOR manages only COORDINATOR-level
     * accounts. PLAYER accounts are never manageable targets.
     */
    private AppUser findManageableUser(Long userId) {
        AppUser user = appUserRepository.findActiveById(userId).orElse(null);
        if (user == null || !canManageRank(rankOf(user)))
            return null;
        return user;
    }

    private boolean canManageRank(int targetRank) {
        int actorRank = actorRank();
        return actorRank == ROOT_RANK || targetRank < actorRank;
    }

    private int rankOf(AppUser user) {
        return user.getRoles().stream().mapToInt(role -> rankOf(role.getRoleName())).max().orElse(0);
    }

    private int rankOf(RoleName role) {
        return switch (role) {
            case ROOT -> ROOT_RANK;
            case ADMINISTRATOR -> ADMIN_RANK;
            case COORDINATOR -> COORDINATOR_RANK;
            case PLAYER -> 0;
        };
    }

    private int actorRank() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            return 0;
        if (hasAuthority(authentication, "ROLE_ROOT"))
            return ROOT_RANK;
        if (hasAuthority(authentication, "ROLE_ADMINISTRATOR"))
            return ADMIN_RANK;
        if (hasAuthority(authentication, "ROLE_COORDINATOR"))
            return COORDINATOR_RANK;
        return 0;
    }

    private String actorName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "" : authentication.getName();
    }

    /** The logged-in user entity, resolved from the security context. */
    private AppUser actor() {
        String name = actorName();
        return name.isBlank() ? null : appUserRepository.findActiveByUsername(name).orElse(null);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }

    private <T> Result<T> forbidden() {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.FORBIDDEN.value());
        result.setErrorMessage("Insufficient permission to manage this user.");
        return result;
    }

    private <T> Result<T> badRequest(String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.BAD_REQUEST.value());
        result.setErrorMessage(message);
        return result;
    }
}
