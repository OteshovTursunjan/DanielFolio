const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_folio_123";
const BOB_UID = "bob_folio_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

// --- UNAUTHENTICATED DENIAL TESTS ---

test("Unauthenticated user: denied read/write on notes, taskLists, and tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/notes`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/taskLists`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/tasks`).get());

  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/notes`).doc("note_1").set({
      userId: ALICE_UID,
      title: "Unauth Note",
      content: "Body",
      category: "personal",
      colorKey: "default",
      pinned: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

// --- NOTES COLLECTION TESTS (MIRRORING FolioRepository.kt) ---

test("Notes: owner can list empty subcollection, create, update, toggle pin, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Empty subcollection list must succeed (Pillar 8)
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/notes`).get());

  // Mirror FolioRepository.addNote payload (NO `id` field)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_1").set({
      userId: ALICE_UID,
      title: "Идеи для проекта",
      content: "Синхронизация заметок через облако",
      category: "ideas",
      colorKey: "sand",
      pinned: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Mirror FolioRepository.toggleNotePin partial update
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_1").update({
      pinned: true,
      updatedAt: updated,
    })
  );

  // Mirror FolioRepository.updateNote partial update
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_1").update({
      title: "Обновлённые идеи",
      content: "Детали архитектуры",
      category: "work",
      colorKey: "sage",
      pinned: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Read & Delete
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_1").get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_1").delete());
});

test("Notes: rejects ghost fields, @DocumentId 'id', future timestamps, immutable createdAt edit, and empty note", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Reject 'id' field in payload
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_bad_id").set({
      id: "note_bad_id",
      userId: ALICE_UID,
      title: "Bad",
      content: "Body",
      category: "personal",
      colorKey: "default",
      pinned: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject ghost field (Shadow Update test)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_ghost").set({
      userId: ALICE_UID,
      title: "Ghost",
      content: "Body",
      category: "personal",
      colorKey: "default",
      pinned: false,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject empty title + empty content
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_empty").set({
      userId: ALICE_UID,
      title: "",
      content: "",
      category: "personal",
      colorKey: "default",
      pinned: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_future").set({
      userId: ALICE_UID,
      title: "Future",
      content: "Body",
      category: "personal",
      colorKey: "default",
      pinned: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Create valid note, then reject mutating immutable createdAt
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_imm").set({
      userId: ALICE_UID,
      title: "Valid",
      content: "",
      category: "personal",
      colorKey: "default",
      pinned: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/notes`).doc("note_imm").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

// --- TASK LISTS & TASKS TESTS (MIRRORING FolioRepository.kt) ---

test("TaskLists & Tasks: owner CRUD, relational list existence check, optional dueDate, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Creating a task referencing a non-existent list MUST fail (Pillar 11)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/tasks`).doc("orphan_task").set({
      userId: ALICE_UID,
      listId: "missing_list",
      title: "Orphan task",
      details: "",
      completed: false,
      priority: "high",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create TaskList (mirroring FolioRepository.addTaskList)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/taskLists`).doc("list_1").set({
      userId: ALICE_UID,
      name: "Покупки",
      colorKey: "sage",
      iconKey: "shopping",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Rename TaskList (mirroring FolioRepository.updateTaskList)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/taskLists`).doc("list_1").update({
      name: "Продукты на неделю",
      colorKey: "terracotta",
      iconKey: "checklist",
      updatedAt: updated,
    })
  );

  // Create Task without optional dueDateEpochDay (mirroring filterValues { it != null })
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/tasks`).doc("task_1").set({
      userId: ALICE_UID,
      listId: "list_1",
      title: "Купить кофе",
      details: "Свежей обжарки",
      completed: false,
      priority: "high",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create Task WITH optional dueDateEpochDay
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/tasks`).doc("task_2").set({
      userId: ALICE_UID,
      listId: "list_1",
      title: "Отправить отчёт",
      details: "",
      completed: false,
      priority: "medium",
      dueDateEpochDay: 20776,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Toggle completed (mirroring FolioRepository.toggleTaskCompleted)
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/tasks`).doc("task_1").update({
      completed: true,
      updatedAt: updated,
    })
  );

  // Update task with due date set
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/tasks`).doc("task_1").update({
      listId: "list_1",
      title: "Купить зерновой кофе",
      details: "Арабика",
      completed: false,
      priority: "low",
      dueDateEpochDay: 20780,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid priority enum (Value Poisoning test)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/tasks`).doc("task_1").update({
      priority: "urgent_invalid",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Cross-user isolation: Bob cannot read, list, update, or delete Alice's data
  await assertFails(bobDb.collection(`users/${ALICE_UID}/notes`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/taskLists`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/tasks`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/tasks`).doc("task_1").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/tasks`).doc("task_1").update({
      completed: true,
      updatedAt: updated,
    })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/tasks`).doc("task_1").delete());

  // Owner list queries succeed
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/taskLists`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/tasks`).get());
});
