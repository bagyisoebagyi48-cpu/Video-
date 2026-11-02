const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or create profiles or rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("call_rooms").where("status", "==", "waiting").get());
});

test("Authenticated user can create and read their own UserProfile, but not another user's", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      countryCode: "MM",
      countryName: "Myanmar",
      targetCountryCode: "ALL",
      onlineStatus: "online",
      ageVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update with ghost field on UserProfile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      countryCode: "MM",
      countryName: "Myanmar",
      targetCountryCode: "ALL",
      onlineStatus: "online",
      ageVerified: true,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Matchmaking flow: Host creates waiting room, Guest joins, Self-join fails, Ended room is locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("call_rooms").doc("room_1").set({
      hostId: ALICE_UID,
      hostName: "Alice",
      hostCountryCode: "MM",
      hostCountryName: "Myanmar",
      targetCountryCode: "ALL",
      guestId: "",
      guestName: "",
      guestCountryCode: "",
      guestCountryName: "",
      status: "waiting",
      rtcOfferSdp: "v=0...",
      rtcAnswerSdp: "",
      hostFrameData: "",
      guestFrameData: "",
      hostAudioMuted: false,
      guestAudioMuted: false,
      hostCameraFacing: "front",
      guestCameraFacing: "front",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Host cannot join own room as guest
  await assertFails(
    aliceDb.collection("call_rooms").doc("room_1").update({
      guestId: ALICE_UID,
      guestName: "Alice",
      guestCountryCode: "MM",
      guestCountryName: "Myanmar",
      status: "connected",
      updatedAt: new Date(),
    })
  );

  // Bob can query waiting rooms
  await assertSucceeds(
    bobDb.collection("call_rooms").where("status", "==", "waiting").get()
  );

  // Bob joins waiting room
  await assertSucceeds(
    bobDb.collection("call_rooms").doc("room_1").update({
      guestId: BOB_UID,
      guestName: "Bob",
      guestCountryCode: "TH",
      guestCountryName: "Thailand",
      status: "connected",
      rtcAnswerSdp: "v=0 answer...",
      updatedAt: new Date(),
    })
  );

  // Charlie cannot read a connected room he is not part of
  await assertFails(charlieDb.collection("call_rooms").doc("room_1").get());

  // Alice and Bob can exchange messages in room_1
  await assertSucceeds(
    aliceDb.collection("call_rooms").doc("room_1").collection("messages").doc("msg_1").set({
      roomId: "room_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hello!",
      participants: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    bobDb.collection("call_rooms").doc("room_1").collection("messages")
      .where("participants", "array-contains", BOB_UID).get()
  );

  // Charlie cannot read a message or list messages in room_1 without his own participant filter
  await assertFails(
    charlieDb.collection("call_rooms").doc("room_1").collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlieDb.collection("call_rooms").doc("room_1").collection("messages").get()
  );

  // Bob ends the call
  await assertSucceeds(
    bobDb.collection("call_rooms").doc("room_1").update({
      status: "ended",
      updatedAt: new Date(),
    })
  );

  // Terminal state lock: cannot update room once ended
  await assertFails(
    aliceDb.collection("call_rooms").doc("room_1").update({
      status: "connected",
      updatedAt: new Date(),
    })
  );
});

test("ModerationAction: User can report/block another user, cannot report self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("moderation_actions").doc("mod_1").set({
      userId: ALICE_UID,
      targetUserId: BOB_UID,
      targetCountryCode: "TH",
      actionType: "report",
      reason: "Inappropriate behavior",
      roomId: "room_1",
      createdAt: now,
    })
  );

  // Cannot report self
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("moderation_actions").doc("mod_2").set({
      userId: ALICE_UID,
      targetUserId: ALICE_UID,
      targetCountryCode: "MM",
      actionType: "block",
      reason: "Self block",
      roomId: "room_1",
      createdAt: now,
    })
  );
});
