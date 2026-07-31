## 2025-02-27 - IDOR in User Deletion
**Vulnerability:** Admin endpoints like `/api/users/admin/{id}` (DELETE) allowed an admin user to delete their own account, potentially locking themselves out of the system.
**Learning:** Security context validations must be enforced on destructive administrative operations to prevent self-deletion or self-modification.
**Prevention:** Always compare the target entity's ID against the current authenticated user's ID (`userSecurityContext.getAuthenticatedUser().id()`) in administrative resources before proceeding with the operation.
