const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-maxvanta-8d6e7c12-eb41-4809-8955-647ea133fcd7";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: fs.readFileSync("firestore.rules", "utf8")
    }
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

test("Unauthenticated user cannot read or write user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  const docRef = unauthDb.collection("users").doc("user1");
  await assertFails(docRef.get());
  await assertFails(docRef.set({
    userId: "user1",
    fullName: "Associado Teste",
    email: "teste@maxseg.com",
    phone: "1599887766"
  }));
});

test("Authenticated user can create, read and update their own profile", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore(DATABASE_ID);
  const docRef = user1Db.collection("users").doc("user1");
  
  await assertSucceeds(docRef.set({
    userId: "user1",
    fullName: "Ricardo Almeida",
    email: "ricardo@maxseg.com",
    phone: "15997123456",
    planId: "vip",
    tier: "TITANIUM",
    cardCode: "MAX-8921-SP",
    status: "ATIVO"
  }));

  await assertSucceeds(docRef.get());
});

test("Cross-user access is denied (User2 cannot read or modify User1 profile)", async () => {
  // Setup user1 profile as admin
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore(DATABASE_ID).collection("users").doc("user1").set({
      userId: "user1",
      fullName: "Ricardo Almeida",
      email: "ricardo@maxseg.com",
      phone: "15997123456"
    });
  });

  const user2Db = testEnv.authenticatedContext("user2").firestore(DATABASE_ID);
  const docRef = user2Db.collection("users").doc("user1");

  await assertFails(docRef.get());
  await assertFails(docRef.update({ fullName: "Hacked Name" }));
  await assertFails(docRef.delete());
});

test("User can manage their emergency contacts and cross-user is blocked", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore(DATABASE_ID);
  const contactRef = user1Db.collection("users").doc("user1").collection("emergencyContacts").doc("contact1");

  await assertSucceeds(contactRef.set({
    userId: "user1",
    name: "Maria Almeida",
    relationship: "Esposa",
    phone: "15997889900",
    isPrimary: true
  }));

  await assertSucceeds(contactRef.get());

  const user2Db = testEnv.authenticatedContext("user2").firestore(DATABASE_ID);
  const user2ContactRef = user2Db.collection("users").doc("user1").collection("emergencyContacts").doc("contact1");
  await assertFails(user2ContactRef.get());
  await assertFails(user2ContactRef.delete());
});

test("User can dispatch SOS alert and view their SOS history", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore(DATABASE_ID);
  const sosRef = user1Db.collection("users").doc("user1").collection("sosAlerts").doc("alert1");

  await assertSucceeds(sosRef.set({
    userId: "user1",
    latitude: -24.5086,
    longitude: -48.8465,
    address: "Praça Alberto Dias, Apiaí-SP",
    status: "DISPATCHED",
    dispatchedUnit: "V-02 Resgate APH SOS",
    estimatedMinutes: 4
  }));

  await assertSucceeds(sosRef.get());

  const user2Db = testEnv.authenticatedContext("user2").firestore(DATABASE_ID);
  const user2SosRef = user2Db.collection("users").doc("user1").collection("sosAlerts").doc("alert1");
  await assertFails(user2SosRef.get());
});
