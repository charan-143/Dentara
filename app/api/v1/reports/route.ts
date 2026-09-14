import { NextResponse } from "next/server";
import { db, newId } from "@/lib/db";

async function ensureReportAttachmentsTable(sql: any) {
  try {
    await sql.query(`
      CREATE TABLE IF NOT EXISTS report_attachments (
        id         TEXT PRIMARY KEY,
        report_id  TEXT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
        filename   TEXT NOT NULL,
        url        TEXT NOT NULL,
        file_type  TEXT,
        created_at TIMESTAMPTZ NOT NULL DEFAULT now()
      );
    `);
  } catch (e) {
    console.warn("Failed to ensure report_attachments table:", e);
  }
}

export async function GET() {
  try {
    const sql = db();
    await ensureReportAttachmentsTable(sql);

    const reports = (await sql.query(
      `SELECT 
        r.*, 
        p.name AS patient_name, 
        p.op_no AS patient_op_no,
        c.name AS clinician_name
      FROM reports r
      JOIN patients p ON r.patient_id = p.id
      LEFT JOIN clinicians c ON r.clinician_id = c.id
      ORDER BY r.taken_at DESC`
    )) as Array<Record<string, any>>;

    let attachments: Array<Record<string, any>> = [];
    try {
      attachments = (await sql.query(
        "SELECT * FROM report_attachments ORDER BY created_at ASC"
      )) as Array<Record<string, any>>;
    } catch {
      attachments = [];
    }

    const attachmentsByReportId = new Map<string, Array<Record<string, any>>>();
    for (const att of attachments) {
      const rid = att.report_id;
      if (!attachmentsByReportId.has(rid)) {
        attachmentsByReportId.set(rid, []);
      }
      attachmentsByReportId.get(rid)!.push(att);
    }

    const reportsWithAttachments = reports.map((r) => ({
      ...r,
      attachments: attachmentsByReportId.get(r.id) || [],
    }));

    return NextResponse.json({
      status: "success",
      count: reportsWithAttachments.length,
      data: reportsWithAttachments,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to fetch diagnostic reports";
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
      kind,
      title,
      summary,
      image,
      taken_at: takenSnake,
      takenAt: takenCamel,
      released_at: releasedSnake,
      releasedAt: releasedCamel,
      releaseImmediately,
      attachments,
    } = body || {};

    const patientId = patientSnake || patientCamel;
    if (!patientId || typeof patientId !== "string") {
      return NextResponse.json(
        { status: "error", message: "patient_id is required." },
        { status: 400 }
      );
    }

    if (!title || typeof title !== "string" || title.trim() === "") {
      return NextResponse.json(
        { status: "error", message: "Report title is required." },
        { status: 400 }
      );
    }

    if (!summary || typeof summary !== "string" || summary.trim() === "") {
      return NextResponse.json(
        { status: "error", message: "Report summary / findings are required." },
        { status: 400 }
      );
    }

    const sql = db();
    await ensureReportAttachmentsTable(sql);

    // Determine clinician id
    let clinicianId = clinicianSnake || clinicianCamel;
    if (!clinicianId) {
      const firstClinician = (await sql.query(
        "SELECT id FROM clinicians WHERE active = true ORDER BY id LIMIT 1"
      )) as Array<{ id: string }>;
      clinicianId = firstClinician[0]?.id || "c1";
    }

    const reportKind = kind || "Radiograph";
    const reportImage = image || null;
    const takenAt = takenSnake || takenCamel ? new Date(takenSnake || takenCamel).toISOString() : new Date().toISOString();
    const releasedAt = releasedSnake || releasedCamel || releaseImmediately ? new Date().toISOString() : null;

    const reportId = newId("rp");

    await sql.query(
      `INSERT INTO reports (
        id, patient_id, clinician_id, kind, title, summary, image, taken_at, released_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [
        reportId,
        patientId,
        clinicianId,
        reportKind,
        title.trim(),
        summary.trim(),
        reportImage,
        takenAt,
        releasedAt,
      ]
    );

    const insertedAttachments: Array<Record<string, any>> = [];

    if (Array.isArray(attachments) && attachments.length > 0) {
      for (const att of attachments) {
        if (!att || typeof att !== "object") continue;
        const filename = String(att.filename || att.name || "attachment").trim();
        const url = String(att.url || att.path || "").trim();
        const fileType = att.file_type || att.fileType || att.type || null;

        if (url) {
          const attId = newId("att");
          await sql.query(
            `INSERT INTO report_attachments (id, report_id, filename, url, file_type, created_at)
            VALUES ($1, $2, $3, $4, $5, NOW())`,
            [attId, reportId, filename, url, fileType]
          );

          insertedAttachments.push({
            id: attId,
            report_id: reportId,
            filename,
            url,
            file_type: fileType,
          });
        }
      }
    }

    const createdReport = {
      id: reportId,
      patient_id: patientId,
      clinician_id: clinicianId,
      kind: reportKind,
      title: title.trim(),
      summary: summary.trim(),
      image: reportImage,
      taken_at: takenAt,
      released_at: releasedAt,
      attachments: insertedAttachments,
    };

    return NextResponse.json(
      { status: "success", data: createdReport },
      { status: 201 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to create diagnostic report";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}
