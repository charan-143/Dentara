import { NextResponse } from "next/server";
import { db, newId } from "@/lib/db";

export async function GET() {
  try {
    const sql = db();
    const plans = (await sql.query(
      `SELECT 
        tp.*, 
        p.name AS patient_name, 
        p.op_no AS patient_op_no,
        c.name AS clinician_name
      FROM plans tp
      JOIN patients p ON tp.patient_id = p.id
      LEFT JOIN clinicians c ON tp.clinician_id = c.id
      ORDER BY tp.created_at DESC`
    )) as Array<Record<string, any>>;

    const planIds = plans.map((p) => p.id);
    let allSteps: Array<Record<string, any>> = [];

    if (planIds.length > 0) {
      allSteps = (await sql.query(
        "SELECT * FROM plan_steps ORDER BY plan_id ASC, ordinal ASC"
      )) as Array<Record<string, any>>;
    }

    const stepsByPlanId = new Map<string, Array<Record<string, any>>>();
    for (const step of allSteps) {
      const pid = step.plan_id;
      if (!stepsByPlanId.has(pid)) {
        stepsByPlanId.set(pid, []);
      }
      stepsByPlanId.get(pid)!.push(step);
    }

    const plansWithSteps = plans.map((p) => ({
      ...p,
      steps: stepsByPlanId.get(p.id) || [],
    }));

    return NextResponse.json({
      status: "success",
      count: plansWithSteps.length,
      data: plansWithSteps,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to fetch treatment plans";
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
      procedure,
      phase,
      steps,
      stepsText,
    } = body || {};

    const patientId = patientSnake || patientCamel;
    if (!patientId || typeof patientId !== "string") {
      return NextResponse.json(
        { status: "error", message: "patient_id is required." },
        { status: 400 }
      );
    }

    if (!procedure || typeof procedure !== "string" || procedure.trim() === "") {
      return NextResponse.json(
        { status: "error", message: "Procedure or plan title is required." },
        { status: 400 }
      );
    }

    const sql = db();

    // Determine clinician id
    let clinicianId = clinicianSnake || clinicianCamel;
    if (!clinicianId) {
      const firstClinician = (await sql.query(
        "SELECT id FROM clinicians WHERE active = true ORDER BY id LIMIT 1"
      )) as Array<{ id: string }>;
      clinicianId = firstClinician[0]?.id || "c1";
    }

    const planPhase = phase === "post" ? "post" : "pre";
    const planId = newId("tp");
    const nowStr = new Date().toISOString();

    await sql.query(
      `INSERT INTO plans (
        id, patient_id, clinician_id, procedure, phase, created_at, published_at, locked_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
      [planId, patientId, clinicianId, procedure.trim(), planPhase, nowStr, nowStr, nowStr]
    );

    // Process steps
    const insertedSteps: Array<Record<string, any>> = [];

    let parsedSteps: Array<{ title: string; detail: string }> = [];

    if (Array.isArray(steps)) {
      parsedSteps = steps.map((item) => {
        if (typeof item === "string") {
          return { title: item.trim(), detail: "" };
        }
        if (item && typeof item === "object") {
          return {
            title: String(item.title || item.name || "").trim(),
            detail: String(item.detail || item.description || "").trim(),
          };
        }
        return { title: "", detail: "" };
      }).filter((s) => s.title !== "");
    } else if (typeof stepsText === "string" && stepsText.trim() !== "") {
      parsedSteps = stepsText
        .split("\n")
        .map((l) => l.trim())
        .filter(Boolean)
        .map((l) => ({ title: l, detail: "" }));
    }

    for (let i = 0; i < parsedSteps.length; i++) {
      const stepItem = parsedSteps[i]!;
      let title = stepItem.title;
      if (!title.toLowerCase().startsWith("advice to") && !title.toLowerCase().startsWith("step")) {
        title = `Advice to ${title}`;
      }

      const stepId = newId("st");
      await sql.query(
        `INSERT INTO plan_steps (id, plan_id, ordinal, title, detail)
        VALUES ($1, $2, $3, $4, $5)`,
        [stepId, planId, i, title, stepItem.detail]
      );

      insertedSteps.push({
        id: stepId,
        plan_id: planId,
        ordinal: i,
        title,
        detail: stepItem.detail,
      });
    }

    const createdPlan = {
      id: planId,
      patient_id: patientId,
      clinician_id: clinicianId,
      procedure: procedure.trim(),
      phase: planPhase,
      created_at: nowStr,
      published_at: nowStr,
      locked_at: nowStr,
      steps: insertedSteps,
    };

    return NextResponse.json(
      { status: "success", data: createdPlan },
      { status: 201 }
    );
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to create treatment plan";
    return NextResponse.json(
      { status: "error", message },
      { status: 500 }
    );
  }
}
