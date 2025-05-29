Alright, team! We've just wrapped up what I'm calling 'Sprint 4', and honestly, it was quite the journey, but we got some critical stuff done. This sprint was all about getting our user authentication and basic authorization solid, and despite a few bumps, we're in a really good place.
Key Achievements

    User Authentication & Authorization: We successfully implemented secure user login and logout using Spring Security. Users can now authenticate, and the system correctly identifies who's logged in.

    Account Ownership Filtering: A massive win is that users now only see their own accounts on the dashboard. This was a critical security fix, ensuring data isolation and preventing a customer from seeing other customers' bank accounts.

    Self-Service Account Creation: Customers can now create their own bank accounts, and these accounts are automatically linked to their user profile, removing the need to select an owner from a dropdown.

Challenges Faced

This sprint wasn't without its challenges, and we tackled some tricky ones:

    Initial Login Redirect Loop (ERR_TOO_MANY_REDIRECTS): We hit a classic Spring Security setup hurdle when trying to access /login. This was resolved by creating dedicated LoginController and login.html/register.html pages to handle the view rendering.

    Persistent Database Schema Mismatches: This was a recurring headache! We battled the 'Unknown column' error for updated_at and the 'Field password_hash doesn't have a default value' error. This required careful, manual database schema adjustments (dropping the rogue password_hash column, ensuring created_at and updated_at columns were correctly defined as NOT NULL with appropriate defaults). It really highlighted the importance of keeping our JPA entity definitions in sync with the actual database schema.

    "Ambiguous Mapping" in AccountController: We encountered a build error because of duplicate POST /accounts/create methods in the AccountController. This was quickly resolved by removing the redundant method, ensuring only one handler for that specific endpoint.

    Stubborn defaultValue() Compilation Error: The defaultValue() attribute on the @Column annotation in User.java caused a very persistent compilation error, even after extensive cache clearing and dependency updates. We implemented a practical workaround by removing the defaultValue attribute from the annotation and instead relying on the User constructor and @PrePersist callback to manually set the default ROLE_CUSTOMER. This achieves the same functional goal without blocking compilation.

    Thymeleaf sec:authorize Not Working: Even after adding the sec:authorize attributes to the HTML, the links for Admin/Employee panels were still visible to regular customers. This was due to a missing thymeleaf-extras-springsecurity6 dependency, which, once correctly added and recognized, allowed Thymeleaf to process the security-specific attributes and hide unauthorized links.

Looking Ahead

With these core features in place, we're well-positioned for the next phase. Our immediate focus will be on:

    Securing Individual Account Actions: Ensuring that when a user tries to view, edit, deposit, withdraw, or transfer funds for a specific account, the system verifies that the account actually belongs to them (or they have appropriate permissions). This will involve adding ownership checks in the AccountService methods.

    Implementing Full RBAC for Functionality: Building out the actual Admin and Employee panels, and using Spring Security's @PreAuthorize annotations to restrict access to specific controller and service methods based on roles (e.g., only admins can list all users, employees can view all accounts but not modify all user details).

Overall, a very productive sprint with a lot of learning and problem-solving! Great work, team!
