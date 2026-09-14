import { NextResponse } from "next/server";
import { db, newId } from "@/lib/db";

export async function GET() {
  try {
    const sql = db();
    const patients = (await sql.query(
      "SELECT * FROM patients ORDER BY name ASC"
    )) as Array<Record<string, any>>;

    return NextResponse.json({
      status: "success",
      count: patients.length,
      data: patients,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to fetch patients";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const {
      name,
      dob,
      phone,
      email,
      address,
      mrn: customMrn,
      op_no: customOpNoSnake,
      opNo: customOpNoCamel,
      primary_clinician_id: clinicianSnake,
      primaryClinicianId: clinicianCamel,
      medical_history: medSnake,
      medicalHistory: medCamel,
      family_history: famSnake,
      familyHistory: famCamel,
      past_dental_history: dentSnake,
      pastDentalHistory: dentCamel,
    } = body || {};

    if (!name || typeof name !== "string" || name.trim() === "") {
      return NextResponse.json(
        { status: "error", message: "Patient name is required." },
        { status: 400 }
      );
    }

    if (!dob || typeof dob !== "string" || dob.trim() === "") {
      return NextResponse.json(
        { status: "error", message: "Date of birth (dob) is required." },
        { status: 400 }
      );
    }

    const sql = db();
    const id = newId("p");

    const seqRes = (await sql.query(
      "SELECT count(*)::int AS n FROM patients"
    )) as Array<{ n: number }>;
    const count = seqRes[0]?.n ?? 0;

    const mrn = customMrn || `TD-${40000 + count + 1}`;
    const opNo = customOpNoSnake || customOpNoCamel || `OP-${40000 + count + 1}`;
    const primaryClinicianId = clinicianSnake || clinicianCamel || null;
    const medicalHistory = medSnake || medCamel || null;
    const familyHistory = famSnake || famCamel || null;
    const pastDentalHistory = dentSnake || dentCamel || null;

    await sql.query(
      `INSERT INTO patients (
        id, mrn, op_no, name, dob, phone, email, address,
        primary_clinician_id, medical_history, family_history, past_dental_history, created_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, NOW())`,
      [
        id,
        mrn,
        opNo,
        name.trim(),
        dob.trim(),
        phone || null,
        email || null,
        address || null,
        primaryClinicianId,
        medicalHistory,
        familyHistory,
        pastDentalHistory,
      ]
    );

    const insertedRows = (await sql.query(
      "SELECT * FROM patients WHERE id = $1",
      [id]
    )) as Array<Record<string, any>>;

    const newPatient = insertedRows[0] || {
      id,
      mrn,
      op_no: opNo,
      name: name.trim(),
      dob: dob.trim(),
      phone: phone || null,
      email: email || null,
      address: address || null,
      primary_clinician_id: primaryClinicianId,
      medical_history: medicalHistory,
      family_history: familyHistory,
      past_dental_history: pastDentalHistory,
    };

    return NextResponse.json(
      { status: "success", data: newPatient },
      { status: 201 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to create patient";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}
