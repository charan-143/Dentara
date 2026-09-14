import { NextResponse } from "next/server";
import { db, newId } from "@/lib/db";

export async function GET() {
  try {
    const sql = db();
    const appointments = (await sql.query(
      `SELECT 
        a.*, 
        p.name AS patient_name, 
        p.op_no AS patient_op_no
      FROM appointments a
      JOIN patients p ON a.patient_id = p.id
      ORDER BY a.starts_at DESC`
    )) as Array<Record<string, any>>;

    return NextResponse.json({
      status: "success",
      count: appointments.length,
      data: appointments,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to fetch appointments";
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
      patient_id: patientSnake,
      patientId: patientCamel,
      clinician_id: clinicianSnake,
      clinicianId: clinicianCamel,
      starts_at: startsSnake,
      startsAt: startsCamel,
      date,
      slot,
      duration_min: durationSnake,
      durationMin: durationCamel,
      type,
      status,
      room,
    } = body || {};

    const patientId = patientSnake || patientCamel;
    if (!patientId || typeof patientId !== "string") {
      return NextResponse.json(
        { status: "error", message: "patient_id is required." },
        { status: 400 }
      );
    }

    const sql = db();

    // Determine default or given clinician
    let clinicianId = clinicianSnake || clinicianCamel;
    if (!clinicianId) {
      const firstClinician = (await sql.query(
        "SELECT id FROM clinicians WHERE active = true ORDER BY id LIMIT 1"
      )) as Array<{ id: string }>;
      clinicianId = firstClinician[0]?.id || "c1";
    }

    // Determine starts_at timestamp
    let startsAtIso: string;
    if (startsSnake || startsCamel) {
      startsAtIso = new Date(startsSnake || startsCamel).toISOString();
    } else if (date && slot) {
      startsAtIso = new Date(`${date}T${slot}:00`).toISOString();
    } else {
      startsAtIso = new Date().toISOString();
    }

    const durationMin = Number(durationSnake || durationCamel || 30);
    const appointmentType = type || "Examination";
    const appointmentStatus = status || "confirmed";
    const appointmentRoom = room || "";
    const id = newId("a");

    await sql.query(
      `INSERT INTO appointments (
        id, patient_id, clinician_id, starts_at, duration_min, type, status, room, created_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())`,
      [
        id,
        patientId,
        clinicianId,
        startsAtIso,
        durationMin,
        appointmentType,
        appointmentStatus,
        appointmentRoom,
      ]
    );

    const inserted = (await sql.query(
      `SELECT 
        a.*, 
        p.name AS patient_name, 
        p.op_no AS patient_op_no
      FROM appointments a
      JOIN patients p ON a.patient_id = p.id
      WHERE a.id = $1`,
      [id]
    )) as Array<Record<string, any>>;

    const appointment = inserted[0] || {
      id,
      patient_id: patientId,
      clinician_id: clinicianId,
      starts_at: startsAtIso,
      duration_min: durationMin,
      type: appointmentType,
      status: appointmentStatus,
      room: appointmentRoom,
    };

    return NextResponse.json(
      { status: "success", data: appointment },
      { status: 201 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to book appointment";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}

export async function PUT(request: Request) {
  try {
    const body = await request.json();
    const { id, appointmentId, status } = body || {};
    const targetId = id || appointmentId;

    if (!targetId || typeof targetId !== "string") {
      return NextResponse.json(
        { status: "error", message: "Appointment id is required." },
        { status: 400 }
      );
    }

    const validStatuses = ["confirmed", "completed", "cancelled"];
    if (!status || !validStatuses.includes(status)) {
      return NextResponse.json(
        {
          status: "error",
          message: `Invalid status. Must be one of: ${validStatuses.join(", ")}`,
        },
        { status: 400 }
      );
    }

    const sql = db();
    await sql.query(
      "UPDATE appointments SET status = $1 WHERE id = $2",
      [status, targetId]
    );

    const updated = (await sql.query(
      `SELECT 
        a.*, 
        p.name AS patient_name, 
        p.op_no AS patient_op_no
      FROM appointments a
      JOIN patients p ON a.patient_id = p.id
      WHERE a.id = $1`,
      [targetId]
    )) as Array<Record<string, any>>;

    if (updated.length === 0) {
      return NextResponse.json(
        { status: "error", message: "Appointment record not found." },
        { status: 404 }
      );
    }

    return NextResponse.json({
      status: "success",
      data: updated[0],
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update appointment";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}
