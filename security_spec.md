# JIMMY Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **UserProfile (`/users/{userId}`)**:
   - Can only be read, created, updated, or deleted by the authenticated owner (`request.auth.uid == userId`).
   - `userId` and `createdAt` are immutable across updates.
2. **CallRoom (`/call_rooms/{roomId}`)**:
   - Can only be created by an authenticated `hostId == request.auth.uid` in `'waiting'` status with `guestId == ''`.
   - Can only be joined (`'waiting' -> 'connected'`) by an authenticated user whose UID is NOT `hostId` (prevents self-matching at the database level).
   - Once `status == 'ended'`, the room is terminally locked against any further updates.
3. **ChatMessage (`/call_rooms/{roomId}/messages/{messageId}`)**:
   - Can only be read or listed by users present in the bounded 2-element `participants` array (`request.auth.uid in resource.data.participants`).
   - Can only be created if the parent `/call_rooms/{roomId}` exists, `senderId == request.auth.uid`, and `request.auth.uid in incoming().participants`.
   - Messages are append-only (`allow update, delete: if false`).
4. **ModerationAction (`/users/{userId}/moderation_actions/{actionId}`)**:
   - Strictly isolated to `request.auth.uid == userId`.
   - Users cannot report or block themselves (`targetUserId != request.auth.uid`).

## 2. Dirty Dozen Adversarial Payloads
1. Unauthenticated read on `/users/{userId}` -> Rejected.
2. Unauthenticated list on `/call_rooms` -> Rejected.
3. Cross-user read on `/users/{otherUserId}` -> Rejected.
4. Shadow update injecting `isAdmin: true` into `/users/{userId}` -> Rejected by `hasOnly`.
5. Mutating immutable `createdAt` or `userId` on `/users/{userId}` -> Rejected.
6. Host attempting to join their own `/call_rooms/{roomId}` as `guestId` -> Rejected (`hostId != request.auth.uid`).
7. Third-party user reading a `'connected'` `/call_rooms/{roomId}` they are not participating in -> Rejected.
8. Updating a `/call_rooms/{roomId}` after `status == 'ended'` -> Rejected by Terminal State Lock.
9. Creating a `/call_rooms/{roomId}/messages/{messageId}` for a non-existent room -> Rejected by `exists()`.
10. Spoofing `senderId` in `/call_rooms/{roomId}/messages/{messageId}` -> Rejected (`senderId == request.auth.uid`).
11. Reading messages in a room where caller is not in `participants` -> Rejected.
12. Submitting a 10KB string into `text` or `rtcOfferSdp` -> Rejected by `.size()` bounds.
