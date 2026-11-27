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

function validProfilePayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "StarCaptain",
    highestUnlockedLevel: 1,
    totalStars: 0,
    endlessHighScore: 0,
    blitzHighScore: 0,
    totalBubblesPopped: 0,
    starCoins: 150,
    dailyStreak: 0,
    lastDailyDay: "",
    bombBoosters: 2,
    prismBoosters: 2,
    laserBoosters: 1,
    soundEnabled: true,
    hapticsEnabled: true,
    equippedSkin: "CYAN_INTERCEPTOR",
    unlockedSkins: ["CYAN_INTERCEPTOR"],
    claimedQuests: [],
    claimedPassTiers: [],
    loginRewardDay: 0,
    lastLoginRewardDate: "",
    lastWheelSpinDate: "",
    lastMysteryChestDate: "",
    mysteryChestsOpened: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validLeaderboardPayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "StarCaptain",
    endlessHighScore: 4200,
    blitzHighScore: 3100,
    totalStars: 18,
    pilotRankNumber: 3,
    pilotRankTitle: "Nebula Ace",
    equippedSkin: "SOLAR_VALKYRIE",
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write profiles, levels, runs, or leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("levels").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("runs").get());
  await assertFails(unauthDb.collection("leaderboard").get());
  await assertFails(unauthDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID)));
});

test("Authenticated owner: valid v4 profile create, Season Pass, Chest, Blitz & Skin updates succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      highestUnlockedLevel: 24,
      totalStars: 60,
      blitzHighScore: 8400,
      totalBubblesPopped: 1400,
      starCoins: 540,
      dailyStreak: 2,
      lastDailyDay: "2026-11-27",
      bombBoosters: 4,
      equippedSkin: "SOLAR_VALKYRIE",
      unlockedSkins: ["CYAN_INTERCEPTOR", "SOLAR_VALKYRIE"],
      claimedQuests: ["quest_pop_50"],
      claimedPassTiers: ["free_1", "bonus_1"],
      loginRewardDay: 2,
      lastLoginRewardDate: "2026-11-27",
      lastWheelSpinDate: "2026-11-27",
      lastMysteryChestDate: "2026-11-27",
      mysteryChestsOpened: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      soundEnabled: false,
      hapticsEnabled: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid cannon skin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      equippedSkin: "HACKED_SKIN",
      updatedAt: new Date(Date.now() - 250),
    })
  );

  // Reject negative starCoins
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      starCoins: -50,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject ghost field / shadow update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Leaderboard: signed-in users can read all rankings, but only update their own entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID))
  );
  await assertSucceeds(
    bobDb.collection("leaderboard").orderBy("endlessHighScore", "desc").limit(25).get()
  );
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      ...validLeaderboardPayload(ALICE_UID),
      pilotRankNumber: 99,
    })
  );
});

test("Authenticated user: cannot read, write, or spoof another user's profile or subcollections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({
    starCoins: 9999,
    updatedAt: new Date(Date.now() - 1000),
  }));
});

test("LevelProgress & BLITZ/DAILY/ENDLESS GameRun: valid writes succeed; invalid bounds fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 8000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("levels").doc("level_30").set({
      levelNumber: 30,
      bestScore: 14500,
      stars: 3,
      completed: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("runs").doc("blitz_1").set({
      mode: "BLITZ",
      levelNumber: 0,
      score: 9100,
      maxCombo: 14,
      bubblesPopped: 128,
      starsEarned: 0,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("runs").doc("bad_mode").set({
      mode: "CHEAT",
      levelNumber: 0,
      score: 100,
      maxCombo: 1,
      bubblesPopped: 5,
      starsEarned: 0,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});
