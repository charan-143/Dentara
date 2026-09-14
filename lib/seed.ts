import { db, newId } from "./db";
import { hashPassword } from "./password";

const pad = (n: number) => String(n).padStart(2, "0");
const DAY = 86_400_000;

/** Local wall-clock time offset by whole days, as an ISO string for TIMESTAMPTZ. */
function at(dayOffset: number, hhmm: string): string {
  const d = new Date();
  d.setDate(d.getDate() + dayOffset);
  const [h, m] = hhmm.split(":").map(Number);
  d.setHours(h ?? 0, m ?? 0, 0, 0);
  return d.toISOString();
}

/** Local calendar date as YYYY-MM-DD offset by days. */
function on(dayOffset: number): string {
  const d = new Date(Date.now() + dayOffset * DAY);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/**
 * Populates the Neon / PGlite database tables with default clinical data
 * matching practice roster if tables are empty.
 */
export async function seedDatabase(customDb?: any) {
  const sql = customDb ?? db();

  try {
    const existing = (await sql`SELECT count(*)::int AS n FROM clinicians`) as Array<{ n: number }>;
    const count = Number(existing[0]?.n ?? 0);
    if (count > 0) {
      return;
    }
  } catch (err) {
    // If query fails or table doesn't exist yet, proceed with seeding attempt
  }

  // 1. Clinicians
  const clinicians = [
    {
      id: "c_ingrid",
      name: "Dr. Ingrid Halvorsen",
      credentials: "BDS, MSc",
      specialty: "Restorative & Endodontics",
      room: "Surgery 1",
      photo: "https://picsum.photos/seed/c_ingrid-clinician/240/240",
      bio: "Leads the practice and treats restorative and endodontic cases.",
    },
    {
      id: "c_tomas",
      name: "Dr. Tomas Ferreira",
      credentials: "DDS, Endodontics",
      specialty: "Root canal treatment",
      room: "Surgery 2",
      photo: "https://picsum.photos/seed/c_tomas-clinician/240/240",
      bio: "Handles retreatment and cracked-tooth cases under the operating microscope.",
    },
    {
      id: "c_anaya",
      name: "Dr. Anaya Krishnamurthy",
      credentials: "BDS, MFDS",
      specialty: "Restorative and implants",
      room: "Surgery 3",
      photo: "https://picsum.photos/seed/c_anaya-clinician/240/240",
      bio: "Crowns, bridges and single-tooth implants, including same-day temporaries.",
    },
  ];

  for (const c of clinicians) {
    await sql`
      INSERT INTO clinicians (id, name, credentials, specialty, room, photo, bio, active)
      VALUES (${c.id}, ${c.name}, ${c.credentials}, ${c.specialty}, ${c.room}, ${c.photo}, ${c.bio}, true)
      ON CONFLICT (id) DO NOTHING
    `;
  }

  // 2. Patients
  const patients = [
    {
      id: "p_1",
      mrn: "TD-40182",
      op_no: "OP-40182",
      name: "Rosalind Achebe",
      dob: "1981-04-12",
      phone: "+44 7700 900142",
      email: "r.achebe@example.co.uk",
      address: "742 Evergreen Terrace, Portland, OR 97201",
      primary_clinician_id: "c_ingrid",
      medical_history: "Type 2 diabetes, diet controlled. Penicillin allergy.",
      family_history: "Family history of periodontal disease.",
      past_dental_history: "Root canal treatment tooth 36, regular hygiene visits.",
      photo: "https://picsum.photos/seed/p_1-patient/240/240",
      last_visit: on(-21),
    },
    {
      id: "p_2",
      mrn: "TD-40183",
      op_no: "OP-40183",
      name: "Dmitri Vollmer",
      dob: "1969-11-23",
      phone: "+44 7700 900288",
      email: "d.vollmer@example.co.uk",
      address: "128 Hawthorne Blvd, Portland, OR 97214",
      primary_clinician_id: "c_ingrid",
      medical_history: "Anticoagulant therapy (apixaban).",
      family_history: "Hypertension.",
      past_dental_history: "Surgical extraction tooth 48.",
      photo: "https://picsum.photos/seed/p_2-patient/240/240",
      last_visit: on(-3),
    },
    {
      id: "p_3",
      mrn: "TD-40184",
      op_no: "OP-40184",
      name: "Owen Blackwood",
      dob: "1994-07-05",
      phone: "+44 7700 900319",
      email: "o.blackwood@example.co.uk",
      address: "450 Division St, Portland, OR 97202",
      primary_clinician_id: "c_ingrid",
      medical_history: "Mild asthma. Latex sensitivity.",
      family_history: "No significant family history.",
      past_dental_history: "Routine checkups, composite fillings.",
      photo: "https://picsum.photos/seed/p_3-patient/240/240",
      last_visit: on(-9),
    },
    {
      id: "p_4",
      mrn: "TD-40185",
      op_no: "OP-40185",
      name: "Marisol Cabrera-Reyes",
      dob: "1988-02-18",
      phone: "+44 7700 900451",
      email: "m.cabrera@example.co.uk",
      address: "892 Pearl District Way, Portland, OR 97209",
      primary_clinician_id: "c_ingrid",
      medical_history: "Bisphosphonate therapy. Hypertension.",
      family_history: "Osteoarthritis.",
      past_dental_history: "Crown fit tooth 46.",
      photo: "https://picsum.photos/seed/p_4-patient/240/240",
      last_visit: on(-140),
    },
  ];

  for (const p of patients) {
    await sql`
      INSERT INTO patients (
        id, mrn, op_no, name, dob, phone, email, address,
        primary_clinician_id, medical_history, family_history,
        past_dental_history, photo, last_visit
      )
      VALUES (
        ${p.id}, ${p.mrn}, ${p.op_no}, ${p.name}, ${p.dob}, ${p.phone}, ${p.email}, ${p.address},
        ${p.primary_clinician_id}, ${p.medical_history}, ${p.family_history},
        ${p.past_dental_history}, ${p.photo}, ${p.last_visit}
      )
      ON CONFLICT (id) DO NOTHING
    `;
  }

  // 3. Allergies & Conditions
  const allergies = [
    { p: "p_1", substance: "Penicillin", reaction: "Urticarial rash", severity: "severe" },
    { p: "p_3", substance: "Latex", reaction: "Contact dermatitis", severity: "moderate" },
  ];
  for (const a of allergies) {
    await sql`
      INSERT INTO allergies (id, patient_id, substance, reaction, severity)
      VALUES (${newId("al")}, ${a.p}, ${a.substance}, ${a.reaction}, ${a.severity})
    `;
  }

  const conditions = [
    { p: "p_1", label: "Type 2 diabetes, diet controlled" },
    { p: "p_2", label: "Anticoagulant therapy, apixaban" },
    { p: "p_4", label: "Hypertension" },
    { p: "p_4", label: "Bisphosphonate therapy" },
  ];
  for (const c of conditions) {
    await sql`
      INSERT INTO conditions (id, patient_id, label)
      VALUES (${newId("cn")}, ${c.p}, ${c.label})
    `;
  }

  // 4. Appointments
  const appointments = [
    { id: "a_1", p: "p_1", c: "c_ingrid", at: at(1, "09:30"), min: 60, type: "Root canal, tooth 36", status: "confirmed", room: "Surgery 1" },
    { id: "a_2", p: "p_2", c: "c_ingrid", at: at(1, "11:00"), min: 30, type: "Post-extraction review", status: "confirmed", room: "Surgery 1" },
    { id: "a_3", p: "p_3", c: "c_ingrid", at: at(1, "14:00"), min: 45, type: "Hygiene & Scaling", status: "confirmed", room: "Surgery 1" },
    { id: "a_4", p: "p_4", c: "c_ingrid", at: at(1, "15:30"), min: 30, type: "Comprehensive Examination", status: "confirmed", room: "Surgery 1" },
    { id: "a_5", p: "p_1", c: "c_ingrid", at: at(0, "10:00"), min: 45, type: "Root canal, second visit", status: "confirmed", room: "Surgery 1" },
    { id: "a_6", p: "p_2", c: "c_ingrid", at: at(0, "08:30"), min: 45, type: "Surgical extraction, tooth 48", status: "completed", room: "Surgery 1" },
    { id: "a_7", p: "p_1", c: "c_ingrid", at: at(-21, "16:00"), min: 30, type: "Emergency assessment", status: "completed", room: "Surgery 1" },
  ];
  for (const a of appointments) {
    await sql`
      INSERT INTO appointments (id, patient_id, clinician_id, starts_at, duration_min, type, status, room)
      VALUES (${a.id}, ${a.p}, ${a.c}, ${a.at}, ${a.min}, ${a.type}, ${a.status}, ${a.room})
      ON CONFLICT (id) DO NOTHING
    `;
  }

  // 5. Treatment Plans & Steps
  const plans = [
    {
      id: "tp_1", p: "p_1", c: "c_ingrid", phase: "pre" as const, published: at(-6, "17:40"),
      procedure: "Root canal treatment, lower left first molar (tooth 36)",
      steps: [
        ["Eat a normal meal beforehand", "The appointment runs about an hour and your mouth will be numb afterwards."],
        ["Keep taking your usual medicines", "Including your diabetes medication. Do not skip a dose to prepare for this visit."],
        ["Take 400mg ibuprofen one hour before", "Only if you already tolerate it. This reduces tenderness during the first day."],
        ["Confirm the penicillin allergy on arrival", "It is on your record. We say it out loud together before any prescribing decision."],
      ],
    },
    {
      id: "tp_2", p: "p_1", c: "c_ingrid", phase: "post" as const, published: null,
      procedure: "Root canal treatment, lower left first molar (tooth 36)",
      steps: [
        ["Do not chew on that side until the crown is fitted", "The temporary filling is softer than enamel and can fracture the weakened cusp."],
        ["Expect tenderness for three to five days", "Biting pressure will feel bruised. Throbbing that wakes you at night is not expected."],
        ["Take ibuprofen 400mg with food, up to three times daily", "Stop after five days. Do not exceed 1200mg in 24 hours without asking us."],
        ["Rinse with warm salt water from tomorrow", "Half a teaspoon of salt in a cup of warm water, twice a day, after meals."],
      ],
    },
    {
      id: "tp_3", p: "p_2", c: "c_ingrid", phase: "post" as const, published: at(-3, "09:25"),
      procedure: "Surgical extraction, lower right wisdom tooth (tooth 48)",
      steps: [
        ["Bite on the gauze for 30 minutes", "Firm continuous pressure. Replace it once if it soaks through, then stop."],
        ["No rinsing, spitting or straws for 24 hours", "Disturbing the clot is what causes a dry socket."],
        ["Hold your apixaban dose only if we told you to", "We agreed this with your cardiology team beforehand."],
        ["Return in one week for review", "Sooner if you get a foul taste, spreading swelling, or worsening pain after day three."],
      ],
    },
  ];

  for (const plan of plans) {
    await sql`
      INSERT INTO plans (id, patient_id, clinician_id, procedure, phase, published_at, locked_at)
      VALUES (${plan.id}, ${plan.p}, ${plan.c}, ${plan.procedure}, ${plan.phase}, ${plan.published}, ${plan.published})
      ON CONFLICT (id) DO NOTHING
    `;
    for (let i = 0; i < plan.steps.length; i += 1) {
      const step = plan.steps[i]!;
      await sql`
        INSERT INTO plan_steps (id, plan_id, ordinal, title, detail)
        VALUES (${newId("st")}, ${plan.id}, ${i}, ${step[0]}, ${step[1]})
      `;
    }
  }

  await sql`
    INSERT INTO plan_addenda (id, plan_id, author_id, body, created_at)
    VALUES (${newId("ad")}, ${"tp_3"}, ${"c_ingrid"},
            ${"Patient rang about oozing at 22:00. Advised firm gauze pressure for 20 minutes, which settled it."},
            ${at(-2, "11:10")})
  `;

  // 6. Prescriptions & Reminders & Dose Logs
  const prescriptions = [
    { id: "rx_1", p: "p_1", c: "c_ingrid", drug: "Ibuprofen", form: "tablet", dose: "400mg", route: "oral", freq: "Three times daily with food", days: 5, ind: "Post-endodontic pain", issued: at(-6, "17:45") },
    { id: "rx_2", p: "p_1", c: "c_ingrid", drug: "Chlorhexidine gluconate 0.2%", form: "mouthwash", dose: "10ml", route: "topical rinse", freq: "Twice daily after brushing", days: 7, ind: "Plaque control around temporary restoration", issued: at(-6, "17:46") },
    { id: "rx_3", p: "p_2", c: "c_ingrid", drug: "Amoxicillin", form: "capsule", dose: "500mg", route: "oral", freq: "Three times daily", days: 5, ind: "Prophylaxis after surgical extraction", issued: at(-3, "09:30") },
  ];
  for (const r of prescriptions) {
    await sql`
      INSERT INTO prescriptions (id, patient_id, clinician_id, drug, form, dose, route, frequency, duration_days, indication, issued_at)
      VALUES (${r.id}, ${r.p}, ${r.c}, ${r.drug}, ${r.form}, ${r.dose}, ${r.route}, ${r.freq}, ${r.days}, ${r.ind}, ${r.issued})
      ON CONFLICT (id) DO NOTHING
    `;
  }

  const reminders = [
    { id: "rm_1", rx: "rx_1", p: "p_1", times: ["08:00", "14:00", "20:00"], from: on(-6), to: on(-1) },
    { id: "rm_2", rx: "rx_2", p: "p_1", times: ["08:30", "21:00"], from: on(-6), to: on(1) },
    { id: "rm_3", rx: "rx_3", p: "p_2", times: ["07:00", "15:00", "23:00"], from: on(-3), to: on(2) },
  ];
  for (const r of reminders) {
    await sql`
      INSERT INTO reminders (id, prescription_id, patient_id, times, starts_on, ends_on)
      VALUES (${r.id}, ${r.rx}, ${r.p}, ${JSON.stringify(r.times)}, ${r.from}, ${r.to})
      ON CONFLICT (id) DO NOTHING
    `;
  }

  const doses: Array<[string, string, string, boolean]> = [
    ["rm_1", on(-2), "08:00", true], ["rm_1", on(-2), "14:00", true], ["rm_1", on(-2), "20:00", false],
    ["rm_1", on(-1), "08:00", true], ["rm_1", on(-1), "14:00", true], ["rm_1", on(-1), "20:00", true],
    ["rm_2", on(-1), "08:30", true], ["rm_2", on(0), "08:30", true],
    ["rm_3", on(-1), "07:00", true], ["rm_3", on(0), "07:00", true],
  ];
  for (const [reminder, date, slot, taken] of doses) {
    await sql`
      INSERT INTO dose_log (id, reminder_id, on_date, slot, taken)
      VALUES (${newId("dl")}, ${reminder}, ${date}, ${slot}, ${taken})
      ON CONFLICT (reminder_id, on_date, slot) DO NOTHING
    `;
  }

  // 7. Diagnostic Reports
  const reports = [
    {
      id: "rp_1", p: "p_1", c: "c_ingrid", kind: "Radiograph", title: "Periapical radiograph, tooth 36",
      summary: "Well defined periapical radiolucency at the mesial root, roughly 4mm across. Consistent with irreversible pulpitis progressing to apical periodontitis.",
      image: "https://picsum.photos/seed/thornbury-periapical/640/420", taken: at(-21, "16:20"), released: at(-21, "18:00"),
    },
    {
      id: "rp_2", p: "p_1", c: "c_ingrid", kind: "Charting", title: "Six point periodontal chart",
      summary: "Generalised probing depths of 2mm to 3mm. Isolated 5mm pocket distal to tooth 36 with bleeding on probing.",
      image: null, taken: at(-21, "16:35"), released: at(-21, "18:00"),
    },
    {
      id: "rp_3", p: "p_2", c: "c_ingrid", kind: "Radiograph", title: "Panoramic radiograph",
      summary: "Impacted lower right wisdom tooth in mesioangular position, roots clear of the inferior alveolar canal.",
      image: "https://picsum.photos/seed/thornbury-panoramic/640/420", taken: at(-10, "10:05"), released: at(-10, "12:30"),
    },
    {
      id: "rp_4", p: "p_1", c: "c_ingrid", kind: "Chairside test", title: "Pulp sensibility test summary",
      summary: "Cold test: lingering response over 30 seconds at tooth 36. Percussion tender at 36.",
      image: null, taken: at(-21, "16:45"), released: null,
    },
  ];
  for (const r of reports) {
    await sql`
      INSERT INTO reports (id, patient_id, clinician_id, kind, title, summary, image, taken_at, released_at)
      VALUES (${r.id}, ${r.p}, ${r.c}, ${r.kind}, ${r.title}, ${r.summary}, ${r.image}, ${r.taken}, ${r.released})
      ON CONFLICT (id) DO NOTHING
    `;
  }

  // 8. Staff Accounts
  const logins = [
    { email: "i.halvorsen@thornbury.example", password: "Cusp-Lantern-72", role: "admin", clinician: "c_ingrid" },
    { email: "t.ferreira@thornbury.example", password: "Apex-Meridian-19", role: "clinician", clinician: "c_tomas" },
    { email: "a.krishnamurthy@thornbury.example", password: "Bridge-Quarry-55", role: "clinician", clinician: "c_anaya" },
  ];
  for (const login of logins) {
    const { hash, salt, kdf } = hashPassword(login.password);
    await sql`
      INSERT INTO accounts (id, email, role, clinician_id, password_hash, password_salt, kdf)
      VALUES (${newId("ac")}, ${login.email}, ${login.role}, ${login.clinician}, ${hash}, ${salt}, ${kdf})
      ON CONFLICT (email) DO NOTHING
    `;
  }
}
