# Security Specification: Aura Profile, Role Onboarding & Admin Activity Portal

## 1. Data Invariants
1. **Strict Multi-User Isolation (`/users/{userId}`)**:
   - Every `/users/{userId}` document can only be read (`get`, `list`) by the authenticated owner (`request.auth.uid == userId`) or a verified administrator (`isAdmin()`).
   - Default actor for `"fokoufdr@gmail.com"` is `ADMIN`; all other users default to `VISITOR` and can select their e-learning role (`VISITOR`, `STUDENT`, `TEACHER`, `SCHOOL`, `PARENT`) after account creation.
   - `userId` and `createdAt` are strictly immutable on update.
2. **Activity Audit Stream (`/activities/{activityId}`)**:
   - Any authenticated user can `create` an activity document ONLY for their own `userId` (`request.resource.data.userId == request.auth.uid`) with initial `reviewStatus == 'RECORDED'`.
   - Regular users can `get` and `list` ONLY their own activity documents (`resource.data.userId == request.auth.uid`).
   - Verified administrators (`isAdmin()`, exclusively `"fokoufdr@gmail.com"` by default) can `get`, `list`, `update` (`reviewStatus`, `severity`, `details`, `updatedAt`), and `delete` all activity documents across the application for the Admin Portal Dashboard.
3. **Administrator Registry (`/admins/{adminId}`)**:
   - Only the verified administrator (`fokoufdr@gmail.com` with `request.auth.token.email_verified == true` or an existing `/admins/{uid}` document) is recognized as `isAdmin()`.
   - Only an administrator can create, update, list, or delete documents in `/admins/{adminId}`.
