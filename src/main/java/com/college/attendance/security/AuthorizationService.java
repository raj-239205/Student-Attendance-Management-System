package com.college.attendance.security;

import com.college.attendance.model.Session;
import com.college.attendance.model.User;
import com.college.attendance.ui.navigation.NavigationItem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Service enforcing non-bypassable role-based authorization for all views and features.
 *
 * <p>
 * Ensures that UI hiding is backed by authoritative service checks:
 * <ul>
 *   <li>ADMIN: Full system administration (Dashboard, Students, Subjects, Attendance, Defaulters, Reports).</li>
 *   <li>FACULTY: Academic operations (Dashboard, Students, Attendance, Defaulters, Reports).</li>
 *   <li>STUDENT: Personal records only (Dashboard, My Attendance, My Profile). Denied all administrative views.</li>
 * </ul>
 * </p>
 */
public class AuthorizationService {

    private static final Set<NavigationItem> ADMIN_PERMISSIONS = Collections.unmodifiableSet(
            EnumSet.of(
                    NavigationItem.DASHBOARD,
                    NavigationItem.STUDENTS,
                    NavigationItem.SUBJECTS,
                    NavigationItem.ATTENDANCE,
                    NavigationItem.DEFAULTERS,
                    NavigationItem.REPORTS
            )
    );

    private static final Set<NavigationItem> FACULTY_PERMISSIONS = Collections.unmodifiableSet(
            EnumSet.of(
                    NavigationItem.DASHBOARD,
                    NavigationItem.STUDENTS,
                    NavigationItem.ATTENDANCE,
                    NavigationItem.DEFAULTERS,
                    NavigationItem.REPORTS
            )
    );

    private static final Set<NavigationItem> STUDENT_PERMISSIONS = Collections.unmodifiableSet(
            EnumSet.of(
                    NavigationItem.DASHBOARD,
                    NavigationItem.MY_ATTENDANCE,
                    NavigationItem.MY_PROFILE
            )
    );

    /**
     * Verifies whether a given role is authorized to access a navigation destination.
     *
     * @param role the user role
     * @param item the target navigation item
     * @return {@code true} if access is permitted, {@code false} otherwise
     */
    public boolean canAccess(User.Role role, NavigationItem item) {
        if (role == null || item == null) {
            return false;
        }

        return switch (role) {
            case ADMIN -> ADMIN_PERMISSIONS.contains(item);
            case FACULTY -> FACULTY_PERMISSIONS.contains(item);
            case STUDENT -> STUDENT_PERMISSIONS.contains(item);
        };
    }

    /**
     * Verifies whether the current active session is authorized to access an item.
     *
     * @param item the target navigation item
     * @return {@code true} if authorized, {@code false} if unauthenticated or unauthorized
     */
    public boolean canAccess(NavigationItem item) {
        Session session = Session.getCurrent();
        if (session == null) {
            return false;
        }
        return canAccess(session.getRole(), item);
    }

    /**
     * Returns the ordered list of authorized navigation items for a given role.
     *
     * @param role the user role
     * @return list of allowed navigation items
     */
    public List<NavigationItem> getAllowedNavigationItems(User.Role role) {
        if (role == null) {
            return Collections.emptyList();
        }

        return switch (role) {
            case ADMIN -> List.of(
                    NavigationItem.DASHBOARD,
                    NavigationItem.STUDENTS,
                    NavigationItem.SUBJECTS,
                    NavigationItem.ATTENDANCE,
                    NavigationItem.DEFAULTERS,
                    NavigationItem.REPORTS
            );
            case FACULTY -> List.of(
                    NavigationItem.DASHBOARD,
                    NavigationItem.STUDENTS,
                    NavigationItem.ATTENDANCE,
                    NavigationItem.DEFAULTERS,
                    NavigationItem.REPORTS
            );
            case STUDENT -> List.of(
                    NavigationItem.DASHBOARD,
                    NavigationItem.MY_ATTENDANCE,
                    NavigationItem.MY_PROFILE
            );
        };
    }
}
