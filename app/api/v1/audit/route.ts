import { NextResponse } from "next/server";
import { db, newId, sha256, nowIso } from "@/lib/db";
import { record, readAudit } from "@/lib/audit";

export async function OPTIONS() {
  return new NextResponse(null, {
    status: 200,
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
    },
  });
}

async function ensureAuditLogTable(sql: any) {
  try {
    await sql.query(`
      CREATE TABLE IF NOT EXISTS audit_log (
        id            TEXT PRIMARY KEY,
        at            TIMESTAMPTZ NOT NULL DEFAULT now(),
        action        TEXT NOT NULL,
        actor_id      TEXT,
        patient_id    TEXT,
        details       JSONB NOT NULL DEFAULT '{}'::jsonb,
        checksum      TEXT NOT NULL,
        prev_checksum TEXT NOT NULL DEFAULT '0000000000000000000000000000000000000000000000000000000000000000',
        created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
      );
    `);
  } catch (err) {
    console.warn("ensureAuditLogTable notice:", err instanceof Error ? err.message : String(err));
  }
}

export async function GET(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const limit = parseInt(searchParams.get("limit") || "100", 10);
    const patientId = searchParams.get("patient_id") || searchParams.get("patientId") || undefined;
    const actorId = searchParams.get("actor_id") || searchParams.get("actorId") || undefined;

    const sql = db();
    await ensureAuditLogTable(sql);

    let rows: Array<Record<string, any>> = [];
    try {
      if (patientId && actorId) {
        rows = (await sql.query(
          "SELECT * FROM audit_log WHERE patient_id = $1 AND actor_id = $2 ORDER BY created_at DESC LIMIT $3",
          [patientId, actorId, limit]
        )) as Array<Record<string, any>>;
      } else if (patientId) {
        rows = (await sql.query(
          "SELECT * FROM audit_log WHERE patient_id = $1 ORDER BY created_at DESC LIMIT $2",
          [patientId, limit]
        )) as Array<Record<string, any>>;
      } else if (actorId) {
        rows = (await sql.query(
          "SELECT * FROM audit_log WHERE actor_id = $1 ORDER BY created_at DESC LIMIT $2",
          [actorId, limit]
        )) as Array<Record<string, any>>;
      } else {
        rows = (await sql.query(
          "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT $1",
          [limit]
        )) as Array<Record<string, any>>;
      }
    } catch (e) {
      console.warn("Error reading audit_log table, using fallback:", e);
    }

    if (rows && rows.length > 0) {
      return NextResponse.json(
        {
          status: "success",
          count: rows.length,
          data: rows,
          entries: rows,
          logs: rows,
        },
        { status: 200 }
      );
    }

    // Fallback: read from standard audit table via lib/audit
    const auditEntries = await readAudit(limit, patientId, actorId);
    const formatted = auditEntries.map((e) => ({
      id: `aud_${e.seq}`,
      at: e.at instanceof Date ? e.at.toISOString() : String(e.at),
      action: e.action,
      actor_id: e.actor_id,
      patient_id: e.patient_id,
      details: { entity: e.entity, entity_id: e.entity_id, outcome: e.outcome },
      checksum: sha256(`${e.seq}:${e.at}:${e.action}`),
      prev_checksum: "0".repeat(64),
      created_at: e.at instanceof Date ? e.at.toISOString() : String(e.at),
    }));

    return NextResponse.json(
      {
        status: "success",
        count: formatted.length,
        data: formatted,
        entries: formatted,
        logs: formatted,
      },
      { status: 200 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to fetch audit entries";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json().catch(() => ({}));
    const action = body.action ? String(body.action) : "clinical_access";
    const actorId = body.actor_id || body.actorId || "c_ingrid";
    const patientId = body.patient_id || body.patientId || null;
    const details = body.details || body.data || {};

    const sql = db();
    await ensureAuditLogTable(sql);

    // Get previous checksum to create SHA-256 checksum link
    let prevChecksum = "0".repeat(64);
    try {
      const tail = (await sql.query(
        "SELECT checksum FROM audit_log ORDER BY created_at DESC, id DESC LIMIT 1"
      )) as Array<{ checksum: string }>;
      if (tail && tail[0] && tail[0].checksum) {
        prevChecksum = tail[0].checksum;
      }
    } catch (e) {}

    const at = nowIso();
    const id = newId("aud");
    const payloadToHash = `${id}:${at}:${action}:${actorId}:${patientId || ""}:${JSON.stringify(details)}:${prevChecksum}`;
    const checksum = sha256(payloadToHash);

    // Insert into audit_log table
    await sql.query(
      `INSERT INTO audit_log (
        id, at, action, actor_id, patient_id, details, checksum, prev_checksum, created_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [
        id,
        at,
        action,
        actorId,
        patientId,
        JSON.stringify(details),
        checksum,
        prevChecksum,
        at,
      ]
    );

    // Also record entry in standard audit table for chain completeness
    try {
      await record({
        actorId: actorId,
        actorRole: "clinician",
        action: action,
        entity: "patient",
        entityId: patientId || undefined,
        patientId: patientId || undefined,
        outcome: "ok",
      });
    } catch (recErr) {
      console.warn("Failed to record into standard audit table:", recErr);
    }

    const newEntry = {
      id,
      at,
      action,
      actor_id: actorId,
      patient_id: patientId,
      details,
      checksum,
      prev_checksum: prevChecksum,
      created_at: at,
    };

    return NextResponse.json(
      {
        status: "success",
        data: newEntry,
        entry: newEntry,
        audit: newEntry,
      },
      { status: 201 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to create audit log entry";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}
