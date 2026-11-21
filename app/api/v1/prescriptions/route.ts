import { NextResponse } from "next/server";
import { db, newId } from "@/lib/db";
import { authenticateApiRequest } from "@/lib/auth";
import { record } from "@/lib/audit";

export async function GET(request: Request) {
  try {
    const user = await authenticateApiRequest(request);
    if (!user) {
      return NextResponse.json(
        { status: "error", message: "Unauthorized. Authentication required." },
        { status: 401 }
      );
    }

    const sql = db();
    const prescriptions = (await sql.query(
      `SELECT 
        rx.*, 
        p.name AS patient_name, 
        p.op_no AS patient_op_no,
        c.name AS clinician_name
      FROM prescriptions rx
      JOIN patients p ON rx.patient_id = p.id
      LEFT JOIN clinicians c ON rx.clinician_id = c.id
      ORDER BY rx.issued_at DESC`
    )) as Array<Record<string, any>>;

    return NextResponse.json({
      status: "success",
      count: prescriptions.length,
      data: prescriptions,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to fetch prescriptions";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const user = await authenticateApiRequest(request);
    if (!user) {
      return NextResponse.json(
        { status: "error", message: "Unauthorized. Authentication required." },
        { status: 401 }
      );
    }

    const body = await request.json();
    const {
      patient_id: patientSnake,
      patientId: patientCamel,
      drug,
      form,
      dose,
      route,
      frequency,
      duration_days: durationSnake,
      durationDays: durationCamel,
      refills: refillsInput,
      indication,
      override_reason: overrideSnake,
      overrideReason: overrideCamel,
      safety_review_summary: safetyReviewSnake,
      safetyReviewSummary: safetyReviewCamel,
      clinician_confirmation: confirmationSnake,
      clinicianConfirmation: confirmationCamel,
    } = body || {};

    const patientId = patientSnake || patientCamel;
    if (!patientId || typeof patientId !== "string") {
      return NextResponse.json(
        { status: "error", message: "patient_id is required." },
        { status: 400 }
      );
    }

    if (!drug || typeof drug !== "string" || drug.trim() === "") {
      return NextResponse.json(
        { status: "error", message: "Medication / drug name is required." },
        { status: 400 }
      );
    }

    if (!dose || typeof dose !== "string" || dose.trim() === "") {
      return NextResponse.json(
        { status: "error", message: "Dose (e.g. 500mg) is required." },
        { status: 400 }
      );
    }

    const sql = db();

    // Determine clinician id
    // Attribute prescribing to the authenticated account, never a client-supplied identity.
    const clinicianId = user.clinicianId;

    const rxForm = form || "Tablet";
    const rxRoute = route || "Oral";
    const rxFrequency = frequency || "Once daily";
    const rxDurationDays = Number(durationSnake || durationCamel || 5);
    const rxRefills = Number(refillsInput || 0);
    const rxIndication = indication || "Clinical indication recorded";
    const overrideReason = overrideSnake || overrideCamel || null;
    const safetyReviewSummary = safetyReviewSnake || safetyReviewCamel || null;
    const clinicianConfirmation = confirmationSnake ?? confirmationCamel;

    if (clinicianConfirmation !== true || typeof safetyReviewSummary !== "string" || !safetyReviewSummary.trim()) {
      return NextResponse.json(
        { status: "error", message: "Clinician confirmation and medication safety review are required before issuing this prescription." },
        { status: 400 }
      );
    }

    // Check patient recorded allergies for safety
    const allergies = (await sql.query(
      "SELECT substance, reaction, severity FROM allergies WHERE patient_id = $1",
      [patientId]
    )) as Array<{ substance: string; reaction: string; severity: string }>;

    const drugLower = drug.toLowerCase();
    const matchedAllergy = allergies.find((a) => {
      const sub = (a.substance || "").toLowerCase();
      if (drugLower.includes(sub) || sub.includes(drugLower)) return true;
      if (
        sub.includes("penicillin") &&
        (drugLower.includes("amoxicillin") ||
          drugLower.includes("ampicillin") ||
          drugLower.includes("co-amoxiclav"))
      )
        return true;
      if (
        sub.includes("nsaid") &&
        (drugLower.includes("ibuprofen") ||
          drugLower.includes("naproxen") ||
          drugLower.includes("aspirin") ||
          drugLower.includes("diclofenac"))
      )
        return true;
      if (
        sub.includes("codeine") &&
        (drugLower.includes("co-codamol") || drugLower.includes("dihydrocodeine"))
      )
        return true;
      if (
        sub.includes("sulfa") &&
        (drugLower.includes("sulfamethoxazole") || drugLower.includes("trimethoprim"))
      )
        return true;
      return false;
    });

    if (matchedAllergy && !overrideReason) {
      return NextResponse.json(
        {
          status: "error",
          message: `ALLERGY_ALERT: Patient has a recorded allergy to "${matchedAllergy.substance}" (${matchedAllergy.reaction}, severity: ${matchedAllergy.severity}). Provide a clinical override reason to issue this prescription.`,
        },
        { status: 400 }
      );
    }
    if (overrideReason && typeof overrideReason !== "string") {
      return NextResponse.json(
        { status: "error", message: "A written clinical override justification is required." },
        { status: 400 }
      );
    }
    if (overrideReason && !overrideReason.trim()) {
      return NextResponse.json(
        { status: "error", message: "A written clinical override justification is required." },
        { status: 400 }
      );
    }

    const id = newId("rx");

    await sql.query(
      `INSERT INTO prescriptions (
        id, patient_id, clinician_id, drug, form, dose, route, frequency,
        duration_days, refills, indication, issued_at, override_reason, safety_review_summary
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, NOW(), $12, $13)`,
      [
        id,
        patientId,
        clinicianId,
        drug.trim(),
        rxForm,
        dose.trim(),
        rxRoute,
        rxFrequency,
        rxDurationDays,
        rxRefills,
        rxIndication,
        overrideReason,
        safetyReviewSummary.trim(),
      ]
    );

    await record({
      actorId: user.clinicianId,
      actorRole: user.role,
      action: overrideReason ? "issued prescription with clinical override" : "issued prescription",
      entity: "prescription",
      entityId: id,
      patientId,
    });

    const inserted = (await sql.query(
      `SELECT 
        rx.*, 
        p.name AS patient_name, 
        p.op_no AS patient_op_no,
        c.name AS clinician_name
      FROM prescriptions rx
      JOIN patients p ON rx.patient_id = p.id
      LEFT JOIN clinicians c ON rx.clinician_id = c.id
      WHERE rx.id = $1`,
      [id]
    )) as Array<Record<string, any>>;

    const prescription = inserted[0] || {
      id,
      patient_id: patientId,
      clinician_id: clinicianId,
      drug: drug.trim(),
      form: rxForm,
      dose: dose.trim(),
      route: rxRoute,
      frequency: rxFrequency,
      duration_days: rxDurationDays,
      refills: rxRefills,
      indication: rxIndication,
      override_reason: overrideReason,
      safety_review_summary: safetyReviewSummary.trim(),
    };

    return NextResponse.json(
      { status: "success", data: prescription },
      { status: 201 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to issue prescription";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}
